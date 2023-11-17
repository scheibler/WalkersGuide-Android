package org.walkersguide.android.ui.fragment.tabs;

import org.walkersguide.android.ui.fragment.tabs.routes.RouterFragment;
import org.walkersguide.android.ui.fragment.tabs.routes.RecordRouteFragment;
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
import java.util.ArrayList;
import java.util.Arrays;


public class RoutesTabLayoutFragment extends TabLayoutFragment {

	public static RoutesTabLayoutFragment newInstance() {
		RoutesTabLayoutFragment fragment = new RoutesTabLayoutFragment();
		return fragment;
	}


	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        initializeViewPagerAndTabLayout(
                new RoutesTabAdapter(RoutesTabLayoutFragment.this), Tab.ROUTER);
    }


    /**
     * tabs
     */

    private enum Tab {
        ROUTER, RECORD
    }


	private class RoutesTabAdapter extends AbstractTabAdapter {

        public RoutesTabAdapter(Fragment fragment) {
            super(fragment, new ArrayList<Tab>(Arrays.asList(Tab.values())));
        }

        @Override public Fragment createFragment(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case ROUTER:
                        return RouterFragment.newInstance(
                                SettingsManager.getInstance().getLastSelectedRoute(), true);
                    case RECORD:
                        return RecordRouteFragment.newInstance();
                }
            }
            return null;
        }

        @Override public String getFragmentName(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case ROUTER:
                        return getResources().getString(R.string.fragmentRouterName);
                    case RECORD:
                        return getResources().getString(R.string.fragmentRecordRouteName);
                }
            }
            return null;
        }
	}

}
