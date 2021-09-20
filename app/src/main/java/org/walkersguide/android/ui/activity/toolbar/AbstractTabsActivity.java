package org.walkersguide.android.ui.activity.toolbar;

import org.walkersguide.android.ui.dialog.SendFeedbackDialog;
import android.content.Intent;

import android.os.Bundle;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;

import java.util.TreeSet;

import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.ui.dialog.HikingTrailsDialog;
import org.walkersguide.android.ui.activity.AbstractToolbarActivity;
import org.walkersguide.android.util.Constants;
import android.view.Menu;
import android.widget.TextView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.widget.ViewPager2;
import androidx.fragment.app.FragmentManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import java.util.ArrayList;
import java.util.Arrays;
import android.view.View;
import timber.log.Timber;


public abstract class AbstractTabsActivity extends AbstractToolbarActivity {
    private static final String KEY_TAB_INDEX = "tabIndex";
    private static final int NO_TAB_SELECTED = -1;

    public abstract void tabSelected(int tabIndex);

	public abstract class AbstractTabAdapter extends FragmentStateAdapter {
        public AbstractTabAdapter(FragmentActivity activity) {
            super(activity);
        }
        public abstract String getFragmentName(int position);
	}


	private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private int tabIndex;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Timber.d("onCreate");

        if (savedInstanceState != null) {
            tabIndex = savedInstanceState.getInt(KEY_TAB_INDEX, NO_TAB_SELECTED);
        } else {
            tabIndex = NO_TAB_SELECTED;
        }

		viewPager = (ViewPager2) findViewById(R.id.pager);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                Timber.d("onPageSelected: %1$d", position);
                tabLayout.selectTab(tabLayout.getTabAt(position));
                setToolbarTitle(
                        tabLayout.getTabAt(position).getText().toString());
                tabIndex = position;
                // notify child activities
                tabSelected(tabIndex);
            }
        });

		tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                Timber.d("onTabSelected");
                viewPager.setCurrentItem(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

	@Override public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
    	savedInstanceState.putInt(KEY_TAB_INDEX, tabIndex);
    }

    public void initializeViewPagerAndTabLayout(AbstractTabAdapter tabAdapter, int initialTabIndex) {
        Timber.d("initializeViewPagerAndTabLayout: %1$d == %2$d", tabIndex, initialTabIndex);
        if (tabIndex == NO_TAB_SELECTED) {
            tabIndex = initialTabIndex;
        }

        // add tabs
        for (int i=0; i<tabAdapter.getItemCount(); i++) {
            tabLayout.addTab(
                    tabLayout.newTab().setText(tabAdapter.getFragmentName(i)));
        }
        if (tabAdapter.getItemCount() <= 1) {
            tabLayout.setVisibility(View.GONE);
        } else {
            tabLayout.setVisibility(View.VISIBLE);
        }

        // initialize view pager
        viewPager.setAdapter(tabAdapter);
        viewPager.setCurrentItem(tabIndex);
    }

}
