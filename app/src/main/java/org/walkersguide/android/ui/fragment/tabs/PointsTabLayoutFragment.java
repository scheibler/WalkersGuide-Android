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


public class PointsTabLayoutFragment extends TabLayoutFragment {

    public static PointsTabLayoutFragment newInstance() {
        PointsTabLayoutFragment fragment = new PointsTabLayoutFragment();
        return fragment;
    }


    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViewPagerAndTabLayout(
                new PointsTabAdapter(PointsTabLayoutFragment.this), Tab.NEARBY);
    }


    /**
     * tabs
     */

    private enum Tab {
        NEARBY
    }


    private class PointsTabAdapter extends AbstractTabAdapter {

        public PointsTabAdapter(Fragment fragment) {
            super(fragment, new ArrayList<Tab>(Arrays.asList(Tab.values())));
        }

        @Override public Fragment createFragment(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case NEARBY:
                        return PoiProfileListFragment.embedded();
                }
            }
            return null;
        }

        @Override public String getFragmentName(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case NEARBY:
                        return getResources().getString(R.string.fragmentPoiProfileListNameEmbedded);
                }
            }
            return null;
        }
    }

}
