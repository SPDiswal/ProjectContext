package dk.au.ProjectContext;

import android.app.Activity;
import android.content.Context;
import android.location.*;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.List;

public class LearnerActivity extends Activity
{
    private BusRouteLoader busRoutes = new BusRouteLoader();

    private Location currentLocation;
    private List<BusStop> currentBusRoute;

    private LocationManager locationManager;
    private LocationListener listener = new LocationListener()
    {
        public void onLocationChanged(final Location location)
        {
            currentLocation = location;
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

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.learner);

        this.locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, listener);
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

            Toast.makeText(this, "Go!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, "No location yet :(", Toast.LENGTH_SHORT).show();
        }
    }
}
