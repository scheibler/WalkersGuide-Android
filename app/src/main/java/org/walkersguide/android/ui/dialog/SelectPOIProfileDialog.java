package org.walkersguide.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;

import android.support.v4.app.DialogFragment;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.listener.POIProfileListener;
import org.walkersguide.android.R;
import java.util.Map;
import java.util.TreeMap;
import android.app.Activity;
import org.walkersguide.android.listener.SelectPOIProfileListener;


public class SelectPOIProfileDialog extends DialogFragment {

    // Store instance variables
    private SelectPOIProfileListener selectPOIProfileListener;
    private AccessDatabase accessDatabaseInstance;

    public static SelectPOIProfileDialog newInstance(int poiProfileId) {
        SelectPOIProfileDialog selectPOIProfileDialogInstance = new SelectPOIProfileDialog();
        Bundle args = new Bundle();
        args.putInt("poiProfileId", poiProfileId);
        selectPOIProfileDialogInstance.setArguments(args);
        return selectPOIProfileDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof SelectPOIProfileListener) {
            selectPOIProfileListener = (SelectPOIProfileListener) getTargetFragment();
        } else if (context instanceof Activity
                && (Activity) context instanceof SelectPOIProfileListener) {
            selectPOIProfileListener = (SelectPOIProfileListener) context;
        }
        accessDatabaseInstance = AccessDatabase.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        TreeMap<Integer,String> poiProfileMap = accessDatabaseInstance.getPOIProfileMap();
        String[] formattedPOIProfileNameArray = new String[poiProfileMap.size()];
        int indexOfSelectedPOIProfile = -1;
        int index = 0;
        for (Map.Entry<Integer,String> profile : poiProfileMap.entrySet()) {
            formattedPOIProfileNameArray[index] = profile.getValue();
            if (profile.getKey() == getArguments().getInt("poiProfileId", -1)) {
                indexOfSelectedPOIProfile = index;
            }
            index += 1;
        }

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectPOIProfileDialogTitle))
            .setSingleChoiceItems(
                    formattedPOIProfileNameArray,
                    indexOfSelectedPOIProfile,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            int selectedProfileId = -1;
                            int index = 0;
                            for (Integer profileId : accessDatabaseInstance.getPOIProfileMap().keySet()) {
                                if (index == which) {
                                    selectedProfileId = profileId;
                                    break;
                                }
                                index += 1;
                            }
                            if (selectPOIProfileListener != null
                                    && selectedProfileId > -1) {
                                selectPOIProfileListener.poiProfileSelected(selectedProfileId);
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

    @Override public void onStop() {
        super.onStop();
        selectPOIProfileListener = null;
    }
}
