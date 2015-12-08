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
    private List<BusStop> currentBusRoute;
    private BusStop nextBusStop;

    private TrafficLevel traffic;

    private List<Sample> samples = new ArrayList<Sample>();

    private LocationManager locationManager;
    private LocationListener listener;

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.learner);

        this.locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        this.listener = new LocationListener()
        {
            public void onLocationChanged(final Location location)
            {
                currentLocation = location;

                if (currentBusRoute != null)
                {
                    float distanceToNextBusStop = currentLocation.distanceTo(nextBusStop.getLocation());

                    addSample(distanceToNextBusStop);
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

    private void addSample(float distanceToNextBusStop)
    {
        Sample sample = new Sample(currentLocation, distanceToNextBusStop, new Date(), traffic);

        samples.add(sample);
    }

    private void updateCurrentBusStop(float distanceToNextBusStop)
    {
        ((TextView) findViewById(R.id.nextStop)).setText(nextBusStop.getName());
        ((TextView) findViewById(R.id.distance)).setText((int) Math.floor(distanceToNextBusStop) + " m");

        if (Float.compare(distanceToNextBusStop, DISTANCE_THRESHOLD) < 0)
        {
            int currentBusStopIndex = currentBusRoute.indexOf(nextBusStop);
            int nextBusStopIndex = currentBusStopIndex + 1;

            if (nextBusStopIndex < currentBusRoute.size())
            {
                nextBusStop = currentBusRoute.get(nextBusStopIndex);
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
            Location locationOfSilkeborgvej = busRoutes.getMarienlund().get(0).getLocation();
            Location locationOfRandersvej = busRoutes.getTangkrogen().get(0).getLocation();

            float distanceToSilkeborgvej = currentLocation.distanceTo(locationOfSilkeborgvej);
            float distanceToRandersvej = currentLocation.distanceTo(locationOfRandersvej);

            currentBusRoute = Float.compare(distanceToSilkeborgvej, distanceToRandersvej) < 0
                              ? busRoutes.getMarienlund()
                              : busRoutes.getTangkrogen();

            nextBusStop = currentBusRoute.get(0);

            Toast.makeText(this, "Go!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, "No location yet :(", Toast.LENGTH_SHORT).show();
        }
    }

    public void onTrafficLevelJam(View view)
    {
        traffic = TrafficLevel.JAM;
    }

    public void onTrafficLevelSlow(View view)
    {
        traffic = TrafficLevel.SLOW;
    }

    public void onTrafficLevelNormal(View view)
    {
        traffic = TrafficLevel.NORMAL;
    }
}
