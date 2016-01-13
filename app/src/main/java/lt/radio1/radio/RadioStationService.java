package lt.radio1.radio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.os.Process;

import java.io.IOException;

public class RadioStationService extends Service {

    private MediaPlayer player;

    private static final String TAG = "MYSERVICE";

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private final IBinder mBinder = new LocalBinder();
    private float volume;

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public class LocalBinder extends Binder {
        RadioStationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return RadioStationService.this;
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            startPlaying();/*
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getApplicationContext())
                            .setSmallIcon(R.drawable.logo)
                            .setContentTitle("My notification")
                            .setContentText("Hello World!");
// Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);

// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
// Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
            startForeground(1,mBuilder.build());*/

        }
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        Log.d("TAG", "Create");

        initializeMediaPlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TAG", "Start");
        return START_NOT_STICKY;
    }

    private int what;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("TAG", "Bind");

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = 1;
        what = msg.what;
        mServiceHandler.sendMessage(msg);

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopPlaying();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mServiceHandler.removeMessages(what);
        player = null;
    }

    @Override
    public void onRebind(Intent intent) {

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = 1;
        what = msg.what;
        mServiceHandler.sendMessage(msg);

        super.onRebind(intent);
    }

    private void startPlaying() {
        player.prepareAsync();
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                if (player != null) {
                    player.start();
                }
            }
        });
    }

    public void stopPlaying() {
        if (player.isPlaying()) {
            player.stop();
            player.reset();
            player.release();
        }
    }

    public void setPlayerVolume(float volume) {
        player.setVolume(volume, volume);
    }

    private void initializeMediaPlayer() {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            player.setDataSource("http://95.154.254.83:5394/");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Log.d("LAG", "test");
            }
        });

    }

}
