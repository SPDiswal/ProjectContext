package dk.au.ProjectContext;

import android.location.Location;

public class BusStop
{
    private String name;
    private Location location;

    public BusStop(String name, Location location)
    {
        this.name = name;
        this.location = location;
    }

    public String getName()
    {
        return name;
    }

    public Location getLocation()
    {
        return location;
    }
}
