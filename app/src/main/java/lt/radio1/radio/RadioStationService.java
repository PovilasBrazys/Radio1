package lt.radio1.radio;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;

public class RadioStationService extends Service implements AsyncResponse {

    private final String tag = getClass().getName();
    private final IBinder mBinder = new LocalBinder();

    private WifiManager.WifiLock wifiLock;
    private NotificationCompat.Builder mBuilder;

    private MediaPlayer mMediaPlayer;

    public static final String ACTION_MyUpdate = "lt.radio1.radio.UPDATE";
    public static final String EXTRA_KEY_UPDATE = "EXTRA_UPDATE";

    Handler handler = new Handler();

    @Override
    public void onCreate() {
        Log.d(tag, "onCreate");

        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
        wifiLock.acquire();

        initializeMediaPlayer();
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Radio 1")
                .setContentText("musuique non stop")
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        /*.addAction(R.drawable.ic_pause_white_48dp, "stop", null)*/
        Intent notifyIntent =
                new Intent(this, MainActivity.class);

        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT
                );

        mBuilder.setContentIntent(notifyPendingIntent);
        startForeground(1, mBuilder.build());

    }

    public Runnable run = new Runnable() {
        @Override
        public void run() {
            Log.d(tag, "run");
            getTitle();
            handler.removeCallbacks(run);
            handler.postDelayed(run, 5000);
            Log.d(tag, "run1");
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(tag, "onStartCommand");
        float vol = intent.getExtras().getFloat("soundVol");
        mMediaPlayer.setVolume(vol, vol);
        mMediaPlayer.prepareAsync();
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mMediaPlayer.start();
            }
        });
        return START_NOT_STICKY;
    }

    private void initializeMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mMediaPlayer.setDataSource("http://95.154.254.83:5394/");

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(tag, "onBind");
        handler.postDelayed(run, 0);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(tag, "onUnbind");
        Intent intentUpdate = new Intent();
        intentUpdate.setAction(ACTION_MyUpdate);
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra(EXTRA_KEY_UPDATE, "Play To Start");
        sendBroadcast(intentUpdate);
        handler.removeCallbacks(run);
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(tag, "onRebind");
        handler.postDelayed(run, 0);
    }

    @Override
    public void onDestroy() {
        Log.d(tag, "onDestroy");
        mMediaPlayer.release();
        mMediaPlayer = null;
        wifiLock.release();
        wifiLock = null;
        handler = null;
        mBuilder = null;
    }

    @Override
    public void processFinish(String output) {
        mBuilder.setContentText(output);
        startForeground(1, mBuilder.build());
    }

    public class LocalBinder extends Binder {
        RadioStationService getService() {
            return RadioStationService.this;
        }
    }

    public void setMediaPlayerVol(float vol) {
        mMediaPlayer.setVolume(vol, vol);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    DownloadWebpageTask title;

    public void getTitle() {
        if (isNetworkAvailable()) {
            title = new DownloadWebpageTask(getApplicationContext());
            title.delegate = this;
            title.execute();
        } else {
            Intent intentUpdate = new Intent();
            intentUpdate.setAction(ACTION_MyUpdate);
            intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
            intentUpdate.putExtra(EXTRA_KEY_UPDATE, "No network connection available.");
            sendBroadcast(intentUpdate);
        }
    }

}