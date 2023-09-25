package org.walkersguide.android.ui.fragment.pt;

import android.os.Handler;
import android.os.Looper;
import timber.log.Timber;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.util.GlobalInstance;
import android.text.format.DateFormat;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.widget.DatePicker;
import java.util.Calendar;


import de.schildbach.pte.dto.Departure;
import java.util.ListIterator;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import androidx.fragment.app.DialogFragment;
import androidx.core.view.ViewCompat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Handler;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Location;


import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.select.SelectPublicTransportProviderDialog;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.server.pt.PtException;
import org.walkersguide.android.server.pt.PtUtility;
import org.walkersguide.android.server.pt.PtUtility;
import java.util.Date;
import android.widget.AdapterView;
import android.widget.Button;
import android.text.TextUtils;
import android.widget.HeaderViewListAdapter;
import android.widget.TimePicker;
import android.widget.LinearLayout;
import de.schildbach.pte.NetworkId;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import org.json.JSONException;

import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.pt.NearbyStationsTask;
import org.walkersguide.android.server.pt.StationDeparturesTask;
import org.walkersguide.android.server.pt.PtException;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;
import org.walkersguide.android.util.Helper;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import org.walkersguide.android.ui.fragment.RootFragment;


public class DeparturesFragment extends RootFragment implements FragmentResultListener, MenuProvider, Runnable {
    private static final String KEY_IS_DIALOG = "isDialog";


    // constructors

    // used by ObjectDetailsTabLayoutFragment
    private static final String KEY_COORDINATES_FOR_STATION_REQUEST = "coordinatesForStationRequest";

    private Point coordinatesForStationRequest;

	public static DeparturesFragment newInstance(double latitude, double longitude) {
		DeparturesFragment fragment = new DeparturesFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_COORDINATES_FOR_STATION_REQUEST, Point.fromDouble(latitude, longitude));
        args.putBoolean(KEY_IS_DIALOG, false);
        fragment.setArguments(args);
		return fragment;
	}

    // used by TripDetailsFragment
    private static final String KEY_STATION = "station";
    private static final String KEY_DEPARTURE_TIME = "departureTime";

    private Location station;
    private Date departureTime;

	public static DeparturesFragment newInstance(Location station, Date departureTime) {
		DeparturesFragment fragment = new DeparturesFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_STATION, station);
        args.putSerializable(KEY_DEPARTURE_TIME, departureTime);
        args.putBoolean(KEY_IS_DIALOG, true);
        fragment.setArguments(args);
		return fragment;
	}


    // fragment
    private static final String KEY_CACHED_DEPARTURE_LIST = "cachedDepartureList";
    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_LIST_POSITION = "listPosition";

    private SettingsManager settingsManagerInstance;
    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;

    private ArrayList<Departure> cachedDepartureList;
    private int listPosition;
	private Handler nextDeparturesHandler;

	// ui components
    private ListView listViewDepartures;
    private TextView labelHeading, labelEmptyListView;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (! getArguments().getBoolean(KEY_IS_DIALOG)) {
            // is required, when the fragment is attached via ViewPager
            setShowsDialog(false);
        }

        settingsManagerInstance = SettingsManager.getInstance();
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
        nextDeparturesHandler= new Handler(Looper.getMainLooper());

        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectPublicTransportProviderDialog.REQUEST_SELECT_PT_PROVIDER, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectPtStationDialog.REQUEST_SELECT_STATION, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectDepartureDateAndTimeDialog.REQUEST_SELECT_DATE_AND_TIME, this, this);
	}

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        Timber.d("onFragmentResult: %1$s", requestKey);
        if (coordinatesForStationRequest == null) {
            return;
        }
        if (serverTaskExecutorInstance.taskInProgress(taskId)) {
            serverTaskExecutorInstance.cancelTask(taskId);
        }

        if (requestKey.equals(SelectPublicTransportProviderDialog.REQUEST_SELECT_PT_PROVIDER)) {
            settingsManagerInstance.setSelectedNetworkId(
                    (NetworkId) bundle.getSerializable(SelectPublicTransportProviderDialog.EXTRA_NETWORK_ID));
            station = null;
            departureTime = null;

        } else if (requestKey.equals(SelectPtStationDialog.REQUEST_SELECT_STATION)) {
            station = (Location) bundle.getSerializable(SelectPtStationDialog.EXTRA_STATION);
            departureTime = new Date();

        } else if (requestKey.equals(SelectDepartureDateAndTimeDialog.REQUEST_SELECT_DATE_AND_TIME)) {
            departureTime = (Date) bundle.getSerializable(SelectDepartureDateAndTimeDialog.EXTRA_DEPARTURE_TIME);
        }

        cachedDepartureList = null;
        listPosition = 0;
        prepareRequest();
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_departures_fragment, menu);
    }

    @Override public void onPrepareMenu(@NonNull Menu menu) {
        MenuItem menuItemRefresh = menu.findItem(R.id.menuItemRefresh);
        if (serverTaskExecutorInstance.taskInProgress(taskId)) {
            menuItemRefresh.setTitle(
                    getResources().getString(R.string.menuItemCancel));
        } else {
            menuItemRefresh.setTitle(
                    getResources().getString(R.string.menuItemRefresh));
        }
    }

    @Override public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemRefresh) {
            if (serverTaskExecutorInstance.taskInProgress(taskId)) {
                serverTaskExecutorInstance.cancelTask(taskId);
            } else if (coordinatesForStationRequest != null || station != null) {
                cachedDepartureList = null;
                station = null;
                departureTime = null;
                listPosition = 0;
                prepareRequest();
            }

        } else if (item.getItemId() == R.id.menuItemPublicTransportProvider) {
            SelectPublicTransportProviderDialog.newInstance(
                    settingsManagerInstance.getSelectedNetworkId())
                .show(getChildFragmentManager(), "SelectPublicTransportProviderDialog");

        } else if (item.getItemId() == R.id.menuItemSelectDepartureDateAndTime) {
            SelectDepartureDateAndTimeDialog.newInstance(departureTime)
                .show(getChildFragmentManager(), "SelectDepartureDateAndTimeDialog");

        } else {
            return false;
        }
        return true;
        }


    /**
     * create view
     */

    @Override public String getTitle() {
        if (station != null) {
            return PtUtility.getLocationName(station);
        }
        return null;
    }

    @Override public int getLayoutResourceId() {
        return R.layout.fragment_departures;
    }

	@Override public View configureView(View view, Bundle savedInstanceState) {
        coordinatesForStationRequest = (Point) getArguments().getSerializable(KEY_COORDINATES_FOR_STATION_REQUEST);
        if (savedInstanceState != null) {
            cachedDepartureList = (ArrayList<Departure>) savedInstanceState.getSerializable(KEY_CACHED_DEPARTURE_LIST);
            station = (Location) savedInstanceState.getSerializable(KEY_STATION);
            departureTime = (Date) savedInstanceState.getSerializable(KEY_DEPARTURE_TIME);
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            cachedDepartureList = null;
            station = (Location) getArguments().getSerializable(KEY_STATION);
            departureTime = (Date) getArguments().getSerializable(KEY_DEPARTURE_TIME);
            taskId = ServerTaskExecutor.NO_TASK_ID;
            listPosition = 0;
        }

        LinearLayout layoutTop = (LinearLayout) view.findViewById(R.id.layoutTop);
        layoutTop.setVisibility(View.GONE);

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        labelHeading.setVisibility(View.GONE);

        listViewDepartures = (ListView) view.findViewById(R.id.listView);
        listViewDepartures.setVisibility(View.GONE);
        listViewDepartures.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final Departure departure = (Departure) parent.getItemAtPosition(position);
                if (station != null && departure != null) {
                    mainActivityController.addFragment(
                            TripDetailsFragment.newInstance(station, departure));
                }
            }
        });

        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        listViewDepartures.setEmptyView(labelEmptyListView);
        labelEmptyListView.setVisibility(View.GONE);

        if (coordinatesForStationRequest != null || station != null) {
            labelHeading.setVisibility(View.VISIBLE);
            listViewDepartures.setVisibility(View.VISIBLE);
            labelEmptyListView.setVisibility(View.VISIBLE);

            // show simulate location layout if not embedded in PointDetailsActivity
            if (coordinatesForStationRequest == null && station != null) {
                GPS stationGpsPoint = null;
                try {
                    stationGpsPoint = new GPS.Builder(
                            station.coord.getLatAsDouble(),
                            station.coord.getLonAsDouble())
                        .setName(
                                String.format(
                                    "%1$s %2$s",
                                    GlobalInstance.getStringResource(R.string.labelNearby),
                                    PtUtility.getLocationName(station)))
                        .build();
                } catch (JSONException e) {}

                if (stationGpsPoint != null) {
                    TextViewAndActionButton layoutStationGpsPoint = (TextViewAndActionButton) view.findViewById(R.id.layoutStationGpsPoint);
                    layoutStationGpsPoint.configureAsSingleObject(stationGpsPoint);
                    layoutTop.setVisibility(View.VISIBLE);
                }
            }
        }

        return view;
    }

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_CACHED_DEPARTURE_LIST, cachedDepartureList);
        savedInstanceState.putSerializable(KEY_STATION, station);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
        savedInstanceState.putSerializable(KEY_DEPARTURE_TIME, departureTime);
        savedInstanceState.putInt(KEY_LIST_POSITION, listPosition);
    }

    @Override public void onResume() {
        super.onResume();
        // prepare request
        if (cachedDepartureList != null) {
            departureTaskWasSuccessful(cachedDepartureList);
        } else if (coordinatesForStationRequest != null || station != null) {
            prepareRequest();
        }

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_NEARBY_STATIONS_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_STATION_DEPARTURES_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);
    }

    @Override public void onPause() {
        super.onPause();
        if (station != null) {
            nextDeparturesHandler.removeCallbacks(DeparturesFragment.this);
        }
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
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
                getResources().getQuantityString(
                    R.plurals.departure, 0, 0));

        // list view
        listViewDepartures.setAdapter(null);
        listViewDepartures.setOnScrollListener(null);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setText(
                getResources().getString(R.string.messagePleaseWait));
        nextDeparturesHandler.removeCallbacks(DeparturesFragment.this);

        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {

            if (station != null) {
                taskId = serverTaskExecutorInstance.executeTask(
                        new StationDeparturesTask(
                            settingsManagerInstance.getSelectedNetworkId(),
                            station, departureTime));
            } else {
                taskId = serverTaskExecutorInstance.executeTask(
                        new NearbyStationsTask(
                            settingsManagerInstance.getSelectedNetworkId(),
                            coordinatesForStationRequest, null));
            }
        }
    }

    // background task results

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_NEARBY_STATIONS_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_STATION_DEPARTURES_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_NEARBY_STATIONS_TASK_SUCCESSFUL)) {
                    stationTaskWasSuccessful(
                            (ArrayList<Location>) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_STATION_LIST));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_STATION_DEPARTURES_TASK_SUCCESSFUL)) {
                    departureTaskWasSuccessful(
                            (ArrayList<Departure>) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_DEPARTURE_LIST));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {
                    labelEmptyListView.setText(
                            GlobalInstance.getStringResource(R.string.errorReqRequestCancelled));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    PtException ptException = (PtException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (ptException != null) {
                        if (ptException.showPtProviderDialog()) {
                            SelectPublicTransportProviderDialog.newInstance(
                                    settingsManagerInstance.getSelectedNetworkId())
                                .show(getChildFragmentManager(), "SelectPublicTransportProviderDialog");
                        } else {
                            ViewCompat.setAccessibilityLiveRegion(
                                    labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                        }
                        labelEmptyListView.setText(ptException.getMessage());
                    }
                }
            }
        }
    };


    /**
     * nearby stations
     */

    public void stationTaskWasSuccessful(ArrayList<Location> stationList) {
        // filter station list by distance
        ArrayList<Location> nearbyStationList = new ArrayList<Location>();
        for (Location station : stationList) {
            if (PtUtility.distanceBetweenTwoPoints(coordinatesForStationRequest, station.coord) < 200) {
                nearbyStationList.add(station);
            }
        }

        // select
        switch (nearbyStationList.size()) {
            case 0:
                ViewCompat.setAccessibilityLiveRegion(
                        labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                labelEmptyListView.setText(
                        getResources().getString(R.string.labelNoPtStationsNearby));
                break;
            case 1:
                station = nearbyStationList.get(0);
                departureTime = new Date();
                listPosition = 0;
                prepareRequest();
                break;
            default:
                if (isAdded()) {
                    SelectPtStationDialog.newInstance(coordinatesForStationRequest, nearbyStationList)
                        .show(getChildFragmentManager(), "SelectPtStationDialog");
                }
                labelEmptyListView.setText("");
                break;
        }
    }


    public static class SelectPtStationDialog extends DialogFragment {
        public static final String REQUEST_SELECT_STATION = "selectStation";
        public static final String EXTRA_STATION = "station";

        public static SelectPtStationDialog newInstance(Point currentPosition, ArrayList<Location> stationList) {
            SelectPtStationDialog selectPtStationDialogInstance = new SelectPtStationDialog();
            Bundle args = new Bundle();
            args.putSerializable("currentPosition", currentPosition);
            args.putSerializable("stationList", stationList);
            selectPtStationDialogInstance.setArguments(args);
            return selectPtStationDialogInstance;
        }


        private Point currentPosition;
        private ArrayList<Location> stationList;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            currentPosition = (Point) getArguments().getSerializable("currentPosition");
            stationList = (ArrayList<Location>) getArguments().getSerializable("stationList");
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectPtStationDialogTitle))
                .setItems(
                        new String[]{getResources().getString(R.string.messagePleaseWait)},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                        )
                .setNegativeButton(
                        getResources().getString(R.string.dialogCancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                        )
                .create();
        }

        @Override public void onStart() {
            super.onStart();
            AlertDialog dialog = (AlertDialog) getDialog();
            if (dialog != null) {
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });

                // list view
                ListView listViewItems = (ListView) dialog.getListView();
                listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                        Location station = (Location) parent.getItemAtPosition(position);
                        if (station != null) {
                            Bundle result = new Bundle();
                            result.putSerializable(EXTRA_STATION, station);
                            getParentFragmentManager().setFragmentResult(REQUEST_SELECT_STATION, result);
                            dismiss();
                        }
                    }
                });

                // fill listview
                listViewItems.setAdapter(
                        new StationAdapter(getActivity(), currentPosition, stationList));
            }
        }
    }


    private static  class StationAdapter extends ArrayAdapter<Location> {

        private Context context;
        private Point position;
        private ArrayList<Location> stationList;

        public StationAdapter(Context context, Point position, ArrayList<Location> stationList) {
            super(context, R.layout.layout_single_text_view);
            this.context = context;
            this.position = position;
            this.stationList = stationList;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(this.context).inflate(R.layout.layout_single_text_view, parent, false);
                holder = new EntryHolder();
                holder.label = (TextView) convertView.findViewById(R.id.label);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }

            Location station = getItem(position);
            if (holder.label != null) {
                ArrayList<String> stationDescriptionList = new ArrayList<String>();
                // station name
                stationDescriptionList.add(PtUtility.getLocationName(station));
                // available vehicle types
                if (station.products != null && ! station.products.isEmpty()) {
                    stationDescriptionList.add(
                            PtUtility.vehicleTypesToString(station.products));
                }
                // distance to current position
                if (this.position != null) {
                    int distanceInMeters = PtUtility.distanceBetweenTwoPoints(this.position, station.coord);
                    stationDescriptionList.add(
                            context.getResources().getQuantityString(
                                R.plurals.meter, distanceInMeters, distanceInMeters));
                }
                holder.label.setText(TextUtils.join("\n", stationDescriptionList));
            }
            return convertView;
        }

        @Override public int getCount() {
            if (stationList != null) {
                return stationList.size();
            }
            return 0;
        }

        @Override public Location getItem(int position) {
            return stationList.get(position);
        }

        private class EntryHolder {
            public TextView label;
        }
    }


    /**
     * station departures
     */

    public void departureTaskWasSuccessful(ArrayList<Departure> departureList) {
        this.cachedDepartureList = departureList;

        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        labelHeading.setText(
                getResources().getQuantityString(
                    R.plurals.departure, departureList.size(), departureList.size()));

        // listview
        DepartureAdapter departureAdapter = new DepartureAdapter(
                DeparturesFragment.this.getContext(), station, departureList);
        departureAdapter.notifyDataSetChanged();
        listViewDepartures.setAdapter(departureAdapter);
        labelEmptyListView.setText("");

        // list position
        listViewDepartures.setSelection(listPosition);
        listViewDepartures.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (listPosition != firstVisibleItem) {
                listPosition = firstVisibleItem;
            }
            }
        });

        // start relative departure time updates
        nextDeparturesHandler.postDelayed(
                DeparturesFragment.this,
                61000 - (System.currentTimeMillis() % 60000));
    }

    private DepartureAdapter getDepartureAdapterFromListView() {
        if (listViewDepartures.getAdapter() != null) {
            if (listViewDepartures.getAdapter() instanceof HeaderViewListAdapter) {
                return (DepartureAdapter) ((HeaderViewListAdapter) listViewDepartures.getAdapter()).getWrappedAdapter();
            }
            return (DepartureAdapter) listViewDepartures.getAdapter();
        }
        return null;
    }

    @Override public void run() {
        // update departure list every 60 seconds
        DepartureAdapter departureAdapter = getDepartureAdapterFromListView();
        if (departureAdapter != null) {
            departureAdapter.notifyDataSetChanged();

            // update heading
            ViewCompat.setAccessibilityLiveRegion(
                    labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
            labelHeading.setText(
                    getResources().getQuantityString(
                        R.plurals.departure, departureAdapter.getCount(), departureAdapter.getCount()));
            ViewCompat.setAccessibilityLiveRegion(
                    labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);

            // plan next update
            if (departureAdapter.getCount() > 0) {
                nextDeparturesHandler.postDelayed(DeparturesFragment.this, 60000);
            }
        }
    }


    private static class DepartureAdapter extends ArrayAdapter<Departure> {

        private Context context;
        private Location station;
        private ArrayList<Departure> departureList;

        public DepartureAdapter(Context context, Location station, ArrayList<Departure> departureList) {
            super(context, R.layout.layout_single_text_view);
            this.context = context;
            this.station = station;
            this.departureList = departureList;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Departure departure = getItem(position);

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

            String labelText = String.format(
                    context.getResources().getString(R.string.labelDepartureAdapter),
                    PtUtility.getLineLabel(departure.line, true),
                    PtUtility.getLocationName(departure.destination),
                    PtUtility.formatRelativeDepartureTime(
                        PtUtility.getDepartureTime(departure), false),
                    PtUtility.formatAbsoluteDepartureTime(
                        PtUtility.getDepartureTime(departure)));
            if (departure.position != null) {
                labelText += "\n" + String.format(
                        context.getResources().getString(R.string.labelFromPlatform),
                        departure.position.toString());
            }
            holder.label.setText(labelText);

            String labelContentDescription = String.format(
                    context.getResources().getString(R.string.labelDepartureAdapterCD),
                    PtUtility.getLineLabel(departure.line, true),
                    PtUtility.getLocationName(departure.destination),
                    PtUtility.formatRelativeDepartureTime(
                        PtUtility.getDepartureTime(departure), true),
                    PtUtility.formatAbsoluteDepartureTime(
                        PtUtility.getDepartureTime(departure)));
            if (departure.position != null) {
                labelContentDescription += ", " + String.format(
                        context.getResources().getString(R.string.labelFromPlatform),
                        departure.position.toString());
            }
            holder.label.setContentDescription(labelContentDescription);

            return convertView;
        }

        @Override public int getCount() {
            if (departureList != null) {
                return departureList.size();
            }
            return 0;
        }

        @Override public Departure getItem(int position) {
            return departureList.get(position);
        }

        @Override public void notifyDataSetChanged() {
            ListIterator<Departure> departureListIterator = this.departureList.listIterator();
            while(departureListIterator.hasNext()){
                Departure departure = departureListIterator.next();
                if (PtUtility.getDepartureTime(departure).before(new Date(System.currentTimeMillis()-60000))) {
                    departureListIterator.remove();
                }
            }
            super.notifyDataSetChanged();
        }

        private class EntryHolder {
            public TextView label;
        }
    }


    /**
     * pick departure date and time
     */

    public static class SelectDepartureDateAndTimeDialog extends DialogFragment
            implements OnDateSetListener, OnTimeSetListener {
        public static final String REQUEST_SELECT_DATE_AND_TIME = "selectDateAndTime";
        public static final String EXTRA_DEPARTURE_TIME = "departureTime";

        public static SelectDepartureDateAndTimeDialog newInstance(Date preSelectedDepartureDate) {
            // create calendar object
            Calendar cal = Calendar.getInstance();
            if (preSelectedDepartureDate != null) {
                cal.setTime(preSelectedDepartureDate);
            } else {
                cal.setTime(new Date());
            }
            // create dialog object
            SelectDepartureDateAndTimeDialog dialog = new SelectDepartureDateAndTimeDialog();
            Bundle args = new Bundle();
            args.putSerializable("calendar", cal);
            dialog.setArguments(args);
            return dialog;
        }

        private Calendar calendar;
        private Button buttonSelectDate, buttonSelectTime;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                calendar = (Calendar) savedInstanceState.getSerializable("calendar");
            } else {
                calendar = (Calendar) getArguments().getSerializable("calendar");
            }

            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_select_date_and_time, nullParent);

            buttonSelectDate = (Button) view.findViewById(R.id.buttonSelectDate);
            buttonSelectDate.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    DatePickerDialog dialog = new DatePickerDialog(
                            getActivity(),
                            SelectDepartureDateAndTimeDialog.this,
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH));
                    dialog.getDatePicker().setMinDate((new Date()).getTime());
                    dialog.show();
                }
            });

            buttonSelectTime = (Button) view.findViewById(R.id.buttonSelectTime);
            buttonSelectTime.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    TimePickerDialog dialog = new TimePickerDialog(
                            getActivity(),
                            SelectDepartureDateAndTimeDialog.this,
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            DateFormat.is24HourFormat(getActivity()));
                    dialog.show();
                }
            });

            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectDepartureDateAndTimeDialogTitle))
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogOK),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setNeutralButton(
                        getResources().getString(R.string.dialogNow),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogCancel),
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
                // positive button
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        Bundle result = new Bundle();
                        result.putSerializable(EXTRA_DEPARTURE_TIME, calendar.getTime());
                        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_DATE_AND_TIME, result);
                        dismiss();
                    }
                });
                // neutral button
                Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        calendar.setTime(new Date());
                        updateButtonLabels();
                    }
                });
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dismiss();
                    }
                });
            }
            updateButtonLabels();
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putSerializable("calendar", calendar);
        }


        @Override public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateButtonLabels();
        }

        @Override public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            updateButtonLabels();
        }

        private void updateButtonLabels() {
            buttonSelectDate.setText(
                    DateFormat.getMediumDateFormat(getActivity()).format(calendar.getTime()));
            buttonSelectTime.setText(
                    DateFormat.getTimeFormat(getActivity()).format(calendar.getTime()));
        }
    }

}
