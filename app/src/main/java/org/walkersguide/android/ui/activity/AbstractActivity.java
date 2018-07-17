package org.walkersguide.android.ui.activity;

import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.ServerStatus;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.ui.dialog.RequestAddressDialog;
import org.walkersguide.android.ui.dialog.SaveCurrentPositionDialog;
import org.walkersguide.android.ui.dialog.SelectDirectionSourceDialog;
import org.walkersguide.android.ui.dialog.SelectLocationSourceDialog;
import org.walkersguide.android.ui.dialog.SelectMapDialog;
import org.walkersguide.android.ui.dialog.SelectPublicTransportProviderDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public abstract class AbstractActivity extends AppCompatActivity {

    private static final int ASK_FOR_LOCATION_PERMISSION_ID = 61;

    public GlobalInstance globalInstance;
	public AccessDatabase accessDatabaseInstance;
    public DirectionManager directionManagerInstance;
    public PositionManager positionManagerInstance;
	public SettingsManager settingsManagerInstance;

    private SimpleMessageDialog simpleMessageDialog;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        globalInstance = (GlobalInstance) getApplicationContext();
		accessDatabaseInstance = AccessDatabase.getInstance(this);
        directionManagerInstance = DirectionManager.getInstance(this);
        positionManagerInstance = PositionManager.getInstance(this);
		settingsManagerInstance = SettingsManager.getInstance(this);
    }


    /**
     * toolbar
     */

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity, menu);
        return true;
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // update direction menu item
        int currentDirection = directionManagerInstance.getCurrentDirection();
        switch (directionManagerInstance.getDirectionSource()) {
            case Constants.DIRECTION_SOURCE.COMPASS:
                if (currentDirection == Constants.DUMMY.DIRECTION) {
                    menu.findItem(R.id.menuItemDirection).setTitle(
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.directionSourceCompass),
                                getResources().getString(R.string.messageError1005))
                            );
                } else {
                    menu.findItem(R.id.menuItemDirection).setTitle(
                            String.format(
                                getResources().getString(R.string.formattedDirectionValue),
                                getResources().getString(R.string.directionSourceCompass),
                                currentDirection,
                                StringUtility.formatGeographicDirection(this, currentDirection))
                            );
                }
                break;
            case Constants.DIRECTION_SOURCE.GPS:
                if (currentDirection == Constants.DUMMY.DIRECTION) {
                    menu.findItem(R.id.menuItemDirection).setTitle(
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.directionSourceGPS),
                                getResources().getString(R.string.messageError1005))
                            );
                } else {
                    menu.findItem(R.id.menuItemDirection).setTitle(
                            String.format(
                                getResources().getString(R.string.formattedDirectionValue),
                                getResources().getString(R.string.directionSourceGPS),
                                currentDirection,
                                StringUtility.formatGeographicDirection(this, currentDirection))
                            );
                }
                break;
            case Constants.DIRECTION_SOURCE.SIMULATION:
                if (currentDirection == Constants.DUMMY.DIRECTION) {
                    menu.findItem(R.id.menuItemDirection).setTitle(
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.directionSourceSimulated),
                                getResources().getString(R.string.messageError1005))
                            );
                } else {
                    menu.findItem(R.id.menuItemDirection).setTitle(
                            String.format(
                                getResources().getString(R.string.formattedDirectionValue),
                                getResources().getString(R.string.directionSourceSimulated),
                                currentDirection,
                                StringUtility.formatGeographicDirection(this, currentDirection))
                            );
                }
                break;
            default:
                menu.findItem(R.id.menuItemDirection).setTitle(
                        getResources().getString(R.string.menuItemDirection));
                break;
        }

        // update location menu item
        PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
        switch (positionManagerInstance.getLocationSource()) {
            case Constants.LOCATION_SOURCE.GPS:
                if (currentLocation.equals(PositionManager.getDummyLocation(this))) {
                    menu.findItem(R.id.menuItemLocation).setTitle(
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.locationSourceGPS),
                                getResources().getString(R.string.messageError1004))
                            );
                } else {
                    menu.findItem(R.id.menuItemLocation).setTitle(
                            String.format(
                                "%1$s: %2$s: %3$d %4$s",
                                getResources().getString(R.string.locationSourceGPS),
                                getResources().getString(R.string.labelGPSAccuracy),
                                Math.round(((GPS) currentLocation.getPoint()).getAccuracy()),
                                getResources().getString(R.string.unitMeters))
                            );
                }
                break;
            case Constants.LOCATION_SOURCE.SIMULATION:
                if (currentLocation.equals(PositionManager.getDummyLocation(this))) {
                    menu.findItem(R.id.menuItemLocation).setTitle(
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.locationSourceSimulatedPoint),
                                getResources().getString(R.string.messageError1004))
                            );
                } else {
                    menu.findItem(R.id.menuItemLocation).setTitle(
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.locationSourceSimulatedPoint),
                                currentLocation.getPoint().getName())
                            );
                }
                break;
            default:
                menu.findItem(R.id.menuItemLocation).setTitle(
                        getResources().getString(R.string.menuItemLocation));
                break;
        }
        
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemDirection:
                SelectDirectionSourceDialog.newInstance().show(
                        getSupportFragmentManager(), "SelectDirectionSourceDialog");
                break;
            case R.id.menuItemLocation:
                SelectLocationSourceDialog.newInstance().show(
                        getSupportFragmentManager(), "SelectLocationSourceDialog");
                break;
            case R.id.menuItemPlanRoute:
                PlanRouteDialog.newInstance().show(
                        getSupportFragmentManager(), "PlanRouteDialog");
                break;
            case R.id.menuItemRequestAddress:
                RequestAddressDialog.newInstance().show(
                        getSupportFragmentManager(), "RequestAddressDialog");
                break;
            case R.id.menuItemSaveCurrentPosition:
                SaveCurrentPositionDialog.newInstance().show(
                        getSupportFragmentManager(), "SaveCurrentPositionDialog");
                break;
            case R.id.menuItemSettings:
                Intent intentStartSettingsActivity = new Intent(AbstractActivity.this, SettingsActivity.class);
                startActivity(intentStartSettingsActivity);
                break;
            case R.id.menuItemInfo:
                Intent intentStartInfoActivity = new Intent(AbstractActivity.this, InfoActivity.class);
                startActivity(intentStartInfoActivity);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
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
        invalidateOptionsMenu();

        // listen for some actions
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_SERVER_STATUS_UPDATED);
        filter.addAction(Constants.ACTION_NEW_DIRECTION);
        filter.addAction(Constants.ACTION_NEW_LOCATION);
        filter.addAction(Constants.ACTION_UPDATE_UI);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (! lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // launch system settings activity to activate gps
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);

        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // ask for location permission
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ASK_FOR_LOCATION_PERMISSION_ID);

        } else if (globalInstance.applicationWasInBackground()) {
            globalInstance.setApplicationInBackground(false);

            // activate sensors
            positionManagerInstance.startGPS();
            directionManagerInstance.startSensors();

            // server status
            ServerSettings serverSettings = settingsManagerInstance.getServerSettings();
            ServerStatus serverStatus = new ServerStatus(
                    this, ServerStatus.ACTION_UPDATE_BOTH, serverSettings.getServerURL(), serverSettings.getSelectedMap());
            serverStatus.execute();
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
            /*
            // print intent and extras
            System.out.println("xxx abstract action: " + intent.getAction());
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                for (String key : bundle.keySet()) {
                    Object value = bundle.get(key);
                    System.out.println("xxx     " + String.format("%s %s (%s)", key, value.toString(), value.getClass().getName()));
                }
            }
            */
            // kill previous instance of simple message dialog
            System.out.println("xxx abstract action: " + intent.getAction());
            if (simpleMessageDialog != null) {
                System.out.println("xxx dismiss");
                simpleMessageDialog.dismiss();
                simpleMessageDialog = null;
            }
            // process intent action
            if (
                       (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                        && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD1.ID)
                    || (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)
                        && intent.getIntExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_THRESHOLD_ID, -1) >= DirectionManager.THRESHOLD1.ID)
                    ) {
                invalidateOptionsMenu();
            } else if(intent.getAction().equals(Constants.ACTION_SERVER_STATUS_UPDATED)) {
                if (! AccessDatabase.getInstance(context).getMapList().isEmpty()
                        && SettingsManager.getInstance(context).getServerSettings().getSelectedMap() == null) {
                    System.out.println("xxx select map");
                    SelectMapDialog.newInstance(null)
                        .show(getSupportFragmentManager(), "SelectMapDialog");
                } else if (! AccessDatabase.getInstance(context).getPublicTransportProviderList().isEmpty()
                        && SettingsManager.getInstance(context).getServerSettings().getSelectedPublicTransportProvider() == null) {
                    SelectPublicTransportProviderDialog.newInstance(null)
                        .show(getSupportFragmentManager(), "SelectPublicTransportProviderDialog");
                } else if (! intent.getStringExtra(Constants.ACTION_SERVER_STATUS_UPDATED_ATTR.STRING_RETURN_MESSAGE).equals("")) {
                    System.out.println("xxx simple");
                    simpleMessageDialog = SimpleMessageDialog.newInstance(
                            intent.getStringExtra(Constants.ACTION_SERVER_STATUS_UPDATED_ATTR.STRING_RETURN_MESSAGE));
                    simpleMessageDialog.show(getSupportFragmentManager(), "SimpleMessageDialog");
                }
            } else if(intent.getAction().equals(Constants.ACTION_UPDATE_UI)) {
                onPause();
                onResume();
            }
        }
    };

}
