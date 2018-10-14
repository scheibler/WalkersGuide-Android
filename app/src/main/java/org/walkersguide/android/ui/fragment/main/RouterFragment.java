package org.walkersguide.android.ui.fragment.main;

import android.os.Vibrator;
import org.walkersguide.android.ui.adapter.RouteIdAdapter;
import org.walkersguide.android.ui.adapter.RouteObjectAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;

import android.text.InputType;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import org.json.JSONException;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.data.route.RouteObject;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.listener.RouteListener;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.RouteManager;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.activity.SegmentDetailsActivity;
import org.walkersguide.android.ui.dialog.ExcludedWaysDialog;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.RouteSettings;
import android.widget.AbsListView;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.AutoCompleteTextView;
import android.text.TextUtils;
import org.walkersguide.android.util.TextChangedListener;
import android.view.WindowManager;
import java.util.Iterator;
import android.text.Editable;
import org.walkersguide.android.database.SQLiteHelper;


public class RouterFragment extends Fragment implements FragmentCommunicator, RouteListener {

	// Store instance variables
    private AccessDatabase accessDatabaseInstance;
    private RouteManager routeManagerInstance;
	private SettingsManager settingsManagerInstance;
    private Vibrator vibrator;

	// ui components
    private TextView labelRouteStartPoint, labelRouteDestinationPoint;
    private TextView labelHeading, labelRouteSegmentDescription, labelRoutePointDescription;
    private TextView labelDistanceAndBearing;

	// newInstance constructor for creating fragment with arguments
	public static RouterFragment newInstance() {
		RouterFragment routerFragmentInstance = new RouterFragment();
		return routerFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
			((MainActivity) activity).routerFragmentCommunicator = this;
		}
        accessDatabaseInstance = AccessDatabase.getInstance(context);
        routeManagerInstance = RouteManager.getInstance(context);
		settingsManagerInstance = SettingsManager.getInstance(context);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_router_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menuItemNextRoutePoint).setVisible(false);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemRecalculate:
            case R.id.menuItemRecalculateWithCurrentPosition:
                RouteSettings routeSettings = settingsManagerInstance.getRouteSettings();
                int selectedRouteId = routeSettings.getSelectedRouteId();
                if (accessDatabaseInstance.getRouteIdList(null).contains(selectedRouteId)) {
                    if (item.getItemId() == R.id.menuItemRecalculateWithCurrentPosition) {
                        routeSettings.setStartPoint(
                                PositionManager.getInstance(getActivity()).getCurrentLocation());
                    } else {
                        routeSettings.setStartPoint(
                                accessDatabaseInstance.getStartPointOfRoute(selectedRouteId));
                    }
                    routeSettings.setDestinationPoint(
                            accessDatabaseInstance.getDestinationPointOfRoute(selectedRouteId));
                    routeSettings.setViaPointList(
                            accessDatabaseInstance.getViaPointListOfRoute(selectedRouteId));
                    PlanRouteDialog.newInstance().show(
                            getActivity().getSupportFragmentManager(), "PlanRouteDialog");
                } else {
                    SimpleMessageDialog.newInstance(getResources().getString(R.string.errorNoRouteSelected))
                        .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
                }
                return true;
            case R.id.menuItemRouteHistory:
                RouteHistoryDialog.newInstance().show(
                        getActivity().getSupportFragmentManager(), "RouteHistoryDialog");
                return true;
            case R.id.menuItemExcludedWays:
                ExcludedWaysDialog.newInstance().show(
                        getActivity().getSupportFragmentManager(), "ExcludedWaysDialog");
                return true;
            default:
                return false;
        }
    }

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
		return inflater.inflate(R.layout.fragment_router, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        // top layout
        labelRouteStartPoint = (TextView) view.findViewById(R.id.labelRouteStartPoint);
        labelRouteStartPoint.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                PointWrapper point = (PointWrapper) v.getTag();
                if (point != null) {
                    Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                    try {
                        detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, point.toJson().toString());
                    } catch (JSONException e) {
                        detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                    }
                    startActivity(detailsIntent);
                }
            }
        });

        labelRouteDestinationPoint = (TextView) view.findViewById(R.id.labelRouteDestinationPoint);
        labelRouteDestinationPoint.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                PointWrapper point = (PointWrapper) v.getTag();
                if (point != null) {
                    Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                    try {
                        detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, point.toJson().toString());
                    } catch (JSONException e) {
                        detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                    }
                    startActivity(detailsIntent);
                }
            }
        });

        Button buttonRouteCommandList = (Button) view.findViewById(R.id.buttonRouteCommandList);
        buttonRouteCommandList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                RouteCommandsDialog.newInstance(
                        settingsManagerInstance.getRouteSettings().getSelectedRouteId())
                    .show(getActivity().getSupportFragmentManager(), "RouteCommandsDialog");
            }
        });

        // content layout
        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
		labelRouteSegmentDescription = (TextView) view.findViewById(R.id.labelRouteSegmentDescription);
        labelRouteSegmentDescription.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                SegmentWrapper segment = (SegmentWrapper) v.getTag();
                if (segment != null) {
                    Intent detailsIntent = new Intent(getActivity(), SegmentDetailsActivity.class);
                    try {
                        detailsIntent.putExtra(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, segment.toJson().toString());
                    } catch (JSONException e) {
                        detailsIntent.putExtra(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "");
                    }
                    startActivity(detailsIntent);
                }
            }
        });
		labelRoutePointDescription = (TextView) view.findViewById(R.id.labelRoutePointDescription);
        labelRoutePointDescription.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                PointWrapper point = (PointWrapper) v.getTag();
                if (point != null) {
                    Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                    try {
                        detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, point.toJson().toString());
                    } catch (JSONException e) {
                        detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                    }
                    startActivity(detailsIntent);
                }
            }
        });

		// bottom layout
		labelDistanceAndBearing = (TextView) view.findViewById(R.id.labelDistanceAndBearing);
        Button buttonPreviousRouteObject = (Button) view.findViewById(R.id.buttonPreviousRouteObject);
        buttonPreviousRouteObject.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                routeManagerInstance.skipToPreviousRouteObject(
                        RouterFragment.this,
                        settingsManagerInstance.getRouteSettings().getSelectedRouteId());
            }
        });
        Button buttonNextRouteObject = (Button) view.findViewById(R.id.buttonNextRouteObject);
        buttonNextRouteObject.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                routeManagerInstance.skipToNextRouteObject(
                        RouterFragment.this,
                        settingsManagerInstance.getRouteSettings().getSelectedRouteId());
            }
        });
    }

    @Override public void onFragmentEnabled() {
        labelHeading.setText(
                getResources().getString(R.string.messagePleaseWait));
        labelRouteStartPoint.setVisibility(View.GONE);
        labelRouteDestinationPoint.setVisibility(View.GONE);
        labelRouteSegmentDescription.setVisibility(View.GONE);
        labelRoutePointDescription.setVisibility(View.GONE);
        labelDistanceAndBearing.setVisibility(View.GONE);
        // request route
        routeManagerInstance.requestRoute(
                RouterFragment.this,
                settingsManagerInstance.getRouteSettings().getSelectedRouteId());
    }

	@Override public void onFragmentDisabled() {
        routeManagerInstance.invalidateRouteRequest((RouterFragment) this);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(newLocationAndDirectionReceiver);
    }

	@Override public void routeCalculationFinished(int returnCode, String returnMessage, int routeId) {
    }

	@Override public void routeRequestFinished(int returnCode, String returnMessage, Route route) {
        if (route != null) {
            RouteObject currentRouteObject = route.getCurrentRouteObject();
            // heading
            labelRouteStartPoint.setText(
                    String.format(
                        "%1$s: %2$s",
                        getResources().getString(R.string.buttonStartPoint),
                        route.getStartPoint().getPoint().getName())
                    );
            labelRouteStartPoint.setTag(route.getStartPoint());
            labelRouteStartPoint.setVisibility(View.VISIBLE);
            labelRouteDestinationPoint.setText(
                    String.format(
                        "%1$s: %2$s",
                        getResources().getString(R.string.buttonDestinationPoint),
                        route.getDestinationPoint().getPoint().getName())
                    );
            labelRouteDestinationPoint.setTag(route.getDestinationPoint());
            labelRouteDestinationPoint.setVisibility(View.VISIBLE);
            // instructions label
            labelHeading.setText(
                    String.format(
                        getResources().getString(R.string.labelRoutePosition),
                        route.getRouteObjectList().indexOf(currentRouteObject)+1,
                        route.getRouteObjectList().size())
                    );
            // segment
            if (route.getRouteObjectList().indexOf(currentRouteObject) == 0) {
                labelRouteSegmentDescription.setText(
                        getResources().getString(R.string.proceedToFirstRoutePoint));
                labelRouteSegmentDescription.setTag(null);
                labelRouteSegmentDescription.setVisibility(View.VISIBLE);
            } else if (! currentRouteObject.getRouteSegment().equals(RouteObject.getDummyRouteSegment(getActivity()))) {
                labelRouteSegmentDescription.setText(
                        currentRouteObject.getRouteSegment().toString());
                labelRouteSegmentDescription.setTag(currentRouteObject.getRouteSegment());
                labelRouteSegmentDescription.setVisibility(View.VISIBLE);
            }
            // point
            if (! currentRouteObject.getRoutePoint().equals(PositionManager.getDummyLocation(getActivity()))) {
                labelRoutePointDescription.setText(
                        currentRouteObject.getRoutePoint().toString());
                labelRoutePointDescription.setTag(currentRouteObject.getRoutePoint());
                labelRoutePointDescription.setVisibility(View.VISIBLE);
            }
            // distance
            labelDistanceAndBearing.setText("");
            labelDistanceAndBearing.setContentDescription("");
            labelDistanceAndBearing.setVisibility(View.VISIBLE);
            // request current location and direction for labelDistanceAndBearing field
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_NEW_LOCATION);
            filter.addAction(Constants.ACTION_NEW_DIRECTION);
            filter.addAction(Constants.ACTION_SHAKE_DETECTED);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(newLocationAndDirectionReceiver, filter);
            PositionManager.getInstance(getActivity()).requestCurrentLocation();
        } else {
            // error message
            labelHeading.setText(returnMessage);
        }
    }

    private BroadcastReceiver newLocationAndDirectionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (
                    (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                        && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD0.ID)
                    || (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)
                        && intent.getIntExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_THRESHOLD_ID, -1) >= DirectionManager.THRESHOLD2.ID)
                    ) {
                RouteObject currentRouteObject = AccessDatabase.getInstance(context).getCurrentObjectDataOfRoute(
                        SettingsManager.getInstance(context).getRouteSettings().getSelectedRouteId());
                if (currentRouteObject != null) {
                    // update distance and bearing label
                    labelDistanceAndBearing.setText(
                            String.format(
                                "%1$s, %2$s",
                                getResources().getQuantityString(
                                    R.plurals.meter,
                                    currentRouteObject.getRoutePoint().distanceFromCurrentLocation(),
                                    currentRouteObject.getRoutePoint().distanceFromCurrentLocation()),
                                StringUtility.formatRelativeViewingDirection(
                                    context, currentRouteObject.getRoutePoint().bearingFromCurrentLocation()))
                        );
                    labelDistanceAndBearing.setContentDescription(
                            String.format(
                                context.getResources().getString(R.string.labelPointDistanceAndBearing),
                                getResources().getQuantityString(
                                    R.plurals.meter,
                                    currentRouteObject.getRoutePoint().distanceFromCurrentLocation(),
                                    currentRouteObject.getRoutePoint().distanceFromCurrentLocation()),
                                StringUtility.formatRelativeViewingDirection(
                                    context, currentRouteObject.getRoutePoint().bearingFromCurrentLocation()))
                        );
                }
            } else if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                // reload
                vibrator.vibrate(250);
                RouteManager.getInstance(context).skipToNextRouteObject(
                        RouterFragment.this,
                        SettingsManager.getInstance(context).getRouteSettings().getSelectedRouteId());
            }
        }
    };


    public static class RouteCommandsDialog extends DialogFragment implements RouteListener {

        // Store instance variables
        private AccessDatabase accessDatabaseInstance;
        private RouteManager routeManagerInstance;
    	private SettingsManager settingsManagerInstance;
        private int routeId, listPosition;

        // ui components
        private ListView listViewRouteObjects;
        private TextView labelHeading, labelEmptyListView;

        public static RouteCommandsDialog newInstance(int routeId) {
            RouteCommandsDialog routeCommandsDialogInstance = new RouteCommandsDialog();
            Bundle args = new Bundle();
            args.putInt("routeId", routeId);
            routeCommandsDialogInstance.setArguments(args);
            return routeCommandsDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            routeManagerInstance = RouteManager.getInstance(context);
    		settingsManagerInstance = SettingsManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                routeId = savedInstanceState.getInt("routeId");
                listPosition = savedInstanceState.getInt("listPosition");
            } else {
                routeId = getArguments().getInt("routeId");
                listPosition = -1;
            }

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_heading_and_list_view_with_refresh_button, nullParent);
            // heading
            labelHeading = (TextView) view.findViewById(R.id.labelHeading);
            ImageButton buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
            buttonRefresh.setVisibility(View.GONE);

            listViewRouteObjects = (ListView) view.findViewById(R.id.listView);
            listViewRouteObjects.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    final RouteObject routeObject = (RouteObject) parent.getItemAtPosition(position);
                    PopupMenu popupMore = new PopupMenu(getActivity(), view);
                    popupMore.inflate(R.menu.menu_list_view_route_commands);
                    if (routeObject.getRouteSegment().equals(RouteObject.getDummyRouteSegment(getActivity()))) {
                        popupMore.getMenu().findItem(R.id.menuItemShowSegmentDetails).setVisible(false);
                    }
                    popupMore.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.menuItemShowPointDetails:
                                    Intent pointDetailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                                    try {
                                        pointDetailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, routeObject.getRoutePoint().toJson().toString());
                                    } catch (JSONException e) {
                                        pointDetailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                                    }
                                    getActivity().startActivity(pointDetailsIntent);
                                    return true;
                                case R.id.menuItemShowSegmentDetails:
                                    Intent segmentDetailsIntent = new Intent(getActivity(), SegmentDetailsActivity.class);
                                    try {
                                        segmentDetailsIntent.putExtra(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, routeObject.getRouteSegment().toJson().toString());
                                    } catch (JSONException e) {
                                        segmentDetailsIntent.putExtra(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "");
                                    }
                                    getActivity().startActivity(segmentDetailsIntent);
                                    return true;
                                case R.id.menuItemJumpToRouteObject:
                                    routeManagerInstance.jumpToRouteObjectAtIndex(
                                            null, routeId, routeObject.getIndex());
                                    Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                                    dismiss();
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    popupMore.show();
                }
            });
            labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
            listViewRouteObjects.setEmptyView(labelEmptyListView);

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.routeCommandsDialogTitle))
                .setView(view)
                .setNegativeButton(
                        getResources().getString(R.string.dialogClose),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                        )
                .create();
        }

        @Override public void onStart() {
            super.onStart();
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
            // request route
            prepareRouteRequest();
        }

        @Override public void onStop() {
            super.onStop();
            routeManagerInstance.invalidateRouteRequest(
                    (RouteCommandsDialog) this);
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putInt("routeId", routeId);
            savedInstanceState.putInt("listPosition",  listPosition);
        }

        private void prepareRouteRequest() {
            labelHeading.setText(
                    getResources().getString(R.string.messagePleaseWait));
            listViewRouteObjects.setAdapter(null);
            listViewRouteObjects.setOnScrollListener(null);
            labelEmptyListView.setVisibility(View.GONE);
            routeManagerInstance.requestRoute(
                    (RouteCommandsDialog) this, routeId);
        }

    	@Override public void routeCalculationFinished(int returnCode, String returnMessage, int routeId) {
        }

        @Override public void routeRequestFinished(int returnCode, String returnMessage, Route route) {
            if (route != null) {
                // heading and list view
                labelHeading.setText(route.getDescription());
                listViewRouteObjects.setAdapter(
                        new RouteObjectAdapter(
                            getActivity(),
                            route.getRouteObjectList(),
                            route.getCurrentRouteObject())
                        );
                // list position
                if (listPosition == -1) {
                    listViewRouteObjects.setSelection(
                            route.getRouteObjectList().indexOf(
                                route.getCurrentRouteObject()));
                } else {
                    listViewRouteObjects.setSelection(listPosition);
                }
                listViewRouteObjects.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
                    @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        if (listPosition != firstVisibleItem) {
                            listPosition = firstVisibleItem;
                        }
                    }
                });
            } else {
                labelHeading.setText(returnMessage);
            }
        }
    }

    public static class RouteHistoryDialog extends DialogFragment {

        // Store instance variables
        private AccessDatabase accessDatabaseInstance;
        private InputMethodManager imm;
        private RouteManager routeManagerInstance;
    	private SettingsManager settingsManagerInstance;
        private int listPosition;
        private String searchTerm;

        // ui components
        private AutoCompleteTextView editSearch;
        private ImageButton buttonClearSearch;
        private ListView listViewRoutes;
        private TextView labelHeading, labelEmptyListView;

        public static RouteHistoryDialog newInstance() {
            RouteHistoryDialog routeHistoryDialogInstance = new RouteHistoryDialog();
            return routeHistoryDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            routeManagerInstance = RouteManager.getInstance(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                listPosition = savedInstanceState.getInt("listPosition");
                searchTerm = savedInstanceState.getString("searchTerm");
            } else {
                listPosition = 0;
                searchTerm = "";
            }

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.fragment_poi, nullParent);

            Button buttonSelectProfile = (Button) view.findViewById(R.id.buttonSelectProfile);
            buttonSelectProfile.setVisibility(View.GONE);

            editSearch = (AutoCompleteTextView) view.findViewById(R.id.editInput);
            editSearch.setText(searchTerm);
            editSearch.setHint(getResources().getString(R.string.dialogSearch));
            editSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        // add to search term history
                        if (! TextUtils.isEmpty(searchTerm)) {
                            settingsManagerInstance.getSearchTermHistory().addSearchTerm(searchTerm);
                        }
                        // reload route list
                        listPosition = 0;
                        prepareRouteList();
                        return true;
                    }
                    return false;
                }
            });
            editSearch.addTextChangedListener(new TextChangedListener<AutoCompleteTextView>(editSearch) {
                @Override public void onTextChanged(AutoCompleteTextView view, Editable s) {
                    searchTerm = editSearch.getText().toString();
                    if (! TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.GONE) {
                        buttonClearSearch.setVisibility(View.VISIBLE);
                    } else if (TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.VISIBLE) {
                        buttonClearSearch.setVisibility(View.GONE);
                    }
                }
            });
            // add auto complete suggestions
            ArrayAdapter<String> searchTermHistoryAdapter = new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_dropdown_item_1line,
                    settingsManagerInstance.getSearchTermHistory().getSearchTermList());
            editSearch.setAdapter(searchTermHistoryAdapter);
            // hide keyboard
            getActivity().getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            buttonClearSearch = (ImageButton) view.findViewById(R.id.buttonClearInput);
            buttonClearSearch.setContentDescription(getResources().getString(R.string.buttonClearSearch));
            buttonClearSearch.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    // clear edit text
                    editSearch.setText("");
                    // reload poi profile
                    listPosition = 0;
                    prepareRouteList();
                }
            });

            labelHeading = (TextView) view.findViewById(R.id.labelHeading);
            ImageButton buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
            buttonRefresh.setVisibility(View.GONE);

            listViewRoutes = (ListView) view.findViewById(R.id.listView);
            listViewRoutes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    final int selectedRouteId = (Integer) parent.getItemAtPosition(position);
                    PopupMenu popupMore = new PopupMenu(getActivity(), view);
                    popupMore.inflate(R.menu.menu_list_view_select_point_dialog);
                    popupMore.getMenu().findItem(R.id.menuItemShowDetails).setVisible(false);
                    popupMore.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override public boolean onMenuItemClick(MenuItem item) {
                            Intent intent;
                            RouteSettings routeSettings = settingsManagerInstance.getRouteSettings();
                            switch (item.getItemId()) {
                                case R.id.menuItemSelect:
                                    routeSettings.setSelectedRouteId(selectedRouteId);
                                    routeSettings.setStartPoint(
                                            accessDatabaseInstance.getStartPointOfRoute(selectedRouteId));
                                    routeSettings.setDestinationPoint(
                                            accessDatabaseInstance.getDestinationPointOfRoute(selectedRouteId));
                                    routeSettings.setViaPointList(
                                            accessDatabaseInstance.getViaPointListOfRoute(selectedRouteId));
                                    // update route creation timestamp in database
                                    accessDatabaseInstance.updateCurrentObjectIndexOfRoute(selectedRouteId, 0);
                                    accessDatabaseInstance.updateTimestampOfRoute(selectedRouteId);
                                    // reload ui
                                    intent = new Intent(Constants.ACTION_UPDATE_UI);
                                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                                    dismiss();
                                    return true;
                                case R.id.menuItemRemove:
                                    accessDatabaseInstance.removeRoute(selectedRouteId);
                                    // reload ui
                                    if (routeSettings.getSelectedRouteId() == -1) {
                                        intent = new Intent(Constants.ACTION_UPDATE_UI);
                                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                                    }
                                    prepareRouteList();
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    popupMore.show();
                }
            });
            labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
            listViewRoutes.setEmptyView(labelEmptyListView);

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.routeHistoryDialogTitle))
                .setView(view)
                .setNegativeButton(
                        getResources().getString(R.string.dialogClose),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                        )
                .create();
        }

        @Override public void onStart() {
            super.onStart();
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
            // get route list
            prepareRouteList();
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putInt("listPosition",  listPosition);
            savedInstanceState.putString("searchTerm", searchTerm);
        }

        private void prepareRouteList() {
            // hide keyboard
            editSearch.dismissDropDown();
            imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
            // clear search button
            if (! TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.GONE) {
                buttonClearSearch.setVisibility(View.VISIBLE);
            } else if (TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.VISIBLE) {
                buttonClearSearch.setVisibility(View.GONE);
            }

            ArrayList<Integer> routeIdList = null;
            if (TextUtils.isEmpty(searchTerm)) {
                // show all routes sorted by timestamp descending
                routeIdList = accessDatabaseInstance.getRouteIdList(SQLiteHelper.ROUTE_CREATED + " DESC");
            } else {
                routeIdList = accessDatabaseInstance.getRouteIdList(SQLiteHelper.ROUTE_NAME + " ASC");
                // filter route list by search term
                for (Iterator<Integer> iterator = routeIdList.iterator(); iterator.hasNext();) {
                    String routeName = accessDatabaseInstance.getNameOfRoute(iterator.next());
                    for (String word : searchTerm.split("\\s")) {
                        if (! routeName.toLowerCase().contains(word.toLowerCase())) {
                            // object does not match
                            iterator.remove();
                            break;
                        }
                    }
                }
            }

            // header field and list view
            if (! TextUtils.isEmpty(searchTerm)) {
                labelHeading.setText(
                        String.format(
                            getResources().getString(R.string.labelSelectHistoryPointDialogHeaderSearch),
                            getResources().getQuantityString(
                                R.plurals.result, routeIdList.size(), routeIdList.size()),
                            searchTerm,
                            StringUtility.formatProfileSortCriteria(
                                getActivity(), Constants.SORT_CRITERIA.NAME_ASC))
                        );
            } else {
                labelHeading.setText(
                        String.format(
                            getResources().getString(R.string.labelSelectHistoryPointDialogHeaderSuccess),
                            getResources().getQuantityString(
                                R.plurals.route, routeIdList.size(), routeIdList.size()),
                            StringUtility.formatProfileSortCriteria(
                                getActivity(), Constants.SORT_CRITERIA.ORDER_DESC))
                        );
            }
            // fill route id list
            listViewRoutes.setAdapter(
                    new RouteIdAdapter(getActivity(), routeIdList));

            // list position
            listViewRoutes.setSelection(listPosition);
            listViewRoutes.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
                @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (listPosition != firstVisibleItem) {
                        listPosition = firstVisibleItem;
                    }
                }
            });
        }
    }

}
