package org.walkersguide.android.ui.activity.toolbar.tabs;

import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.walkersguide.android.server.wg.poi.PoiProfileRequest;
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
import org.walkersguide.android.ui.fragment.object_list.extended.PoiListFromServerFragment;
import org.walkersguide.android.ui.activity.toolbar.FragmentContainerActivity;
import org.walkersguide.android.ui.activity.toolbar.TabLayoutActivity;
import org.walkersguide.android.ui.activity.toolbar.TabLayoutActivity.AbstractTabAdapter;
import org.walkersguide.android.ui.activity.toolbar.tabs.PointAndRouteTabActivity;
import org.walkersguide.android.ui.dialog.SendFeedbackDialog;

import android.os.Bundle;

import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;


import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.ui.dialog.WhereAmIDialog;
import org.walkersguide.android.ui.dialog.create.SaveCurrentLocationDialog;
import org.walkersguide.android.ui.fragment.RouterFragment;
import android.view.Menu;
import androidx.fragment.app.FragmentActivity;
import java.util.ArrayList;
import org.walkersguide.android.util.SettingsManager;
import android.content.Context;
import android.content.Intent;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.ui.fragment.object_list.extended.HikingTrailListFromServerFragment;
import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.database.profile.FavoritesProfile;
import timber.log.Timber;
import org.walkersguide.android.server.wg.poi.PoiProfile;


public class MainActivity extends TabLayoutActivity {

    public static void loadRoute(Context context, Route route) {
        SettingsManager settingsManagerInstance = SettingsManager.getInstance();
        settingsManagerInstance.setSelectedRoute(route);
        settingsManagerInstance.setSelectedTabForMainActivity(MainActivity.Tab.ROUTER);
        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        mainActivityIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(mainActivityIntent);
    }

    public static void loadPoiProfile(Context context, PoiProfile poiProfile) {
        SettingsManager settingsManagerInstance = SettingsManager.getInstance();
        settingsManagerInstance.setSelectedPoiProfile(poiProfile);
        settingsManagerInstance.setSelectedTabForMainActivity(MainActivity.Tab.POI);
        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        mainActivityIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(mainActivityIntent);
    }


	private SettingsManager settingsManagerInstance;

	private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override public int getLayoutResourceId() {
		return R.layout.activity_main;
    }

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settingsManagerInstance = SettingsManager.getInstance();

        // navigation drawer
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        navigationView = (NavigationView) findViewById(R.id.navigationView);

        // Setup click events on the Navigation View Items.
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(MenuItem menuItem) {
                drawerLayout.closeDrawers();
                if (menuItem.getItemId() == R.id.menuItemPlanRoute) {
                    PlanRouteDialog.newInstance()
                        .show(getSupportFragmentManager(), "PlanRouteDialog");
                } else if (menuItem.getItemId() == R.id.menuItemWhereAmI) {
                    WhereAmIDialog.newInstance()
                        .show(getSupportFragmentManager(), "WhereAmIDialog");
                } else if (menuItem.getItemId() == R.id.menuItemSaveCurrentLocation) {
                    SaveCurrentLocationDialog.newInstance()
                        .show(getSupportFragmentManager(), "SaveCurrentLocationDialog");
                } else if (menuItem.getItemId() == R.id.menuItemFavorites) {
                    PointAndRouteTabActivity.showFavorites(MainActivity.this);
                } else if (menuItem.getItemId() == R.id.menuItemHistory) {
                    PointAndRouteTabActivity.showHistory(MainActivity.this);
                } else if (menuItem.getItemId() == R.id.menuItemSettings) {
                    FragmentContainerActivity.showSettings(MainActivity.this);
                } else if (menuItem.getItemId() == R.id.menuItemInfo) {
                    FragmentContainerActivity.showInfo(MainActivity.this);
                } else if (menuItem.getItemId() == R.id.menuItemContactMe) {
                    SendFeedbackDialog.newInstance(
                            SendFeedbackDialog.FeedbackToken.QUESTION)
                        .show(getSupportFragmentManager(), "SendFeedbackDialog");
                } else {
                    return false;
                }
                return true;
            }
        });

        // drawer toggle
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, super.getToolbar(), R.string.openNavigationDrawer, R.string.closeNavigationDrawer);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        initializeViewPagerAndTabLayout(
                createTabAdapter(), settingsManagerInstance.getSelectedTabForMainActivity());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initializeViewPagerAndTabLayout(
                createTabAdapter(), settingsManagerInstance.getSelectedTabForMainActivity());
    }

    @Override public void tabSelected(Enum<?> newTab) {
        super.tabSelected(newTab);
        if (newTab instanceof Tab) {
            settingsManagerInstance.setSelectedTabForMainActivity((Tab) newTab);
        }
    }

    @Override public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }


    /**
     * tabs
     */

    public enum Tab {
        FAVORITE_POINTS, ROUTER, POI, HIKING_TRAILS
    }

    private TabAdapter createTabAdapter() {
        ArrayList<Tab> tabList = new ArrayList<Tab>();
        tabList.add(Tab.FAVORITE_POINTS);
        tabList.add(Tab.ROUTER);
        tabList.add(Tab.POI);
        // hide the hiking trails tab for now
        //tabList.add(Tab.HIKING_TRAILS);
        return new TabAdapter(MainActivity.this, tabList);
    }


	private class TabAdapter extends AbstractTabAdapter {

        public TabAdapter(FragmentActivity activity, ArrayList<Tab> tabList) {
            super(activity, tabList);
        }

        @Override public Fragment createFragment(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case FAVORITE_POINTS:
                        return ObjectListFromDatabaseFragment.createFragment(
                                FavoritesProfile.favoritePoints(), SortMethod.DISTANCE_ASC);
                    case POI:
                        return PoiListFromServerFragment.createPoiFragment();
                    case ROUTER:
                        return RouterFragment.newInstance();
                    case HIKING_TRAILS:
                        return HikingTrailListFromServerFragment.newInstance();
                }
            }
            return null;
        }

        @Override public String getFragmentName(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case FAVORITE_POINTS:
                        return getResources().getString(R.string.favoritesProfile);
                    case POI:
                        return getResources().getString(R.string.fragmentPOIName);
                    case ROUTER:
                        return getResources().getString(R.string.fragmentRouterName);
                    case HIKING_TRAILS:
                        return getResources().getString(R.string.fragmentHikingTrailListName);
                }
            }
            return null;
        }
	}

}
