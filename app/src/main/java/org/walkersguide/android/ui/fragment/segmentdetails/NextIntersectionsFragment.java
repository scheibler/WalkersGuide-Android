package org.walkersguide.android.ui.fragment.segmentdetails;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.Intersection;
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
import org.walkersguide.android.server.POIManager;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.data.basic.segment.IntersectionSegment;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.Switch;
import org.walkersguide.android.data.profile.NextIntersectionsProfile;
import android.widget.ImageButton;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import android.widget.CompoundButton;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.PointAndSegmentDetailsSettings;
import java.util.ArrayList;
import android.widget.ArrayAdapter;
import android.widget.AbsListView;
import org.walkersguide.android.listener.NextIntersectionsListener;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;


public class NextIntersectionsFragment extends Fragment
    implements FragmentCommunicator, NextIntersectionsListener{

	// Store instance variables
    private POIManager poiManagerInstance;
	private SettingsManager settingsManagerInstance;
    private IntersectionSegment intersectionSegment;
    private int listPosition;

    // query in progress
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private Vibrator vibrator;

    // ui components
    private Button buttonRefresh;
    private ListView listViewNextIntersections;
    private Switch buttonShowAllPoints;
    private TextView labelFragmentHeader, labelListViewEmpty;


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
        poiManagerInstance = POIManager.getInstance(context);
        // settings manager
		settingsManagerInstance = SettingsManager.getInstance(context);
        // progress updater
        this.progressHandler = new Handler();
        this.progressUpdater = new ProgressUpdater();
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_next_intersections, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        try {
            intersectionSegment= new IntersectionSegment(
                    getActivity(), new JSONObject(getArguments().getString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "")));
        } catch (JSONException e) {
            intersectionSegment = null;
        }

        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt("listPosition");
        } else {
            listPosition = 0;
        }

        labelFragmentHeader = (TextView) view.findViewById(R.id.labelFragmentHeader);
        ImageButton buttonJumpToTop = (ImageButton) view.findViewById(R.id.buttonJumpToTop);
        buttonJumpToTop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (listViewNextIntersections.getAdapter() != null) {
                    listViewNextIntersections.setSelection(0);
                }
            }
        });

        listViewNextIntersections = (ListView) view.findViewById(R.id.listViewNextIntersections);
        listViewNextIntersections.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                PointProfileObject pointProfileObject = (PointProfileObject) parent.getItemAtPosition(position);
                // open details activity
                Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                try {
                    detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, pointProfileObject.toJson().toString());
                } catch (JSONException e) {
                    detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                }
                startActivity(detailsIntent);
            }
        });
        labelListViewEmpty    = (TextView) view.findViewById(R.id.labelListViewEmpty);
        listViewNextIntersections.setEmptyView(labelListViewEmpty);

        // bottom layout

        buttonRefresh = (Button) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (poiManagerInstance.nextIntersectionsRequestInProgress()) {
                    poiManagerInstance.cancelNextIntersectionsRequest();
                } else {
                    requestNextIntersections();
                }
            }
        });

        buttonShowAllPoints = (Switch) view.findViewById(R.id.buttonShowAllPoints);
        buttonShowAllPoints.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                PointAndSegmentDetailsSettings pointAndSegmentDetailsSettings = settingsManagerInstance.getPointAndSegmentDetailsSettings();
                if (pointAndSegmentDetailsSettings.getShowAllPoints() != isChecked) {
                    if (poiManagerInstance.nextIntersectionsRequestInProgress()) {
                        poiManagerInstance.cancelNextIntersectionsRequest();
                    }
                    pointAndSegmentDetailsSettings.setShowAllPoints(isChecked);
                    listPosition = 0;
                    requestNextIntersections();
                }
            }
        });
    }

    @Override public void onFragmentEnabled() {
        PointAndSegmentDetailsSettings pointAndSegmentDetailsSettings = settingsManagerInstance.getPointAndSegmentDetailsSettings();
        buttonShowAllPoints.setChecked(
                pointAndSegmentDetailsSettings.getShowAllPoints());
        // listen for device shakes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_LOCATION);
        filter.addAction(Constants.ACTION_SHAKE_DETECTED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // request next intersections
        requestNextIntersections();
    }

	@Override public void onFragmentDisabled() {
        poiManagerInstance.invalidateNextIntersectionsRequest(NextIntersectionsFragment.this);
        progressHandler.removeCallbacks(progressUpdater);
        // unregister shake broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        // list view
        listViewNextIntersections.setAdapter(null);
        listViewNextIntersections.setOnScrollListener(null);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("listPosition",  listPosition);
    }

    private void requestNextIntersections() {
        listViewNextIntersections.setAdapter(null);
        listViewNextIntersections.setOnScrollListener(null);
        labelListViewEmpty.setText("");
        labelListViewEmpty.setVisibility(View.GONE);
        // header and refresh button
        labelFragmentHeader.setText(
                getResources().getString(R.string.messagePleaseWait));
        buttonRefresh.setText(
                getResources().getString(R.string.buttonCancel));
        // start poi profile update request
        progressHandler.postDelayed(progressUpdater, 2000);
        poiManagerInstance.requestNextIntersections(
                NextIntersectionsFragment.this,
                intersectionSegment.getIntersectionNodeId(),
                intersectionSegment.getWayId(),
                intersectionSegment.getNextNodeId());
    }

    @Override public void nextIntersectionsRequestFinished(int returnCode, String returnMessage, NextIntersectionsProfile nextIntersectionsProfile, boolean resetListPosition) {
        PointAndSegmentDetailsSettings pointAndSegmentDetailsSettings = settingsManagerInstance.getPointAndSegmentDetailsSettings();
        buttonRefresh.setText(
                getResources().getString(R.string.buttonRefresh));
        progressHandler.removeCallbacks(progressUpdater);

        if (nextIntersectionsProfile != null
                && nextIntersectionsProfile.getPointProfileObjectList() != null) {

            ArrayList<PointProfileObject> nextIntersectionsList = nextIntersectionsList = nextIntersectionsProfile.getPointProfileObjectList();
            int totalLength = 0;
            for (int i=1; i<nextIntersectionsList.size(); i++) {
                totalLength += nextIntersectionsList.get(i).getPoint()
                    .distanceTo(nextIntersectionsList.get(i-1).getPoint());
            }

            if (! pointAndSegmentDetailsSettings.getShowAllPoints()) {
                // big intersections only
                nextIntersectionsList = new ArrayList<PointProfileObject>();
                for (PointProfileObject pointProfileObject : nextIntersectionsProfile.getPointProfileObjectList()) {
                    if (pointProfileObject.getPoint() instanceof Intersection) {
                        Intersection intersection  = (Intersection) pointProfileObject.getPoint();
                        if (intersection.getNumberOfStreetsWithName() > 1) {
                            nextIntersectionsList.add(pointProfileObject);
                        }
                    }
                }
            }

            labelFragmentHeader.setText(
                    String.format(
                        getResources().getString(R.string.labelNextIntersectionsSuccess),
                        nextIntersectionsList.size(),
                        getResources().getQuantityString(
                            R.plurals.meter, totalLength, totalLength))
                    );

            listViewNextIntersections.setAdapter(
                    new ArrayAdapter<PointProfileObject>(
                        getActivity(),
                        android.R.layout.simple_list_item_1,
                        nextIntersectionsList)
                    );

            // list position
            if (resetListPosition) {
                listViewNextIntersections.setSelection(0);
            } else {
                listViewNextIntersections.setSelection(listPosition);
            }
            listViewNextIntersections.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
                @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (listPosition != firstVisibleItem) {
                        listPosition = firstVisibleItem;
                    }
                }
            });

        } else {
            labelListViewEmpty.setText(returnMessage);
            labelListViewEmpty.setVisibility(View.VISIBLE);
        }

        // error message dialog
        if (! (returnCode == Constants.RC.OK || returnCode == Constants.RC.CANCELLED)) {
            SimpleMessageDialog.newInstance(returnMessage)
                .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
        }
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                    && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD3.ID) {
            } else if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                // reload
                vibrator.vibrate(250);
                requestNextIntersections();
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
