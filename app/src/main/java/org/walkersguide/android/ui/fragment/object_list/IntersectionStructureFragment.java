package org.walkersguide.android.ui.fragment.object_list;

import org.walkersguide.android.ui.fragment.ObjectListFragment.ObjectWithIdAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;




import org.walkersguide.android.data.sensor.attribute.NewDirectionAttributes;
import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.data.sensor.threshold.BearingThreshold;
import org.walkersguide.android.util.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.ObjectWithId;


public class IntersectionStructureFragment extends SimpleObjectListFragment {

    @Override public String getDialogTitle() {
    	return getResources().getString(R.string.fragmentIntersectionStructureName);
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        // listen for direction changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_DIRECTION);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
    }

    public void requestUiUpdate() {
        super.requestUiUpdate();
        ObjectWithIdAdapter adapter = (ObjectWithIdAdapter) listViewObject.getAdapter();
        Direction currentDirection = DirectionManager.getInstance().getCurrentDirection();
        if (adapter != null && currentDirection != null) {
            labelHeading.setText(
                    String.format(
                        GlobalInstance.getStringResource(R.string.labelNumberOfIntersectionWaysAndBearing),
                        GlobalInstance.getPluralResource(R.plurals.street, adapter.getCount()),
                        StringUtility.formatGeographicDirection(currentDirection.getBearing()))
                    );
            adapter.sortObjectList(
                    // bearing offset = 68 -> sort the ways, which are slightly to the left of the user, to the top of the list
                    new ObjectWithId.SortByBearingFromCurrentDirection(68, true));
        }
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)) {
                NewDirectionAttributes newDirectionAttributes = NewDirectionAttributes.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_DIRECTION_ATTRIBUTES));
                if (newDirectionAttributes != null
                        && newDirectionAttributes.getAggregatingBearingThreshold().isAtLeast(BearingThreshold.FIFTEEN_DEGREES)) {
                    requestUiUpdate();
                }
            }
        }
    };

}
