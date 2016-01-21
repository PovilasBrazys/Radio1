package lt.radio1.radio;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import static lt.radio1.radio.RadioStationService.ACTION_MyUpdate;
import static lt.radio1.radio.RadioStationService.EXTRA_KEY_UPDATE;

public class DownloadWebpageTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = DownloadWebpageTask.class.getName();
    private String title;

    Context context;

    public AsyncResponse delegate = null;

    DownloadWebpageTask(Context context){
        this.context = context;
    }

    @Override
    protected String doInBackground(Void... urls) {
        try {
            return downloadUrl("http://95.154.254.83:5394/currentsong?sid=1");
        } catch (IOException e) {
            return "Unable to retrieve web page. URL may be invalid.";
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result.equals("")) {
            title = result;
            Intent intentUpdate = new Intent();
            intentUpdate.setAction(ACTION_MyUpdate);
            intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
            intentUpdate.putExtra(EXTRA_KEY_UPDATE, "Radio1 is offline right now");
            context.sendBroadcast(intentUpdate);
        }
        if (!result.equals(title)) {
            Log.d(TAG, "The response is: " + 2);
            title = result;
            Intent intentUpdate = new Intent();
            intentUpdate.setAction(ACTION_MyUpdate);
            intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
            intentUpdate.putExtra(EXTRA_KEY_UPDATE, result);
            context.sendBroadcast(intentUpdate);
        }

        delegate.processFinish(result);
    }

    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream();
            String contentAsString = readIt(is);
            return contentAsString;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public String readIt(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        reader.close();
        return out.toString();
    }

}