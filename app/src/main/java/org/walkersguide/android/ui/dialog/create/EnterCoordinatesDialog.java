package org.walkersguide.android.ui.dialog.create;

import org.walkersguide.android.database.profile.static_profile.HistoryProfile;
import org.walkersguide.android.ui.UiHelper;

import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
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
import org.walkersguide.android.R;
import android.text.InputType;
import org.json.JSONException;
import android.text.TextUtils;


public class EnterCoordinatesDialog extends DialogFragment {
    public static final String REQUEST_ENTER_COORDINATES = "enterCoordinates";
    public static final String EXTRA_COORDINATES = "coordinates";


    // instance constructors

    public static EnterCoordinatesDialog newInstance() {
        EnterCoordinatesDialog dialog = new EnterCoordinatesDialog();
        return dialog;
    }


    // dialog
    private EditTextAndClearInputButton layoutLatitude, layoutLongitude;
    private EditTextAndClearInputButton layoutOptionalName;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_enter_coordinates, nullParent);

        layoutLatitude = (EditTextAndClearInputButton) view.findViewById(R.id.layoutLatitude);
        layoutLatitude.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        layoutLatitude.setLabelText(
                getResources().getString(R.string.labelGPSLatitude));
        layoutLatitude.setEditorAction(
                EditorInfo.IME_ACTION_NEXT,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        layoutLongitude.requestFocus();
                    }
                });

        layoutLongitude = (EditTextAndClearInputButton) view.findViewById(R.id.layoutLongitude);
        layoutLongitude.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        layoutLongitude.setLabelText(
                getResources().getString(R.string.labelGPSLongitude));
        layoutLongitude.setEditorAction(
                EditorInfo.IME_ACTION_NEXT,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        layoutOptionalName.requestFocus();
                    }
                });

        layoutOptionalName = (EditTextAndClearInputButton) view.findViewById(R.id.layoutOptionalName);
        layoutOptionalName.setLabelText(
                getResources().getString(R.string.labelOptionalName));
        layoutOptionalName.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        tryToCreateGpsPoint();
                    }
                });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.enterCoordinatesDialogName))
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
                    tryToCreateGpsPoint();
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

    private void tryToCreateGpsPoint() {
        Double latitude = null;
        try {
            latitude = Double.valueOf(layoutLatitude.getInputText());
        } catch (NumberFormatException e) {
            latitude = null;
        } finally {
            if (latitude == null
                    || latitude <= -180.0
                    || latitude > 180.0) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageLatitudeMissing),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        Double longitude = null;
        try {
            longitude = Double.valueOf(layoutLongitude.getInputText());
        } catch (NumberFormatException e) {
            longitude = null;
        } finally {
            if (longitude == null
                    || longitude <= -180.0
                    || longitude > 180.0) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageLongitudeMissing),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        GPS newLocation = null;
        try {
            newLocation = new GPS.Builder(latitude, longitude).build();
        } catch (JSONException e) {}
        if (newLocation != null
                && HistoryProfile.allPoints().add(newLocation)) {

            // rename if a name was given
            String newName = layoutOptionalName.getInputText();
            if (! TextUtils.isEmpty(newName)) {
                newLocation.rename(newName);
            }

            // push results and dismiss dialog
            Bundle result = new Bundle();
            result.putSerializable(EXTRA_COORDINATES, newLocation);
            getParentFragmentManager().setFragmentResult(REQUEST_ENTER_COORDINATES, result);
            dismiss();
        }
    }

}
