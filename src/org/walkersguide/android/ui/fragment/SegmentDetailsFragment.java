package org.walkersguide.android.ui.fragment;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.ui.activity.SegmentDetailsActivity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SegmentDetailsFragment extends Fragment implements FragmentCommunicator {

	// Store instance variables
    private SegmentWrapper segmentWrapper;

	// ui components

	// newInstance constructor for creating fragment with arguments
	public static SegmentDetailsFragment newInstance(SegmentWrapper segmentWrapper) {
		SegmentDetailsFragment segmentDetailsFragmentInstance = new SegmentDetailsFragment();
        Bundle args = new Bundle();
        try {
            args.putString("jsonSegmentSerialized", segmentWrapper.toJson().toString());
        } catch (JSONException e) {
            args.putString("jsonSegmentSerialized", "");
        }
        segmentDetailsFragmentInstance.setArguments(args);
		return segmentDetailsFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
			((SegmentDetailsActivity) activity).segmentDetailsFragmentCommunicator = this;
		}
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_point_details, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        try {
            segmentWrapper = new SegmentWrapper(
                    getActivity(), new JSONObject(getArguments().getString("jsonSegmentSerialized", "")));
        } catch (JSONException e) {
            segmentWrapper = null;
        }

    }

    @Override public void onFragmentEnabled() {
    }

	@Override public void onFragmentDisabled() {
    }

}
