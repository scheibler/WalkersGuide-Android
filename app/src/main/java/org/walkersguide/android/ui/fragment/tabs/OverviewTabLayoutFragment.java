package org.walkersguide.android.ui.fragment.tabs;

import org.walkersguide.android.ui.fragment.tabs.overview.PinningFragment;
import org.walkersguide.android.ui.fragment.tabs.overview.TrackingFragment;
import org.walkersguide.android.ui.fragment.TabLayoutFragment;
import org.walkersguide.android.ui.fragment.TabLayoutFragment.AbstractTabAdapter;
import org.walkersguide.android.R;


import android.os.Bundle;



import android.view.View;



import androidx.fragment.app.Fragment;



import java.util.ArrayList;
import java.util.Arrays;
import org.walkersguide.android.ui.view.ResolveCurrentAddressView;
import android.widget.Button;
import org.walkersguide.android.ui.fragment.profile_list.CollectionListFragment;
import org.walkersguide.android.ui.fragment.HistoryFragment;


public class OverviewTabLayoutFragment extends TabLayoutFragment {

    public static OverviewTabLayoutFragment newInstance() {
        OverviewTabLayoutFragment fragment = new OverviewTabLayoutFragment();
        return fragment;
    }


    private ResolveCurrentAddressView layoutClosestAddress;

    @Override public int getLayoutResourceId() {
        return R.layout.fragment_overview;
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutClosestAddress = (ResolveCurrentAddressView) view.findViewById(R.id.layoutClosestAddress);
        layoutClosestAddress.requestAddressForCurrentLocation();

        Button buttonCollections = (Button) view.findViewById(R.id.buttonCollections);
        buttonCollections.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.addFragment(
                        CollectionListFragment.newInstance());
            }
        });

        Button buttonHistory = (Button) view.findViewById(R.id.buttonHistory);
        buttonHistory.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.addFragment(
                        HistoryFragment.newInstance());
            }
        });

        initializeViewPagerAndTabLayout(
                new OverviewTabAdapter(OverviewTabLayoutFragment.this), Tab.PINNING);
    }


    /**
     * tabs
     */

    private enum Tab {
        PINNING, TRACKING
    }


    private class OverviewTabAdapter extends AbstractTabAdapter {

        public OverviewTabAdapter(Fragment fragment) {
            super(fragment, new ArrayList<Tab>(Arrays.asList(Tab.values())));
        }

        @Override public Fragment createFragment(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case PINNING:
                        return PinningFragment.newInstance();
                    case TRACKING:
                        return TrackingFragment.newInstance();
                }
            }
            return null;
        }

        @Override public String getFragmentName(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case PINNING:
                        return getResources().getString(R.string.fragmentPinningName);
                    case TRACKING:
                        return getResources().getString(R.string.fragmentTrackingName);
                }
            }
            return null;
        }
    }

}
