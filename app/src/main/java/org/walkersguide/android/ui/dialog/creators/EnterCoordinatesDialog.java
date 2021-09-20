package org.walkersguide.android.ui.dialog.creators;

import org.walkersguide.android.database.profiles.DatabasePointProfile;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.text.TextUtils;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;



import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.R;
import android.app.Activity;
import org.walkersguide.android.util.GlobalInstance;


public class EnterCoordinatesDialog extends DialogFragment {

    public interface EnterCoordinatesListener {
        public void coordinatesPointCreated(GPS coordinates);
    }


    private EnterCoordinatesListener listener;
    private EditText editLatitude, editLongitude, editName;

    public static EnterCoordinatesDialog newInstance() {
        EnterCoordinatesDialog dialog = new EnterCoordinatesDialog();
        return dialog;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof EnterCoordinatesListener) {
            listener = (EnterCoordinatesListener) getTargetFragment();
        } else if (context instanceof Activity
                && (Activity) context instanceof EnterCoordinatesListener) {
            listener = (EnterCoordinatesListener) context;
        }
    }

    @Override public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_enter_coordinates, nullParent);

        editLatitude = (EditText) view.findViewById(R.id.editLatitude);
        editLongitude = (EditText) view.findViewById(R.id.editLongitude);
        editName = (EditText) view.findViewById(R.id.editName);
        editName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (tryToCreateGpsPoint()) {
                        dismiss();
                    }
                    return true;
                }
                return false;
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
                    if (tryToCreateGpsPoint()) {
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

    private boolean tryToCreateGpsPoint() {
        hideKeyboard();

        // latitude
        double latitude = 1000000.0;
        try {
            latitude = Double.valueOf(editLatitude.getText().toString());
        } catch (NumberFormatException e) {
        } finally {
            if (latitude <= -180.0 || latitude > 180.0) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageLatitudeMissing),
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }

        // longitude
        double longitude = 1000000.0;
        try {
            longitude = Double.valueOf(editLongitude.getText().toString());
        } catch (NumberFormatException e) {
        } finally {
            if (longitude <= -180.0 || longitude > 180.0) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageLongitudeMissing),
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }

        GPS.Builder gpsBuilder = new GPS.Builder(
                latitude, longitude, System.currentTimeMillis());
        // optional name
        String name = editName.getText().toString().trim();
        if (! TextUtils.isEmpty(name)) {
            gpsBuilder.overwriteName(name);
        }

        GPS newLocation = gpsBuilder.build();
        if (newLocation != null) {
            AccessDatabase.getInstance().addObjectToDatabaseProfile(
                    newLocation, DatabasePointProfile.GPS_POINTS);
            if (listener != null) {
                listener.coordinatesPointCreated(newLocation);
            }
            return true;
        }
        return false;
    }

    private void hideKeyboard() {
        InputMethodManager imm =(InputMethodManager) GlobalInstance.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editLatitude.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(editLongitude.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(editName.getWindowToken(), 0);
    }

}
