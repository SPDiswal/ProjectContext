package dk.au.ProjectContext.modelling;

import java.util.*;

public class Sample
{
    private int distance;
    private Date time;
    private int traffic;

    public Sample(final int distance, final int traffic)
    {
        this.distance = distance;
        this.traffic = traffic;
        this.time = Calendar.getInstance().getTime();
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

    public void setTraffic(final int traffic)
    {
        this.traffic = traffic;
    }
}
