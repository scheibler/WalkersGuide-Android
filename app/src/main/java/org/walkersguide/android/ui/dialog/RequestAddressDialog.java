package org.walkersguide.android.ui.dialog;

import org.json.JSONException;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.FavoritesProfile;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.server.AddressManager;
import org.walkersguide.android.listener.AddressListener;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.TTSWrapper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import org.walkersguide.android.helper.StringUtility;
import android.os.Build;
import android.annotation.TargetApi;
import android.support.v4.view.ViewCompat;

public class RequestAddressDialog extends DialogFragment implements AddressListener {

    // Store instance variables
    private AccessDatabase accessDatabaseInstance;
    private PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;
    private TTSWrapper ttsWrapperInstance;
    private AddressManager addressManagerRequest;
    private boolean manualRequest;

    // ui components
    private TextView labelAddress;

    public static RequestAddressDialog newInstance() {
        RequestAddressDialog requestAddressDialogInstance = new RequestAddressDialog();
        return requestAddressDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        accessDatabaseInstance = AccessDatabase.getInstance(context);
        positionManagerInstance = PositionManager.getInstance(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
        ttsWrapperInstance = TTSWrapper.getInstance(context);
        addressManagerRequest = null;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        manualRequest = false;
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_single_text_view, nullParent);

        labelAddress = (TextView) view.findViewById(R.id.label);
        ViewCompat.setAccessibilityLiveRegion(
                labelAddress, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.requestAddressDialogName))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogDetails),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNeutralButton(
                    getResources().getString(R.string.dialogUpdate),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogClose),
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
            // positive button: show details
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    PointWrapper addressPoint = (PointWrapper) view.getTag();
                    if (addressPoint == null) {
                        SimpleMessageDialog.newInstance(
                                getResources().getString(R.string.messageNoAddressSelected))
                            .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
                    } else {
                        Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                        try {
                            detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, addressPoint.toJson().toString());
                        } catch (JSONException e) {
                            detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                        }
                        startActivity(detailsIntent);
                    }
                }
            });
            // neutral button: update
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    requestAddressForCurrentLocation();
                    manualRequest = true;
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
        // request address
        requestAddressForCurrentLocation();
    }

    private void requestAddressForCurrentLocation() {
        PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
        if (currentLocation.equals(PositionManager.getDummyLocation(getActivity()))) {
            String error = String.format(
                    getResources().getString(R.string.messageAddressRequestFailed),
                    getResources().getString(R.string.messageError1004));
            labelAddress.setText(error);
            //ttsWrapperInstance.speak(error, true, true);
        } else {
            labelAddress.setText("");
            addressManagerRequest = new AddressManager(
                    getActivity(),
                    RequestAddressDialog.this,
                    currentLocation.getPoint().getLatitude(),
                    currentLocation.getPoint().getLongitude());
            addressManagerRequest.execute();
        }
    }

    @Override public void addressRequestFinished(int returnCode, String returnMessage, PointWrapper addressPoint) {
        if (returnCode == Constants.ID.OK) {
            // stick to positive button
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setTag(addressPoint);
            }
            // show results
            String success = String.format(
                    getResources().getString(R.string.messageAddressRequestSuccessful),
                    addressPoint.getPoint().getName(),
                    String.format(
                        getResources().getString(R.string.labelPointDistanceAndBearing),
                        addressPoint.distanceFromCurrentLocation(),
                        StringUtility.formatInstructionDirection(
                            getActivity(), addressPoint.bearingFromCurrentLocation()))
                    );
            labelAddress.setText(success);
            // speak aloud if it was a manual request
            if (manualRequest) {
                //ttsWrapperInstance.speak(success, true, true);
                manualRequest = false;
            }

        } else {
            // clear positive button tag
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setTag(null);
            }
            // show results
            String error = String.format(
                    getResources().getString(R.string.messageAddressRequestFailed),
                    returnMessage);
            labelAddress.setText(error);
            // speak aloud if it was a manual request
            if (manualRequest) {
                //ttsWrapperInstance.speak(error, true, true);
                manualRequest = false;
            }
        }
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (addressManagerRequest != null
                && addressManagerRequest.getStatus() != AsyncTask.Status.FINISHED) {
            addressManagerRequest.cancel();
        }
    }

}
