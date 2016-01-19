package lt.radio1.radio;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class RadioStationService extends Service {

    private final String tag = getClass().getName();
    private final IBinder mBinder = new LocalBinder();

    private MediaPlayer mMediaPlayer = null;
    private WifiManager.WifiLock wifiLock;
    private NotificationCompat.Builder mBuilder;

    @Override
    public void onCreate() {
        Log.d(tag, "onCreate");

        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
        wifiLock.acquire();
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_play_arrow_white_48dp)
                .setContentTitle("Event tracker")
                .setContentText("Events received");

        startForeground(1, mBuilder.build());
        initializeMediaPlayer();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(tag, "onStartCommand");
        setMediaPlayerVol(intent.getExtras().getFloat("soundVol"));
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
        //Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(tag, "onUnbind");
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(tag, "onRebind");
    }

    @Override
    public void onDestroy() {
        Log.d(tag, "onDestroy");
        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        mMediaPlayer.release();
        mMediaPlayer = null;
        wifiLock.release();
        wifiLock = null;
        mBuilder = null;
    }

    public class LocalBinder extends Binder {
        RadioStationService getService() {
            return RadioStationService.this;
        }
    }

    public void setMediaPlayerVol(float vol) {
        mMediaPlayer.setVolume(vol, vol);
    }

}