package org.walkersguide.android.ui.activity.toolbar;

import androidx.viewpager2.adapter.FragmentStateAdapter;
import org.walkersguide.android.ui.dialog.create.RouteFromGpxFileDialog;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.ui.fragment.tabs.ContainerTabLayoutFragment;
import org.walkersguide.android.ui.fragment.object_list.extended.PoiListFromServerFragment;
import org.walkersguide.android.ui.activity.toolbar.FragmentContainerActivity;
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
import androidx.fragment.app.FragmentActivity;
import java.util.ArrayList;
import org.walkersguide.android.util.SettingsManager;
import android.content.Context;
import android.content.Intent;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.ui.fragment.object_list.extended.HikingTrailListFromServerFragment;
import timber.log.Timber;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.ui.dialog.create.EnterAddressDialog;
import org.walkersguide.android.ui.dialog.create.EnterCoordinatesDialog;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.ui.dialog.create.PointFromCoordinatesLinkDialog;

import org.walkersguide.android.ui.activity.ToolbarActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;


public class MainActivity extends ToolbarActivity implements FragmentResultListener {
    private static String KEY_SELECTED_TAB = "selectedTab";

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

	private ViewPager2 viewPager;
    private TabLayout tabLayout;

    private Tab selectedTab;

    @Override public int getLayoutResourceId() {
		return R.layout.activity_main;
    }

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settingsManagerInstance = SettingsManager.getInstance();

        // fragment result listener
        getSupportFragmentManager()
            .setFragmentResultListener(
                    PointFromCoordinatesLinkDialog.REQUEST_FROM_COORDINATES_LINK, this, this);
        getSupportFragmentManager()
            .setFragmentResultListener(
                    EnterAddressDialog.REQUEST_ENTER_ADDRESS, this, this);
        getSupportFragmentManager()
            .setFragmentResultListener(
                    EnterCoordinatesDialog.REQUEST_ENTER_COORDINATES, this, this);

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
                } else if (menuItem.getItemId() == R.id.menuItemCreateFavoriteCurrentPosition) {
                    SaveCurrentLocationDialog.newInstance()
                        .show(getSupportFragmentManager(), "SaveCurrentLocationDialog");
                } else if (menuItem.getItemId() == R.id.menuItemHistory) {
                    FragmentContainerActivity.showHistory(MainActivity.this);
                } else if (menuItem.getItemId() == R.id.menuItemImportPointFromPostAddress) {
                    EnterAddressDialog.newInstance()
                        .show(getSupportFragmentManager(), "EnterAddressDialog");
                } else if (menuItem.getItemId() == R.id.menuItemImportPointFromCoordinates) {
                    EnterCoordinatesDialog.newInstance()
                        .show(getSupportFragmentManager(), "EnterCoordinatesDialog");
                } else if (menuItem.getItemId() == R.id.menuItemImportPointFromUrl) {
                    PointFromCoordinatesLinkDialog.newInstance()
                        .show(getSupportFragmentManager(), "PointFromCoordinatesLinkDialog");
                } else if (menuItem.getItemId() == R.id.menuItemRouteFromGpxFile) {
                    RouteFromGpxFileDialog.newInstance()
                        .show(getSupportFragmentManager(), "RouteFromGpxFileDialog");
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

		viewPager = (ViewPager2) findViewById(R.id.pager);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                Timber.d("onPageSelected: %1$d", position);
                tabLayout.selectTab(tabLayout.getTabAt(position));
                setToolbarTitle(
                        tabLayout.getTabAt(position).getText().toString());
                settingsManagerInstance.setSelectedTabForMainActivity(
                        ((TabAdapter) viewPager.getAdapter()).getTab(position));
            }
        });

		tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        if (savedInstanceState != null) {
            selectedTab = (Tab) savedInstanceState.getSerializable(KEY_SELECTED_TAB);
        } else if (getIntent() != null && getIntent().getExtras() != null) {
            selectedTab = (Tab) getIntent().getExtras().getSerializable(KEY_SELECTED_TAB);
        } else {
            selectedTab = settingsManagerInstance.getSelectedTabForMainActivity();
        }

        initializeViewPagerAndTabLayout();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initializeViewPagerAndTabLayout();
    }

    public void initializeViewPagerAndTabLayout() {
        ArrayList<Tab> tabList = new ArrayList<Tab>();
        tabList.add(Tab.FAVORITES);
        tabList.add(Tab.ROUTER);
        tabList.add(Tab.POI);
        // hide the hiking trails tab for now
        //tabList.add(Tab.HIKING_TRAILS);
        TabAdapter tabAdapter = new TabAdapter(MainActivity.this, tabList);

        // prepare tab layout
        for (int i=0; i<tabAdapter.getItemCount(); i++) {
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setText(tabAdapter.getFragmentName(i));
            tabLayout.addTab(tab);
        }

        // load fragment
        viewPager.setAdapter(tabAdapter);
        viewPager.setCurrentItem(tabAdapter.getTabIndex(selectedTab));
    }

	@Override public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
    	savedInstanceState.putSerializable(KEY_SELECTED_TAB, selectedTab);
    }

    @Override public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(PointFromCoordinatesLinkDialog.REQUEST_FROM_COORDINATES_LINK)
                || requestKey.equals(EnterAddressDialog.REQUEST_ENTER_ADDRESS)
                || requestKey.equals(EnterCoordinatesDialog.REQUEST_ENTER_COORDINATES)) {
            Point newFavorite = null;
            if (requestKey.equals(PointFromCoordinatesLinkDialog.REQUEST_FROM_COORDINATES_LINK)) {
                newFavorite = (GPS) bundle.getSerializable(PointFromCoordinatesLinkDialog.EXTRA_COORDINATES);
            } else if (requestKey.equals(EnterAddressDialog.REQUEST_ENTER_ADDRESS)) {
                newFavorite = (StreetAddress) bundle.getSerializable(EnterAddressDialog.EXTRA_STREET_ADDRESS);
            } else if (requestKey.equals(EnterCoordinatesDialog.REQUEST_ENTER_COORDINATES)) {
                newFavorite = (GPS) bundle.getSerializable(EnterCoordinatesDialog.EXTRA_COORDINATES);
            }
            if (newFavorite != null) {
                FragmentContainerActivity.showObjectDetails(MainActivity.this, newFavorite);
            } else {
                SimpleMessageDialog.newInstance(
                        getResources().getString(R.string.errorFavoriteCreationFailed))
                    .show(getSupportFragmentManager(), "SimpleMessageDialog");
            }
        }
    }


    /**
     * tabs
     */

    public enum Tab {
        FAVORITES, ROUTER, POI, HIKING_TRAILS
    }


    private class TabAdapter extends FragmentStateAdapter {
        private ArrayList<? extends Enum> tabList;

        public TabAdapter(FragmentActivity activity, ArrayList<? extends Enum> tabList) {
            super(activity);
            this.tabList = tabList;
        }

        @Override public Fragment createFragment(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case FAVORITES:
                        return ContainerTabLayoutFragment.favorites();
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

        @Override public int getItemCount() {
            return this.tabList.size();
        }

        public String getFragmentName(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case FAVORITES:
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

        public <T extends Enum> T getTab(int index) {
            if (index >= 0 && index < this.tabList.size()) {
                return (T) this.tabList.get(index);
            }
            return (T) this.tabList.get(0);
        }

        public int getTabIndex(Enum<?> tab) {
            int tabIndex = this.tabList.indexOf(tab);
            Timber.d("getTabIndex: tabIndex=%1$d", tabIndex);
            if (tabIndex >= 0) {
                return tabIndex;
            }
            return 0;
        }
    }

}
