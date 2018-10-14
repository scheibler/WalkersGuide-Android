package org.walkersguide.android.ui.activity;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.Manifest;

import android.os.Bundle;
import android.os.Handler;

import android.provider.Settings;

import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import android.text.format.DateFormat;
import android.text.InputFilter;
import android.text.InputType;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.route.RouteObject;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.ChildDialogCloseListener;
import org.walkersguide.android.listener.ServerStatusListener;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.ui.dialog.RequestAddressDialog;
import org.walkersguide.android.ui.dialog.SaveCurrentPositionDialog;
import org.walkersguide.android.ui.dialog.SelectMapDialog;
import org.walkersguide.android.ui.dialog.SelectPointDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.ui.filter.InputFilterMinMax;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;


public abstract class AbstractActivity extends AppCompatActivity implements ServerStatusListener {

    private static final int ASK_FOR_LOCATION_PERMISSION_ID = 61;

    public GlobalInstance globalInstance;
	public AccessDatabase accessDatabaseInstance;
    public DirectionManager directionManagerInstance;
    public PositionManager positionManagerInstance;
	public SettingsManager settingsManagerInstance;

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
        inflater.inflate(R.menu.menu_toolbar_abstract_activity, menu);
        return true;
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // update next route point
        RouteObject currentRouteObject = accessDatabaseInstance.getCurrentObjectDataOfRoute(
                settingsManagerInstance.getRouteSettings().getSelectedRouteId());
        if (currentRouteObject != null) {
            int distanceFromCurrentLocation = currentRouteObject.getRoutePoint().distanceFromCurrentLocation();
            menu.findItem(R.id.menuItemNextRoutePoint).setTitle(
                    String.format(
                        "%1$s: %2$s, %3$s (%4$s)",
                        getResources().getString(R.string.menuItemNextRoutePoint),
                        getResources().getQuantityString(
                            R.plurals.meter, distanceFromCurrentLocation, distanceFromCurrentLocation),
                        StringUtility.formatRelativeViewingDirection(
                            AbstractActivity.this, currentRouteObject.getRoutePoint().bearingFromCurrentLocation()),
                        currentRouteObject.getRoutePoint().getPoint().getName())
                    );
        } else {
            menu.findItem(R.id.menuItemNextRoutePoint).setTitle(
                    getResources().getString(R.string.errorNoRouteSelected));
        }

        // update direction menu item
        int currentDirection = directionManagerInstance.getCurrentDirection();
        switch (directionManagerInstance.getDirectionSource()) {
            case Constants.DIRECTION_SOURCE.COMPASS:
                if (currentDirection == Constants.DUMMY.DIRECTION) {
                    menu.findItem(R.id.menuItemDirection).setTitle(
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.directionSourceCompass),
                                getResources().getString(R.string.errorNoDirectionFound))
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
                                getResources().getString(R.string.errorNoDirectionFound))
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
                                getResources().getString(R.string.errorNoDirectionFound))
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
                                getResources().getString(R.string.errorNoLocationFound))
                            );
                } else {
                    int roundedAccuracy = Math.round(((GPS) currentLocation.getPoint()).getAccuracy());
                    menu.findItem(R.id.menuItemLocation).setTitle(
                            String.format(
                                "%1$s: %2$s: %3$s",
                                getResources().getString(R.string.locationSourceGPS),
                                getResources().getString(R.string.labelGPSAccuracy),
                                getResources().getQuantityString(
                                    R.plurals.meter, roundedAccuracy, roundedAccuracy))
                            );
                }
                break;
            case Constants.LOCATION_SOURCE.SIMULATION:
                if (currentLocation.equals(PositionManager.getDummyLocation(this))) {
                    menu.findItem(R.id.menuItemLocation).setTitle(
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.locationSourceSimulatedPoint),
                                getResources().getString(R.string.errorNoLocationFound))
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
            case R.id.menuItemNextRoutePoint:
                View menuItemNextRoutePoint = findViewById(R.id.menuItemNextRoutePoint);
                if (menuItemNextRoutePoint != null) {
                    showNextRoutePointPopupMenu(menuItemNextRoutePoint);
                }
                break;
            case R.id.menuItemDirection:
                SelectDirectionSourceDialog.newInstance().show(
                        getSupportFragmentManager(), "SelectDirectionSourceDialog");
                break;
            case R.id.menuItemLocation:
                SelectLocationSourceDialog.newInstance().show(
                        getSupportFragmentManager(), "SelectLocationSourceDialog");
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showNextRoutePointPopupMenu(View menuItemNextRoutePoint) {
        final RouteObject routeObject = accessDatabaseInstance.getCurrentObjectDataOfRoute(
                settingsManagerInstance.getRouteSettings().getSelectedRouteId());
        if (routeObject != null) {
            PopupMenu popupMore = new PopupMenu(AbstractActivity.this, menuItemNextRoutePoint);
            popupMore.inflate(R.menu.menu_menu_item_next_route_point);
            if (routeObject.getRouteSegment().equals(RouteObject.getDummyRouteSegment(AbstractActivity.this))) {
                popupMore.getMenu().findItem(R.id.menuItemShowNextRouteSegmentDetails).setVisible(false);
            }
            popupMore.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menuItemJumpToRouterTab:
                            // show router fragment of main activity
                            settingsManagerInstance.getGeneralSettings().setRecentOpenTab(Constants.MAIN_FRAGMENT.ROUTER);
                            Intent mainActivityIntent = new Intent(AbstractActivity.this, MainActivity.class);
                            mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(mainActivityIntent);
                            return true;
                        case R.id.menuItemShowNextRoutePointDetails:
                            Intent pointDetailsIntent = new Intent(AbstractActivity.this, PointDetailsActivity.class);
                            try {
                                pointDetailsIntent.putExtra(
                                        Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED,
                                        routeObject.getRoutePoint().toJson().toString());
                            } catch (JSONException e) {
                                pointDetailsIntent.putExtra(
                                        Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED,
                                        "");
                            }
                            startActivity(pointDetailsIntent);
                            return true;
                        case R.id.menuItemShowNextRouteSegmentDetails:
                            Intent segmentDetailsIntent = new Intent(AbstractActivity.this, SegmentDetailsActivity.class);
                            try {
                                segmentDetailsIntent.putExtra(
                                        Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED,
                                        routeObject.getRouteSegment().toJson().toString());
                            } catch (JSONException e) {
                                segmentDetailsIntent.putExtra(
                                        Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED,
                                        "");
                            }
                            startActivity(segmentDetailsIntent);
                            return true;
                        default:
                            return false;
                    }
                }
            });
            popupMore.show();
        } else {
            Toast.makeText(
                    AbstractActivity.this,
                    getResources().getString(R.string.errorNoRouteSelected),
                    Toast.LENGTH_LONG).show();
        }
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        ServerStatusManager.getInstance(this).invalidateServerStatusRequest(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        globalInstance.startActivityTransitionTimer();
    }

    @Override public void onResume() {
        super.onResume();
        globalInstance.stopActivityTransitionTimer();
        invalidateOptionsMenu();

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
            // server status request
            ServerStatusManager.getInstance(this).requestServerStatus(
                    (AbstractActivity) this,
                    settingsManagerInstance.getServerSettings().getServerURL());
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

	@Override public void serverStatusRequestFinished(int returnCode, String returnMessage, ServerInstance serverInstance) {
        if (returnCode == Constants.RC.OK) {
            if (settingsManagerInstance.getServerSettings().getSelectedMap() == null) {
                SelectMapDialog.newInstance()
                    .show(getSupportFragmentManager(), "SelectMapDialog");
            }
            Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else {
            SimpleMessageDialog.newInstance(returnMessage)
                .show(getSupportFragmentManager(), "SimpleMessageDialog");
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
                        AbstractActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        ASK_FOR_LOCATION_PERMISSION_ID);
            } else if (
                       (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                        && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD1.ID)
                    || (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)
                        && intent.getIntExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_THRESHOLD_ID, -1) >= DirectionManager.THRESHOLD1.ID)
                    ) {
                invalidateOptionsMenu();
            } else if(intent.getAction().equals(Constants.ACTION_UPDATE_UI)) {
                onPause();
                onResume();
            }
        }
    };


    /**
     * dialogs
     */

    public static class SelectDirectionSourceDialog extends DialogFragment implements ChildDialogCloseListener {

        private DirectionManager directionManagerInstance;
        private RadioButton radioCompassDirection, radioGPSDirection;
        private Switch buttonEnableSimulation;
        private Button buttonSimulatedDirection;

        public static SelectDirectionSourceDialog newInstance() {
            SelectDirectionSourceDialog selectDirectionSourceDialogInstance = new SelectDirectionSourceDialog();
            return selectDirectionSourceDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            directionManagerInstance = DirectionManager.getInstance(context);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_NEW_COMPASS_DIRECTION);
            filter.addAction(Constants.ACTION_NEW_GPS_DIRECTION);
            filter.addAction(Constants.ACTION_NEW_SIMULATED_DIRECTION);
            LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_select_direction_source, nullParent);

            // compass
            radioCompassDirection = (RadioButton) view.findViewById(R.id.radioCompassDirection);
            if (directionManagerInstance.getDirectionSource() == Constants.DIRECTION_SOURCE.SIMULATION) {
                radioCompassDirection.setChecked(
                        directionManagerInstance.getPreviousDirectionSource() == Constants.DIRECTION_SOURCE.COMPASS);
            } else {
                radioCompassDirection.setChecked(
                        directionManagerInstance.getDirectionSource() == Constants.DIRECTION_SOURCE.COMPASS);
            }
            radioCompassDirection.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        // set direction source to compass
                        directionManagerInstance.setDirectionSource(Constants.DIRECTION_SOURCE.COMPASS);
                        // uncheck gps radio button
                        radioGPSDirection.setChecked(false);
                    }
                }
            });

            // gps
            radioGPSDirection = (RadioButton) view.findViewById(R.id.radioGPSDirection);
            if (directionManagerInstance.getDirectionSource() == Constants.DIRECTION_SOURCE.SIMULATION) {
                radioGPSDirection.setChecked(
                        directionManagerInstance.getPreviousDirectionSource() == Constants.DIRECTION_SOURCE.GPS);
            } else {
                radioGPSDirection.setChecked(
                        directionManagerInstance.getDirectionSource() == Constants.DIRECTION_SOURCE.GPS);
            }
            radioGPSDirection.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        // set direction source to gps bearing
                        directionManagerInstance.setDirectionSource(Constants.DIRECTION_SOURCE.GPS);
                        // uncheck compass radio button
                        radioCompassDirection.setChecked(false);
                    }
                }
            });

            // simulated direction
            buttonEnableSimulation = (Switch) view.findViewById(R.id.buttonEnableSimulation);
            buttonEnableSimulation.setChecked(
                    directionManagerInstance.getDirectionSource() == Constants.DIRECTION_SOURCE.SIMULATION);
            buttonEnableSimulation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                    if (isChecked) {
                        // set location source to simulation
                        directionManagerInstance.setDirectionSource(Constants.DIRECTION_SOURCE.SIMULATION);
                    } else {
                        // set location source to compass or gps
                        if (radioCompassDirection.isChecked()) {
                            directionManagerInstance.setDirectionSource(Constants.DIRECTION_SOURCE.COMPASS);
                        } else if (radioGPSDirection.isChecked()) {
                            directionManagerInstance.setDirectionSource(Constants.DIRECTION_SOURCE.GPS);
                        }
                    }
                }
            });

            buttonSimulatedDirection = (Button) view.findViewById(R.id.buttonSimulatedDirection);
            buttonSimulatedDirection.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    EnterDirectionValueDialog enterDirectionValueDialog = EnterDirectionValueDialog.newInstance();
                    enterDirectionValueDialog.setTargetFragment(SelectDirectionSourceDialog.this, 1);
                    enterDirectionValueDialog.show(getActivity().getSupportFragmentManager(), "EnterDirectionValueDialog");
                }
            });

            // request directions
            directionManagerInstance.requestCompassDirection();
            directionManagerInstance.requestGPSDirection();
            directionManagerInstance.requestSimulatedDirection();

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectDirectionSourceDialogName))
                .setView(view)
                .setNegativeButton(
                        getResources().getString(R.string.dialogClose),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // update ui
                                Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                                dismiss();
                            }
                        })
                .create();
        }

        @Override public void childDialogClosed() {
            directionManagerInstance.requestSimulatedDirection();
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        }

        private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.ACTION_NEW_COMPASS_DIRECTION)) {
                    int compassDirection = intent.getIntExtra(Constants.ACTION_NEW_COMPASS_DIRECTION_ATTR.INT_DIRECTION_IN_DEGREE, -1);
                    radioCompassDirection.setText(
                            String.format(
                                context.getResources().getString(R.string.formattedDirectionValue),
                                context.getResources().getString(R.string.directionSourceCompass),
                                compassDirection,
                                StringUtility.formatGeographicDirection(context, compassDirection))
                            );
                } else if (intent.getAction().equals(Constants.ACTION_NEW_GPS_DIRECTION)) {
                    int gpsDirection = intent.getIntExtra(Constants.ACTION_NEW_GPS_DIRECTION_ATTR.INT_DIRECTION_IN_DEGREE, -1);
                    radioGPSDirection.setText(
                            String.format(
                                context.getResources().getString(R.string.formattedDirectionValue),
                                context.getResources().getString(R.string.directionSourceGPS),
                                gpsDirection,
                                StringUtility.formatGeographicDirection(context, gpsDirection))
                            );
                } else if (intent.getAction().equals(Constants.ACTION_NEW_SIMULATED_DIRECTION)) {
                    int simulatedDirection = intent.getIntExtra(Constants.ACTION_NEW_SIMULATED_DIRECTION_ATTR.INT_DIRECTION_IN_DEGREE, 0);
                    buttonSimulatedDirection.setText(
                            String.format(
                                context.getResources().getString(R.string.formattedDirectionValue),
                                context.getResources().getString(R.string.directionSourceSimulated),
                                simulatedDirection,
                                StringUtility.formatGeographicDirection(context, simulatedDirection))
                            );
                }
            }
        };

        public static class EnterDirectionValueDialog extends DialogFragment {

            private ChildDialogCloseListener childDialogCloseListener;
            private DirectionManager directionManagerInstance;
            private InputMethodManager imm;
            private EditText editSimulatedDirection;

            public static EnterDirectionValueDialog newInstance() {
                EnterDirectionValueDialog enterDirectionValueDialog = new EnterDirectionValueDialog();
                return enterDirectionValueDialog;
            }

            @Override public void onAttach(Context context){
                super.onAttach(context);
                if (getTargetFragment() != null
                        && getTargetFragment() instanceof ChildDialogCloseListener) {
                    childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
                }
                directionManagerInstance = DirectionManager.getInstance(context);
                imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                IntentFilter filter = new IntentFilter();
                filter.addAction(Constants.ACTION_NEW_SIMULATED_DIRECTION);
                LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
            }

            @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
                // custom view
                final ViewGroup nullParent = null;
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View view = inflater.inflate(R.layout.layout_single_edit_text, nullParent);

                editSimulatedDirection = (EditText) view.findViewById(R.id.editInput);
                editSimulatedDirection.setHint(getResources().getString(R.string.editHintDirectionInDegree));
                editSimulatedDirection.setImeOptions(EditorInfo.IME_ACTION_DONE);
                editSimulatedDirection.setInputType(InputType.TYPE_CLASS_NUMBER);
                editSimulatedDirection.setFilters(new InputFilter[]{ new InputFilterMinMax("0", "359")});
                editSimulatedDirection.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            applyChanges();
                            return true;
                        }
                        return false;
                    }
                });

                ImageButton buttonDelete = (ImageButton) view.findViewById(R.id.buttonDelete);
                buttonDelete.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        // clear edit text
                        editSimulatedDirection.setText("");
                        // show keyboard
                        imm.showSoftInput(editSimulatedDirection, InputMethodManager.SHOW_IMPLICIT);
                    }
                });

                // request simulation direction
                directionManagerInstance.requestSimulatedDirection();

                // create dialog
                return new AlertDialog.Builder(getActivity())
                    .setTitle(getResources().getString(R.string.enterDirectionValueDialogTitle))
                    .setView(view)
                    .setPositiveButton(
                        getResources().getString(R.string.dialogOK),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                applyChanges();
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogCancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss();
                            }
                        })
                .create();
            }

            @Override public void onStart() {
                super.onStart();
                // show keyboard
                new Handler().postDelayed(
                        new Runnable() {
                            @Override public void run() {
                                imm.showSoftInput(editSimulatedDirection, InputMethodManager.SHOW_IMPLICIT);
                            }
                        }, 50);
            }

            @Override public void onDismiss(final DialogInterface dialog) {
                super.onDismiss(dialog);
                LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
            }

            private void applyChanges() {
                // simulated direction value
                int simulatedDirection = -1;
                try {
                    simulatedDirection = Integer.parseInt(editSimulatedDirection.getText().toString());
                } catch (NumberFormatException nfe) {
                    simulatedDirection = -1;
                } finally {
                    if (simulatedDirection >= 0 && simulatedDirection <= 359) {
                        directionManagerInstance.setSimulatedDirection(simulatedDirection);
                        if (childDialogCloseListener != null) {
                            childDialogCloseListener.childDialogClosed();
                        }
                        dismiss();
                    } else {
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.editHintDirectionInDegree),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }

            private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
                @Override public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(Constants.ACTION_NEW_SIMULATED_DIRECTION)) {
                        int simulatedDirection = intent.getIntExtra(Constants.ACTION_NEW_SIMULATED_DIRECTION_ATTR.INT_DIRECTION_IN_DEGREE, 0);
                        editSimulatedDirection.setText(
                                String.valueOf(simulatedDirection));
                    }
                }
            };
        }
    }


    public static class SelectLocationSourceDialog extends DialogFragment implements ChildDialogCloseListener {

        private PositionManager positionManagerInstance;
        private TextView labelGPSProvider, labelGPSAccuracy, labelGPSTime;
        private Switch buttonEnableSimulation;
        private Button buttonSimulatedLocation;

        public static SelectLocationSourceDialog newInstance() {
            SelectLocationSourceDialog selectLocationSourceDialogInstance = new SelectLocationSourceDialog();
            return selectLocationSourceDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            positionManagerInstance = PositionManager.getInstance(context);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_NEW_GPS_LOCATION);
            filter.addAction(Constants.ACTION_NEW_SIMULATED_LOCATION);
            LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_select_location_source, nullParent);

            // gps
            labelGPSProvider = (TextView) view.findViewById(R.id.labelGPSProvider);
            labelGPSAccuracy = (TextView) view.findViewById(R.id.labelGPSAccuracy);
            labelGPSTime = (TextView) view.findViewById(R.id.labelGPSTime);

            Button buttonGPSAddress = (Button) view.findViewById(R.id.buttonGPSAddress);
            buttonGPSAddress.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    RequestAddressDialog.newInstance().show(
                            getActivity().getSupportFragmentManager(), "RequestAddressDialog");
                }
            });

            Button buttonGPSDetails = (Button) view.findViewById(R.id.buttonGPSDetails);
            buttonGPSDetails.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    GPSDetailsDialog.newInstance().show(
                            getActivity().getSupportFragmentManager(), "GPSDetailsDialog");
                }
            });

            Button buttonGPSSave = (Button) view.findViewById(R.id.buttonGPSSave);
            buttonGPSSave.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    SaveCurrentPositionDialog.newInstance(new TreeSet<Integer>())
                        .show(getActivity().getSupportFragmentManager(), "SaveCurrentPositionDialog");
                }
            });

            // simulated point
            buttonEnableSimulation = (Switch) view.findViewById(R.id.buttonEnableSimulation);
            buttonEnableSimulation.setChecked(
                    positionManagerInstance.getLocationSource() == Constants.LOCATION_SOURCE.SIMULATION);
            buttonEnableSimulation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                    if (isChecked) {
                        if (positionManagerInstance.getSimulatedLocation().equals(PositionManager.getDummyLocation(getActivity()))) {
                            // no simulated point selected
                            Toast.makeText(
                                    getActivity(),
                                    getResources().getString(R.string.labelNoSimulatedPointSelected),
                                    Toast.LENGTH_LONG).show();
                            buttonEnableSimulation.setChecked(false);
                            return;
                        }
                        // set location source to simulation
                        positionManagerInstance.setLocationSource(Constants.LOCATION_SOURCE.SIMULATION);
                    } else {
                        // set location source to gps
                        positionManagerInstance.setLocationSource(Constants.LOCATION_SOURCE.GPS);
                    }
                    // request gps location
                    positionManagerInstance.requestGPSLocation();
                    positionManagerInstance.requestSimulatedLocation();
                }
            });

            buttonSimulatedLocation = (Button) view.findViewById(R.id.buttonSimulatedLocation);
            buttonSimulatedLocation.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    SelectPointDialog selectPointDialog = SelectPointDialog.newInstance(Constants.POINT_PUT_INTO.SIMULATION);
                    selectPointDialog.setTargetFragment(SelectLocationSourceDialog.this, 1);
                    selectPointDialog.show(getActivity().getSupportFragmentManager(), "SelectPointDialog");
                }
            });

            // request gps locations
            positionManagerInstance.requestGPSLocation();
            positionManagerInstance.requestSimulatedLocation();

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectLocationSourceDialogName))
                .setView(view)
                .setNegativeButton(
                        getResources().getString(R.string.dialogClose),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // update ui
                                Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                                dismiss();
                            }
                        })
            .create();
        }

        @Override public void childDialogClosed() {
            // request gps locations again
            positionManagerInstance.requestGPSLocation();
            positionManagerInstance.requestSimulatedLocation();
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        }

        private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.ACTION_NEW_GPS_LOCATION)) {
                    // clear fields
                    labelGPSProvider.setText(context.getResources().getString(R.string.labelGPSProvider));
                    labelGPSAccuracy.setText(context.getResources().getString(R.string.labelGPSAccuracy));
                    labelGPSTime.setText(context.getResources().getString(R.string.labelGPSTime));
                    // new location
                    GPS gpsLocation = null;
                    try {
                        gpsLocation = new GPS(
                                context,
                                new JSONObject(
                                    intent.getStringExtra(Constants.ACTION_NEW_GPS_LOCATION_ATTR.STRING_POINT_SERIALIZED))
                                );
                    } catch (JSONException e) {
                        gpsLocation = null;
                    } finally {
                        if (gpsLocation != null) {
                            if (gpsLocation.getNumberOfSatellites() >= 0) {
                                labelGPSProvider.setText(
                                        String.format(
                                            "%1$s: %2$s, %3$s",
                                            context.getResources().getString(R.string.labelGPSProvider),
                                            gpsLocation.getProvider(),
                                            getResources().getQuantityString(
                                                R.plurals.satellite,
                                                    gpsLocation.getNumberOfSatellites(),
                                                    gpsLocation.getNumberOfSatellites()))
                                        );
                            } else {
                                labelGPSProvider.setText(
                                        String.format(
                                            "%1$s: %2$s",
                                            context.getResources().getString(R.string.labelGPSProvider),
                                            gpsLocation.getProvider())
                                        );
                            }
                            if (gpsLocation.getAccuracy() >= 0.0) {
                                labelGPSAccuracy.setText(
                                        String.format(
                                            "%1$s: %2$s",
                                            context.getResources().getString(R.string.labelGPSAccuracy),
                                            context.getResources().getQuantityString(
                                                R.plurals.meter,
                                                Math.round(gpsLocation.getAccuracy()),
                                                Math.round(gpsLocation.getAccuracy())))
                                        );
                            }
                            if (gpsLocation.getTime() >= 0) {
                                String formattedTime = DateFormat.getTimeFormat(context).format(
                                        new Date(gpsLocation.getTime()));
                                String formattedDate = DateFormat.getDateFormat(context).format(
                                        new Date(gpsLocation.getTime()));
                                if (formattedDate.equals(DateFormat.getDateFormat(context).format(new Date()))) {
                                    labelGPSTime.setText(
                                            String.format(
                                                "%1$s: %2$s",
                                                context.getResources().getString(R.string.labelGPSTime),
                                                formattedTime)
                                            );
                                } else {
                                    labelGPSTime.setText(
                                            String.format(
                                                "%1$s: %2$s, %3$s",
                                                context.getResources().getString(R.string.labelGPSTime),
                                                formattedDate,
                                                formattedTime)
                                            );
                                }
                            }
                        }
                    }

                } else if (intent.getAction().equals(Constants.ACTION_NEW_SIMULATED_LOCATION)) {
                    PointWrapper simulatedLocation = null;
                    try {
                        simulatedLocation = new PointWrapper(
                                context,
                                new JSONObject(
                                    intent.getStringExtra(Constants.ACTION_NEW_GPS_LOCATION_ATTR.STRING_POINT_SERIALIZED))
                                );
                    } catch (JSONException e) {
                        simulatedLocation = null;
                    } finally {
                        if (simulatedLocation.equals(PositionManager.getDummyLocation(getActivity()))) {
                            buttonSimulatedLocation.setText(
                                    context.getResources().getString(R.string.labelNoSimulatedPointSelected));
                        } else {
                            buttonSimulatedLocation.setText(simulatedLocation.toString());
                        }
                    }
                }
            }
        };


        public static class GPSDetailsDialog extends DialogFragment {

            private PositionManager positionManagerInstance;
            private TextView labelGPSLatitude, labelGPSLongitude;
            private TextView labelGPSProvider, labelGPSAccuracy;
            private TextView labelGPSAltitude, labelGPSBearing;
            private TextView labelGPSSpeed, labelGPSTime;

            public static GPSDetailsDialog newInstance() {
                GPSDetailsDialog gpsDetailsDialogInstance = new GPSDetailsDialog();
                return gpsDetailsDialogInstance;
            }

            @Override public void onAttach(Context context){
                super.onAttach(context);
                positionManagerInstance = PositionManager.getInstance(context);
                IntentFilter filter = new IntentFilter();
                filter.addAction(Constants.ACTION_NEW_GPS_LOCATION);
                LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
            }

            @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
                // custom view
                final ViewGroup nullParent = null;
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View view = inflater.inflate(R.layout.dialog_gps_details, nullParent);

                labelGPSLatitude = (TextView) view.findViewById(R.id.labelGPSLatitude);
                labelGPSLongitude = (TextView) view.findViewById(R.id.labelGPSLongitude);
                labelGPSProvider = (TextView) view.findViewById(R.id.labelGPSProvider);
                labelGPSAccuracy = (TextView) view.findViewById(R.id.labelGPSAccuracy);
                labelGPSAltitude = (TextView) view.findViewById(R.id.labelGPSAltitude);
                labelGPSBearing = (TextView) view.findViewById(R.id.labelGPSBearing);
                labelGPSSpeed = (TextView) view.findViewById(R.id.labelGPSSpeed);
                labelGPSTime = (TextView) view.findViewById(R.id.labelGPSTime);

                // request gps location
                positionManagerInstance.requestGPSLocation();

                // create dialog
                return new AlertDialog.Builder(getActivity())
                    .setTitle(getResources().getString(R.string.gpsDetailsDialogName))
                    .setView(view)
                    .setNegativeButton(
                            getResources().getString(R.string.dialogClose),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dismiss();
                                }
                            })
                .create();
            }

            @Override public void onDismiss(final DialogInterface dialog) {
                super.onDismiss(dialog);
                LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
            }

            private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
                @Override public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(Constants.ACTION_NEW_GPS_LOCATION)) {
                        // clear fields
                        labelGPSLatitude.setText(context.getResources().getString(R.string.labelGPSLatitude));
                        labelGPSLongitude.setText(context.getResources().getString(R.string.labelGPSLongitude));
                        labelGPSProvider.setText(context.getResources().getString(R.string.labelGPSProvider));
                        labelGPSAccuracy.setText(context.getResources().getString(R.string.labelGPSAccuracy));
                        labelGPSAltitude.setText(context.getResources().getString(R.string.labelGPSAltitude));
                        labelGPSBearing.setText(context.getResources().getString(R.string.labelGPSBearing));
                        labelGPSSpeed.setText(context.getResources().getString(R.string.labelGPSSpeed));
                        labelGPSTime.setText(context.getResources().getString(R.string.labelGPSTime));
                        // new location
                        GPS gpsLocation = null;
                        try {
                            gpsLocation = new GPS(
                                    context,
                                    new JSONObject(
                                        intent.getStringExtra(Constants.ACTION_NEW_GPS_LOCATION_ATTR.STRING_POINT_SERIALIZED))
                                    );
                        } catch (JSONException e) {
                            gpsLocation = null;
                        } finally {
                            if (gpsLocation != null) {
                                labelGPSLatitude.setText(
                                        String.format(
                                            "%1$s: %2$f",
                                            context.getResources().getString(R.string.labelGPSLatitude),
                                            gpsLocation.getLatitude())
                                        );
                                labelGPSLongitude.setText(
                                        String.format(
                                            "%1$s: %2$f",
                                            context.getResources().getString(R.string.labelGPSLongitude),
                                            gpsLocation.getLongitude())
                                        );
                                if (gpsLocation.getNumberOfSatellites() >= 0) {
                                    labelGPSProvider.setText(
                                            String.format(
                                                "%1$s: %2$s, %3$s",
                                                context.getResources().getString(R.string.labelGPSProvider),
                                                gpsLocation.getProvider(),
                                                getResources().getQuantityString(
                                                    R.plurals.satellite,
                                                    gpsLocation.getNumberOfSatellites(),
                                                    gpsLocation.getNumberOfSatellites()))
                                            );
                                } else {
                                    labelGPSProvider.setText(
                                            String.format(
                                                "%1$s: %2$s",
                                                context.getResources().getString(R.string.labelGPSProvider),
                                                gpsLocation.getProvider())
                                            );
                                }
                                if (gpsLocation.getAccuracy() >= 0.0) {
                                    labelGPSAccuracy.setText(
                                            String.format(
                                                "%1$s: %2$s",
                                                context.getResources().getString(R.string.labelGPSAccuracy),
                                                context.getResources().getQuantityString(
                                                    R.plurals.meter,
                                                    Math.round(gpsLocation.getAccuracy()),
                                                    Math.round(gpsLocation.getAccuracy())))
                                            );
                                }
                                if (gpsLocation.getAltitude() >= 0.0) {
                                    labelGPSAltitude.setText(
                                            String.format(
                                                "%1$s: %2$s",
                                                context.getResources().getString(R.string.labelGPSAltitude),
                                                context.getResources().getQuantityString(
                                                    R.plurals.meter,
                                                    (int) Math.round(gpsLocation.getAltitude()),
                                                    (int) Math.round(gpsLocation.getAltitude())))
                                            );
                                }
                                if (gpsLocation.getBearing() >= 0.0) {
                                    labelGPSBearing.setText(
                                            String.format(
                                                "%1$s: %2$d°",
                                                context.getResources().getString(R.string.labelGPSBearing),
                                                Math.round(gpsLocation.getBearing()))
                                            );
                                }
                                if (gpsLocation.getSpeed() >= 0.0) {
                                    labelGPSSpeed.setText(
                                            String.format(
                                                "%1$s: %2$d km/h",
                                                context.getResources().getString(R.string.labelGPSSpeed),
                                                Math.round(gpsLocation.getSpeed() * 3.6))
                                            );
                                }
                                if (gpsLocation.getTime() >= 0) {
                                    String formattedTime = DateFormat.getTimeFormat(context).format(
                                            new Date(gpsLocation.getTime()));
                                    String formattedDate = DateFormat.getDateFormat(context).format(
                                            new Date(gpsLocation.getTime()));
                                    if (formattedDate.equals(DateFormat.getDateFormat(context).format(new Date()))) {
                                        labelGPSTime.setText(
                                                String.format(
                                                    "%1$s: %2$s",
                                                    context.getResources().getString(R.string.labelGPSTime),
                                                    formattedTime)
                                                );
                                    } else {
                                        labelGPSTime.setText(
                                                String.format(
                                                    "%1$s: %2$s %3$s",
                                                    context.getResources().getString(R.string.labelGPSTime),
                                                    formattedDate,
                                                    formattedTime)
                                                );
                                    }
                                }
                            }
                        }
                    }
                }
            };
        }
    }

}
