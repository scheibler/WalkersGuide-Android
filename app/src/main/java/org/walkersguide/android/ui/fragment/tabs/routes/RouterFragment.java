package org.walkersguide.android.ui.fragment.tabs.routes;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Build;
import android.annotation.SuppressLint;
import org.walkersguide.android.ui.view.IntersectionScheme;
import android.widget.ImageButton;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.MainActivityController;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.sensor.bearing.AcceptNewBearing;
import org.walkersguide.android.data.angle.RelativeBearing;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.bearing.BearingSensorValue;
import org.walkersguide.android.server.wg.p2p.P2pRouteRequest;
import org.walkersguide.android.ui.view.RouteObjectView;
import org.walkersguide.android.ui.view.TextViewAndActionButton;
import android.widget.Toast;
import timber.log.Timber;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Vibrator;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.TextView;



import org.walkersguide.android.data.object_with_id.route.RouteObject;
import org.walkersguide.android.sensor.position.AcceptNewPosition;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.tts.TTSWrapper;
import androidx.fragment.app.Fragment;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.Route;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import java.util.Locale;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.data.ObjectWithId.SortByBearingRelativeTo;
import org.walkersguide.android.util.Helper;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import androidx.fragment.app.DialogFragment;
import android.app.Dialog;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.widget.ListView;
import androidx.core.util.Pair;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AbsListView;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.data.Angle;
import java.util.Collections;
import android.text.TextUtils;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.widget.LinearLayout;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import org.walkersguide.android.ui.UiHelper;
import android.text.SpannableString;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;


public class RouterFragment extends Fragment implements FragmentResultListener, MenuProvider {


	// instance constructor

	public static RouterFragment newInstance() {
		RouterFragment fragment = new RouterFragment();
		return fragment;
	}


    // fragment
    private Route route;
    private RouteBroadcastReceiver routeBroadcastReceiver;
	private SettingsManager settingsManagerInstance;
    private TTSWrapper ttsWrapperInstance;

    private TextViewAndActionButton layoutRoute;
    private TextView labelTotalDistance;
    private TextView labelHeading;
    private ImageButton buttonJumpToRoutePoint;
    private RouteObjectView layoutCurrentRouteObject;
    // optional intersection structure details
    private LinearLayout layoutIntersectionStructure;
    private TextView labelIntersectionStructure;
    private IntersectionScheme intersectionScheme;
    // bottom
    private TextView labelDistanceAndBearing;
    private ImageButton buttonPreviousRouteObject, buttonNextRouteObject;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        routeBroadcastReceiver = new RouteBroadcastReceiver();
		settingsManagerInstance = SettingsManager.getInstance();
        ttsWrapperInstance = TTSWrapper.getInstance();

        getChildFragmentManager()
            .setFragmentResultListener(
                    JumpToRoutePointDialog.REQUEST_SELECT_ROUTE_POINT, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(JumpToRoutePointDialog.REQUEST_SELECT_ROUTE_POINT)) {
            int newIndex = bundle.getInt(JumpToRoutePointDialog.EXTRA_ROUTE_POINT_INDEX, -1);
            if (route != null && newIndex >= 0) {
                route.jumpToRouteObjectAt(newIndex);
                updateUi();
            }
        }
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_router_fragment, menu);
    }

    @Override public void onPrepareMenu(@NonNull Menu menu) {
        // auto-skip
        MenuItem menuItemAutoSkipToNextRoutePoint = menu.findItem(R.id.menuItemAutoSkipToNextRoutePoint);
        if (menuItemAutoSkipToNextRoutePoint != null) {
            menuItemAutoSkipToNextRoutePoint.setChecked(
                    settingsManagerInstance.getAutoSkipToNextRoutePoint());
        }
        // precise bearings
        MenuItem menuItemShowPreciseBearingValues = menu.findItem(R.id.menuItemShowPreciseBearingValues);
        if (menuItemShowPreciseBearingValues != null) {
            menuItemShowPreciseBearingValues.setChecked(
                    settingsManagerInstance.getShowPreciseBearingValues());
        }
        // intersection layout details
        MenuItem menuItemShowIntersectionLayoutDetails = menu.findItem(R.id.menuItemShowIntersectionLayoutDetails);
        if (menuItemShowIntersectionLayoutDetails != null) {
            menuItemShowIntersectionLayoutDetails.setChecked(
                    settingsManagerInstance.getShowIntersectionLayoutDetails());
            // only show intersection structure details menu item on android >= 7
            // due to stream() method in "updateUi"
            menuItemShowIntersectionLayoutDetails.setVisible(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
        }
    }

    @Override public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemRecalculateWithCurrentPosition
                || item.getItemId() == R.id.menuItemRecalculateOriginalRoute
                || item.getItemId() == R.id.menuItemRecalculateReturnRoute) {
            P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
            if (route == null) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.errorWgReqNoRouteSelected),
                        Toast.LENGTH_LONG).show();
                return true;
            }

            // start point
            if (item.getItemId() == R.id.menuItemRecalculateWithCurrentPosition) {
                Point currentLocation = PositionManager.getInstance().getCurrentLocation();
                if (currentLocation != null) {
                    p2pRouteRequest.setStartPoint(currentLocation);
                } else {
                    Toast.makeText(
                            getActivity(),
                            GlobalInstance.getStringResource(R.string.errorNoLocationFound),
                            Toast.LENGTH_LONG).show();
                    return true;
                }
            } else if (item.getItemId() == R.id.menuItemRecalculateReturnRoute) {
                p2pRouteRequest.setStartPoint(route.getDestinationPoint());
            } else {
                p2pRouteRequest.setStartPoint(route.getStartPoint());
            }

            // destination point
            if (item.getItemId() == R.id.menuItemRecalculateReturnRoute) {
                p2pRouteRequest.setDestinationPoint(route.getStartPoint());
            } else {
                p2pRouteRequest.setDestinationPoint(route.getDestinationPoint());
            }

            // set via points and show plan route dialog
            p2pRouteRequest.setViaPoint1(route.getViaPoint1());
            p2pRouteRequest.setViaPoint2(route.getViaPoint2());
            p2pRouteRequest.setViaPoint3(route.getViaPoint3());
            settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
            mainActivityController.openPlanRouteDialog();

        } else if (item.getItemId() == R.id.menuItemAutoSkipToNextRoutePoint) {
            settingsManagerInstance.setAutoSkipToNextRoutePoint(
                    ! settingsManagerInstance.getAutoSkipToNextRoutePoint());

        } else if (item.getItemId() == R.id.menuItemShowPreciseBearingValues) {
            settingsManagerInstance.setShowPreciseBearingValues(
                    ! settingsManagerInstance.getShowPreciseBearingValues());
            if (route != null) {
                updateUi();
            }

        } else if (item.getItemId() == R.id.menuItemShowIntersectionLayoutDetails) {
            settingsManagerInstance.setShowIntersectionLayoutDetails(
                    ! settingsManagerInstance.getShowIntersectionLayoutDetails());
            if (route != null) {
                updateUi();
            }

        } else {
            return false;
        }
        return true;
    }


    /**
     * create view
     */

    private MainActivityController mainActivityController;

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            if (activity instanceof MainActivity) {
                mainActivityController = (MainActivityController) ((MainActivity) activity);
            }
        }
    }

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_router, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        // content layout

        layoutRoute = (TextViewAndActionButton) view.findViewById(R.id.layoutRoute);
        labelTotalDistance = (TextView) view.findViewById(R.id.labelTotalDistance);

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        buttonJumpToRoutePoint = (ImageButton) view.findViewById(R.id.buttonJumpToRoutePoint);
        buttonJumpToRoutePoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (route != null) {
                    JumpToRoutePointDialog.newInstance(route)
                        .show(getChildFragmentManager(), "JumpToRoutePointDialog");
                }
            }
        });

        layoutCurrentRouteObject = (RouteObjectView) view.findViewById(R.id.layoutCurrentRouteObject);

        layoutIntersectionStructure = (LinearLayout) view.findViewById(R.id.layoutIntersectionStructure);
        labelIntersectionStructure = (TextView) view.findViewById(R.id.labelIntersectionStructure);
        intersectionScheme = (IntersectionScheme) view.findViewById(R.id.intersectionScheme);

        // bottom layout
        labelDistanceAndBearing = (TextView) view.findViewById(R.id.labelDistanceAndBearing);

        buttonPreviousRouteObject = (ImageButton) view.findViewById(R.id.buttonPreviousRouteObject);
        buttonPreviousRouteObject.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (route.hasPreviousRouteObject()) {
                    onPostProcessSkipRouteObjectManually(
                            route.skipToPreviousRouteObject());
                }
            }
        });

        buttonNextRouteObject = (ImageButton) view.findViewById(R.id.buttonNextRouteObject);
        buttonNextRouteObject.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (route.hasNextRouteObject()) {
                    onPostProcessSkipRouteObjectManually(
                            route.skipToNextRouteObject());
                }
            }
        });
    }


    /**
     * pause and resume
     */
    private AcceptNewPosition acceptNewPositionForTtsAnnouncement;

    @Override public void onPause() {
        super.onPause();
        if (route != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(routeBroadcastReceiver);
        }
    }

    @Override public void onResume() {
        super.onResume();
        Timber.d("onResume");
        layoutRoute.setVisibility(View.GONE);
        labelTotalDistance.setVisibility(View.GONE);
        buttonJumpToRoutePoint.setVisibility(View.GONE);
        layoutCurrentRouteObject.setVisibility(View.GONE);
        layoutIntersectionStructure.setVisibility(View.GONE);
        labelDistanceAndBearing.setVisibility(View.GONE);
        buttonPreviousRouteObject.setVisibility(View.GONE);
        buttonNextRouteObject.setVisibility(View.GONE);

        // load route from settings
        route = settingsManagerInstance.getSelectedRoute();
        if (route != null) {
            layoutRoute.setVisibility(View.VISIBLE);
            labelTotalDistance.setVisibility(View.VISIBLE);
            buttonJumpToRoutePoint.setVisibility(View.VISIBLE);
            layoutCurrentRouteObject.setVisibility(View.VISIBLE);
            labelDistanceAndBearing.setVisibility(View.VISIBLE);
            buttonPreviousRouteObject.setVisibility(View.VISIBLE);
            buttonNextRouteObject.setVisibility(View.VISIBLE);
            updateUi();

            // broadcast filter
            IntentFilter filter = new IntentFilter();
            filter.addAction(PositionManager.ACTION_NEW_LOCATION);
            filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
            filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_SATELLITE);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(routeBroadcastReceiver, filter);

            // request current location for labelDistanceAndBearing field
            acceptNewPositionForTtsAnnouncement = AcceptNewPosition.newInstanceForTtsAnnouncement();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override public void run() {
                    // wait, until onResume is finished and the ui has focus
                    PositionManager.getInstance().requestCurrentLocation();
                }
            }, 200);

        } else {
            labelHeading.setText(
                    GlobalInstance.getStringResource(R.string.errorWgReqNoRouteSelected));
        }
    }

    private void onPostProcessSkipRouteObjectManually(boolean skipWasSuccessful) {
        if (skipWasSuccessful) {
            ttsWrapperInstance.announce(
                    route.getCurrentRouteObject().formatSegmentInstruction());
        }
        updateUi();
    }

    private void updateUi() {
        RouteObject currentRouteObject = route.getCurrentRouteObject();

        layoutRoute.configureAsSingleObject(route);
        labelTotalDistance.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelTotalDistance),
                    route.getElapsedLength(),
                    GlobalInstance.getPluralResource(
                        R.plurals.meters, route.getTotalLength()))
                );

        labelHeading.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelRoutePosition),
                    route.getCurrentPosition() + 1,
                    route.getRouteObjectList().size())
                );
        layoutCurrentRouteObject.configureAsSingleObject(currentRouteObject);

        // intersection structure
        layoutIntersectionStructure.setVisibility(View.GONE);
        if (currentRouteObject.getPoint() instanceof Intersection
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N   // due to stream() method below
                && settingsManagerInstance.getShowIntersectionLayoutDetails()) {
            Intersection intersection = (Intersection) currentRouteObject.getPoint();

            Bearing inverseBearingOfPreviousRouteSegment = null;
            for (IntersectionSegment intersectionSegment : intersection.getSegmentList()) {
                if (intersectionSegment.isPartOfPreviousRouteSegment()) {
                    inverseBearingOfPreviousRouteSegment = intersectionSegment.getBearing().inverse();
                    break;
                }
            }
            if (inverseBearingOfPreviousRouteSegment != null) {

                CharSequence formattedIntersectionStructure = new SpannableString("");
                LinkedHashMap<RelativeBearing,IntersectionSegment> intersectionSegmentRelativeToInstructionMap =
                    new LinkedHashMap<RelativeBearing,IntersectionSegment>();
                // bearing offset = 157 -> sort the ways, which are strongly to the left of the user, to the top of the list
                SortByBearingRelativeTo comparator = new ObjectWithId.SortByBearingRelativeTo(
                        inverseBearingOfPreviousRouteSegment, Angle.Quadrant.Q3.max, true);

                int index = 0;
                for (IntersectionSegment intersectionSegment : intersection.getSegmentList().stream().sorted(comparator).collect(Collectors.toList())) {
                    RelativeBearing relativeBearingIntersectionSegment = intersectionSegment
                        .getBearing()
                        .relativeTo(inverseBearingOfPreviousRouteSegment);
                    intersectionSegmentRelativeToInstructionMap.put(
                            relativeBearingIntersectionSegment, intersectionSegment);
                    // instruction structure label text (preserves text formatting)
                    formattedIntersectionStructure = TextUtils.concat(
                            formattedIntersectionStructure,
                            UiHelper.bold(
                                relativeBearingIntersectionSegment.getDirection().toString()),
                            ":\n",
                            intersectionSegment.isPartOfNextRouteSegment()
                            ? UiHelper.red(intersectionSegment.getName())
                            : intersectionSegment.getName());
                    if (index < intersection.getSegmentList().size()-1) {
                        formattedIntersectionStructure = TextUtils.concat(
                                formattedIntersectionStructure, ",\n");
                    }
                    index++;
                }

                labelIntersectionStructure.setText(formattedIntersectionStructure);
                intersectionScheme.configureView(
                        intersection.getName(), intersectionSegmentRelativeToInstructionMap);
                layoutIntersectionStructure.setVisibility(View.VISIBLE);
            }
        }

        updateDistanceAndBearingLabel(currentRouteObject);
    }

    private void updateDistanceAndBearingLabel(RouteObject currentRouteObject) {
        labelDistanceAndBearing.setText(
                currentRouteObject
                .getPoint()
                .formatDistanceAndRelativeBearingFromCurrentLocation(
                    R.plurals.meter, settingsManagerInstance.getShowPreciseBearingValues()));
    }


    private class RouteBroadcastReceiver extends BroadcastReceiver {
        private static final int SHORTLY_BEFORE_ARRIVAL_THRESHOLD_IN_METERS = 30;

        // distance label
        private AcceptNewPosition acceptNewPositionForDistanceLabel = AcceptNewPosition.newInstanceForDistanceLabelUpdate();
        private AcceptNewBearing acceptNewBearing = AcceptNewBearing.newInstanceForDistanceLabelUpdate();

        private RouteObject lastRouteObject = null;

        private boolean shortlyBeforeArrivalAnnounced, arrivalAnnounced;
        private long arrivalTime;

        @Override public void onReceive(Context context, Intent intent) {
            RouteObject currentRouteObject = route.getCurrentRouteObject();

            if (! getActivity().hasWindowFocus()) {
                if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)
                        && intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION) != null
                        && intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false)) {
                    updateDistanceAndBearingLabel(currentRouteObject);
                }
                return;
            }

            if (! currentRouteObject.equals(lastRouteObject)) {
                // skipped to next route object
                this.lastRouteObject = currentRouteObject;
                this.shortlyBeforeArrivalAnnounced = false;
                this.arrivalAnnounced = false;
                this.arrivalTime = System.currentTimeMillis();
            }

            if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                Point currentLocation = (Point) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION);
                if (currentLocation != null) {
                    if (intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false)
                            || acceptNewPositionForDistanceLabel.updatePoint(currentLocation)) {
                        updateDistanceAndBearingLabel(currentRouteObject);
                    }
                    if (acceptNewPositionForTtsAnnouncement.updatePoint(currentLocation)) {
                        ttsWrapperInstance.announce(
                                currentRouteObject.getPoint().formatDistanceAndRelativeBearingFromCurrentLocation(R.plurals.meter));
                    }
                }

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                Bearing currentBearing = (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (currentBearing != null
                        && acceptNewBearing.updateBearing(currentBearing)) {
                    updateDistanceAndBearingLabel(currentRouteObject);
                }

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_SATELLITE)) {
                BearingSensorValue bearingValueFromSatellite = (BearingSensorValue) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (bearingValueFromSatellite != null) {
                    // announce shortly before arrival
                    if (
                               ! shortlyBeforeArrivalAnnounced
                            && ! currentRouteObject.getIsFirstRouteObject()
                            && currentRouteObject.getPoint().distanceFromCurrentLocation() < SHORTLY_BEFORE_ARRIVAL_THRESHOLD_IN_METERS
                            && currentRouteObject.getSegment().getDistance() > SHORTLY_BEFORE_ARRIVAL_THRESHOLD_IN_METERS) {
                        announceShortlyBeforeArrival(currentRouteObject);
                    }
                    // announce arrival
                    if (
                               ! arrivalAnnounced
                            && nextRouteObjectWithinRange(currentRouteObject, bearingValueFromSatellite)
                            && ! walkingReverseDetected(currentRouteObject, bearingValueFromSatellite)
                            && (System.currentTimeMillis() - arrivalTime) > 5000) {
                        announceArrival(currentRouteObject);
                    }
                }
            }
        }

        private boolean nextRouteObjectWithinRange(RouteObject currentRouteObject, BearingSensorValue bearingValueFromSatellite) {
            if (! PositionManager.getInstance().hasCurrentLocation()) {
                return false;
            }

            Integer distance = currentRouteObject
                .getPoint()
                .distanceFromCurrentLocation();
            RelativeBearing relativeBearing = currentRouteObject
                .getPoint()
                .bearingFromCurrentLocation()
                .relativeTo(bearingValueFromSatellite);

            if (distance < 25
                    && relativeBearing.withinRange(80, 280)) {
                return true;
            } else if (distance < 20
                    && relativeBearing.withinRange(65, 295)) {
                return true;
            } else if (distance < 15
                    && relativeBearing.withinRange(50, 310)) {
                return true;
            } else if (distance < 10
                    && relativeBearing.withinRange(35, 325)) {
                return true;
            } else if (distance < 5) {
                return true;
            }
            return false;
        }

        private boolean walkingReverseDetected(RouteObject currentRouteObject, BearingSensorValue bearingValueFromSatellite) {
            if (currentRouteObject.getIsFirstRouteObject() || currentRouteObject.getIsLastRouteObject()) {
                return false;
            }
            return currentRouteObject
                .getSegment()
                .getBearing()
                .shiftBy(currentRouteObject.getTurn().getDegree())
                .shiftBy(-180)      // reverse
                .relativeTo(bearingValueFromSatellite)
                .getDirection() == RelativeBearing.Direction.STRAIGHT_AHEAD;
        }

        private void announceShortlyBeforeArrival(RouteObject currentRouteObject) {
            shortlyBeforeArrivalAnnounced = true;
            ttsWrapperInstance.announce(
                    route.formatShortlyBeforeArrivalAtPointMessage());
        }

        private void announceArrival(RouteObject currentRouteObject) {
            shortlyBeforeArrivalAnnounced = true;
            arrivalAnnounced = true;
            ttsWrapperInstance.announce(
                    route.formatArrivalAtPointMessage());
            Helper.vibrateOnce(Helper.VIBRATION_DURATION_LONG);

            // auto jump to next route point
            if (route.hasNextRouteObject()
                    && settingsManagerInstance.getAutoSkipToNextRoutePoint()) {
                route.skipToNextRouteObject();
                updateUi();
            }
        }
    }


    /**
     * jump to route point dialog
     */

    public static class JumpToRoutePointDialog extends DialogFragment {
        public static final String REQUEST_SELECT_ROUTE_POINT = "selectRoutePoint";
        public static final String EXTRA_ROUTE_POINT_INDEX = "routePointIndex";


        // instance constructors

        public static JumpToRoutePointDialog newInstance(Route route) {
            ArrayList<Point> pointList = new ArrayList<Point>();
            for (RouteObject routeObject : route.getRouteObjectList()) {
                pointList.add(routeObject.getPoint());
            }
            JumpToRoutePointDialog dialog= new JumpToRoutePointDialog();
            Bundle args = new Bundle();
            args.putSerializable(KEY_POINT_LIST, pointList);
            args.putInt(KEY_SELECTED_LIST_INDEX, route.getCurrentPosition());
            dialog.setArguments(args);
            return dialog;
        }


        // dialog
        private static final String KEY_POINT_LIST = "pointList";
        private static final String KEY_SELECTED_LIST_INDEX = "selectedListIndex";
        private static final String KEY_LIST_POSITION = "listPosition";

        private ArrayList<Point> pointList;
        private int selectedListIndex;
        private int listPosition;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            pointList = (ArrayList<Point>) getArguments().getSerializable(KEY_POINT_LIST);
            selectedListIndex = getArguments().getInt(KEY_SELECTED_LIST_INDEX);
            if (savedInstanceState != null) {
                listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
            } else {
                listPosition = selectedListIndex;
            }

            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.jumpToRoutePointDialogTitle))
                .setItems(
                        new String[]{getResources().getString(R.string.messagePleaseWait)},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                        )
                .setNeutralButton(
                        getResources().getString(R.string.buttonScrollToClosestPoint),
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

                Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        AlertDialog dialog = (AlertDialog) getDialog();
                        if (dialog != null) {
                            ListView listViewItems = (ListView) dialog.getListView();
                            if (listViewItems != null) {
                                Pair<Integer,Integer> closest = null;
                                for (int i=0; i<pointList.size(); i++) {
                                    Integer distanceFromCurrentLocation = pointList.get(i).distanceFromCurrentLocation();
                                    if (distanceFromCurrentLocation != null
                                            && (closest == null || distanceFromCurrentLocation < closest.second)) {
                                        closest = Pair.create(i, distanceFromCurrentLocation);
                                    }
                                }
                                if (closest != null) {
                                    listViewItems.setSelection(closest.first);
                                }
                            }
                        }
                    }
                });

                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dismiss();
                    }
                });

                ListView listViewItems = (ListView) dialog.getListView();
                listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                        Bundle result = new Bundle();
                        result.putSerializable(EXTRA_ROUTE_POINT_INDEX, position);
                        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_ROUTE_POINT, result);
                        dismiss();
                    }
                });

                // fill listview
                ArrayList<String> pointToStringList = new ArrayList<String>();
                int pointNumber = 1;
                for (Point point : pointList) {
                    pointToStringList.add(
                            String.format(
                                Locale.ROOT,
                                "%1$d.\t%2$s\n\t%3$s",
                                pointNumber++,
                                point.getName(),
                                point.formatDistanceAndRelativeBearingFromCurrentLocation(R.plurals.inMeters))
                            );
                }
                listViewItems.setAdapter(
                        new ArrayAdapter<String>(
                            getActivity(), android.R.layout.select_dialog_singlechoice, pointToStringList));
                listViewItems.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                listViewItems.setItemChecked(selectedListIndex, true);

                // list position
                listViewItems.setSelection(listPosition);
                listViewItems.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
                    @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        if (listPosition != firstVisibleItem) {
                            listPosition = firstVisibleItem;
                        }
                    }
                });
            }
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putInt(KEY_LIST_POSITION,  listPosition);
        }
    }

}
