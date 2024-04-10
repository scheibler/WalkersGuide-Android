package org.walkersguide.android.ui.dialog.create;

import org.walkersguide.android.database.profile.static_profile.HistoryProfile;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.ui.UiHelper;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;


import android.view.inputmethod.EditorInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;


import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;

import org.walkersguide.android.server.address.AddressException;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.widget.ListView;
import android.widget.AdapterView;
import androidx.appcompat.widget.SwitchCompat;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.sensor.PositionManager;

import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.address.ResolveAddressStringTask;
import org.walkersguide.android.server.address.AddressException;


public class EnterAddressDialog extends DialogFragment implements FragmentResultListener {
    public static final String REQUEST_ENTER_ADDRESS = "enterAddress";
    public static final String EXTRA_STREET_ADDRESS = "streetAddress";


    // instance constructors

    public static EnterAddressDialog newInstance() {
        return newInstance(null);
    }

    public static EnterAddressDialog newInstance(String addressString) {
        EnterAddressDialog dialog = new EnterAddressDialog();
        Bundle args = new Bundle();
        args.putString(KEY_ADDRESS_STRING, addressString);
        dialog.setArguments(args);
        return dialog;
    }

    // dialog
    private static final String KEY_ADDRESS_STRING = "addressString";
    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_NEARBY_CURRENT_LOCATION_IS_CHECKED = "nearbyCurrentLocationIsChecked";
    private static final String KEY_TRIED_TO_RESOLVE_ADDRESS_AUTOMATICALLY = "triedToResolveAddressAutomatically";

    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;
    private boolean triedToResolveAddressAutomatically;

    private EditTextAndClearInputButton layoutAddress;
    private SwitchCompat buttonNearbyCurrentLocation;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectAddressPointFromListDialog.REQUEST_SELECT_ADDRESS, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectAddressPointFromListDialog.REQUEST_SELECT_ADDRESS)) {
            addressSelected(
                    (StreetAddress) bundle.getSerializable(SelectAddressPointFromListDialog.EXTRA_STREET_ADDRESS));
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
            triedToResolveAddressAutomatically = savedInstanceState.getBoolean(KEY_TRIED_TO_RESOLVE_ADDRESS_AUTOMATICALLY);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
            triedToResolveAddressAutomatically = false;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_enter_address, nullParent);

        layoutAddress = (EditTextAndClearInputButton) view.findViewById(R.id.layoutAddress);
        layoutAddress.setHint(getResources().getString(R.string.editHintAddress));
        layoutAddress.setInputText(
                savedInstanceState != null
                ? savedInstanceState.getString(KEY_ADDRESS_STRING)
                : getArguments().getString(KEY_ADDRESS_STRING));
        layoutAddress.setVisibility(View.GONE);
        layoutAddress.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        tryToGetCoordinatesForAddress();
                    }
                });

        buttonNearbyCurrentLocation = (SwitchCompat) view.findViewById(R.id.buttonNearbyCurrentLocation);
        buttonNearbyCurrentLocation.setVisibility(View.GONE);

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
                    getResources().getString(R.string.dialogBack),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog == null) return;

        // positive button
        Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        updatePositiveButtonText(
                serverTaskExecutorInstance.taskInProgress(taskId));
        buttonPositive.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (serverTaskExecutorInstance.taskInProgress(taskId)) {
                    serverTaskExecutorInstance.cancelTask(taskId);
                } else {
                    tryToGetCoordinatesForAddress();
                }
            }
        });

        // negative button
        Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        buttonNegative.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                dismiss();
            }
        });

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_RESOLVE_ADDRESS_STRING_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);

        // auto mode if an address was given
        if (! triedToResolveAddressAutomatically
                && ! TextUtils.isEmpty(getArguments().getString(KEY_ADDRESS_STRING))) {
            tryToGetCoordinatesForAddress();
            triedToResolveAddressAutomatically = true;
        } else {
            layoutAddress.setVisibility(View.VISIBLE);
            buttonNearbyCurrentLocation.setVisibility(View.VISIBLE);
        }
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
        savedInstanceState.putString(KEY_ADDRESS_STRING, layoutAddress.getInputText());
        savedInstanceState.putBoolean(KEY_TRIED_TO_RESOLVE_ADDRESS_AUTOMATICALLY, triedToResolveAddressAutomatically);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            serverTaskExecutorInstance.cancelTask(taskId);
            UiHelper.hideKeyboard(this);
        }
    }

    private void tryToGetCoordinatesForAddress() {
        final String addressString = layoutAddress.getInputText();
        if (TextUtils.isEmpty(addressString)) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageAddressMissing),
                    Toast.LENGTH_LONG).show();
            return;
        }

        // get current position
        Point currentLocation = null;
        if (buttonNearbyCurrentLocation.isChecked()) {
            currentLocation = PositionManager.getInstance().getCurrentLocation();
            if (currentLocation == null) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.errorNoLocationFound),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        updatePositiveButtonText(true);
        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            taskId = serverTaskExecutorInstance.executeTask(
                    new ResolveAddressStringTask(addressString, currentLocation));
        }
    }

    private void updatePositiveButtonText(boolean requestInProgress) {
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setText(
                    requestInProgress
                    ? getResources().getString(R.string.dialogCancel)
                    : getResources().getString(R.string.dialogOK));
        }
    }


    // background task results

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_RESOLVE_ADDRESS_STRING_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_RESOLVE_ADDRESS_STRING_TASK_SUCCESSFUL)) {
                    ArrayList<StreetAddress> addressList = (ArrayList<StreetAddress>) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_STREET_ADDRESS_LIST);
                    if (addressList.size() == 1) {
                        addressSelected(addressList.get(0));
                    } else {
                        SelectAddressPointFromListDialog.newInstance(addressList)
                            .show(getChildFragmentManager(), "SelectAddressPointFromListDialog");
                    }

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    AddressException addressException = (AddressException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (addressException != null) {
                        SimpleMessageDialog.newInstance(addressException.getMessage())
                            .show(getChildFragmentManager(), "SimpleMessageDialog");
                    }
                }

                layoutAddress.setVisibility(View.VISIBLE);
                buttonNearbyCurrentLocation.setVisibility(View.VISIBLE);
                updatePositiveButtonText(false);
            }
        }
    };

    private void addressSelected(StreetAddress address) {
        HistoryProfile.addressPoints().addObject(address);
        // return
        Bundle result = new Bundle();
        result.putSerializable(EXTRA_STREET_ADDRESS, address);
        getParentFragmentManager().setFragmentResult(REQUEST_ENTER_ADDRESS, result);
        dismiss();
    }


    public static class SelectAddressPointFromListDialog extends DialogFragment {
        public static final String REQUEST_SELECT_ADDRESS = "selectAddress";
        public static final String EXTRA_STREET_ADDRESS = "streetAddress";


        // instance constructors

        public static SelectAddressPointFromListDialog newInstance(ArrayList<StreetAddress> addressPointList) {
            SelectAddressPointFromListDialog dialog = new SelectAddressPointFromListDialog();
            Bundle args = new Bundle();
            args.putSerializable(KEY_ADDRESS_POINT_LIST, addressPointList);
            dialog.setArguments(args);
            return dialog;
        }

        // dialog
        private static final String KEY_ADDRESS_POINT_LIST = "addressPointList";

        private ArrayList<StreetAddress> addressPointList;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            addressPointList = (ArrayList<StreetAddress>) getArguments().getSerializable(KEY_ADDRESS_POINT_LIST);

            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectAddressPointFromListDialogTitle))
                .setItems(
                        new String[]{getResources().getString(R.string.messagePleaseWait)},
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

                ListView listViewItems = (ListView) dialog.getListView();
                listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                        StreetAddress selectedAddress = (StreetAddress) parent.getItemAtPosition(position);
                        if (selectedAddress != null) {
                            Bundle result = new Bundle();
                            result.putSerializable(EXTRA_STREET_ADDRESS, selectedAddress);
                            getParentFragmentManager().setFragmentResult(REQUEST_SELECT_ADDRESS, result);
                        }
                    }
                });

                listViewItems.setAdapter(
                        new ArrayAdapter<StreetAddress>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            addressPointList));

                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dismiss();
                    }
                });
            }
        }
    }

}
