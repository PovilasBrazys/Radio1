package lt.radio1.radio;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.util.List;

import static lt.radio1.radio.RadioStationService.ACTION_MyUpdate;
import static lt.radio1.radio.RadioStationService.EXTRA_KEY_UPDATE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getName();

    private ImageButton buttonPlay, buttonStopPlay;
    private ImageButton buttonVolumeUp, buttonVolumeDown;
    private ViewFlipper mViewFlipper;
    private ImageView imageWave;
    private TextView songTitle;
    private IntentFilter songTitleIntent;
    private SeekBar mVolumeSeekBar;
    //private Animation rotate;
    private final GestureDetector detector = new GestureDetector(new SwipeGestureDetector());
    private Intent intent;

    private RadioStationService mService;

    private boolean isPlaying = false;
    private boolean mBound = false;
    private float volume = 1f;

    int[] ints = new int[] {R.drawable.slide01, R.drawable.slide02, R.drawable.slide03};
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
        songTitleIntent = new IntentFilter(ACTION_MyUpdate);
        songTitleIntent.addCategory(Intent.CATEGORY_DEFAULT);
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
        //rotate = AnimationUtils.loadAnimation(this, R.anim.wobble);

        mViewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
        mViewFlipper.setFlipInterval(5000);
        mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_right));
        mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_left));
        ImageView imageView;
        Resources res = getResources();
        for (int i = 0; i < 3; i++){
            imageView = new ImageView(this);
            imageView.setImageDrawable(res.getDrawable(ints[i]));
            mViewFlipper.addView(imageView);
        }
        mViewFlipper.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                detector.onTouchEvent(event);
                return true;
            }
        });

        mVolumeSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volume = (float) (progress == 100 ? 99 : progress) / (float) 100;
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

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.m_start_play:
                if (isNetworkAvailable()) {
                    if (!isPlaying) {
                        buttonStopPlay.setVisibility(View.VISIBLE);
                        buttonPlay.setVisibility(View.INVISIBLE);
                        //imageWave.startAnimation(rotate);
                        intent.putExtra("soundVol", volume);
                        startService(intent);
                        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                        isPlaying = true;
                    }
                } else {
                    Toast.makeText(this, "Connect to Internet", Toast.LENGTH_SHORT).show();
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
                    SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
                    editor.putBoolean("isPlaying", isPlaying);
                    editor.putFloat("volume", volume);
                    editor.commit();
                }
                break;
            case R.id.volume_up:
                if (volume != 0.99f) {
                    volume += (10 - (mVolumeSeekBar.getProgress() % 10)) / (float) 100;
                    Log.e("TAG", "up " + volume);
                    mVolumeSeekBar.setProgress((int) (volume * 100));
                }
                break;
            case R.id.volume_down:
                if (volume != 0f) {
                    volume -= (mVolumeSeekBar.getProgress() % 10 == 0 ? 10 : mVolumeSeekBar.getProgress() % 10) / (float) 100;
                    Log.e("TAG", "DOWN " + volume);
                    mVolumeSeekBar.setProgress((int) (volume * 100));
                }
                break;
        }
    }

    public class MyBroadcastReceiver_Update extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String update = intent.getStringExtra(EXTRA_KEY_UPDATE);
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

    @Override
    protected void onStart() {
        super.onStart();
        //new DownloadWebpageTask(this);
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        volume = sharedPref.getFloat("volume", 1f);
        isPlaying = sharedPref.getBoolean("isPlaying", false);
        mVolumeSeekBar.setProgress(100);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(myBroadcastReceiver_Update, songTitleIntent);
        if (isPlaying) {
            //imageWave.startAnimation(rotate);
            buttonPlay.setVisibility(View.INVISIBLE);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } else {
            buttonStopPlay.setVisibility(View.INVISIBLE);
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
        unregisterReceiver(myBroadcastReceiver_Update);
        if (mBound) {
            unbindService(mConnection);
        }
        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
        editor.putBoolean("isPlaying", isPlaying);
        editor.putFloat("volume", volume);
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // getMenuInflater().inflate(R.menu.main, menu);
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

        if (id == R.id.nav_facebook) {
            launchFacebook();
        } else if (id == R.id.nav_twitter) {
            startTwitter();
        } else if (id == R.id.nav_pinterest) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("pinterest://www.pinterest.com/radio1lithuania")));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pinterest.com/radio1lithuania")));
            }
        } else if (id == R.id.nav_linkedin) {
            Intent intent = null;
            try {
                getPackageManager().getPackageInfo("com.linkedin.android", 0);
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("linkedin://profile/in/radio1-lithuania-9a5618111"));
            } catch (Exception e) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/radio1-lithuania-9a5618111"));
            } finally {
                startActivity(intent);
            }
        } else if (id == R.id.nav_contact) {
            Intent intent = new Intent(this, ContactsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_menu_radio1_website) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.radio1.lt/"));
            startActivity(intent);
        } else if (id == R.id.nav_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "http://www.radio1.lt/");
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return false;
    }

    public final void launchFacebook() {
        final String urlFb = "fb://page/" + "1607910642807572";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(urlFb));

        final PackageManager packageManager = getPackageManager();
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() == 0) {
            final String urlBrowser = "https://www.facebook.com/radiohit1";
            intent.setData(Uri.parse(urlBrowser));
        }

        startActivity(intent);
    }

    public void startTwitter() {
        Intent intent = null;
        try {
            this.getPackageManager().getPackageInfo("com.twitter.android", 0);
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?user_id=4654618720"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } catch (Exception e) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/Radio1Lithuania"));
        }
        this.startActivity(intent);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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
