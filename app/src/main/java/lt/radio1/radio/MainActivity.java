package lt.radio1.radio;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getName();

    private ImageButton buttonPlay, buttonStopPlay;
    private ImageButton buttonVolumeUp, buttonVolumeDown;
    private ViewFlipper mViewFlipper;
    private ImageView imageWave, iconImage;
    private TextView songTitle;
    private IntentFilter songTitleIntent;
    private SeekBar mVolumeSeekBar;
    private Animation rotate;
    private Intent titleService;
    private final GestureDetector detector = new GestureDetector(new SwipeGestureDetector());
    private Intent intent;

    private RadioStationService mService;

    private boolean isPlaying = false;
    private boolean mBound = false;
    private float volume = 1f;

    private MyBroadcastReceiver_Update myBroadcastReceiver_Update;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initializeUIElements();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        myBroadcastReceiver_Update = new MyBroadcastReceiver_Update();
        songTitleIntent = new IntentFilter(TitleService.ACTION_MyUpdate);
        songTitleIntent.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(myBroadcastReceiver_Update, songTitleIntent);
        titleService = new Intent(this, TitleService.class);
        startService(titleService);


        intent = new Intent(this, RadioStationService.class);

    }

    private void initializeUIElements() {

        buttonPlay = (ImageButton) findViewById(R.id.m_start_play);
        buttonStopPlay = (ImageButton) findViewById(R.id.m_stop_play);
        buttonStopPlay.setOnClickListener(this);
        buttonPlay.setOnClickListener(this);

        buttonVolumeUp = (ImageButton) findViewById(R.id.volume_up);
        buttonVolumeDown = (ImageButton) findViewById(R.id.volume_down);
        buttonVolumeUp.setOnClickListener(this);
        buttonVolumeDown.setOnClickListener(this);
        buttonVolumeUp.setOnLongClickListener(this);
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

        mVolumeSeekBar = (SeekBar) findViewById(R.id.seekBar);
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        volume = sharedPref.getFloat("volume", 1f);
        isPlaying = sharedPref.getBoolean("isPlaying", false);
        if (isPlaying) {
            buttonPlay.setVisibility(View.INVISIBLE);
        } else {
            buttonStopPlay.setVisibility(View.INVISIBLE);
        }
        mVolumeSeekBar.setProgress(100);
        mVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volume = (float) (progress == 100 ? 99 : progress) / (float) 100;
                //volume = (float) (1 - (Math.log(1f - volume) / Math.log(1f)));
                if (mBound) {
                    mService.setMediaPlayerVol(volume);
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

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.m_start_play:
                if (!isPlaying) {
                    buttonStopPlay.setVisibility(View.VISIBLE);
                    buttonPlay.setVisibility(View.INVISIBLE);
                    imageWave.startAnimation(rotate);
                    intent.putExtra("soundVol", volume);
                    startService(intent);
                    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                    isPlaying = true;
                }
                break;
            case R.id.m_stop_play:
                if (isPlaying) {
                    buttonPlay.setVisibility(View.VISIBLE);
                    buttonStopPlay.setVisibility(View.INVISIBLE);
                    imageWave.setAnimation(null);
                    if (mBound) {
                        unbindService(mConnection);
                        mBound = false;
                    }
                    stopService(intent);
                    isPlaying = false;
                }
                break;
            case R.id.volume_up:
                if (volume != 0.99f) {
                    volume += (10 - (mVolumeSeekBar.getProgress() % 10)) / (float)100;
                    Log.e("TAG", "up " + volume);
                    mVolumeSeekBar.setProgress((int) (volume * 100));
                }
                break;
            case R.id.volume_down:
                if (volume != 0f) {
                    volume -= (mVolumeSeekBar.getProgress() % 10 == 0 ? 10 : mVolumeSeekBar.getProgress() % 10) / (float)100;
                    Log.e("TAG", "DOWN " + volume);
                    mVolumeSeekBar.setProgress((int) (volume * 100));
                }
                break;
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


    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.volume_up:
                mVolumeSeekBar.setProgress(100);
                break;
            case R.id.volume_down:
                mVolumeSeekBar.setProgress(0);
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
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (isPlaying) {
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
        mVolumeSeekBar.setProgress((int) (volume * 100));
        mViewFlipper.startFlipping();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mViewFlipper.stopFlipping();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
        }
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("isPlaying", isPlaying);
        editor.putFloat("volume", volume);
        editor.commit();
        stopService(titleService);
        unregisterReceiver(myBroadcastReceiver_Update);
    }

    class SwipeGestureDetector extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        boolean isFlipping = true;
        Handler handler = new Handler();
        boolean swipeToRight = false;

        public Runnable run = new Runnable() {
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

}
