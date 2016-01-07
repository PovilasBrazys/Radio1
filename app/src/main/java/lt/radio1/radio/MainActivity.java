package lt.radio1.radio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.ViewFlipper;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener  {

    private ImageButton buttonPlay, buttonStopPlay;
    private ImageButton buttonVolumeUp, buttonVolumeDown;
    private ViewFlipper mViewFlipper;
    private ImageView imageWave;
//    private MediaPlayer player;
    private Toolbar mToolbar;
    private SeekBar mSeekBar;
    private static final int maxVolume = 100;
    private int playerVolume = 100;
    private float volume;
    private Animation rotate;

    RadioStationService mService;
    boolean mBound = false;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            RadioStationService.LocalBinder binder = (RadioStationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        initializeUIElements();

        //initializeMediaPlayer();


    }

    private void initializeUIElements() {

        buttonPlay = (ImageButton) findViewById(R.id.m_start_play);
        buttonPlay.setOnClickListener(this);

        buttonStopPlay = (ImageButton) findViewById(R.id.m_stop_play);
        buttonStopPlay.setVisibility(View.INVISIBLE);
        buttonStopPlay.setOnClickListener(this);

        buttonVolumeUp = (ImageButton) findViewById(R.id.volume_up);
        buttonVolumeUp.setOnClickListener(this);
        buttonVolumeUp.setOnLongClickListener(this);
        buttonVolumeDown = (ImageButton) findViewById(R.id.volume_down);
        buttonVolumeDown.setOnClickListener(this);
        buttonVolumeDown.setOnLongClickListener(this);

        imageWave = (ImageView) findViewById(R.id.wave_image);
        rotate = AnimationUtils.loadAnimation(this, R.anim.wobble);

        mViewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);

        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mSeekBar.setMax(maxVolume);
        mSeekBar.setProgress(playerVolume);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                playerVolume = mSeekBar.getProgress() == 100 ? 99 : mSeekBar.getProgress();
                volume = (float) (1 - (Math.log(maxVolume - playerVolume) / Math.log(maxVolume)));
                mService.setPlayerVolume(volume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.m_start_play:
                startPlaying();
                break;
            case R.id.m_stop_play:
                stopPlaying();
                break;
            case R.id.volume_up:
                if (playerVolume != 100) {
                    playerVolume += 10 - (mSeekBar.getProgress() % 10);
                    mSeekBar.setProgress(playerVolume);
                }
                break;
            case R.id.volume_down:
                if (playerVolume != 0) {
                    playerVolume -= mSeekBar.getProgress() % 10 == 0 ? 10 : mSeekBar.getProgress() % 10;
                    mSeekBar.setProgress(playerVolume);
                }
                break;
        }
    }

    private void startPlaying() {
        buttonStopPlay.setVisibility(View.VISIBLE);
        buttonPlay.setVisibility(View.INVISIBLE);


        // Bind to LocalService
        Intent intent = new Intent(this, RadioStationService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        imageWave.startAnimation(rotate);
        /*volume = (float) (1 - (Math.log(maxVolume - playerVolume) / Math.log(maxVolume)));

        player.setVolume(volume, volume);
        player.prepareAsync();

        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                player.start();
            }
        });*/

    }

    /*private void initializeMediaPlayer() {
        player = new MediaPlayer();
        player.setOnInfoListener(this);
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

    }*/

    private void stopPlaying() {
        /*if (player.isPlaying()) {
            player.stop();
            player.release();
            initializeMediaPlayer();
        }*/
        unbindService(mConnection);

        buttonPlay.setVisibility(View.VISIBLE);
        buttonStopPlay.setVisibility(View.INVISIBLE);

        imageWave.setAnimation(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stopPlaying();
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.volume_up:
                mSeekBar.setProgress(100);
                break;
            case R.id.volume_down:
                mSeekBar.setProgress(0);
                break;
        }
        return true;
    }

}
