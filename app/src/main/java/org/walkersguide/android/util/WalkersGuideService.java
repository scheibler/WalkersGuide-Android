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


public class WalkersGuideService extends Service implements LocationUpdate {


    /**
     * control service
     */

    // start scan request
    public static final String ACTION_START_SCAN = String.format(
            "%1$s.action.startScan", BuildConfig.APPLICATION_ID);

    public static boolean startScan(boolean tryToResolvePossibleStartScanFailure) {
        StartScanFailure failure = null;
        if (       ! isLocationProviderAvailable()) {
            failure = StartScanFailure.NO_LOCATION_PROVIDER;
        } else if (! isForegroundLocationPermissionGranted()) {
            failure = StartScanFailure.FOREGROUND_LOCATION_PERMISSION_DENIED;
        } else if (! isPostNotificationsPermissionGranted()) {
            failure = StartScanFailure.POST_NOTIFICATIONS_PERMISSION_DENIED;
        }
        if (failure != null) {
            if (tryToResolvePossibleStartScanFailure) {
                sendStartScanResponseBroadcast(failure);
            }
            return false;
        }

        // inform user about disabled location module
        if (! isLocationModuleEnabled()) {
            sendStartScanResponseBroadcast(
                    StartScanFailure.LOCATION_MODULE_DISABLED);
        }
        // start service
        Intent intent = createServiceRequestIntent(ACTION_START_SCAN);
        ContextCompat.startForegroundService(GlobalInstance.getContext(), intent);
        return true;
    }


    // stop scan request
    public static final String ACTION_STOP_SCAN = String.format(
            "%1$s.action.stopScan", BuildConfig.APPLICATION_ID);

    public static void stopScan() {
        Intent intent = createServiceRequestIntent(ACTION_STOP_SCAN);
        GlobalInstance.getContext().startService(intent);
    }


    // helper
    private static Intent createServiceRequestIntent(String action) {
        return new Intent(
                action, null, GlobalInstance.getContext(), WalkersGuideService.class);
    }


    /**
     * response broadcasts
     */

    // service running state changed broadcast
    public static final String ACTION_SERVICE_RUNNING_STATE_CHANGED = String.format(
            "%1$s.action.serviceRunningStateChanged", BuildConfig.APPLICATION_ID);
    public static final String EXTRA_IS_RUNNING = "isRunning";

    public static void sendServiceRunningStateChangedBroadcast(boolean isRunning) {
        Intent intent = new Intent(ACTION_SERVICE_RUNNING_STATE_CHANGED);
        intent.putExtra(EXTRA_IS_RUNNING, isRunning);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }



    // start scan response broadcast
    public static final String ACTION_START_SCAN_RESPONSE = String.format(
            "%1$s.action.startScanResponse", BuildConfig.APPLICATION_ID);
    public static final String EXTRA_START_SCAN_FAILURE = "startScanFailure";

    public enum StartScanFailure {
        NO_LOCATION_PROVIDER, FOREGROUND_LOCATION_PERMISSION_DENIED, POST_NOTIFICATIONS_PERMISSION_DENIED, LOCATION_MODULE_DISABLED
    }

    public static void sendStartScanResponseBroadcast(StartScanFailure failure) {
        Intent intent = new Intent(ACTION_START_SCAN_RESPONSE);
        intent.putExtra(EXTRA_START_SCAN_FAILURE, failure);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }


    /**
     * constructor
     */

    private NotificationManager notificationManagerInstance = null;

    private DeviceSensorManager deviceSensorManagerInstance;
    private PositionManager positionManagerInstance;
	private SettingsManager settingsManagerInstance;

    private enum State {
        OFF, PAUSED, FOREGROUND
    }
    private State state;

    @Override public void onCreate() {
        super.onCreate();
        this.notificationManagerInstance = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        //
        deviceSensorManagerInstance = DeviceSensorManager.getInstance();
        positionManagerInstance = PositionManager.getInstance();
		settingsManagerInstance = SettingsManager.getInstance();
        // off state
        this.state = State.OFF;
        // create notification channel
        this.createNotificationChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null
                && intent.getAction() != null) {
            Timber.d("onStartCommand: state= %1$s, action= %2$s", state, intent.getAction());

            if (intent.getAction().equals(ACTION_START_SCAN)) {
                if (! isPostNotificationsPermissionGranted()) {
                    destroyService();
                    return START_STICKY;
                }

                switch (state) {
                    case OFF:
                        // listen for sensor broadcasts
                        IntentFilter sensorIntentFilter = new IntentFilter();
                        sensorIntentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
                        registerReceiver(sensorIntentReceiver, sensorIntentFilter);
                        // state change: off -> paused
                        state = State.PAUSED;
                        // break statement is missing intentionally

                    case PAUSED:
                        // enable sensors
                        if (       isLocationProviderAvailable()
                                && isForegroundLocationPermissionGranted()
                                && isPostNotificationsPermissionGranted()
                                && isLocationModuleEnabled()) {
                            positionManagerInstance.startGPS();
                            positionManagerInstance.setLocationUpdateListener(this);
                            deviceSensorManagerInstance.startSensors();
                            // state change: off -> foreground
                            state = State.FOREGROUND;
                            sendServiceRunningStateChangedBroadcast(true);
                        }
                        // create or update foreground notification
                        startForeground(
                                WALKERS_GUIDE_SERVICE_NOTIFICATION_ID,
                                getWalkersGuideServiceNotification());
                        Timber.d("foreground");
                        break;
                }

            } else if (intent.getAction().equals(ACTION_STOP_SCAN)) {
                destroyService();
            }
        }

        return START_STICKY;
    }

    @Override public void onDestroy() {
        super.onDestroy();
        destroyService();
    }

    private void switchFromRunningIntoPauseState() {
        // deactivate sensors
        positionManagerInstance.stopGPS();
        deviceSensorManagerInstance.stopSensors();
        state = State.PAUSED;
        // update service notification
        notificationManagerInstance.notify(
                WALKERS_GUIDE_SERVICE_NOTIFICATION_ID,
                getWalkersGuideServiceNotification());
        sendServiceRunningStateChangedBroadcast(false);
    }

    private void destroyService() {
        switch (state) {
            case FOREGROUND:
                switchFromRunningIntoPauseState();
                // break statement is missing intentionally
            case PAUSED:
                unregisterReceiver(sensorIntentReceiver);
                break;
        }

        // state change: * -> off
        state = State.OFF;
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
        switch (state) {
            case FOREGROUND:
                return true;
            default:
                return false;
        }
    }

    public String getStatusMessage() {
        String message;
        switch (state) {
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
    }

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

        return new NotificationCompat.Builder(this, WALKERS_GUIDE_SERVICE_NOTIFICATION_CHANNEL_ID)
            // visibility
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // icon, text and intent
            .setSmallIcon(
                    R.drawable.ic_launcher_invers)
            .setContentTitle(getStatusMessage())
            .setContentIntent(
                    PendingIntent.getActivity(
                        this, 0, launchWalkersGuideActivityIntent, getPendingIntentFlags()))
            .addAction(
                    R.drawable.clear,
                    "blub",
                    PendingIntent.getService(
                        this, 0, createServiceRequestIntent(ACTION_STOP_SCAN),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
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
                    startScan(false);
                } else {
                    switchFromRunningIntoPauseState();
                }
            }
        }
    };


    /**
     * location updates
     */

    @Override public void newLocation(Point point, boolean isImportant) {
        Timber.d("newLocation: point=%1$s, isImportant=%2$s", point, isImportant);
    }

    @Override public void newGPSLocation(GPS gps) {
    }

    @Override public void newSimulatedLocation(Point point) {
    }

}
