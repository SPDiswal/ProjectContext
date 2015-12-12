package dk.au.ProjectContext.utilities;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.net.URI;

public class HttpRequest
{
    private String uri;

    public HttpRequest(final String uri)
    {
        this.uri = uri;
    }

    public InputStream get() throws IOException
    {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet request = new HttpGet();
        request.setURI(URI.create(uri));

        HttpResponse response = httpclient.execute(request);

        if (response.getStatusLine().getStatusCode() == 200)
        {
            HttpEntity entity = response.getEntity();
            return entity != null ? entity.getContent() : null;
        }

        return null;
    }
}
