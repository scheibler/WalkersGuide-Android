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


public abstract class SimpleObjectListFragment extends ObjectListFragment {

    public abstract void  successfulViewPopulationFinished();

    public static Bundle createArgsBundle(ArrayList<? extends ObjectWithId> objectList) {
        Bundle args = ObjectListFragment.createArgsBundle(null);
        args.putSerializable(KEY_OBJECT_LIST, objectList);
        return args;
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
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
    }

    @Override public void onResume() {
        super.onResume();
    }

    @Override public void requestUiUpdate() {
        this.prepareRequest();
        if (objectList != null) {
            super.populateUiAfterRequestWasSuccessful(
                    GlobalInstance.getPluralResource(
                        getPluralResourceId(), objectList.size()),
                    objectList, true);
            successfulViewPopulationFinished();
        } else {
            super.populateUiAfterRequestFailed("");
        }
    }

    @Override public void requestMoreResults() {
    }

}
