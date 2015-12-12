package dk.au.ProjectContext.location;

import android.location.*;
import android.os.Bundle;

public class StandardLocationListener implements LocationListener
{
    private LocationChangedListener listener;

    public StandardLocationListener(final LocationChangedListener listener)
    {
        this.listener = listener;
    }

    public void onLocationChanged(final Location location)
    {
        listener.retrieveLocation(location);
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

    public interface LocationChangedListener
    {
        void retrieveLocation(Location location);
    }
}
