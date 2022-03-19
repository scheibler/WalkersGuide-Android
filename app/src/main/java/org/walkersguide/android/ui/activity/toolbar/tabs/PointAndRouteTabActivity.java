package org.walkersguide.android.ui.activity.toolbar.tabs;

import org.walkersguide.android.server.wg.poi.PoiProfileRequest;
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
import org.walkersguide.android.ui.fragment.object_list.extended.PoiListFromServerFragment;
import org.walkersguide.android.ui.activity.toolbar.FragmentContainerActivity;
import org.walkersguide.android.ui.activity.toolbar.TabLayoutActivity;
import org.walkersguide.android.ui.activity.toolbar.TabLayoutActivity.AbstractTabAdapter;
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
import org.walkersguide.android.database.profile.FavoritesProfile;
import org.walkersguide.android.database.SortMethod;


public class PointAndRouteTabActivity extends TabLayoutActivity {
    private static final String KEY_MODE = "mode";

    public static void showFavorites(Context packageContext) {
        Intent intent = new Intent(packageContext, PointAndRouteTabActivity.class);
        intent.putExtra(KEY_MODE, Mode.FAVORITES);
        packageContext.startActivity(intent);
    }

    public static void showHistory(Context packageContext) {
        Intent intent = new Intent(packageContext, PointAndRouteTabActivity.class);
        intent.putExtra(KEY_MODE, Mode.HISTORY);
        packageContext.startActivity(intent);
    }

    private enum Mode {
        FAVORITES, HISTORY
    }


    @Override public int getLayoutResourceId() {
		return R.layout.layout_toolbar_and_view_pager_and_tab_layout;
    }

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ArrayList<Tab> tabList = new ArrayList<Tab>();
        tabList.add(Tab.POINTS);
        tabList.add(Tab.ROUTES);

        AbstractTabAdapter tabAdapter = null;
        Mode mode = (Mode) getIntent().getExtras().getSerializable(KEY_MODE);
        if (mode == Mode.FAVORITES) {
            tabAdapter = new FavoritesTabAdapter(
                    PointAndRouteTabActivity.this, tabList);
        } else if (mode == Mode.HISTORY) {
            tabAdapter = new ObjectHistoryTabAdapter(
                    PointAndRouteTabActivity.this, tabList);
        }

        if (tabAdapter != null) {
            initializeViewPagerAndTabLayout(tabAdapter, Tab.POINTS);
        }
    }


    /**
     * tabs
     */

    public enum Tab {
        POINTS, ROUTES
    }


	private class FavoritesTabAdapter extends AbstractTabAdapter {

        public FavoritesTabAdapter(FragmentActivity activity, ArrayList<Tab> tabList) {
            super(activity, tabList);
        }

        @Override public Fragment createFragment(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case POINTS:
                        return ObjectListFromDatabaseFragment.createFragment(
                                FavoritesProfile.favoritePoints(), SortMethod.ACCESSED_DESC);
                    case ROUTES:
                        return ObjectListFromDatabaseFragment.createFragment(
                                FavoritesProfile.favoriteRoutes(), SortMethod.ACCESSED_DESC);
                }
            }
            return null;
        }

        @Override public String getFragmentName(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case POINTS:
                        return getResources().getString(R.string.fragmentPointFavoritesName);
                    case ROUTES:
                        return getResources().getString(R.string.fragmentRouteFavoritesName);
                }
            }
            return null;
        }
	}


	private class ObjectHistoryTabAdapter extends AbstractTabAdapter {

        public ObjectHistoryTabAdapter(FragmentActivity activity, ArrayList<Tab> tabList) {
            super(activity, tabList);
        }

        @Override public Fragment createFragment(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case POINTS:
                        return ObjectListFromDatabaseFragment.createPointHistoryFragment();
                    case ROUTES:
                        return ObjectListFromDatabaseFragment.createRouteHistoryFragment();
                }
            }
            return null;
        }

        @Override public String getFragmentName(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case POINTS:
                        return getResources().getString(R.string.fragmentPointHistoryName);
                    case ROUTES:
                        return getResources().getString(R.string.fragmentRouteHistoryName);
                }
            }
            return null;
        }
	}

}
