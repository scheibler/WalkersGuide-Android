package org.walkersguide.android.ui.dialog.creators;

import org.walkersguide.android.database.profiles.DatabasePointProfile;
import org.walkersguide.android.server.address.AddressManager.AddressRequestListener;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;


import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.server.util.ServerUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.server.address.AddressManager;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.data.basic.point.StreetAddress;
import android.app.Activity;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.GlobalInstance;
import android.widget.EditText;


public class EnterAddressDialog extends DialogFragment implements AddressRequestListener {
    public static final String REQUEST_ENTER_ADDRESS = "enterAddress";
    public static final String EXTRA_STREET_ADDRESS = "streetAddress";


    // instance constructors

    public static EnterAddressDialog newInstance() {
        EnterAddressDialog dialog = new EnterAddressDialog();
        return dialog;
    }

    // dialog
    private AddressManager addressManagerRequest;

    private EditText editAddress;
    private Switch buttonNearbyCurrentLocation;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_enter_address, nullParent);

        editAddress = (EditText) view.findViewById(R.id.editAddress);
        editAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    tryToGetCoordinatesForAddress();
                    return true;
                }
                return false;
            }
        });

        buttonNearbyCurrentLocation = (Switch) view.findViewById(R.id.buttonNearbyCurrentLocation);
        if (savedInstanceState != null) {
            buttonNearbyCurrentLocation.setChecked(
                    savedInstanceState.getBoolean("nearbyCurrentLocationIsChecked"));
        } else {
            buttonNearbyCurrentLocation.setChecked(true);
        }

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.enterAddressDialogName))
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
                    tryToGetCoordinatesForAddress();
                }
            });
            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dialog.dismiss();
                }
            });
            //
            addressManagerRequest = null;
        }
    }

    @Override public void onStop() {
        super.onStop();
        if (addressManagerRequest != null
                && addressManagerRequest.getStatus() != AsyncTask.Status.FINISHED) {
            addressManagerRequest.cancel();
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(
                "nearbyCurrentLocationIsChecked", buttonNearbyCurrentLocation.isChecked());
    }

    private void tryToGetCoordinatesForAddress() {
        String address = editAddress.getText().toString().trim();
        if (address.equals("")) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageAddressMissing),
                    Toast.LENGTH_LONG).show();
        } else {
            hideKeyboard();
            addressManagerRequest = new AddressManager(
                    EnterAddressDialog.this,
                    address,
                    buttonNearbyCurrentLocation.isChecked());
            addressManagerRequest.execute();
        }
    }

    @Override public void requireAddressRequestSuccessful(StreetAddress addressPoint) {
    }

    @Override public void requireCoordinatesRequestSuccessful(ArrayList<StreetAddress> addressPointList) {
        // add to search history
        SettingsManager.getInstance().addToSearchTermHistory(
                editAddress.getText().toString().trim());

        // single result
        //if (addressPointList.size() == 1) {
            AccessDatabase.getInstance().addObjectToDatabaseProfile(
                    addressPointList.get(0), DatabasePointProfile.ADDRESS_POINTS);
            Bundle result = new Bundle();
            result.putSerializable(EXTRA_STREET_ADDRESS, addressPointList.get(0));
            getParentFragmentManager().setFragmentResult(REQUEST_ENTER_ADDRESS, result);
            dismiss();
        //}
    }

    @Override public void addressOrCoordinatesRequestFailed(int returnCode) {
        if (isAdded()) {
            SimpleMessageDialog.newInstance(
                    ServerUtility.getErrorMessageForReturnCode(returnCode))
                .show(getChildFragmentManager(), "SimpleMessageDialog");
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm =(InputMethodManager) GlobalInstance.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editAddress.getWindowToken(), 0);
    }

}
