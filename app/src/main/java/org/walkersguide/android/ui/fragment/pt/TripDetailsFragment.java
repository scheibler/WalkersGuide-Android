package org.walkersguide.android.ui.fragment.pt;

import org.walkersguide.android.ui.activity.toolbar.FragmentContainerActivity;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Stop;
import java.util.ListIterator;
import android.os.Looper;
import androidx.core.view.ViewCompat;
import android.content.Context;

import android.os.Bundle;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import org.walkersguide.android.R;
import org.walkersguide.android.server.pt.PtException;
import org.walkersguide.android.server.pt.PtUtility;
import java.util.Date;
import android.widget.AdapterView;
import androidx.fragment.app.Fragment;
import org.walkersguide.android.util.SettingsManager;
import android.os.Handler;
import android.os.Looper;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.pt.TripDetailsTask;
import org.walkersguide.android.server.pt.PtException;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.Intent;
import org.walkersguide.android.util.GlobalInstance;


public class TripDetailsFragment extends Fragment implements Runnable {


    // instance constructors

	public static TripDetailsFragment newInstance(Location station, Departure departure) {
		TripDetailsFragment fragment = new TripDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_STATION, station);
        args.putSerializable(KEY_DEPARTURE, departure);
        fragment.setArguments(args);
		return fragment;
	}


    // fragment
    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_STATION = "station";
    private static final String KEY_DEPARTURE = "departure";
    private static final String KEY_LIST_POSITION = "listPosition";

    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;
	private Handler nextDeparturesHandler = new Handler(Looper.getMainLooper());

    private Location station;
    private Departure departure;
    private int listPosition;

	// ui components
    private ImageButton buttonRefresh;
    private ListView listViewTrip;
    private TextView labelHeading, labelEmptyListView;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
    }


    /**
     * create view
     */

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.layout_heading_and_list_view_with_refresh_button, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        station = (Location) getArguments().getSerializable(KEY_STATION);
        departure = (Departure) getArguments().getSerializable(KEY_DEPARTURE);
        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
            listPosition = 0;
        }

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        labelHeading.setVisibility(View.GONE);

        buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setVisibility(View.GONE);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (serverTaskExecutorInstance.taskInProgress(taskId)) {
                    serverTaskExecutorInstance.cancelTask(taskId);
                } else if (listViewTrip.getAdapter() != null) {
                    updateListView();
                } else if (station != null && departure != null) {
                    listPosition = 0;
                    prepareRequest();
                }
            }
        });

        listViewTrip = (ListView) view.findViewById(R.id.listView);
        listViewTrip.setVisibility(View.GONE);
        listViewTrip.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final Stop selectedStop = (Stop) parent.getItemAtPosition(position);
                if (selectedStop != null) {
                    FragmentContainerActivity.showDepartures(
                            TripDetailsFragment.this.getContext(),
                            selectedStop.location,
                            PtUtility.getDepartureTime(selectedStop));
                }
            }
        });

        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setVisibility(View.GONE);
        listViewTrip.setEmptyView(labelEmptyListView);

        if (station != null && departure != null) {
            // show controls
            labelHeading.setVisibility(View.VISIBLE);
            labelHeading.setVisibility(View.VISIBLE);
            buttonRefresh.setVisibility(View.VISIBLE);
            listViewTrip.setVisibility(View.VISIBLE);
            labelEmptyListView.setVisibility(View.VISIBLE);
        }
    }

    @Override public void onResume() {
        super.onResume();
        // prepare request
        if (station != null && departure != null) {
            prepareRequest();
        }

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_TRIP_DETAILS_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);
    }

    @Override public void onPause() {
        super.onPause();
        if (station != null && departure != null) {
            nextDeparturesHandler.removeCallbacks(TripDetailsFragment.this);
        }
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
        savedInstanceState.putInt(KEY_LIST_POSITION, listPosition);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            serverTaskExecutorInstance.cancelTask(taskId);
        }
    }


    private void prepareRequest() {
        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelHeading.setText(
                getResources().getQuantityString(R.plurals.station, 0, 0));
        updateRefreshButton(true);

        // list view
        listViewTrip.setAdapter(null);
        listViewTrip.setOnScrollListener(null);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setText(
                getResources().getString(R.string.messagePleaseWait));

        nextDeparturesHandler.removeCallbacks(TripDetailsFragment.this);
        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            taskId = serverTaskExecutorInstance.executeTask(
                    new TripDetailsTask(
                        SettingsManager.getInstance().getSelectedNetworkId(),
                        station, departure));
        }
    }

    // background task results

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_TRIP_DETAILS_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_TRIP_DETAILS_TASK_SUCCESSFUL)) {
                    tripTaskWasSuccessful(
                            (ArrayList<Stop>) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_STOP_LIST));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {
                    labelEmptyListView.setText(
                            GlobalInstance.getStringResource(R.string.errorReqRequestCancelled));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    PtException ptException = (PtException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (ptException != null) {
                        ViewCompat.setAccessibilityLiveRegion(
                                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                        labelEmptyListView.setText(ptException.getMessage());
                    }
                }

                updateRefreshButton(false);
            }
        }
    };

    private void updateRefreshButton(boolean requestInProgress) {
        if (requestInProgress) {
            buttonRefresh.setContentDescription(
                    GlobalInstance.getStringResource(R.string.buttonCancel));
            buttonRefresh.setImageResource(R.drawable.cancel);
        } else {
            buttonRefresh.setContentDescription(
                    GlobalInstance.getStringResource(R.string.buttonRefresh));
            buttonRefresh.setImageResource(R.drawable.refresh);
        }
    }

    private void updateListView() {
        TripAdapter tripAdapter = (TripAdapter) listViewTrip.getAdapter();
        if (tripAdapter != null) {
            tripAdapter.notifyDataSetChanged();
            labelHeading.setText(
                    getResources().getQuantityString(
                        R.plurals.station, tripAdapter.getCount(), tripAdapter.getCount()));
        }
    }


    /**
     * trip details
     */

    private void tripTaskWasSuccessful(ArrayList<Stop> stopList) {
        // listview
        listViewTrip.setAdapter(
                new TripAdapter(TripDetailsFragment.this.getContext(), stopList));
        labelEmptyListView.setText(
                getResources().getString(R.string.labelNoMoreStops));

        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        labelHeading.setText(
                getResources().getQuantityString(
                    R.plurals.station, stopList.size(), stopList.size()));
        updateRefreshButton(false);

        // list position
        listViewTrip.setSelection(listPosition);
        listViewTrip.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });

        // start relative departure time updates
        nextDeparturesHandler.postDelayed(TripDetailsFragment.this, 60000);
    }

    // update trip every 60 seconds

    @Override public void run() {
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        updateListView();
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        TripAdapter tripAdapter = (TripAdapter) listViewTrip.getAdapter();
        if (tripAdapter != null && tripAdapter.getCount() > 0) {
            nextDeparturesHandler.postDelayed(TripDetailsFragment.this, 60000);
        }
    }


    private static class TripAdapter extends ArrayAdapter<Stop> {

        private Context context;
        private ArrayList<Stop> stopList;

        public TripAdapter(Context context, ArrayList<Stop> stopList) {
            super(context, R.layout.layout_single_text_view);
            this.context = context;
            this.stopList = stopList;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Stop stop = getItem(position);

            // load item layout
            EntryHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(this.context).inflate(R.layout.layout_single_text_view, parent, false);
                holder = new EntryHolder();
                holder.label = (TextView) convertView.findViewById(R.id.label);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }

            holder.label.setText(
                    String.format(
                        context.getResources().getString(R.string.labelTripAdapter),
                        PtUtility.getLocationName(stop.location),
                        PtUtility.formatRelativeDepartureTime(
                            context, PtUtility.getDepartureTime(stop), false),
                        PtUtility.formatAbsoluteDepartureTime(
                            context, PtUtility.getDepartureTime(stop)))
                    );

            holder.label.setContentDescription(
                    String.format(
                        context.getResources().getString(R.string.labelTripAdapterCD),
                        PtUtility.getLocationName(stop.location),
                        PtUtility.formatRelativeDepartureTime(
                            context, PtUtility.getDepartureTime(stop), true),
                        PtUtility.formatAbsoluteDepartureTime(
                            context, PtUtility.getDepartureTime(stop)))
                    );

            return convertView;
        }

        @Override public int getCount() {
            if (stopList != null) {
                return stopList.size();
            }
            return 0;
        }

        @Override public Stop getItem(int position) {
            return stopList.get(position);
        }

        @Override public void notifyDataSetChanged() {
            ListIterator<Stop> stopListIterator = this.stopList.listIterator();
            while(stopListIterator.hasNext()){
                Stop stop = stopListIterator.next();
                if (PtUtility.getDepartureTime(stop).before(new Date())) {
                    stopListIterator.remove();
                }
            }
            super.notifyDataSetChanged();
        }

        private class EntryHolder {
            public TextView label;
        }
    }

}
