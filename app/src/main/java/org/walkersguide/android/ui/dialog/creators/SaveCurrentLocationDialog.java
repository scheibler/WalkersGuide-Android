package org.walkersguide.android.ui.dialog.creators;

import org.walkersguide.android.ui.dialog.toolbar.LocationSensorDetailsDialog;
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

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;



import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;
import android.widget.EditText;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.ui.listener.TextChangedListener;
import android.text.Editable;
import android.text.TextUtils;
import org.walkersguide.android.server.util.ServerUtility;

public class SaveCurrentLocationDialog extends DialogFragment {

    private EditText editName;

    public static SaveCurrentLocationDialog newInstance() {
        SaveCurrentLocationDialog dialog = new SaveCurrentLocationDialog();
        return dialog;
    }


    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_save_current_location, nullParent);

        editName = (EditText) view.findViewById(R.id.editName);
        editName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (tryToSaveCurrentLocation()) {
                        dismiss();
                    }
                    return true;
                }
                return false;
            }
        });

        Button buttonLocationSensorDetails = (Button) view.findViewById(R.id.buttonLocationSensorDetails);
        buttonLocationSensorDetails.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                LocationSensorDetailsDialog.newInstance()
                    .show(getChildFragmentManager(), "LocationSensorDetailsDialog");
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.saveCurrentLocationDialogTitle))
            .setView(view)
            .setPositiveButton(
                getResources().getString(R.string.dialogOK),
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
                    if (tryToSaveCurrentLocation()) {
                        dismiss();
                    }
                }
            });

            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dialog.dismiss();
                }
            });
        }
    }

    private boolean tryToSaveCurrentLocation() {
        // name
        hideKeyboard();
        String name = editName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageNameMissing),
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // current sensor location
        Point currentSensorLocation = PositionManager.getInstance().getGPSLocation();
        if (currentSensorLocation == null) {
            Toast.makeText(
                    getActivity(),
                    ServerUtility.getErrorMessageForReturnCode(Constants.RC.NO_LOCATION_FOUND),
                    Toast.LENGTH_LONG).show();
            return false;
        }

        GPS.Builder gpsBuilder = new GPS.Builder(
                currentSensorLocation.getLatitude(),
                currentSensorLocation.getLongitude(),
                System.currentTimeMillis());
        gpsBuilder.overwriteName(name);
        GPS newLocation = gpsBuilder.build();
        if (newLocation != null) {
            AccessDatabase.getInstance().addObjectToDatabaseProfile(
                    newLocation, DatabasePointProfile.FAVORITES);
            AccessDatabase.getInstance().addObjectToDatabaseProfile(
                    newLocation, DatabasePointProfile.GPS_POINTS);
            return true;
        } else {
            Toast.makeText(
                    getActivity(),
                    ServerUtility.getErrorMessageForReturnCode(Constants.RC.NO_LOCATION_FOUND),
                    Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm =(InputMethodManager) GlobalInstance.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editName.getWindowToken(), 0);
    }

}
