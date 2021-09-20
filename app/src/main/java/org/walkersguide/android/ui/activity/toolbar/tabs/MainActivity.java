package org.walkersguide.android.ui.activity.toolbar.tabs;

import org.walkersguide.android.database.profiles.DatabasePointProfile;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.server.poi.PoiProfileRequest;
import org.walkersguide.android.ui.fragment.object_list.ObjectListFromDatabaseFragment;
import org.walkersguide.android.ui.fragment.object_list.PoiListFromServerFragment;
import org.walkersguide.android.ui.activity.SettingsActivity;
import org.walkersguide.android.ui.activity.toolbar.FragmentContainerActivity;
import org.walkersguide.android.ui.activity.toolbar.AbstractTabsActivity;
import org.walkersguide.android.ui.activity.toolbar.AbstractTabsActivity.AbstractTabAdapter;
import org.walkersguide.android.ui.dialog.SendFeedbackDialog;
import android.content.Intent;

import android.os.Bundle;

import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;

import java.util.TreeSet;

import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.ui.dialog.WhereAmIDialog;
import org.walkersguide.android.ui.dialog.HikingTrailsDialog;
import org.walkersguide.android.ui.dialog.creators.SaveCurrentLocationDialog;
import org.walkersguide.android.ui.fragment.routing.RouterFragment;
import org.walkersguide.android.util.Constants;
import android.view.Menu;
import androidx.fragment.app.FragmentActivity;
import java.util.ArrayList;
import timber.log.Timber;
import org.walkersguide.android.util.SettingsManager;


public class MainActivity extends AbstractTabsActivity {

    public enum Tab {
        FAVORITES, POI, ROUTER
    }


	private DrawerLayout drawerLayout;
    private NavigationView navigationView;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Timber.d("onCreate");

        // navigation drawer
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        navigationView = (NavigationView) findViewById(R.id.navigationView);
        navigationView.setItemIconTintList(null);
        // hide hiking trails menu item for now
        Menu navigationViewMenu = navigationView.getMenu();
        navigationViewMenu.findItem(R.id.menuItemHikingTrails).setVisible(false);

        // Setup click events on the Navigation View Items.
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(MenuItem menuItem) {
                drawerLayout.closeDrawers();
                switch (menuItem.getItemId()) {
                    case R.id.menuItemPlanRoute:
                        PlanRouteDialog.newInstance()
                            .show(getSupportFragmentManager(), "PlanRouteDialog");
                        break;
                    case R.id.menuItemWhereAmI:
                        WhereAmIDialog.newInstance()
                            .show(getSupportFragmentManager(), "WhereAmIDialog");
                        break;
                    case R.id.menuItemSaveCurrentLocation:
                        SaveCurrentLocationDialog.newInstance()
                            .show(getSupportFragmentManager(), "SaveCurrentLocationDialog");
                        break;
                    case R.id.menuItemLastPoints:
                        FragmentContainerActivity.showPointHistory(MainActivity.this);
                        break;
                    case R.id.menuItemLastRoutes:
                        FragmentContainerActivity.showRouteHistory(MainActivity.this);
                        break;
                    case R.id.menuItemHikingTrails:
                        HikingTrailsDialog.newInstance()
                            .show(getSupportFragmentManager(), "HikingTrailsDialog");
                        break;
                    case R.id.menuItemSettings:
                        Intent intentStartSettingsActivity = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(intentStartSettingsActivity);
                        break;
                    case R.id.menuItemInfo:
                        FragmentContainerActivity.showInfo(MainActivity.this);
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

        // drawer toggle
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, super.toolbar, R.string.openNavigationDrawer, R.string.closeNavigationDrawer);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // prepare tab list
        ArrayList<MainActivity.Tab> tabList = new ArrayList<MainActivity.Tab>();
        tabList.add(Tab.ROUTER);
        tabList.add(Tab.POI);
        tabList.add(Tab.FAVORITES);

        initializeViewPagerAndTabLayout(
                new TabAdapter(MainActivity.this, tabList),
                settingsManagerInstance.getSelectedTabForMainActivity());
    }

    @Override public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }


    /**
     * implement AbstractToolbarActivity and AbstractTabsActivity functions
     */

    public int getLayoutResourceId() {
		return R.layout.activity_main;
    }

    public void tabSelected(int tabIndex) {
        settingsManagerInstance.setSelectedTabForMainActivity(tabIndex);
    }


    /**
     * tab adapter
     */

	private class TabAdapter extends AbstractTabAdapter {
        private ArrayList<MainActivity.Tab> tabList;

        public TabAdapter(FragmentActivity activity, ArrayList<MainActivity.Tab> tabList) {
            super(activity);
            this.tabList = tabList;
        }

        @Override public Fragment createFragment(int position) {
            switch (this.tabList.get(position)) {
                case FAVORITES:
                    return ObjectListFromDatabaseFragment.createFragment(
                            new DatabaseProfileRequest(DatabasePointProfile.FAVORITES), false);
                case POI:
                    return PoiListFromServerFragment.createFragment(
                            new PoiProfileRequest(
                                SettingsManager.getInstance().getSelectedPoiProfile()),
                            true);
                case ROUTER:
                    return RouterFragment.newInstance();
                default:
                    return null;
            }
        }

		@Override public int getItemCount() {
			return this.tabList.size();
		}

        @Override public String getFragmentName(int position) {
            switch (this.tabList.get(position)) {
                case FAVORITES:
                    return DatabasePointProfile.FAVORITES.getName();
                case POI:
                    return getResources().getString(R.string.fragmentPOIName);
                case ROUTER:
                    return getResources().getString(R.string.fragmentRouterName);
                default:
                    return "";
            }
        }
	}

}
