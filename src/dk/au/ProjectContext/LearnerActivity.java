package dk.au.ProjectContext;

import android.app.Activity;
import android.content.Context;
import android.location.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import java.util.*;

public class LearnerActivity extends Activity
{
    public static final int SAMPLE_TIME = 2000; // Milliseconds.
    public static final int DISTANCE_THRESHOLD = 40; // Metres.

    private BusRouteLoader busRoutes = new BusRouteLoader();

    private Location currentLocation;
    private BusRoute currentBusRoute;
    private BusStop nextBusStop;

    private int traffic;

    private List<Sample> samples = new ArrayList<Sample>();

    private Map<BusStop, List<Sample>> models = new HashMap<BusStop, List<Sample>>();

    private LocationManager locationManager;
    private LocationListener listener;

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
        return new Sample(currentLocation, distanceToBusStop, new Date(), traffic);
    }

    private void updateCurrentBusStop(int distanceToNextBusStop)
    {
        ((TextView) findViewById(R.id.nextStop)).setText(nextBusStop.getName());
        ((TextView) findViewById(R.id.distance)).setText(distanceToNextBusStop + " m");

        if (distanceToNextBusStop < DISTANCE_THRESHOLD)
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

            BusStop nextCandidateBusStop = currentBusRoute.next(nextBusStop);

            if (nextCandidateBusStop != null)
            {
                nextBusStop = nextCandidateBusStop;
            }
            else
            {
                currentBusRoute = null;
                ((TextView) findViewById(R.id.distance)).setText("");

                Toast.makeText(LearnerActivity.this, "Finished!", Toast.LENGTH_SHORT).show();
            }
        }
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
    }

    public void onTrafficLevelSlow(View view)
    {
        traffic = 2;
    }

    public void onTrafficLevelNormal(View view)
    {
        traffic = 0;
    }
}
