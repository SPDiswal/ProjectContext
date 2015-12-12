package dk.au.ProjectContext.modelling;

import android.location.Location;
import dk.au.ProjectContext.external.rejseplanen.*;
import dk.au.ProjectContext.external.weather.Weather;
import weka.core.*;

import java.text.*;
import java.util.*;

public class Modeller
{
    public static final String ON_TIME_CLASS = "OnTime";
    public static final String EARLY_BY_CLASS = "EarlyBy";
    public static final String LATE_BY_CLASS = "LateBy";
    public static final String DELAY_OUT_OF_SCOPE = "300Plus";
    public static final int NUMBER_OF_FEATURES = 11;

    private final ModelFinishedListener listener;

    private final Journey journey;
    private final Weather weather;
    private final Route route;

    private final int locationSampleTime;
    private final int nearbyStopDistanceThreshold;

    private final Map<Stop, List<Sample>> models = new HashMap<Stop, List<Sample>>();

    private FastVector features;
    private Attribute distanceAttribute;
    private Attribute timeOfDayAttribute;
    private Attribute dayOfWeekAttribute;
    private Attribute scheduledTimeOfArrivalAttribute;
    private Attribute weatherAttribute;
    private Attribute temperatureAttribute;
    private Attribute windSpeedAttribute;
    private Attribute windDirectionAttribute;
    private Attribute precipitationAttribute;
    private Attribute trafficAttribute;
    private Attribute predictedTimeDelayAttribute;

    public Modeller(final ModelFinishedListener listener,
                    final Journey journey,
                    final Weather weather,
                    final int locationSampleTime,
                    final int nearbyStopDistanceThreshold)
    {
        this.listener = listener;
        this.journey = journey;
        this.weather = weather;
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

        // Day of week: Numeric.
        dayOfWeekAttribute = new Attribute("dayOfWeek");
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
        FastVector delays = new FastVector(2 * 20 + 3);
        delays.addElement(ON_TIME_CLASS);

        for (int i = 15; i <= 300; i += 15)
        {
            delays.addElement(EARLY_BY_CLASS + i);
            delays.addElement(LATE_BY_CLASS + i);
        }

        delays.addElement(EARLY_BY_CLASS + DELAY_OUT_OF_SCOPE);
        delays.addElement(LATE_BY_CLASS + DELAY_OUT_OF_SCOPE);

        predictedTimeDelayAttribute = new Attribute("delay", delays);
        features.addElement(predictedTimeDelayAttribute);
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

        listener.retrieveInstances(stopToSkip, instances);
    }

    private Instances createInstancesFromSamples(final Stop stop)
    {
        int scheduledArrivalTime = journey.getTimetable().get(stop);
        List<Sample> samples = models.get(stop);

        Instances instances = createRelation("Schedule");

        Sample firstSample = samples.get(0);
        Sample lastSample = samples.get(samples.size() - 1);

        int actualArrivalTime = convertToSecondsSinceMidnight(lastSample.getTime());
        int timeDelay = actualArrivalTime - scheduledArrivalTime;

        Calendar c = Calendar.getInstance();
        c.setTime(firstSample.getTime());
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

        String delay = ON_TIME_CLASS;

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
            int timeOfDay = convertToSecondsSinceMidnight(sample.getTime());
            int traffic = sample.getTraffic();
            int distance = sample.getDistance();

            Instance instance = createInstance(distance, timeOfDay, dayOfWeek, scheduledArrivalTime, traffic);
            instance.setClassValue(/*predictedTimeDelayAttribute, */delay);

            instances.add(instance);
        }

        return instances;
    }

    public Instances createRelation(final String name)
    {
        Instances instances = new Instances(name, features, 0);
        instances.setClassIndex(instances.numAttributes() - 1);
        return instances;
    }

    public Instance createInstance(final int distance,
                                   final int timeOfDay,
                                   final int dayOfWeek,
                                   final int scheduledTimeOfArrival,
                                   final int trafficLevel)
    {
        Instance instance = new Instance(NUMBER_OF_FEATURES);

        instance.setValue(distanceAttribute, distance);
        instance.setValue(timeOfDayAttribute, timeOfDay);
        instance.setValue(dayOfWeekAttribute, dayOfWeek);
        instance.setValue(scheduledTimeOfArrivalAttribute, scheduledTimeOfArrival);
        instance.setValue(weatherAttribute, weather.getConditions());
        instance.setValue(temperatureAttribute, weather.getTemperature());
        instance.setValue(windSpeedAttribute, weather.getWindSpeed());
        instance.setValue(windDirectionAttribute, weather.getWindDirection());
        instance.setValue(precipitationAttribute, weather.getPrecipitation());
        instance.setValue(trafficAttribute, trafficLevel);
        instance.setClassMissing();

        return instance;
    }

    public int convertToSecondsSinceMidnight(final Date time)
    {
        DateFormat formatter = new SimpleDateFormat("HHmmss");
        String timeOfDay = formatter.format(time);

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
        void retrieveInstances(Stop stop, Instances instances);
    }
}
