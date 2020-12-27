package org.walkersguide.android.ui.fragment.pointdetails.pt;

import org.walkersguide.android.sensor.PositionManager;
import android.widget.Switch;
import android.widget.CompoundButton;
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
import org.walkersguide.android.pt.StationManager;
import org.walkersguide.android.pt.StationManager.StationListener;
import org.walkersguide.android.pt.DepartureManager;
import org.walkersguide.android.pt.DepartureManager.DepartureListener;
import java.util.Date;
import android.widget.AdapterView;
import android.widget.Button;
import android.text.TextUtils;
import android.widget.HeaderViewListAdapter;
import android.widget.TimePicker;
import android.widget.LinearLayout;


public class DeparturesFragment extends AbstractUITab
    implements StationListener, DepartureListener, Runnable {

	// Store instance variables
    private DepartureManager departureManagerInstance;
    private StationManager stationManagerInstance;
	private SettingsManager settingsManagerInstance;
    private int listPosition;
	private Handler nextDeparturesHandler;

	// ui components
    private ImageButton buttonRefresh;
    private ListView listViewDepartures;
    private TextView labelHeading, labelEmptyListView, labelMoreResultsFooter;

    // constructors
    //
    // used by PointDetailsActivity
    private Point coordinatesForStationRequest;

	public static DeparturesFragment newInstance(PointWrapper pointWrapper) {
        Point coordinatesForStationRequest = Point.fromDouble(
                pointWrapper.getPoint().getLatitude(), pointWrapper.getPoint().getLongitude());
		DeparturesFragment departuresFragmentInstance = new DeparturesFragment();
        Bundle args = new Bundle();
        args.putSerializable("coordinatesForStationRequest", coordinatesForStationRequest);
        departuresFragmentInstance.setArguments(args);
		return departuresFragmentInstance;
	}

    // used by TripDetailsFragment
    private Location station;
    private Date departureTime;

	public static DeparturesFragment newInstance(Location station, Date departureTime) {
		DeparturesFragment departuresFragmentInstance = new DeparturesFragment();
        Bundle args = new Bundle();
        args.putSerializable("station", station);
        args.putSerializable("departureTime", departureTime);
        departuresFragmentInstance.setArguments(args);
		return departuresFragmentInstance;
	}


	@Override public void onAttach(Context context) {
		super.onAttach(context);
        departureManagerInstance = DepartureManager.getInstance(context);
        stationManagerInstance = StationManager.getInstance(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
        nextDeparturesHandler= new Handler(Looper.getMainLooper());
	}

    /**
     * menu
     */

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_departures_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemPublicTransportProvider:
                SelectPublicTransportProviderDialog.newInstance()
                    .show(getActivity().getSupportFragmentManager(), "SelectPublicTransportProviderDialog");
                return true;
            case R.id.menuItemSelectDepartureDateAndTime:
                SelectDepartureDateAndTimeDialog.newInstance(departureTime)
                    .show(getActivity().getSupportFragmentManager(), "SelectDepartureDateAndTimeDialog");
                return true;
            default:
                return false;
        }
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
            setHasOptionsMenu(true);
		    return configureView(
                   inflater.inflate(R.layout.fragment_departures, container, false),
                   savedInstanceState,
                   false);
        }
	}

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = configureView(
                inflater.inflate(R.layout.fragment_departures, nullParent),
                savedInstanceState,
                true);

        String dialogTitle;
        if (station != null) {
            dialogTitle = PTHelper.getLocationName(station);
        } else {
            dialogTitle = getResources().getString(R.string.fragmentDeparturesName);
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

	private View configureView(View view, Bundle savedInstanceState, boolean isDialog) {
        if (savedInstanceState != null) {
            coordinatesForStationRequest = (Point) savedInstanceState.getSerializable("coordinatesForStationRequest");
            station = (Location) savedInstanceState.getSerializable("station");
            departureTime = (Date) savedInstanceState.getSerializable("departureTime");
            listPosition = savedInstanceState.getInt("listPosition");
        } else {
            coordinatesForStationRequest = (Point) getArguments().getSerializable("coordinatesForStationRequest");
            station = (Location) getArguments().getSerializable("station");
            departureTime = (Date) getArguments().getSerializable("departureTime");
            listPosition = 0;
        }

        LinearLayout layoutBottom = (LinearLayout) view.findViewById(R.id.layoutBottom);
        layoutBottom.setVisibility(View.GONE);

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        labelHeading.setVisibility(View.GONE);

        buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setVisibility(View.GONE);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (departureManagerInstance.departureRequestTaskInProgress()) {
                    departureManagerInstance.cancelDepartureRequestTask();
                } else if (stationManagerInstance.stationRequestTaskInProgress()) {
                    stationManagerInstance.cancelStationRequestTask();
                } else if (listViewDepartures.getAdapter() != null) {
                    updateListView();
                } else if (coordinatesForStationRequest != null || station != null) {
                    station = null;
                    departureTime = null;
                    listPosition = 0;
                    prepareRequest();
                }
            }
        });

        listViewDepartures = (ListView) view.findViewById(R.id.listView);
        listViewDepartures.setVisibility(View.GONE);
        listViewDepartures.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final Departure departure = (Departure) parent.getItemAtPosition(position);
                if (station != null && departure != null) {
                    TripDetailsFragment.newInstance(station, departure)
                        .show(getActivity().getSupportFragmentManager(), "TripDetailsFragment");
                }
            }
        });

        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setVisibility(View.GONE);
        listViewDepartures.setEmptyView(labelEmptyListView);

        if (coordinatesForStationRequest != null || station != null) {
            // only if in dialog mode
            if (isDialog) {
                // simulation
                final PositionManager positionManagerInstance = PositionManager.getInstance(getActivity());
                PointWrapper nonFinalStationLocationFromPTE = null;
                try {
                    JSONObject jsonStationLocationFromPTE = new JSONObject();
                    jsonStationLocationFromPTE.put(
                            "lat", station.coord.getLatAsDouble());
                    jsonStationLocationFromPTE.put(
                            "lon", station.coord.getLonAsDouble());
                    jsonStationLocationFromPTE.put(
                            "name", String.format(
                                GlobalInstance.getStringResource(R.string.editGPSCoordinatesNearestAddress),
                                PTHelper.getLocationName(station))
                            );
                    jsonStationLocationFromPTE.put(
                            "type", Constants.POINT.GPS);
                    jsonStationLocationFromPTE.put(
                            "sub_type", GlobalInstance.getStringResource(R.string.currentLocationName));
                    jsonStationLocationFromPTE.put(
                            "time", System.currentTimeMillis());
                    nonFinalStationLocationFromPTE = new PointWrapper(getActivity(), jsonStationLocationFromPTE);
                } catch (JSONException e) {
                    nonFinalStationLocationFromPTE = null;
                } finally {
                    if (nonFinalStationLocationFromPTE != null) {
                        final PointWrapper stationLocationFromPTE = nonFinalStationLocationFromPTE;
                        Switch switchSimulateNearbyStationLocation = (Switch) view.findViewById(R.id.switchSimulateNearbyStationLocation);
                        switchSimulateNearbyStationLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                                boolean isSimulated = 
                                    positionManagerInstance.getSimulationEnabled()
                                    && stationLocationFromPTE.equals(positionManagerInstance.getCurrentLocation());
                                if (isChecked && ! isSimulated) {
                                    positionManagerInstance.setSimulatedLocation(stationLocationFromPTE);
                                    positionManagerInstance.setSimulationEnabled(true);
                                } else if (! isChecked && isSimulated) {
                                    positionManagerInstance.setSimulationEnabled(false);
                                }
                            }
                        });
                        // check
                        switchSimulateNearbyStationLocation.setChecked(
                                positionManagerInstance.getSimulationEnabled()
                                && stationLocationFromPTE.equals(positionManagerInstance.getCurrentLocation()));
                    }
                }
                layoutBottom.setVisibility(View.VISIBLE);
            }

            labelHeading.setVisibility(View.VISIBLE);
            buttonRefresh.setVisibility(View.VISIBLE);
            listViewDepartures.setVisibility(View.VISIBLE);
            labelEmptyListView.setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (coordinatesForStationRequest != null) {
            savedInstanceState.putSerializable("coordinatesForStationRequest", coordinatesForStationRequest);
        }
        if (station != null) {
            savedInstanceState.putSerializable("station", station);
        }
        if (departureTime != null) {
            savedInstanceState.putSerializable("departureTime", departureTime);
        }
        savedInstanceState.putInt("listPosition",  listPosition);
    }


    /**
     * pause and resume
     */

    @Override public void fragmentInvisible() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        if (station != null) {
            nextDeparturesHandler.removeCallbacks(DeparturesFragment.this);
            departureManagerInstance.invalidateDepartureRequestTask(DeparturesFragment.this);
        } else if (coordinatesForStationRequest != null) {
            stationManagerInstance.invalidateStationRequestTask(DeparturesFragment.this);
        }
    }

    @Override public void fragmentVisible() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(SelectPublicTransportProviderDialog.NEW_NETWORK_PROVIDER);
        filter.addAction(SelectPtStationDialog.PT_STATION_SELECTED);
        filter.addAction(SelectDepartureDateAndTimeDialog.NEW_DEPARTURE_SELECTED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // prepare request
        if (coordinatesForStationRequest != null || station != null) {
            prepareRequest();
        }
    }

    private void prepareRequest() {
        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelHeading.setText(
                getResources().getQuantityString(
                    R.plurals.departure, 0, 0));
        updateRefreshButton(true);

        // list view
        listViewDepartures.setAdapter(null);
        listViewDepartures.setOnScrollListener(null);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setText(
                getResources().getString(R.string.messagePleaseWait));
        nextDeparturesHandler.removeCallbacks(DeparturesFragment.this);

        if (station != null) {
            departureManagerInstance.requestDeparture(
                    DeparturesFragment.this,
                    settingsManagerInstance.getServerSettings().getSelectedPublicTransportProvider(),
                    station,
                    departureTime);
        } else {
            stationManagerInstance.requestStationList(
                    DeparturesFragment.this,
                    settingsManagerInstance.getServerSettings().getSelectedPublicTransportProvider(),
                    coordinatesForStationRequest,
                    null);
        }
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
        DepartureAdapter departureAdapter = getDepartureAdapterFromListView();
        if (departureAdapter != null) {
            departureAdapter.notifyDataSetChanged();
            labelHeading.setText(
                    getResources().getQuantityString(
                        R.plurals.departure, departureAdapter.getCount(), departureAdapter.getCount()));
        }
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


    /**
     * local broadcasts
     */

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (coordinatesForStationRequest == null) {
                return;
            }
            if (departureManagerInstance.departureRequestTaskInProgress()) {
                departureManagerInstance.cancelDepartureRequestTask();
            } else if (stationManagerInstance.stationRequestTaskInProgress()) {
                stationManagerInstance.cancelStationRequestTask();
            }
            if (intent.getAction().equals(SelectPublicTransportProviderDialog.NEW_NETWORK_PROVIDER)) {
                station = null;
                departureTime = null;
            } else if (intent.getAction().equals(SelectPtStationDialog.PT_STATION_SELECTED)) {
                station = (Location) intent.getExtras().getSerializable("station");
                departureTime = new Date();
            } else if (intent.getAction().equals(SelectDepartureDateAndTimeDialog.NEW_DEPARTURE_SELECTED)) {
                departureTime = (Date) intent.getExtras().getSerializable("departureTime");
            }
            listPosition = 0;
            prepareRequest();
        }
    };


    /**
     * stationListener
     */

    @Override public void stationRequestTaskSuccessful(Point currentPosition, ArrayList<Location> stationList) {
        updateRefreshButton(false);
        // filter station list by distance
        ArrayList<Location> nearbyStationList = new ArrayList<Location>();
        for (Location station : stationList) {
            if (PTHelper.distanceBetweenTwoPoints(currentPosition, station.coord) < 200) {
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
                    SelectPtStationDialog.newInstance(currentPosition, nearbyStationList)
                        .show(getActivity().getSupportFragmentManager(), "SelectPtStationDialog");
                }
                labelEmptyListView.setText("");
                break;
        }
    }

    @Override public void stationRequestTaskFailed(int returnCode) {
        updateRefreshButton(false);
        // show select network provider dialog
        if (isAdded()
                && returnCode == PTHelper.RC_NO_NETWORK_PROVIDER) {
            SelectPublicTransportProviderDialog.newInstance()
                .show(getActivity().getSupportFragmentManager(), "SelectPublicTransportProviderDialog");
        } else {
            ViewCompat.setAccessibilityLiveRegion(
                    labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        }
        labelEmptyListView.setText(
                PTHelper.getErrorMessageForReturnCode(DeparturesFragment.this.getContext(), returnCode));
    }


    public static class SelectPtStationDialog extends DialogFragment {
        public static final String PT_STATION_SELECTED = "org.walkersguide.android.intent.pt_station_selected";

        private Point currentPosition;
        private ArrayList<Location> stationList;

        public static SelectPtStationDialog newInstance(Point currentPosition, ArrayList<Location> stationList) {
            SelectPtStationDialog selectPtStationDialogInstance = new SelectPtStationDialog();
            Bundle args = new Bundle();
            args.putSerializable("currentPosition", currentPosition);
            args.putSerializable("stationList", stationList);
            selectPtStationDialogInstance.setArguments(args);
            return selectPtStationDialogInstance;
        }

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
                            Intent intent = new Intent(PT_STATION_SELECTED);
                            intent.putExtra("station", station);
                            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
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
        Location station = getItem(position);
        if (holder.label != null) {
            ArrayList<String> stationDescriptionList = new ArrayList<String>();
            // station name
            stationDescriptionList.add(PTHelper.getLocationName(station));
            // available vehicle types
            if (station.products != null && ! station.products.isEmpty()) {
                stationDescriptionList.add(
                        PTHelper.vehicleTypesToString(context, station.products));
            }
            // distance to current position
            if (this.position != null) {
                int distanceInMeters = PTHelper.distanceBetweenTwoPoints(this.position, station.coord);
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
     * DepartureListener
     */

    @Override public void departureRequestTaskSuccessful(ArrayList<Departure> departureList) {
        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        labelHeading.setText(
                getResources().getQuantityString(
                    R.plurals.departure, departureList.size(), departureList.size()));
        updateRefreshButton(false);
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

    @Override public void departureRequestTaskFailed(int returnCode) {
        updateRefreshButton(false);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        labelEmptyListView.setText(
                PTHelper.getErrorMessageForReturnCode(DeparturesFragment.this.getContext(), returnCode));
    }

    @Override public void run() {
        // update departure list every 60 seconds
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        updateListView();
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        // plan next update
        DepartureAdapter departureAdapter = (DepartureAdapter) listViewDepartures.getAdapter();
        if (departureAdapter != null) {
            nextDeparturesHandler.postDelayed(DeparturesFragment.this, 60000);
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
                    PTHelper.getVehicleName(context, departure.line.product),
                    departure.line.label,
                    PTHelper.getLocationName(departure.destination),
                    PTHelper.formatRelativeDepartureTime(
                        context, PTHelper.getDepartureTime(departure), false),
                    PTHelper.formatAbsoluteDepartureTime(
                        context, PTHelper.getDepartureTime(departure)));
            if (departure.position != null) {
                labelText += "\n" + String.format(
                        context.getResources().getString(R.string.labelFromPlatform),
                        departure.position.toString());
            }
            holder.label.setText(labelText);

            String labelContentDescription = String.format(
                    context.getResources().getString(R.string.labelDepartureAdapterCD),
                    PTHelper.getVehicleName(context, departure.line.product),
                    departure.line.label,
                    PTHelper.getLocationName(departure.destination),
                    PTHelper.formatRelativeDepartureTime(
                        context, PTHelper.getDepartureTime(departure), true),
                    PTHelper.formatAbsoluteDepartureTime(
                        context, PTHelper.getDepartureTime(departure)));
            if (departure.position != null) {
                labelContentDescription += ",\n" + String.format(
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
                if (PTHelper.getDepartureTime(departure).before(new Date(System.currentTimeMillis()-60000))) {
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
        public static final String NEW_DEPARTURE_SELECTED = "org.walkersguide.android.intent.new_departure_selected";

        private Calendar calendar;
        private Button buttonSelectDate, buttonSelectTime;

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
                        Intent intent = new Intent(NEW_DEPARTURE_SELECTED);
                        intent.putExtra("departureTime", calendar.getTime());
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
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
