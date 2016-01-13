package lt.radio1.radio;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = MainActivity.class.getName();

    private ImageButton buttonPlay, buttonStopPlay;
    private ImageButton buttonVolumeUp, buttonVolumeDown;
    private ViewFlipper mViewFlipper;
    private ImageView imageWave;
    private TextView songTitle;
    private Toolbar mToolbar;
    private SeekBar mSeekBar;
    private static final int maxVolume = 100;
    private int playerVolume = 100;
    private float volume = 1f;
    private Animation rotate;
    private Intent titleService;
    private final GestureDetector detector = new GestureDetector(new SwipeGestureDetector());
    private Intent radioIntent;

    RadioStationService mService;
    boolean mBound = false;
    boolean isMusicService = false;

    private MyBroadcastReceiver_Update myBroadcastReceiver_Update;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        initializeUIElements();

        myBroadcastReceiver_Update = new MyBroadcastReceiver_Update();
        IntentFilter songTitleIntent = new IntentFilter(TitleService.ACTION_MyUpdate);
        songTitleIntent.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(myBroadcastReceiver_Update, songTitleIntent);

        titleService = new Intent(this, TitleService.class);
        startService(titleService);

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

        songTitle = (TextView) findViewById(R.id.song_title);

        imageWave = (ImageView) findViewById(R.id.wave_image);
        rotate = AnimationUtils.loadAnimation(this, R.anim.wobble);

        mViewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
        mViewFlipper.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                detector.onTouchEvent(event);
                return true;
            }
        });

        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mSeekBar.setMax(maxVolume);
        mSeekBar.setProgress(playerVolume);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                playerVolume = mSeekBar.getProgress() == 100 ? 99 : mSeekBar.getProgress();
                volume = (float) (1 - (Math.log(maxVolume - playerVolume) / Math.log(maxVolume)));
                if (mBound) {
                    mService.setPlayerVolume(volume);
                }
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
            case R.id.m_stop_play:
                stopPlaying();
                break;
            case R.id.m_start_play:
                startPlaying();
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

    class SwipeGestureDetector extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        boolean isFlipping = true;
        Handler handler = new Handler();
        boolean swipeToRight = false;

        Runnable run = new Runnable() {
            @Override
            public void run() {
                if (swipeToRight) {
                    mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_right));
                    mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_left));
                } else {
                    mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_left));
                    mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_right));
                }
                mViewFlipper.startFlipping();
                mViewFlipper.showNext();
                isFlipping = true;
            }
        };

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    return swipeAnim(false);
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    return swipeAnim(true);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        private boolean swipeAnim(boolean swipeToRight) {
            if (swipeToRight) {
                mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_left));
                mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_right));
                mViewFlipper.showPrevious();
            } else {
                mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_right));
                mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_left));
                mViewFlipper.showNext();
            }
            this.swipeToRight = swipeToRight;
            if (isFlipping) {
                mViewFlipper.stopFlipping();
                handler.postDelayed(run, 5000);
                isFlipping = false;
            } else {
                handler.removeCallbacks(run);
                handler.postDelayed(run, 5000);
            }
            return true;
        }

    }


    public class MyBroadcastReceiver_Update extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String update = intent.getStringExtra(TitleService.EXTRA_KEY_UPDATE);
            Log.d(TAG, "Update " + update);
            songTitle.setText(update);
        }
    }

    private void startPlaying() {
        buttonStopPlay.setVisibility(View.VISIBLE);
        buttonPlay.setVisibility(View.INVISIBLE);
        radioIntent = new Intent(this, RadioStationService.class);
        bindService(radioIntent, mConnection, Context.BIND_AUTO_CREATE);
        imageWave.startAnimation(rotate);
        isMusicService = true;
    }

    private void stopPlaying() {
        buttonPlay.setVisibility(View.VISIBLE);
        buttonStopPlay.setVisibility(View.INVISIBLE);
        imageWave.setAnimation(null);
        unbindService(mConnection);
        isMusicService = false;
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

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            RadioStationService.LocalBinder binder = (RadioStationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mSeekBar.setProgress(playerVolume);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(myBroadcastReceiver_Update);
        stopService(titleService);
        if (isMusicService) {
            unbindService(mConnection);
            stopService(radioIntent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
