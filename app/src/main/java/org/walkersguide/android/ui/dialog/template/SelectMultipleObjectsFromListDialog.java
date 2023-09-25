package org.walkersguide.android.ui.dialog.template;

import org.walkersguide.android.R;
import org.walkersguide.android.server.wg.poi.PoiCategory;
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


public abstract class SelectMultipleObjectsFromListDialog<T> extends DialogFragment {

    public abstract String getDialogTitleNothingSelected();
    public abstract int getPluralResourceId();
    public abstract void execute(ArrayList<T> selectedObjectsList);

    public static <T> Bundle createInitialObjectListBundle(
            ArrayList<T> allObjectsList, ArrayList<T> selectedObjectsList) {
        Bundle args = new Bundle();
        args.putSerializable(KEY_ALL_OBJECTS_LIST, allObjectsList);
        args.putSerializable(KEY_SELECTED_OBJECTS_LIST, selectedObjectsList);
        return args;
    }


    // dialog
    private static final String KEY_ALL_OBJECTS_LIST = "allObjectsList";
    private static final String KEY_SELECTED_OBJECTS_LIST = "selectedObjectsList";

    private ArrayList<T> allObjectsList, selectedObjectsList;

    public boolean hasAllObjectsList() {
        return this.allObjectsList != null;
    }

    public void updateAllObjectsList(ArrayList<T> newList) {
        this.allObjectsList = newList;
    }

    public String getDialogTitle() {
        return selectedObjectsList.isEmpty()
            ? getDialogTitleNothingSelected()
            : GlobalInstance.getPluralResource(
                    getPluralResourceId(), selectedObjectsList.size());
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        allObjectsList = savedInstanceState != null
            ? (ArrayList<T>) savedInstanceState.getSerializable(KEY_ALL_OBJECTS_LIST)
            : (ArrayList<T>) getArguments().getSerializable(KEY_ALL_OBJECTS_LIST);
        selectedObjectsList = savedInstanceState != null
            ? (ArrayList<T>) savedInstanceState.getSerializable(KEY_SELECTED_OBJECTS_LIST)
            : (ArrayList<T>) getArguments().getSerializable(KEY_SELECTED_OBJECTS_LIST);

        return new AlertDialog.Builder(getActivity())
            .setMultiChoiceItems(
                    new String[]{getResources().getString(R.string.messagePleaseWait)},
                    null,
                    new DialogInterface.OnMultiChoiceClickListener() { 
                        @Override public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            T selectedObject = null;
                            try {
                                selectedObject = allObjectsList.get(which);
                            } catch (IndexOutOfBoundsException | NullPointerException e) {
                                selectedObject = null;
                            } finally {
                                if (selectedObject != null) {
                                    // add or remove
                                    if (selectedObjectsList.contains(selectedObject)) {
                                        selectedObjectsList.remove(selectedObject);
                                    } else {
                                        selectedObjectsList.add(selectedObject);
                                    }
                                    updateDialogTitleAndNeutralButtonLabel();
                                }
                            }
                        }
                    }
                    )
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .setNeutralButton(
                    getResources().getString(R.string.dialogClear),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .setNegativeButton(
                    getResources().getString(R.string.dialogClose),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .create();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_ALL_OBJECTS_LIST, allObjectsList);
        savedInstanceState.putSerializable(KEY_SELECTED_OBJECTS_LIST, selectedObjectsList);
    }

    public void initializeDialog() {
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setVisibility(View.VISIBLE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    execute(selectedObjectsList);
                }
            });

            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setVisibility(View.VISIBLE);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (selectedObjectsList.isEmpty()) {
                        selectedObjectsList = new ArrayList<T>(allObjectsList);
                    } else {
                        selectedObjectsList.clear();
                    }
                    updateListViewSelections();
                    updateDialogTitleAndNeutralButtonLabel();
                }
            });

            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });

            // listview
            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setAdapter(
                    new ArrayAdapter<T>(
                        SelectMultipleObjectsFromListDialog.this.getContext(),
                        android.R.layout.select_dialog_multichoice,
                        allObjectsList));
            updateListViewSelections();
            updateDialogTitleAndNeutralButtonLabel();
        }
    }

    private void updateListViewSelections() {
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            ListView listViewItems = (ListView) dialog.getListView();
            for (T object : allObjectsList) {
                listViewItems.setItemChecked(
                        allObjectsList.indexOf(object),
                        selectedObjectsList.contains(object));
            }
        }
    }

    private void updateDialogTitleAndNeutralButtonLabel() {
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog .setTitle(getDialogTitle());

            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (selectedObjectsList.isEmpty()) {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogSelectAll));
            } else {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogClear));
            }
        }
    }

}
