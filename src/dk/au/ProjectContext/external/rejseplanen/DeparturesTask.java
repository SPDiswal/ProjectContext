package dk.au.ProjectContext.external.rejseplanen;

import android.os.AsyncTask;
import dk.au.ProjectContext.utilities.JsonRequest;
import org.json.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class DeparturesTask extends AsyncTask<Object, Void, List<Departure>>
{
    // Via our own proxy-service on DigitalOcean that transforms the XML from Rejseplanen to JSON.
    private static final String REJSEPLANEN_DEPARTURES = "http://178.62.241.8:3000/departures/";

    private final DeparturesTaskListener listener;
    private final Map<String, Route> routes;

    public DeparturesTask(final DeparturesTaskListener listener, final Set<Route> routes)
    {
        this.listener = listener;
        this.routes = new HashMap<String, Route>();

        for (Route route : routes)
        {
            this.routes.put(route.getId(), route);
        }
    }

    @Override
    protected List<Departure> doInBackground(final Object... params)
    {
        try
        {
            String firstStopId = ((Stop) params[0]).getId();
            String dateTime = new SimpleDateFormat("dd/MM/yy/HH/mm").format((Date) params[1]);

            JSONObject json = new JsonRequest(REJSEPLANEN_DEPARTURES + "/" + firstStopId + "/" + dateTime).get();
            if (json != null) return parseDepartures(json.getJSONArray("departures"));
        }
        catch (Exception ignored)
        {
        }

        throw new RejseplanenUnavailableException();
    }

    private List<Departure> parseDepartures(final JSONArray departuresArray) throws JSONException
    {
        List<Departure> result = new ArrayList<Departure>();

        for (int i = 0; i < departuresArray.length(); i++)
        {
            Departure departure = parseDeparture(departuresArray.getJSONObject(i));
            result.add(departure);
        }

        return result;
    }

    private Departure parseDeparture(final JSONObject departure) throws JSONException
    {
        String routeId = departure.getString("routeId");
        String[] timeOfDayTokens = departure.getString("time").split(":");
        String journeyId = departure.getString("journeyId");

        Route route = routes.get(routeId);

        int hours = Integer.parseInt(timeOfDayTokens[0]);
        int minutes = Integer.parseInt(timeOfDayTokens[1]);
        int timeOfDay = hours * 3600 + minutes * 60;

        return new Departure(route, timeOfDay, journeyId);
    }

    @Override
    protected void onPostExecute(final List<Departure> departures)
    {
        super.onPostExecute(departures);
        listener.retrieveDepartures(departures);
    }

    public interface DeparturesTaskListener
    {
        void retrieveDepartures(List<Departure> departures);
    }
}
