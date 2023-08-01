    package org.walkersguide.android.ui.dialog.toolbar;

import android.widget.Toast;
import org.walkersguide.android.sensor.bearing.BearingSensor;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.bearing.BearingSensorValue;
import org.walkersguide.android.sensor.DeviceSensorManager;
import android.text.InputFilter;
import android.text.Spanned;
import java.lang.NumberFormatException;
import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.ui.TextChangedListener;
import org.walkersguide.android.ui.UiHelper;

import androidx.appcompat.app.AlertDialog;
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
import android.widget.TextView;



import org.walkersguide.android.R;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.EditText;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import java.util.Locale;
import timber.log.Timber;


public class BearingDetailsDialog extends DialogFragment {


    // instance constructors

    public static BearingDetailsDialog newInstance() {
        BearingDetailsDialog dialog = new BearingDetailsDialog();
        return dialog;
    }


    // dialog
    private DeviceSensorManager deviceSensorManagerInstance;

    private RadioButton radioCompass, radioSatellite;
    private TextView labelCompassDetails, labelSatelliteDetails;
    private SwitchCompat buttonEnableSimulation;
    private EditText editDegree;


	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        deviceSensorManagerInstance = DeviceSensorManager.getInstance();
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_bearing_details, nullParent);

        // compass
        labelCompassDetails = (TextView) view.findViewById(R.id.labelCompassDetails);
        radioCompass = (RadioButton) view.findViewById(R.id.radioCompass);
        radioCompass.setChecked(
                deviceSensorManagerInstance.getSelectedBearingSensor() == BearingSensor.COMPASS);
        radioCompass.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    deviceSensorManagerInstance.setSelectedBearingSensor(BearingSensor.COMPASS);
                    // uncheck gps radio button
                    radioSatellite.setChecked(false);
                }
            }
        });

        // gps
        labelSatelliteDetails = (TextView) view.findViewById(R.id.labelSatelliteDetails);
        radioSatellite = (RadioButton) view.findViewById(R.id.radioSatellite);
        radioSatellite.setChecked(
                deviceSensorManagerInstance.getSelectedBearingSensor() == BearingSensor.SATELLITE);
        radioSatellite.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (deviceSensorManagerInstance.getBearingValueFromSatellite() == null) {
                        // no satellite bearing
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.errorNoBearingFound),
                                Toast.LENGTH_LONG).show();
                        radioCompass.setChecked(true);
                        radioSatellite.setChecked(false);
                    } else {
                        deviceSensorManagerInstance.setSelectedBearingSensor(BearingSensor.SATELLITE);
                        // uncheck compass radio button
                        radioCompass.setChecked(false);
                    }
                }
            }
        });

        // simulated direction

        buttonEnableSimulation = (SwitchCompat) view.findViewById(R.id.buttonEnableSimulation);
        buttonEnableSimulation.setChecked(deviceSensorManagerInstance.getSimulationEnabled());
        buttonEnableSimulation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                Timber.d("onCheckedChanged: %1$s", isChecked);
                if (deviceSensorManagerInstance.getSimulationEnabled() != isChecked) {
                    if (isChecked && deviceSensorManagerInstance.getSimulatedBearing() == null) {
                        editDegree.setText(String.valueOf(0));
                    }
                    deviceSensorManagerInstance.setSimulationEnabled(isChecked);
                }
            }
        });

        Button buttonDecreaseSimulatedBearing = (Button) view.findViewById(R.id.buttonDecreaseSimulatedBearing);
        buttonDecreaseSimulatedBearing.setText(
                String.format(
                    Locale.getDefault(), "-%1$d°", Math.abs(CHANGE_BEARING_INTERVAL)));
        buttonDecreaseSimulatedBearing.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                int newBearingInDegree = getSimulatedBearingInDegree() - CHANGE_BEARING_INTERVAL;
                if (newBearingInDegree < InputFilterForBearingInDegree.MIN) {
                    newBearingInDegree += InputFilterForBearingInDegree.MAX + 1;
                }
                editDegree.setText(String.valueOf(newBearingInDegree));
                buttonEnableSimulation.setChecked(true);
                TTSWrapper
                    .getInstance()
                    .screenReader(
                            (new Bearing(newBearingInDegree)).toString());
            }
        });

        editDegree = (EditText) view.findViewById(R.id.editDegree);
        editDegree.setText(
                String.valueOf(getSimulatedBearingInDegree()));
        editDegree.selectAll();
        editDegree.setFilters(
                new InputFilter[]{ new InputFilterForBearingInDegree() });
        editDegree.addTextChangedListener(new TextChangedListener<EditText>(editDegree) {
            @Override public void onTextChanged(EditText view, Editable s) {
                int newBearingInDegree = 0;
                try {
                    newBearingInDegree = Integer.parseInt(view.getText().toString());
                } catch (NumberFormatException nfe) {}
                deviceSensorManagerInstance.setSimulatedBearing(
                        new Bearing(newBearingInDegree));
            }
        });
        editDegree.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View view) {
                editDegree.setText(String.valueOf(0));
                buttonEnableSimulation.setChecked(true);
                return true;
            }
        });
        editDegree.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (UiHelper.isDoSomeThingEditorAction(actionId, EditorInfo.IME_ACTION_DONE, event)) {
                    buttonEnableSimulation.setChecked(true);
                    UiHelper.hideKeyboard(BearingDetailsDialog.this);
                    return true;
                }
                return false;
            }
        });

        Button buttonIncreaseSimulatedBearing = (Button) view.findViewById(R.id.buttonIncreaseSimulatedBearing);
        buttonIncreaseSimulatedBearing.setText(
                String.format(
                    Locale.getDefault(), "+%1$d°", Math.abs(CHANGE_BEARING_INTERVAL)));
        buttonIncreaseSimulatedBearing.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                int newBearingInDegree = getSimulatedBearingInDegree() + CHANGE_BEARING_INTERVAL;
                if (newBearingInDegree > InputFilterForBearingInDegree.MAX) {
                    newBearingInDegree -= 360;
                }
                editDegree.setText(String.valueOf(newBearingInDegree));
                buttonEnableSimulation.setChecked(true);
                TTSWrapper
                    .getInstance()
                    .screenReader(
                            (new Bearing(newBearingInDegree)).toString());
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.bearingDetailsDialogTitle))
            .setView(view)
            .setNegativeButton(
                    getResources().getString(R.string.dialogClose),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .create();
    }

    // simulated bearing
    private static final int CHANGE_BEARING_INTERVAL = 45;

    private int getSimulatedBearingInDegree() {
        Bearing simulatedBearing = deviceSensorManagerInstance.getSimulatedBearing();
        if (simulatedBearing != null) {
            return simulatedBearing.getDegree();
        }
        return 0;
    }


    /**
     * new bearing broadcasts
     */

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_COMPASS);
        filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_SATELLITE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // request directions
        deviceSensorManagerInstance.requestBearingValueFromCompass();
        deviceSensorManagerInstance.requestBearingValueFromSatellite();
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_COMPASS)) {
                BearingSensorValue bearingValueFromCompass = (BearingSensorValue) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (bearingValueFromCompass != null) {
                    radioCompass.setText(
                            String.format(
                                "%1$s: %2$s",
                                context.getResources().getString(R.string.bearingSensorCompass),
                                bearingValueFromCompass.toString())
                            );
                    labelCompassDetails.setText(
                            bearingValueFromCompass.formatDetails());
                } else {
                    radioCompass.setText(
                            context.getResources().getString(R.string.bearingSensorCompass));
                    labelCompassDetails.setText(
                            context.getResources().getString(R.string.errorNoBearingFound));
                }

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_SATELLITE)) {
                BearingSensorValue bearingValueFromSatellite = (BearingSensorValue) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (bearingValueFromSatellite != null) {
                    radioSatellite.setText(
                            String.format(
                                "%1$s: %2$s",
                                context.getResources().getString(R.string.bearingSensorSatellite),
                                bearingValueFromSatellite.toString())
                            );
                    labelSatelliteDetails.setText(
                            bearingValueFromSatellite.formatDetails());
                } else {
                    radioSatellite.setText(
                            context.getResources().getString(R.string.bearingSensorSatellite));
                    labelSatelliteDetails.setText(
                            context.getResources().getString(R.string.errorNoBearingFound));
                }
            }
        }
    };


    public class InputFilterForBearingInDegree implements InputFilter {
        public static final int MIN = 0;
        public static final int  MAX = 359;

        @Override public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                // Remove the string out of destination that is to be replaced
                String newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend, dest.toString().length());
                // Add the new string in
                newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart, newVal.length());
                int input = Integer.parseInt(newVal);
                if (input >= MIN && input <= MAX) {
                    return null;
                }
            } catch (NumberFormatException nfe) {}
            return "";
        }
    }

}
