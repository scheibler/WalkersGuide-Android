package org.walkersguide.android.ui.activity;

import org.walkersguide.android.shortcut.StaticShortcutAction;
import org.walkersguide.android.ui.dialog.create.SaveCurrentLocationDialog;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.bearing.BearingSensorValue;
import org.walkersguide.android.sensor.bearing.BearingSensor;
import org.walkersguide.android.sensor.bearing.BearingSensorAccuracyRating;
import android.net.Uri;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.ui.dialog.toolbar.BearingDetailsDialog;
import org.walkersguide.android.ui.dialog.toolbar.LocationDetailsDialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.Manifest;

import android.os.Bundle;

import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;


import android.view.Menu;
import android.view.View;

import android.widget.ImageButton;
import android.widget.TextView;



import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import androidx.appcompat.app.AppCompatDelegate;
import org.walkersguide.android.data.object_with_id.Point;
import androidx.appcompat.widget.Toolbar;
import timber.log.Timber;
import android.app.Dialog;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import org.walkersguide.android.ui.dialog.edit.RenameObjectDialog;
import android.widget.Toast;
import android.view.MenuItem;
import androidx.core.app.NavUtils;
import androidx.core.app.TaskStackBuilder;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.ui.dialog.WhereAmIDialog;


public abstract class ToolbarActivity extends AppCompatActivity implements FragmentResultListener {

    public abstract int getLayoutResourceId();

    private GlobalInstance globalInstance;
    private DeviceSensorManager deviceSensorManagerInstance;
    private PositionManager positionManagerInstance;
	private SettingsManager settingsManagerInstance;

    private boolean skipPositionManagerInitialisationDuringOnResume;

    private Toolbar toolbar;
    private TextView labelToolbarTitle;
    private ImageButton buttonBearingDetails, buttonLocationDetails;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());
        skipPositionManagerInitialisationDuringOnResume = false;

        globalInstance = (GlobalInstance) getApplicationContext();
        deviceSensorManagerInstance = DeviceSensorManager.getInstance();
        positionManagerInstance = PositionManager.getInstance();
		settingsManagerInstance = SettingsManager.getInstance();

        getSupportFragmentManager()
            .setFragmentResultListener(
                    RenameObjectDialog.REQUEST_RENAME_OBJECT_SUCCESSFUL, this, this);
        getSupportFragmentManager()
            .setFragmentResultListener(
                    SaveCurrentLocationDialog.REQUEST__LOCATION_SAVED_SUCCESSFULLY, this, this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        labelToolbarTitle = (TextView) findViewById(R.id.labelToolbarTitle);

        buttonBearingDetails = (ImageButton) findViewById(R.id.buttonBearingDetails);
        buttonBearingDetails.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                BearingDetailsDialog.newInstance()
                    .show(getSupportFragmentManager(), "BearingDetailsDialog");
            }
        });

        buttonLocationDetails = (ImageButton) findViewById(R.id.buttonLocationDetails);
        buttonLocationDetails.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                LocationDetailsDialog.newInstance()
                    .show(getSupportFragmentManager(), "LocationDetailsDialog");
            }
        });
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(RenameObjectDialog.REQUEST_RENAME_OBJECT_SUCCESSFUL)
                || requestKey.equals(SaveCurrentLocationDialog.REQUEST__LOCATION_SAVED_SUCCESSFULLY)) {
            ToolbarActivity.this.recreate();
        }
    }

    public Toolbar getToolbar() {
        return this.toolbar;
    }

    public void setToolbarTitle(String title) {
        labelToolbarTitle.setText(title);
    }

    private void updateBearingDetailsButton() {
        Bearing currentBearing = deviceSensorManagerInstance.getCurrentBearing();
        StringBuilder bearingDescriptionBuilder = new StringBuilder();

        bearingDescriptionBuilder.append(
                String.format(
                    "%1$s: ",
                    deviceSensorManagerInstance.getSimulationEnabled()
                    ? getResources().getString(R.string.toolbarSensorPrefixSimulation)
                    : deviceSensorManagerInstance.getSelectedBearingSensor()));

        if (currentBearing == null) {
            bearingDescriptionBuilder.append(
                    getResources().getString(R.string.errorNoBearingFound));

        } else {
            bearingDescriptionBuilder.append(currentBearing.toString());

            if (currentBearing instanceof BearingSensorValue) {
                BearingSensorValue currentSensorBearing = (BearingSensorValue) currentBearing;
                if (currentSensorBearing.isOutdated()) {
                    bearingDescriptionBuilder.append(
                            String.format(", %1$s", getResources().getString(R.string.toolbarSensorDataOutdated)));
                }
                if (currentSensorBearing.getAccuracyRating() == BearingSensorAccuracyRating.LOW
                        && deviceSensorManagerInstance.getSelectedBearingSensor() == BearingSensor.COMPASS) {
                    bearingDescriptionBuilder.append(
                            String.format(", %1$s", getResources().getString(R.string.toolbarSensorCalibrationRequired)));
                }
            }
        }

        buttonBearingDetails.setContentDescription(bearingDescriptionBuilder.toString());
    }

    private void updateLocationDetailsButton() {
        Point currentLocation = positionManagerInstance.getCurrentLocation();
        StringBuilder locationDescriptionBuilder = new StringBuilder();

        locationDescriptionBuilder.append(
                String.format(
                    "%1$s: ",
                    positionManagerInstance.getSimulationEnabled()
                    ? getResources().getString(R.string.toolbarSensorPrefixSimulation)
                    : getResources().getString(R.string.toolbarSensorPrefixPosition)));

        if (currentLocation == null) {
            locationDescriptionBuilder.append(
                    getResources().getString(R.string.errorNoLocationFound));

        } else if (currentLocation instanceof GPS) {
            GPS currentGPSLocation = (GPS) currentLocation;
            locationDescriptionBuilder.append(
                    currentGPSLocation.formatAccuracyInMeters());
            if (currentGPSLocation.isOutdated()) {
                locationDescriptionBuilder.append(
                        String.format(", %1$s", GlobalInstance.getStringResource(R.string.toolbarSensorDataOutdated)));
            }

        } else {
            locationDescriptionBuilder.append(
                    currentLocation.getName());
        }

        buttonLocationDetails.setContentDescription(locationDescriptionBuilder.toString());
    }


    /**
     * menu
     */

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent upIntent = NavUtils.getParentActivityIntent(this);
            if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(upIntent)
                    .startActivities();
            } else {
                upIntent.setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavUtils.navigateUpTo(this, upIntent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * monitor navigation drawer open / close status
     */
    private boolean toolbarMenuIsOpen = false;

    @Override public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null
                && featureId == AppCompatDelegate.FEATURE_SUPPORT_ACTION_BAR) {
            toolbarMenuIsOpen = true;
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override public void onPanelClosed(int featureId, Menu menu) {
        toolbarMenuIsOpen = false;
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        globalInstance.startActivityTransitionTimer();
    }

    @Override public void onResume() {
        super.onResume();
        globalInstance.stopActivityTransitionTimer();
        updateBearingDetailsButton();
        updateLocationDetailsButton();

        // listen for some actions
        IntentFilter filter = new IntentFilter();
        filter.addAction(PositionManager.ACTION_LOCATION_PROVIDER_DISABLED);
        filter.addAction(PositionManager.ACTION_FOREGROUND_LOCATION_PERMISSION_DENIED);
        filter.addAction(PositionManager.ACTION_NEW_LOCATION);
        filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);

        if (globalInstance.applicationWasInBackground()) {
            globalInstance.setApplicationInBackground(false);

            // activate sensors
            if (! skipPositionManagerInitialisationDuringOnResume) {
                positionManagerInstance.startGPS();
            }
            deviceSensorManagerInstance.startSensors();

            for (StaticShortcutAction action : globalInstance.getEnabledStaticShortcutActions()) {
                if (action == StaticShortcutAction.OPEN_PLAN_ROUTE_DIALOG) {
                    PlanRouteDialog.newInstance()
                        .show(getSupportFragmentManager(), "PlanRouteDialog");
                } else if (action == StaticShortcutAction.OPEN_SAVE_CURRENT_LOCATION_DIALOG) {
                    SaveCurrentLocationDialog.newInstance()
                        .show(getSupportFragmentManager(), "SaveCurrentLocationDialog");
                } else if (action == StaticShortcutAction.OPEN_WHERE_AM_I_DIALOG) {
                    WhereAmIDialog.newInstance()
                        .show(getSupportFragmentManager(), "WhereAmIDialog");
                }
            }

            // cleanup after app resume
            globalInstance.clearCaches();
            globalInstance.resetEnabledStaticShortcutActions();
        }
    }


    /**
     * broadcast receiver
     */
    private static final int PERMISSION_REQUEST_ENABLE_GPS = 1;
    private static final int PERMISSION_REQUEST_APP_SETTINGS = 2;
    private static final int PERMISSION_REQUEST_FOREGROUND_LOCATION = 3;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {

            if(intent.getAction().equals(PositionManager.ACTION_LOCATION_PROVIDER_DISABLED)) {
                Dialog enableLocationDialog = new AlertDialog.Builder(ToolbarActivity.this)
                    .setTitle(
                            context.getResources().getString(R.string.enableLocationDialogTitle))
                    .setMessage(
                            context.getResources().getString(R.string.enableLocationDialogMessage))
                    .setCancelable(false)
                    .setPositiveButton(
                            context.getResources().getString(R.string.dialogOK),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    startActivityForResult(intent, PERMISSION_REQUEST_ENABLE_GPS);
                                }
                            })
                    .setNegativeButton(
                            context.getResources().getString(R.string.dialogCancel),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    showMessage(
                                            context.getResources().getString(R.string.messageLocationProviderDisabled));
                                }
                            })
                    .create();
                enableLocationDialog.show();

            } else if(intent.getAction().equals(PositionManager.ACTION_FOREGROUND_LOCATION_PERMISSION_DENIED)) {
                if (! ActivityCompat.shouldShowRequestPermissionRationale(ToolbarActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // ask for location permission for the first time
                    ActivityCompat.requestPermissions(
                            ToolbarActivity.this,
                            new String[] {
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION },
                            PERMISSION_REQUEST_FOREGROUND_LOCATION);
                } else {
                    // open app info page on later permission requests
                    Dialog appInfoDialog = new AlertDialog.Builder(ToolbarActivity.this)
                        .setTitle(
                                context.getResources().getString(R.string.appInfoDialogTitle))
                        .setMessage(
                                context.getResources().getString(R.string.appInfoDialogMessage))
                        .setCancelable(false)
                        .setPositiveButton(
                                context.getResources().getString(R.string.dialogOK),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent appInfoIntent = new Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        appInfoIntent.setData(
                                                Uri.fromParts("package", getPackageName(), null));
                                        startActivityForResult(
                                                appInfoIntent, PERMISSION_REQUEST_APP_SETTINGS);
                                    }
                                })
                        .setNegativeButton(
                                context.getResources().getString(R.string.dialogCancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        showMessage(
                                                context.getResources().getString(R.string.messageLocationPermissionDenied));
                                    }
                                })
                        .create();
                    appInfoDialog.show();
                }

            } else if (! toolbarMenuIsOpen) {
                if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                    updateBearingDetailsButton();
                } else if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                    updateLocationDetailsButton();
                }
            }
        }
    };

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.d("onActivityResult: requestCode = %1$d", requestCode);
        switch (requestCode) {

            case PERMISSION_REQUEST_ENABLE_GPS:
                if (! PositionManager.locationServiceEnabled()) {
                    skipPositionManagerInitialisationDuringOnResume = true;
                    showMessage(
                            getResources().getString(R.string.messageLocationProviderDisabled));
                } else {
                    positionManagerInstance.startGPS();
                }
                break;

            case PERMISSION_REQUEST_APP_SETTINGS:
                if (! PositionManager.foregroundLocationPermissionGranted()) {
                    skipPositionManagerInitialisationDuringOnResume = true;
                    showMessage(
                            getResources().getString(R.string.messageLocationPermissionDenied));
                } else {
                    if (! PositionManager.backgroundLocationPermissionGranted()) {
                        // warn the user about "only foreground location permission granted"
                    }
                    positionManagerInstance.startGPS();
                }
                break;

            default:
                break;
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        for (int i=0; i<permissions.length; i++) {
            if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)
                    && requestCode == PERMISSION_REQUEST_FOREGROUND_LOCATION) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    skipPositionManagerInitialisationDuringOnResume = true;
                    showMessage(
                            getResources().getString(R.string.messageLocationPermissionDenied));
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // request background location permission
                    }
                    positionManagerInstance.startGPS();
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showMessage(String message) {
        SimpleMessageDialog.newInstance(message)
            .show(getSupportFragmentManager(), "SimpleMessageDialog");
    }

}
