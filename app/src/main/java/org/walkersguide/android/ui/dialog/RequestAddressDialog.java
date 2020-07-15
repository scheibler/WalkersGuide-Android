package org.walkersguide.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.core.view.ViewCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.AddressManager;
import org.walkersguide.android.server.AddressManager.AddressListener;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import java.util.ArrayList;
import org.walkersguide.android.data.profile.HistoryPointProfile;
import org.walkersguide.android.data.basic.point.StreetAddress;

public class RequestAddressDialog extends DialogFragment implements AddressListener {

    // Store instance variables
    private AccessDatabase accessDatabaseInstance;
    private PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;
    private AddressManager addressManagerRequest;

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
        addressManagerRequest = null;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
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
                    getResources().getString(R.string.dialogRefresh),
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
        if (currentLocation == null) {
            labelAddress.setText(
                    String.format(
                        getResources().getString(R.string.messageAddressRequestFailed),
                        getResources().getString(R.string.errorNoLocationFound))
                    );
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

    @Override public void addressRequestFinished(Context context, int returnCode, ArrayList<PointWrapper> addressPointList) {
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog == null) {
            return;
        }

        if (returnCode == Constants.RC.OK
                && addressPointList != null) {
            PointWrapper addressPoint = addressPointList.get(0);

            // add to database
            AccessDatabase.getInstance(context).addFavoritePointToProfile(
                    addressPoint, HistoryPointProfile.ID_ADDRESS_POINTS);

            // stick to positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setTag(addressPoint);

            // show results
            if (addressPoint.getPoint() instanceof StreetAddress) {
                labelAddress.setText(
                        String.format(
                            context.getResources().getString(R.string.messageAddressRequestSuccessful),
                            ((StreetAddress) addressPoint.getPoint()).formatAddressLongLength(),
                            String.format(
                                context.getResources().getString(R.string.labelPointDistanceAndBearing),
                                context.getResources().getQuantityString(
                                    R.plurals.meter,
                                    addressPoint.distanceFromCurrentLocation(),
                                    addressPoint.distanceFromCurrentLocation()),
                                StringUtility.formatRelativeViewingDirection(
                                    context, addressPoint.bearingFromCurrentLocation()))
                            )
                        );
            } else {
                labelAddress.setText(addressPoint.toString());
            }

        } else {
            // clear positive button tag
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setTag(null);
            // show results
            labelAddress.setText(
                    String.format(
                        context.getResources().getString(R.string.messageAddressRequestFailed),
                        ServerUtility.getErrorMessageForReturnCode(context, returnCode))
                    );
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
