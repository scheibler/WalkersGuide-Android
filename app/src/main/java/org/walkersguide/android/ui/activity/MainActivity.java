package org.walkersguide.android.ui.activity;

import androidx.fragment.app.DialogFragment;
import org.walkersguide.android.ui.dialog.InfoDialog;
import org.walkersguide.android.ui.fragment.tabs.OverviewTabLayoutFragment;
import org.walkersguide.android.ui.fragment.tabs.PointsTabLayoutFragment;
import org.walkersguide.android.ui.fragment.tabs.RoutesTabLayoutFragment;
import org.walkersguide.android.ui.fragment.tabs.ObjectDetailsTabLayoutFragment;
import org.walkersguide.android.shortcut.StaticShortcutAction;
import org.walkersguide.android.data.angle.bearing.BearingSensorValue;
import org.walkersguide.android.sensor.bearing.BearingSensor;
import org.walkersguide.android.sensor.bearing.BearingSensorAccuracyRating;
import android.net.Uri;
import org.walkersguide.android.ui.dialog.toolbar.BearingDetailsDialog;
import org.walkersguide.android.ui.dialog.toolbar.LocationDetailsDialog;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import android.Manifest;

import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import android.widget.ImageButton;
import android.widget.TextView;

import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.GlobalInstance;
import androidx.appcompat.widget.Toolbar;
import android.app.Dialog;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import android.view.WindowManager;

import androidx.viewpager2.adapter.FragmentStateAdapter;
import org.walkersguide.android.ui.dialog.create.RouteFromGpxFileDialog;
import androidx.annotation.NonNull;
import org.walkersguide.android.ui.fragment.object_list.extended.PoiListFromServerFragment;
import org.walkersguide.android.ui.dialog.SendFeedbackDialog;

import android.os.Bundle;

import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;


import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.ui.dialog.WhereAmIDialog;
import org.walkersguide.android.ui.dialog.create.SaveCurrentLocationDialog;
import org.walkersguide.android.ui.fragment.tabs.routes.RouterFragment;
import androidx.fragment.app.FragmentActivity;
import java.util.ArrayList;
import org.walkersguide.android.util.SettingsManager;
import android.content.Context;
import android.content.Intent;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.ui.fragment.object_list.extended.HikingTrailListFromServerFragment;
import timber.log.Timber;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.ui.dialog.create.EnterAddressDialog;
import org.walkersguide.android.ui.dialog.create.EnterCoordinatesDialog;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.ui.dialog.create.PointFromCoordinatesLinkDialog;

import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import android.view.GestureDetector;
import android.view.MotionEvent;
import org.walkersguide.android.data.angle.Bearing;
import java.util.List;


public class MainActivity extends AppCompatActivity implements MainActivityController, OnTabSelectedListener {
    public static String EXTRA_NEW_TAB = "newTab";
    public static String EXTRA_CLEAR_BACK_STACK = "clearBackStack";

    public static void loadRoute(Context context, Route route) {
        SettingsManager settingsManagerInstance = SettingsManager.getInstance();
        settingsManagerInstance.setSelectedRoute(route);
        settingsManagerInstance.setSelectedTabForMainActivity(MainActivity.Tab.ROUTES);
        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        mainActivityIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mainActivityIntent.putExtra(EXTRA_CLEAR_BACK_STACK, true);
        context.startActivity(mainActivityIntent);
    }

    public static void loadPoiProfile(Context context, PoiProfile poiProfile) {
        SettingsManager settingsManagerInstance = SettingsManager.getInstance();
        settingsManagerInstance.setSelectedPoiProfile(poiProfile);
        settingsManagerInstance.setSelectedTabForMainActivity(MainActivity.Tab.POINTS);
        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        mainActivityIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mainActivityIntent.putExtra(EXTRA_CLEAR_BACK_STACK, true);
        context.startActivity(mainActivityIntent);
    }


    private GlobalInstance globalInstance;
    private DeviceSensorManager deviceSensorManagerInstance;
    private PositionManager positionManagerInstance;
	private SettingsManager settingsManagerInstance;
    private boolean skipPositionManagerInitialisationDuringOnResume;

    private Toolbar toolbar;
    private TextView labelToolbarTitle;
    private ImageButton buttonUpperLeftCorner, buttonBearingDetails, buttonLocationDetails;

	private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TabLayout tabLayout;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerFlingGestureDetector();

        globalInstance = (GlobalInstance) getApplicationContext();
        deviceSensorManagerInstance = DeviceSensorManager.getInstance();
        positionManagerInstance = PositionManager.getInstance();
		settingsManagerInstance = SettingsManager.getInstance();
        skipPositionManagerInitialisationDuringOnResume = false;

        // toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        labelToolbarTitle = (TextView) findViewById(R.id.labelToolbarTitle);

        buttonUpperLeftCorner = (ImageButton) findViewById(R.id.buttonUpperLeftCorner);
        buttonUpperLeftCorner.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Timber.d("buttonUpperLeftCorner getBackStackEntryCount(): %1$d", getSupportFragmentManager().getBackStackEntryCount());
                if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
                    for (int i=1; i<getSupportFragmentManager().getBackStackEntryCount(); i++) {
                        getSupportFragmentManager().popBackStack();
                    }
                    updateToolbarButtonInTheUpperLeftCorner();
                } else {
                    // open main menu
                    drawerLayout.open();
                }
            }
        });

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

        // navigation drawer
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        navigationView = (NavigationView) findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(MenuItem menuItem) {
                drawerLayout.closeDrawers();
                if (menuItem.getItemId() == R.id.menuItemPlanRoute) {
                    openPlanRouteDialog();
                } else if (menuItem.getItemId() == R.id.menuItemCreateFavoriteCurrentPosition) {
                    SaveCurrentLocationDialog.newInstance()
                        .show(getSupportFragmentManager(), "SaveCurrentLocationDialog");
                } else if (menuItem.getItemId() == R.id.menuItemRouteFromGpxFile) {
                    RouteFromGpxFileDialog.newInstance()
                        .show(getSupportFragmentManager(), "RouteFromGpxFileDialog");
                } else if (menuItem.getItemId() == R.id.menuItemInfo) {
                    InfoDialog.newInstance()
                        .show(getSupportFragmentManager(), "InfoDialog");
                } else if (menuItem.getItemId() == R.id.menuItemContactMe) {
                    SendFeedbackDialog.newInstance(
                            SendFeedbackDialog.FeedbackToken.QUESTION)
                        .show(getSupportFragmentManager(), "SendFeedbackDialog");
                } else {
                    return false;
                }
                return true;
            }
        });

        // tab layout
		tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.selectTab(null);
        tabLayout.addOnTabSelectedListener(this);
        openTab(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Timber.d("onNewIntent");
        openTab(intent);
    }

    private void openTab(Intent intent) {
        Tab tabToOpen = null;
        if (intent != null && intent.getExtras() != null) {
            tabToOpen = (Tab) intent.getExtras().getSerializable(EXTRA_NEW_TAB);
            // clear back stack is optional
            if (tabToOpen != null
                    && intent.getExtras().getBoolean(EXTRA_CLEAR_BACK_STACK, false)) {
                clearBackStackForTab(tabToOpen);
            }
            Timber.d("new tab from intent: %1$s", tabToOpen);
        }
        Timber.d("openTab");
        loadFragment(tabToOpen != null ? tabToOpen : getCurrentTab());
    }

    @Override public void onBackPressed() {
        if (drawerLayout.isOpen()) {
            drawerLayout.closeDrawers();
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            super.onBackPressed();
            updateToolbarButtonInTheUpperLeftCorner();
        } else {
            finish();
        }
    }


    /**
     * toolbar
     */

    @Override public void configureToolbarTitle(String title) {
        labelToolbarTitle.setText(title);
    }

    private void updateToolbarButtonInTheUpperLeftCorner() {
        getSupportFragmentManager().executePendingTransactions();
        buttonUpperLeftCorner.setImageResource(
                getSupportFragmentManager().getBackStackEntryCount() > 1
                ? R.drawable.back_to_top
                : R.drawable.main_menu);
        buttonUpperLeftCorner.setContentDescription(
                getSupportFragmentManager().getBackStackEntryCount() > 1
                ? getResources().getString(R.string.navigateUp)
                : getResources().getString(R.string.mainMenu));
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
        displayRemainsActiveSettingChanged(
                settingsManagerInstance.getDisplayRemainsActive());
        updateBearingDetailsButton();
        updateLocationDetailsButton();

        // listen for some actions
        IntentFilter filter = new IntentFilter();
        filter.addAction(PositionManager.ACTION_NO_LOCATION_PROVIDER_AVAILABLE);
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

            // keep bt headset connection alive
            if (settingsManagerInstance.getKeepBluetoothHeadsetConnectionAlive()) {
                globalInstance.startPlaybackOfSilenceWavFile();
            }

            // handle static shortcuts
            for (StaticShortcutAction action : globalInstance.getEnabledStaticShortcutActions()) {
                if (action == StaticShortcutAction.OPEN_PLAN_ROUTE_DIALOG) {
                    openPlanRouteDialog();
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

    @Override public void displayRemainsActiveSettingChanged(boolean remainsActive) {
        Timber.d("displayRemainsActiveSettingChanged: %1$s", remainsActive);
        if (remainsActive) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


    /**
     * broadcast receiver
     */

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(PositionManager.ACTION_NO_LOCATION_PROVIDER_AVAILABLE)) {
                skipPositionManagerInitialisationDuringOnResume = true;
                showMessage(
                        context.getResources().getString(R.string.messageNoLocationProviderAvailable));

            } else if (intent.getAction().equals(PositionManager.ACTION_LOCATION_PROVIDER_DISABLED)) {
                Dialog enableLocationDialog = new AlertDialog.Builder(MainActivity.this)
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
                if (! ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // ask for location permission for the first time
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[] {
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION },
                            PERMISSION_REQUEST_FOREGROUND_LOCATION);
                } else {
                    // open app info page on later permission requests
                    Dialog appInfoDialog = new AlertDialog.Builder(MainActivity.this)
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

            } else if (drawerLayout != null && ! drawerLayout.isOpen()) {
                if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                    updateBearingDetailsButton();
                } else if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                    updateLocationDetailsButton();
                }
            }
        }
    };


    // acquire permission results
    private static final int PERMISSION_REQUEST_ENABLE_GPS = 1;
    private static final int PERMISSION_REQUEST_APP_SETTINGS = 2;
    private static final int PERMISSION_REQUEST_FOREGROUND_LOCATION = 3;

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


    /**
     * tabs
     */

    public enum Tab {

        OVERVIEW(0),
        ROUTES(1),
        POINTS(2);

        public static Tab getTabAtPosition(int position) {
            for (Tab tab : Tab.values()) {
                if (tab.position == position) {
                    return tab;
                }
            }
            return null;
        }

        public int position;

        private Tab(int position) {
            this.position = position;
        }
    }

    private Tab getCurrentTab() {
        return settingsManagerInstance.getSelectedTabForMainActivity();
    }

    private void clearBackStackForTab(Tab tab) {
        getSupportFragmentManager().clearBackStack(tab.name());
        getSupportFragmentManager().executePendingTransactions();
    }

    private void loadFragment(Tab newTab) {
        Timber.d("loadFragment: %1$s", newTab);
        tabLayout.selectTab(
                tabLayout.getTabAt(newTab.position));
    }

    // TabLayout.OnTabSelectedListener

    @Override public void onTabSelected(TabLayout.Tab tabLayoutTab) {
        Tab newTab = Tab.getTabAtPosition(tabLayoutTab.getPosition());
        Timber.d("onTabSelected: %1$s", newTab);
        if (newTab == null) {
            // just a precaution
            newTab = SettingsManager.DEFAULT_SELECTED_TAB_MAIN_ACTIVITY;
        }
        String tag = newTab.name();

        Timber.d("loadFragment before restore, getBackStackEntryCount(): %1$d", getSupportFragmentManager().getBackStackEntryCount());
        getSupportFragmentManager().saveBackStack(getCurrentTab().name());
        getSupportFragmentManager().restoreBackStack(tag);
        getSupportFragmentManager().executePendingTransactions();
        Timber.d("loadFragment after restore, getBackStackEntryCount(): %1$d", getSupportFragmentManager().getBackStackEntryCount());

        // only replace, if the fragment is not already attached
        Timber.d("fragment %1$s for tag %2$s", getSupportFragmentManager().findFragmentByTag(tag), tag);
        if (getSupportFragmentManager().findFragmentByTag(tag) == null) {

            Fragment fragment = null;
            switch (newTab) {
                case OVERVIEW:
                    fragment = OverviewTabLayoutFragment.newInstance();
                    break;
                case ROUTES:
                    fragment = RoutesTabLayoutFragment.newInstance();
                    break;
                case POINTS:
                    fragment = PointsTabLayoutFragment.newInstance();
                    break;
                default:
                    return;
            }

            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainerView, fragment, tag)
                .setReorderingAllowed(true)
                .addToBackStack(tag)
                .commit();
        }

        updateToolbarButtonInTheUpperLeftCorner();
        settingsManagerInstance.setSelectedTabForMainActivity(newTab);
    }

    @Override public void onTabUnselected(TabLayout.Tab tab) {
        Timber.d("onTabUnselected");
    }

    @Override public void onTabReselected(TabLayout.Tab tab) {
        Timber.d("onTabReselected");
    }

    // MainActivityController

    @Override public void openPlanRouteDialog() {
        // close, if already open
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment instanceof PlanRouteDialog) {
                    ((PlanRouteDialog) fragment).dismiss();
                    Timber.d("PlanRouteDialog dismissed");
                }
            }
        }
        // open on top
        PlanRouteDialog.newInstance()
            .show(getSupportFragmentManager(), "PlanRouteDialog");
    }

    @Override public void addFragment(DialogFragment fragment) {
        if (hasOpenDialog()) {
            Timber.d("addFragment: fragment wnats to be a dialog");
            fragment.show(getSupportFragmentManager(), null);
        } else {
            Timber.d("addFragment: want to be embedded");
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainerView, fragment, null)
                .setReorderingAllowed(true)
                .addToBackStack(getCurrentTab().name())
                .commit();
            updateToolbarButtonInTheUpperLeftCorner();
        }
    }

    private boolean hasOpenDialog() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment instanceof DialogFragment) {
                    if (((DialogFragment) fragment).getShowsDialog()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    // GestureDetector.SimpleOnGestureListener
    private static final long VELOCITY_THRESHOLD = 2000;
    private GestureDetector mDetector;

    private void registerFlingGestureDetector() {
        mDetector = new GestureDetector(
                MainActivity.this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                            final float velocityX, final float velocityY){

                        Timber.d("x=%1$f, y=%2$f", Math.abs(velocityX), Math.abs(velocityY));
                        if (Math.abs(velocityX) < VELOCITY_THRESHOLD
                                && Math.abs(velocityY) < VELOCITY_THRESHOLD){
                            return false;//if the fling is not fast enough then it's just like drag
                        }

                        // if velocity in X direction is higher than velocity in Y direction,
                        // then the fling is horizontal, else->vertical
                        if (Math.abs(velocityX) > Math.abs(velocityY)){
                            if (velocityX >= 0) {
                                // if velocityX is positive, then it's towards right
                                if (drawerLayout != null && drawerLayout.isOpen()) {
                                    drawerLayout.closeDrawers();
                                } else if (! hasOpenDialog()) {
                                    previousFragment();
                                }
                            } else {
                                // if velocityX is negative, then it's towards left
                                if (drawerLayout != null && drawerLayout.isOpen()) {
                                    // do nothing
                                } else if (! hasOpenDialog()) {
                                    nextFragment();
                                }
                            }
                            return true;
                        }

                        return super.onFling(e1, e2, velocityX, velocityY);
                    }
                });
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    private void previousFragment() {
        Tab previousTab = Tab.getTabAtPosition(
                getCurrentTab().position - 1);
        if (previousTab != null) {
            loadFragment(previousTab);
        }
    }

    private void nextFragment() {
        Tab nextTab = Tab.getTabAtPosition(
                getCurrentTab().position + 1);
        if (nextTab != null) {
            loadFragment(nextTab);
        }
    }

}
