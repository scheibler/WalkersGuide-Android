package org.walkersguide.android.ui.dialog.create;

import org.walkersguide.android.database.profile.FavoritesProfile;
import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.ui.UiHelper;
import org.walkersguide.android.ui.dialog.toolbar.LocationSensorDetailsDialog;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.inputmethod.EditorInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.Toast;



import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import android.text.TextUtils;
import org.json.JSONException;

public class SaveCurrentLocationDialog extends DialogFragment {


    private EditTextAndClearInputButton layoutName;

    public static SaveCurrentLocationDialog newInstance() {
        SaveCurrentLocationDialog dialog = new SaveCurrentLocationDialog();
        return dialog;
    }


    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_save_current_location, nullParent);

        layoutName = (EditTextAndClearInputButton) view.findViewById(R.id.layoutName);
        layoutName.setLabelText(getResources().getString(R.string.labelName));
        layoutName.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        tryToSaveCurrentLocation();
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
                    tryToSaveCurrentLocation();
                }
            });

            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }

    private void tryToSaveCurrentLocation() {
        // name
        String name = layoutName.getInputText();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageNameMissing),
                    Toast.LENGTH_LONG).show();
            return;
        }

        // current sensor location
        GPS currentSensorLocation = PositionManager.getInstance().getGPSLocation();
        if (currentSensorLocation == null) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.errorNoLocationFound),
                    Toast.LENGTH_LONG).show();
            return;
        }

        GPS newLocation = null;
        try {
            GPS.Builder newLocationBuilder = new GPS.Builder(
                    currentSensorLocation.getLatitude(), currentSensorLocation.getLongitude())
                .setName(name);
            if (currentSensorLocation.getAccuracy() != null) {
                newLocationBuilder.setAccuracy(currentSensorLocation.getAccuracy());
            }
            if (currentSensorLocation.getAltitude() != null) {
                newLocationBuilder.setAltitude(currentSensorLocation.getAltitude());
            }
            newLocation = newLocationBuilder.build();
        } catch (JSONException e) {}
        if (newLocation != null && newLocation.addToFavorites()) {
            dismiss();

        } else {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.errorNoLocationFound),
                    Toast.LENGTH_LONG).show();
        }
    }

}
