package dk.au.ProjectContext.external.rejseplanen;

import android.os.AsyncTask;
import dk.au.ProjectContext.utilities.JsonRequest;
import org.json.*;

import java.util.*;

public class RoutesTask extends AsyncTask<Void, Void, List<Route>>
{
    // Via our own proxy-service on DigitalOcean that transforms the XML from Rejseplanen to JSON.
    private static final String REJSEPLANEN_ROUTES = "http://178.62.241.8:3000/routes";

    private RoutesTaskListener listener;

    public RoutesTask(final RoutesTaskListener listener)
    {
        this.listener = listener;
    }

    @Override
    protected List<Route> doInBackground(final Void... params)
    {
        try
        {
            JSONObject json = new JsonRequest(REJSEPLANEN_ROUTES).get();
            if (json != null) return parseRoutes(json.getJSONObject("routes"));
        }
        catch (Exception ignored)
        {
        }

        throw new RejseplanenUnavailableException();
    }

    private List<Route> parseRoutes(final JSONObject routes) throws JSONException
    {
        List<Route> result = new ArrayList<Route>();

        for (Iterator iterator = routes.keys(); iterator.hasNext(); )
        {
            String id = (String) iterator.next();
            JSONObject route = routes.getJSONObject(id);

            String name = route.getString("name");
            String direction = route.getString("direction");
            JSONArray stopsArray = route.getJSONArray("stops");
            List<Stop> stops = new ArrayList<Stop>();

            for (int i = 0; i < stopsArray.length(); i++)
            {
                Stop stop = parseStop(stopsArray.getJSONObject(i));
                stops.add(stop);
            }

            result.add(new Route(id, name, direction, stops));
        }

        return result;
    }

    private Stop parseStop(final JSONObject json) throws JSONException
    {
        String id = json.getString("id");
        String name = json.getString("name");
        double latitude = json.getDouble("latitude");
        double longitude = json.getDouble("longitude");

        return new Stop(id, name, latitude, longitude);
    }

    @Override
    protected void onPostExecute(final List<Route> routes)
    {
        super.onPostExecute(routes);
        listener.retrieveRoutes(routes);
    }

    public interface RoutesTaskListener
    {
        void retrieveRoutes(List<Route> routes);
    }
}
