package org.walkersguide.android.ui.fragment.segmentdetails;

import android.support.v4.view.ViewCompat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;

import android.support.v4.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.point.Intersection;
import org.walkersguide.android.data.basic.segment.IntersectionSegment;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.data.profile.NextIntersectionsProfile;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.server.POIManager;
import org.walkersguide.android.server.POIManager.NextIntersectionsListener;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.dialog.SelectMapDialog;
import org.walkersguide.android.ui.fragment.AbstractUITab;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.POISettings;


public class NextIntersectionsFragment extends AbstractUITab implements NextIntersectionsListener{

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
    private ImageButton buttonRefresh;
    private ListView listViewNextIntersections;
    private TextView labelHeading, labelEmptyListView;


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
        poiManagerInstance = POIManager.getInstance(context);
        // settings manager
		settingsManagerInstance = SettingsManager.getInstance(context);
        // progress updater
        this.progressHandler = new Handler();
        this.progressUpdater = new ProgressUpdater();
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}


    /**
     * create view
     */

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_next_intersections_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItemRefresh = menu.findItem(R.id.menuItemRefresh);
        if (poiManagerInstance.nextIntersectionsRequestInProgress()) {
            menuItemRefresh.setTitle(getResources().getString(R.string.menuItemCancel));
        } else {
            menuItemRefresh.setTitle(getResources().getString(R.string.menuItemRefresh));
        }
        // check or uncheck show all points menu item
        MenuItem menuItemShowAllPoints = menu.findItem(R.id.menuItemShowAllPoints);
        menuItemShowAllPoints.setChecked(
                settingsManagerInstance.getPOISettings().getShowAllPoints());
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemRefresh:
                if (poiManagerInstance.nextIntersectionsRequestInProgress()) {
                    poiManagerInstance.cancelNextIntersectionsRequest();
                } else {
                    requestNextIntersections(getActivity());
                }
                break;
            case R.id.menuItemShowAllPoints:
                POISettings poiSettings = settingsManagerInstance.getPOISettings();
                poiSettings.setShowAllPoints(! poiSettings.getShowAllPoints());
                listPosition = 0;
                if (poiManagerInstance.nextIntersectionsRequestInProgress()) {
                    poiManagerInstance.cancelNextIntersectionsRequest();
                }
                requestNextIntersections(getActivity());
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * create view
     */

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
		return inflater.inflate(R.layout.layout_heading_and_list_view_with_refresh_button, container, false);
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

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (poiManagerInstance.nextIntersectionsRequestInProgress()) {
                    poiManagerInstance.cancelNextIntersectionsRequest();
                } else {
                    requestNextIntersections(getActivity());
                }
            }
        });

        listViewNextIntersections = (ListView) view.findViewById(R.id.listView);
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
        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        listViewNextIntersections.setEmptyView(labelEmptyListView);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("listPosition",  listPosition);
    }


    /**
     * pause and resume
     */

    @Override public void fragmentVisible() {
        // listen for device shakes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_SHAKE_DETECTED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // request next intersections
        requestNextIntersections(getActivity());
    }

    @Override public void fragmentInvisible() {
        poiManagerInstance.invalidateNextIntersectionsRequest(NextIntersectionsFragment.this);
        progressHandler.removeCallbacks(progressUpdater);
        // unregister shake broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    private void requestNextIntersections(Context context) {
        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelHeading.setText(
                context.getResources().getQuantityString(R.plurals.point, 0, 0));
        buttonRefresh.setContentDescription(
                context.getResources().getString(R.string.buttonCancel));
        buttonRefresh.setImageResource(R.drawable.cancel);

        // list view
        listViewNextIntersections.setAdapter(null);
        listViewNextIntersections.setOnScrollListener(null);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setText(
                context.getResources().getString(R.string.messagePleaseWait));
        // start poi profile update request
        progressHandler.postDelayed(progressUpdater, 2000);
        poiManagerInstance.requestNextIntersections(
                NextIntersectionsFragment.this,
                intersectionSegment.getIntersectionNodeId(),
                intersectionSegment.getWayId(),
                intersectionSegment.getNextNodeId());
    }

    @Override public void nextIntersectionsRequestFinished(Context context, int returnCode, NextIntersectionsProfile nextIntersectionsProfile, boolean resetListPosition) {
        POISettings poiSettings = settingsManagerInstance.getPOISettings();
        buttonRefresh.setContentDescription(
                context.getResources().getString(R.string.buttonRefresh));
        buttonRefresh.setImageResource(R.drawable.refresh);
        progressHandler.removeCallbacks(progressUpdater);

        if (returnCode == Constants.RC.OK
                && nextIntersectionsProfile != null
                && nextIntersectionsProfile.getPointProfileObjectList() != null) {

            ArrayList<PointProfileObject> nextIntersectionsList = nextIntersectionsProfile.getPointProfileObjectList();
            int totalLength = 0;
            for (int i=1; i<nextIntersectionsList.size(); i++) {
                totalLength += nextIntersectionsList.get(i).getPoint()
                    .distanceTo(nextIntersectionsList.get(i-1).getPoint());
            }

            if (! poiSettings.getShowAllPoints()) {
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

            ViewCompat.setAccessibilityLiveRegion(
                    labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
            labelHeading.setText(
                    String.format(
                        context.getResources().getString(R.string.labelNextIntersectionsSuccess),
                        context.getResources().getQuantityString(
                            R.plurals.point, nextIntersectionsList.size(), nextIntersectionsList.size()),
                        context.getResources().getQuantityString(
                            R.plurals.meter, totalLength, totalLength))
                    );
            listViewNextIntersections.setAdapter(
                    new ArrayAdapter<PointProfileObject>(
                        context, android.R.layout.simple_list_item_1, nextIntersectionsList));
            labelEmptyListView.setText("");

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
            ViewCompat.setAccessibilityLiveRegion(
                    labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
            labelEmptyListView.setText(
                    ServerUtility.getErrorMessageForReturnCode(context, returnCode));
            // show select map dialog
            if (isAdded()
                    && (
                           returnCode == Constants.RC.MAP_LOADING_FAILED
                        || returnCode == Constants.RC.WRONG_MAP_SELECTED)
                    ) {
                SelectMapDialog.newInstance()
                    .show(getActivity().getSupportFragmentManager(), "SelectMapDialog");
            }
        }
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                // reload
                vibrator.vibrate(250);
                requestNextIntersections(context);
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
