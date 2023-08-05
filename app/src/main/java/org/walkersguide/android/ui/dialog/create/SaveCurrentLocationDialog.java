package org.walkersguide.android.ui.dialog.create;

import org.walkersguide.android.database.profile.FavoritesProfile;
import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.ui.UiHelper;
import org.walkersguide.android.ui.dialog.toolbar.LocationSensorDetailsDialog;
import androidx.appcompat.app.AlertDialog;

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
import org.walkersguide.android.database.DatabaseProfile;
import android.app.Dialog;

public class SaveCurrentLocationDialog extends DialogFragment {
    public static final String REQUEST_SAVE_CURRENT_LOCATION = "saveCurrentLocation";
    public static final String EXTRA_CURRENT_LOCATION = "currentLocation";


    public static SaveCurrentLocationDialog newInstance() {
        SaveCurrentLocationDialog dialog = new SaveCurrentLocationDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_MODE, Mode.RETURN_POINT);
        dialog.setArguments(args);
        return dialog;
    }

    public static SaveCurrentLocationDialog addToFavorites() {
        SaveCurrentLocationDialog dialog = new SaveCurrentLocationDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_MODE, Mode.ADD_TO_FAVORITES);
        dialog.setArguments(args);
        return dialog;
    }

    public static SaveCurrentLocationDialog addToPinnedPoints() {
        SaveCurrentLocationDialog dialog = new SaveCurrentLocationDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_MODE, Mode.ADD_TO_PINNED_POINTS);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_MODE = "mode";

    private enum Mode {
        RETURN_POINT, ADD_TO_FAVORITES, ADD_TO_PINNED_POINTS
    }
    private Mode mode;

    private EditTextAndClearInputButton layoutName;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        mode = (Mode) getArguments().getSerializable(KEY_MODE);

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
        UiHelper.hideKeyboard(SaveCurrentLocationDialog.this);

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
        if (newLocation == null) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.errorNoLocationFound),
                    Toast.LENGTH_LONG).show();

        } else {
            switch (mode) {

                case RETURN_POINT:
                    Bundle result = new Bundle();
                    result.putSerializable(EXTRA_CURRENT_LOCATION, newLocation);
                    getParentFragmentManager().setFragmentResult(REQUEST_SAVE_CURRENT_LOCATION, result);
                    dismiss();
                    break;

                case ADD_TO_FAVORITES:
                    if (newLocation.addToFavorites()) {
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.messageCurrentLocationAddedToFavoritesSuccessful),
                                Toast.LENGTH_LONG).show();
                        dismiss();
                    } else {
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.errorSavePointFailed),
                                Toast.LENGTH_LONG).show();
                    }
                    break;

                case ADD_TO_PINNED_POINTS:
                    if (DatabaseProfile.pinnedPoints().add(newLocation)) {
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.messageCurrentLocationAddedToPinnedPointsSuccessful),
                                Toast.LENGTH_LONG).show();
                        dismiss();
                    } else {
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.errorSavePointFailed),
                                Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
    }

}
