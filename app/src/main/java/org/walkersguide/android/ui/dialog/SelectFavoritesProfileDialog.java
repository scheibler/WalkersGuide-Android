package org.walkersguide.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;

import android.support.v4.app.DialogFragment;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.R;
import java.util.Map;
import java.util.TreeMap;
import android.app.Activity;
import org.walkersguide.android.listener.SelectFavoritesProfileListener;


public class SelectFavoritesProfileDialog extends DialogFragment {

    // Store instance variables
    private SelectFavoritesProfileListener selectFavoritesProfileListener;
    private AccessDatabase accessDatabaseInstance;

    public static SelectFavoritesProfileDialog newInstance(int favoritesProfileId) {
        SelectFavoritesProfileDialog selectFavoritesProfileDialogInstance = new SelectFavoritesProfileDialog();
        Bundle args = new Bundle();
        args.putInt("favoritesProfileId", favoritesProfileId);
        selectFavoritesProfileDialogInstance.setArguments(args);
        return selectFavoritesProfileDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof SelectFavoritesProfileListener) {
            selectFavoritesProfileListener = (SelectFavoritesProfileListener) getTargetFragment();
        } else if (context instanceof Activity
                && (Activity) context instanceof SelectFavoritesProfileListener) {
            selectFavoritesProfileListener = (SelectFavoritesProfileListener) context;
        }
        accessDatabaseInstance = AccessDatabase.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        TreeMap<Integer,String> favoritesProfileMap = accessDatabaseInstance.getFavoritesProfileMap();
        String[] formattedFavoritesProfileNameArray = new String[favoritesProfileMap.size()];
        int indexOfSelectedFavoritesProfile = -1;
        int index = 0;
        for (Map.Entry<Integer,String> profile : favoritesProfileMap.entrySet()) {
            formattedFavoritesProfileNameArray[index] = profile.getValue();
            if (profile.getKey() == getArguments().getInt("favoritesProfileId", -1)) {
                indexOfSelectedFavoritesProfile = index;
            }
            index += 1;
        }

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectFavoritesProfileDialogTitle))
            .setSingleChoiceItems(
                    formattedFavoritesProfileNameArray,
                    indexOfSelectedFavoritesProfile,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            int selectedProfileId = -1;
                            int index = 0;
                            for (Integer profileId : accessDatabaseInstance.getFavoritesProfileMap().keySet()) {
                                if (index == which) {
                                    selectedProfileId = profileId;
                                    break;
                                }
                                index += 1;
                            }
                            if (selectFavoritesProfileListener != null
                                    && selectedProfileId > -1) {
                                selectFavoritesProfileListener.favoritesProfileSelected(selectedProfileId);
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
        selectFavoritesProfileListener = null;
    }
}
