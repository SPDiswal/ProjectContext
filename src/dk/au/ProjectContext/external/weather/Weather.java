package dk.au.ProjectContext.external.weather;

public class Weather
{
    private String conditions;
    private int temperature;
    private int windSpeed;
    private int windDirection;
    private int precipitation;

    public Weather(final String conditions,
                   final int temperature,
                   final int windSpeed,
                   final int windDirection,
                   final int precipitation)
    {
        this.conditions = conditions;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.precipitation = precipitation;
    }

    public String getConditions()
    {
        return conditions;
    }

    public int getTemperature()
    {
        return temperature;
    }

    public int getWindSpeed()
    {
        return windSpeed;
    }

    public int getWindDirection()
    {
        return windDirection;
    }

    public int getPrecipitation()
    {
        return precipitation;
    }
}
