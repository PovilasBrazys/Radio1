package lt.radio1.radio;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Html;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public class TitleService extends IntentService {

    private static final String TAG = TitleService.class.getName();

    public static final String ACTION_MyUpdate = "lt.radio1.radio.UPDATE";
    public static final String EXTRA_KEY_UPDATE = "EXTRA_UPDATE";

    private String title;
    boolean onBreak;

    public TitleService() {
        super("TitleService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        long endTime = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < endTime) {
            synchronized (this) {
                try {
                    getTitle();
                    wait(endTime - System.currentTimeMillis());
                    endTime = System.currentTimeMillis() + 5000;
                    if (onBreak) break;
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        onBreak = true;
        super.onDestroy();
    }

    public void getTitle() {
        String stringUrl = "http://95.154.254.83:5394/currentsong?sid=1";
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadWebpageTask().execute(stringUrl);
        } else {
            Intent intentUpdate = new Intent();
            intentUpdate.setAction(ACTION_MyUpdate);
            intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
            intentUpdate.putExtra(EXTRA_KEY_UPDATE, "No network connection available.");
            sendBroadcast(intentUpdate);
        }
    }

    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (!result.equals(title)) {
                title = result;
                Intent intentUpdate = new Intent();
                intentUpdate.setAction(ACTION_MyUpdate);
                intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
                intentUpdate.putExtra(EXTRA_KEY_UPDATE, result);
                sendBroadcast(intentUpdate);
            }
        }
    }

    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(TAG, "The response is: " + response);
            is = conn.getInputStream();

            String contentAsString = readIt(is);
            return contentAsString;

        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public String readIt(InputStream stream) throws IOException, UnsupportedEncodingException {
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