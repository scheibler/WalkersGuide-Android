package org.walkersguide.android.ui.dialog.select;

import org.walkersguide.android.ui.dialog.template.SelectMultipleObjectsFromListDialog;
import org.walkersguide.android.R;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import org.walkersguide.android.server.wg.status.ServerInstance;
import android.widget.Button;


import org.walkersguide.android.ui.dialog.SimpleMessageDialog;

import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.status.ServerStatusTask;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.server.wg.WgException;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.Intent;
import android.content.Context;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.database.profile.Collection;
import org.walkersguide.android.database.util.AccessDatabase;


public class SelectCollectionsDialog extends SelectMultipleObjectsFromListDialog<Collection> {
    public static final String REQUEST_SELECT_COLLECTIONS = "selectCollections";
    public static final String EXTRA_COLLECTION_LIST = "collectionList";


    // instance constructors

    public static SelectCollectionsDialog newInstance(ArrayList<Collection> selectedCollectionList) {
        SelectCollectionsDialog dialog= new SelectCollectionsDialog();
        dialog.setArguments(
                createInitialObjectListBundle(
                    AccessDatabase.getInstance().getCollectionList(), selectedCollectionList));
        return dialog;
    }


    // dialog

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            initializeDialog();
        }
    }

    @Override public String getDialogTitleNothingSelected() {
        return getResources().getString(R.string.selectCollectionsDialogTitle);
    }

    @Override public int getPluralResourceId() {
        return R.plurals.collectionSelected;
    }

    @Override public void execute(ArrayList<Collection> selectedCollectionList) {
        Bundle result = new Bundle();
        result.putSerializable(EXTRA_COLLECTION_LIST, selectedCollectionList);
        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_COLLECTIONS, result);
        dismiss();
    }

}
