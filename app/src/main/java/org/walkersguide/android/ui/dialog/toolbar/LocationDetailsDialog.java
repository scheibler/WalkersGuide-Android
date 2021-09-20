    package org.walkersguide.android.ui.dialog.toolbar;

    import org.walkersguide.android.ui.view.TextViewAndActionButton;
    import org.walkersguide.android.ui.view.TextViewAndActionButton.LabelTextConfig;
import org.walkersguide.android.ui.dialog.LocationSensorDetailsDialog;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;


import android.os.Bundle;


import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;



import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.dialog.selectors.SelectRouteOrSimulationPointDialog;
import org.walkersguide.android.ui.dialog.selectors.SelectRouteOrSimulationPointDialog.SelectRouteOrSimulationPointListener;
import org.walkersguide.android.ui.dialog.selectors.SelectRouteOrSimulationPointDialog.WhereToPut;
import org.walkersguide.android.ui.dialog.WhereAmIDialog;
import org.walkersguide.android.ui.dialog.creators.SaveCurrentLocationDialog;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.data.basic.point.Point;


public class LocationDetailsDialog extends DialogFragment implements SelectRouteOrSimulationPointListener {

    private PositionManager positionManagerInstance;

    private TextView labelGPSCoordinates, labelGPSAccuracy, labelGPSTime;
    private Switch buttonEnableSimulation;
    private TextViewAndActionButton layoutSimulationPoint;

    public static LocationDetailsDialog newInstance() {
        LocationDetailsDialog dialog = new LocationDetailsDialog();
        return dialog;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        positionManagerInstance = PositionManager.getInstance();

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_location_details, nullParent);

        // top layout
        Button buttonWhereAmI = (Button) view.findViewById(R.id.buttonWhereAmI);
        buttonWhereAmI.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WhereAmIDialog.newInstance()
                    .show(getActivity().getSupportFragmentManager(), "WhereAmIDialog");
            }
        });
        Button buttonSaveCurrentLocation = (Button) view.findViewById(R.id.buttonSaveCurrentLocation);
        buttonSaveCurrentLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SaveCurrentLocationDialog.newInstance()
                    .show(getActivity().getSupportFragmentManager(), "SaveCurrentLocationDialog");
            }
        });

        // location sensor details
        labelGPSCoordinates = (TextView) view.findViewById(R.id.labelGPSCoordinates);
        labelGPSAccuracy = (TextView) view.findViewById(R.id.labelGPSAccuracy);
        labelGPSTime = (TextView) view.findViewById(R.id.labelGPSTime);
        Button buttonLocationSensorDetails = (Button) view.findViewById(R.id.buttonLocationSensorDetails);
        buttonLocationSensorDetails.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                LocationSensorDetailsDialog.newInstance()
                    .show(getActivity().getSupportFragmentManager(), "LocationSensorDetailsDialog");
            }
        });

        // simulated point
        buttonEnableSimulation = (Switch) view.findViewById(R.id.buttonEnableSimulation);
        buttonEnableSimulation.setChecked(positionManagerInstance.getSimulationEnabled());
        buttonEnableSimulation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (positionManagerInstance.getSimulationEnabled() != isChecked) {
                    // check or uncheck simulation
                    positionManagerInstance.setSimulationEnabled(isChecked);
                    if (isChecked && positionManagerInstance.getCurrentLocation() == null) {
                        // no simulated point selected
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.labelNoPointSelected),
                                Toast.LENGTH_LONG).show();
                        positionManagerInstance.setSimulationEnabled(false);
                        buttonEnableSimulation.setChecked(false);
                    }
                }
            }
        });

        layoutSimulationPoint = (TextViewAndActionButton) view.findViewById(R.id.layoutSimulationPoint);
        layoutSimulationPoint.setOnLabelClickListener(new TextViewAndActionButton.OnLabelClickListener() {
            @Override public void onLabelClick(TextViewAndActionButton view) {
                SelectRouteOrSimulationPointDialog dialog = SelectRouteOrSimulationPointDialog.newInstance(
                        SelectRouteOrSimulationPointDialog.WhereToPut.SIMULATION_POINT);
                dialog.setTargetFragment(LocationDetailsDialog.this, 1);
                dialog.show(getActivity().getSupportFragmentManager(), "SelectRouteOrSimulationPointDialog");
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.locationDetailsDialogTitle))
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

    @Override public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_GPS_LOCATION);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // request location
        positionManagerInstance.requestGPSLocation();
        // update simulation point button
        updateSimulationPoint();
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void routeOrSimulationPointSelected(Point point, WhereToPut whereToPut) {
        if (whereToPut == SelectRouteOrSimulationPointDialog.WhereToPut.SIMULATION_POINT) {
            positionManagerInstance.setSimulatedLocation(point);
            updateSimulationPoint();
            // add to history
            if (point != null) {
                AccessDatabase.getInstance().addObjectToDatabaseProfile(
                        point, DatabasePointProfile.SIMULATED_POINTS);
            }
        }
    }

    private void updateSimulationPoint() {
        layoutSimulationPoint.configureView(
                positionManagerInstance.getSimulatedLocation(),
                LabelTextConfig.simulation(false));
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_GPS_LOCATION)) {
                // clear fields
                labelGPSCoordinates.setText(context.getResources().getString(R.string.labelGPSCoordinates));
                labelGPSAccuracy.setText(context.getResources().getString(R.string.labelGPSAccuracy));
                labelGPSTime.setText(context.getResources().getString(R.string.labelGPSTime));

                // get gps location
                PointWrapper pointWrapper = PointWrapper.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_GPS_LOCATION_OBJECT));
                if (pointWrapper  != null
                        && pointWrapper.getPoint() instanceof GPS) {
                    GPS gpsLocation = (GPS) pointWrapper.getPoint();

                    // fill labels
                    labelGPSCoordinates.setText(gpsLocation.formatCoordinates());
                    labelGPSAccuracy.setText(gpsLocation.formatAccuracyInMeters());
                    labelGPSTime.setText(gpsLocation.formatTimestamp());
                }
            }
        }
    };

}
