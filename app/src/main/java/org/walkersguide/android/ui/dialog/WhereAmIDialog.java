package org.walkersguide.android.ui.dialog;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;
    import org.walkersguide.android.ui.view.TextViewAndActionButton.LabelTextConfig;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
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

import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.AddressManager;
import org.walkersguide.android.server.AddressManager.AddressRequestListener;
import org.walkersguide.android.ui.activity.toolbar.tabs.PointDetailsActivity;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import java.util.ArrayList;
import org.walkersguide.android.data.basic.point.StreetAddress;
import org.walkersguide.android.data.basic.point.Point;


public class WhereAmIDialog extends DialogFragment implements AddressRequestListener {

    private AddressManager addressManagerRequest;
    private TextViewAndActionButton layoutCurrentAddress;

    public static WhereAmIDialog newInstance() {
        WhereAmIDialog dialog = new WhereAmIDialog();
        return dialog;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        layoutCurrentAddress = new TextViewAndActionButton(WhereAmIDialog.this.getContext());
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutCurrentAddress.setLayoutParams(lp);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.whereAmIDialogTitle))
            .setView(layoutCurrentAddress)
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
            // request address
            requestAddressForCurrentLocation();
        }
    }

    private void requestAddressForCurrentLocation() {
        Point currentLocation = PositionManager.getInstance().getCurrentLocation();
        layoutCurrentAddress.configureView(null);
        if (currentLocation == null) {
            layoutCurrentAddress.setLabelText(
                    ServerUtility.getErrorMessageForReturnCode(Constants.RC.NO_LOCATION_FOUND));
        } else {
            layoutCurrentAddress.setLabelText(
                    getResources().getString(R.string.messagePleaseWait));
            addressManagerRequest = new AddressManager(
                    WhereAmIDialog.this,
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());
            addressManagerRequest.execute();
        }
    }

    @Override public void requireAddressRequestSuccessful(StreetAddress addressPoint) {
        layoutCurrentAddress.configureView(
                addressPoint, LabelTextConfig.empty(false));
        // add to database
        AccessDatabase.getInstance().addObjectToDatabaseProfile(
                addressPoint, DatabasePointProfile.ADDRESS_POINTS);
    }

    @Override public void requireCoordinatesRequestSuccessful(ArrayList<StreetAddress> addressPointList) {
    }

    @Override public void addressOrCoordinatesRequestFailed(int returnCode) {
        layoutCurrentAddress.configureView(null);
        layoutCurrentAddress.setLabelText(
                ServerUtility.getErrorMessageForReturnCode(returnCode));
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (addressManagerRequest != null
                && addressManagerRequest.getStatus() != AsyncTask.Status.FINISHED) {
            addressManagerRequest.cancel();
        }
    }

}
