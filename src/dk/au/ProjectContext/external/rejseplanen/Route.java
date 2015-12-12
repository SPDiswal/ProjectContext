package dk.au.ProjectContext.external.rejseplanen;

import android.location.Location;

import java.util.*;

public class Route implements Iterable<Stop>
{
    private String id;
    private String name;
    private String direction;

    private List<Stop> stops;

    public Route(final String id, final String name, final String direction, final List<Stop> stops)
    {
        this.id = id;
        this.name = name;
        this.direction = direction;
        this.stops = stops;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDirection()
    {
        return direction;
    }

    public List<Stop> getStops()
    {
        return stops;
    }

    public Stop first()
    {
        return stops.get(0);
    }

    public Stop last()
    {
        return stops.get(stops.size() - 1);
    }

    public Stop next(final Stop stop)
    {
        int currentStopIndex = stops.indexOf(stop);
        int nextStopIndex = currentStopIndex + 1;
        return nextStopIndex < stops.size() ? stops.get(nextStopIndex) : null;
    }

    public boolean isBefore(final Stop first, final Stop second)
    {
        return stops.indexOf(first) < stops.indexOf(second);
    }

    public int distanceTo(final Location location, final Stop stop)
    {
        return (int) Math.floor(location.distanceTo(stop.getLocation()));
    }

    public int distanceBetween(final Stop first, final Stop second)
    {
        int firstIndex = stops.indexOf(first);
        int secondIndex = stops.indexOf(second);

        float distanceSum = 0;

        for (int i = firstIndex; i < secondIndex; i++)
        {
            distanceSum += stops.get(i).getLocation().distanceTo(stops.get(i + 1).getLocation());
        }

        return (int) Math.floor(distanceSum);
    }

    public Iterator<Stop> iterator()
    {
        return stops.iterator();
    }
}
