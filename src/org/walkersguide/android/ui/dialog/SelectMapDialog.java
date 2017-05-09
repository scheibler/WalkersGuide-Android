package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.R;
import org.walkersguide.android.data.server.Map;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.listener.ServerStatusListener;
import org.walkersguide.android.server.ServerStatus;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

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

public class SelectMapDialog extends DialogFragment implements ServerStatusListener {

    // Store instance variables
    private AccessDatabase accessDatabaseInstance;
    private SettingsManager settingsManagerInstance;
    private ServerStatus serverStatusRequest;

    public static SelectMapDialog newInstance(Map map) {
        SelectMapDialog selectMapDialogInstance = new SelectMapDialog();
        Bundle args = new Bundle();
        if (map != null) {
            args.putString("mapName", map.getName());
        } else {
            args.putString("mapName", "");
        }
        selectMapDialogInstance.setArguments(args);
        return selectMapDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        accessDatabaseInstance = AccessDatabase.getInstance(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
        serverStatusRequest = null;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] formattedMapNameArray = new String[accessDatabaseInstance.getMapList().size()];
        int indexOfSelectedMap = -1;
        int index = 0;
        for (Map map : accessDatabaseInstance.getMapList()) {
            formattedMapNameArray[index] = map.getName();
            if (map.getName().equals(getArguments().getString("mapName"))) {
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
                            Map selectedMap = null;
                            try {
                                selectedMap = accessDatabaseInstance.getMapList().get(which);
                            } catch (IndexOutOfBoundsException e) {
                                selectedMap = null;
                            } finally {
                                if (selectedMap != null) {
                                    serverStatusRequest = new ServerStatus(
                                            getActivity(),
                                            SelectMapDialog.this,
                                            ServerStatus.ACTION_UPDATE_MAP,
                                            settingsManagerInstance.getServerSettings().getServerURL(),
                                            selectedMap);
                                    serverStatusRequest.execute();
                                }
                            }
                        }
                    }
                    )
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dialog.dismiss();
                }
            });
        }
    }

    @Override public void statusRequestFinished(int updateAction, int returnCode, String returnMessage) {
        if (returnCode == Constants.ID.OK) {
            ((GlobalInstance) getActivity().getApplicationContext()).setApplicationInBackground(true);
            Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            dismiss();
        } else {
            SimpleMessageDialog.newInstance(returnMessage)
                .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
        }
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (serverStatusRequest != null
                && serverStatusRequest.getStatus() != AsyncTask.Status.FINISHED) {
            serverStatusRequest.cancel();
        }
    }

}
