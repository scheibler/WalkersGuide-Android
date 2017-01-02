package org.walkersguide.android;

import org.walkersguide.android.interfaces.FragmentCommunicator;
import org.walkersguide.android.ui.AbstractActivity;
import org.walkersguide.android.ui.POIFragment;
import org.walkersguide.android.ui.RouterFragment;
import org.walkersguide.android.ui.SettingsActivity;
import org.walkersguide.android.utils.SettingsManager.GeneralSettings;
import org.walkersguide.android.utils.TTSWrapper;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends AbstractActivity {

	// communicate with attached fragments
	public FragmentCommunicator routerFragmentCommunicator;
	public FragmentCommunicator poiFragmentCommunicator;

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the three primary sections of the app. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	private AppSectionsPagerAdapter mAppSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will display the three primary sections of the
	 * app, one at a time.
	 */
	private ViewPager mViewPager;

	// fragment handler
	private Handler onFragmentDisabledHandler;
	private Handler onFragmentEnabledHandler;

    // tts
    private TTSWrapper ttsWrapperInstance;
    private boolean switchFragmentGestureDetected;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // ttsWrapper instance
        ttsWrapperInstance = TTSWrapper.getInstance(this);

		// Create the adapter that will return a fragment for each of the
		// primary sections
		// of the app.
		mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(this);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(
                getResources().getString(R.string.app_name));

		// Set up the ViewPager, attaching the adapter and setting up a listener
		// for when the
		// user swipes between sections.
		mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                GeneralSettings generalSettings = settingsManagerInstance.getGeneralSettings();
    			if (generalSettings.getRecentOpenTab() != position) {
                    leaveActiveFragment();
                    // announce if switched by gesture
                    if (switchFragmentGestureDetected) {
                        ttsWrapperInstance.speak(
                                mAppSectionsPagerAdapter.getPageTitle(position).toString(), true, true);
                    }
                    switchFragmentGestureDetected = true;
                    // switch fragment
    				generalSettings.setRecentOpenTab(position);
                    enterActiveFragment();
                }
            }
        });

		// initialize handlers for disabling and enabling fragments
		onFragmentDisabledHandler = new Handler();
		onFragmentEnabledHandler = new Handler();

        // open the recent tab
        switchFragmentGestureDetected = true;
        mViewPager.setCurrentItem(
                settingsManagerInstance.getGeneralSettings().getRecentOpenTab());
    }

	@Override public void onPause() {
		super.onPause();
		leaveActiveFragment();
	}

    @Override public void onResume() {
        super.onResume();
        enterActiveFragment();
    }


    /**
     * toolbar menu
     */

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity_main, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemRouterTab:
                switchFragmentGestureDetected = false;
                mViewPager.setCurrentItem(0);
                break;
            case R.id.menuItemPOITab:
                switchFragmentGestureDetected = false;
                mViewPager.setCurrentItem(1);
                break;
            case R.id.menuItemSettings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * fragment management
     */

	private void leaveActiveFragment() {
		onFragmentDisabledHandler.postDelayed(new OnFragmentDisabledUpdater(
				settingsManagerInstance.getGeneralSettings().getRecentOpenTab()), 0);
	}

	private class OnFragmentDisabledUpdater implements Runnable {
		private static final int NUMBER_OF_RETRIES = 5;
		private int counter;
		private int currentFragment;

		public OnFragmentDisabledUpdater(int currentFragment) {
			this.counter = 0;
			this.currentFragment = currentFragment;
		}

        @Override public void run() {
            switch (currentFragment) {
                case 0:
                    if (routerFragmentCommunicator != null) {
                        routerFragmentCommunicator.onFragmentDisabled();
                        return;
                    }
                    break;
                case 1:
                    if (poiFragmentCommunicator != null) {
                        poiFragmentCommunicator.onFragmentDisabled();
                        return;
                    }
                    break;
                default:
                    return;
            }
            if (counter < NUMBER_OF_RETRIES) {
                counter += 1;
                onFragmentDisabledHandler.postDelayed(this, 100);
            }
        }
    }

	private void enterActiveFragment() {
		onFragmentEnabledHandler.postDelayed(new OnFragmentEnabledUpdater(
				settingsManagerInstance.getGeneralSettings().getRecentOpenTab()), 0);
	}

	private class OnFragmentEnabledUpdater implements Runnable {
		private static final int NUMBER_OF_RETRIES = 5;
		private int counter;
		private int currentFragment;

		public OnFragmentEnabledUpdater(int currentFragment) {
			this.counter = 0;
			this.currentFragment = currentFragment;
		}

        @Override public void run() {
            System.out.println("xxx mainActivity enter " + currentFragment + " (counter = " + counter + ")");
            switch (currentFragment) {
                case 0:
                    if (routerFragmentCommunicator != null) {
                        routerFragmentCommunicator.onFragmentEnabled();
                        return;
                    }
                    break;
                case 1:
                    if (poiFragmentCommunicator != null) {
                        poiFragmentCommunicator.onFragmentEnabled();
                        return;
                    }
                    break;
                default:
                    return;
            }
            if (counter < NUMBER_OF_RETRIES) {
                counter += 1;
                onFragmentEnabledHandler.postDelayed(this, 100);
            }
        }
    }


	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */
	public class AppSectionsPagerAdapter extends FragmentStatePagerAdapter {

		private String[] tabTitles;

		public AppSectionsPagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
            this.tabTitles = new String[] {
    				getResources().getString(R.string.menuItemRouterTab),
    				getResources().getString(R.string.menuItemPOITab)
            };
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return RouterFragment.newInstance();
                case 1:
                    return POIFragment.newInstance();
                default:
                    return null;
            }
        }

		@Override public int getCount() {
			return this.tabTitles.length;
		}

		@Override public CharSequence getPageTitle(int position) {
			return this.tabTitles[position];
		}
	}

}
