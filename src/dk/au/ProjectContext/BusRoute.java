package dk.au.ProjectContext;

import android.location.Location;

import java.util.*;

public class BusRoute implements Iterable<BusStop>
{
    private List<BusStop> busStops;
    private String name;

    public BusRoute(List<BusStop> busStops, String name)
    {
        this.busStops = busStops;
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public BusStop first()
    {
        return busStops.get(0);
    }

    public BusStop next(BusStop busStop)
    {
        int currentBusStopIndex = busStops.indexOf(busStop);
        int nextBusStopIndex = currentBusStopIndex + 1;
        return nextBusStopIndex < busStops.size() ? busStops.get(nextBusStopIndex) : null;
    }

    public boolean isBefore(BusStop first, BusStop second)
    {
        return busStops.indexOf(first) < busStops.indexOf(second);
    }

    public int distanceTo(Location location, BusStop busStop)
    {
        return (int) Math.floor(location.distanceTo(busStop.getLocation()));
    }

    public int distanceBetween(BusStop first, BusStop second)
    {
        int firstIndex = busStops.indexOf(first);
        int secondIndex = busStops.indexOf(second);

        float distanceSum = 0;

        for (int i = firstIndex; i < secondIndex; i++)
        {
            distanceSum += busStops.get(i).getLocation().distanceTo(busStops.get(i + 1).getLocation());
        }

        return (int) Math.floor(distanceSum);
    }

    public Iterator<BusStop> iterator()
    {
        return busStops.iterator();
    }
}
