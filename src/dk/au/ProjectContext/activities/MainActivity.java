package dk.au.ProjectContext.activities;

import android.app.Activity;
import android.content.Context;
import android.location.*;
import android.os.*;
import android.os.Environment;
import android.util.Log;
import android.view.*;
import android.widget.*;
import dk.au.ProjectContext.R;
import dk.au.ProjectContext.external.classifiers.ClassifierTask;
import dk.au.ProjectContext.external.rejseplanen.*;
import dk.au.ProjectContext.external.weather.*;
import dk.au.ProjectContext.location.StandardLocationListener;
import dk.au.ProjectContext.modelling.Modeller;
import weka.classifiers.Classifier;
import weka.core.*;
import weka.core.converters.*;

import java.io.*;
import java.util.*;

public class MainActivity extends Activity implements RoutesTask.RoutesTaskListener,
                                                      DeparturesTask.DeparturesTaskListener,
                                                      JourneyTask.JourneyTaskListener,
                                                      WeatherTask.WeatherTaskListener,
                                                      StandardLocationListener.LocationChangedListener,
                                                      Modeller.ModelFinishedListener,
                                                      ClassifierTask.ModelTaskListener
{
    private File outputPath = new File(Environment.getExternalStorageDirectory() + "/AU-ContextAwareness");

    private Weather weather;

    private Set<Route> routes;
    private Set<Stop> stops;

    private MenuItem locatingProgressAction;
    private MenuItem goAction;
    private MenuItem overrideNextAction;
    private boolean actionsAvailable = false;

    private int trafficLevel = 0;
    private long overrideNextStartTime;

    private Location currentLocation;
    private Journey currentJourney;
    private Route currentRoute;
    private Stop nextStop;

    private Stop nearbyStop;
    private int nextDepartureTime;
    private String nextDepartureDirection;

    private Modeller modeller;
    private Classifier classifier;

    public void retrieveRoutes(final List<Route> routes)
    {
        this.routes = new HashSet<Route>(routes);
        this.stops = new HashSet<Stop>();

        setTitle(getString(R.string.actionLocatingProgress));

        for (Route route : routes)
        {
            stops.addAll(route.getStops());
        }
    }

    public void retrieveDepartures(final List<Departure> departures)
    {
        for (Departure departure : departures)
        {
            if (nearbyStop != null && departure.getRoute().getStops().contains(nearbyStop))
            {
                String journeyId = departure.getJourneyId();
                new JourneyTask(this, departure.getRoute()).execute(journeyId);
                break;
            }
        }
    }

    public void retrieveJourney(final Journey journey)
    {
        final int nearbyStopDistanceThreshold = getResources().getInteger(R.integer.nearbyStopDistanceThreshold);

        if (nearbyStop != null)
        {
            this.modeller = new Modeller(this, journey, weather, nearbyStopDistanceThreshold);
            this.currentJourney = journey;
            this.nextStop = nearbyStop;
        }
    }

    public void retrieveWeather(final Weather weather)
    {
        this.weather = weather;
    }

    public void retrieveLocation(final Location location)
    {
        this.currentLocation = location;

        if (routes != null) updateNearbyStop();
        updateDisplay();

        if (isEnRoute()) modeller.takeSample(nextStop, currentLocation, trafficLevel);
    }

    public void retrieveInstances(final Stop stop, final Instances instances)
    {
        saveModel(stop, instances);
        goToNextStop();
    }

    public void retrieveClassifier(final Classifier classifier)
    {
        this.classifier = classifier;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        new WeatherTask(this).execute();
        new RoutesTask(this).execute();

        ensureOutputPath();
        enableTrafficLevelSpinner();
        listenForLocationChanges();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions, menu);

        locatingProgressAction = menu.findItem(R.id.locatingProgressAction);
        locatingProgressAction.setActionView(R.layout.progress_locating);

        goAction = menu.findItem(R.id.goAction);
        overrideNextAction = menu.findItem(R.id.overrideNextAction);

        actionsAvailable = true;

        showAction(locatingProgressAction);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.goAction:
                go();
                return true;

            case R.id.overrideNextAction:
                overrideNext();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAction(final MenuItem action)
    {
        if (actionsAvailable)
        {
            locatingProgressAction.setVisible(false);
            goAction.setVisible(false);
            overrideNextAction.setVisible(false);
            if (action != null) action.setVisible(true);
        }
    }

    private void updateNearbyStop()
    {
        Stop nearbyStopCandidate = getNearbyStop(currentLocation);

        if (nearbyStopCandidate != nearbyStop)
        {
            nearbyStop = nearbyStopCandidate;

            if (nearbyStop != null && !isEnRoute())
            {
                Date now = Calendar.getInstance().getTime();
                new DeparturesTask(new DeparturesTask.DeparturesTaskListener()
                {
                    public void retrieveDepartures(final List<Departure> departures)
                    {
                        for (Departure departure : departures)
                        {
                            if (departure.getRoute().getStops().contains(nearbyStop))
                            {
                                nextDepartureTime = departure.getTimeOfDay();
                                nextDepartureDirection = departure.getRoute().getName()
                                                         + " for " + departure.getRoute().getDirection();
                                break;
                            }
                        }
                    }
                }, routes).execute(nearbyStop, now);
            }
        }
    }

    private Stop getNearbyStop(final Location location)
    {
        final int nearbyStopDistanceThreshold = getResources().getInteger(R.integer.nearbyStopDistanceThreshold);

        Stop nearestStopSoFar = null;
        int smallestDistanceSoFar = Integer.MAX_VALUE;

        for (Stop stop : stops)
        {
            Location locationOfStop = stop.getLocation();
            int distanceToStop = (int) Math.ceil(location.distanceTo(locationOfStop));

            if (distanceToStop < smallestDistanceSoFar && distanceToStop < nearbyStopDistanceThreshold)
            {
                smallestDistanceSoFar = distanceToStop;
                nearestStopSoFar = stop;
            }
        }

        return nearestStopSoFar;
    }

    private void go()
    {
        currentRoute = getNearestRoute();

        Calendar fiveMinutesAgo = Calendar.getInstance();
        fiveMinutesAgo.add(Calendar.MINUTE, -5);

        new DeparturesTask(this, routes).execute(currentRoute.first(), fiveMinutesAgo.getTime());
        new ClassifierTask(this, getAssets()).execute(currentRoute.getId(), currentRoute.last().getId());
    }

    private Route getNearestRoute()
    {
        Route nearestRouteSoFar = null;
        int smallestDistanceSoFar = Integer.MAX_VALUE;

        for (Route route : routes)
        {
            Location locationOfFirstStop = route.first().getLocation();
            int distanceToFirstStop = (int) Math.ceil(currentLocation.distanceTo(locationOfFirstStop));

            if (distanceToFirstStop < smallestDistanceSoFar)
            {
                smallestDistanceSoFar = distanceToFirstStop;
                nearestRouteSoFar = route;
            }
        }

        return nearestRouteSoFar;
    }

    private void goToNextStop()
    {
        Stop nextStopCandidate = currentRoute.next(nextStop);

        if (nextStopCandidate != null)
        {
            nextStop = nextStopCandidate;
        }
        else
        {
            whenFinished();
        }
    }

    private void whenFinished()
    {
        currentJourney = null;
        new WeatherTask(this).execute();
    }

    private void saveModel(final Stop stop, final Instances instances)
    {
        String fileName = currentRoute.getId() + "-" + stop.getId() + ".arff";
        File file = new File(outputPath, fileName);

        Instances instancesToSave;

        if (isExternalStorageAvailable())
        {
            try
            {
                if (file.exists())
                {
                    ArffLoader loader = new ArffLoader();
                    loader.setFile(file);
                    instancesToSave = loader.getDataSet();

                    for (int i = 0; i < instances.numInstances(); i++)
                    {
                        instancesToSave.add(instances.instance(i));
                    }
                }
                else
                {
                    instancesToSave = instances;
                }

                ArffSaver saver = new ArffSaver();
                saver.setInstances(instancesToSave);
                saver.setFile(file);
                saver.writeBatch();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private boolean isExternalStorageAvailable()
    {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void updateDisplay()
    {
        if (isEnRoute())
        {
            Stop finalStop = currentRoute.last();

            int distanceToNextStop = currentRoute.distanceTo(currentLocation, nextStop);
            int distanceToFinalStop = currentRoute.distanceTo(currentLocation, nextStop)
                                      + currentRoute.distanceBetween(nextStop, finalStop);

            String route = currentRoute.getName() + " for " + currentRoute.getDirection();
            String nextStopName = nextStop.getName();

            ((TextView) findViewById(R.id.routeHeader)).setText(route);
            ((TextView) findViewById(R.id.nextStop)).setText(nextStopName);
            ((TextView) findViewById(R.id.distanceToNextStop)).setText(distanceToNextStop + " m");

            String finalStopName = finalStop.getName();
            int finalStopArrivalTimeSecondsSinceMidnight = currentJourney.getTimetable().get(finalStop);

            int finalStopArrivalTimeHours = (int) Math.floor(finalStopArrivalTimeSecondsSinceMidnight / 3600);
            int finalStopArrivalTimeMinutes = (int) Math.floor((finalStopArrivalTimeSecondsSinceMidnight % 3600) / 60);

            String finalStopArrivalTime = String.format("%02d", finalStopArrivalTimeHours)
                                          + ":" + String.format("%02d", finalStopArrivalTimeMinutes);
            String predictedTimeDelay = predictDelay(distanceToFinalStop, finalStopArrivalTimeSecondsSinceMidnight);

            ((TextView) findViewById(R.id.finalStopHeader)).setText(finalStopName);
            ((TextView) findViewById(R.id.finalStopArrivalTime)).setText(finalStopArrivalTime);
            ((TextView) findViewById(R.id.finalStopDelay)).setText(predictedTimeDelay);
            ((TextView) findViewById(R.id.distanceToFinalStop)).setText(distanceToFinalStop + " m");

            findViewById(R.id.enRouteContainer).setVisibility(View.VISIBLE);
            showAction(overrideNextAction);
        }
        else
        {
            findViewById(R.id.enRouteContainer).setVisibility(View.GONE);
        }

        if (nearbyStop != null)
        {
            setTitle(nearbyStop.getName());

            if (!isEnRoute())
            {
                int nextDepartureTimeHours = (int) Math.floor(nextDepartureTime / 3600);
                int nextDepartureTimeMinutes = (int) Math.floor((nextDepartureTime % 3600) / 60);

                String nextDepartureTimeFormatted = String.format("%02d", nextDepartureTimeHours)
                                                    + ":" + String.format("%02d", nextDepartureTimeMinutes);

                ((TextView) findViewById(R.id.nextDepartureTime)).setText(nextDepartureTimeFormatted);
                ((TextView) findViewById(R.id.nextDepartureDirection)).setText(nextDepartureDirection);
                findViewById(R.id.timetableContainer).setVisibility(View.VISIBLE);

                showAction(goAction);
            }
        }
        else
        {
            setTitle("");
            findViewById(R.id.timetableContainer).setVisibility(View.GONE);

            showAction(null);
        }
    }

    private String predictDelay(final int distance, final int scheduledArrivalTime)
    {
        if (modeller == null || classifier == null) return "on time";

        try
        {
            Date today = Calendar.getInstance().getTime();
            int timeOfDay = modeller.convertToSecondsSinceMidnight(today);

            Calendar c = Calendar.getInstance();
            c.setTime(today);
            int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

            Instances predictionRelation = modeller.createRelation("Prediction");
            Instance i = modeller.createInstance(distance, timeOfDay, dayOfWeek, scheduledArrivalTime, trafficLevel);
            i.setDataset(predictionRelation);

            double classIndex = classifier.classifyInstance(i);
            String predictedDelay = predictionRelation.classAttribute().value((int) classIndex);

            if (predictedDelay.startsWith("EarlyBy"))
            {
                return "early by " + predictedDelay.substring(7) + " seconds";
            }
            else if (predictedDelay.startsWith("LateBy"))
            {
                return "late by " + predictedDelay.substring(6) + " seconds";
            }
            else { return "on time"; }
        }
        catch (Exception e)
        {
            return "on time";
        }
    }

    private void overrideNext()
    {
        final int overrideNextTimeout = getResources().getInteger(R.integer.overrideNextTimeout);
        long elapsedOverrideNextTime = System.currentTimeMillis() - overrideNextStartTime;

        if (elapsedOverrideNextTime <= overrideNextTimeout)
        {
            modeller.goToNext(nextStop);
            overrideNextStartTime = 0;
        }
        else
        {
            Toast.makeText(this, getString(R.string.messageOverrideNextTapAgain), Toast.LENGTH_SHORT).show();
            overrideNextStartTime = System.currentTimeMillis();
        }
    }

    private void ensureOutputPath()
    {
        if (!outputPath.exists()) outputPath.mkdir();
    }

    private void enableTrafficLevelSpinner()
    {
        final int trafficLevelFactor = getResources().getInteger(R.integer.trafficLevelFactor);
        Spinner trafficLevelSpinner = (Spinner) findViewById(R.id.trafficLevel);

        trafficLevelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id)
            {
                trafficLevel = position * trafficLevelFactor;
            }

            public void onNothingSelected(final AdapterView<?> parent) { }
        });
    }

    private void listenForLocationChanges()
    {
        final int locationSampleTime = getResources().getInteger(R.integer.locationSampleTime);

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener listener = new StandardLocationListener(this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationSampleTime, 0, listener);
    }

    private boolean isEnRoute()
    {
        return currentJourney != null;
    }
}
