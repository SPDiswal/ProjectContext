package dk.au.ProjectContext;

import android.location.Location;

import java.util.Date;

public class Sample
{
    private double latitude;
    private double longitude;
    private int distance;
    private Date time;
    private int traffic;

    public Sample(Location location, int distance, Date time, int traffic)
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

    public int getDistance()
    {
        return distance;
    }

    public Date getTime()
    {
        return time;
    }

    public int getTraffic()
    {
        return traffic;
    }

    public void setTraffic(int traffic)
    {
        this.traffic = traffic;
    }
}
