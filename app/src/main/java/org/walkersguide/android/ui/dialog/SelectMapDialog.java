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

import java.util.ArrayList;

import org.walkersguide.android.data.server.OSMMap;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.R;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.server.ServerStatusManager.ServerStatusListener;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;


public class SelectMapDialog extends DialogFragment implements ServerStatusListener {

    // Store instance variables
    private ServerStatusManager serverStatusManagerInstance;
    private SettingsManager settingsManagerInstance;

    public static SelectMapDialog newInstance() {
        SelectMapDialog selectMapDialogInstance = new SelectMapDialog();
        return selectMapDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
        serverStatusManagerInstance = ServerStatusManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectMapDialogTitle))
            .setItems(
                    new String[]{getResources().getString(R.string.messagePleaseWait)},
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .setNeutralButton(
                    getResources().getString(R.string.dialogSomethingMissing),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SendFeedbackDialog.newInstance(
                                    SendFeedbackDialog.Token.MAP_REQUEST)
                                .show(getActivity().getSupportFragmentManager(), "SendFeedbackDialog");
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
                (SelectMapDialog) this, settingsManagerInstance.getServerSettings().getServerURL());
    }

    @Override public void serverStatusRequestFinished(Context context, int returnCode, ServerInstance serverInstance) {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (returnCode == Constants.RC.OK
                && dialog != null
                && serverInstance != null) {
            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    OSMMap selectedMap = (OSMMap) parent.getItemAtPosition(position);
                    if (selectedMap != null) {
                        settingsManagerInstance.getServerSettings().setSelectedMap(selectedMap);
                        Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                    }
                    dismiss();
                }
            });
            // fill listview
            ArrayList<OSMMap> osmMapList = new ArrayList<OSMMap>();
            for (OSMMap map : serverInstance.getAvailableMapList()) {
                osmMapList.add(map);
            }
            listViewItems.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listViewItems.setAdapter(
                    new ArrayAdapter<OSMMap>(
                        context, android.R.layout.select_dialog_singlechoice, osmMapList));
            // select list item
            OSMMap selectedMap = settingsManagerInstance.getServerSettings().getSelectedMap();
            if (selectedMap != null) {
                listViewItems.setItemChecked(
                        osmMapList.indexOf(selectedMap), true);
            }
        }
    }

}
