package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.server.address.AddressException;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;
import android.app.AlertDialog;
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


public class WhereAmIDialog extends DialogFragment {
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

    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;
    private boolean onlyResolveAddressAndCloseDialogImmediately, announceNewAddress;

    private TextViewAndActionButton layoutCurrentAddress;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
        }
        onlyResolveAddressAndCloseDialogImmediately = getArguments().getBoolean(KEY_ONLY_RESOLVE_ADDRESS);
        announceNewAddress = false;

        layoutCurrentAddress = new TextViewAndActionButton(WhereAmIDialog.this.getContext(), true);
        layoutCurrentAddress.setLayoutParams(
                new LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layoutCurrentAddress.setAutoUpdate(true);

        // create dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
            .setView(layoutCurrentAddress)
            .setNegativeButton(
                    onlyResolveAddressAndCloseDialogImmediately
                    ? getResources().getString(R.string.dialogCancel)
                    : getResources().getString(R.string.dialogClose),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

        if (! onlyResolveAddressAndCloseDialogImmediately) {
            dialogBuilder.setTitle(getResources().getString(R.string.whereAmIDialogTitle))
                .setNeutralButton(
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
                        requestAddressForCurrentLocation();
                        announceNewAddress = true;
                    }
                });
            }
        }

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_RESOLVE_COORDINATES_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        localIntentFilter.addAction(DeviceSensorManager.ACTION_SHAKE_DETECTED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);

        // request address
        requestAddressForCurrentLocation();
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            serverTaskExecutorInstance.cancelTask(taskId);
        }
    }

    private void requestAddressForCurrentLocation() {
        layoutCurrentAddress.reset();

        // get current position
        final Point currentLocation = PositionManager.getInstance().getCurrentLocation();
        if (currentLocation == null) {
            layoutCurrentAddress.configureAsSingleObject(
                    null, getResources().getString(R.string.errorNoLocationFound));
            return;
        }

        layoutCurrentAddress.configureAsSingleObject(
                null, getResources().getString(R.string.messagePleaseWait));
        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            taskId = serverTaskExecutorInstance.executeTask(
                    new ResolveCoordinatesTask(
                        currentLocation.getLatitude(), currentLocation.getLongitude()));
        }
    }

    // background task results

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_RESOLVE_COORDINATES_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_RESOLVE_COORDINATES_TASK_SUCCESSFUL)) {
                    StreetAddress addressPoint = (StreetAddress) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_STREET_ADDRESS);
                    if (addressPoint != null) {
                        if (onlyResolveAddressAndCloseDialogImmediately) {
                            Bundle result = new Bundle();
                            result.putSerializable(EXTRA_STREET_ADDRESS, addressPoint);
                            getParentFragmentManager().setFragmentResult(REQUEST_RESOLVE_COORDINATES, result);
                            dismiss();
                        } else {
                            layoutCurrentAddress.configureAsSingleObject(addressPoint);
                            // announce
                            if (announceNewAddress) {
                                TTSWrapper.getInstance().screenReader(addressPoint.toString());
                            }
                        }
                    }

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {
                    layoutCurrentAddress.configureAsSingleObject(
                            null, context.getResources().getString(R.string.errorReqRequestCancelled));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    AddressException addressException = (AddressException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (addressException != null) {
                        layoutCurrentAddress.configureAsSingleObject(
                                null, addressException.getMessage());
                    }
                }
                announceNewAddress = false;

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_SHAKE_DETECTED)) {
                requestAddressForCurrentLocation();
                announceNewAddress = true;
            }
        }
    };

}
