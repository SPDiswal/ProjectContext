package dk.au.ProjectContext.activities;

import android.app.Activity;
import android.content.Context;
import android.location.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import dk.au.ProjectContext.R;
import dk.au.ProjectContext.external.rejseplanen.*;
import dk.au.ProjectContext.external.weather.*;
import dk.au.ProjectContext.location.StandardLocationListener;
import dk.au.ProjectContext.models.Modeller;
import weka.core.Instances;
import weka.core.converters.*;

import java.io.*;
import java.util.*;

public class MainActivity extends Activity implements RoutesTask.RoutesTaskListener,
                                                      DeparturesTask.DeparturesTaskListener,
                                                      WeatherTask.WeatherTaskListener,
                                                      StandardLocationListener.LocationChangedListener,
                                                      Modeller.ModelFinishedListener
{
    private File outputPath = new File(Environment.getExternalStorageDirectory() + "/AU-ContextAwareness");

    private Weather weather;

    private Set<Route> routes;
    private Set<Stop> stops;

    private Set<Journey> journeys;

    private MenuItem locatingProgressAction;
    private MenuItem goAction;
    private MenuItem overrideNextAction;
    private boolean actionsAvailable = false;

    private int trafficLevel = 0;
    private long overrideNextStartTime;

    private Location currentLocation;
    private Stop nearestStop;

    private Journey currentJourney;
    private Route currentRoute;
    private Stop nextStop;

    private Modeller modeller;

    public void retrieveRoutes(final List<Route> routes)
    {
        this.routes = new HashSet<Route>(routes);
        this.stops = new HashSet<Stop>();

        setTitle(getString(R.string.actionLocatingProgress));

        for (Route route : routes)
        {
            stops.addAll(route.getStops());

            Calendar fiveMinutesAgo = Calendar.getInstance();
            fiveMinutesAgo.add(Calendar.MINUTE, -5);

            new DeparturesTask(this, routes).execute(route.first(), fiveMinutesAgo.getTime());
        }
    }

    public void retrieveDepartures(final List<Departure> departures)
    {

    }

    public void retrieveWeather(final Weather weather)
    {
        this.weather = weather;
    }

    public void retrieveLocation(final Location location)
    {
        currentLocation = location;

        updateNearestStop();

        if (currentRoute != null)
        {
            updateDisplay();
            showAction(overrideNextAction);

            modeller.takeSample(nextStop, currentLocation, trafficLevel);
        }
        else
        {
            clearDisplay();
            showAction(goAction);
        }
    }

    public void retrieveModel(final Stop stop, final Instances instances)
    {
        saveModel(stop, instances);
        goToNextStop();
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
            action.setVisible(true);
        }
    }

    private void hideAllActions()
    {
        if (actionsAvailable)
        {
            locatingProgressAction.setVisible(false);
            goAction.setVisible(false);
            overrideNextAction.setVisible(false);
        }
    }

    private void updateNearestStop()
    {
        Stop nearestStop = getNearestStop(currentLocation);
        setTitle(nearestStop.getName());
    }

    private void go()
    {
        final int locationSampleTime = getResources().getInteger(R.integer.locationSampleTime);
        final int nearbyStopDistanceThreshold = getResources().getInteger(R.integer.nearbyStopDistanceThreshold);

        if (currentLocation != null)
        {
            currentRoute = getNearestRoute();
            modeller = new Modeller(this, currentRoute, locationSampleTime, nearbyStopDistanceThreshold);

            nextStop = currentRoute.first();

            updateDisplay();
            showAction(overrideNextAction);
        }
    }

    private Stop getNearestStop(final Location location)
    {
        Stop nearestStopSoFar = null;
        int smallestDistanceSoFar = Integer.MAX_VALUE;

        for (Stop stop : stops)
        {
            Location locationOfStop = stop.getLocation();
            int distanceToStop = (int) Math.ceil(location.distanceTo(locationOfStop));

            if (distanceToStop < smallestDistanceSoFar)
            {
                smallestDistanceSoFar = distanceToStop;
                nearestStopSoFar = stop;
            }
        }

        return nearestStopSoFar;
    }

    private Route getNearestRoute()
    {
        Route nearestRouteSoFar = routes.get(0);
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
            updateDisplay();
        }
        else
        {
            whenFinished();
        }
    }

    private void whenFinished()
    {
        currentRoute = null;
        new WeatherTask(this).execute();

        clearDisplay();
        showAction(goAction);
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

        //        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");
        //        Date today = Calendar.getInstance().getTime();
        //        String timeOfDay = formatter.format(today);
    }

    private boolean isExternalStorageAvailable()
    {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void updateDisplay()
    {
        if (currentRoute != null)
        {
            int distanceToNextStop = currentRoute.distanceTo(currentLocation, nextStop);
            int distanceToFinalStop = currentRoute.distanceTo(currentLocation, nextStop)
                                      + currentRoute.distanceBetween(nextStop, currentRoute.last());

            String route = currentRoute.getName() + " for " + currentRoute.getDirection();
            String finalStop = currentRoute.last().getName();

            setTitle(nextStop.getName());

            ((TextView) findViewById(R.id.distance)).setText(distanceToNextStop + " m");
            ((TextView) findViewById(R.id.route)).setText(route);
            ((TextView) findViewById(R.id.distanceToFinalStop)).setText(distanceToFinalStop + " m to " + finalStop);

            findViewById(R.id.distance).setVisibility(View.VISIBLE);
            findViewById(R.id.route).setVisibility(View.VISIBLE);
            findViewById(R.id.distanceToFinalStop).setVisibility(View.VISIBLE);
        }
    }

    private void clearDisplay()
    {
        setTitle(getString(R.string.ready));

        ((TextView) findViewById(R.id.distance)).setText("");
        ((TextView) findViewById(R.id.route)).setText("");
        ((TextView) findViewById(R.id.distanceToFinalStop)).setText("");

        findViewById(R.id.distance).setVisibility(View.GONE);
        findViewById(R.id.route).setVisibility(View.GONE);
        findViewById(R.id.distanceToFinalStop).setVisibility(View.GONE);
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
}
