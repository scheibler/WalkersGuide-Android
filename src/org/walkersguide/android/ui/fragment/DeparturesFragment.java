package org.walkersguide.android.ui.fragment;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.PointWrapper;
import org.walkersguide.android.data.basic.wrapper.Station;
import org.walkersguide.android.data.station.Departure;
import org.walkersguide.android.listener.DepartureResultListener;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.server.DepartureManager;
import org.walkersguide.android.ui.activity.PointDetailsActivity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class DeparturesFragment extends Fragment
    implements FragmentCommunicator, DepartureResultListener {

	// Store instance variables
    private Station station;

	// ui components
    private ListView listViewDepartures;
    private TextView labelEmptyDepartureListView;

	// newInstance constructor for creating fragment with arguments
	public static DeparturesFragment newInstance(PointWrapper pointWrapper) {
		DeparturesFragment departuresFragmentInstance = new DeparturesFragment();
        Bundle args = new Bundle();
        try {
            args.putString("jsonPointSerialized", pointWrapper.toJson().toString());
        } catch (JSONException e) {
            args.putString("jsonPointSerialized", "");
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
			((PointDetailsActivity) activity).pointDetailsFragmentCommunicator = this;
		}
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_departures, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        try {
            station = new Station(
                    getActivity(), new JSONObject(getArguments().getString("jsonPointSerialized", "")));
        } catch (JSONException e) {
            station = null;
        }

        ImageButton buttonRefreshDepartureList = (ImageButton) view.findViewById(R.id.buttonRefreshDepartureList);
        buttonRefreshDepartureList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                DepartureManager.getInstance(getActivity())
                    .requestDepartureList(DeparturesFragment.this, station);
            }
        });

        // departure layout
        listViewDepartures = (ListView) view.findViewById(R.id.listViewDepartures);
        labelEmptyDepartureListView = (TextView) view.findViewById(R.id.labelEmptyDepartureListView);
        listViewDepartures.setEmptyView(labelEmptyDepartureListView);
    }

    @Override public void onFragmentEnabled() {
        listViewDepartures.setAdapter(null);
        labelEmptyDepartureListView.setText(
                getResources().getString(R.string.labelRequestingDepartures));
        DepartureManager.getInstance(getActivity()).requestDepartureList(DeparturesFragment.this, station);
    }

	@Override public void onFragmentDisabled() {
        DepartureManager.getInstance(getActivity()).cancelDepartureRequest();
    }

	@Override public void departureQuerySuccessful(ArrayList<Departure> departureList) {
        listViewDepartures.setAdapter(
                new ArrayAdapter<Departure>(
                    getActivity(), android.R.layout.simple_list_item_1, departureList));
        labelEmptyDepartureListView.setText(
                getResources().getString(R.string.labelNoDeparturesFound));
    }

    @Override public void departureQueryFailed(String error) {
        listViewDepartures.setAdapter(null);
        labelEmptyDepartureListView.setText(error);
    }

}
