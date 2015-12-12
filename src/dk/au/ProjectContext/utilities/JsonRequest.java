package dk.au.ProjectContext.utilities;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.*;

import java.io.*;
import java.net.URI;

public class JsonRequest
{
    private String uri;

    public JsonRequest(final String uri)
    {
        this.uri = uri;
    }

    public JSONObject get() throws Exception
    {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet request = new HttpGet();
        request.setURI(URI.create(uri));

        HttpResponse response = httpclient.execute(request);

        if (response.getStatusLine().getStatusCode() == 200)
        {
            HttpEntity entity = response.getEntity();
            return entity != null ? getJson(entity) : null;
        }

        return null;
    }

    private JSONObject getJson(final HttpEntity response) throws IOException, JSONException
    {
        String line;
        StringBuilder builder = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getContent(), "UTF-8"));

        while ((line = reader.readLine()) != null)
        {
            builder.append(line).append("\n");
        }

        reader.close();
        return new JSONObject(builder.toString());
    }
}
