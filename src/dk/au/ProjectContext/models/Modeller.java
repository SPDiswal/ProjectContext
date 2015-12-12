package dk.au.ProjectContext.models;

import android.location.Location;
import dk.au.ProjectContext.external.rejseplanen.*;
import weka.core.*;

import java.util.*;

public class Modeller
{
    private final ModelFinishedListener listener;
    private final Journey journey;
    private final Route route;
    private final int locationSampleTime;
    private final int nearbyStopDistanceThreshold;

    private final Map<Stop, List<Sample>> models = new HashMap<Stop, List<Sample>>();
    private FastVector features;

    public Modeller(final ModelFinishedListener listener,
                    final Journey journey,
                    final int locationSampleTime,
                    final int nearbyStopDistanceThreshold)
    {
        this.listener = listener;
        this.journey = journey;
        this.route = journey.getRoute();
        this.locationSampleTime = locationSampleTime;
        this.nearbyStopDistanceThreshold = nearbyStopDistanceThreshold;

        for (Stop stop : this.route)
        {
            models.put(stop, new ArrayList<Sample>());
        }

        initialiseAttributes();
    }

    private void initialiseAttributes()
    {
        features = new FastVector(11);

        // Distance: Numeric (metres).
        Attribute distanceAttribute = new Attribute("distance");
        features.addElement(distanceAttribute);

        // Time of day: Numeric (seconds since midnight).
        Attribute timeOfDayAttribute = new Attribute("timeOfDay");
        features.addElement(timeOfDayAttribute);

        // Day of week: Nominal.
        FastVector daysOfTheWeek = new FastVector(7);
        daysOfTheWeek.addElement("Monday");
        daysOfTheWeek.addElement("Tuesday");
        daysOfTheWeek.addElement("Wednesday");
        daysOfTheWeek.addElement("Thursday");
        daysOfTheWeek.addElement("Friday");
        daysOfTheWeek.addElement("Saturday");
        daysOfTheWeek.addElement("Sunday");

        Attribute dayOfWeekAttribute = new Attribute("dayOfWeek", daysOfTheWeek);
        features.addElement(dayOfWeekAttribute);

        // Scheduled time of arrival: Numeric (seconds since midnight).
        Attribute scheduledTimeOfArrivalAttribute = new Attribute("scheduledTimeOfArrival");
        features.addElement(scheduledTimeOfArrivalAttribute);

        // Weather: String.
        Attribute weatherAttribute = new Attribute("weather");
        features.addElement(weatherAttribute);

        // Temperature: Numeric (degrees Celsius).
        Attribute temperatureAttribute = new Attribute("temperature");
        features.addElement(temperatureAttribute);

        // Wind speed: Numeric (metres per second).
        Attribute windSpeedAttribute = new Attribute("windSpeed");
        features.addElement(windSpeedAttribute);

        // Wind direction: Numeric (degrees).
        Attribute windDirectionAttribute = new Attribute("windDirection");
        features.addElement(windDirectionAttribute);

        // Precipitation: Numeric (millimetres).
        Attribute precipitationAttribute = new Attribute("precipitation");
        features.addElement(precipitationAttribute);

        // Traffic level: Numeric (level 0 to 4).
        Attribute trafficAttribute = new Attribute("traffic");
        features.addElement(trafficAttribute);

        // Predicted time delay (class): Nominal (seconds of delay).
        FastVector delays = new FastVector(8);
        delays.addElement("OnTime");

        for (int i = 15; i < 300; i += 15)
        {
            delays.addElement("EarlyBy" + i);
            delays.addElement("LateBy" + i);
        }

        Attribute delayAttribute = new Attribute("delay", delays);
        features.addElement(delayAttribute);
    }

    public void takeSample(final Stop nextStop, final Location location, final int trafficLevel)
    {
        addSampleToModels(nextStop, location, trafficLevel);
        if (isNearStop(nextStop, location)) goToNext(nextStop);
    }

    public void goToNext(final Stop stopToSkip)
    {
        aggregateSamplesToAverages(stopToSkip);
        Instances instances = createInstancesFromSamples(stopToSkip);

        listener.retrieveModel(stopToSkip, instances);
    }

    private Instances createInstancesFromSamples(final Stop stop)
    {
        List<Sample> samples = models.get(stop);

        Instances instances = new Instances("Schedule", features, 0);



        // TODO: Fetch timetables (departures + journeys) from Rejseplanen, either here or in the activity.

        return instances;
    }

    private void addSampleToModels(final Stop nextStop, final Location location, final int trafficLevel)
    {
        for (Stop stop : route)
        {
            if (!route.isBefore(stop, nextStop))
            {
                int distanceToStop = route.distanceTo(location, nextStop) + route.distanceBetween(nextStop, stop);
                models.get(stop).add(new Sample(distanceToStop, trafficLevel));
            }
        }
    }

    private boolean isNearStop(final Stop nextStop, final Location location)
    {
        int distanceToNextStop = (int) Math.floor(location.distanceTo(nextStop.getLocation()));

        return distanceToNextStop < nearbyStopDistanceThreshold
               || distanceToNextStop < (locationSampleTime / 1000 * location.getSpeed());
    }

    private void aggregateSamplesToAverages(final Stop nextStop)
    {
        List<Sample> samples = models.get(nextStop);

        for (int i = 0; i < samples.size(); i++)
        {
            Sample sample = samples.get(i);
            int trafficSum = 0;

            for (int j = i; j < samples.size(); j++)
            {
                trafficSum += samples.get(j).getTraffic();
            }

            int trafficAverage = (int) Math.floor((double) trafficSum / (double) (samples.size() - i));
            sample.setTraffic(trafficAverage);
        }
    }

    public interface ModelFinishedListener
    {
        void retrieveModel(Stop stop, Instances instances);
    }
}
