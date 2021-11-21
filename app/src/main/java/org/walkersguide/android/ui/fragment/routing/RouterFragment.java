package org.walkersguide.android.ui.fragment.routing;

import org.walkersguide.android.ui.view.TextViewAndActionButton;
    import org.walkersguide.android.ui.view.TextViewAndActionButton.LabelTextConfig;
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



import org.walkersguide.android.data.route.RouteObject;
import org.walkersguide.android.data.sensor.attribute.NewDirectionAttributes;
import org.walkersguide.android.data.sensor.attribute.NewLocationAttributes;
import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.data.sensor.threshold.BearingThreshold;
import org.walkersguide.android.data.sensor.threshold.DistanceThreshold;
import org.walkersguide.android.util.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.PlanRouteSettings;
import org.walkersguide.android.util.TTSWrapper;
import org.walkersguide.android.data.sensor.threshold.SpeedThreshold;
import androidx.fragment.app.Fragment;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.ui.activity.toolbar.FragmentContainerActivity;


public class RouterFragment extends Fragment {

	// Store instance variables
    private Route route;
    private RouteBroadcastReceiver routeBroadcastReceiver;
	private SettingsManager settingsManagerInstance;
    private TTSWrapper ttsWrapperInstance;

	// ui components
    private TextViewAndActionButton layoutStartPoint, layoutDestinationPoint;
    private Button buttonRouteCommandList;
    private TextViewAndActionButton layoutCurrentSegment, layoutCurrentPoint;
    private TextView labelHeading, labelDistanceAndBearing;
    private Button buttonPreviousRouteObject, buttonNextRouteObject;

	// newInstance constructor for creating fragment with arguments
	public static RouterFragment newInstance() {
		RouterFragment fragment = new RouterFragment();
		return fragment;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
        routeBroadcastReceiver = new RouteBroadcastReceiver();
		settingsManagerInstance = SettingsManager.getInstance();
        ttsWrapperInstance = TTSWrapper.getInstance();
	}


    /**
     * menu
     */

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_router_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // check or uncheck filter result menu item
        MenuItem menuItemAutoSkipToNextRoutePoint = menu.findItem(R.id.menuItemAutoSkipToNextRoutePoint);
        if (menuItemAutoSkipToNextRoutePoint != null) {
            menuItemAutoSkipToNextRoutePoint.setChecked(
                    settingsManagerInstance.getAutoSkipToNextRoutePoint());
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menuItemRecalculate:
            case R.id.menuItemRecalculateWithCurrentPosition:
            case R.id.menuItemRecalculateReturnRoute:
                PlanRouteSettings planRouteSettings = settingsManagerInstance.getPlanRouteSettings();
                if (route == null) {
                    Toast.makeText(
                            getActivity(),
                            GlobalInstance.getStringResource(R.string.errorNoRouteSelected),
                            Toast.LENGTH_LONG).show();
                    return true;
                }

                // start point
                if (item.getItemId() == R.id.menuItemRecalculateWithCurrentPosition) {
                    Point currentLocation = PositionManager.getInstance().getCurrentLocation();
                    if (currentLocation != null) {
                        planRouteSettings.setStartPoint(currentLocation);
                    } else {
                        Toast.makeText(
                                getActivity(),
                                GlobalInstance.getStringResource(R.string.errorNoLocationFound),
                                Toast.LENGTH_LONG).show();
                        return true;
                    }
                } else if (item.getItemId() == R.id.menuItemRecalculateReturnRoute) {
                    planRouteSettings.setStartPoint(route.getDestinationPoint());
                } else {
                    planRouteSettings.setStartPoint(route.getStartPoint());
                }

                // destination point
                if (item.getItemId() == R.id.menuItemRecalculateReturnRoute) {
                    planRouteSettings.setDestinationPoint(route.getStartPoint());
                } else {
                    planRouteSettings.setDestinationPoint(route.getDestinationPoint());
                }

                // set via points and show plan route dialog
                planRouteSettings.setViaPoint1(route.getViaPoint1());
                planRouteSettings.setViaPoint2(route.getViaPoint2());
                planRouteSettings.setViaPoint3(route.getViaPoint3());
                PlanRouteDialog.newInstance()
                    .show(getActivity().getSupportFragmentManager(), "PlanRouteDialog");
                return true;

            case R.id.menuItemSkipToFirstRoutePoint:
                if (route != null) {
                    route.jumpToRouteObjectAt(0);
                    updateUiExceptDistanceLabel();
                }
                return true;

            case R.id.menuItemAutoSkipToNextRoutePoint:
                settingsManagerInstance.setAutoSkipToNextRoutePoint(
                        ! settingsManagerInstance.getAutoSkipToNextRoutePoint());
                return true;

            default:
                return false;
        }
    }


    /**
     * create view
     */

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
		return inflater.inflate(R.layout.fragment_router, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        // top layout
        layoutStartPoint = (TextViewAndActionButton) view.findViewById(R.id.layoutStartPoint);
        layoutDestinationPoint = (TextViewAndActionButton) view.findViewById(R.id.layoutDestinationPoint);

        buttonRouteCommandList = (Button) view.findViewById(R.id.buttonRouteCommandList);
        buttonRouteCommandList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                FragmentContainerActivity.showRouteDetails(
                        RouterFragment.this.getContext(), route);
            }
        });

        // content layout
        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        layoutCurrentSegment = (TextViewAndActionButton) view.findViewById(R.id.layoutCurrentSegment);
        layoutCurrentPoint = (TextViewAndActionButton) view.findViewById(R.id.layoutCurrentPoint);

        // bottom layout
        labelDistanceAndBearing = (TextView) view.findViewById(R.id.labelDistanceAndBearing);

        buttonPreviousRouteObject = (Button) view.findViewById(R.id.buttonPreviousRouteObject);
        buttonPreviousRouteObject.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (route.hasPreviousRouteObject()) {
                    route.skipToPreviousRouteObject();
                            updateUiExceptDistanceLabel();
                }
            }
        });

        buttonNextRouteObject = (Button) view.findViewById(R.id.buttonNextRouteObject);
        buttonNextRouteObject.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (route.hasNextRouteObject()) {
                    route.skipToNextRouteObject();
                            updateUiExceptDistanceLabel();
                }
            }
        });
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        if (route != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(routeBroadcastReceiver);
        }
    }

    @Override public void onResume() {
        super.onResume();
        Timber.d("onResume");
        layoutStartPoint.setVisibility(View.GONE);
        layoutDestinationPoint.setVisibility(View.GONE);
        buttonRouteCommandList.setVisibility(View.GONE);
        layoutCurrentSegment.setVisibility(View.GONE);
        layoutCurrentPoint.setVisibility(View.GONE);
        labelDistanceAndBearing.setVisibility(View.GONE);
        buttonPreviousRouteObject.setVisibility(View.GONE);
        buttonNextRouteObject.setVisibility(View.GONE);

        // load route from settings
        route = settingsManagerInstance.getSelectedRoute();
        if (route != null) {
            layoutStartPoint.setVisibility(View.VISIBLE);
            layoutDestinationPoint.setVisibility(View.VISIBLE);
            buttonRouteCommandList.setVisibility(View.VISIBLE);
            layoutCurrentSegment.setVisibility(View.VISIBLE);
            layoutCurrentPoint.setVisibility(View.VISIBLE);
            labelDistanceAndBearing.setVisibility(View.VISIBLE);
            buttonPreviousRouteObject.setVisibility(View.VISIBLE);
            buttonNextRouteObject.setVisibility(View.VISIBLE);
            updateUiExceptDistanceLabel();

            // broadcast filter
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_NEW_LOCATION);
            filter.addAction(Constants.ACTION_NEW_DIRECTION);
            filter.addAction(Constants.ACTION_NEW_GPS_DIRECTION);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(routeBroadcastReceiver, filter);

            // request current location for labelDistanceAndBearing field
            PositionManager.getInstance().requestCurrentLocation();

        } else {
            labelHeading.setText(
                    GlobalInstance.getStringResource(R.string.errorNoRouteSelected));
        }
    }


    private class RouteBroadcastReceiver extends BroadcastReceiver {
        private static final int SHORTLY_BEFORE_ARRIVAL_THRESHOLD_IN_METERS = 30;

        private RouteObject lastRouteObject;
        private Vibrator vibrator;

        private boolean shortlyBeforeArrivalAnnounced, arrivalAnnounced;
        private long arrivalTime;

        public RouteBroadcastReceiver() {
            this.lastRouteObject = null;
            this.vibrator = (Vibrator) GlobalInstance.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            initialize();
        }

        @Override public void onReceive(Context context, Intent intent) {
            RouteObject currentRouteObject = route.getCurrentRouteObject();
            if (! currentRouteObject.equals(lastRouteObject)) {
                // skipped to next route object
                lastRouteObject = currentRouteObject;
                initialize();
            }

            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)) {
                NewLocationAttributes newLocationAttributes = NewLocationAttributes.fromString(
                        intent.getStringExtra(Constants.ACTION_NEW_LOCATION_ATTRIBUTES));
                if (newLocationAttributes != null
                        && newLocationAttributes.getAggregatingDistanceThreshold().isAtLeast(DistanceThreshold.ZERO_METERS)) {
                    updateDistanceAndBearingLabel(currentRouteObject);
                    // announce new position
                    if (newLocationAttributes.getAggregatingDistanceThreshold().isAtLeast(DistanceThreshold.TWENTY_METERS)
                            && newLocationAttributes.getSpeedThreshold().isAtMost(SpeedThreshold.FIVE_KMH)) {
                        ttsWrapperInstance.announceToScreenReader(
                                String.format(
                                    GlobalInstance.getStringResource(R.string.labelPointDistanceAndBearing),
                                    GlobalInstance.getPluralResource(R.plurals.meter, currentRouteObject.getPoint().distanceFromCurrentLocation()),
                                    StringUtility.formatRelativeViewingDirection(
                                            currentRouteObject.getPoint().bearingFromCurrentLocation()))
                                );
                    }
                }

            } else if (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)) {
                NewDirectionAttributes newDirectionAttributes = NewDirectionAttributes.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_DIRECTION_ATTRIBUTES));
                if (newDirectionAttributes != null
                        && newDirectionAttributes.getAggregatingBearingThreshold().isAtLeast(BearingThreshold.TEN_DEGREES)) {
                    updateDistanceAndBearingLabel(currentRouteObject);
                }

            } else if (intent.getAction().equals(Constants.ACTION_NEW_GPS_DIRECTION)) {
                Direction gpsDirection = Direction.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_GPS_DIRECTION_OBJECT));
                if (gpsDirection != null) {
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
                            && nextRouteObjectWithinRange(currentRouteObject, gpsDirection)
                            && ! walkingReverseDetected(currentRouteObject, gpsDirection)
                            && (System.currentTimeMillis() - arrivalTime) > 5000) {
                        announceArrival(currentRouteObject);
                    }
                }
            }
        }

        private void initialize() {
            this.shortlyBeforeArrivalAnnounced = false;
            this.arrivalAnnounced = false;
            this.arrivalTime = System.currentTimeMillis();
        }

        private boolean nextRouteObjectWithinRange(RouteObject currentRouteObject, Direction gpsDirection) {
            Point point = currentRouteObject.getPoint();
            if (point.distanceFromCurrentLocation() < 25
                    && point.bearingFromCurrentLocation(gpsDirection) > 80
                    && point.bearingFromCurrentLocation(gpsDirection) < 280) {
                return true;
            } else if (point.distanceFromCurrentLocation() < 20
                    && point.bearingFromCurrentLocation(gpsDirection) > 65
                    && point.bearingFromCurrentLocation(gpsDirection) < 295) {
                return true;
            } else if (point.distanceFromCurrentLocation() < 15
                    && point.bearingFromCurrentLocation(gpsDirection) > 50
                    && point.bearingFromCurrentLocation(gpsDirection) < 310) {
                return true;
            } else if (point.distanceFromCurrentLocation() < 10
                    && point.bearingFromCurrentLocation(gpsDirection) > 35
                    && point.bearingFromCurrentLocation(gpsDirection) < 325) {
                return true;
            } else if (point.distanceFromCurrentLocation() < 5) {
                return true;
            }
            return false;
        }

        private boolean walkingReverseDetected(RouteObject currentRouteObject, Direction gpsDirection) {
            if (! currentRouteObject.getIsFirstRouteObject()) {
                // bearing of next route segment
                int bearingOfNextRouteSegment = currentRouteObject.getSegment().getBearing()
                    + currentRouteObject.getTurn();
                if (bearingOfNextRouteSegment >= 360) {
                    bearingOfNextRouteSegment -= 360;
                }
                // reverse
                int reversedBearingOfNextRouteSegment = bearingOfNextRouteSegment - 180;
                if (reversedBearingOfNextRouteSegment < 0) {
                    reversedBearingOfNextRouteSegment += 360;
                }
                // take the current viewing direction into account
                int relativeDirection = reversedBearingOfNextRouteSegment - gpsDirection.getBearing();
                if (relativeDirection < 0) {
                    relativeDirection += 360;
                }
                if (relativeDirection < 23 || relativeDirection > 338) {
                    return true;
                }
            }
            return false;
        }

        private void announceArrival(RouteObject currentRouteObject) {
            shortlyBeforeArrivalAnnounced = true;
            arrivalAnnounced = true;
            vibrator.vibrate(250);
            // speak
            String routePointFoundMessage = null;
            if (currentRouteObject.getIsFirstRouteObject()) {
                routePointFoundMessage = GlobalInstance.getStringResource(R.string.messageArrivedAtRouteStart);
            } else if (currentRouteObject.getIsLastRouteObject()) {
                routePointFoundMessage = GlobalInstance.getStringResource(R.string.messageArrivedAtRouteDestination);
            } else {
                routePointFoundMessage = String.format(
                        GlobalInstance.getStringResource(R.string.messageArrivedAtRoutePoint),
                        route.getCurrentPosition()+1,
                        StringUtility.formatInstructionDirection(currentRouteObject.getTurn()));
            }
            ttsWrapperInstance.announceToEveryone(routePointFoundMessage);
            // auto jump to next route point
            if (route.hasNextRouteObject()
                    && settingsManagerInstance.getAutoSkipToNextRoutePoint()) {
                route.skipToNextRouteObject();
                updateUiExceptDistanceLabel();
            }
        }

        private void announceShortlyBeforeArrival(RouteObject currentRouteObject) {
            shortlyBeforeArrivalAnnounced = true;
            if (currentRouteObject.getTurn() != null) {
                ttsWrapperInstance.announceToEveryone(
                        String.format(
                            GlobalInstance.getStringResource(R.string.messageAlmostArrivedAtRoutePoint),
                            StringUtility.formatInstructionDirection(currentRouteObject.getTurn()))
                        );
            }
        }

        private void updateDistanceAndBearingLabel(RouteObject currentRouteObject) {
            labelDistanceAndBearing.setText(
                    String.format(
                        "%1$s, %2$s",
                        GlobalInstance.getPluralResource(R.plurals.meter, currentRouteObject.getPoint().distanceFromCurrentLocation()),
                        StringUtility.formatRelativeViewingDirection(
                            currentRouteObject.getPoint().bearingFromCurrentLocation()))
                    );
        }
    }

    public void updateUiExceptDistanceLabel() {
        // top layout
        layoutStartPoint.configureView(route.getStartPoint(), LabelTextConfig.start(true));
        layoutDestinationPoint.configureView(route.getDestinationPoint(), LabelTextConfig.destination(true));

        // main layout
        labelHeading.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelRoutePosition),
                    route.getCurrentPosition() + 1,
                    route.getRouteObjectList().size())
                );
        layoutCurrentSegment.configureView(route.getCurrentRouteObject().getSegment(), LabelTextConfig.empty(true));
        layoutCurrentPoint.configureView(route.getCurrentRouteObject().getPoint(), LabelTextConfig.empty(true));

        if (route.getCurrentRouteObject().getIsFirstRouteObject()) {
            layoutCurrentSegment.setLabelText(
                    GlobalInstance.getStringResource(R.string.proceedToFirstRoutePoint));
        }
    }

}
