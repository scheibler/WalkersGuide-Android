package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.server.address.AddressException;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.View;

import android.widget.Button;


import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.data.object_with_id.Point;


import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.address.ResolveCoordinatesTask;
import org.walkersguide.android.server.address.AddressException;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.ui.view.ResolveCurrentAddressView;
import org.walkersguide.android.ui.view.ResolveCurrentAddressView.OnCurrentAddressResolvedListener;


public class WhereAmIDialog extends DialogFragment implements OnCurrentAddressResolvedListener {
    public static final String REQUEST_RESOLVE_COORDINATES = "resolveCoordinates";
    public static final String EXTRA_STREET_ADDRESS = "streetAddress";


    // instance constructors

    public static WhereAmIDialog newInstance() {
        return WhereAmIDialog.newInstance(false);
    }

    public static WhereAmIDialog newInstance(boolean onlyResolveAddressAndCloseDialogImmediately) {
        WhereAmIDialog dialog = new WhereAmIDialog();
        Bundle args = new Bundle();
        args.putBoolean(KEY_ONLY_RESOLVE_ADDRESS, onlyResolveAddressAndCloseDialogImmediately);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_ONLY_RESOLVE_ADDRESS = "onlyResolveAddressAndCloseDialogImmediately";

    private boolean onlyResolveAddressAndCloseDialogImmediately;
    private ResolveCurrentAddressView layoutClosestAddress;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        onlyResolveAddressAndCloseDialogImmediately = getArguments().getBoolean(KEY_ONLY_RESOLVE_ADDRESS);

        layoutClosestAddress = new ResolveCurrentAddressView(WhereAmIDialog.this.getContext());
        layoutClosestAddress.setLayoutParams(
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layoutClosestAddress.setOnCurrentAddressResolvedListener(this);
        layoutClosestAddress.setTaskId(
                savedInstanceState != null
                ? savedInstanceState.getLong(KEY_TASK_ID)
                : ServerTaskExecutor.NO_TASK_ID);

        // create dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
            .setView(layoutClosestAddress)
            .setNegativeButton(
                    onlyResolveAddressAndCloseDialogImmediately
                    ? getResources().getString(R.string.dialogCancel)
                    : getResources().getString(R.string.dialogClose),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

        if (! onlyResolveAddressAndCloseDialogImmediately) {
            dialogBuilder.setNeutralButton(
                        getResources().getString(R.string.dialogRefresh),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
        }

        return dialogBuilder.create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {

            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });

            // neutral button: update (is optional)
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (buttonNeutral != null) {
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        layoutClosestAddress.requestAddressForCurrentLocation();
                    }
                });
            }
        }

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(DeviceSensorManager.ACTION_SHAKE_DETECTED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, layoutClosestAddress.getTaskId());
    }

    @Override public void onCurrentAddressResolved(StreetAddress addressPoint) {
        if (onlyResolveAddressAndCloseDialogImmediately) {
            dismiss();
            Bundle result = new Bundle();
            result.putSerializable(EXTRA_STREET_ADDRESS, addressPoint);
            getParentFragmentManager().setFragmentResult(REQUEST_RESOLVE_COORDINATES, result);
        } else {
            // announce
            TTSWrapper.getInstance().screenReader(addressPoint.toString());
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            layoutClosestAddress.cancelTask();
        }
    }


    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DeviceSensorManager.ACTION_SHAKE_DETECTED)) {
                layoutClosestAddress.requestAddressForCurrentLocation();
            }
        }
    };

}
