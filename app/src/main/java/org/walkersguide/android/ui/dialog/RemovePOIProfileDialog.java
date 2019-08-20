package org.walkersguide.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;


public class RemovePOIProfileDialog extends DialogFragment {

    // Store instance variables
    private AccessDatabase accessDatabaseInstance;
    private int poiProfileId;

    public static RemovePOIProfileDialog newInstance(int poiProfileId) {
        RemovePOIProfileDialog removePOIProfileDialogInstance = new RemovePOIProfileDialog();
        Bundle args = new Bundle();
        args.putInt("poiProfileId", poiProfileId);
        removePOIProfileDialogInstance.setArguments(args);
        return removePOIProfileDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        accessDatabaseInstance = AccessDatabase.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        poiProfileId = getArguments().getInt("poiProfileId", -1);
        return new AlertDialog.Builder(getActivity())
            .setMessage(
                    String.format(
                        getResources().getString(R.string.removeProfileDialogTitle),
                        accessDatabaseInstance.getNameOfPOIProfile(poiProfileId)))
            .setPositiveButton(
                    getResources().getString(R.string.dialogYes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            accessDatabaseInstance.removePOIProfile(poiProfileId);
                            Intent intent = new Intent(Constants.ACTION_POI_PROFILE_REMOVED);
                            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                            dismiss();
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogNo),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
            .create();
    }

}
