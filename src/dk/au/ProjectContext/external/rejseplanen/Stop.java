package dk.au.ProjectContext.external.rejseplanen;

import android.location.Location;

public class Stop
{
    private String id;
    private String name;
    private Location location;

    public Stop(final String id, final String name, final double latitude, final double longitude)
    {
        this.id = id;
        this.name = name;

        this.location = new Location("projectContext");
        this.location.setLatitude(latitude);
        this.location.setLongitude(longitude);
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public Location getLocation()
    {
        return location;
    }

    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Stop stop = (Stop) o;
        return id.equals(stop.id) && location.equals(stop.location);

    }

    public int hashCode()
    {
        int result = id.hashCode();
        result = 31 * result + location.hashCode();
        return result;
    }
}
