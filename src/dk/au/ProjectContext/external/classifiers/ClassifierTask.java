package dk.au.ProjectContext.external.classifiers;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;
import dk.au.ProjectContext.utilities.HttpRequest;
import weka.classifiers.Classifier;

import java.io.*;

public class ClassifierTask extends AsyncTask<String, Void, Classifier>
{
    // Via our own service on DigitalOcean.
    private static final String MODEL_SERVICE = "http://178.62.241.8:3000/classifiers";

    private final ModelTaskListener listener;
    private final AssetManager assetManager;

    public ClassifierTask(final ModelTaskListener listener, final AssetManager assetManager)
    {
        this.listener = listener;
        this.assetManager = assetManager;
    }

    @Override
    protected Classifier doInBackground(final String... params)
    {
        try
        {
            String routeId = params[0];
            String stopId = params[1];

            InputStream response = assetManager.open(routeId + "-" + stopId + ".model");
//            InputStream response = new HttpRequest(MODEL_SERVICE + "/" + routeId + "/" + stopId).get();
            if (response != null) return parseClassifier(response);
        }
        catch (Exception ignored)
        {
        }

        Log.d("ClassifierTask", "Classifier failed to load");

        return null;
    }

    private Classifier parseClassifier(final InputStream inputStream) throws IOException, ClassNotFoundException
    {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        Classifier classifier = (Classifier) objectInputStream.readObject();
        objectInputStream.close();

        Log.d("ClassifierTask", "Classifier loaded successfully");

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
