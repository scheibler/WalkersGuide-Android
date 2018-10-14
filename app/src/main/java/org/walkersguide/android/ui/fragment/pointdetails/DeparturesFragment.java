package org.walkersguide.android.ui.fragment.pointdetails;

import android.app.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;

import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.point.Station;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.station.Departure;
import org.walkersguide.android.listener.DepartureResultListener;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.R;
import org.walkersguide.android.server.DepartureManager;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.util.Constants;


public class DeparturesFragment extends Fragment
    implements FragmentCommunicator, DepartureResultListener {

	// Store instance variables
    private DepartureManager departureManagerInstance;
    private Station station;
    private int listPosition;

    // query in progress
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private Vibrator vibrator;

	// ui components
    private ImageButton buttonRefresh;
    private ListView listViewDepartures;
    private TextView labelHeading, labelEmptyListView;

	// newInstance constructor for creating fragment with arguments
	public static DeparturesFragment newInstance(PointWrapper pointWrapper) {
		DeparturesFragment departuresFragmentInstance = new DeparturesFragment();
        Bundle args = new Bundle();
        try {
            args.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, pointWrapper.toJson().toString());
        } catch (JSONException e) {
            args.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
        }
        departuresFragmentInstance.setArguments(args);
		return departuresFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
			((PointDetailsActivity) activity).departuresFragmentCommunicator = this;
		}
        departureManagerInstance = DepartureManager.getInstance(getActivity());
        // progress updater
        this.progressHandler = new Handler();
        this.progressUpdater = new ProgressUpdater();
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.layout_heading_and_list_view_with_refresh_button, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        try {
            station = new Station(
                    getActivity(), new JSONObject(getArguments().getString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "")));
        } catch (JSONException e) {
            station = null;
        }

        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt("listPosition");
        } else {
            listPosition = 0;
        }

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (departureManagerInstance.departureRequestInProgress()) {
                    departureManagerInstance.cancelDepartureRequest();
                } else {
                    requestNextDepartures();
                }
            }
        });

        // list view
        listViewDepartures = (ListView) view.findViewById(R.id.listView);
        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        listViewDepartures.setEmptyView(labelEmptyListView);
    }

    @Override public void onFragmentEnabled() {
        // listen for device shakes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_SHAKE_DETECTED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // request next departures
        requestNextDepartures();
    }

	@Override public void onFragmentDisabled() {
        departureManagerInstance.invalidateDepartureRequest(this);
        progressHandler.removeCallbacks(progressUpdater);
        // unregister shake broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("listPosition",  listPosition);
    }

    private void requestNextDepartures() {
        // heading
        labelHeading.setText(
                getResources().getString(R.string.messagePleaseWait));
        buttonRefresh.setContentDescription(
                getResources().getString(R.string.buttonCancel));
        buttonRefresh.setImageResource(R.drawable.cancel);
        // list view
        listViewDepartures.setAdapter(null);
        listViewDepartures.setOnScrollListener(null);
        labelEmptyListView.setText("");
        // request next departures
        progressHandler.postDelayed(progressUpdater, 2000);
        departureManagerInstance.requestDepartureList(DeparturesFragment.this, station);
    }

	@Override public void departureRequestFinished(int returnCode, String returnMessage, ArrayList<Departure> departureList, boolean resetListPosition) {
        buttonRefresh.setContentDescription(
                getResources().getString(R.string.buttonRefresh));
        buttonRefresh.setImageResource(R.drawable.refresh);
        progressHandler.removeCallbacks(progressUpdater);

        if (departureList != null) {
            labelHeading.setText(
                    String.format(
                        getResources().getString(R.string.labelNextDeparturesSuccess),
                        getResources().getQuantityString(
                            R.plurals.departure, departureList.size(), departureList.size()))
                    );
            listViewDepartures.setAdapter(
                    new ArrayAdapter<Departure>(
                        getActivity(), android.R.layout.simple_list_item_1, departureList));

            // list position
            if (resetListPosition) {
                listViewDepartures.setSelection(0);
            } else {
                listViewDepartures.setSelection(listPosition);
            }
            listViewDepartures.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
                @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (listPosition != firstVisibleItem) {
                        listPosition = firstVisibleItem;
                    }
                }
            });

        } else {
            labelHeading.setText(getResources().getString(R.string.fragmentDeparturesName));
            labelEmptyListView.setText(returnMessage);
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                // reload
                vibrator.vibrate(250);
                requestNextDepartures();
            }
        }
    };


    private class ProgressUpdater implements Runnable {
        public void run() {
            vibrator.vibrate(50);
            progressHandler.postDelayed(this, 2000);
        }
    }

}
