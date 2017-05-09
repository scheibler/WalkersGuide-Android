package org.walkersguide.android.ui.fragment;

import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.POI;
import org.walkersguide.android.data.basic.point.PointWrapper;
import org.walkersguide.android.data.basic.point.PointWrapper.SortByDistanceFromCurrentPosition;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.adapter.PointWrapperAdapter;
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

public class EntrancesFragment extends Fragment implements FragmentCommunicator {

	// Store instance variables
    private ArrayList<PointWrapper> entranceList;

	// ui components
    private ListView listViewEntrances;

	// newInstance constructor for creating fragment with arguments
	public static EntrancesFragment newInstance(PointWrapper pointWrapper) {
		EntrancesFragment entrancesFragmentInstance = new EntrancesFragment();
        Bundle args = new Bundle();
        try {
            args.putString("jsonPointSerialized", pointWrapper.toJson().toString());
        } catch (JSONException e) {
            args.putString("jsonPointSerialized", "");
        }
        entrancesFragmentInstance.setArguments(args);
		return entrancesFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
			((PointDetailsActivity) activity).entrancesFragmentCommunicator = this;
		}
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_entrances, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        try {
            entranceList = new POI(
                    getActivity(), new JSONObject(getArguments().getString("jsonPointSerialized", "")))
                .getEntranceList();
        } catch (JSONException e) {
            entranceList = new ArrayList<PointWrapper>();
        }
        Collections.sort(entranceList, new SortByDistanceFromCurrentPosition());

        TextView labelFragmentHeader = (TextView) view.findViewById(R.id.labelFragmentHeader);
        if (entranceList.size() == 1) {
            labelFragmentHeader.setText(
                    getResources().getString(R.string.labelNumberOfEntrancesSingular));
        } else {
            labelFragmentHeader.setText(
                    String.format(
                        getResources().getString(R.string.labelNumberOfEntrancesPlural),
                        entranceList.size())
                    );
        }

        listViewEntrances = (ListView) view.findViewById(R.id.listViewEntrances);
        listViewEntrances.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                PointWrapper pointWrapper = (PointWrapper) parent.getItemAtPosition(position);
                System.out.println("xxx entrance clicked: " + pointWrapper.toString());
                Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                try {
                    detailsIntent.putExtra("jsonPoint", pointWrapper.toJson().toString());
                } catch (JSONException e) {
                    detailsIntent.putExtra("jsonPoint", "");
                }
                startActivity(detailsIntent);
            }
        });
    }

    @Override public void onFragmentEnabled() {
        listViewEntrances.setAdapter(
                new PointWrapperAdapter(getActivity(), entranceList));
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
            if (
                    (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                        && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD1.ID)
                    || (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)
                        && intent.getIntExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_THRESHOLD_ID, -1) >= DirectionManager.THRESHOLD2.ID)
                    ) {
                PointWrapperAdapter pointWrapperAdapter = (PointWrapperAdapter) listViewEntrances.getAdapter();
                if (pointWrapperAdapter != null) {
                    pointWrapperAdapter.notifyDataSetChanged();
                }
            }
        }
    };

}
