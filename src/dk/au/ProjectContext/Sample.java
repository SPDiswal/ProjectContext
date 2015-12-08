package dk.au.ProjectContext;

import android.location.Location;

import java.util.Date;

public class Sample
{
    private double latitude;
    private double longitude;
    private double distance;
    private Date time;
    private TrafficLevel traffic;

    public Sample(Location location, float distance, Date time, TrafficLevel traffic)
    {
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.distance = distance;
        this.time = time;
        this.traffic = traffic;
    }

    public double getLatitude()
    {
        return latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public double getDistance()
    {
        return distance;
    }

    public Date getTime()
    {
        return time;
    }

    public TrafficLevel getTraffic()
    {
        return traffic;
    }
}
