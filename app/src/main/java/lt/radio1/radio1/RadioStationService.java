package lt.radio1.radio1;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.IOException;

public class RadioStationService extends Service implements AsyncResponse, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener {

    public static final String ACTION_MyUpdate = "lt.radio1.radio.UPDATE";
    public static final String EXTRA_KEY_UPDATE = "EXTRA_UPDATE";

    private String songTitle = "";
    private boolean bind = false;

    private final IBinder mBinder = new LocalBinder();
    private final Handler handler = new Handler();

    private NotificationCompat.Builder mBuilder;
    private MediaPlayer mMediaPlayer;
    private DownloadWebpageTask title;

    @Override
    public void onCreate() {
        final WifiManager.WifiLock wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
        wifiLock.acquire();

        initializeMediaPlayer();
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_logo)
                .setContentTitle("Radio 1")
                .setContentText("musuique non stop")
                .setVisibility(Notification.VISIBILITY_PUBLIC);
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
            getTitle();
            handler.removeCallbacks(run);
            handler.postDelayed(run, 5000);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        float vol = intent.getExtras().getFloat("soundVol");
        mMediaPlayer.setVolume(vol, vol);
        mMediaPlayer.prepareAsync();
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mMediaPlayer.start();
            }
        });
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        return START_NOT_STICKY;
    }

    private void initializeMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mMediaPlayer.setDataSource("http://87.117.197.33:7971/");
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
        bind = true;
        handler.postDelayed(run, 0);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        bind = false;
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
        bind = true;
        handler.postDelayed(run, 0);
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mBuilder != null) {
            mBuilder = null;
        }
    }


    @Override
    public void processFinish(String result) {
        mBuilder.setContentText(result);
        if (result.equals("")) {
            setSongTitle("Radio1 is offline right now");
        } else if (!result.equals(songTitle)|| bind) {
            songTitle = result;
            setSongTitle(songTitle);
        }
    }

    private void setSongTitle(String result) {
        Intent intentUpdate = new Intent();
        intentUpdate.setAction(ACTION_MyUpdate);
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra(EXTRA_KEY_UPDATE, result);
        sendBroadcast(intentUpdate);
        startForeground(1, mBuilder.build());
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (!songTitle.equals("")) {
            Toast.makeText(getApplicationContext(), "Radio1 is offline right now", Toast.LENGTH_LONG);
        } else {
            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG);
        }
        return false;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Toast.makeText(getApplicationContext(), "Low performance", Toast.LENGTH_SHORT);
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

    public void getTitle() {
        if (isNetworkAvailable()) {
            title = new DownloadWebpageTask();
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