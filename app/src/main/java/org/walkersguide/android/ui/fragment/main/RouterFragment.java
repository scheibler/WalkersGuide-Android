package org.walkersguide.android.ui.fragment.main;

import java.util.ArrayList;
import java.util.Map;

import org.json.JSONException;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.data.route.RouteObject;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.listener.RouteListener;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.RouteManager;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.activity.SegmentDetailsActivity;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.RouteSettings;

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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import org.walkersguide.android.ui.dialog.ExcludedWaysDialog;

public class RouterFragment extends Fragment
    implements FragmentCommunicator, OnMenuItemClickListener, RouteListener {

	// Store instance variables
    private AccessDatabase accessDatabaseInstance;
	private SettingsManager settingsManagerInstance;

	// ui components
    private TextView labelDistanceAndBearing, labelRoutePosition;
    private ScrollView scrollviewDescription;
    private TextView labelRouteSegmentDescription, labelRoutePointDescription;
    private Button buttonPlanRoute;

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
		settingsManagerInstance = SettingsManager.getInstance(context);
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_router, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        // top layout
        Button buttonRouteCommandList = (Button) view.findViewById(R.id.buttonRouteCommandList);
        buttonRouteCommandList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                RouteCommandsDialog.newInstance(
                        settingsManagerInstance.getRouteSettings().getSelectedRouteId())
                    .show(getActivity().getSupportFragmentManager(), "RouteCommandsDialog");
            }
        });

        Button buttonMore = (Button) view.findViewById(R.id.buttonMore);
        buttonMore.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                PopupMenu popupMore = new PopupMenu(getActivity(), view);
                popupMore.setOnMenuItemClickListener(RouterFragment.this);
                popupMore.inflate(R.menu.menu_router_fragment_button_more);
                popupMore.show();
            }
        });

        // content layout
        buttonPlanRoute = (Button) view.findViewById(R.id.buttonPlanRoute);
        buttonPlanRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                PlanRouteDialog.newInstance().show(
                        getActivity().getSupportFragmentManager(), "PlanRouteDialog");
            }
        });

		scrollviewDescription = (ScrollView) view.findViewById(R.id.scrollviewDescription);
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
		labelRoutePosition = (TextView) view.findViewById(R.id.labelRoutePosition);

        Button buttonPreviousRouteObject = (Button) view.findViewById(R.id.buttonPreviousRouteObject);
        buttonPreviousRouteObject.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                RouteManager.getInstance(getActivity()).skipToPreviousRouteObject(
                        RouterFragment.this,
                        settingsManagerInstance.getRouteSettings().getSelectedRouteId());
            }
        });

        Button buttonNextRouteObject = (Button) view.findViewById(R.id.buttonNextRouteObject);
        buttonNextRouteObject.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                RouteManager.getInstance(getActivity()).skipToNextRouteObject(
                        RouterFragment.this,
                        settingsManagerInstance.getRouteSettings().getSelectedRouteId());
            }
        });
    }

    @Override public boolean onMenuItemClick(MenuItem item) {
        RouteSettings routeSettings = settingsManagerInstance.getRouteSettings();
        PointWrapper startPoint = accessDatabaseInstance.getStartPointOfRoute(routeSettings.getSelectedRouteId());
        PointWrapper destinationPoint = accessDatabaseInstance.getDestinationPointOfRoute(routeSettings.getSelectedRouteId());
        ArrayList<PointWrapper> viaPointList = accessDatabaseInstance.getViaPointListOfRoute(routeSettings.getSelectedRouteId());
        switch (item.getItemId()) {
            case R.id.menuItemRecalculate:
                if (startPoint != null && destinationPoint != null) {
                    routeSettings.setStartPoint(startPoint);
                    routeSettings.setDestinationPoint(destinationPoint);
                    routeSettings.setViaPointList(viaPointList);
                    PlanRouteDialog.newInstance().show(
                            getActivity().getSupportFragmentManager(), "PlanRouteDialog");
                } else {
                    SimpleMessageDialog.newInstance(getResources().getString(R.string.errorNoRouteSelected))
                        .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
                }
                return true;
            case R.id.menuItemRecalculateWithCurrentPosition:
                if (startPoint != null && destinationPoint != null) {
                    routeSettings.setStartPoint(
                            PositionManager.getInstance(getActivity()).getCurrentLocation());
                    routeSettings.setDestinationPoint(destinationPoint);
                    routeSettings.setViaPointList(viaPointList);
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

    @Override public void onFragmentEnabled() {
        buttonPlanRoute.setVisibility(View.GONE);
        scrollviewDescription.setVisibility(View.GONE);
        labelRouteSegmentDescription.setTag(null);
        labelRoutePointDescription.setTag(null);
        // request route
        RouteManager.getInstance(getActivity()).requestRoute(
                RouterFragment.this,
                settingsManagerInstance.getRouteSettings().getSelectedRouteId());
    }

	@Override public void onFragmentDisabled() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(newLocationAndDirectionReceiver);
    }

	@Override public void routeCalculationFinished(int returnCode, String returnMessage, int routeId) {
    }

	@Override public void routeRequestFinished(int returnCode, String returnMessage, Route route) {
        if (returnCode == Constants.RC.OK) {
            if (route == null) {
                // no route available yet
                buttonPlanRoute.setVisibility(View.VISIBLE);
            } else {
                scrollviewDescription.setVisibility(View.VISIBLE);
                RouteObject currentRouteObject = route.getCurrentRouteObject();
                if (currentRouteObject.getRouteSegment().equals(RouteObject.getDummyRouteSegment(getActivity()))) {
                    labelRouteSegmentDescription.setText("");
                    labelRouteSegmentDescription.setVisibility(View.GONE);
                } else {
                    labelRouteSegmentDescription.setText(
                            String.format(
                                getResources().getString(R.string.labelNextRouteSegment),
                                currentRouteObject.getRouteSegment().toString())
                            );
                    labelRouteSegmentDescription.setTag(currentRouteObject.getRouteSegment());
                    labelRouteSegmentDescription.setVisibility(View.VISIBLE);
                }
                if (currentRouteObject.getRoutePoint().equals(PositionManager.getDummyLocation(getActivity()))) {
                    labelRoutePointDescription.setText("");
                    labelRoutePointDescription.setVisibility(View.GONE);
                } else {
                    labelRoutePointDescription.setText(
                            String.format(
                                getResources().getString(R.string.labelNextRoutePoint),
                                currentRouteObject.getRoutePoint().toString())
                            );
                    labelRoutePointDescription.setTag(currentRouteObject.getRoutePoint());
                    labelRoutePointDescription.setVisibility(View.VISIBLE);
                }
                labelRoutePosition.setText(
                        String.format(
                            getResources().getString(R.string.labelRoutePosition),
                            route.getRouteObjectList().indexOf(currentRouteObject)+1,
                            route.getRouteObjectList().size())
                        );
                // request current location and direction for labelDistanceAndBearing field
                IntentFilter filter = new IntentFilter();
                filter.addAction(Constants.ACTION_NEW_LOCATION);
                filter.addAction(Constants.ACTION_NEW_DIRECTION);
                LocalBroadcastManager.getInstance(getActivity()).registerReceiver(newLocationAndDirectionReceiver, filter);
                PositionManager.getInstance(getActivity()).requestCurrentLocation();
            }
        } else {
            SimpleMessageDialog.newInstance(returnMessage)
                .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
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
                                context.getResources().getString(R.string.labelPointDistanceAndBearing),
                                currentRouteObject.getRoutePoint().distanceFromCurrentLocation(),
                                StringUtility.formatInstructionDirection(
                                    context, currentRouteObject.getRoutePoint().bearingFromCurrentLocation()))
                        );
                }
            }
        }
    };


    public static class RouteCommandsDialog extends DialogFragment implements RouteListener {

        // Store instance variables
        private AccessDatabase accessDatabaseInstance;
        private RouteManager routeManagerInstance;
        private int routeId;

        // ui components
        private ListView listViewRouteObjects;
        private TextView labelRouteSummary, labelEmptyRouteObjectListView;

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
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            routeId = getArguments().getInt("routeId");

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_route_commands, nullParent);

            listViewRouteObjects = (ListView) view.findViewById(R.id.listViewRouteObjects);
            listViewRouteObjects.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    final RouteObject routeObject = (RouteObject) parent.getItemAtPosition(position);
                    PopupMenu popupMore = new PopupMenu(getActivity(), view);
                    popupMore.inflate(R.menu.menu_route_commands_list_view);
                    if (routeObject.getRouteSegment().equals(RouteObject.getDummyRouteSegment(getActivity()))) {
                        popupMore.getMenu().findItem(R.id.menuItemShowSegmentDetails).setVisible(false);
                    }
                    popupMore.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.menuItemShowSegmentDetails:
                                    Intent segmentDetailsIntent = new Intent(getActivity(), SegmentDetailsActivity.class);
                                    try {
                                        segmentDetailsIntent.putExtra(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, routeObject.getRouteSegment().toJson().toString());
                                    } catch (JSONException e) {
                                        segmentDetailsIntent.putExtra(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "");
                                    }
                                    getActivity().startActivity(segmentDetailsIntent);
                                    return true;
                                case R.id.menuItemShowPointDetails:
                                    Intent pointDetailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                                    try {
                                        pointDetailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, routeObject.getRoutePoint().toJson().toString());
                                    } catch (JSONException e) {
                                        pointDetailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                                    }
                                    getActivity().startActivity(pointDetailsIntent);
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

            labelRouteSummary = (TextView) view.findViewById(R.id.labelRouteSummary);
            labelEmptyRouteObjectListView    = (TextView) view.findViewById(R.id.labelEmptyRouteObjectListView);
            listViewRouteObjects.setEmptyView(labelEmptyRouteObjectListView);

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.routeCommandsDialogTitle))
                .setView(view)
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
            listViewRouteObjects.setAdapter(null);
            labelRouteSummary.setVisibility(View.GONE);
            labelEmptyRouteObjectListView.setText(getResources().getString(R.string.messagePleaseWait));
            RouteManager.getInstance(getActivity()).requestRoute(RouteCommandsDialog.this, routeId);
        }

    	@Override public void routeCalculationFinished(int returnCode, String returnMessage, int routeId) {
        }

        @Override public void routeRequestFinished(int returnCode, String returnMessage, Route route) {
            labelEmptyRouteObjectListView.setText(returnMessage);
            if (returnCode == Constants.RC.OK) {
                if (route == null) {
                    // no route available yet
                    labelEmptyRouteObjectListView.setText(
                            getResources().getString(R.string.labelEmptyRouteObjectListView));
                } else {
                    labelRouteSummary.setText(route.getDescription());
                    labelRouteSummary.setVisibility(View.VISIBLE);
                    listViewRouteObjects.setAdapter(
                            new ArrayAdapter<RouteObject>(
                                getActivity(),
                                android.R.layout.simple_list_item_1,
                                route.getRouteObjectList())
                            );
                }
            }
        }
    }


    public static class RouteHistoryDialog extends DialogFragment {

        // Store instance variables
        private InputMethodManager imm;
        private AccessDatabase accessDatabaseInstance;
        private SettingsManager settingsManagerInstance;
        
        // ui components
        private EditText editSearchHistory;
        private ListView listViewRoutes;

        public static RouteHistoryDialog newInstance() {
            RouteHistoryDialog routeHistoryDialogInstance = new RouteHistoryDialog();
            return routeHistoryDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_route_history, nullParent);

            editSearchHistory = (EditText) view.findViewById(R.id.editInput);
            editSearchHistory.setInputType(InputType.TYPE_CLASS_TEXT);
            editSearchHistory.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editSearchHistory.setHint(getResources().getString(R.string.editHintRouteSearch));
            editSearchHistory.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        fillListView();
                        return true;
                    }
                    return false;
                }
            });

            ImageButton buttonDelete = (ImageButton) view.findViewById(R.id.buttonDelete);
            buttonDelete.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    // clear edit text
                    editSearchHistory.setText("");
                    // show keyboard
                    imm.showSoftInput(editSearchHistory, InputMethodManager.SHOW_IMPLICIT);
                }
            });

            listViewRoutes = (ListView) view.findViewById(R.id.listViewRoutes);
            listViewRoutes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    int selectedRouteId = -1;
                    int index = 0;
                    for(Integer routeId : accessDatabaseInstance.getRouteMap().keySet()) {
                        if (index == position) {
                            selectedRouteId = routeId;
                            break;
                        }
                        index += 1;
                    }
                    if (selectedRouteId > -1) {
                        // update route settings
                        RouteSettings routeSettings = settingsManagerInstance.getRouteSettings();
                        routeSettings.setSelectedRouteId(selectedRouteId);
                        routeSettings.setStartPoint(
                                accessDatabaseInstance.getStartPointOfRoute(selectedRouteId));
                        routeSettings.setDestinationPoint(
                                accessDatabaseInstance.getDestinationPointOfRoute(selectedRouteId));
                        routeSettings.setViaPointList(
                                accessDatabaseInstance.getViaPointListOfRoute(selectedRouteId));
                        // reload ui
                        Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                    }
                    dismiss();
                }
            });

            TextView labelEmptyRouteListView    = (TextView) view.findViewById(R.id.labelEmptyRouteListView);
            listViewRoutes.setEmptyView(labelEmptyRouteListView);

            // fill list
            fillListView();

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.routeHistoryDialogTitle))
                .setView(view)
                .setNegativeButton(
                        getResources().getString(R.string.dialogCancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss();
                            }
                        }
                        )
                .create();
        }

        private void fillListView() {
            // search string
            String searchFor = editSearchHistory.getText().toString().trim().replaceAll("\\s", ".*");
            if (! searchFor.equals("")) {
                searchFor = String.format("(?i:.*%1$s.*)", searchFor);
            }
            System.out.println("xxx search: " + searchFor);
            ArrayList<String> routeNameList = new ArrayList<String>();
            for (Map.Entry<Integer,String> route : accessDatabaseInstance.getRouteMap().entrySet()) {
                System.out.println("xxx obj: " + route.getValue());
                if (searchFor.equals("")
                        || route.getValue().matches(searchFor)) {
                    routeNameList.add(route.getValue());
                }
            }
            listViewRoutes.setAdapter(
                    new ArrayAdapter<String>(
                        getActivity(), android.R.layout.simple_list_item_1, routeNameList)
                    );
        }
    }

}
