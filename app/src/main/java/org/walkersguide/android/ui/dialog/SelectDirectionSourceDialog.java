package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.R;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.sensor.DirectionManager;
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
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Toast;

public class SelectDirectionSourceDialog extends DialogFragment {

    private InputMethodManager imm;
    private DirectionManager directionManagerInstance;
    private RadioButton radioCompassDirection, radioGPSDirection, radioSimulatedDirection;
    private EditText editSimulatedDirection;

    public static SelectDirectionSourceDialog newInstance() {
        SelectDirectionSourceDialog selectDirectionSourceDialogInstance = new SelectDirectionSourceDialog();
        return selectDirectionSourceDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
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
        radioCompassDirection.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // uncheck other related radio buttons
                    radioGPSDirection.setChecked(false);
                    radioSimulatedDirection.setChecked(false);
                }
            }
        });

        // gps
        radioGPSDirection = (RadioButton) view.findViewById(R.id.radioGPSDirection);
        radioGPSDirection.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // uncheck other related radio buttons
                    radioCompassDirection.setChecked(false);
                    radioSimulatedDirection.setChecked(false);
                }
            }
        });

        // simulated
        radioSimulatedDirection = (RadioButton) view.findViewById(R.id.radioSimulatedDirection);
        radioSimulatedDirection.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // uncheck other related radio buttons
                    radioCompassDirection.setChecked(false);
                    radioGPSDirection.setChecked(false);
                }
            }
        });

        editSimulatedDirection = (EditText) view.findViewById(R.id.editInput);
        editSimulatedDirection.setHint(getResources().getString(R.string.editHintDirectionInDegree));
        editSimulatedDirection.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editSimulatedDirection.setInputType(InputType.TYPE_CLASS_NUMBER);
        editSimulatedDirection.setFilters(new InputFilter[]{ new InputFilterMinMax("0", "359")});

        ImageButton buttonDelete = (ImageButton) view.findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // clear edit text
                editSimulatedDirection.setText("");
                // show keyboard
                imm.showSoftInput(editSimulatedDirection, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // select source
        switch (directionManagerInstance.getDirectionSource()) {
            case Constants.DIRECTION_SOURCE.COMPASS:
                radioCompassDirection.setChecked(true);
                break;
            case Constants.DIRECTION_SOURCE.GPS:
                radioGPSDirection.setChecked(true);
                break;
            case Constants.DIRECTION_SOURCE.SIMULATION:
                radioSimulatedDirection.setChecked(true);
                break;
            default:
                break;
        }

        // request directions
        directionManagerInstance.requestCompassDirection();
        directionManagerInstance.requestGPSDirection();
        directionManagerInstance.requestSimulatedDirection();

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectDirectionSourceDialogName))
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
            } else {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.editHintDirectionInDegree),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        // set direction source
        if (radioCompassDirection.isChecked()) {
            directionManagerInstance.setDirectionSource(Constants.DIRECTION_SOURCE.COMPASS);
        } else if (radioGPSDirection.isChecked()) {
            directionManagerInstance.setDirectionSource(Constants.DIRECTION_SOURCE.GPS);
        } else if (radioSimulatedDirection.isChecked()) {
            directionManagerInstance.setDirectionSource(Constants.DIRECTION_SOURCE.SIMULATION);
        }

        // update ui
        Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        dismiss();
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
                radioSimulatedDirection.setText(
                        String.format(
                            context.getResources().getString(R.string.formattedDirectionValue),
                            context.getResources().getString(R.string.directionSourceSimulated),
                            simulatedDirection,
                            StringUtility.formatGeographicDirection(context, simulatedDirection))
                        );
                editSimulatedDirection.setText(String.valueOf(simulatedDirection));
            }
        }
    };


    private class InputFilterMinMax implements InputFilter {

        private int min, max;

        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public InputFilterMinMax(String min, String max) {
            this.min = Integer.parseInt(min);
            this.max = Integer.parseInt(max);
        }

        @Override public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                // Remove the string out of destination that is to be replaced
                String newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend, dest.toString().length());
                // Add the new string in
                newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart, newVal.length());
                int input = Integer.parseInt(newVal);
                if (isInRange(min, max, input)) {
                    return null;
                }
            } catch (NumberFormatException nfe) {}
            return "";
        }

        private boolean isInRange(int a, int b, int c) {
            return b > a ? c >= a && c <= b : c >= b && c <= a;
        }
    }

}
