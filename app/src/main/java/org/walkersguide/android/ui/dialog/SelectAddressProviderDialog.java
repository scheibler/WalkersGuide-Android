package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.R;
import org.walkersguide.android.data.server.AddressProvider;
import org.walkersguide.android.data.server.PublicTransportProvider;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;


public class SelectAddressProviderDialog extends DialogFragment {

    // Store instance variables
    private SettingsManager settingsManagerInstance;

    public static SelectAddressProviderDialog newInstance() {
        SelectAddressProviderDialog selectAddressProviderDialogInstance = new SelectAddressProviderDialog();
        return selectAddressProviderDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] formattedAddressProviderNameArray = new String[Constants.AddressProviderValueArray.length];
        int indexOfSelectedAddressProvider = -1;
        for (int i=0; i<Constants.AddressProviderValueArray.length; i++) {
            AddressProvider addressProvider = new AddressProvider(getActivity(), Constants.AddressProviderValueArray[i]);
            formattedAddressProviderNameArray[i] = addressProvider.getName();
            if (addressProvider.equals(settingsManagerInstance.getServerSettings().getSelectedAddressProvider())) {
                indexOfSelectedAddressProvider = i;
            }
        }

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectAddressProviderDialogTitle))
            .setSingleChoiceItems(
                    formattedAddressProviderNameArray,
                    indexOfSelectedAddressProvider,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            AddressProvider newAddressProvider = null;
                            try {
                                newAddressProvider = new AddressProvider(
                                        getActivity(), Constants.AddressProviderValueArray[which]);
                            } catch (IndexOutOfBoundsException e) {
                                newAddressProvider = null;
                            } finally {
                                if (newAddressProvider != null) {
                                    settingsManagerInstance.getServerSettings().setSelectedAddressProvider(newAddressProvider);
                                    Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                                }
                            }
                            dismiss();
                        }
                    }
                    )
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    }
                    )
            .create();
    }

}
