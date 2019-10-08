package org.walkersguide.android.ui.dialog;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.AsyncTask;
import android.os.Bundle;

import android.support.v4.app.DialogFragment;


import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.helper.PointUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.AddressManager;
import org.walkersguide.android.server.AddressManager.AddressListener;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog.ChildDialogCloseListener;
import org.walkersguide.android.util.Constants;
import android.widget.RadioButton;
import android.text.InputType;
import android.widget.CompoundButton;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.point.StreetAddress;
import android.text.TextUtils;




public class GetCurrentPositionDialog extends DialogFragment implements AddressListener {

    private ChildDialogCloseListener childDialogCloseListener;
    private AddressManager addressManagerRequest;
    private InputMethodManager imm;
    private PositionManager positionManagerInstance;

    private PointWrapper currentLocation, addressPoint;
    private boolean gpsCoordinatesSelected;
    private int pointPutInto;

    private RadioButton radioGPSCoordinates;
    private TextView labelGPSCoordinatesDetails;
    private EditText editGPSCoordinatesName;
    private ImageButton buttonDelete;
    private RadioButton radioNearestAddress;
    private TextView labelNearestAddress;
    private TextView labelErrorMessage;

    public static GetCurrentPositionDialog newInstance(int pointPutInto) {
        GetCurrentPositionDialog getCurrentPositionDialogInstance= new GetCurrentPositionDialog();
        Bundle args = new Bundle();
        args.putInt("pointPutInto", pointPutInto);
        getCurrentPositionDialogInstance.setArguments(args);
        return getCurrentPositionDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof ChildDialogCloseListener) {
            childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
        }
        addressManagerRequest = null;
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        positionManagerInstance = PositionManager.getInstance(context);
        currentLocation = null;
        addressPoint = null;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        pointPutInto = getArguments().getInt("pointPutInto");
        if (savedInstanceState != null) {
            gpsCoordinatesSelected = savedInstanceState.getBoolean("gpsCoordinatesSelected");
        } else {
            gpsCoordinatesSelected = true;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_get_current_position, nullParent);

        labelGPSCoordinatesDetails = (TextView) view.findViewById(R.id.labelGPSCoordinatesDetails);
        labelNearestAddress = (TextView) view.findViewById(R.id.labelNearestAddress);
        labelErrorMessage = (TextView) view.findViewById(R.id.labelErrorMessage);

        radioGPSCoordinates = (RadioButton) view.findViewById(R.id.radioGPSCoordinates);
        setLabelForRadioGPSCoordinates();
        radioGPSCoordinates.setChecked(gpsCoordinatesSelected);
        radioGPSCoordinates.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    gpsCoordinatesSelected = true;
                    radioNearestAddress.setChecked(false);
                }
            }
        });
        radioNearestAddress = (RadioButton) view.findViewById(R.id.radioNearestAddress);
        radioNearestAddress.setChecked(! gpsCoordinatesSelected);
        radioNearestAddress.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    gpsCoordinatesSelected = false;
                    radioGPSCoordinates.setChecked(false);
                }
            }
        });

        editGPSCoordinatesName = (EditText) view.findViewById(R.id.editInput);
        editGPSCoordinatesName.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editGPSCoordinatesName.setInputType(InputType.TYPE_CLASS_TEXT);
        editGPSCoordinatesName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    tryToSelectCurrentPosition();
                    return true;
                }
                return false;
            }
        });
        buttonDelete = (ImageButton) view.findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // clear edit text
                editGPSCoordinatesName.setText("");
                // show keyboard
                imm.showSoftInput(editGPSCoordinatesName, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.pointSelectFromCurrentLocation))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNeutralButton(
                    getResources().getString(R.string.dialogRefresh),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    tryToSelectCurrentPosition();
                }
            });
            // neutral button: update
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    refreshCurrentLocation();
                }
            });
            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });
            refreshCurrentLocation();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)      // api 17
    private void setLabelForRadioGPSCoordinates() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            radioGPSCoordinates.setLabelFor(R.id.editInput);
        }
    }

    private void refreshCurrentLocation() {
        // hide ui elements
        radioGPSCoordinates.setVisibility(View.GONE);
        labelGPSCoordinatesDetails.setVisibility(View.GONE);
        editGPSCoordinatesName.setVisibility(View.GONE);
        buttonDelete.setVisibility(View.GONE);
        radioNearestAddress.setVisibility(View.GONE);
        labelNearestAddress.setVisibility(View.GONE);
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.GONE);
        }

        // request address for current position
        currentLocation = positionManagerInstance.getCurrentLocation();
        if (currentLocation == null) {
            labelErrorMessage.setText(
                    String.format(
                        getResources().getString(R.string.messageAddressRequestFailed),
                        getResources().getString(R.string.errorNoLocationFound))
                    );

        } else {
            if (positionManagerInstance.getSimulationEnabled()) {
                labelGPSCoordinatesDetails.setText(
                        String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.locationSourceSimulatedPoint),
                            currentLocation.getPoint().getName())
                        );
            } else if (currentLocation.getPoint() instanceof GPS) {
                labelGPSCoordinatesDetails.setText(
                        ((GPS) currentLocation.getPoint()).getShortStatusMessage());
            } else {
                labelGPSCoordinatesDetails.setText(
                        currentLocation.getPoint().getName());
            }
            editGPSCoordinatesName.setText(currentLocation.getPoint().getName());
            labelErrorMessage.setText(
                    getResources().getString(R.string.messagePleaseWait));
            addressManagerRequest = new AddressManager(
                    getActivity(),
                    GetCurrentPositionDialog.this,
                    currentLocation.getPoint().getLatitude(),
                    currentLocation.getPoint().getLongitude());
            addressManagerRequest.execute();
        }

        labelErrorMessage.setVisibility(View.VISIBLE);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("gpsCoordinatesSelected", gpsCoordinatesSelected);
    }

    @Override public void addressRequestFinished(Context context, int returnCode, ArrayList<PointWrapper> addressPointList) {
        labelErrorMessage.setVisibility(View.GONE);
        labelGPSCoordinatesDetails.setVisibility(View.VISIBLE);
        if (! positionManagerInstance.getSimulationEnabled()) {
            editGPSCoordinatesName.setVisibility(View.VISIBLE);
            buttonDelete.setVisibility(View.VISIBLE);
        }
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
        }

        // address found
        if (returnCode == Constants.RC.OK
                && addressPointList != null) {
            addressPoint = addressPointList.get(0);
            radioGPSCoordinates.setVisibility(View.VISIBLE);
            if (addressPoint.getPoint() instanceof StreetAddress) {
                editGPSCoordinatesName.setText(
                        String.format(
                            getResources().getString(R.string.editGPSCoordinatesNearestAddress),
                            ((StreetAddress) addressPoint.getPoint()).formatAddressShortLength())
                        );
            } else {
                editGPSCoordinatesName.setText(
                        String.format(
                            getResources().getString(R.string.editGPSCoordinatesNearestAddress),
                            addressPoint.getPoint().getName())
                        );
            }
            radioNearestAddress.setVisibility(View.VISIBLE);
            labelNearestAddress.setText(addressPoint.toString());
            labelNearestAddress.setVisibility(View.VISIBLE);
        }
    }

    private void tryToSelectCurrentPosition() {
        if (addressPoint != null && ! gpsCoordinatesSelected) {
            PointUtility.putNewPoint(
                    getActivity(), addressPoint, pointPutInto);
        } else {
            String gpsCoordinatesName = editGPSCoordinatesName.getText().toString().trim();
            if (! TextUtils.isEmpty(gpsCoordinatesName)) {
                // replace name
                try {
                    JSONObject jsonCurrentLocation = currentLocation.toJson();
                    jsonCurrentLocation.put("name", gpsCoordinatesName);
                    currentLocation = new PointWrapper(getActivity(), jsonCurrentLocation);
                } catch (JSONException e) {}
            }
            PointUtility.putNewPoint(
                    getActivity(), currentLocation, pointPutInto);
        }
        if (childDialogCloseListener != null) {
            childDialogCloseListener.childDialogClosed();
        }
        dismiss();
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        childDialogCloseListener = null;
        if (addressManagerRequest != null
                && addressManagerRequest.getStatus() != AsyncTask.Status.FINISHED) {
            addressManagerRequest.cancel();
        }
    }

}
