package org.walkersguide.android.ui.fragment.tabs;

import android.widget.ImageButton;
import org.walkersguide.android.ui.fragment.tabs.overview.PinFragment;
import org.walkersguide.android.ui.fragment.tabs.overview.TrackFragment;
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
import timber.log.Timber;
import org.walkersguide.android.util.GlobalInstance;


public class OverviewTabLayoutFragment extends TabLayoutFragment {

    public static OverviewTabLayoutFragment newInstance(Enum<?> selectedTab) {
        OverviewTabLayoutFragment fragment = new OverviewTabLayoutFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_TAB, selectedTab);
        fragment.setArguments(args);
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

        ImageButton buttonCollections = (ImageButton) view.findViewById(R.id.buttonCollections);
        buttonCollections.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.addFragment(
                        CollectionListFragment.newInstance());
            }
        });

        ImageButton buttonHistory = (ImageButton) view.findViewById(R.id.buttonHistory);
        buttonHistory.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.addFragment(
                        HistoryFragment.newInstance());
            }
        });

        initializeViewPagerAndTabLayout(new OverviewTabAdapter());
    }


    /**
     * tabs
     */

    public enum Tab {
        PIN(GlobalInstance.getStringResource(R.string.fragmentPinName)),
        TRACK(GlobalInstance.getStringResource(R.string.fragmentTrackName));

        public String label;
        private Tab(String label) {
            this.label = label;
        }

        @Override public String toString() {
            return this.label;
        }
    }


    private class OverviewTabAdapter extends AbstractTabAdapter {

        public OverviewTabAdapter() {
            super(new ArrayList<Tab>(Arrays.asList(Tab.values())));
        }

        @Override public Enum<?> getDefaultTab() {
            return Tab.PIN;
        }

        @Override public Fragment getFragment(int position) {
            Tab tab = getTab(position);
            Timber.d("createFragment: position=%1$d, tab=%2$s", position, tab);
            if (tab != null) {
                switch (tab) {
                    case PIN:
                        return PinFragment.newInstance();
                    case TRACK:
                        return TrackFragment.newInstance();
                }
            }
            return null;
        }
    }

}
