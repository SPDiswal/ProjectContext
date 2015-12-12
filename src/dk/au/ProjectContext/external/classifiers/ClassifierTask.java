package dk.au.ProjectContext.external.classifiers;

import android.os.AsyncTask;
import dk.au.ProjectContext.utilities.HttpRequest;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;

import java.io.*;

public class ClassifierTask extends AsyncTask<String, Void, Classifier>
{
    // Via our own service on DigitalOcean.
    private static final String MODEL_SERVICE = "http://178.62.241.8:3000/models";

    private final ModelTaskListener listener;

    public ClassifierTask(final ModelTaskListener listener)
    {
        this.listener = listener;
    }

    @Override
    protected Classifier doInBackground(final String... params)
    {
        try
        {
            String routeId = params[0];
            String stopId = params[1];

            InputStream response = new HttpRequest(MODEL_SERVICE + "/" + routeId + "/" + stopId).get();
            if (response != null) return parseClassifier(response);
        }
        catch (Exception ignored)
        {
        }

        return null;
    }

    private Classifier parseClassifier(final InputStream inputStream) throws IOException, ClassNotFoundException
    {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        Classifier classifier = (Classifier) objectInputStream.readObject();
        objectInputStream.close();
        return classifier;
    }

    @Override
    protected void onPostExecute(final Classifier classifier)
    {
        super.onPostExecute(classifier);
        listener.retrieveClassifier(classifier);
    }

    public interface ModelTaskListener
    {
        void retrieveClassifier(Classifier classifier);
    }
}
