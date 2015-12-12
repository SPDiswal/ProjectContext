package dk.au.ProjectContext.external.rejseplanen;

public class Departure
{
    private Route route;
    private int timeOfDay;
    private String journeyId;

    public Departure(final Route route, final int timeOfDay, final String journeyId)
    {
        this.route = route;
        this.timeOfDay = timeOfDay;
        this.journeyId = journeyId;
    }

    public Route getRoute()
    {
        return route;
    }

    public int getTimeOfDay()
    {
        return timeOfDay;
    }

    public String getJourneyId()
    {
        return journeyId;
    }
}
