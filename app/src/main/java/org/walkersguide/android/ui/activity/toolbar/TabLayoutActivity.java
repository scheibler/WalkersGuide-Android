package org.walkersguide.android.ui.activity.toolbar;


import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;



import org.walkersguide.android.R;
import org.walkersguide.android.ui.activity.ToolbarActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.fragment.app.FragmentActivity;
import java.util.ArrayList;
import android.view.View;
import timber.log.Timber;


public abstract class TabLayoutActivity extends ToolbarActivity {
    public static String KEY_SELECTED_TAB = "selectedTab";

	private ViewPager2 viewPager;
    private TabLayout tabLayout;

    private Enum<?> selectedTab;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		viewPager = (ViewPager2) findViewById(R.id.pager);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                Timber.d("onPageSelected: %1$d", position);
                tabLayout.selectTab(tabLayout.getTabAt(position));
                setToolbarTitle(
                        tabLayout.getTabAt(position).getText().toString());
                // notify child activities
                tabSelected(
                        ((AbstractTabAdapter) viewPager.getAdapter()).getTab(position));
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
            selectedTab = (Enum<?>) savedInstanceState.getSerializable(KEY_SELECTED_TAB);
        } else if (getIntent() != null && getIntent().getExtras() != null) {
            selectedTab = (Enum<?>) getIntent().getExtras().getSerializable(KEY_SELECTED_TAB);
        }
    }

    public void initializeViewPagerAndTabLayout(AbstractTabAdapter tabAdapter) {
        initializeViewPagerAndTabLayout(tabAdapter, null);
    }

    public void initializeViewPagerAndTabLayout(AbstractTabAdapter tabAdapter, Enum<?> newTab) {
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
    }

	@Override public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
    	savedInstanceState.putSerializable(KEY_SELECTED_TAB, selectedTab);
    }


	public abstract class AbstractTabAdapter extends FragmentStateAdapter {
        private ArrayList<? extends Enum> tabList;

        public AbstractTabAdapter(FragmentActivity activity, ArrayList<? extends Enum> tabList) {
            super(activity);
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
