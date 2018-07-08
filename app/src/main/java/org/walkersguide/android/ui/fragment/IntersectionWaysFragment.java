package org.walkersguide.android.ui.fragment;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.Intersection;
import org.walkersguide.android.data.basic.segment.IntersectionSegment;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.activity.SegmentDetailsActivity;
import org.walkersguide.android.ui.adapter.IntersectionSegmentAdapter;
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

public class IntersectionWaysFragment extends Fragment implements FragmentCommunicator {

	// Store instance variables
    private ArrayList<IntersectionSegment> intersectionSegmentList;

	// ui components
    private ListView listViewIntersectionWays;

	// newInstance constructor for creating fragment with arguments
	public static IntersectionWaysFragment newInstance(PointWrapper pointWrapper) {
		IntersectionWaysFragment intersectionWaysFragmentInstance = new IntersectionWaysFragment();
        Bundle args = new Bundle();
        try {
            args.putString("jsonPointSerialized", pointWrapper.toJson().toString());
        } catch (JSONException e) {
            args.putString("jsonPointSerialized", "");
        }
        intersectionWaysFragmentInstance.setArguments(args);
		return intersectionWaysFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
			((PointDetailsActivity) activity).intersectionWaysFragmentCommunicator = this;
		}
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_intersection_ways, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        try {
            intersectionSegmentList = new Intersection(
                    getActivity(), new JSONObject(getArguments().getString("jsonPointSerialized", "")))
                .getSegmentList();
        } catch (JSONException e) {
            intersectionSegmentList = new ArrayList<IntersectionSegment>();
        }

        TextView labelFragmentHeader = (TextView) view.findViewById(R.id.labelFragmentHeader);
        labelFragmentHeader.setText(
                String.format(
                    getResources().getString(R.string.labelNumberOfIntersectionWaysAndBearing),
                    intersectionSegmentList.size(),
                    StringUtility.formatGeographicDirection(
                        getActivity(), DirectionManager.getInstance(getActivity()).getCurrentDirection()))
                );

        listViewIntersectionWays = (ListView) view.findViewById(R.id.listViewIntersectionWays);
        listViewIntersectionWays.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                IntersectionSegment intersectionSegment = (IntersectionSegment) parent.getItemAtPosition(position);
                Intent detailsIntent = new Intent(getActivity(), SegmentDetailsActivity.class);
                try {
                    detailsIntent.putExtra("jsonSegmentSerialized", intersectionSegment.toJson().toString());
                } catch (JSONException e) {
                    detailsIntent.putExtra("jsonSegmentSerialized", "");
                }
                startActivity(detailsIntent);
            }
        });
    }

    @Override public void onFragmentEnabled() {
        listViewIntersectionWays.setAdapter(
                new IntersectionSegmentAdapter(getActivity(), intersectionSegmentList));
        // listen for direction changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_DIRECTION);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(newLocationAndDirectionReceiver, filter);
        // request current direction value
        DirectionManager.getInstance(getActivity()).requestCurrentDirection();
    }

	@Override public void onFragmentDisabled() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(newLocationAndDirectionReceiver);
    }


    private BroadcastReceiver newLocationAndDirectionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)
                    && intent.getIntExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_THRESHOLD_ID, -1) >= DirectionManager.THRESHOLD1.ID) {
                IntersectionSegmentAdapter intersectionSegmentAdapter = (IntersectionSegmentAdapter) listViewIntersectionWays.getAdapter();
                if (intersectionSegmentAdapter != null) {
                    intersectionSegmentAdapter.notifyDataSetChanged();
                }
            }
        }
    };

}
