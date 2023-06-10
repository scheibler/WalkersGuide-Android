package org.walkersguide.android.ui.fragment.object_list;

import org.walkersguide.android.ui.fragment.ObjectListFragment;
import org.walkersguide.android.data.ObjectWithId;

import java.util.ArrayList;

import android.os.Bundle;

import android.view.View;

import androidx.core.view.ViewCompat;

import org.walkersguide.android.R;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.Entrance;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.POI;
import org.walkersguide.android.data.object_with_id.point.PedestrianCrossing;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.Point;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;


public abstract class SimpleObjectListFragment extends ObjectListFragment {

    public abstract void  successfulViewPopulationFinished();

    public static class BundleBuilder extends ObjectListFragment.BundleBuilder {
        public BundleBuilder(ArrayList<? extends ObjectWithId> objectList) {
            super();
            bundle.putSerializable(KEY_OBJECT_LIST, objectList);
            setAutoUpdate(true);
        }
    }


    // dialog
    private static final String KEY_OBJECT_LIST = "objectList";


    /**
     * create view
     */

    private ArrayList<? extends ObjectWithId> objectList;

	@Override public View configureView(View view, Bundle savedInstanceState) {
        view = super.configureView(view, savedInstanceState);
        objectList = (ArrayList<? extends ObjectWithId>) getArguments().getSerializable(KEY_OBJECT_LIST);
        return view;
    }


    /**
     * menu
     */

    @Override public void onPrepareMenu(@NonNull Menu menu) {
        super.onPrepareMenu(menu);
        // show auto update
        MenuItem menuItemAutoUpdate = menu.findItem(R.id.menuItemAutoUpdate);
        menuItemAutoUpdate.setVisible(true);
        // show announce object ahead
        MenuItem menuItemAnnounceObjectAhead = menu.findItem(R.id.menuItemAnnounceObjectAhead);
        menuItemAnnounceObjectAhead.setVisible(true);
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
    }

    @Override public void onResume() {
        super.onResume();
    }

    @Override public boolean isUiUpdateRequestInProgress() {
        return false;
    }

    @Override public void requestUiUpdate() {
        this.prepareRequest();
        if (objectList != null) {
            super.populateUiAfterRequestWasSuccessful(null, objectList, true, false);
            successfulViewPopulationFinished();
        } else {
            super.populateUiAfterRequestFailed("");
        }
    }

    @Override public void requestMoreResults() {
    }

}
