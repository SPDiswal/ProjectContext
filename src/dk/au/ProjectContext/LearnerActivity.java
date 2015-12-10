package dk.au.ProjectContext;

import android.app.Activity;
import android.content.Context;
import android.location.*;
import android.os.*;
import android.view.View;
import android.widget.*;

import java.io.*;
import java.text.*;
import java.util.*;

public class LearnerActivity extends Activity
{
    public static final int SAMPLE_TIME = 2000; // Milliseconds.
    public static final int DISTANCE_THRESHOLD = 40; // Metres.

    private int traffic = 0;
    private boolean goingToNext;

    private BusRouteLoader busRoutes = new BusRouteLoader();

    private Location currentLocation;
    private BusRoute currentBusRoute;
    private BusStop nextBusStop;

    private Map<BusStop, List<Sample>> models = new HashMap<BusStop, List<Sample>>();

    private LocationManager locationManager;
    private LocationListener listener;

    private File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.learner);

        for (BusStop busStop : busRoutes.getTangkrogen())
        {
            models.put(busStop, new ArrayList<Sample>());
        }

        for (BusStop busStop : busRoutes.getMarienlund())
        {
            models.put(busStop, new ArrayList<Sample>());
        }

        this.locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        this.listener = new LocationListener()
        {
            public void onLocationChanged(final Location location)
            {
                currentLocation = location;

                if (currentBusRoute != null)
                {
                    addSamplesToModels();

                    int distanceToNextBusStop = (int) Math.floor(currentLocation.distanceTo(nextBusStop.getLocation()));
                    updateCurrentBusStop(distanceToNextBusStop);
                }
            }

            public void onStatusChanged(final String provider, final int status, final Bundle extras)
            {
            }

            public void onProviderEnabled(final String provider)
            {
            }

            public void onProviderDisabled(final String provider)
            {
            }
        };

        this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, SAMPLE_TIME, 0, listener);
    }

    private void addSamplesToModels()
    {
        for (BusStop busStop : currentBusRoute)
        {
            if (!currentBusRoute.isBefore(busStop, nextBusStop))
            {
                int distanceToBusStop = currentBusRoute.distanceTo(currentLocation, nextBusStop)
                                        + currentBusRoute.distanceBetween(nextBusStop, busStop);

                models.get(busStop).add(sample(distanceToBusStop));
            }
        }
    }

    private Sample sample(int distanceToBusStop)
    {
        Date today = Calendar.getInstance().getTime();
        return new Sample(currentLocation, distanceToBusStop, today, traffic);
    }

    private void updateCurrentBusStop(int distanceToNextBusStop)
    {
        ((TextView) findViewById(R.id.nextStop)).setText(nextBusStop.getName());
        ((TextView) findViewById(R.id.distance)).setText(distanceToNextBusStop + " m");

        if (distanceToNextBusStop < DISTANCE_THRESHOLD)
        {
            aggregateSamplesToAverages();
            saveModel(nextBusStop);

            BusStop nextCandidateBusStop = currentBusRoute.next(nextBusStop);

            if (nextCandidateBusStop != null)
            {
                nextBusStop = nextCandidateBusStop;
            }
            else
            {
                finished();
            }
        }
    }

    private void aggregateSamplesToAverages()
    {
        List<Sample> samples = models.get(nextBusStop);

        for (int i = 0; i < samples.size(); i++)
        {
            Sample sample = samples.get(i);
            int trafficSum = 0;

            for (int j = i; j < samples.size(); j++)
            {
                trafficSum += samples.get(j).getTraffic();
            }

            int trafficAverage = (int) Math.floor((double) trafficSum / (double) (samples.size() - i));
            sample.setTraffic(trafficAverage);
        }
    }

    private boolean isExternalStorageAvailable()
    {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void saveModel(BusStop busStop)
    {
        List<Sample> samples = models.get(busStop);
        String busRouteName = currentBusRoute.getName();
        String busStopName = busStop.getName();

        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");
        Date today = Calendar.getInstance().getTime();
        String timeOfDay = formatter.format(today);

        String fileName = timeOfDay + "-" + busRouteName.replace(" ", "-") + "-" + busStopName.replace(" ", "-");
        File file = new File(path, fileName);

        if (isExternalStorageAvailable())
        {
            try
            {
                FileWriter writer = new FileWriter(file, true);

                for (Sample sample : samples)
                {
                    writer.write(sample.toString() + "\n");
                }

                writer.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void finished()
    {
        currentBusRoute = null;
        ((TextView) findViewById(R.id.distance)).setText("");

        findViewById(R.id.start).setVisibility(View.VISIBLE);

        Toast.makeText(LearnerActivity.this, "Finished!", Toast.LENGTH_SHORT).show();
    }

    public void onStart(View view)
    {
        if (currentLocation != null)
        {
            Location locationOfSilkeborgvej = busRoutes.getMarienlund().first().getLocation();
            Location locationOfRandersvej = busRoutes.getTangkrogen().first().getLocation();

            float distanceToSilkeborgvej = currentLocation.distanceTo(locationOfSilkeborgvej);
            float distanceToRandersvej = currentLocation.distanceTo(locationOfRandersvej);

            currentBusRoute = Float.compare(distanceToSilkeborgvej, distanceToRandersvej) < 0
                              ? busRoutes.getMarienlund()
                              : busRoutes.getTangkrogen();

            nextBusStop = currentBusRoute.first();

            findViewById(R.id.start).setVisibility(View.GONE);
            Toast.makeText(this, "Go!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, "No location yet :(", Toast.LENGTH_SHORT).show();
        }
    }

    public void onTrafficLevelJam(View view)
    {
        traffic = 4;
        goingToNext = false;
    }

    public void onTrafficLevelSlow(View view)
    {
        traffic = 2;
        goingToNext = false;
    }

    public void onTrafficLevelNormal(View view)
    {
        traffic = 0;
        goingToNext = false;
    }

    public void onGoingToNext(View view)
    {
        if (!goingToNext)
        {
            Toast.makeText(this, "Tap again to go to the next bus stop.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            updateCurrentBusStop(0);
        }

        goingToNext = !goingToNext;
    }
}
