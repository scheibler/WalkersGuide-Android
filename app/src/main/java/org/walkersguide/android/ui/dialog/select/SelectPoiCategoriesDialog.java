package org.walkersguide.android.ui.dialog.select;

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
import org.walkersguide.android.ui.dialog.template.SelectMultipleObjectsFromListDialog;


public class SelectPoiCategoriesDialog extends SelectMultipleObjectsFromListDialog<PoiCategory> {
    public static final String REQUEST_SELECT_POI_CATEGORIES = "selectPoiCategories";
    public static final String EXTRA_POI_CATEGORY_LIST = "poiCategoryList";


    // instance constructors

    public static SelectPoiCategoriesDialog newInstance(ArrayList<PoiCategory> selectedPoiCategoryList) {
        SelectPoiCategoriesDialog dialog= new SelectPoiCategoriesDialog();
        dialog.setArguments(
                createInitialObjectListBundle(
                    null, selectedPoiCategoryList));
        return dialog;
    }


    // dialog
    private static final String KEY_TASK_ID = "taskId";

    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
        taskId = savedInstanceState != null
            ? savedInstanceState.getLong(KEY_TASK_ID)
            : ServerTaskExecutor.NO_TASK_ID;
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            // hide positive and neutral buttons for now
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setVisibility(View.GONE);
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setVisibility(View.GONE);
        }

        if (hasAllObjectsList()) {
            initializeDialog();

        } else {
            IntentFilter localIntentFilter = new IntentFilter();
            localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_STATUS_TASK_SUCCESSFUL);
            localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
            localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);

            if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
                taskId = serverTaskExecutorInstance.executeTask(new ServerStatusTask());
            }
        }
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
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
                    updateAllObjectsList(
                            ((ServerInstance) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_SERVER_INSTANCE))
                                .getSupportedPoiCategoryList()
                            );
                    initializeDialog();

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

    @Override public String getDialogTitleNothingSelected() {
        return getResources().getString(R.string.selectPoiCategoriesDialogTitle);
    }

    @Override public int getPluralResourceId() {
        return R.plurals.poiCategorySelected;
    }

    @Override public void execute(ArrayList<PoiCategory> selectedPoiCategoryList) {
        Bundle result = new Bundle();
        result.putSerializable(EXTRA_POI_CATEGORY_LIST, selectedPoiCategoryList);
        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_POI_CATEGORIES, result);
        dismiss();
    }

}
