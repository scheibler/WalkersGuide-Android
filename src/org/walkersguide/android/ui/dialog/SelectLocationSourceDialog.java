package org.walkersguide.android.ui.dialog;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.point.PointWrapper;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class SelectLocationSourceDialog extends DialogFragment {

    private PositionManager positionManagerInstance;
    private RadioButton radioGPSLocation, radioSimulatedLocation;
    private Button buttonSimulatedLocation;
    private TextView labelGPSLatitude, labelGPSLongitude;
    private TextView labelGPSProvider, labelGPSAccuracy;
    private TextView labelGPSAltitude, labelGPSBearing;
    private TextView labelGPSSpeed, labelGPSTime;

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
        radioGPSLocation = (RadioButton) view.findViewById(R.id.radioGPSLocation);
        radioGPSLocation.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // uncheck other related radio buttons
                    radioSimulatedLocation.setChecked(false);
                }
            }
        });

        labelGPSLatitude = (TextView) view.findViewById(R.id.labelGPSLatitude);
        labelGPSLongitude = (TextView) view.findViewById(R.id.labelGPSLongitude);
        labelGPSProvider = (TextView) view.findViewById(R.id.labelGPSProvider);
        labelGPSAccuracy = (TextView) view.findViewById(R.id.labelGPSAccuracy);
        labelGPSAltitude = (TextView) view.findViewById(R.id.labelGPSAltitude);
        labelGPSBearing = (TextView) view.findViewById(R.id.labelGPSBearing);
        labelGPSSpeed = (TextView) view.findViewById(R.id.labelGPSSpeed);
        labelGPSTime = (TextView) view.findViewById(R.id.labelGPSTime);

        // simulated point
        radioSimulatedLocation = (RadioButton) view.findViewById(R.id.radioSimulatedLocation);
        radioSimulatedLocation.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (positionManagerInstance.getSimulatedLocation().equals(PositionManager.getDummyLocation(getActivity()))) {
                        // no simulated point selected
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.labelNoSimulatedPointSelected),
                                Toast.LENGTH_LONG).show();
                        radioSimulatedLocation.setChecked(false);
                        radioGPSLocation.setChecked(true);
                    } else {
                        // uncheck other related radio buttons
                        radioGPSLocation.setChecked(false);
                    }
                }
            }
        });

        buttonSimulatedLocation = (Button) view.findViewById(R.id.buttonSimulatedLocation);
        buttonSimulatedLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                EnterAddressDialog.newInstance(Constants.POINT_PUT_INTO.SIMULATION).show(
                        getActivity().getSupportFragmentManager(), "EnterAddressDialog");
            }
        });

        // select source
        switch (positionManagerInstance.getLocationSource()) {
            case Constants.LOCATION_SOURCE.GPS:
                radioGPSLocation.setChecked(true);
                break;
            case Constants.LOCATION_SOURCE.SIMULATION:
                radioSimulatedLocation.setChecked(true);
                break;
            default:
                break;
        }

        // request gps locations
        positionManagerInstance.requestGPSLocation();
        positionManagerInstance.requestSimulatedLocation();

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectLocationSourceDialogName))
            .setView(view)
            .setPositiveButton(
                getResources().getString(R.string.dialogOK),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (radioGPSLocation.isChecked()) {
                            // set location source to gps
                            positionManagerInstance.setLocationSource(Constants.LOCATION_SOURCE.GPS);
                        } else if (radioSimulatedLocation.isChecked()) {
                            // set location source to simulation
                            positionManagerInstance.setLocationSource(Constants.LOCATION_SOURCE.SIMULATION);
                        }
                        // update ui
                        Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                        dismiss();
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
                                        "%1$s: %2$s, %3$d %4$s",
                                        context.getResources().getString(R.string.labelGPSProvider),
                                        gpsLocation.getProvider(),
                                        gpsLocation.getNumberOfSatellites(),
                                        context.getResources().getString(R.string.unitSatellites))
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
                                        "%1$s: %2$d %3$s",
                                        context.getResources().getString(R.string.labelGPSAccuracy),
                                        Math.round(gpsLocation.getAccuracy()),
                                        context.getResources().getString(R.string.unitMeters))
                                    );
                        }
                        if (gpsLocation.getAltitude() >= 0.0) {
                            labelGPSAltitude.setText(
                                    String.format(
                                        "%1$s: %2$d %3$s",
                                        context.getResources().getString(R.string.labelGPSAltitude),
                                        Math.round(gpsLocation.getAltitude()),
                                        context.getResources().getString(R.string.unitMeters))
                                    );
                        }
                        if (gpsLocation.getBearing() >= 0.0) {
                            labelGPSBearing.setText(
                                    String.format(
                                        "%1$s: %2$d %3$s",
                                        context.getResources().getString(R.string.labelGPSBearing),
                                        Math.round(gpsLocation.getBearing()),
                                        context.getResources().getString(R.string.unitDegree))
                                    );
                        }
                        if (gpsLocation.getSpeed() >= 0.0) {
                            labelGPSSpeed.setText(
                                    String.format(
                                        "%1$s: %2$d %3$s",
                                        context.getResources().getString(R.string.labelGPSSpeed),
                                        Math.round(gpsLocation.getSpeed()),
                                        context.getResources().getString(R.string.unitKMH))
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
                                            formattedTime,
                                            formattedDate)
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

}
