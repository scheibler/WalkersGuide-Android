package org.walkersguide.android.ui.fragment.tabs;

import org.walkersguide.android.ui.fragment.TabLayoutFragment;
import org.walkersguide.android.ui.fragment.TabLayoutFragment.AbstractTabAdapter;
import android.text.format.DateFormat;
import android.widget.TextView;
import java.util.Date;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.server.wg.status.OSMMap;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.R;
import android.content.Context;


import android.os.Bundle;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;


import androidx.fragment.app.Fragment;
import org.walkersguide.android.util.SettingsManager;


import org.walkersguide.android.ui.dialog.SimpleMessageDialog;

import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.status.ServerStatusTask;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.server.wg.WgException;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.Intent;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.fragment.app.FragmentActivity;
import timber.log.Timber;
import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
import org.walkersguide.android.database.profile.FavoritesProfile;
import java.util.ArrayList;
import java.util.Arrays;


public class HistoryTabLayoutFragment extends TabLayoutFragment {
    private static final String KEY_MODE = "mode";

	public static HistoryTabLayoutFragment newInstance() {
		HistoryTabLayoutFragment fragment = new HistoryTabLayoutFragment();
		return fragment;
	}


	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        initializeViewPagerAndTabLayout(
                new HistoryTabAdapter(HistoryTabLayoutFragment.this), Tab.LAST_POINTS);
    }


    /**
     * tabs
     */

    private enum Tab {
        LAST_POINTS, LAST_ROUTES
    }


	private class HistoryTabAdapter extends AbstractTabAdapter {

        public HistoryTabAdapter(Fragment fragment) {
            super(fragment, new ArrayList<Tab>(Arrays.asList(Tab.values())));
        }

        @Override public Fragment createFragment(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case LAST_POINTS:
                        return ObjectListFromDatabaseFragment.createPointHistoryFragment();
                    case LAST_ROUTES:
                        return ObjectListFromDatabaseFragment.createRouteHistoryFragment();
                }
            }
            return null;
        }

        @Override public String getFragmentName(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case LAST_POINTS:
                        return getResources().getString(R.string.fragmentPointHistoryName);
                    case LAST_ROUTES:
                        return getResources().getString(R.string.fragmentRouteHistoryName);
                }
            }
            return null;
        }
	}

}