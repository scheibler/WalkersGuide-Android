package org.walkersguide.android.ui.fragment.pointdetails.pt;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Stop;
import java.util.ListIterator;
import android.os.Handler;
import android.os.Looper;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import androidx.fragment.app.DialogFragment;
import timber.log.Timber;
import androidx.core.view.ViewCompat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Location;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.SelectPublicTransportProviderDialog;
import org.walkersguide.android.ui.fragment.AbstractUITab;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.pt.PTHelper;
import org.walkersguide.android.pt.TripManager;
import org.walkersguide.android.pt.TripManager.TripListener;
import java.util.Date;
import android.widget.AdapterView;
import android.widget.Button;
import android.text.TextUtils;
import android.widget.HeaderViewListAdapter;
import org.walkersguide.android.util.GlobalInstance;


public class TripDetailsFragment extends AbstractUITab implements TripListener, Runnable {

	// Store instance variables
	private SettingsManager settingsManagerInstance;
    private TripManager tripManagerInstance;
	private Handler nextDeparturesHandler;

    private Location station;
    private Departure departure;
    private int listPosition;

	// ui components
    private ImageButton buttonRefresh;
    private ListView listViewTrip;
    private TextView labelHeading, labelEmptyListView;

	public static TripDetailsFragment newInstance(Location station, Departure departure) {
		TripDetailsFragment tripDetailsFragmentInstance = new TripDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable("station", station);
        args.putSerializable("departure", departure);
        tripDetailsFragmentInstance.setArguments(args);
		return tripDetailsFragmentInstance;
	}


	@Override public void onAttach(Context context) {
		super.onAttach(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
        tripManagerInstance = TripManager.getInstance(context);
        nextDeparturesHandler= new Handler(Looper.getMainLooper());
	}


    /**
     * create view
     */

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getDialog() != null) {
            // fragment is a dialog
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            // fragment is embetted
		    return configureView(
                   inflater.inflate(R.layout.layout_heading_and_list_view_with_refresh_button, container, false),
                   savedInstanceState);
        }
	}

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = configureView(
                inflater.inflate(R.layout.layout_heading_and_list_view_with_refresh_button, nullParent),
                savedInstanceState);

        String dialogTitle;
        if (station != null && departure != null) {
            dialogTitle = String.format(
                    "%1$s%2$s, %3$s",
                    departure.line.product.code,
                    departure.line.label,
                    PTHelper.getLocationName(departure.destination));
        } else {
            dialogTitle = getResources().getString(R.string.tripDetailsFragmentTitle);
        }

        return new AlertDialog.Builder(getActivity())
            .setTitle(dialogTitle)
            .setView(view)
            .setNegativeButton(
                    getResources().getString(R.string.dialogClose),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
            .create();
    }

	private View configureView(View view, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            station = (Location) savedInstanceState.getSerializable("station");
            departure = (Departure) savedInstanceState.getSerializable("departure");
            listPosition = savedInstanceState.getInt("listPosition");
        } else {
            station = (Location) getArguments().getSerializable("station");
            departure = (Departure) getArguments().getSerializable("departure");
            listPosition = 0;
        }

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        labelHeading.setVisibility(View.GONE);

        buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setVisibility(View.GONE);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (tripManagerInstance.tripRequestTaskInProgress()) {
                    tripManagerInstance.cancelTripRequestTask();
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
                    DeparturesFragment.newInstance(
                            selectedStop.location, PTHelper.getDepartureTime(selectedStop))
                        .show(getActivity().getSupportFragmentManager(), "DeparturesFragment");
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

        return view;
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (station != null) {
            savedInstanceState.putSerializable("station", station);
        }
        if (departure != null) {
            savedInstanceState.putSerializable("departure", departure);
        }
        savedInstanceState.putInt("listPosition",  listPosition);
    }


    /**
     * pause and resume
     */

    @Override public void fragmentInvisible() {
        if (station != null && departure != null) {
            nextDeparturesHandler.removeCallbacks(TripDetailsFragment.this);
            tripManagerInstance.invalidateTripRequestTask((TripDetailsFragment) this);
        }
    }

    @Override public void fragmentVisible() {
        // prepare request
        if (station != null && departure != null) {
            prepareRequest();
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
        // request departure
        nextDeparturesHandler.removeCallbacks(TripDetailsFragment.this);
        tripManagerInstance.requestTrip(
                TripDetailsFragment.this,
                settingsManagerInstance.getServerSettings().getSelectedPublicTransportProvider(),
                station, departure);
    }

    private void updateRefreshButton(boolean showCancel) {
        if (showCancel) {
            buttonRefresh.setContentDescription(
                    getResources().getString(R.string.buttonCancel));
            buttonRefresh.setImageResource(R.drawable.cancel);
        } else {
            buttonRefresh.setContentDescription(
                    getResources().getString(R.string.buttonRefresh));
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
     * trip request interface
     */

    @Override public void tripRequestTaskSuccessful(ArrayList<Stop> stopList) {
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

    @Override public void tripRequestTaskFailed(int returnCode) {
        updateRefreshButton(false);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        labelEmptyListView.setText(
                PTHelper.getErrorMessageForReturnCode(TripDetailsFragment.this.getContext(), returnCode));
    }


    /**
     * update trip every 60 seconds
     */

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
                        PTHelper.getLocationName(stop.location),
                        PTHelper.formatRelativeDepartureTime(
                            context, PTHelper.getDepartureTime(stop), false),
                        PTHelper.formatAbsoluteDepartureTime(
                            context, PTHelper.getDepartureTime(stop)))
                    );

            holder.label.setContentDescription(
                    String.format(
                        context.getResources().getString(R.string.labelTripAdapterCD),
                        PTHelper.getLocationName(stop.location),
                        PTHelper.formatRelativeDepartureTime(
                            context, PTHelper.getDepartureTime(stop), true),
                        PTHelper.formatAbsoluteDepartureTime(
                            context, PTHelper.getDepartureTime(stop)))
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
                if (PTHelper.getDepartureTime(stop).before(new Date())) {
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
