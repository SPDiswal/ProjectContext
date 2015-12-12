package dk.au.ProjectContext.external.rejseplanen;

public class Departure
{
    private Route route;
    private int timeOfDay;

    public Departure(final Route route, final int timeOfDay)
    {
        this.route = route;
        this.timeOfDay = timeOfDay;
    }

    public Route getRoute()
    {
        return route;
    }

    public int getTimeOfDay()
    {
        return timeOfDay;
    }
}
