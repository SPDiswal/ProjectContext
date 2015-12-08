package dk.au.ProjectContext;

import android.location.Location;

import java.util.*;

public class BusRouteLoader
{
    private List<BusStop> marienlund5a = new ArrayList<BusStop>();
    private List<BusStop> tangkrogen5a = new ArrayList<BusStop>();

    public BusRouteLoader()
    {
        marienlund5a.add(new BusStop("Silkeborgvej", at(56.156794, 10.183073)));
        marienlund5a.add(new BusStop("AUH i Tage-Hansens Gade", at(56.159471, 10.183161)));
        marienlund5a.add(new BusStop("Viborgvej", at(56.161562, 10.183919)));
        marienlund5a.add(new BusStop("Møllevang", at(56.165650, 10.188319)));
        marienlund5a.add(new BusStop("Vanggaardscentret", at(56.167840, 10.191674)));
        marienlund5a.add(new BusStop("Hangøvej", at(56.169546, 10.195574)));
        marienlund5a.add(new BusStop("Universitetet", at(56.170894, 10.200538)));
        marienlund5a.add(new BusStop("Randersvej", at(56.171726, 10.205864)));

        tangkrogen5a.add(new BusStop("Randersvej", at(56.171721, 10.203812)));
        tangkrogen5a.add(new BusStop("Universitetet", at(56.171109, 10.199859)));
        tangkrogen5a.add(new BusStop("Hangøvej", at(56.169685, 10.194715)));
        tangkrogen5a.add(new BusStop("Vanggaardscentret", at(56.167367, 10.189850)));
        tangkrogen5a.add(new BusStop("Møllevang", at(56.165934, 10.187849)));
        tangkrogen5a.add(new BusStop("Viborgvej", at(56.161809, 10.183373)));
        tangkrogen5a.add(new BusStop("AUH i Tage-Hansens Gade", at(56.158871, 10.182463)));
        tangkrogen5a.add(new BusStop("Silkeborgvej", at(56.155291, 10.182462)));
    }

    public BusRoute getMarienlund()
    {
        return new BusRoute(marienlund5a);
    }

    public BusRoute getTangkrogen()
    {
        return new BusRoute(tangkrogen5a);
    }

    private static Location at(double latitude, double longitude)
    {
        Location location = new Location("projectContext");

        location.setLatitude(latitude);
        location.setLongitude(longitude);

        return location;
    }
}
