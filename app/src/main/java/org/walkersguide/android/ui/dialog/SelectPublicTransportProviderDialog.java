package org.walkersguide.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;

import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.walkersguide.android.data.server.PublicTransportProvider;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.R;
import org.walkersguide.android.server.DepartureManager;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.server.ServerStatusManager.ServerStatusListener;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;


public class SelectPublicTransportProviderDialog extends DialogFragment implements ServerStatusListener {

    // Store instance variables
    private DepartureManager departureManagerInstance;
    private SettingsManager settingsManagerInstance;
    private ServerStatusManager serverStatusManagerInstance;

    public static SelectPublicTransportProviderDialog newInstance() {
        SelectPublicTransportProviderDialog selectPublicTransportProviderDialogInstance = new SelectPublicTransportProviderDialog();
        return selectPublicTransportProviderDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        departureManagerInstance = DepartureManager.getInstance(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
        serverStatusManagerInstance = ServerStatusManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectPublicTransportProviderDialogTitle))
            .setItems(
                    new String[]{getResources().getString(R.string.messagePleaseWait)},
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
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

    @Override public void onStop() {
        super.onStop();
        serverStatusManagerInstance.invalidateServerStatusRequest(this);
    }

    @Override public void onStart() {
        super.onStart();
        serverStatusManagerInstance.requestServerStatus(
                (SelectPublicTransportProviderDialog) this, settingsManagerInstance.getServerSettings().getServerURL());
    }

    @Override public void serverStatusRequestFinished(Context context, int returnCode, ServerInstance serverInstance) {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (returnCode == Constants.RC.OK
                && dialog != null
                && serverInstance != null) {
            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    PublicTransportProvider selectedProvider = (PublicTransportProvider) parent.getItemAtPosition(position);
                    if (selectedProvider != null) {
                        departureManagerInstance.cleanDepartureCache();
                        settingsManagerInstance.getServerSettings().setSelectedPublicTransportProvider(selectedProvider);
                        Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                    }
                    dismiss();
                }
            });
            // fill listview
            listViewItems.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listViewItems.setAdapter(
                    new ArrayAdapter<PublicTransportProvider>(
                        context,
                        android.R.layout.select_dialog_singlechoice,
                        serverInstance.getSupportedPublicTransportProviderList()));
            // select list item
            PublicTransportProvider selectedProvider = settingsManagerInstance.getServerSettings().getSelectedPublicTransportProvider();
            if (selectedProvider != null) {
                listViewItems.setItemChecked(
                        serverInstance.getSupportedPublicTransportProviderList().indexOf(selectedProvider), true);
            }
        }
    }

}
