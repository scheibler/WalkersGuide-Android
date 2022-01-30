package org.walkersguide.android.ui.fragment.object_list.simple;

import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.Angle;
import org.walkersguide.android.util.TTSWrapper;
import org.walkersguide.android.sensor.bearing.AcceptNewQuadrant;
import org.walkersguide.android.ui.fragment.object_list.SimpleObjectListFragment;
import org.walkersguide.android.ui.fragment.ObjectListFragment.ObjectWithIdAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;




import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.ObjectWithId;
import java.util.ArrayList;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;


public class IntersectionStructureFragment extends SimpleObjectListFragment {


	public static IntersectionStructureFragment newInstance(ArrayList<IntersectionSegment> intersectionSegmentList) {
		IntersectionStructureFragment fragment = new IntersectionStructureFragment();
        fragment.setArguments(
                SimpleObjectListFragment.createArgsBundle(intersectionSegmentList));
		return fragment;
	}


    @Override public String getDialogTitle() {
    	return getResources().getString(R.string.fragmentIntersectionStructureName);
    }

    @Override public int getPluralResourceId() {
        return R.plurals.street;
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        // listen for bearing changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // request current direction to update the ui
        DeviceSensorManager.getInstance().requestCurrentBearing();
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        private AcceptNewQuadrant acceptNewQuadrant = AcceptNewQuadrant.newInstanceForObjectListSort();

        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                Bearing currentBearing = (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (acceptNewQuadrant.updateQuadrant(currentBearing.getQuadrant())) {
                    sortListAndAnnounceSegmentStraightAhead();
                }
            }
        }
    };

    @Override public void  successfulViewPopulationFinished() {
        sortListAndAnnounceSegmentStraightAhead();
    }


    private void sortListAndAnnounceSegmentStraightAhead() {
        ObjectWithIdAdapter adapter = getListAdapter();
        if (adapter != null) {
            boolean newItemOnTop = adapter.sortObjectList(
                    // bearing offset = 68 -> sort the ways, which are slightly to the left of the user, to the top of the list
                    new ObjectWithId.SortByBearingRelativeToCurrentBearing(
                        Angle.Quadrant.Q1.max, true));
            if (newItemOnTop) {
                TTSWrapper.getInstance()
                    .announceToScreenReader(adapter.getItem(0).getName());
            }
        }
    }

}
