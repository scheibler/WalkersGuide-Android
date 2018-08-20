package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.R;
import org.walkersguide.android.data.server.OSMMap;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;

import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.server.ServerStatusManager;
import java.util.ArrayList;


public class SelectMapDialog extends DialogFragment {

    // Store instance variables
    private ServerStatusManager serverStatusManagerInstance;
    private SettingsManager settingsManagerInstance;
    private ArrayList<OSMMap> osmMapList;

    public static SelectMapDialog newInstance() {
        SelectMapDialog selectMapDialogInstance = new SelectMapDialog();
        return selectMapDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
        serverStatusManagerInstance = ServerStatusManager.getInstance(context);
        // get selectable maps
        this.osmMapList = new ArrayList<OSMMap>();
        ServerInstance serverInstance = serverStatusManagerInstance.getServerInstance();
        if (serverInstance != null) {
            for (OSMMap map : serverInstance.getAvailableMapList()) {
                if (! map.getDevelopment()
                        || settingsManagerInstance.getServerSettings().getLogQueriesOnServer()) {
                    this.osmMapList.add(map);
                }
            }
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] formattedMapNameArray = new String[this.osmMapList.size()];
        int indexOfSelectedMap = -1;
        int index = 0;
        for (OSMMap map : this.osmMapList) {
            formattedMapNameArray[index] = map.toString();
            if (map.equals(settingsManagerInstance.getServerSettings().getSelectedMap())) {
                indexOfSelectedMap = index;
            }
            index += 1;
        }

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectMapDialogTitle))
            .setSingleChoiceItems(
                    formattedMapNameArray,
                    indexOfSelectedMap,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            OSMMap selectedMap = null;
                            try {
                                selectedMap = osmMapList.get(which);
                            } catch (IndexOutOfBoundsException e) {
                                selectedMap = null;
                            } finally {
                                if (selectedMap != null) {
                                    settingsManagerInstance.getServerSettings().setSelectedMap(selectedMap);
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
