package org.walkersguide.android.ui.fragment;

import org.walkersguide.android.ui.fragment.tabs.routes.NavigateFragment;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
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


public abstract class TabLayoutFragment extends RootFragment implements OnTabSelectedListener {
    protected static String KEY_SELECTED_TAB = "selectedTab";


    private TabLayout tabLayout;
    private AbstractTabAdapter tabAdapter;
    private Enum<?> selectedTab;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tabAdapter = null;
        if (savedInstanceState != null) {
            selectedTab = (Enum<?>) savedInstanceState.getSerializable(KEY_SELECTED_TAB);
        } else if (getArguments() != null) {
            selectedTab = (Enum<?>) getArguments().getSerializable(KEY_SELECTED_TAB);
        }
        Timber.d("onCreate: %1$s", selectedTab);
    }

    @Override public String getTitle() {
        if (this.tabAdapter != null && this.selectedTab != null) {
            return tabAdapter.getFragmentName(
                    tabAdapter.getTabIndex(this.selectedTab));
        }
        return null;
    }

    @Override public int getLayoutResourceId() {
        return R.layout.layout_fragment_container_view_and_tab_layout_above;
    }

    @Override public View configureView(View view, Bundle savedInstanceState) {
        Timber.d("configureView");
        tabLayout = (TabLayout) view.findViewById(R.id.tabLayout);
        return view;
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Timber.d("onSaveInstanceState: %1$s", selectedTab);
        savedInstanceState.putSerializable(KEY_SELECTED_TAB, selectedTab);
    }


    public void initializeViewPagerAndTabLayout(AbstractTabAdapter adapter) {
        Timber.d("initializeViewPagerAndTabLayout: %1$s", selectedTab);
        this.tabAdapter = adapter;
        if (this.selectedTab == null) {
            this.selectedTab = adapter.getDefaultTab();
            Timber.d("initializeViewPagerAndTabLayout set default tab: %1$s", selectedTab);
        }

        // prepare tab layout
        tabLayout.clearOnTabSelectedListeners();
        tabLayout.removeAllTabs();
        for (int i=0; i<tabAdapter.getTabList().size(); i++) {
            TabLayout.Tab tabLayoutTab = tabLayout.newTab();
            tabLayoutTab.setText(tabAdapter.getFragmentName(i));
            tabLayoutTab.setContentDescription(tabAdapter.getFragmentDescription(i));
            tabLayout.addTab(tabLayoutTab);
        }

        tabLayout.selectTab(null);
        tabLayout.addOnTabSelectedListener(this);
        tabLayout.setVisibility(
                tabAdapter.getTabList().size() > 1 ? View.VISIBLE : View.GONE);
        setSelectedTab();
    }

    public void changeTab(Enum<?> newTab) {
        if (tabLayout != null && newTab != null) {
            selectedTab = newTab;
            setSelectedTab();
        }
    }

    private void setSelectedTab() {
        tabLayout.selectTab(
                tabLayout.getTabAt(
                    tabAdapter.getTabIndex(selectedTab)));
    }

    // TabLayout.OnTabSelectedListener

    @Override public void onTabSelected(TabLayout.Tab tabLayoutTab) {
        selectedTab = tabAdapter.getTab(tabLayoutTab.getPosition());
        String tag = selectedTab.name();
        Timber.d("onTabSelected: selectedTab=%1$s", selectedTab);

        Fragment fragment = getChildFragmentManager().findFragmentByTag(tag) != null
            ? getChildFragmentManager().findFragmentByTag(tag)
            : tabAdapter.getFragment(tabLayoutTab.getPosition());
        Timber.d("fragment %1$s for tag %2$s", (fragment != null), tag);
        getChildFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainerViewSubTabs, fragment, tag)
            .addToBackStack(null)
            .commit();
        getChildFragmentManager().executePendingTransactions();
        updateToolbarTitle();

        if (fragment instanceof NavigateFragment
                && getChildFragmentManager().findFragmentByTag(tag) != null) {
            // only call the loadNewRouteFromSettings function, if the fragment is already attached
            // otherwise the fragment throws a NullPointerException
            ((NavigateFragment) fragment).loadNewRouteFromSettings();
        }
    }

    @Override public void onTabUnselected(TabLayout.Tab tab) {
        Timber.d("onTabUnselected");
    }

    @Override public void onTabReselected(TabLayout.Tab tab) {
        Timber.d("onTabReselected: selectedTab=%1$s", selectedTab);
        Fragment fragment = getChildFragmentManager().findFragmentByTag(selectedTab.name());
        if (fragment instanceof NavigateFragment) {
            ((NavigateFragment) fragment).loadNewRouteFromSettings();
        }
    }


    public abstract class AbstractTabAdapter {
        private ArrayList<? extends Enum> tabList;

        public AbstractTabAdapter(ArrayList<? extends Enum> tabList) {
            this.tabList = tabList;
        }

        public abstract Enum<?> getDefaultTab();
        public abstract Fragment getFragment(int position);

        public String getFragmentName(int position) {
            Enum<?> tab = getTab(position);
            if (tab != null) {
                return tab.toString();
            }
            return null;
        }

        public ArrayList<? extends Enum> getTabList() {
            return this.tabList;
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

        public String getFragmentDescription(int position) {
            return getFragmentName(position);
        }
    }

}
