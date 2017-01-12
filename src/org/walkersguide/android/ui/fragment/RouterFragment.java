package org.walkersguide.android.ui.fragment;

import org.walkersguide.android.R;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.TTSWrapper;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class RouterFragment extends Fragment implements FragmentCommunicator {

	// Store instance variables
	private SettingsManager settingsManagerInstance;
    private TTSWrapper ttsWrapperInstance;

	// ui components
    private TextView labelDistanceAndBearing, labelRoutePosition;
	private Button buttonNewRoute, buttonSelectPOIProfile;
    private ScrollView scrollviewDescription;
    private TextView labelRouteSegmentDescription, labelRoutePointDescription, labelIntersectionDescription;

	// newInstance constructor for creating fragment with arguments
	public static RouterFragment newInstance() {
		RouterFragment routerFragmentInstance = new RouterFragment();
		return routerFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
			((MainActivity) activity).routerFragmentCommunicator = this;
		}
        // settings manager
		settingsManagerInstance = SettingsManager.getInstance(context);
        // tts
        ttsWrapperInstance = TTSWrapper.getInstance(context);
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_router, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        // top layout
        Button buttonRouteCommandList = (Button) view.findViewById(R.id.buttonRouteCommandList);
        buttonRouteCommandList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            }
        });

        Button buttonRecalculateRoute = (Button) view.findViewById(R.id.buttonRecalculateRoute);
        buttonRecalculateRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            }
        });

        // content layout
        buttonNewRoute = (Button) view.findViewById(R.id.buttonNewRoute);
        buttonNewRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            }
        });

		scrollviewDescription = (ScrollView) view.findViewById(R.id.scrollviewDescription);
        scrollviewDescription.setVisibility(View.GONE);

		labelRouteSegmentDescription = (TextView) view.findViewById(R.id.labelRouteSegmentDescription);
		labelRoutePointDescription = (TextView) view.findViewById(R.id.labelRoutePointDescription);
		labelIntersectionDescription = (TextView) view.findViewById(R.id.labelIntersectionDescription);

		// bottom layout
		labelDistanceAndBearing = (TextView) view.findViewById(R.id.labelDistanceAndBearing);
		labelRoutePosition = (TextView) view.findViewById(R.id.labelRoutePosition);

        Button buttonPreviousRouteObject = (Button) view.findViewById(R.id.buttonPreviousRouteObject);
        buttonPreviousRouteObject.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            }
        });

        Button buttonNextRouteObject = (Button) view.findViewById(R.id.buttonNextRouteObject);
        buttonNextRouteObject.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            }
        });

        buttonSelectPOIProfile = (Button) view.findViewById(R.id.buttonSelectPOIProfile);
        buttonSelectPOIProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            }
        });
    }

    @Override public void onFragmentEnabled() {
    }

	@Override public void onFragmentDisabled() {
    }

}
