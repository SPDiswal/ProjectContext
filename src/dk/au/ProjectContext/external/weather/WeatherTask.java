package dk.au.ProjectContext.external.weather;

import android.os.AsyncTask;
import dk.au.ProjectContext.utilities.JsonRequest;
import org.json.*;

public class WeatherTask extends AsyncTask<Void, Void, Weather>
{
    private static final String OPEN_WEATHER_MAP = "http://api.openweathermap.org/data/2.5/weather"
                                                   + "?q=Aarhus"
                                                   + "&units=metric"
                                                   + "&appid=2de143494c0b295cca9337e1e96b00e0";

    private WeatherTaskListener listener;

    public WeatherTask(final WeatherTaskListener listener)
    {
        this.listener = listener;
    }

    @Override
    protected Weather doInBackground(final Void... params)
    {
        try
        {
            JSONObject json = new JsonRequest(OPEN_WEATHER_MAP).get();
            if (json != null) return parseWeather(json);
        }
        catch (Exception ignored)
        {
        }

        throw new WeatherUnavailableException();
    }

    private Weather parseWeather(final JSONObject json) throws JSONException
    {
        String conditions = json.getJSONArray("weather").getJSONObject(0).getString("main");
        int temperature = json.getJSONObject("main").getInt("temp");
        int windSpeed = json.getJSONObject("wind").getInt("speed");
        int windDirection = json.getJSONObject("wind").getInt("deg");
        int precipitation = 0;

        if (json.has("rain"))
        {
            precipitation += (int) Math.ceil(json.getJSONObject("rain").getInt("3h"));
        }

        if (json.has("snow"))
        {
            precipitation += (int) Math.ceil(json.getJSONObject("snow").getInt("3h"));
        }

        return new Weather(conditions, temperature, windSpeed, windDirection, precipitation);
    }

    @Override
    protected void onPostExecute(final Weather weather)
    {
        super.onPostExecute(weather);
        listener.retrieveWeather(weather);
    }

    public interface WeatherTaskListener
    {
        void retrieveWeather(Weather weather);
    }
}
