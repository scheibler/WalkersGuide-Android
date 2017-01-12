package org.walkersguide.android.ui.activity;

import org.walkersguide.android.R;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.ui.fragment.FavoriteFragment;
import org.walkersguide.android.ui.fragment.LocationAndDirectionFragment;
import org.walkersguide.android.ui.fragment.POIFragment;
import org.walkersguide.android.ui.fragment.RouterFragment;
import org.walkersguide.android.ui.fragment.SearchFragment;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager.GeneralSettings;
import org.walkersguide.android.util.TTSWrapper;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends AbstractActivity {

	// communicate with attached fragments
	public FragmentCommunicator searchFragmentCommunicator;
	public FragmentCommunicator favoriteFragmentCommunicator;
	public FragmentCommunicator routerFragmentCommunicator;
	public FragmentCommunicator poiFragmentCommunicator;
	public FragmentCommunicator locationAndDirectionFragmentCommunicator;

	// navigation drawer
	private DrawerLayout drawerLayout;
    private NavigationView navigationView;

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

        // navigation drawer
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        navigationView = (NavigationView) findViewById(R.id.navigationView);
        navigationView.setItemIconTintList(null);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(
        		mAppSectionsPagerAdapter.getPageTitle(
                    settingsManagerInstance.getGeneralSettings().getRecentOpenTab())
                .toString());
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.openNavigationDrawer, R.string.closeNavigationDrawer);
        drawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.syncState();

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
                    // set toolbar title
        			String activeTabName = mAppSectionsPagerAdapter.getPageTitle(position).toString();
                    getSupportActionBar().setTitle(activeTabName);
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

        // Setup click events on the Navigation View Items.
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(MenuItem menuItem) {
                drawerLayout.closeDrawers();
                if (menuItem.getItemId() == R.id.menuItemSearchFragment) {
                    switchFragmentGestureDetected = false;
                    mViewPager.setCurrentItem(Constants.FRAGMENT.SEARCH);
                } else if (menuItem.getItemId() == R.id.menuItemFavoriteFragment) {
                    switchFragmentGestureDetected = false;
                    mViewPager.setCurrentItem(Constants.FRAGMENT.FAVORITE);
                } else if (menuItem.getItemId() == R.id.menuItemRouterFragment) {
                    switchFragmentGestureDetected = false;
                    mViewPager.setCurrentItem(Constants.FRAGMENT.ROUTER);
                } else if (menuItem.getItemId() == R.id.menuItemPOIFragment) {
                    switchFragmentGestureDetected = false;
                    mViewPager.setCurrentItem(Constants.FRAGMENT.POI);
                } else if (menuItem.getItemId() == R.id.menuItemLocationAndDirectionFragment) {
                    switchFragmentGestureDetected = false;
                    mViewPager.setCurrentItem(Constants.FRAGMENT.LOCATION_AND_DIRECTION);
                }
                return true;
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
                case Constants.FRAGMENT.SEARCH:
                    if (searchFragmentCommunicator != null) {
                        searchFragmentCommunicator.onFragmentDisabled();
                        return;
                    }
                    break;
                case Constants.FRAGMENT.FAVORITE:
                    if (favoriteFragmentCommunicator != null) {
                        favoriteFragmentCommunicator.onFragmentDisabled();
                        return;
                    }
                    break;
                case Constants.FRAGMENT.ROUTER:
                    if (routerFragmentCommunicator != null) {
                        routerFragmentCommunicator.onFragmentDisabled();
                        return;
                    }
                    break;
                case Constants.FRAGMENT.POI:
                    if (poiFragmentCommunicator != null) {
                        poiFragmentCommunicator.onFragmentDisabled();
                        return;
                    }
                    break;
                case Constants.FRAGMENT.LOCATION_AND_DIRECTION:
                    if (locationAndDirectionFragmentCommunicator != null) {
                        locationAndDirectionFragmentCommunicator.onFragmentDisabled();
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
            switch (currentFragment) {
                case Constants.FRAGMENT.SEARCH:
                    if (searchFragmentCommunicator != null) {
                        searchFragmentCommunicator.onFragmentEnabled();
                        return;
                    }
                    break;
                case Constants.FRAGMENT.FAVORITE:
                    if (favoriteFragmentCommunicator != null) {
                        favoriteFragmentCommunicator.onFragmentEnabled();
                        return;
                    }
                    break;
                case Constants.FRAGMENT.ROUTER:
                    if (routerFragmentCommunicator != null) {
                        routerFragmentCommunicator.onFragmentEnabled();
                        return;
                    }
                    break;
                case Constants.FRAGMENT.POI:
                    if (poiFragmentCommunicator != null) {
                        poiFragmentCommunicator.onFragmentEnabled();
                        return;
                    }
                    break;
                case Constants.FRAGMENT.LOCATION_AND_DIRECTION:
                    if (locationAndDirectionFragmentCommunicator != null) {
                        locationAndDirectionFragmentCommunicator.onFragmentEnabled();
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

		public AppSectionsPagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case Constants.FRAGMENT.SEARCH:
                    return SearchFragment.newInstance();
                case Constants.FRAGMENT.FAVORITE:
                    return FavoriteFragment.newInstance();
                case Constants.FRAGMENT.ROUTER:
                    return RouterFragment.newInstance();
                case Constants.FRAGMENT.POI:
                    return POIFragment.newInstance();
                case Constants.FRAGMENT.LOCATION_AND_DIRECTION:
                    return LocationAndDirectionFragment.newInstance();
                default:
                    return null;
            }
        }

		@Override public CharSequence getPageTitle(int position) {
            switch (position) {
                case Constants.FRAGMENT.SEARCH:
    				return getResources().getString(R.string.fragmentSearchName);
                case Constants.FRAGMENT.FAVORITE:
    				return getResources().getString(R.string.fragmentFavoriteName);
                case Constants.FRAGMENT.ROUTER:
    				return getResources().getString(R.string.fragmentRouterName);
                case Constants.FRAGMENT.POI:
    				return getResources().getString(R.string.fragmentPOIName);
                case Constants.FRAGMENT.LOCATION_AND_DIRECTION:
    				return getResources().getString(R.string.fragmentLocationAndDirectionName);
                default:
                    return "";
            }
		}

		@Override public int getCount() {
			return Constants.MainActivityFragmentValueArray.length;
		}
	}

}
