package dk.au.ProjectContext.utilities;

import org.json.*;

import java.io.*;

public class JsonRequest
{
    private String uri;

    public JsonRequest(final String uri)
    {
        this.uri = uri;
    }

    public JSONObject get() throws Exception
    {
        InputStream response = new HttpRequest(uri).get();
        return response != null ? getJson(response) : null;
    }

    private JSONObject getJson(final InputStream contents) throws IOException, JSONException
    {
        String line;
        StringBuilder builder = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(contents, "UTF-8"));

        while ((line = reader.readLine()) != null)
        {
            builder.append(line).append("\n");
        }

        reader.close();
        return new JSONObject(builder.toString());
    }
}
