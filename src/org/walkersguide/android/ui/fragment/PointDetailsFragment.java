package org.walkersguide.android.ui.fragment;

import java.util.ArrayList;

import org.json.JSONException;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.PedestrianCrossing;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.util.SettingsManager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class PointDetailsFragment extends Fragment implements FragmentCommunicator {

	// Store instance variables
	private SettingsManager settingsManagerInstance;
    private ArrayList<PedestrianCrossing> pedestrianCrossingList;

	// ui components

	// newInstance constructor for creating fragment with arguments
	public static PointDetailsFragment newInstance(PointWrapper pointWrapper) {
		PointDetailsFragment pointDetailsFragmentInstance = new PointDetailsFragment();
        Bundle args = new Bundle();
        try {
            args.putString("jsonPointSerialized", pointWrapper.toJson().toString());
        } catch (JSONException e) {
            args.putString("jsonPointSerialized", "");
        }
        pointDetailsFragmentInstance.setArguments(args);
		return pointDetailsFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
			((PointDetailsActivity) activity).pointDetailsFragmentCommunicator = this;
		}
        // settings manager
		settingsManagerInstance = SettingsManager.getInstance(context);
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_point_details, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
    }

    @Override public void onFragmentEnabled() {
    }

	@Override public void onFragmentDisabled() {
    }

}
