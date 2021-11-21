    package org.walkersguide.android.ui.dialog.toolbar;

import org.walkersguide.android.ui.dialog.selectors.SelectIntegerDialog;
import org.walkersguide.android.ui.dialog.selectors.SelectIntegerDialog.Token;
import android.os.Build;
import java.lang.System;
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
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;



import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;


public class DirectionDetailsDialog extends DialogFragment implements FragmentResultListener {

    private DirectionManager directionManagerInstance;
    private RadioButton radioCompassDirection;
    private TextView labelCompassDirectionDetails;
    private RadioButton radioGPSDirection;
    private TextView labelGPSDirectionDetails;
    private Switch buttonEnableSimulation;
    private Button buttonSimulatedDirection;

    public static DirectionDetailsDialog newInstance() {
        DirectionDetailsDialog dialog = new DirectionDetailsDialog();
        return dialog;
    }


	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectIntegerDialog.REQUEST_SELECT_INTEGER, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectIntegerDialog.REQUEST_SELECT_INTEGER)) {
            Token token = (Token) bundle.getSerializable(SelectIntegerDialog.EXTRA_TOKEN);
            int newInteger = bundle.getInt(SelectIntegerDialog.EXTRA_INTEGER);
            switch (token) {
                case COMPASS_DIRECTION:
                    directionManagerInstance.setSimulatedDirection(
                            new Direction.Builder(GlobalInstance.getContext(), newInteger).build());
                    updateSimulationDirection();
                    break;
            }
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        directionManagerInstance = DirectionManager.getInstance();

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_direction_details, nullParent);

        // compass
        labelCompassDirectionDetails = (TextView) view.findViewById(R.id.labelCompassDirectionDetails);
        radioCompassDirection = (RadioButton) view.findViewById(R.id.radioCompassDirection);
        radioCompassDirection.setChecked(
                directionManagerInstance.getDirectionSource() == Constants.DIRECTION_SOURCE.COMPASS);
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
        labelGPSDirectionDetails = (TextView) view.findViewById(R.id.labelGPSDirectionDetails);
        radioGPSDirection = (RadioButton) view.findViewById(R.id.radioGPSDirection);
        radioGPSDirection.setChecked(
                directionManagerInstance.getDirectionSource() == Constants.DIRECTION_SOURCE.GPS);
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
        buttonEnableSimulation.setChecked(directionManagerInstance.getSimulationEnabled());
        buttonEnableSimulation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (directionManagerInstance.getSimulationEnabled() != isChecked) {
                    // check or uncheck simulation
                    directionManagerInstance.setSimulationEnabled(isChecked);
                    if (isChecked && directionManagerInstance.getCurrentDirection() == null) {
                        // no simulated direction selected
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.errorNoDirectionFound),
                                Toast.LENGTH_LONG).show();
                        directionManagerInstance.setSimulationEnabled(false);
                        buttonEnableSimulation.setChecked(false);
                    }
                }
            }
        });

        buttonSimulatedDirection = (Button) view.findViewById(R.id.buttonSimulatedDirection);
        buttonSimulatedDirection.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectIntegerDialog.newInstance(
                        SelectIntegerDialog.Token.COMPASS_DIRECTION,
                        (Integer) view.getTag())
                    .show(getChildFragmentManager(), "DirectionDetailsDialog");
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.directionDetailsDialogTitle))
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

    @Override public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_COMPASS_DIRECTION);
        filter.addAction(Constants.ACTION_NEW_GPS_DIRECTION);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // request directions
        directionManagerInstance.requestCompassDirection();
        directionManagerInstance.requestGPSDirection();
        // update simulation direction button
        updateSimulationDirection();
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    private void updateSimulationDirection() {
        Direction simulationDirection = directionManagerInstance.getSimulatedDirection();
        buttonSimulatedDirection.setTag(simulationDirection);
        buttonSimulatedDirection.setText(
                String.format(
                    "%1$s: %2$s",
                    getResources().getString(R.string.directionSourceSimulated),
                    simulationDirection != null
                    ?  simulationDirection.toString()
                    : getResources().getString(R.string.errorNoDirectionFound))
                );
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_COMPASS_DIRECTION)) {
                Direction compassDirection = Direction.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_COMPASS_DIRECTION_OBJECT));
                if (compassDirection != null) {
                    radioCompassDirection.setText(
                            String.format(
                                "%1$s: %2$s",
                                context.getResources().getString(R.string.directionSourceCompass),
                                compassDirection.toString())
                            );
                    labelCompassDirectionDetails.setText(
                            fillDirectionDetailsLabel(
                                context, Constants.DIRECTION_SOURCE.COMPASS, compassDirection));
                } else {
                    radioCompassDirection.setText(
                            context.getResources().getString(R.string.directionSourceCompass));
                    labelCompassDirectionDetails.setText(
                            context.getResources().getString(R.string.errorNoDirectionSelected));
                }

            } else if (intent.getAction().equals(Constants.ACTION_NEW_GPS_DIRECTION)) {
                Direction gpsDirection = Direction.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_GPS_DIRECTION_OBJECT));
                if (gpsDirection != null) {
                    radioGPSDirection.setText(
                            String.format(
                                "%1$s: %2$s",
                                context.getResources().getString(R.string.directionSourceGPS),
                                gpsDirection.toString())
                            );
                    labelGPSDirectionDetails.setText(
                            fillDirectionDetailsLabel(
                                context, Constants.DIRECTION_SOURCE.GPS, gpsDirection));
                } else {
                    radioGPSDirection.setText(
                            context.getResources().getString(R.string.directionSourceGPS));
                    labelGPSDirectionDetails.setText(
                            context.getResources().getString(R.string.errorNoDirectionFound));
                }
            }
        }

        private String fillDirectionDetailsLabel(Context context, int directionSource, Direction direction) {
            StringBuilder stringBuilder = new StringBuilder();
            // append formatted accuracy
            if (direction.getAccuracyRating() != Constants.DIRECTION_ACCURACY_RATING.UNKNOWN) {
                stringBuilder.append(
                        String.format(
                            "%1$s: %2$s",
                            context.getResources().getString(R.string.directionAccuracyLabel),
                            Direction.formatAccuracyRating(context, direction.getAccuracyRating()))
                        );
                if (directionSource == Constants.DIRECTION_SOURCE.COMPASS
                        && direction.getAccuracyRating() == Constants.DIRECTION_ACCURACY_RATING.LOW) {
                    stringBuilder.append(
                            String.format(" (%1$s)", context.getResources().getString(R.string.toolbarCompassCalibrationRequired)));
                        }
            }
            // append direction outdated message
            if (direction.isOutdated()) {
                if (stringBuilder.length() > 0) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        stringBuilder.append(System.lineSeparator());
                    } else {
                        stringBuilder.append("\n");
                    }
                }
                stringBuilder.append(
                        context.getResources().getString(R.string.directionAccuracyOutdated));
            }
            return stringBuilder.toString();
        }
    };

}
