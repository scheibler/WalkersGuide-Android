package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.R;
import org.walkersguide.android.data.server.PublicTransportProvider;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.server.ServerStatusManager;


public class SelectPublicTransportProviderDialog extends DialogFragment {

    // Store instance variables
    private SettingsManager settingsManagerInstance;
    private ServerStatusManager serverStatusManagerInstance;

    public static SelectPublicTransportProviderDialog newInstance() {
        SelectPublicTransportProviderDialog selectPublicTransportProviderDialogInstance = new SelectPublicTransportProviderDialog();
        return selectPublicTransportProviderDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
        serverStatusManagerInstance = ServerStatusManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        ServerInstance serverInstance = serverStatusManagerInstance.getServerInstance();
        String[] formattedPublicTransportProviderNameArray = new String[0];
        if (serverInstance != null) {
            formattedPublicTransportProviderNameArray = new String[serverInstance.getSupportedPublicTransportProviderList().size()];
        }
        int indexOfSelectedPublicTransportProvider = -1;
        int index = 0;
        if (serverInstance != null) {
            for (PublicTransportProvider provider : serverInstance.getSupportedPublicTransportProviderList()) {
                formattedPublicTransportProviderNameArray[index] = provider.toString();
                if (provider.equals(settingsManagerInstance.getServerSettings().getSelectedPublicTransportProvider())) {
                    indexOfSelectedPublicTransportProvider = index;
                }
                index += 1;
            }
        }

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectPublicTransportProviderDialogTitle))
            .setSingleChoiceItems(
                    formattedPublicTransportProviderNameArray,
                    indexOfSelectedPublicTransportProvider,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            PublicTransportProvider selectedProvider = null;
                            try {
                                selectedProvider = serverStatusManagerInstance.getServerInstance().getSupportedPublicTransportProviderList().get(which);
                            } catch (IndexOutOfBoundsException e) {
                                selectedProvider = null;
                            } finally {
                                if (selectedProvider != null) {
                                    settingsManagerInstance.getServerSettings().setSelectedPublicTransportProvider(selectedProvider);
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
