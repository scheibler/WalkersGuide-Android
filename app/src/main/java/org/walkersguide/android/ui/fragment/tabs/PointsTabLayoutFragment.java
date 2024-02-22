package org.walkersguide.android.ui.fragment.tabs;

import org.walkersguide.android.ui.fragment.profile_list.PoiProfileListFragment;
import org.walkersguide.android.ui.fragment.TabLayoutFragment;
import org.walkersguide.android.ui.fragment.TabLayoutFragment.AbstractTabAdapter;
import org.walkersguide.android.R;


import android.os.Bundle;



import android.view.View;



import androidx.fragment.app.Fragment;



import java.util.ArrayList;
import java.util.Arrays;
import org.walkersguide.android.util.GlobalInstance;


public class PointsTabLayoutFragment extends TabLayoutFragment {

    public static PointsTabLayoutFragment newInstance(Enum<?> selectedTab) {
        PointsTabLayoutFragment fragment = new PointsTabLayoutFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_TAB, selectedTab);
        fragment.setArguments(args);
        return fragment;
    }


    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViewPagerAndTabLayout(new PointsTabAdapter());
    }


    /**
     * tabs
     */

    public enum Tab {
        NEARBY(GlobalInstance.getStringResource(R.string.fragmentPoiProfileListNameEmbedded));

        public String label;
        private Tab(String label) {
            this.label = label;
        }

        @Override public String toString() {
            return this.label;
        }
    }


    private class PointsTabAdapter extends AbstractTabAdapter {

        public PointsTabAdapter() {
            super(new ArrayList<Tab>(Arrays.asList(Tab.values())));
        }

        @Override public Enum<?> getDefaultTab() {
            return Tab.NEARBY;
        }

        @Override public Fragment getFragment(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case NEARBY:
                        return PoiProfileListFragment.embedded();
                }
            }
            return null;
        }
    }

}
