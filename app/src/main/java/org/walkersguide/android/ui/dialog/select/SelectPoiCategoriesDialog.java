package org.walkersguide.android.ui.dialog.select;

import org.walkersguide.android.R;
import org.walkersguide.android.server.wg.poi.PoiCategory;
import android.app.AlertDialog;
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


public class SelectPoiCategoriesDialog extends DialogFragment {
    public static final String REQUEST_SELECT_POI_CATEGORIES = "selectPoiCategories";
    public static final String EXTRA_POI_CATEGORY_LIST = "poiCategoryList";


    // instance constructors

    public static SelectPoiCategoriesDialog newInstance(ArrayList<PoiCategory> selectedPoiCategoryList) {
        SelectPoiCategoriesDialog dialog= new SelectPoiCategoriesDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_POI_CATEGORY_LIST, selectedPoiCategoryList);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_SELECTED_POI_CATEGORY_LIST = "selectedPoiCategoryList";

    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;
    private ArrayList<PoiCategory> supportedPoiCategoryList, selectedPoiCategoryList;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        supportedPoiCategoryList = null;
        if(savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
            selectedPoiCategoryList = (ArrayList<PoiCategory>) savedInstanceState.getSerializable(KEY_SELECTED_POI_CATEGORY_LIST);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
            selectedPoiCategoryList = (ArrayList<PoiCategory>) getArguments().getSerializable(KEY_SELECTED_POI_CATEGORY_LIST);
        }

        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectPoiCategoriesDialogTitle))
            .setMultiChoiceItems(
                    new String[]{getResources().getString(R.string.messagePleaseWait)},
                    null,
                    new DialogInterface.OnMultiChoiceClickListener() { 
                        @Override public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            PoiCategory selectedPoiCategory = null;
                            try {
                                selectedPoiCategory = supportedPoiCategoryList.get(which);
                            } catch (IndexOutOfBoundsException | NullPointerException e) {
                                selectedPoiCategory = null;
                            } finally {
                                if (selectedPoiCategory != null) {
                                    // add or remove
                                    if (selectedPoiCategoryList.contains(selectedPoiCategory)) {
                                        selectedPoiCategoryList.remove(selectedPoiCategory);
                                    } else {
                                        selectedPoiCategoryList.add(selectedPoiCategory);
                                    }
                                    updateNeutralButtonLabel();
                                }
                            }
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

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            // hide neutral button for now
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setVisibility(View.GONE);
        }

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_STATUS_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);

        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            taskId = serverTaskExecutorInstance.executeTask(new ServerStatusTask());
        }
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
        savedInstanceState.putSerializable(KEY_SELECTED_POI_CATEGORY_LIST, selectedPoiCategoryList);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            Bundle result = new Bundle();
            result.putSerializable(EXTRA_POI_CATEGORY_LIST, selectedPoiCategoryList);
            getParentFragmentManager().setFragmentResult(REQUEST_SELECT_POI_CATEGORIES, result);
            // stop server instance request, if running
            serverTaskExecutorInstance.cancelTask(taskId, false);
        }
    }

    // background task results

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_STATUS_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_STATUS_TASK_SUCCESSFUL)) {
                    initializeDialog(
                            (ServerInstance) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_SERVER_INSTANCE));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    WgException wgException = (WgException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (wgException != null) {
                        SimpleMessageDialog.newInstance(wgException.getMessage())
                            .show(getChildFragmentManager(), "SimpleMessageDialog");
                    }
                }
            }
        }
    };

    private void initializeDialog(ServerInstance serverInstance) {
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            supportedPoiCategoryList = serverInstance.getSupportedPoiCategoryList();

            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setVisibility(View.VISIBLE);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (selectedPoiCategoryList.isEmpty()) {
                        selectedPoiCategoryList = new ArrayList<PoiCategory>(supportedPoiCategoryList);
                    } else {
                        selectedPoiCategoryList.clear();
                    }
                    updateListViewSelections();
                    updateNeutralButtonLabel();
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
                    new ArrayAdapter<PoiCategory>(
                        SelectPoiCategoriesDialog.this.getContext(),
                        android.R.layout.select_dialog_multichoice,
                        supportedPoiCategoryList));
            updateListViewSelections();
            updateNeutralButtonLabel();
        }
    }

    private void updateListViewSelections() {
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            ListView listViewItems = (ListView) dialog.getListView();
            for (PoiCategory category : supportedPoiCategoryList) {
                listViewItems.setItemChecked(
                        supportedPoiCategoryList.indexOf(category),
                        selectedPoiCategoryList.contains(category));
            }
        }
    }

    private void updateNeutralButtonLabel() {
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (selectedPoiCategoryList.isEmpty()) {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogSelectAll));
            } else {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogClear));
            }
        }
    }

}
