package dk.au.ProjectContext.models;

import android.location.Location;
import dk.au.ProjectContext.external.rejseplanen.*;
import weka.core.*;

import java.text.*;
import java.util.*;

public class Modeller
{
    public static final String ON_TIME_CLASS = "OnTime";
    public static final String EARLY_BY_CLASS = "EarlyBy";
    public static final String LATE_BY_CLASS = "LateBy";
    public static final int NUMBER_OF_FEATURES = 11;
    public static final String DELAY_OUT_OF_SCOPE = "300Plus";
    private final ModelFinishedListener listener;
    private final Journey journey;
    private final Route route;
    private final int locationSampleTime;
    private final int nearbyStopDistanceThreshold;

    private final Map<Stop, List<Sample>> models = new HashMap<Stop, List<Sample>>();
    private FastVector features;
    private Attribute distanceAttribute;
    private Attribute timeOfDayAttribute;
    private FastVector daysOfTheWeek;
    private Attribute dayOfWeekAttribute;
    private Attribute scheduledTimeOfArrivalAttribute;
    private Attribute weatherAttribute;
    private Attribute temperatureAttribute;
    private Attribute windSpeedAttribute;
    private Attribute windDirectionAttribute;
    private Attribute precipitationAttribute;
    private Attribute trafficAttribute;
    private FastVector delays;
    private Attribute delayAttribute;

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
        features = new FastVector(NUMBER_OF_FEATURES);

        // Distance: Numeric (metres).
        distanceAttribute = new Attribute("distance");
        features.addElement(distanceAttribute);

        // Time of day: Numeric (seconds since midnight).
        timeOfDayAttribute = new Attribute("timeOfDay");
        features.addElement(timeOfDayAttribute);

        // Day of week: Nominal.
        daysOfTheWeek = new FastVector(7);
        daysOfTheWeek.addElement("Monday");
        daysOfTheWeek.addElement("Tuesday");
        daysOfTheWeek.addElement("Wednesday");
        daysOfTheWeek.addElement("Thursday");
        daysOfTheWeek.addElement("Friday");
        daysOfTheWeek.addElement("Saturday");
        daysOfTheWeek.addElement("Sunday");

        dayOfWeekAttribute = new Attribute("dayOfWeek", daysOfTheWeek);
        features.addElement(dayOfWeekAttribute);

        // Scheduled time of arrival: Numeric (seconds since midnight).
        scheduledTimeOfArrivalAttribute = new Attribute("scheduledTimeOfArrival");
        features.addElement(scheduledTimeOfArrivalAttribute);

        // Weather: String.
        weatherAttribute = new Attribute("weather");
        features.addElement(weatherAttribute);

        // Temperature: Numeric (degrees Celsius).
        temperatureAttribute = new Attribute("temperature");
        features.addElement(temperatureAttribute);

        // Wind speed: Numeric (metres per second).
        windSpeedAttribute = new Attribute("windSpeed");
        features.addElement(windSpeedAttribute);

        // Wind direction: Numeric (degrees of direction).
        windDirectionAttribute = new Attribute("windDirection");
        features.addElement(windDirectionAttribute);

        // Precipitation: Numeric (millimetres).
        precipitationAttribute = new Attribute("precipitation");
        features.addElement(precipitationAttribute);

        // Traffic level: Numeric (level 0 to 4).
        trafficAttribute = new Attribute("traffic");
        features.addElement(trafficAttribute);

        // Predicted time delay (class): Nominal (seconds of delay).
        delays = new FastVector(2 * 20 + 3);
        delays.addElement(ON_TIME_CLASS);

        for (int i = 15; i <= 300; i += 15)
        {
            delays.addElement(EARLY_BY_CLASS + i);
            delays.addElement(LATE_BY_CLASS + i);
        }

        delays.addElement(EARLY_BY_CLASS + DELAY_OUT_OF_SCOPE);
        delays.addElement(LATE_BY_CLASS + DELAY_OUT_OF_SCOPE);

        delayAttribute = new Attribute("delay", delays);
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
        int scheduledTimeOfArrivalSecondsSinceMidnight = journey.getTimetable().get(stop);
        List<Sample> samples = models.get(stop);

        Instances instances = new Instances("Schedule", features, 0);

        int actualTimeOfArrivalSecondsSinceMidnight = getActualTimeOfArrival(samples.get(samples.size() - 1));
        int timeDelay = actualTimeOfArrivalSecondsSinceMidnight - scheduledTimeOfArrivalSecondsSinceMidnight;

        String delay;

        if (timeDelay >= -14 && timeDelay <= 14)
        {
            delay = ON_TIME_CLASS;
        }

        for (int i = 15; i <= 300; i += 15)
        {
            if (timeDelay >= i && timeDelay <= i + 14)
            {
                delay = LATE_BY_CLASS + i;
            }
            else if (timeDelay >= -i - 14 && timeDelay <= -i)
            {
                delay = EARLY_BY_CLASS + i;
            }
        }

        if (timeDelay > 300)
        {
            delay = LATE_BY_CLASS + DELAY_OUT_OF_SCOPE;
        }
        else if (timeDelay < -300)
        {
            delay = EARLY_BY_CLASS + DELAY_OUT_OF_SCOPE;
        }

        for (Sample sample : samples)
        {
            Instance instance = new Instance(NUMBER_OF_FEATURES);

            instance.setValue(distanceAttribute, sample.getDistance());
            instance.setValue();
            instance.setValue();
            instance.setValue();
            instance.setValue();
            instance.setValue();
            instance.setValue();
            instance.setValue();
            instance.setValue();
            instance.setValue();
            instance.setValue();

            instances.add(instance);
        }

        return instances;
    }

    private int getActualTimeOfArrival(final Sample lastSample)
    {
        Date actualTimeOfArrival = lastSample.getTime();

        DateFormat formatter = new SimpleDateFormat("HHmmss");
        String timeOfDay = formatter.format(actualTimeOfArrival);

        int hours = Integer.parseInt(timeOfDay.substring(0, 2));
        int minutes = Integer.parseInt(timeOfDay.substring(2, 4));
        int seconds = Integer.parseInt(timeOfDay.substring(4, 6));

        return hours * 3600 + minutes * 60 + seconds;
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
