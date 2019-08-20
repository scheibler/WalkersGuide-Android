package org.walkersguide.android.ui.fragment.pointdetails;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Vibrator;

import android.support.v4.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.point.POI;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.basic.wrapper.PointWrapper.SortByDistanceFromCurrentPosition;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.adapter.PointWrapperAdapter;
import org.walkersguide.android.ui.fragment.AbstractUITab;
import org.walkersguide.android.util.Constants;


public class EntrancesFragment extends AbstractUITab {

	// Store instance variables
    private ArrayList<PointWrapper> entranceList;
    private Vibrator vibrator;

	// ui components
    private ImageButton buttonRefresh;
    private ListView listViewEntrances;
    private TextView labelHeading, labelEmptyListView;

	// newInstance constructor for creating fragment with arguments
	public static EntrancesFragment newInstance(PointWrapper pointWrapper) {
		EntrancesFragment entrancesFragmentInstance = new EntrancesFragment();
        Bundle args = new Bundle();
        try {
            args.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, pointWrapper.toJson().toString());
        } catch (JSONException e) {
            args.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
        }
        entrancesFragmentInstance.setArguments(args);
		return entrancesFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}


    /**
     * create view
     */

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.layout_heading_and_list_view_with_refresh_button, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        try {
            entranceList = new POI(
                    getActivity(), new JSONObject(getArguments().getString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "")))
                .getEntranceList();
        } catch (JSONException e) {
            entranceList = new ArrayList<PointWrapper>();
        }
        Collections.sort(entranceList, new SortByDistanceFromCurrentPosition());

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                updateUI();
            }
        });

        listViewEntrances = (ListView) view.findViewById(R.id.listView);
        listViewEntrances.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                PointWrapper pointWrapper = (PointWrapper) parent.getItemAtPosition(position);
                Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                try {
                    detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, pointWrapper.toJson().toString());
                } catch (JSONException e) {
                    detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                }
                startActivity(detailsIntent);
            }
        });
        listViewEntrances.setAdapter(
                new PointWrapperAdapter(getActivity(), entranceList));

        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setVisibility(View.GONE);
        listViewEntrances.setEmptyView(labelEmptyListView);
    }


    /**
     * pause and resume
     */

    @Override public void fragmentVisible() {
        // listen for device shakes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_SHAKE_DETECTED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // update ui
        updateUI();
    }

    @Override public void fragmentInvisible() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    private void updateUI() {
        labelHeading.setText(
                String.format(
                    getResources().getString(R.string.labelNumberOfEntrancesSuccess),
                    getResources().getQuantityString(
                        R.plurals.entrance, entranceList.size(), entranceList.size()))
                );
        PointWrapperAdapter pointWrapperAdapter = (PointWrapperAdapter) listViewEntrances.getAdapter();
        if (pointWrapperAdapter != null) {
            pointWrapperAdapter.notifyDataSetChanged();
        }
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                vibrator.vibrate(250);
                updateUI();
            }
        }
    };

}
