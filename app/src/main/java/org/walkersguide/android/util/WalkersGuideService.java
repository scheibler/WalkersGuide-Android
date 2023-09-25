package org.walkersguide.android.util;

import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.R;

import java.lang.Class;

import java.text.Normalizer;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.location.LocationManager;

import android.media.AudioAttributes;

import android.net.Uri;

import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.Vibrator;

import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import timber.log.Timber;

import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.sensor.PositionManager.LocationUpdate;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.point.GPS;
import android.widget.Toast;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.database.profile.StaticProfile;
import org.walkersguide.android.data.angle.RelativeBearing;
import org.walkersguide.android.data.object_with_id.route.RouteObject;
import org.walkersguide.android.data.angle.Turn;
import org.walkersguide.android.data.angle.Bearing;


public class WalkersGuideService extends Service implements LocationUpdate {


    /**
     * start and stop service
     */

    // start service request
    private static final String ACTION_START_SERVICE = String.format(
            "%1$s.action.startService", BuildConfig.APPLICATION_ID);

    public static boolean startService() {
        StartServiceFailure failure = null;
        if (! isForegroundLocationPermissionGranted()) {
            failure = StartServiceFailure.FOREGROUND_LOCATION_PERMISSION_DENIED;
        } else if (! isPostNotificationsPermissionGranted()) {
            failure = StartServiceFailure.POST_NOTIFICATIONS_PERMISSION_DENIED;
        }

        if (failure != null) {
            Intent intent = new Intent(ACTION_START_SERVICE_FAILED);
            intent.putExtra(EXTRA_START_SERVICE_FAILURE, failure);
            LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
            return false;
        } else {
            // start service
            Intent intent = createServiceRequestIntent(ACTION_START_SERVICE);
            ContextCompat.startForegroundService(GlobalInstance.getContext(), intent);
            return true;
        }
    }

    // start service response broadcast
    public static final String ACTION_START_SERVICE_FAILED = String.format(
            "%1$s.action.startServiceFailed", BuildConfig.APPLICATION_ID);
    public static final String EXTRA_START_SERVICE_FAILURE = "startServiceFailure";

    public enum StartServiceFailure {
        NO_LOCATION_PROVIDER, FOREGROUND_LOCATION_PERMISSION_DENIED, POST_NOTIFICATIONS_PERMISSION_DENIED
    }


    // request service state
    private static final String ACTION_REQUEST_SERVICE_STATE = String.format(
            "%1$s.action.routeRecordingStatus", BuildConfig.APPLICATION_ID);

    public static void requestServiceState() {
        GlobalInstance.getContext().startService(
                createServiceRequestIntent(ACTION_REQUEST_SERVICE_STATE));
    }


    // stop service request
    private static final String ACTION_STOP_SERVICE = String.format(
            "%1$s.action.stopService", BuildConfig.APPLICATION_ID);

    public static void stopService() {
        Intent intent = createServiceRequestIntent(ACTION_STOP_SERVICE);
        GlobalInstance.getContext().startService(intent);
    }


    // helper
    private static Intent createServiceRequestIntent(String action) {
        return new Intent(
                action, null, GlobalInstance.getContext(), WalkersGuideService.class);
    }


    /**
     * service broadcasts
     */

    // service state changed broadcast
    public static final String ACTION_SERVICE_STATE_CHANGED = String.format(
            "%1$s.action.serviceStateChanged", BuildConfig.APPLICATION_ID);
    public static final String EXTRA_SERVICE_STATE = "serviceState";
    public static final String EXTRA_SERVICE_MESSAGE = "message";

    private void sendServiceStateChangedBroadcast() {
        Intent intent = new Intent(ACTION_SERVICE_STATE_CHANGED);
        intent.putExtra(EXTRA_SERVICE_STATE, serviceState);

        String message = "";
        if (       ! isLocationProviderAvailable()) {
            message = getResources().getString(R.string.messageNoLocationProviderAvailableShort);
        } else if (! isForegroundLocationPermissionGranted()) {
            message = getResources().getString(R.string.messageLocationPermissionDeniedShort);
        } else if (! isPostNotificationsPermissionGranted()) {
            message = getResources().getString(R.string.messagePostNotificationsPermissionDeniedShort);
        } else if (! isLocationModuleEnabled()) {
            message = getResources().getString(R.string.messageLocationProviderDisabledShort);
        }
        intent.putExtra(EXTRA_SERVICE_MESSAGE, message);

        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }


    /**
     * constructor
     */

    private NotificationManager notificationManagerInstance = null;

    private DeviceSensorManager deviceSensorManagerInstance;
    private PositionManager positionManagerInstance;
	private SettingsManager settingsManagerInstance;

    public enum ServiceState {
        OFF, STOPPED, FOREGROUND
    }
    private ServiceState serviceState;

    @Override public void onCreate() {
        super.onCreate();
        this.notificationManagerInstance = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        //
        deviceSensorManagerInstance = DeviceSensorManager.getInstance();
        positionManagerInstance = PositionManager.getInstance();
		settingsManagerInstance = SettingsManager.getInstance();
        // off state
        this.serviceState = ServiceState.OFF;
        // create notification channel
        this.createNotificationChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }
        Timber.d("onStartCommand: serviceState= %1$s, action= %2$s", serviceState, intent.getAction());

        if (intent.getAction().equals(ACTION_START_SERVICE)) {

            // check permissions on every service start again
            if (       ! isLocationProviderAvailable()
                    || ! isForegroundLocationPermissionGranted()
                    || ! isPostNotificationsPermissionGranted()) {
                // missing permissions, must stay offline
                // destroy service for good measure to prevent problems with the "10 sec foreground service missing notification" problem
                destroyService();
                return START_STICKY;
            }

            boolean serviceStateChanged = false;
            switch (serviceState) {

                case OFF:
                    // listen for sensor broadcasts
                    IntentFilter sensorIntentFilter = new IntentFilter();
                    sensorIntentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
                    registerReceiver(sensorIntentReceiver, sensorIntentFilter);
                    // state change: off -> stopped
                    serviceState = ServiceState.STOPPED;
                    serviceStateChanged = true;
                    // break statement is missing intentionally

                case STOPPED:
                    // enable sensors
                    if (isLocationModuleEnabled()) {
                        positionManagerInstance.startGPS();
                        positionManagerInstance.setLocationUpdateListener(this);
                        deviceSensorManagerInstance.startSensors();
                        // state change: stopped -> foreground
                        serviceState = ServiceState.FOREGROUND;
                        serviceStateChanged = true;
                    }
                    // create or update foreground notification
                    startForeground(
                            WALKERS_GUIDE_SERVICE_NOTIFICATION_ID,
                            getWalkersGuideServiceNotification());
                    // send service state changed broadcast only if necessary
                    if (serviceStateChanged) {
                        sendServiceStateChangedBroadcast();
                    }
                    break;
            }
            return START_STICKY;

        } else if (intent.getAction().equals(ACTION_REQUEST_SERVICE_STATE)) {
            sendServiceStateChangedBroadcast();
            return START_STICKY;
        }

        // all other requests demand, that the service is at least in the state STOPPED
        if (serviceState == ServiceState.OFF) {
            return START_STICKY;
        }

        if (intent.getAction().equals(ACTION_STOP_SERVICE)) {
            switch (serviceState) {
                case FOREGROUND:
                    switchFromForegroundIntoStoppedState();
                    // break statement is missing intentionally
                case STOPPED:
                    if (routeRecordingState == RouteRecordingState.OFF) {
                        destroyService();
                    }
                    break;
            }

        // route recording

        } else if (intent.getAction().equals(ACTION_START_ROUTE_RECORDING)) {
            switch (routeRecordingState) {
                case OFF:
                    recordedPointList.clear();
                case PAUSED:
                    routeRecordingState = RouteRecordingState.RUNNING;
                    sendRouteRecordingChangedBroadcast();
                    break;
            }

        } else if (intent.getAction().equals(ACTION_PAUSE_OR_RESUME_ROUTE_RECORDING)) {
            switch (routeRecordingState) {
                case PAUSED:
                    routeRecordingState = RouteRecordingState.RUNNING;
                    sendRouteRecordingChangedBroadcast();
                    break;
                case RUNNING:
                    routeRecordingState = RouteRecordingState.PAUSED;
                    sendRouteRecordingChangedBroadcast();
                    break;
            }

        } else if (intent.getAction().equals(ACTION_FINISH_ROUTE_RECORDING)) {
            switch (routeRecordingState) {
                case RUNNING:
                case PAUSED:
                    /*
                    //Route testRoute = Route.load(9223372035729079104l);
                    Route testRoute = Route.load(9223372032695025401l);
                    for (RouteObject obj : testRoute.getRouteObjectList()) {
                        GPS gps = (GPS) obj.getPoint();
                        gps.rename(null);
                        recordedPointList.add(gps);
                    }*/

                    String routeName = intent.getStringExtra(EXTRA_ROUTE_NAME);
                    if (TextUtils.isEmpty(routeName)) {
                        sendRouteRecordingFailedBroadcast(
                                getResources().getString(R.string.messageRecordedRouteNameIsMissing));
                        break;
                    }
                    if (recordedPointList.size() < 2) {
                        sendRouteRecordingFailedBroadcast(
                                getResources().getString(R.string.messageRecordedRouteTooFewPoints));
                        break;
                    }

                    Route route = createRouteFromRecordedPointList(routeName);
                    if (route != null
                            && StaticProfile.recordedRoutes().add(route)) {
                        routeRecordingState = RouteRecordingState.OFF;
                        recordedPointList.clear();
                        sendRouteRecordingChangedBroadcast();
                        Toast.makeText(
                                WalkersGuideService.this,
                                getResources().getString(R.string.messageSaveRecordedRouteSuccessful),
                                Toast.LENGTH_LONG)
                            .show();
                    } else {
                        sendRouteRecordingFailedBroadcast(
                                getResources().getString(R.string.messageSaveRecordedRouteFailed));
                    }
                    break;
            }

        } else if (intent.getAction().equals(ACTION_CANCEL_ROUTE_RECORDING)) {
            switch (routeRecordingState) {
                case RUNNING:
                case PAUSED:
                    routeRecordingState = RouteRecordingState.OFF;
                    recordedPointList.clear();
                    sendRouteRecordingChangedBroadcast();
                    break;
            }

        } else if (intent.getAction().equals(ACTION_REQUEST_ROUTE_RECORDING_STATE)) {
            sendRouteRecordingChangedBroadcast();

        } else if (intent.getAction().equals(ACTION_ADD_POINT_TO_RECORDED_ROUTE)) {
            switch (routeRecordingState) {
                case RUNNING:
                case PAUSED:
                    GPS pointToAdd = (GPS) intent.getSerializableExtra(EXTRA_POINT_TO_ADD);
                    if (pointToAdd != null) {
                        recordedPointList.add(pointToAdd);
                        sendRouteRecordingChangedBroadcast();
                    }
                    break;
            }
        }

        return START_STICKY;
    }

    @Override public void onDestroy() {
        super.onDestroy();
        destroyService();
    }

    private void switchFromForegroundIntoStoppedState() {
        // deactivate sensors
        positionManagerInstance.stopGPS();
        deviceSensorManagerInstance.stopSensors();
        serviceState = ServiceState.STOPPED;
        sendServiceStateChangedBroadcast();
        // update service notification
        notificationManagerInstance.notify(
                WALKERS_GUIDE_SERVICE_NOTIFICATION_ID,
                getWalkersGuideServiceNotification());
    }

    private void destroyService() {
        switch (serviceState) {
            case FOREGROUND:
                switchFromForegroundIntoStoppedState();
                // break statement is missing intentionally
            case STOPPED:
                routeRecordingState = RouteRecordingState.OFF;
                recordedPointList.clear();
                unregisterReceiver(sensorIntentReceiver);
                break;
        }

        // state change: * -> off
        serviceState = ServiceState.OFF;
        sendServiceStateChangedBroadcast();
        notificationManagerInstance.cancel(WALKERS_GUIDE_SERVICE_NOTIFICATION_ID);
        stopForeground(true);
        stopSelf();
    }


    /**
     * start service requirements
     */

    public static boolean isLocationProviderAvailable() {
        LocationManager locationManager = (LocationManager) GlobalInstance.getContext().getSystemService(Context.LOCATION_SERVICE);
        return locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
            || locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
            || locationManager.getAllProviders().contains(LocationManager.FUSED_PROVIDER);
    }

    public static boolean isForegroundLocationPermissionGranted() {
        Context context = GlobalInstance.getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    public static boolean isPostNotificationsPermissionGranted() {
        Context context = GlobalInstance.getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    public static boolean isLocationModuleEnabled() {
        Context context = GlobalInstance.getContext();
        if (! ((LocationManager) context.getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return false;
        }
        return true;
    }


    /**
     * binder
     */
    private final IBinder mBinder = new LocalBinder();

    @Override public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public boolean isRunning() {
        switch (serviceState) {
            case FOREGROUND:
                return true;
            default:
                return false;
        }
    }

    /*
    public String getStatusMessage() {
        String message;
        switch (serviceState) {
            case FOREGROUND:
                message = getResources().getString(R.string.walkersGuideServiceStateForeground);
                break;
            case PAUSED:
                String reason = null;
                if (! isLocationProviderAvailable()) {
                    reason = getResources().getString(R.string.messageNoLocationProviderAvailableShort);
                } else if (! isForegroundLocationPermissionGranted()) {
                    reason = getResources().getString(R.string.messageLocationPermissionDeniedShort);
                } else if (! isPostNotificationsPermissionGranted()) {
                    reason = getResources().getString(R.string.messagePostNotificationsPermissionDeniedShort);
                } else if (! isLocationModuleEnabled()) {
                    reason = getResources().getString(R.string.messageLocationProviderDisabledShort);
                }
                //
                message = reason != null
                    ? String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.walkersGuideServiceStatePaused),
                            reason)
                    : getResources().getString(R.string.walkersGuideServiceStatePaused);
                break;
            default:
                message = getResources().getString(R.string.walkersGuideServiceStateOff);
        }
        return String.format(
                "%1$s %2$s",
                getResources().getString(R.string.walkersGuideServiceNotificationChannelName),
                message);
    }*/

    public class LocalBinder extends Binder {
        public WalkersGuideService getService() {
            return WalkersGuideService.this;
        }
    }


    /**
     * notification
     */

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel walkersGuideServiceNotificationChannel = new NotificationChannel(
                    WALKERS_GUIDE_SERVICE_NOTIFICATION_CHANNEL_ID,
                    getResources().getString(R.string.walkersGuideServiceNotificationChannelName),
                    NotificationManager.IMPORTANCE_LOW);
            walkersGuideServiceNotificationChannel.setShowBadge(false);
            walkersGuideServiceNotificationChannel.setDescription(
                    getResources().getString(R.string.walkersGuideServiceNotificationChannelDescription));
            notificationManagerInstance.createNotificationChannel(walkersGuideServiceNotificationChannel);
        }
    }

    // WalkersGuide service
    private static final String WALKERS_GUIDE_SERVICE_NOTIFICATION_CHANNEL_ID = String.format(
            "%1$s.channel.walkers_guide_service", BuildConfig.APPLICATION_ID);
    private static final int WALKERS_GUIDE_SERVICE_NOTIFICATION_ID = 8203;

    private Notification getWalkersGuideServiceNotification() {
        Intent launchWalkersGuideActivityIntent = new Intent(
                WalkersGuideService.this, MainActivity.class);
        launchWalkersGuideActivityIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        String message = "";
        switch (serviceState) {
            case FOREGROUND:
                message = getResources().getString(R.string.walkersGuideServiceStateForeground);
                break;
            case STOPPED:
                if (! isLocationModuleEnabled()) {
                    message = getResources().getString(R.string.messageLocationProviderDisabledShort);
                } else if (routeRecordingState != RouteRecordingState.OFF) {
                    message = getResources().getString(R.string.messageRouteRecordingPaused);
                } else {
                    // fallback message, should not be shown
                    message = getResources().getString(R.string.walkersGuideServiceStateStopped);
                }
                break;
            // case OFF doesn't show a notification
        }

        return new NotificationCompat.Builder(this, WALKERS_GUIDE_SERVICE_NOTIFICATION_CHANNEL_ID)
            // visibility
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // icon, text and intent
            .setSmallIcon(R.drawable.ic_launcher_invers)
            .setContentTitle(message)
            .setContentIntent(
                    PendingIntent.getActivity(
                        this, 0, launchWalkersGuideActivityIntent, getPendingIntentFlags()))
            .setOngoing(true)
            .setShowWhen(false)
            .setUsesChronometer(false)
            .setAutoCancel(false)
            .build();
    }

    @SuppressLint("Deprecation, UnspecifiedImmutableFlag")
    private static int getPendingIntentFlags() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT;
    }


    /**
     * sensor broadcasts
     */

    private BroadcastReceiver sensorIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Timber.d("Sensor action: %1$s", intent.getAction());
            if (intent.getAction().equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                if (isLocationModuleEnabled()) {
                    // only go into foreground state, if app is in foreground itself
                    if (! GlobalInstance.getInstance().applicationWasInBackground()) {
                        GlobalInstance.getContext().startService(
                                createServiceRequestIntent(ACTION_START_SERVICE));
                    }
                } else {
                    switchFromForegroundIntoStoppedState();
                }
            }
        }
    };


    /**
     * location updates
     */

    @Override public void newLocation(Point point, boolean isImportant) {
        //Timber.d("newLocation: point=%1$s, isImportant=%2$s", point, isImportant);
    }

    @Override public void newGPSLocation(GPS gps) {
        switch (routeRecordingState) {
            case RUNNING:
                if (shouldPointBeAddedToTheRecordedRoute(gps)) {
                    recordedPointList.add(gps);
                    sendRouteRecordingChangedBroadcast();
                }
                break;
        }
    }

    @Override public void newSimulatedLocation(Point point) {
    }


    /**
     * route recording
     */
    private static final int RECORDED_POINTS_MIN_DISTANCE_IN_METERS = 5;

    private ArrayList<GPS> recordedPointList = new ArrayList<GPS>();
    private RouteRecordingState routeRecordingState = RouteRecordingState.OFF;

    public enum RouteRecordingState {
        OFF, PAUSED, RUNNING
    }

    // control route recording requests
    private static final String ACTION_START_ROUTE_RECORDING = String.format(
            "%1$s.action.startRouteRecording", BuildConfig.APPLICATION_ID);

    public static void startRouteRecording() {
        GlobalInstance.getContext().startService(
                createServiceRequestIntent(ACTION_START_ROUTE_RECORDING));
    }

    private static final String ACTION_PAUSE_OR_RESUME_ROUTE_RECORDING = String.format(
            "%1$s.action.pauseOrResumeRouteRecording", BuildConfig.APPLICATION_ID);

    public static void pauseOrResumeRouteRecording() {
        GlobalInstance.getContext().startService(
                createServiceRequestIntent(ACTION_PAUSE_OR_RESUME_ROUTE_RECORDING));
    }

    private static final String ACTION_FINISH_ROUTE_RECORDING = String.format(
            "%1$s.action.finishRouteRecording", BuildConfig.APPLICATION_ID);
    private static final String EXTRA_ROUTE_NAME = "routeName";

    public static void finishRouteRecording(String routeName) {
        Intent intent = createServiceRequestIntent(ACTION_FINISH_ROUTE_RECORDING);
        intent.putExtra(EXTRA_ROUTE_NAME, routeName);
        GlobalInstance.getContext().startService(intent);
    }

    private static final String ACTION_CANCEL_ROUTE_RECORDING = String.format(
            "%1$s.action.cancelRouteRecording", BuildConfig.APPLICATION_ID);

    public static void cancelRouteRecording() {
        GlobalInstance.getContext().startService(
                createServiceRequestIntent(ACTION_CANCEL_ROUTE_RECORDING));
    }

    private static final String ACTION_REQUEST_ROUTE_RECORDING_STATE = String.format(
            "%1$s.action.requestRouteRecordingState", BuildConfig.APPLICATION_ID);

    public static void requestRouteRecordingState() {
        GlobalInstance.getContext().startService(
                createServiceRequestIntent(ACTION_REQUEST_ROUTE_RECORDING_STATE));
    }

    private static final String ACTION_ADD_POINT_TO_RECORDED_ROUTE = String.format(
            "%1$s.action.addPointToRecordedRoute", BuildConfig.APPLICATION_ID);
    private static final String EXTRA_POINT_TO_ADD = "pointToAdd";

    public static void addPointToRecordedRoute(GPS pointToAdd) {
        Intent intent = createServiceRequestIntent(ACTION_ADD_POINT_TO_RECORDED_ROUTE);
        intent.putExtra(EXTRA_POINT_TO_ADD, pointToAdd);
        GlobalInstance.getContext().startService(intent);
    }

    // route recording changed broadcast
    public static final String ACTION_ROUTE_RECORDING_CHANGED = String.format(
            "%1$s.action.routeRecordingChanged", BuildConfig.APPLICATION_ID);
    public static final String EXTRA_RECORDING_STATE = "recordingState";
    public static final String EXTRA_NUMBER_OF_POINTS = "numberOfPoints";
    public static final String EXTRA_NUMBER_OF_POINTS_BY_USER = "numberOfPointsByUser";
    public static final String EXTRA_DISTANCE = "distance";

    private void sendRouteRecordingChangedBroadcast() {
        Intent intent = new Intent(ACTION_ROUTE_RECORDING_CHANGED);
        intent.putExtra(EXTRA_RECORDING_STATE, routeRecordingState);
        intent.putExtra(EXTRA_NUMBER_OF_POINTS, getNumberOfRecordedPoints());
        intent.putExtra(EXTRA_NUMBER_OF_POINTS_BY_USER, getNumberOfRecordedPointsByUser());
        intent.putExtra(EXTRA_DISTANCE, getDistanceBetweenRecordedPoints());
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    // route recording failed broadcast
    public static final String ACTION_ROUTE_RECORDING_FAILED = String.format(
            "%1$s.action.routeRecordingFailed", BuildConfig.APPLICATION_ID);
    public static final String EXTRA_ROUTE_RECORDING_FAILED_MESSAGE = "routeRecordingFailedMessage";

    private void sendRouteRecordingFailedBroadcast(String message) {
        Intent intent = new Intent(ACTION_ROUTE_RECORDING_FAILED);
        intent.putExtra(EXTRA_ROUTE_RECORDING_FAILED_MESSAGE, message);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }


    public ArrayList<GPS> getRecordedPointList() {
        return this.recordedPointList;
    }

    public int getNumberOfRecordedPoints() {
        return this.recordedPointList.size();
    }


    public int getNumberOfRecordedPointsByUser() {
        int numberOfAddedPointsByUser = 0;
        for (GPS gps : recordedPointList) {
            if (gps.wasRenamed()) {
                numberOfAddedPointsByUser++;
            }
        }
        return numberOfAddedPointsByUser;
    }

    public int getDistanceBetweenRecordedPoints() {
        int distance = 0;
        for (int i=0; i<this.recordedPointList.size()-1; i++) {
            distance += this.recordedPointList.get(i)
                .distanceTo(this.recordedPointList.get(i+1));
        }
        return distance;
    }

    private boolean shouldPointBeAddedToTheRecordedRoute(GPS pointToAdd) {
        // trivial cases
        if (       pointToAdd == null
                || pointToAdd.getBearing() == null
                || pointToAdd.getAccuracy() == null
                || pointToAdd.getAccuracy() > 10.0) {
            return false;
        } else if (recordedPointList.isEmpty()) {
            return true;
        }

        GPS previousPoint = recordedPointList.get(recordedPointList.size()-1);
        if (previousPoint.distanceTo(pointToAdd) < RECORDED_POINTS_MIN_DISTANCE_IN_METERS) {
            return false;
        }

        return previousPoint
            .bearingTo(pointToAdd)
            .relativeTo(pointToAdd.getBearing())
            .getDirection() != RelativeBearing.Direction.STRAIGHT_AHEAD;
    }

    private Route createRouteFromRecordedPointList(String routeName) {
        ArrayList<GPS> filteredRecordedPointList = null;

        // add destination point, if not automatically done
        GPS destination = positionManagerInstance.getGPSLocation();
        if (destination != null
                && destination.distanceTo(recordedPointList.get(recordedPointList.size()-1)) > RECORDED_POINTS_MIN_DISTANCE_IN_METERS
                && destination.distanceTo(recordedPointList.get(recordedPointList.size()-1)) < 200) {
            recordedPointList.add(destination);
        }

        // filter redundant points by turn value
        filteredRecordedPointList = new ArrayList<GPS>();
        // first point must always be added
        filteredRecordedPointList.add(recordedPointList.get(0));
        for (int i=1; i<recordedPointList.size()-1; i++) {
            GPS previous = filteredRecordedPointList.get(filteredRecordedPointList.size()-1);
            GPS current = recordedPointList.get(i);
            GPS next = recordedPointList.get(i+1);
            // get bearings between segments
            Bearing bearingFromPreviousToCurrent = previous.bearingTo(current);
            Bearing bearingFromCurrentToNext = current.bearingTo(next);
            // calculate turn between these two segments
            Turn turn = bearingFromPreviousToCurrent.turnTo(bearingFromCurrentToNext);
            Timber.d("%1$d: %2$s - %3$s = %4$s", filteredRecordedPointList.size(), bearingFromPreviousToCurrent, bearingFromCurrentToNext, turn);
            // skip the current point, if object wasn't added manually or turn != cross
            if (current.wasRenamed()      // hack to determine, if the point was added by the user
                    || turn.getInstruction() != Turn.Instruction.CROSS) {
                filteredRecordedPointList.add(current);
            }
        }
        // and the last point must always be added too
        filteredRecordedPointList.add(recordedPointList.get(recordedPointList.size()-1));
        recordedPointList = filteredRecordedPointList;

        // filter redundant points by small distance
        filteredRecordedPointList = new ArrayList<GPS>();
        // first point must always be added
        filteredRecordedPointList.add(recordedPointList.get(0));
        for (int i=1; i<recordedPointList.size(); i++) {
            GPS previous = filteredRecordedPointList.get(filteredRecordedPointList.size()-1);
            GPS current = recordedPointList.get(i);
            // skip, if the current point wasn't added manually and it is closer than 4m away from the previous one
            if (current.wasRenamed()      // hack to determine, if the point was added by the user
                    || previous.distanceTo(current) > 3) {
                filteredRecordedPointList.add(current);
            }
        }
        recordedPointList = filteredRecordedPointList;

        // rename recorded points
        int pointNumber = 1;
        for (GPS point : recordedPointList) {
            if (! point.wasRenamed()) {         // don't rename points, who were added by the user
                point.rename(
                        String.format(
                            getResources().getString(R.string.labelRecordedPointName),
                            pointNumber)
                        );
            }
            pointNumber++;
        }

        // create route
        Route route = null;
        try {
            route = Route.fromPointList(recordedPointList, false);
            route.rename(routeName);
        } catch (JSONException e) {}
        return route;
    }

}
