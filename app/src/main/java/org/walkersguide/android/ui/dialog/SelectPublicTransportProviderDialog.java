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

public class SelectPublicTransportProviderDialog extends DialogFragment {

    // Store instance variables
    private AccessDatabase accessDatabaseInstance;
    private SettingsManager settingsManagerInstance;

    public static SelectPublicTransportProviderDialog newInstance(PublicTransportProvider provider) {
        SelectPublicTransportProviderDialog selectPublicTransportProviderDialogInstance = new SelectPublicTransportProviderDialog();
        Bundle args = new Bundle();
        if (provider != null) {
            args.putString("providerName", provider.getName());
        } else {
            args.putString("providerName", "");
        }
        selectPublicTransportProviderDialogInstance.setArguments(args);
        return selectPublicTransportProviderDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        accessDatabaseInstance = AccessDatabase.getInstance(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] formattedPublicTransportProviderNameArray = new String[accessDatabaseInstance.getPublicTransportProviderList().size()];
        int indexOfSelectedPublicTransportProvider = -1;
        int index = 0;
        for (PublicTransportProvider provider : accessDatabaseInstance.getPublicTransportProviderList()) {
            formattedPublicTransportProviderNameArray[index] = provider.getName();
            if (provider.getName().equals(getArguments().getString("providerName"))) {
                indexOfSelectedPublicTransportProvider = index;
            }
            index += 1;
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
                                selectedProvider = accessDatabaseInstance.getPublicTransportProviderList().get(which);
                            } catch (IndexOutOfBoundsException e) {
                                selectedProvider = null;
                            } finally {
                                if (selectedProvider != null) {
                                    ServerSettings serverSettings = settingsManagerInstance.getServerSettings();
                                    serverSettings.setSelectedPublicTransportProvider(selectedProvider);
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
