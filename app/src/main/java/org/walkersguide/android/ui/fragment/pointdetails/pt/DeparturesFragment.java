package org.walkersguide.android.ui.fragment.pointdetails.pt;

import de.schildbach.pte.dto.Departure;
import java.util.ListIterator;
import android.os.Handler;
import android.os.Looper;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import timber.log.Timber;
import org.walkersguide.android.helper.ServerUtility;
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

	private View configureView(View view, Bundle savedInstanceState) {
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

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View footerView = inflater.inflate(R.layout.layout_single_text_view, null, false);
        labelMoreResultsFooter = (TextView) footerView.findViewById(R.id.label);
        labelMoreResultsFooter.setText(
                getResources().getString(R.string.labelMoreDepartures));
        labelMoreResultsFooter.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                DepartureAdapter departureAdapter = getDepartureAdapterFromListView();
                if (departureAdapter != null
                        && ! departureManagerInstance.departureRequestTaskInProgress()) {
                    // ui
                    ViewCompat.setAccessibilityLiveRegion(
                            labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
                    updateRefreshButton(true);
                    listViewDepartures.setOnScrollListener(null);
                    labelEmptyListView.setText(
                            getResources().getString(R.string.messagePleaseWait));
                    listViewDepartures.removeFooterView(labelMoreResultsFooter);
                    // request more departures
                    nextDeparturesHandler.removeCallbacks(DeparturesFragment.this);
                    departureManagerInstance.requestDeparture(
                            DeparturesFragment.this,
                            settingsManagerInstance.getServerSettings().getSelectedPublicTransportProvider(),
                            station,
                            PTHelper.getDepartureTime(
                                departureAdapter.getItem(departureAdapter.getCount()-1)));
                }
            }
        });

        if (coordinatesForStationRequest != null || station != null) {
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
        if (listViewDepartures.getFooterViewsCount() > 0) {
            listViewDepartures.removeFooterView(labelMoreResultsFooter);
        }
        nextDeparturesHandler.removeCallbacks(DeparturesFragment.this);

        if (station != null) {
            departureManagerInstance.requestDeparture(
                    DeparturesFragment.this,
                    settingsManagerInstance.getServerSettings().getSelectedPublicTransportProvider(),
                    station, departureTime);
        } else {
            stationManagerInstance.requestStationList(
                    DeparturesFragment.this,
                    settingsManagerInstance.getServerSettings().getSelectedPublicTransportProvider(),
                    coordinatesForStationRequest);
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
            if (departureAdapter.getCount() == 0
                    && listViewDepartures.getFooterViewsCount() > 0) {
                listViewDepartures.removeFooterView(labelMoreResultsFooter);
            }
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
            }
            listPosition = 0;
            prepareRequest();
        }
    };


    /**
     * stationListener
     */

    @Override public void stationRequestTaskSuccessful(ArrayList<Location> stationList) {
        updateRefreshButton(false);
        switch (stationList.size()) {
            case 0:
                ViewCompat.setAccessibilityLiveRegion(
                        labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                labelEmptyListView.setText(
                        getResources().getString(R.string.labelNoPtStationsNearby));
                break;
            case 1:
                station = stationList.get(0);
                departureTime = new Date();
                listPosition = 0;
                prepareRequest();
                break;
            default:
                if (isAdded()) {
                    SelectPtStationDialog.newInstance(stationList)
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
                && returnCode == Constants.RC.NO_PT_PROVIDER) {
            SelectPublicTransportProviderDialog.newInstance()
                .show(getActivity().getSupportFragmentManager(), "SelectPublicTransportProviderDialog");
        } else {
            ViewCompat.setAccessibilityLiveRegion(
                    labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        }
        labelEmptyListView.setText(
                ServerUtility.getErrorMessageForReturnCode(DeparturesFragment.this.getContext(), returnCode));
    }


    public static class SelectPtStationDialog extends DialogFragment {
        public static final String PT_STATION_SELECTED = "org.walkersguide.android.intent.pt_station_selected";

        private ArrayList<Location> stationList;

        public static SelectPtStationDialog newInstance(ArrayList<Location> stationList) {
            SelectPtStationDialog selectPtStationDialogInstance = new SelectPtStationDialog();
            Bundle args = new Bundle();
            args.putSerializable("stationList", stationList);
            selectPtStationDialogInstance.setArguments(args);
            return selectPtStationDialogInstance;
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
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
                        new StationAdapter(getActivity(), stationList));
            }
        }

        private class StationAdapter extends ArrayAdapter<Location> {

            private Context context;
            private ArrayList<Location> stationList;

            public StationAdapter(Context context, ArrayList<Location> stationList) {
                super(context, R.layout.layout_single_text_view);
                this.context = context;
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
                if (holder.label != null) {
                    Location station = getItem(position);
                    ArrayList<String> stationDescriptionList = new ArrayList<String>();
                    // station name
                    stationDescriptionList.add(PTHelper.getLocationName(station));
                    // available vehicle types
                    if (station.products != null && ! station.products.isEmpty()) {
                        stationDescriptionList.add(
                                PTHelper.vehicleTypesToString(station.products));
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
    }


    /**
     * DepartureListener
     */

    @Override public void departureRequestTaskSuccessful(ArrayList<Departure> departureList) {
        // listview
        DepartureAdapter departureAdapter = getDepartureAdapterFromListView();
        if (departureAdapter != null) {
            // add to adapter
            departureAdapter.addDepartures(departureList);
        } else {
            // new adapter
            departureAdapter = new DepartureAdapter(DeparturesFragment.this.getContext(), departureList);
            listViewDepartures.setAdapter(departureAdapter);
        }
        labelEmptyListView.setText(
                getResources().getString(R.string.labelNoMoreDepartures));

        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        labelHeading.setText(
                getResources().getQuantityString(
                    R.plurals.departure, departureAdapter.getCount(), departureAdapter.getCount()));
        updateRefreshButton(false);

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

        // add footer and start relative departure time updates
        listViewDepartures.addFooterView(labelMoreResultsFooter, null, true);
        nextDeparturesHandler.postDelayed(DeparturesFragment.this, 60000);
    }

    @Override public void departureRequestTaskFailed(int returnCode) {
        updateRefreshButton(false);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        labelEmptyListView.setText(
                ServerUtility.getErrorMessageForReturnCode(DeparturesFragment.this.getContext(), returnCode));
    }

    @Override public void run() {
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        updateListView();
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        DepartureAdapter departureAdapter = getDepartureAdapterFromListView();
        if (departureAdapter != null && departureAdapter.getCount() > 0) {
            nextDeparturesHandler.postDelayed(DeparturesFragment.this, 60000);
        }
    }


    private class DepartureAdapter extends ArrayAdapter<Departure> {

        private Context context;
        private ArrayList<Departure> departureList;

        public DepartureAdapter(Context context, ArrayList<Departure> departureList) {
            super(context, R.layout.list_item_line_and_departure_time);
            this.context = context;
            this.departureList = departureList;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Departure departure = getItem(position);

            // load item layout
            EntryHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(this.context).inflate(R.layout.list_item_line_and_departure_time, parent, false);
                holder = new EntryHolder();
                holder.labelLineNumber = (TextView) convertView.findViewById(R.id.labelLineNumber);
                holder.labelLineDestination = (TextView) convertView.findViewById(R.id.labelLineDestination);
                holder.labelRelativeDepartureTime = (TextView) convertView.findViewById(R.id.labelRelativeDepartureTime);
                holder.labelAbsoluteDepartureTime = (TextView) convertView.findViewById(R.id.labelAbsoluteDepartureTime);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }

            if (holder.labelLineNumber != null) {
                holder.labelLineNumber.setText(
                        String.format(
                            "%1$s%2$s", departure.line.product.code, departure.line.label));
                holder.labelLineNumber.setContentDescription(
                        String.format(
                            "%1$s%2$s", departure.line.product.code, departure.line.label));
            }
            if (holder.labelLineDestination != null) {
                holder.labelLineDestination.setText(
                        PTHelper.getLocationName(departure.destination));
                holder.labelLineDestination.setContentDescription(
                        String.format(
                            context.getResources().getString(R.string.contentDescriptionLineDestination),
                            PTHelper.getLocationName(departure.destination))
                        );
            }
            if (holder.labelRelativeDepartureTime != null) {
                holder.labelRelativeDepartureTime.setText(
                        PTHelper.formatRelativeDepartureTimeInMinutes(
                            context, PTHelper.getDepartureTime(departure), true));
                holder.labelRelativeDepartureTime.setContentDescription(
                        PTHelper.formatRelativeDepartureTimeInMinutes(
                            context, PTHelper.getDepartureTime(departure), false));
            }
            if (holder.labelAbsoluteDepartureTime != null) {
                holder.labelAbsoluteDepartureTime.setText(
                        PTHelper.formatAbsoluteDepartureTime(
                            context, PTHelper.getDepartureTime(departure), true));
                holder.labelAbsoluteDepartureTime.setContentDescription(
                        PTHelper.formatAbsoluteDepartureTime(
                            context, PTHelper.getDepartureTime(departure), false));
            }

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
                if (PTHelper.getDepartureTime(departure).before(new Date())) {
                    departureListIterator.remove();
                }
            }
            super.notifyDataSetChanged();
        }

        public void addDepartures(ArrayList<Departure> departureListToAdd) {
            if (departureListToAdd != null) {
                for (Departure departureToAdd : departureListToAdd) {
                    if (! this.departureList.contains(departureToAdd)) {
                        this.departureList.add(departureToAdd);
                    }
                }
                notifyDataSetChanged();
            }
        }

        private class EntryHolder {
            public TextView labelLineNumber, labelLineDestination, labelRelativeDepartureTime, labelAbsoluteDepartureTime;
        }
    }

}
