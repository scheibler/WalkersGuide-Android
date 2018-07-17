package org.walkersguide.android.ui.fragment.segmentdetails;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.activity.SegmentDetailsActivity;
import org.walkersguide.android.util.Constants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

public class NextIntersectionsFragment extends Fragment implements FragmentCommunicator {

	// Store instance variables
    private SegmentWrapper segmentWrapper;

	// ui components
    private ListView listViewNextIntersections;

	// newInstance constructor for creating fragment with arguments
	public static NextIntersectionsFragment newInstance(SegmentWrapper segmentWrapper) {
		NextIntersectionsFragment nextIntersectionsFragmentInstance = new NextIntersectionsFragment();
        Bundle args = new Bundle();
        try {
            args.putString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, segmentWrapper.toJson().toString());
        } catch (JSONException e) {
            args.putString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "");
        }
        nextIntersectionsFragmentInstance.setArguments(args);
		return nextIntersectionsFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
			((SegmentDetailsActivity) activity).nextIntersectionsFragmentCommunicator = this;
		}
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_next_intersections, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        try {
            segmentWrapper = new SegmentWrapper(
                    getActivity(), new JSONObject(getArguments().getString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "")));
        } catch (JSONException e) {
            segmentWrapper = null;
        }

        TextView labelFragmentHeader = (TextView) view.findViewById(R.id.labelFragmentHeader);
        labelFragmentHeader.setText(
                getResources().getString(R.string.fragmentNextIntersectionsName));

        listViewNextIntersections = (ListView) view.findViewById(R.id.listViewNextIntersections);
        listViewNextIntersections.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
            }
        });
    }

    @Override public void onFragmentEnabled() {
        listViewNextIntersections.setAdapter(null);
        // listen for direction and position changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_LOCATION);
        filter.addAction(Constants.ACTION_NEW_DIRECTION);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(newLocationAndDirectionReceiver, filter);
        // request current location and direction values
        DirectionManager.getInstance(getActivity()).requestCurrentDirection();
        PositionManager.getInstance(getActivity()).requestCurrentLocation();
    }

	@Override public void onFragmentDisabled() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(newLocationAndDirectionReceiver);
    }

    private BroadcastReceiver newLocationAndDirectionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            /*
            if (
                    (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                        && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD1.ID)
                    || (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)
                        && intent.getIntExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_THRESHOLD_ID, -1) >= DirectionManager.THRESHOLD2.ID)
                    ) {
                PointWrapperAdapter pointWrapperAdapter = (PointWrapperAdapter) listViewNextIntersections.getAdapter();
                if (pointWrapperAdapter != null) {
                    pointWrapperAdapter.notifyDataSetChanged();
                }
            }
            */
        }
    };

}
