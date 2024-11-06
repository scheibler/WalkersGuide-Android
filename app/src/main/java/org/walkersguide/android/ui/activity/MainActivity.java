package org.walkersguide.android.ui.activity;

import org.walkersguide.android.ui.dialog.WhereAmIDialog;
import org.walkersguide.android.ui.dialog.create.EnterAddressDialog;
import org.walkersguide.android.BuildConfig;
import java.util.List;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import com.google.android.material.tabs.TabLayout;

import android.Manifest;

import android.annotation.SuppressLint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.Uri;

import android.os.Bundle;

import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import android.widget.ImageButton;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;

import androidx.core.app.ActivityCompat;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import timber.log.Timber;

import org.walkersguide.android.R;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.bearing.BearingSensorValue;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.database.profile.static_profile.HistoryProfile;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.sensor.bearing.BearingSensor;
import org.walkersguide.android.sensor.bearing.BearingSensorAccuracyRating;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.shortcut.StaticShortcutAction;
import org.walkersguide.android.ui.dialog.ChangelogDialog;
import org.walkersguide.android.ui.dialog.InfoDialog;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.ui.dialog.SendFeedbackDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.ui.dialog.create.SaveCurrentLocationDialog;
import org.walkersguide.android.ui.dialog.toolbar.BearingDetailsDialog;
import org.walkersguide.android.ui.dialog.toolbar.LocationDetailsDialog;
import org.walkersguide.android.ui.fragment.HistoryFragment;
import org.walkersguide.android.ui.fragment.SettingsFragment;
import org.walkersguide.android.ui.fragment.TabLayoutFragment;
import org.walkersguide.android.ui.fragment.object_list.extended.PoiListFromServerFragment;
import org.walkersguide.android.ui.fragment.profile_list.CollectionListFragment;
import org.walkersguide.android.ui.fragment.tabs.ObjectDetailsTabLayoutFragment;
import org.walkersguide.android.ui.fragment.tabs.OverviewTabLayoutFragment;
import org.walkersguide.android.ui.fragment.tabs.PointsTabLayoutFragment;
import org.walkersguide.android.ui.fragment.tabs.RoutesTabLayoutFragment;
import org.walkersguide.android.ui.fragment.tabs.routes.NavigateFragment;
import org.walkersguide.android.ui.view.builder.TextViewBuilder;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.WalkersGuideService.ServiceState;
import org.walkersguide.android.util.WalkersGuideService.StartServiceFailure;
import org.walkersguide.android.util.WalkersGuideService;
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.ui.dialog.create.ImportGpxFileDialog;
import android.text.TextUtils;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.server.wg.p2p.P2pRouteRequest;
import org.walkersguide.android.util.Helper;
import java.util.ArrayList;
import android.hardware.Sensor;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity
        implements FragmentResultListener, MainActivityController, OnTabSelectedListener {
    private static String EXTRA_NEW_TAB = "newTab";
    private static String EXTRA_NEW_SUB_TAB = "newSubTab";

    public static void loadRoute(Context context, Route route) {
        if (route == null) {
            return;
        }
        GlobalInstance.getInstance().clearRouteCurrentPosition(route);
        SettingsManager.getInstance().setLastSelectedRoute(route);

        switch (route.getType()) {
            case P2P_ROUTE:
                HistoryProfile.plannedRoutes().addObject(route);
                break;
            case GPX_TRACK:
                HistoryProfile.routesFromGpxFile().addObject(route);
                break;
            case RECORDED_ROUTE:
                HistoryProfile.recordedRoutes().addObject(route);
                break;
        }

        // see openTab(...) function below
        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        mainActivityIntent.putExtra(EXTRA_NEW_TAB, Tab.ROUTES);
        mainActivityIntent.putExtra(EXTRA_NEW_SUB_TAB, RoutesTabLayoutFragment.Tab.NAVIGATE);
        context.startActivity(mainActivityIntent);
    }

    public static void startRouteRecording(Context context) {
        WalkersGuideService.startRouteRecording();
        // see openTab(...) function below
        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        mainActivityIntent.putExtra(EXTRA_NEW_TAB, Tab.ROUTES);
        mainActivityIntent.putExtra(EXTRA_NEW_SUB_TAB, RoutesTabLayoutFragment.Tab.RECORD);
        context.startActivity(mainActivityIntent);
    }

    // activity
    private static String KEY_CONTROL_SIMULATION_ACCESSIBILITY_ACTION_ID_FOR_BEARING = "controlSimulationAccessibilityActionIdForBearing";
    private static String KEY_CONTROL_SIMULATION_ACCESSIBILITY_ACTION_ID_FOR_LOCATION = "controlSimulationAccessibilityActionIdForLocation";
    private static String KEY_SKIP_POSITION_MANAGER_INITIALISATION_DURING_ON_RESUME = "skipPositionManagerInitialisationDuringOnResume";
    private static final String KEY_LAST_URI = "lastUri";

    private GlobalInstance globalInstance;
    private DeviceSensorManager deviceSensorManagerInstance;
    private PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;

    private ArrayList<Integer> bearingSourceAccessibilityActionIdList = new ArrayList<Integer>();;
    private int controlSimulationAccessibilityActionIdForBearing, controlSimulationAccessibilityActionIdForLocation;
    private boolean broadcastReceiverAlreadyRegistered, skipPositionManagerInitialisationDuringOnResume;
    private Uri lastUri;

    private Toolbar toolbar;
    private TextView labelToolbarTitle, labelWalkersGuideServiceNotRunningWarning;
    private ImageButton buttonNavigateUp, buttonBearingDetails, buttonLocationDetails;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TabLayout tabLayout;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate");
        setContentView(R.layout.activity_main);
        registerFlingGestureDetector();
        AccessDatabase.getInstance();   // important for app-started-for-the-first-time detection

        globalInstance = (GlobalInstance) getApplicationContext();
        deviceSensorManagerInstance = DeviceSensorManager.getInstance();
        positionManagerInstance = PositionManager.getInstance();
        settingsManagerInstance = SettingsManager.getInstance();
        broadcastReceiverAlreadyRegistered = false;
        controlSimulationAccessibilityActionIdForBearing =
            savedInstanceState != null
            ? savedInstanceState.getInt(KEY_CONTROL_SIMULATION_ACCESSIBILITY_ACTION_ID_FOR_BEARING)
            : View.NO_ID;
        controlSimulationAccessibilityActionIdForLocation =
            savedInstanceState != null
            ? savedInstanceState.getInt(KEY_CONTROL_SIMULATION_ACCESSIBILITY_ACTION_ID_FOR_LOCATION)
            : View.NO_ID;
        skipPositionManagerInitialisationDuringOnResume =
            savedInstanceState != null
            ? savedInstanceState.getBoolean(KEY_SKIP_POSITION_MANAGER_INITIALISATION_DURING_ON_RESUME)
            : false;
        lastUri =
            savedInstanceState != null
            ? savedInstanceState.getParcelable(KEY_LAST_URI)
            : null;

        getSupportFragmentManager()
            .setFragmentResultListener(
                    ImportGpxFileDialog.REQUEST_IMPORT_OF_GPX_FILE_WAS_SUCCESSFUL, this, this);
        getSupportFragmentManager()
            .setFragmentResultListener(
                    EnterAddressDialog.REQUEST_ENTER_ADDRESS, this, this);

        // toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        labelToolbarTitle = (TextView) findViewById(R.id.labelToolbarTitle);
        labelWalkersGuideServiceNotRunningWarning = (TextView) findViewById(R.id.labelWalkersGuideServiceNotRunningWarning);

        buttonNavigateUp = (ImageButton) findViewById(R.id.buttonNavigateUp);
        buttonNavigateUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                popEverythingButTheTabLayoutFragmentFromBackStack();
            }
        });

        ImageButton buttonMainMenu = (ImageButton) findViewById(R.id.buttonMainMenu);
        buttonMainMenu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // open main menu
                drawerLayout.open();
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
                    openPlanRouteDialog(false);
                } else if (menuItem.getItemId() == R.id.menuItemSaveCurrentLocation) {
                    SaveCurrentLocationDialog.addToDatabaseProfile()
                        .show(getSupportFragmentManager(), "SaveCurrentLocationDialog");
                } else if (menuItem.getItemId() == R.id.menuItemOpenWhereAmIDialog) {
                    WhereAmIDialog.newInstance()
                        .show(getSupportFragmentManager(), "WhereAmIDialog");
                } else if (menuItem.getItemId() == R.id.menuItemCollections) {
                    CollectionListFragment.newInstance()
                        .show(getSupportFragmentManager(), "CollectionListFragment");
                } else if (menuItem.getItemId() == R.id.menuItemHistory) {
                    HistoryFragment.newInstance()
                        .show(getSupportFragmentManager(), "HistoryFragment");
                } else if (menuItem.getItemId() == R.id.menuItemSettings) {
                    SettingsFragment.newInstance()
                        .show(getSupportFragmentManager(), "SettingsFragment");
                } else if (menuItem.getItemId() == R.id.menuItemInfo) {
                    InfoDialog.newInstance()
                        .show(getSupportFragmentManager(), "InfoDialog");
                } else if (menuItem.getItemId() == R.id.menuItemUserManual) {
                    Intent openBrowserIntent = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                getResources().getString(R.string.variableUserManualUrl)));
                    startActivity(openBrowserIntent);
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

        TabLayout.Tab tabOverview = tabLayout.newTab();
        tabOverview.setCustomView(
                createTabLayoutTabCustomView(
                Tab.OVERVIEW, OverviewTabLayoutFragment.Tab.values()));
        tabLayout.addTab(tabOverview);

        TabLayout.Tab tabRoutes = tabLayout.newTab();
        tabRoutes.setCustomView(
                createTabLayoutTabCustomView(
                Tab.ROUTES, RoutesTabLayoutFragment.Tab.values()));
        tabLayout.addTab(tabRoutes);

        TabLayout.Tab tabPoints = tabLayout.newTab();
        tabPoints.setCustomView(
                createTabLayoutTabCustomView(
                Tab.POINTS, PointsTabLayoutFragment.Tab.values()));
        tabLayout.addTab(tabPoints);

        tabLayout.selectTab(null);
        tabLayout.addOnTabSelectedListener(this);
        openTab(getIntent());
    }

    private TextView createTabLayoutTabCustomView(final Tab tab, final Enum<?>[] subTabs) {
        final LayoutParams lpMatchParent = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        TextView customView = new TextViewBuilder(
                    MainActivity.this, tab.toString(), lpMatchParent)
            .setContentDescription(
                    String.format(
                        "%1$s, %2$s", tab.toString(), getResources().getString(R.string.viewClassNameTab)))
            .centerTextVerticallyAndHorizontally()
            .create();

        customView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                loadFragment(tab);
            }
        });

        customView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View view) {
                PopupMenu contextMenu = new PopupMenu(view.getContext(), view);
                for (int i=0; i<subTabs.length; i++) {
                    contextMenu.getMenu().add(
                            Menu.NONE, i, i+1, subTabs[i].toString());
                }
                contextMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override public boolean onMenuItemClick(MenuItem item) {
                        int i = item.getItemId();
                        if (i >= 0 && i < subTabs.length) {
                            loadFragment(tab, subTabs[i], false);
                        } else {
                            return false;
                        }
                        return true;
                    }
                });
                contextMenu.show();
                return true;
            }
        });

        for (int i=0; i<subTabs.length; i++) {
            final Enum<?> subTab = subTabs[i];
            ViewCompat.addAccessibilityAction(
                    customView,
                    subTab.toString(),
                    (actionView, arguments) -> {
                        loadFragment(tab, subTab, false);
                        return true;
                    });
        }

        return customView;
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Timber.d("onNewIntent");
        openTab(intent);
    }

    private void openTab(Intent intent) {
        Tab tabToOpen = null;
        Enum<?> subTabToOpen = null;

        if (globalInstance.isDynamicShortcutActionEnabled()) {
            tabToOpen = Tab.POINTS;
            subTabToOpen = PointsTabLayoutFragment.Tab.NEARBY;
        } else if (intent != null && intent.getExtras() != null) {
            tabToOpen = (Tab) intent.getExtras().getSerializable(EXTRA_NEW_TAB);
            subTabToOpen = (Enum<?>) intent.getExtras().getSerializable(EXTRA_NEW_SUB_TAB);
        }

        loadFragment(
                tabToOpen != null ? tabToOpen : getCurrentTab(),
                subTabToOpen,
                globalInstance.isDynamicShortcutActionEnabled());
        globalInstance.setDynamicShortcutActionEnabled(false);

        Uri uri = intent.getData();
        if (uri != null
                && ! uri.equals(lastUri)) {
            Timber.d("uri from intent filter: %1$s", uri.toString());
            if ("geo".equals(uri.getScheme())) {
                String[] parts = uri.toString().split("\\?q=");
                if (parts.length > 1
                        && ! TextUtils.isEmpty(parts[1])) {
                    String addressString = null;
                    try {
                        addressString = URLDecoder.decode(
                                parts[1], StandardCharsets.UTF_8.name());
                    } catch (UnsupportedEncodingException e) {
                        addressString = parts[1];
                    }
                    EnterAddressDialog.newInstance(addressString)
                        .show(getSupportFragmentManager(), "EnterAddressDialog");
                } else {
                    SimpleMessageDialog.newInstance(
                            getResources().getString(R.string.errorAddressStringFromIntentEmpty))
                        .show(getSupportFragmentManager(), "SimpleMessageDialog");
                }
            } else {
                ImportGpxFileDialog.newInstance(uri, false)
                    .show(getSupportFragmentManager(), "ImportGpxFileDialog");
            }
            lastUri = uri;
        }
    }

    @Override public void onBackPressed() {
        if (drawerLayout.isOpen()) {
            drawerLayout.closeDrawers();
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            super.onBackPressed();
            updateToolbarNavigateUpButtonVisibility();
        } else {
            finish();
        }
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        Timber.d("onFragmentResult: requestKey=%1$s", requestKey);
        if (requestKey.equals(EnterAddressDialog.REQUEST_ENTER_ADDRESS)) {
            StreetAddress newRouteDestination = (StreetAddress) bundle.getSerializable(EnterAddressDialog.EXTRA_STREET_ADDRESS);
            if (newRouteDestination != null) {
                ObjectDetailsTabLayoutFragment.details(newRouteDestination)
                    .show(getSupportFragmentManager(), "Details");
            }

        } else if (requestKey.equals(ImportGpxFileDialog.REQUEST_IMPORT_OF_GPX_FILE_WAS_SUCCESSFUL)) {
            DatabaseProfile importedGpxFileCollection = (DatabaseProfile) bundle.getSerializable(ImportGpxFileDialog.EXTRA_GPX_FILE_PROFILE);
            if (importedGpxFileCollection != null) {
                ObjectListFromDatabaseFragment.newInstance(importedGpxFileCollection)
                    .show(getSupportFragmentManager(), "ImportedGpxFileCollection");
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_CONTROL_SIMULATION_ACCESSIBILITY_ACTION_ID_FOR_BEARING, controlSimulationAccessibilityActionIdForBearing);
        savedInstanceState.putInt(KEY_CONTROL_SIMULATION_ACCESSIBILITY_ACTION_ID_FOR_LOCATION, controlSimulationAccessibilityActionIdForLocation);
        savedInstanceState.putBoolean(KEY_SKIP_POSITION_MANAGER_INITIALISATION_DURING_ON_RESUME, skipPositionManagerInitialisationDuringOnResume);
        savedInstanceState.putParcelable(KEY_LAST_URI, lastUri);
    }

    @Override public void recreateActivity() {
        MainActivity.this.recreate();
    }


    /**
     * toolbar
     */

    @Override public void configureToolbarTitle(String title) {
        labelToolbarTitle.setText(title);
    }

    private void updateToolbarNavigateUpButtonVisibility() {
        getSupportFragmentManager().executePendingTransactions();
        buttonNavigateUp.setVisibility(
                getSupportFragmentManager().getBackStackEntryCount() > 1
                ? View.VISIBLE : View.GONE);
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

    private void updateAccessibilityActionsOnBearingDetailsButton() {
        // bearing source
        //
        // clear
        for (Integer actionId : bearingSourceAccessibilityActionIdList) {
            ViewCompat.removeAccessibilityAction(buttonBearingDetails, actionId);
        }
        bearingSourceAccessibilityActionIdList.clear();

        if (! settingsManagerInstance.getAutoSwitchBearingSourceEnabled()) {
            for (final BearingSensor sensor : BearingSensor.values()) {
                if (sensor.equals(settingsManagerInstance.getSelectedBearingSensor())) {
                    continue;
                }

                int actionId = ViewCompat.addAccessibilityAction(
                        this.buttonBearingDetails,
                        String.format(
                            GlobalInstance.getStringResource(R.string.accessibilityActionUseBearingSource),
                            sensor.toString()),
                        (actionView, arguments) -> {
                            if (sensor.equals(BearingSensor.SATELLITE)
                                        && deviceSensorManagerInstance.getBearingValueFromSatellite() == null) {
                                // can't enable
                                Toast.makeText(
                                        MainActivity.this,
                                        getResources().getString(R.string.errorNoBearingFound),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                ViewCompat.setAccessibilityLiveRegion(
                                        buttonBearingDetails, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                                Helper.vibrateOnce(
                                        Helper.VIBRATION_DURATION_SHORT, Helper.VIBRATION_INTENSITY_WEAK);
                                deviceSensorManagerInstance.setSelectedBearingSensor(sensor);
                            }
                            return true;
                        });
                if (actionId != View.NO_ID) {
                    bearingSourceAccessibilityActionIdList.add(actionId);
                }
            }
        }

        // simulation status
        //
        // clear
        if (controlSimulationAccessibilityActionIdForBearing != View.NO_ID) {
            ViewCompat.removeAccessibilityAction(
                    this.buttonBearingDetails, controlSimulationAccessibilityActionIdForBearing);
        }

        // and create action to switch status
        Bearing simulatedBearing = deviceSensorManagerInstance.getSimulatedBearing();
        if (simulatedBearing != null) {
            controlSimulationAccessibilityActionIdForBearing = ViewCompat.addAccessibilityAction(
                    this.buttonBearingDetails,
                    String.format(
                        deviceSensorManagerInstance.getSimulationEnabled()
                        ? GlobalInstance.getStringResource(R.string.accessibilityActionEndSimulation)
                        : GlobalInstance.getStringResource(R.string.accessibilityActionStartSimulationBearing),
                        simulatedBearing.toString()),

                    (actionView, arguments) -> {
                        ViewCompat.setAccessibilityLiveRegion(
                                buttonBearingDetails, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                        Helper.vibrateOnce(
                                Helper.VIBRATION_DURATION_SHORT, Helper.VIBRATION_INTENSITY_WEAK);
                        deviceSensorManagerInstance.setSimulationEnabled(
                                ! deviceSensorManagerInstance.getSimulationEnabled());
                        return true;
                    });
        }
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

        } else if (currentLocation instanceof GPS
                && ! positionManagerInstance.getSimulationEnabled()) {
            GPS currentGPSLocation = (GPS) currentLocation;
            locationDescriptionBuilder.append(
                    currentGPSLocation.formatAccuracyInMeters());
            if (currentGPSLocation.getMillisecondsElapsedSinceCreation() > 5*60*1000) {
                locationDescriptionBuilder.append(
                        String.format(", %1$s", GlobalInstance.getStringResource(R.string.toolbarSensorDataOutdated)));
            }

        } else {
            locationDescriptionBuilder.append(
                    currentLocation.getName());
        }

        buttonLocationDetails.setContentDescription(locationDescriptionBuilder.toString());
    }

    private void updateAccessibilityActionsOnLocationDetailsButton() {
        // clear
        if (controlSimulationAccessibilityActionIdForLocation != View.NO_ID) {
            ViewCompat.removeAccessibilityAction(
                    this.buttonLocationDetails, controlSimulationAccessibilityActionIdForLocation);
        }

        // and create action to control simulation status
        Point simulatedPoint = positionManagerInstance.getSimulatedLocation();
        if (simulatedPoint != null) {
            Timber.d("updateAccessibilityActionsOnLocationDetailsButton: set location action, sim enabled: %1$s", positionManagerInstance.getSimulationEnabled());
            controlSimulationAccessibilityActionIdForLocation = ViewCompat.addAccessibilityAction(
                    this.buttonLocationDetails,
                    String.format(
                        positionManagerInstance.getSimulationEnabled()
                        ? GlobalInstance.getStringResource(R.string.accessibilityActionEndSimulation)
                        : GlobalInstance.getStringResource(R.string.accessibilityActionStartSimulationLocation),
                        simulatedPoint.getName()),

                    (actionView, arguments) -> {
                        ViewCompat.setAccessibilityLiveRegion(
                                buttonLocationDetails, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                        Helper.vibrateOnce(
                                Helper.VIBRATION_DURATION_SHORT, Helper.VIBRATION_INTENSITY_WEAK);
                        positionManagerInstance.setSimulationEnabled(
                                ! positionManagerInstance.getSimulationEnabled());
                        return true;
                    });
        }
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        if (broadcastReceiverAlreadyRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
            broadcastReceiverAlreadyRegistered = false;
        }
        globalInstance.startActivityTransitionTimer();
        Timber.d("onPause");
    }

    @Override public void onResume() {
        super.onResume();
        globalInstance.stopActivityTransitionTimer();
        registerBroadcastReceiver();
        displayRemainsActiveSettingChanged(
                settingsManagerInstance.getDisplayRemainsActive());
        updateAccessibilityActionsOnBearingDetailsButton();
        updateBearingDetailsButton();
        updateAccessibilityActionsOnLocationDetailsButton();
        updateLocationDetailsButton();
        WalkersGuideService.requestServiceState();

        if (globalInstance.applicationWasInBackground()) {
            globalInstance.setApplicationInBackground(false);

            if (settingsManagerInstance.showChangelogDialog()) {
                ChangelogDialog.newInstance()
                    .show(getSupportFragmentManager(), "ChangelogDialog");
                settingsManagerInstance.setChangelogDialogVersionCode();

            // activate sensors
            } else if (skipPositionManagerInitialisationDuringOnResume) {
                // skip once
                skipPositionManagerInitialisationDuringOnResume = false;
            } else {
                WalkersGuideService.startService();
            }

            // keep bt headset connection alive
            if (settingsManagerInstance.getKeepBluetoothHeadsetConnectionAlive()) {
                globalInstance.startPlaybackOfSilenceWavFile();
            }

            // handle static shortcuts
            for (StaticShortcutAction action : globalInstance.getEnabledStaticShortcutActions()) {
                if (action == StaticShortcutAction.OPEN_PLAN_ROUTE_DIALOG) {
                    openPlanRouteDialog(false);
                } else if (action == StaticShortcutAction.OPEN_SAVE_CURRENT_LOCATION_DIALOG) {
                    SaveCurrentLocationDialog.addToDatabaseProfile()
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

    private void registerBroadcastReceiver() {
            Timber.d("registerBroadcastReceiver: %1$s", broadcastReceiverAlreadyRegistered);
        if (! broadcastReceiverAlreadyRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(WalkersGuideService.ACTION_START_SERVICE_FAILED);
            filter.addAction(WalkersGuideService.ACTION_SERVICE_STATE_CHANGED);
            filter.addAction(WalkersGuideService.ACTION_SHOW_ROUTE_RECORDING_ONLY_IN_FOREGROUND_MESSAGE);
            //
            filter.addAction(DeviceSensorManager.ACTION_BEARING_SENSOR_CHANGED);
            filter.addAction(DeviceSensorManager.ACTION_SIMULATION_STATUS_CHANGED);
            filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_SIMULATION);
            filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
            //
            filter.addAction(PositionManager.ACTION_SIMULATION_STATUS_CHANGED);
            filter.addAction(PositionManager.ACTION_NEW_SIMULATED_LOCATION);
            filter.addAction(PositionManager.ACTION_NEW_LOCATION);
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
            broadcastReceiverAlreadyRegistered = true;
        }
    }


    /**
     * broadcast receiver
     */
    private static final int PERMISSION_REQUEST_FOREGROUND_LOCATION = 1;
    private static final int PERMISSION_REQUEST_POST_NOTIFICATIONS = 2;

    @SuppressLint("InlinedApi, MissingPermission")
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(WalkersGuideService.ACTION_START_SERVICE_FAILED)) {
                StartServiceFailure failure = (StartServiceFailure) intent.getSerializableExtra(WalkersGuideService.EXTRA_START_SERVICE_FAILURE);
                if (failure == null) {
                    return;
                }

                switch (failure) {
                    case FOREGROUND_LOCATION_PERMISSION_DENIED:
                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                new String[] {
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION },
                                PERMISSION_REQUEST_FOREGROUND_LOCATION);
                        break;
                    case POST_NOTIFICATIONS_PERMISSION_DENIED:
                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                new String[] { Manifest.permission.POST_NOTIFICATIONS },
                                PERMISSION_REQUEST_POST_NOTIFICATIONS);
                        break;
                }

            } else if (intent.getAction().equals(WalkersGuideService.ACTION_SERVICE_STATE_CHANGED)) {
                ServiceState serviceState = (ServiceState) intent.getSerializableExtra(WalkersGuideService.EXTRA_SERVICE_STATE);
                if (serviceState == null) {
                    return;
                }

                switch (serviceState) {
                    case OFF:
                    case STOPPED:
                        labelWalkersGuideServiceNotRunningWarning.setText(
                                String.format(
                                    "%1$s: %2$s",
                                    context.getResources().getString(R.string.labelWalkersGuideServiceNotRunningWarning),
                                    intent.getStringExtra(WalkersGuideService.EXTRA_SERVICE_MESSAGE))
                                );
                        labelWalkersGuideServiceNotRunningWarning.setVisibility(View.VISIBLE);
                        break;
                    default:
                        labelWalkersGuideServiceNotRunningWarning.setText("");
                        labelWalkersGuideServiceNotRunningWarning.setVisibility(View.GONE);
                        break;
                }

            } else if (intent.getAction().equals(WalkersGuideService.ACTION_SHOW_ROUTE_RECORDING_ONLY_IN_FOREGROUND_MESSAGE)) {
                SimpleMessageDialog.newInstance(
                        getResources().getString(R.string.messageNoteRouteRecordingOnlyInForeground))
                    .show(getSupportFragmentManager(), "SimpleMessageDialog");

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_BEARING_SENSOR_CHANGED)
                    || intent.getAction().equals(DeviceSensorManager.ACTION_SIMULATION_STATUS_CHANGED)
                    || intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_SIMULATION)) {
                updateAccessibilityActionsOnBearingDetailsButton();
            } else if (intent.getAction().equals(PositionManager.ACTION_SIMULATION_STATUS_CHANGED)
                    || intent.getAction().equals(PositionManager.ACTION_NEW_SIMULATED_LOCATION)) {
                updateAccessibilityActionsOnLocationDetailsButton();

            } else if (drawerLayout != null && ! drawerLayout.isOpen()) {
                if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                    updateBearingDetailsButton();
                    ViewCompat.setAccessibilityLiveRegion(
                            buttonBearingDetails, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
                } else if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                    updateLocationDetailsButton();
                    ViewCompat.setAccessibilityLiveRegion(
                            buttonLocationDetails, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
                }
            }
        }
    };


    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        registerBroadcastReceiver();
        Timber.d("onActivityResult: requestCode = %1$d", requestCode);
        switch (requestCode) {

            case SimpleMessageDialog.PERMISSION_REQUEST_APP_SETTINGS:
                if (! WalkersGuideService.isForegroundLocationPermissionGranted()) {
                    SimpleMessageDialog.newInstance(
                            getResources().getString(R.string.messageLocationPermissionDenied))
                        .show(getSupportFragmentManager(), "SimpleMessageDialog");
                } else if (! WalkersGuideService.isPostNotificationsPermissionGranted()) {
                    SimpleMessageDialog.newInstance(
                            getResources().getString(R.string.messagePostNotificationsPermissionDenied))
                        .show(getSupportFragmentManager(), "SimpleMessageDialog");
                } else {
                    skipPositionManagerInitialisationDuringOnResume = false;
                }
                break;
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        registerBroadcastReceiver();
        switch (requestCode) {

            case PERMISSION_REQUEST_FOREGROUND_LOCATION:
                if (! WalkersGuideService.isForegroundLocationPermissionGranted()) {
                    skipPositionManagerInitialisationDuringOnResume = true;
                    SimpleMessageDialog.newInstanceWithAppInfoButton(
                            getResources().getString(R.string.messageLocationPermissionDenied))
                        .show(getSupportFragmentManager(), "SimpleMessageDialog");
                }
                break;

            case PERMISSION_REQUEST_POST_NOTIFICATIONS:
                if (! WalkersGuideService.isPostNotificationsPermissionGranted()) {
                    skipPositionManagerInitialisationDuringOnResume = true;
                    SimpleMessageDialog.newInstanceWithAppInfoButton(
                            getResources().getString(R.string.messagePostNotificationsPermissionDenied))
                        .show(getSupportFragmentManager(), "SimpleMessageDialog");
                }
                break;
        }
    }


    /**
     * tabs
     */

    public enum Tab {

        OVERVIEW(0, GlobalInstance.getStringResource(R.string.mainTabNameOverview)),
        ROUTES(1, GlobalInstance.getStringResource(R.string.mainTabNameRoutes)),
        POINTS(2, GlobalInstance.getStringResource(R.string.mainTabNamePoints));

        public static Tab getTabAtPosition(int position) {
            for (Tab tab : Tab.values()) {
                if (tab.position == position) {
                    return tab;
                }
            }
            return null;
        }

        public int position;
        public String label;

        private Tab(int position, String label) {
            this.position = position;
            this.label = label;
        }

        @Override public String toString() {
            return this.label;
        }
    }

    private Tab getCurrentTab() {
        return settingsManagerInstance.getSelectedTabForMainActivity();
    }

    private void popEverythingButTheTabLayoutFragmentFromBackStack() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            for (int i=1; i<getSupportFragmentManager().getBackStackEntryCount(); i++) {
                getSupportFragmentManager().popBackStack();
            }
            updateToolbarNavigateUpButtonVisibility();
        }
    }

    private void loadFragment(Tab newTab) {
        loadFragment(newTab, null, false);
    }

    private void loadFragment(Tab newTab, Enum<?> newSubTab, boolean openLastPointProfileInPointsTab) {
        Timber.d("loadFragment: %1$s, subTab=%2$s, openLastPointProfileInPointsTab=%3$s", newTab, newSubTab, openLastPointProfileInPointsTab);
        tabLayout.setTag(Pair.create(newSubTab, openLastPointProfileInPointsTab));
        tabLayout.selectTab(
                tabLayout.getTabAt(newTab.position));
    }

    // TabLayout.OnTabSelectedListener

    @Override public void onTabSelected(TabLayout.Tab tabLayoutTab) {
        Tab newTab = Tab.getTabAtPosition(tabLayoutTab.getPosition());
        if (newTab == null) {
            // just a precaution
            newTab = SettingsManager.DEFAULT_SELECTED_TAB_MAIN_ACTIVITY;
        }
        String tag = newTab.name();

        Pair<Enum<?>,Boolean> options = unpackAndClearTabLayoutTag();
        Enum<?> newSubTab = options.first;
        boolean openLastPointProfileInPointsTab = options.second;
        Timber.d("onTabSelected: %1$s / %2$s openLastPointProfileInPointsTab=%3$s", newTab, newSubTab, openLastPointProfileInPointsTab);

        Timber.d("loadFragment before restore, getBackStackEntryCount(): %1$d", getSupportFragmentManager().getBackStackEntryCount());
        getSupportFragmentManager().saveBackStack(getCurrentTab().name());
        getSupportFragmentManager().restoreBackStack(tag);
        getSupportFragmentManager().executePendingTransactions();
        Timber.d("loadFragment after restore, getBackStackEntryCount(): %1$d", getSupportFragmentManager().getBackStackEntryCount());

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        Timber.d("fragment %1$s for tag %2$s", (fragment != null), tag);
        if (fragment == null) {

            // only replace, if the fragment is not already attached
            switch (newTab) {
                case OVERVIEW:
                    fragment = OverviewTabLayoutFragment.newInstance(newSubTab);
                    break;
                case ROUTES:
                    fragment = RoutesTabLayoutFragment.newInstance(newSubTab);
                    break;
                case POINTS:
                    fragment = PointsTabLayoutFragment.newInstance(newSubTab);
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

        } else if (fragment instanceof TabLayoutFragment) {
            if (newSubTab != null) {
                popEverythingButTheTabLayoutFragmentFromBackStack();
                ((TabLayoutFragment) fragment).changeTab(newSubTab);
            }
        }

        updateToolbarNavigateUpButtonVisibility();
        settingsManagerInstance.setSelectedTabForMainActivity(newTab);

        if (newTab == Tab.POINTS && openLastPointProfileInPointsTab) {
            addFragment(
                    PoiListFromServerFragment.newInstance(
                        settingsManagerInstance.getSelectedPoiProfile()));
        }
    }

    @Override public void onTabUnselected(TabLayout.Tab tabLayoutTab) {
        Timber.d("onTabUnselected");
    }

    @Override public void onTabReselected(TabLayout.Tab tabLayoutTab) {
        Tab sameTab = Tab.getTabAtPosition(tabLayoutTab.getPosition());
        Pair<Enum<?>,Boolean> options = unpackAndClearTabLayoutTag();
        Enum<?> newSubTab = options.first;
        boolean openLastPointProfileInPointsTab = options.second;
        Timber.d("onTabReselected: %1$s / %2$s", sameTab, newSubTab);

        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(sameTab.name());
        if (currentFragment instanceof TabLayoutFragment) {
            if (newSubTab != null) {
                popEverythingButTheTabLayoutFragmentFromBackStack();
                ((TabLayoutFragment) currentFragment).changeTab(newSubTab);
                updateToolbarNavigateUpButtonVisibility();
            }
        }

        if (openLastPointProfileInPointsTab && sameTab == Tab.POINTS) {
            addFragment(
                    PoiListFromServerFragment.newInstance(
                        settingsManagerInstance.getSelectedPoiProfile()));
        }
    }

    private Pair<Enum<?>,Boolean> unpackAndClearTabLayoutTag() {
        Pair<Enum<?>,Boolean> options = tabLayout.getTag() != null
            ? (Pair<Enum<?>,Boolean>) tabLayout.getTag()
            : Pair.create(null, false);
        tabLayout.setTag(null);
        return options;
    }


    // MainActivityController

    @Override public void openPlanRouteDialog(boolean startRouteCalculationImmediately) {
        // close, if already open
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment instanceof PlanRouteDialog) {
                    ((PlanRouteDialog) fragment).dismiss();
                }
            }
        }
        // open on top
        PlanRouteDialog.newInstance(startRouteCalculationImmediately)
            .show(getSupportFragmentManager(), "PlanRouteDialog");
    }

    @Override public void closeAllOpenDialogs() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment instanceof DialogFragment) {
                    DialogFragment dialogFragment = (DialogFragment) fragment;
                    if (dialogFragment.getShowsDialog()) {
                        dialogFragment.dismiss();
                    }
                }
            }
        }
    }

    @Override public FragmentManager getFragmentManagerInstance() {
        return getSupportFragmentManager();
    }

    @Override public void addFragment(DialogFragment fragment) {
        if (hasOpenDialog()) {
            Timber.d("addFragment: fragment wants to be a dialog");
            fragment.show(getSupportFragmentManager(), null);
        } else {
            Timber.d("addFragment: want to be embedded");
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainerView, fragment, null)
                .setReorderingAllowed(true)
                .addToBackStack(getCurrentTab().name())
                .commit();
            updateToolbarNavigateUpButtonVisibility();
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
    private static final long VELOCITY_THRESHOLD = 1500;
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
                                    // do nothing
                                } else if (! hasOpenDialog()) {
                                    previousFragment();
                                }
                            } else {
                                // if velocityX is negative, then it's towards left
                                if (drawerLayout != null && drawerLayout.isOpen()) {
                                    drawerLayout.closeDrawers();
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
