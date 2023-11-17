package org.walkersguide.android.ui.fragment;

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
import androidx.appcompat.app.AppCompatActivity;
import org.walkersguide.android.ui.fragment.RootFragment;


public abstract class TabLayoutFragment extends RootFragment {
    protected static String KEY_SELECTED_TAB = "selectedTab";

	private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private Enum<?> selectedTab;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            selectedTab = (Enum<?>) savedInstanceState.getSerializable(KEY_SELECTED_TAB);
        } else if (getArguments() != null) {
            selectedTab = (Enum<?>) getArguments().getSerializable(KEY_SELECTED_TAB);
        }
    }

    @Override public String getTitle() {
        AbstractTabAdapter adapter = (AbstractTabAdapter) viewPager.getAdapter();
        if (adapter != null && this.selectedTab != null) {
            return adapter.getFragmentName(
                    adapter.getTabIndex(this.selectedTab));
        }
        return null;
    }

    @Override public int getLayoutResourceId() {
        return R.layout.layout_view_pager_and_tab_layout_above;
    }

	@Override public View configureView(View view, Bundle savedInstanceState) {
		viewPager = (ViewPager2) view.findViewById(R.id.pager);
        viewPager.setUserInputEnabled(false);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                Timber.d("onPageSelected: %1$d", position);
                tabLayout.selectTab(tabLayout.getTabAt(position));
                // notify child fragments
                tabSelected(
                        ((AbstractTabAdapter) viewPager.getAdapter()).getTab(position));
            }
        });

		tabLayout = (TabLayout) view.findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    	savedInstanceState.putSerializable(KEY_SELECTED_TAB, selectedTab);
    }


    public void initializeViewPagerAndTabLayout(AbstractTabAdapter tabAdapter) {
        initializeViewPagerAndTabLayout(tabAdapter, null);
    }

    public void initializeViewPagerAndTabLayout(AbstractTabAdapter tabAdapter, Enum<?> newTab) {
        Timber.d("initializeViewPagerAndTabLayout: %1$s vs. %2$s", selectedTab, newTab);
        if (tabAdapter.getItemCount() > 0) {
            viewPager.setAdapter(tabAdapter);

            // prepare tab layout
            for (int i=0; i<tabAdapter.getItemCount(); i++) {
                TabLayout.Tab tab = tabLayout.newTab();
                tab.setText(tabAdapter.getFragmentName(i));
                tabLayout.addTab(tab);
            }
            tabLayout.setVisibility(
                    tabAdapter.getItemCount() > 1
                    ?  View.VISIBLE : View.GONE);

            // load fragment
            if (newTab != null) {
                selectedTab = newTab;
            }
            viewPager.setCurrentItem(tabAdapter.getTabIndex(selectedTab));
        }
    }

    public void tabSelected(Enum<?> newTab) {
        this.selectedTab = newTab;
        updateToolbarTitle();
    }


    public abstract class AbstractTabAdapter extends FragmentStateAdapter {
        private ArrayList<? extends Enum> tabList;

        public AbstractTabAdapter(Fragment fragment, ArrayList<? extends Enum> tabList) {
            super(fragment);
            this.tabList = tabList;
        }

        public abstract String getFragmentName(int position);

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

        @Override public int getItemCount() {
            return this.tabList.size();
        }
    }

}
