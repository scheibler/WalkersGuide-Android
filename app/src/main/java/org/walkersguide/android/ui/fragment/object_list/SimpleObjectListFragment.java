package org.walkersguide.android.ui.fragment.object_list;

import org.walkersguide.android.ui.fragment.ObjectListFragment;
import org.walkersguide.android.data.ObjectWithId;

import java.util.ArrayList;

import android.os.Bundle;

import android.view.View;

import androidx.core.view.ViewCompat;

import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.Entrance;
import org.walkersguide.android.data.basic.point.Intersection;
import org.walkersguide.android.data.basic.point.POI;
import org.walkersguide.android.data.basic.point.PedestrianCrossing;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.basic.point.Point;


public class SimpleObjectListFragment extends ObjectListFragment {
    private static final String KEY_OBJECT_LIST = "objectList";

    private ArrayList<? extends ObjectWithId> objectList;

	public static SimpleObjectListFragment newInstance(ArrayList<? extends ObjectWithId> objectList) {
		SimpleObjectListFragment fragment = new SimpleObjectListFragment();
        Bundle args = ObjectListFragment.createArgsBundle(false, false, false);
        args.putSerializable(KEY_OBJECT_LIST, objectList);
        fragment.setArguments(args);
		return fragment;
	}


    /**
     * create view
     */

	@Override public View configureView(View view, Bundle savedInstanceState) {
        view = super.configureView(view, savedInstanceState);
        objectList = (ArrayList<? extends ObjectWithId>) getArguments().getSerializable(KEY_OBJECT_LIST);
        return view;
    }

    @Override public void clickedButtonSelectProfile() {
    }

    @Override public void longClickedButtonSelectProfile() {
    }

    @Override public void clickedLabelEmptyListView() {
    }

    @Override public void searchTermChanged(String newSearchTerm) {
    }

    @Override public String getDialogTitle() {
        if (objectList != null) {
    		return getResources().getString(R.string.fragmentEntrancesName);
        } else {
    		return getResources().getString(R.string.fragmentPedestrianCrossingsName);
        }
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
    }

    @Override public void onResume() {
        super.onResume();
        requestUiUpdate();
    }


    /**
     * point list request and response
     */

    public void requestUiUpdate() {
        this.prepareRequest();

        // no further things required
        // fill list and heading
        super.updateRefreshButtonAfterRequestWasFinished();

        if (objectList != null) {
            ViewCompat.setAccessibilityLiveRegion(
                    labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
            labelHeading.setText(
                    GlobalInstance.getPluralResource(R.plurals.object, objectList.size()));
            super.updateListViewAfterRequestWasSuccessful(objectList);
        }
    }

    @Override public void prepareRequest() {
        super.prepareRequest();
    }

}
