package org.walkersguide.android.ui.dialog.toolbar;

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


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;



import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;


public class LocationSensorDetailsDialog extends DialogFragment {

    private TextView labelGPSLatitude, labelGPSLongitude;
    private TextView labelGPSProvider, labelGPSAccuracy;
    private TextView labelGPSAltitude, labelGPSBearing;
    private TextView labelGPSSpeed, labelGPSTime;

    public static LocationSensorDetailsDialog newInstance() {
        LocationSensorDetailsDialog dialog = new LocationSensorDetailsDialog();
        return dialog;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_location_sensor_details, nullParent);

        labelGPSLatitude = (TextView) view.findViewById(R.id.labelGPSLatitude);
        labelGPSLongitude = (TextView) view.findViewById(R.id.labelGPSLongitude);
        labelGPSProvider = (TextView) view.findViewById(R.id.labelGPSProvider);
        labelGPSAccuracy = (TextView) view.findViewById(R.id.labelGPSAccuracy);
        labelGPSAltitude = (TextView) view.findViewById(R.id.labelGPSAltitude);
        labelGPSBearing = (TextView) view.findViewById(R.id.labelGPSBearing);
        labelGPSSpeed = (TextView) view.findViewById(R.id.labelGPSSpeed);
        labelGPSTime = (TextView) view.findViewById(R.id.labelGPSTime);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.locationSensorDetailsDialogTitle))
            .setView(view)
            .setNegativeButton(
                    getResources().getString(R.string.dialogClose),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_GPS_LOCATION);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // request gps location
        PositionManager.getInstance().requestGPSLocation();
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_GPS_LOCATION)) {

                // clear fields
                labelGPSLatitude.setText(context.getResources().getString(R.string.labelGPSLatitude));
                labelGPSLongitude.setText(context.getResources().getString(R.string.labelGPSLongitude));
                labelGPSProvider.setText(context.getResources().getString(R.string.labelGPSProvider));
                labelGPSAccuracy.setText(context.getResources().getString(R.string.labelGPSAccuracy));
                labelGPSAltitude.setText(context.getResources().getString(R.string.labelGPSAltitude));
                labelGPSBearing.setText(context.getResources().getString(R.string.labelGPSBearing));
                labelGPSSpeed.setText(context.getResources().getString(R.string.labelGPSSpeed));
                labelGPSTime.setText(context.getResources().getString(R.string.labelGPSTime));

                // get gps location
                PointWrapper pointWrapper = PointWrapper.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_GPS_LOCATION_OBJECT));
                if (pointWrapper  != null
                        && pointWrapper.getPoint() instanceof GPS) {
                    GPS gpsLocation = (GPS) pointWrapper.getPoint();

                    // fill labels
                    labelGPSLatitude.setText(gpsLocation.formatLatitude());
                    labelGPSLongitude.setText(gpsLocation.formatLongitude());
                    labelGPSProvider.setText(gpsLocation.formatProviderAndNumberOfSatellites());
                    labelGPSAccuracy.setText(gpsLocation.formatAccuracyInMeters());
                    labelGPSAltitude.setText(gpsLocation.formatAltitudeInMeters());
                    labelGPSBearing.setText(gpsLocation.formatBearingInDegrees());
                    labelGPSSpeed.setText(gpsLocation.formatSpeedInKMH());
                    labelGPSTime.setText(gpsLocation.formatTimestamp());
                }
            }
        }
    };

}
