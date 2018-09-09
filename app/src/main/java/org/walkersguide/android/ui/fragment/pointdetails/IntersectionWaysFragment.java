package org.walkersguide.android.ui.fragment.pointdetails;

import org.walkersguide.android.data.profile.NextIntersectionsProfile;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.Intersection;
import org.walkersguide.android.data.basic.segment.IntersectionSegment;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.listener.NextIntersectionsListener;
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
import org.walkersguide.android.server.POIManager;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.DialogFragment;
import android.app.Dialog;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.AbsListView;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.sensor.PositionManager;


public class IntersectionWaysFragment extends Fragment implements FragmentCommunicator {

	// Store instance variables
    private Intersection intersection;

	// ui components
    private ListView listViewIntersectionWays;

	// newInstance constructor for creating fragment with arguments
	public static IntersectionWaysFragment newInstance(PointWrapper pointWrapper) {
		IntersectionWaysFragment intersectionWaysFragmentInstance = new IntersectionWaysFragment();
        Bundle args = new Bundle();
        try {
            args.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, pointWrapper.toJson().toString());
        } catch (JSONException e) {
            args.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
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
            intersection = new Intersection(
                    getActivity(), new JSONObject(getArguments().getString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "")));
        } catch (JSONException e) {
            intersection = null;
        }

        TextView labelFragmentHeader = (TextView) view.findViewById(R.id.labelFragmentHeader);
        labelFragmentHeader.setText(
                String.format(
                    getResources().getString(R.string.labelNumberOfIntersectionWaysAndBearing),
                    intersection.getSegmentList().size(),
                    StringUtility.formatGeographicDirection(
                        getActivity(), DirectionManager.getInstance(getActivity()).getCurrentDirection()))
                );

        listViewIntersectionWays = (ListView) view.findViewById(R.id.listViewIntersectionWays);
        listViewIntersectionWays.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final IntersectionSegment intersectionSegment = (IntersectionSegment) parent.getItemAtPosition(position);
                Intent segmentDetailsIntent = new Intent(getActivity(), SegmentDetailsActivity.class);
                try {
                    segmentDetailsIntent.putExtra(
                            Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED,
                            intersectionSegment.toJson().toString());
                } catch (JSONException e) {
                    segmentDetailsIntent.putExtra(
                            Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "");
                }
                getActivity().startActivity(segmentDetailsIntent);
                /*
                // show options popup menu
                PopupMenu popupMore = new PopupMenu(getActivity(), view);
                popupMore.inflate(R.menu.menu_intersection_ways_fragment_list_view);
                popupMore.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.menuItemShowSegmentDetails:
                                Intent segmentDetailsIntent = new Intent(getActivity(), SegmentDetailsActivity.class);
                                try {
                                    segmentDetailsIntent.putExtra(
                                            Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED,
                                            intersectionSegment.toJson().toString());
                                } catch (JSONException e) {
                                    segmentDetailsIntent.putExtra(
                                            Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "");
                                }
                                getActivity().startActivity(segmentDetailsIntent);
                                return true;
                            case R.id.menuItemJumpToNextBigIntersection:
                            case R.id.menuItemShowNextIntersections:
                                boolean jumpToNextBigIntersection = false;
                                if (item.getItemId() == R.id.menuItemJumpToNextBigIntersection) {
                                    jumpToNextBigIntersection = true;
                                }
                                // show next intersections dialog
                                LoadNextIntersectionsDialog.newInstance(
                                        intersectionSegment.getIntersectionNodeId(),
                                        intersectionSegment.getWayId(),
                                        intersectionSegment.getNextNodeId(),
                                        jumpToNextBigIntersection)
                                    .show(getActivity().getSupportFragmentManager(), "LoadNextIntersectionsDialog");
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popupMore.show();
                */
            }
        });
    }

    @Override public void onFragmentEnabled() {
        listViewIntersectionWays.setAdapter(
                new IntersectionSegmentAdapter(getActivity(), intersection.getSegmentList()));
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


    /*
    public static class LoadNextIntersectionsDialog extends DialogFragment implements NextIntersectionsListener {

        // Store instance variables
        private POIManager poiManagerInstance;
        private long nodeId, wayId, nextNodeId;
        private boolean jumpToNextBigIntersection;
        private int listPosition;

        // query in progress vibration
        private Handler progressHandler;
        private ProgressUpdater progressUpdater;
        private Vibrator vibrator;

        // ui components
        private ListView listViewPOI;
        private TextView labelListViewEmpty;

        public static LoadNextIntersectionsDialog newInstance(long nodeId, long wayId, long nextNodeId, boolean jumpToNextBigIntersection) {
            LoadNextIntersectionsDialog loadNextIntersectionsDialogInstance = new LoadNextIntersectionsDialog();
            Bundle args = new Bundle();
            args.putLong("nodeId", nodeId);
            args.putLong("wayId", wayId);
            args.putLong("nextNodeId", nextNodeId);
            args.putBoolean("jumpToNextBigIntersection", jumpToNextBigIntersection);
            loadNextIntersectionsDialogInstance.setArguments(args);
            return loadNextIntersectionsDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            poiManagerInstance = POIManager.getInstance(context);
            // listen for intents
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_NEW_LOCATION);
            filter.addAction(Constants.ACTION_SHAKE_DETECTED);
            filter.addAction(Constants.ACTION_UPDATE_UI);
            LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
            // progress updater
            this.progressHandler = new Handler();
            this.progressUpdater = new ProgressUpdater();
            this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                nodeId = savedInstanceState.getLong("nodeId");
                wayId = savedInstanceState.getLong("wayId");
                nextNodeId = savedInstanceState.getLong("nextNodeId");
                jumpToNextBigIntersection = savedInstanceState.getBoolean("jumpToNextBigIntersection");
                listPosition = savedInstanceState.getInt("listPosition");
            } else {
                nodeId = getArguments().getLong("nodeId");
                wayId = getArguments().getLong("wayId");
                nextNodeId = getArguments().getLong("nextNodeId");
                jumpToNextBigIntersection = getArguments().getBoolean("jumpToNextBigIntersection");
                listPosition = 0;
            }

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_list_view, nullParent);

            listViewPOI = (ListView) view.findViewById(R.id.listView);
            listViewPOI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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

            labelListViewEmpty = (TextView) view.findViewById(R.id.labelListViewEmpty);
            listViewPOI.setEmptyView(labelListViewEmpty);

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(String.valueOf(nodeId))
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogUpdate),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogClose),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .create();
        }

        @Override public void onStart() {
            super.onStart();
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                // positive button: update
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        // update poi
                        requestNextIntersectionsProfile();
                    }
                });
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
            // request
            requestNextIntersectionsProfile();
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putLong("nodeId", nodeId);
            savedInstanceState.putLong("wayId", wayId);
            savedInstanceState.putLong("nextNodeId", nextNodeId);
            savedInstanceState.putBoolean("jumpToNextBigIntersection",  jumpToNextBigIntersection);
            savedInstanceState.putInt("listPosition",  listPosition);
        }

        @Override public void onStop() {
            super.onStop();
            poiManagerInstance.invalidateNextIntersectionsRequest((LoadNextIntersectionsDialog) this);
            progressHandler.removeCallbacks(progressUpdater);
            // list view
            listViewPOI.setAdapter(null);
            listViewPOI.setOnScrollListener(null);
        }

        private void requestNextIntersectionsProfile() {
            // start or cancel search
            if (poiManagerInstance.requestInProgress()) {
                poiManagerInstance.cancelRequest();
            } else {
                // update ui
                listViewPOI.setAdapter(null);
                listViewPOI.setOnScrollListener(null);
                labelListViewEmpty.setText(
                        getResources().getString(R.string.messagePleaseWait));
                final AlertDialog dialog = (AlertDialog)getDialog();
                if(dialog != null) {
                    Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    buttonPositive.setText(getResources().getString(R.string.dialogCancel));
                }
                poiManagerInstance.requestNextIntersections(
                        LoadNextIntersectionsDialog.this, nodeId, wayId, nextNodeId);
            }
        }

    	@Override public void nextIntersectionsRequestFinished(int returnCode, String returnMessage, NextIntersectionsProfile nextIntersectionsProfile, boolean resetListPosition) {
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setText(getResources().getString(R.string.dialogUpdate));
            }
            progressHandler.removeCallbacks(progressUpdater);

            if (nextIntersectionsProfile != null
                    && nextIntersectionsProfile.getPointProfileObjectList() != null) {

                listViewPOI.setAdapter(
                        new ArrayAdapter<PointProfileObject>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            nextIntersectionsProfile.getPointProfileObjectList())
                        );
                labelListViewEmpty.setText("");

                // list position
                if (resetListPosition) {
                    listViewPOI.setSelection(0);
                } else {
                    listViewPOI.setSelection(listPosition);
                }
                listViewPOI.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
                    @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        if (listPosition != firstVisibleItem) {
                            listPosition = firstVisibleItem;
                        }
                    }
                });

            } else {
                labelListViewEmpty.setText(returnMessage);
            }

            // error message dialog
            if (! (returnCode == Constants.RC.OK || returnCode == Constants.RC.CANCELLED)) {
                SimpleMessageDialog.newInstance(returnMessage)
                    .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");

            } else if (jumpToNextBigIntersection) {
                // open details activity and load next big intersection
                Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                try {
                    detailsIntent.putExtra(
                            Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED,
                            nextIntersectionsProfile.getNextBigIntersection().toJson().toString());
                } catch (JSONException e) {
                    detailsIntent.putExtra(
                            Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                }
                startActivity(detailsIntent);
                dismiss();
            }
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            progressHandler.removeCallbacks(progressUpdater);
            // unregister broadcast receiver
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        }

        private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.ACTION_UPDATE_UI)) {
                    requestNextIntersectionsProfile();
                } else if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                    vibrator.vibrate(250);
                    requestNextIntersectionsProfile();
                } else if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                        && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD3.ID) {
                    requestNextIntersectionsProfile();
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
    */

}
