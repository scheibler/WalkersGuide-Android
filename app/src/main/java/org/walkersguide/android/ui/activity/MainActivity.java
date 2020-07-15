package org.walkersguide.android.ui.activity;

import org.walkersguide.android.ui.dialog.SendFeedbackDialog;
import android.content.Intent;

import android.os.Bundle;

import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;

import android.view.MenuItem;

import java.util.TreeSet;

import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.ui.dialog.RequestAddressDialog;
import org.walkersguide.android.ui.dialog.SaveCurrentPositionDialog;
import org.walkersguide.android.ui.fragment.main.POIFragment;
import org.walkersguide.android.ui.fragment.main.RouterFragment;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager.GeneralSettings;
import org.walkersguide.android.data.profile.HistoryPointProfile;


public class MainActivity extends AbstractActivity {

	// navigation drawer
	private DrawerLayout drawerLayout;
    private NavigationView navigationView;

	private ViewPager viewPager;
    private TabAdapter tabAdapter;
    private TabLayout tabLayout;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // navigation drawer
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        navigationView = (NavigationView) findViewById(R.id.navigationView);
        navigationView.setItemIconTintList(null);

        // Setup click events on the Navigation View Items.
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(MenuItem menuItem) {
                drawerLayout.closeDrawers();
                switch (menuItem.getItemId()) {
                    case R.id.menuItemPlanRoute:
                        PlanRouteDialog.newInstance(false)
                            .show(getSupportFragmentManager(), "PlanRouteDialog");
                        break;
                    case R.id.menuItemLastVisitedPoints:
                        // reset to all points category
                        settingsManagerInstance.getPOISettings()
                            .setSelectedHistoryPointProfileId(HistoryPointProfile.ID_ALL_POINTS);
                        // open dialog
                        POIFragment.newInstance(
                                POIFragment.ContentType.HISTORY_POINTS, Constants.POINT_PUT_INTO.NOWHERE)
                            .show(getSupportFragmentManager(), "LastVisitedPointsDialog");
                        break;
                    case R.id.menuItemSaveCurrentPosition:
                        SaveCurrentPositionDialog.newInstance(
                                new TreeSet<Integer>())
                            .show(getSupportFragmentManager(), "SaveCurrentPositionDialog");
                        break;
                    case R.id.menuItemRequestAddress:
                        RequestAddressDialog.newInstance()
                            .show(getSupportFragmentManager(), "RequestAddressDialog");
                        break;
                    case R.id.menuItemSettings:
                        Intent intentStartSettingsActivity = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(intentStartSettingsActivity);
                        break;
                    case R.id.menuItemInfo:
                        Intent intentStartInfoActivity = new Intent(MainActivity.this, InfoActivity.class);
                        startActivity(intentStartInfoActivity);
                        break;
                    case R.id.menuItemContactMe:
                        SendFeedbackDialog.newInstance(
                                SendFeedbackDialog.Token.QUESTION)
                            .show(getSupportFragmentManager(), "SendFeedbackDialog");
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.openNavigationDrawer, R.string.closeNavigationDrawer);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

		viewPager = (ViewPager) findViewById(R.id.pager);
        tabAdapter = new TabAdapter(this);
        viewPager.setAdapter(tabAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                GeneralSettings generalSettings = settingsManagerInstance.getGeneralSettings();
    			if (generalSettings.getRecentOpenTab() != position) {
                    // set toolbar title
                    setToolbarTitle(position);
                    // save active fragment
    				generalSettings.setRecentOpenTab(position);
                }
            }
        });

		tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        // open the recent tab
        int tabIndex = settingsManagerInstance.getGeneralSettings().getRecentOpenTab();
        setToolbarTitle(tabIndex);
        viewPager.setCurrentItem(tabIndex);
    }

    @Override public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    private void setToolbarTitle(int tabIndex) {
        getSupportActionBar().setTitle(
                tabAdapter.getPageTitle(tabIndex).toString());
    }


	public class TabAdapter extends FragmentPagerAdapter {

		public TabAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case Constants.MAIN_FRAGMENT.ROUTER:
                    return RouterFragment.newInstance();
                case Constants.MAIN_FRAGMENT.POI:
                    return POIFragment.newInstance(
                            POIFragment.ContentType.POI, Constants.POINT_PUT_INTO.NOWHERE);
                default:
                    return null;
            }
        }

		@Override public CharSequence getPageTitle(int position) {
            switch (position) {
                case Constants.MAIN_FRAGMENT.ROUTER:
    				return getResources().getString(R.string.fragmentRouterName);
                case Constants.MAIN_FRAGMENT.POI:
    				return getResources().getString(R.string.fragmentPOIName);
                default:
                    return "";
            }
		}

		@Override public int getCount() {
			return Constants.MainActivityFragmentValueArray.length;
		}
	}

}
