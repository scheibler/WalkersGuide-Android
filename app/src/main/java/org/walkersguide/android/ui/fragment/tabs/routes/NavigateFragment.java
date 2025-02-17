package org.walkersguide.android.ui.fragment.tabs.routes;

import org.walkersguide.android.ui.view.DistanceAndBearingView;
import org.walkersguide.android.data.object_with_id.Segment.SortByBearingRelativeTo;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Build;
import org.walkersguide.android.ui.view.IntersectionScheme;
import android.widget.ImageButton;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.MainActivityController;
import org.walkersguide.android.data.angle.RelativeBearing;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.bearing.BearingSensorValue;
import org.walkersguide.android.server.wg.p2p.P2pRouteRequest;
import org.walkersguide.android.ui.view.RouteObjectView;
import org.walkersguide.android.ui.view.ObjectWithIdView;
import android.widget.Toast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;



import org.walkersguide.android.data.object_with_id.route.RouteObject;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.tts.TTSWrapper.MessageType;
import androidx.fragment.app.Fragment;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.Route;
import androidx.annotation.NonNull;
import org.walkersguide.android.util.Helper;
import android.os.Handler;
import android.os.Looper;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.data.Angle;
import android.text.TextUtils;
import android.widget.LinearLayout;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import org.walkersguide.android.ui.UiHelper;
import android.text.SpannableString;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;


public class NavigateFragment extends Fragment implements MenuProvider {


    // instance constructor

    public static NavigateFragment newInstance(Route route, boolean showObjectWithIdView) {
        NavigateFragment fragment = new NavigateFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ROUTE, route);
        args.putBoolean(KEY_SHOW_OBJECT_WITH_ID_VIEW, showObjectWithIdView);
        fragment.setArguments(args);
        return fragment;
    }


    // fragment
    private static final String KEY_ROUTE = "route";
    private static final String KEY_SHOW_OBJECT_WITH_ID_VIEW = "showObjectWithIdView";

    private Route route;
    private boolean showObjectWithIdView;

    private RouteBroadcastReceiver routeBroadcastReceiver;
    private SettingsManager settingsManagerInstance;
    private TTSWrapper ttsWrapperInstance;

    private ObjectWithIdView layoutRoute;
    private TextView labelHeading;
    private RouteObjectView layoutCurrentRouteObject;
    // optional intersection structure details
    private LinearLayout layoutIntersectionStructure;
    private TextView labelIntersectionStructure;
    private IntersectionScheme intersectionScheme;
    // bottom
    private DistanceAndBearingView labelDistanceAndBearing;
    private ImageButton buttonPreviousRouteObject, buttonNextRouteObject;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        routeBroadcastReceiver = new RouteBroadcastReceiver();
        settingsManagerInstance = SettingsManager.getInstance();
        ttsWrapperInstance = TTSWrapper.getInstance();

        if (savedInstanceState != null) {
            route = (Route) savedInstanceState.getSerializable(KEY_ROUTE);
        } else if (getArguments() != null) {
            route = (Route) getArguments().getSerializable(KEY_ROUTE);
        }
        showObjectWithIdView = getArguments() != null
            ? getArguments().getBoolean(KEY_SHOW_OBJECT_WITH_ID_VIEW) : false;
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_navigate_fragment, menu);
    }

    @Override public void onPrepareMenu(@NonNull Menu menu) {
        // auto-skip
        MenuItem menuItemAutoSkipToNextRoutePoint = menu.findItem(R.id.menuItemAutoSkipToNextRoutePoint);
        if (menuItemAutoSkipToNextRoutePoint != null) {
            menuItemAutoSkipToNextRoutePoint.setChecked(
                    settingsManagerInstance.getAutoSkipToNextRoutePoint());
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
        // precise bearings
        MenuItem menuItemShowPreciseBearingValues = menu.findItem(R.id.menuItemShowPreciseBearingValues);
        if (menuItemShowPreciseBearingValues != null) {
            menuItemShowPreciseBearingValues.setChecked(
                    settingsManagerInstance.getShowPreciseBearingValues());
        }
        // bearing indicator
        MenuItem menuItemShowBearingIndicator = menu.findItem(R.id.menuItemShowBearingIndicator);
        if (menuItemShowBearingIndicator != null) {
            menuItemShowBearingIndicator.setChecked(
                    settingsManagerInstance.getShowBearingIndicator());
        }
    }

    @Override public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemRecalculate) {
            if (route == null) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.errorWgReqNoRouteSelected),
                        Toast.LENGTH_LONG).show();
                return true;
            }

            // get current location
            Point currentLocation = PositionManager.getInstance().getCurrentLocation();
            if (currentLocation == null) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.errorNoLocationFound),
                        Toast.LENGTH_LONG).show();
                return true;
            }

            // create new route request
            P2pRouteRequest p2pRouteRequest = P2pRouteRequest.getDefault();
            // new start but same destination point
            p2pRouteRequest.setStartPoint(currentLocation);
            p2pRouteRequest.setDestinationPoint(route.getDestinationPoint());

            // set via points if not already passed by
            if (route.hasViaPoint()) {
                for (int i=route.getCurrentPosition()+1;
                         i<route.getRouteObjectList().size();
                         i++) {
                    Point point = route.getRouteObjectList().get(i).getPoint();
                    // via point 1
                    if (point.equals(route.getViaPoint1())) {
                        p2pRouteRequest.setViaPoint1(route.getViaPoint1());
                    }
                    // via point 2
                    if (point.equals(route.getViaPoint2())) {
                        p2pRouteRequest.setViaPoint2(route.getViaPoint2());
                    }
                    // via point 3
                    if (point.equals(route.getViaPoint3())) {
                        p2pRouteRequest.setViaPoint3(route.getViaPoint3());
                    }
                }
            }

            // recalculate route
            settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
            mainActivityController.openPlanRouteDialog(true);

        } else if (item.getItemId() == R.id.menuItemJumpToNearestPoint) {
            if (route == null) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.errorWgReqNoRouteSelected),
                        Toast.LENGTH_LONG).show();
                return true;
            }

            route.jumpToRouteObject(route.getClosestRouteObjectFromCurrentLocation());
            updateUi();

        } else if (item.getItemId() == R.id.menuItemAutoSkipToNextRoutePoint) {
            settingsManagerInstance.setAutoSkipToNextRoutePoint(
                    ! settingsManagerInstance.getAutoSkipToNextRoutePoint());

        } else if (item.getItemId() == R.id.menuItemShowIntersectionLayoutDetails) {
            settingsManagerInstance.setShowIntersectionLayoutDetails(
                    ! settingsManagerInstance.getShowIntersectionLayoutDetails());
            if (route != null) {
                updateUi();
            }

        } else if (item.getItemId() == R.id.menuItemShowPreciseBearingValues) {
            settingsManagerInstance.setShowPreciseBearingValues(
                    ! settingsManagerInstance.getShowPreciseBearingValues());
            if (route != null) {
                updateUi();
            }

        } else if (item.getItemId() == R.id.menuItemShowBearingIndicator) {
            settingsManagerInstance.setShowBearingIndicator(
                    ! settingsManagerInstance.getShowBearingIndicator());
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
        return inflater.inflate(R.layout.fragment_navigate, container, false);
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        // content layout

        layoutRoute = (ObjectWithIdView) view.findViewById(R.id.layoutRoute);
        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        layoutCurrentRouteObject = (RouteObjectView) view.findViewById(R.id.layoutCurrentRouteObject);

        layoutIntersectionStructure = (LinearLayout) view.findViewById(R.id.layoutIntersectionStructure);
        labelIntersectionStructure = (TextView) view.findViewById(R.id.labelIntersectionStructure);
        intersectionScheme = (IntersectionScheme) view.findViewById(R.id.intersectionScheme);

        // bottom layout
            labelDistanceAndBearing = (DistanceAndBearingView) view.findViewById(R.id.labelDistanceAndBearing);

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

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_ROUTE, this.route);
    }

    public void loadNewRouteFromSettings() {
        route = SettingsManager.getInstance().getLastSelectedRoute();
        requestUiUpdate();
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(routeBroadcastReceiver);
    }

    @Override public void onResume() {
        super.onResume();

        IntentFilter bearingUpdateFilter = new IntentFilter();
        bearingUpdateFilter.addAction(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_SATELLITE);
        LocalBroadcastManager
            .getInstance(getActivity())
            .registerReceiver(routeBroadcastReceiver, bearingUpdateFilter);

        requestUiUpdate();
    }

    private void requestUiUpdate() {
        layoutRoute.setVisibility(View.GONE);
        layoutCurrentRouteObject.setVisibility(View.GONE);
        layoutIntersectionStructure.setVisibility(View.GONE);
        labelDistanceAndBearing.setVisibility(View.GONE);
        buttonPreviousRouteObject.setVisibility(View.GONE);
        buttonNextRouteObject.setVisibility(View.GONE);

        if (route != null) {
            layoutRoute.setVisibility(
                    showObjectWithIdView ? View.VISIBLE : View.GONE);
            layoutCurrentRouteObject.setVisibility(View.VISIBLE);
            labelDistanceAndBearing.setVisibility(View.VISIBLE);
            buttonPreviousRouteObject.setVisibility(View.VISIBLE);
            buttonNextRouteObject.setVisibility(View.VISIBLE);
            updateUi();

            // request current location for labelDistanceAndBearing field
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
            String nextInstruction;
            if (route.getCurrentRouteObject().getTurn() != null) {
                nextInstruction = String.format(
                        GlobalInstance.getStringResource(R.string.messageNextSegmentInstructionAndTurn),
                        route.getCurrentRouteObject().formatSegmentInstruction(),
                        route.getCurrentRouteObject().getTurn().getInstruction());
            } else if (! TextUtils.isEmpty(route.getCurrentRouteObject().formatSegmentInstruction())) {
                nextInstruction = route.getCurrentRouteObject().formatSegmentInstruction();
            } else {
                nextInstruction = GlobalInstance.getStringResource(R.string.messageFirstSegment);
            }
            ttsWrapperInstance.announce(nextInstruction, MessageType.TOP_PRIORITY);
        }
        updateUi();
    }

    private void updateUi() {
        RouteObject currentRouteObject = route.getCurrentRouteObject();
        int progress = (int) Math.round(
                ((route.getElapsedLength() * 1.0) / route.getTotalLength()) * 100.0);

        labelHeading.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelRoutePosition),
                    route.getCurrentPosition() + 1,
                    route.getRouteObjectList().size(),
                    route.getElapsedLength(),
                    GlobalInstance.getPluralResource(
                        R.plurals.meters,
                        route.getTotalLength()),
                    progress)
                );
        labelHeading.setContentDescription(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelRoutePositionCD),
                    route.getCurrentPosition() + 1,
                    route.getRouteObjectList().size(),
                    route.getElapsedLength(),
                    GlobalInstance.getPluralResource(
                        R.plurals.meter, route.getTotalLength()),
                    progress)
                );

        layoutRoute.configureAsSingleObject(route);
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
                SortByBearingRelativeTo comparator = new Segment.SortByBearingRelativeTo(
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

        labelDistanceAndBearing.setObjectWithId(currentRouteObject.getPoint());
    }


    private class RouteBroadcastReceiver extends BroadcastReceiver {
        private static final int SHORTLY_BEFORE_ARRIVAL_THRESHOLD_IN_METERS = 30;

        private RouteObject lastRouteObject = null;
        private boolean shortlyBeforeArrivalAnnounced, arrivalAnnounced;
        private long arrivalTime;

        @Override public void onReceive(Context context, Intent intent) {
            if (route == null) { return; }
            RouteObject currentRouteObject = route.getCurrentRouteObject();

            if (! currentRouteObject.equals(lastRouteObject)) {
                // skipped to next route object
                this.lastRouteObject = currentRouteObject;
                this.shortlyBeforeArrivalAnnounced = false;
                this.arrivalAnnounced = false;
                this.arrivalTime = System.currentTimeMillis();
            }

            if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_SATELLITE)) {
                BearingSensorValue bearingValueFromSatellite = (BearingSensorValue) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (bearingValueFromSatellite == null) return;

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
                    route.formatShortlyBeforeArrivalAtPointMessage(),
                    MessageType.INSTRUCTION);
        }

        private void announceArrival(RouteObject currentRouteObject) {
            shortlyBeforeArrivalAnnounced = true;
            arrivalAnnounced = true;
            ttsWrapperInstance.announce(
                    route.formatArrivalAtPointMessage(), MessageType.INSTRUCTION);
            Helper.vibrateOnce(Helper.VIBRATION_DURATION_LONG);

            // auto jump to next route point
            if (route.hasNextRouteObject()
                    && settingsManagerInstance.getAutoSkipToNextRoutePoint()) {
                route.skipToNextRouteObject();
                updateUi();
            }
        }
    }

}
