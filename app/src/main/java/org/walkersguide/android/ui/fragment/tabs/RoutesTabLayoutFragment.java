package org.walkersguide.android.ui.fragment.tabs;

import org.walkersguide.android.ui.fragment.tabs.routes.NavigateFragment;
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

    public static RoutesTabLayoutFragment newInstance(Enum<?> selectedTab) {
        RoutesTabLayoutFragment fragment = new RoutesTabLayoutFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_TAB, selectedTab);
        fragment.setArguments(args);
        return fragment;
    }


    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViewPagerAndTabLayout(new RoutesTabAdapter());
    }


    /**
     * tabs
     */

    public enum Tab {
        NAVIGATE(GlobalInstance.getStringResource(R.string.fragmentNavigateName)),
        RECORD(GlobalInstance.getStringResource(R.string.fragmentRecordRouteName));

        public String label;
        private Tab(String label) {
            this.label = label;
        }

        @Override public String toString() {
            return this.label;
        }
    }


    private class RoutesTabAdapter extends AbstractTabAdapter {

        public RoutesTabAdapter() {
            super(new ArrayList<Tab>(Arrays.asList(Tab.values())));
        }

        @Override public Enum<?> getDefaultTab() {
            return Tab.NAVIGATE;
        }

        @Override public Fragment getFragment(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case NAVIGATE:
                        return NavigateFragment.newInstance(
                                SettingsManager.getInstance().getLastSelectedRoute(), true);
                    case RECORD:
                        return RecordRouteFragment.newInstance();
                }
            }
            return null;
        }
    }

}
