package dk.au.ProjectContext.external.rejseplanen;

import android.os.AsyncTask;
import dk.au.ProjectContext.utilities.JsonRequest;
import org.json.*;

import java.util.*;

public class JourneyTask extends AsyncTask<String, Void, Journey>
{
    // Via our own proxy-service on DigitalOcean that transforms the XML from Rejseplanen to JSON.
    private static final String REJSEPLANEN_JOURNEYS = "http://178.62.241.8:3000/journeys";

    private final JourneyTaskListener listener;
    private final Route route;

    public JourneyTask(final JourneyTaskListener listener, final Route route)
    {
        this.listener = listener;
        this.route = route;
    }

    @Override
    protected Journey doInBackground(final String... params)
    {
        try
        {
            String journeyId = params[0];
            JSONObject json = new JsonRequest(REJSEPLANEN_JOURNEYS + "/" + journeyId).get();
            if (json != null) return parseJourney(json.getJSONArray("journey"));
        }
        catch (Exception ignored)
        {
        }

        throw new RejseplanenUnavailableException();
    }

    private Journey parseJourney(final JSONArray stopsArray) throws JSONException
    {
        List<Integer> arrivalTimes = new ArrayList<Integer>();

        for (int i = 0; i < stopsArray.length(); i++)
        {
            JSONObject stop = stopsArray.getJSONObject(i);
            String[] timeOfDayTokens = stop.getString("time").split(":");

            int hours = Integer.parseInt(timeOfDayTokens[0]);
            int minutes = Integer.parseInt(timeOfDayTokens[1]);
            int timeOfDay = hours * 3600 + minutes * 60;

            arrivalTimes.add(timeOfDay);
        }

        return new Journey(route, arrivalTimes);
    }

    @Override
    protected void onPostExecute(final Journey journey)
    {
        super.onPostExecute(journey);
        listener.retrieveJourney(journey);
    }

    public interface JourneyTaskListener
    {
        void retrieveJourney(Journey journey);
    }
}
