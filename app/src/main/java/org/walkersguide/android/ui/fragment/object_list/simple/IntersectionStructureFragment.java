package org.walkersguide.android.ui.fragment.object_list.simple;

import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.Angle;
import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.sensor.bearing.AcceptNewBearing;
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
import timber.log.Timber;
import android.view.Menu;
import android.view.MenuItem;
import org.walkersguide.android.data.angle.RelativeBearing;


public class IntersectionStructureFragment extends SimpleObjectListFragment {

	public static IntersectionStructureFragment newInstance(ArrayList<IntersectionSegment> intersectionSegmentList) {
		IntersectionStructureFragment fragment = new IntersectionStructureFragment();
        fragment.setArguments(
                new SimpleObjectListFragment.BundleBuilder(intersectionSegmentList)
                .setAnnounceObjectAhead(true)
                .build());
		return fragment;
	}


    @Override public String getTitle() {
    	return getResources().getString(R.string.fragmentIntersectionStructureName);
    }

    @Override public int getPluralResourceId() {
        return R.plurals.street;
    }


    /**
     * pause and resume
     */

    @Override public void  successfulViewPopulationFinished() {
        // add custom comparator
        ObjectWithIdAdapter adapter = getListAdapter();
        if (adapter != null) {
            adapter.setListComparator(
                    // bearing offset = 68 -> sort the ways, which are slightly to the left of the user, to the top of the list
                    ObjectWithId.SortByBearingRelativeTo.currentBearing(
                        Angle.Quadrant.Q1.max, true));
            adapter.notifyDataSetChanged();
        }
    }

}
