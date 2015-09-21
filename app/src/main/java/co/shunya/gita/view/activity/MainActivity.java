package co.shunya.gita.view.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nineoldandroids.view.animation.AnimatorProxy;
import co.shunya.gita.BusProvider;
import co.shunya.gita.Constant;

import co.shunya.gita.R;
import co.shunya.gita.db.DB;
import co.shunya.gita.db.model.Track;
import co.shunya.gita.player.MusicService;
import co.shunya.gita.view.fragment.NavigationDrawerFragment;
import co.shunya.gita.view.fragment.PlaceholderFragment;
import co.shunya.gita.view.fragment.PlayerFragment;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.Locale;


public class MainActivity extends ActionBarActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, PlaceholderFragment.ListItemClickListener, PlayerFragment.UpdateMainScreen {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String SAVED_STATE_ACTION_BAR_HIDDEN = "saved_state_action_bar_hidden";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private SlidingUpPanelLayout mSlideUpPanelLayout;
    private DrawerLayout mDrawerLayout;
    private TextView mSlideUpTitleTextView, mSlideUpSbuTitleTextView;
    private ImageView mSlideUpPlayIndi;
    private PlayerFragment playerFragment;
    private ViewPager mPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Setting up up-slider
        setUpSlidingPanel();

        // Set up the drawer.
        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        BusProvider.getInstance().register(this);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, mDrawerLayout);

        SharedPreferences pf = PreferenceManager.getDefaultSharedPreferences(this);
//        mSlideUpPanelLayout.hidePanel();

        mPager = (ViewPager) findViewById(R.id.pagerContent);
        initPager(mNavigationDrawerFragment.getCurrentSelectedPosition());
        int albumCode = pf.getInt(Constant.PREF_ALBUM_CODE, 0);
        Log.i(TAG, "Album Code:" + albumCode);
        mPager.setCurrentItem(albumCode - 1);


        //Manage the state of action bar
        boolean actionBarHidden = savedInstanceState != null && savedInstanceState.getBoolean(SAVED_STATE_ACTION_BAR_HIDDEN, false);
        if (actionBarHidden) {
            int actionBarHeight = getActionBarHeight();
            setActionBarTranslation(-actionBarHeight); //will "hide" an ActionBar
        }
    }

    private void setUpSlidingPanel() {
        mSlideUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlideUpPanelLayout.setPanelSlideListener(paneSlideListener);
        mSlideUpPanelLayout.setEnableDragViewTouchEvents(true);
        if (!isMyServiceRunning(MusicService.class)) {
            mSlideUpPanelLayout.hidePanel();
            Toast.makeText(this, "Last Played: \n" + PreferenceManager.getDefaultSharedPreferences(this).getString(Constant.PREF_LAST_PLAY, ""), Toast.LENGTH_LONG).show();
        }
        Log.i(TAG, "I have been called : mSlideUpPanelLayout.hidePanel();");
        mSlideUpTitleTextView = (TextView) findViewById(R.id.txtSlideUpTitle);
        mSlideUpSbuTitleTextView = (TextView) findViewById(R.id.txtSlideUpSubTitle);
        mSlideUpPlayIndi = (ImageView) findViewById(R.id.imgPlayIndicator);

        FragmentManager fragmentManager = getSupportFragmentManager();
        playerFragment = PlayerFragment.newInstance();
        fragmentManager.beginTransaction().replace(R.id.container_palyer, playerFragment).commit();
    }

    private void initPager(int position) {
        SectionsPagerAdapter adapter;

        /*if (position == 0) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            ArrayAdapter<CharSequence> mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.array_list_album, R.layout.spinner_layout);
            mSpinnerAdapter.setDropDownViewResource(R.layout.spinner_layout);
            actionBar.setListNavigationCallbacks(mSpinnerAdapter, mOnNavigationListener);
            mPager.setOnPageChangeListener(pageChangeListener);
            adapter = new SectionsPagerAdapter(getSupportFragmentManager(), 2, position);
        } else {*/
            ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            adapter = new SectionsPagerAdapter(getSupportFragmentManager(), 1, position);
        /*}*/
        mPager.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }


    @Override
    public void onClick(Track track) {
        mSlideUpTitleTextView.setText(track.getTitle());
        mSlideUpSbuTitleTextView.setText(track.getLanguage());
        mSlideUpPanelLayout.showPanel();
        playerFragment.onClick(track);
    }

    @Override
    public void onClickStop() {
        mSlideUpPanelLayout.hidePanel();
    }

    @Override
    public void updateMainScreen(MusicService.AudioUpdateListener listener) {
        Log.i(TAG, "I have been called : updateMainScreen");

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt(Constant.PREF_LANG_CODE, listener.track.getLanguageCode());
        int albumCode = listener.track.getAlbum();
        Log.i(TAG, "Album Code:" + albumCode);
        editor.putInt(Constant.PREF_ALBUM_CODE, listener.track.getAlbum());
        editor.commit();

        mSlideUpTitleTextView.setText(listener.track.getTitle());
        mSlideUpSbuTitleTextView.setText(listener.track.getLanguage());
        switch (listener.state) {
            case Retrieving:
                break;
            case Stopped:
                mSlideUpPanelLayout.hidePanel();
                break;
            case Preparing:
                Animation rotation_anim = AnimationUtils.loadAnimation(this, R.anim.translate_rotate);
                mSlideUpPlayIndi.startAnimation(rotation_anim);
                break;
            case Playing:
                mSlideUpPlayIndi.clearAnimation();
                if (mSlideUpPanelLayout != null && !mSlideUpPanelLayout.isPanelExpanded() || !mSlideUpPanelLayout.isPanelAnchored()) {
                    mSlideUpPanelLayout.showPanel();
                    Log.i(TAG, "I have been called : updateMainScreen");
                }
                mDrawerLayout.closeDrawer(Gravity.LEFT);
                mSlideUpPlayIndi.setImageResource(R.drawable.ic_play);
                break;
            case Paused:
                mSlideUpPlayIndi.clearAnimation();
                if (mSlideUpPanelLayout != null && !mSlideUpPanelLayout.isPanelExpanded() || !mSlideUpPanelLayout.isPanelAnchored()) {
                    mSlideUpPanelLayout.showPanel();
                }
                mDrawerLayout.closeDrawer(Gravity.LEFT);
                mSlideUpPlayIndi.setImageResource(R.drawable.ic_pause);
                break;
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_STATE_ACTION_BAR_HIDDEN, mSlideUpPanelLayout.isPanelExpanded());
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
        } else if (mSlideUpPanelLayout != null && mSlideUpPanelLayout.isPanelExpanded() || mSlideUpPanelLayout.isPanelAnchored()) {
            mSlideUpPanelLayout.collapsePanel();
        } else {
            super.onBackPressed();
        }
    }

    private int getActionBarHeight() {
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        return actionBarHeight;
    }

    public void setActionBarTranslation(float y) {
        // Figure out the actionbar height
        int actionBarHeight = getActionBarHeight();
        // A hack to add the translation to the action bar
        ViewGroup content = ((ViewGroup) findViewById(android.R.id.content).getParent());
        int children = content.getChildCount();
        for (int i = 0; i < children; i++) {
            View child = content.getChildAt(i);
            if (child.getId() != android.R.id.content) {
                if (y <= -actionBarHeight) {
                    child.setVisibility(View.GONE);
                } else {
                    child.setVisibility(View.VISIBLE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        child.setTranslationY(y);
                    } else {
                        AnimatorProxy.wrap(child).setTranslationY(y);
                    }
                }
            }
        }
    }


    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Log.i("SectionsPagerAdapter", "Current LangId: " + position);
        if (mPager != null) {
            initPager(position);
        }
        // update the main content by replacing fragments
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        PlaceholderFragment placeholderFragment = PlaceholderFragment.newInstance(position, DB.ALBUM_GITA);
//        placeholderFragment.addListClickSubscriber(this);
//        fragmentManager.beginTransaction().replace(R.id.container, placeholderFragment).commit();
        /** Getting a reference to action bar of this activity */


    }

    ActionBar.OnNavigationListener mOnNavigationListener = new ActionBar.OnNavigationListener() {

        @Override
        public boolean onNavigationItemSelected(int position, long itemId) {
            mPager.setCurrentItem(position);
            return true;
        }
    };

    private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            getSupportActionBar().setSelectedNavigationItem(position);
        }
    };


    public void onSectionAttached(int number) {
        String[] array = getResources().getStringArray(R.array.array_list_item);
        mTitle = array[number];
//        if (number == 0) {
//            mTitle = "";
//        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void restoreActionBar() {

        ActionBar actionBar = getSupportActionBar();
        /*if (mNavigationDrawerFragment.getCurrentSelectedPosition() == 0) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        } else {*/
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
//        }
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
//            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private SlidingUpPanelLayout.PanelSlideListener paneSlideListener = new SlidingUpPanelLayout.SimplePanelSlideListener() {

        @Override
        public void onPanelSlide(View view, float v) {
            setActionBarTranslation(mSlideUpPanelLayout.getCurrentParalaxOffset());
        }

        @Override
        public void onPanelExpanded(View panel) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            super.onPanelExpanded(panel);
        }

        @Override
        public void onPanelCollapsed(View panel) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            super.onPanelCollapsed(panel);
        }
    };

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private final int mCount;
        private final int langIndex;

        public SectionsPagerAdapter(FragmentManager fm, int count, int langIndex) {
            super(fm);
            this.mCount = count;
            this.langIndex = langIndex;
            Log.i("SectionsPagerAdapter", "Current LangId: " + langIndex);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            PlaceholderFragment placeholderFragment;
            if (position == 0)
                placeholderFragment = PlaceholderFragment.newInstance(langIndex, DB.ALBUM_GITA);
            else
                placeholderFragment = PlaceholderFragment.newInstance(langIndex, DB.ALBUM_BHAJ);
            placeholderFragment.addListClickSubscriber(MainActivity.this);
            return placeholderFragment;
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return "Gita";
                case 1:
                    return "Bhaj Govindam";
            }
            return null;
        }

        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

    }
}
