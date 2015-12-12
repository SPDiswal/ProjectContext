package dk.au.ProjectContext.external.rejseplanen;

import java.util.*;

public class Journey
{
    private Route route;
    private Map<Stop, Integer> timetable;

    public Journey(final Route route, final List<Integer> arrivalTimes)
    {
        this.route = route;
        this.timetable = new HashMap<Stop, Integer>();

        int i = 0;

        for (Stop stop : route)
        {
            timetable.put(stop, arrivalTimes.get(i));
            i++;
        }
    }

    public Route getRoute()
    {
        return route;
    }

    public Map<Stop, Integer> getTimetable()
    {
        return timetable;
    }
}
