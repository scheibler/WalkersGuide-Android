package org.walkersguide.android.ui.fragment.pointdetails;

import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.POISettings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import android.widget.ImageButton;


public class IntersectionWaysFragment extends Fragment implements FragmentCommunicator {

	// Store instance variables
    private Intersection intersection;
	private SettingsManager settingsManagerInstance;
    private Vibrator vibrator;

	// ui components
    private ImageButton buttonRefresh;
    private ListView listViewIntersectionWays;
    private TextView labelHeading, labelEmptyListView;

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
		settingsManagerInstance = SettingsManager.getInstance(context);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_intersection_ways_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // check or uncheck show all points menu item
        MenuItem menuItemAutoUpdate = menu.findItem(R.id.menuItemAutoUpdate);
        menuItemAutoUpdate.setChecked(
                settingsManagerInstance.getPOISettings().getAutoUpdate());
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemRefresh:
                updateUI();
                break;
            case R.id.menuItemAutoUpdate:
                POISettings poiSettings = settingsManagerInstance.getPOISettings();
                poiSettings.setAutoUpdate(! poiSettings.getAutoUpdate());
                updateUI();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
		return inflater.inflate(R.layout.layout_heading_and_list_view_with_refresh_button, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        try {
            intersection = new Intersection(
                    getActivity(), new JSONObject(getArguments().getString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "")));
        } catch (JSONException e) {
            intersection = null;
        }

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                updateUI();
            }
        });

        listViewIntersectionWays = (ListView) view.findViewById(R.id.listView);
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
            }
        });
        listViewIntersectionWays.setAdapter(
                new IntersectionSegmentAdapter(getActivity(), intersection.getSegmentList()));

        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setVisibility(View.GONE);
        listViewIntersectionWays.setEmptyView(labelEmptyListView);
    }

    @Override public void onFragmentEnabled() {
        // listen for direction changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_DIRECTION);
        filter.addAction(Constants.ACTION_SHAKE_DETECTED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // update ui
        updateUI();
    }

	@Override public void onFragmentDisabled() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    private void updateUI() {
        labelHeading.setText(
                String.format(
                    getResources().getString(R.string.labelNumberOfIntersectionWaysAndBearing),
                    getResources().getQuantityString(
                        R.plurals.street, intersection.getSegmentList().size(), intersection.getSegmentList().size()),
                    StringUtility.formatGeographicDirection(
                        getActivity(), DirectionManager.getInstance(getActivity()).getCurrentDirection()))
                );
        IntersectionSegmentAdapter intersectionSegmentAdapter = (IntersectionSegmentAdapter) listViewIntersectionWays.getAdapter();
        if (intersectionSegmentAdapter != null) {
            intersectionSegmentAdapter.notifyDataSetChanged();
        }
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                vibrator.vibrate(250);
                updateUI();
            } else if (SettingsManager.getInstance(context).getPOISettings().getAutoUpdate()
                    && intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)
                    && intent.getIntExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_THRESHOLD_ID, -1) >= DirectionManager.THRESHOLD1.ID) {
                updateUI();
            }
        }
    };

}
