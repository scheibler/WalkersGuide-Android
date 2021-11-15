package org.walkersguide.android.ui.activity;

import org.walkersguide.android.ui.dialog.toolbar.DirectionDetailsDialog;
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



import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import androidx.appcompat.app.AppCompatDelegate;
import org.walkersguide.android.data.basic.point.Point;
import androidx.appcompat.widget.Toolbar;


public abstract class AbstractToolbarActivity extends AppCompatActivity {
    private static final int ASK_FOR_LOCATION_PERMISSION_ID = 61;

    public abstract int getLayoutResourceId();


    public GlobalInstance globalInstance;
    public DirectionManager directionManagerInstance;
    public PositionManager positionManagerInstance;
	public SettingsManager settingsManagerInstance;

    protected Toolbar toolbar;
    private TextView labelToolbarTitle;
    private ImageButton buttonDirectionDetails, buttonLocationDetails;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        globalInstance = (GlobalInstance) getApplicationContext();
        directionManagerInstance = DirectionManager.getInstance(this);
        positionManagerInstance = PositionManager.getInstance(this);
		settingsManagerInstance = SettingsManager.getInstance();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        labelToolbarTitle = (TextView) findViewById(R.id.labelToolbarTitle);

        buttonDirectionDetails = (ImageButton) findViewById(R.id.buttonDirectionDetails);
        buttonDirectionDetails.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                DirectionDetailsDialog.newInstance()
                    .show(getSupportFragmentManager(), "DirectionDetailsDialog");
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

    public void setToolbarTitle(String title) {
        labelToolbarTitle.setText(title);
    }

    public void updateDirectionDetailsButton() {
        Direction currentDirection = directionManagerInstance.getCurrentDirection();
        StringBuilder directionDescriptionBuilder = new StringBuilder();

        // get direction source name
        boolean outdated = false;
        boolean compassCalibrationRequired = false;
        if (directionManagerInstance.getSimulationEnabled()) {
            directionDescriptionBuilder.append(
                    String.format("%1$s: ", getResources().getString(R.string.directionSourceSimulated)));
        } else {
            switch (directionManagerInstance.getDirectionSource()) {
                case Constants.DIRECTION_SOURCE.COMPASS:
                    directionDescriptionBuilder.append(
                            String.format("%1$s: ", getResources().getString(R.string.directionSourceCompass)));
                    // check for badly calibrated compass
                    if (currentDirection != null
                            && currentDirection.getAccuracyRating() == Constants.DIRECTION_ACCURACY_RATING.LOW) {
                        compassCalibrationRequired = true;
                            }
                    break;
                case Constants.DIRECTION_SOURCE.GPS:
                    directionDescriptionBuilder.append(
                            String.format("%1$s: ", getResources().getString(R.string.directionSourceGPS)));
                    break;
                default:
                    break;
            }
            if (currentDirection != null
                    && currentDirection.isOutdated()) {
                outdated = true;
                    }
        }

        // bearing
        if (currentDirection != null) {
            directionDescriptionBuilder.append(currentDirection.toString());
        } else {
            directionDescriptionBuilder.append(
                    getResources().getString(R.string.errorNoDirectionFound));
        }

        // outdated
        if (outdated) {
            directionDescriptionBuilder.append(
                    String.format(", %1$s", getResources().getString(R.string.toolbarSensorDataOutdated)));
        }

        // calibration required
        if (compassCalibrationRequired) {
            directionDescriptionBuilder.append(
                    String.format(", %1$s", getResources().getString(R.string.toolbarCompassCalibrationRequired)));
        }

        buttonDirectionDetails.setContentDescription(directionDescriptionBuilder.toString());
    }

    public void updateLocationDetailsButton() {
        Point currentLocation = positionManagerInstance.getCurrentLocation();
        StringBuilder locationDescriptionBuilder = new StringBuilder();

        // get location source name
        if (positionManagerInstance.getSimulationEnabled()) {
            locationDescriptionBuilder.append(
                    String.format("%1$s: ", getResources().getString(R.string.locationSourceSimulatedPoint)));
            if (currentLocation == null) {
                locationDescriptionBuilder.append(
                        getResources().getString(R.string.errorNoLocationFound));
            } else {
                locationDescriptionBuilder.append(
                        currentLocation.getName());
            }

        } else {
            locationDescriptionBuilder.append(
                    String.format("%1$s: ", getResources().getString(R.string.locationSourceGPS)));
            if (currentLocation == null) {
                locationDescriptionBuilder.append(
                        getResources().getString(R.string.errorNoLocationFound));
            } else if (currentLocation instanceof GPS) {
                locationDescriptionBuilder.append(
                        ((GPS) currentLocation).getShortStatusMessage());
            } else {
                locationDescriptionBuilder.append(
                        currentLocation.getName());
            }
        }

        buttonLocationDetails.setContentDescription(locationDescriptionBuilder.toString());
    }

    // monitor navigation drawer open / close status
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
        updateDirectionDetailsButton();
        updateLocationDetailsButton();

        // listen for some actions
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_LOCATION_PROVIDER_DISABLED);
        filter.addAction(Constants.ACTION_LOCATION_PERMISSION_DENIED);
        filter.addAction(Constants.ACTION_NEW_DIRECTION);
        filter.addAction(Constants.ACTION_NEW_LOCATION);
        filter.addAction(Constants.ACTION_UPDATE_UI);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);

        if (globalInstance.applicationWasInBackground()) {
            globalInstance.setApplicationInBackground(false);
            // activate sensors
            positionManagerInstance.startGPS();
            directionManagerInstance.startSensors();
            // reset server status request
            ServerStatusManager.getInstance(this).setCachedServerInstance(null);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ASK_FOR_LOCATION_PERMISSION_ID:
                break;
            default:
                break;
        }
    }


    /**
     * broadcast receiver
     */

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            // process intent action
            if(intent.getAction().equals(Constants.ACTION_LOCATION_PROVIDER_DISABLED)) {
                // launch system settings activity to activate gps
                Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(settingsIntent);
            } else if(intent.getAction().equals(Constants.ACTION_LOCATION_PERMISSION_DENIED)) {
                // ask for location permission
                ActivityCompat.requestPermissions(
                        AbstractToolbarActivity.this,
                        new String[] {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION },
                        ASK_FOR_LOCATION_PERMISSION_ID);
            } else if (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)) {
                if (! toolbarMenuIsOpen) {
                    updateDirectionDetailsButton();
                }
            } else if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)) {
                if (! toolbarMenuIsOpen) {
                    updateLocationDetailsButton();
                }
            } else if(intent.getAction().equals(Constants.ACTION_UPDATE_UI)) {
                AbstractToolbarActivity.this.recreate();
            }
        }
    };

}
