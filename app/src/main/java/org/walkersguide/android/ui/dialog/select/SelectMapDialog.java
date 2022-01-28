package org.walkersguide.android.ui.dialog.select;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import org.walkersguide.android.server.wg.status.OSMMap;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.SendFeedbackDialog;

import org.walkersguide.android.ui.dialog.SimpleMessageDialog;

import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.status.ServerStatusTask;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.server.wg.WgException;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.Intent;


public class SelectMapDialog extends DialogFragment {
    public static final String REQUEST_SELECT_MAP = "selectMap";
    public static final String EXTRA_MAP = "map";


    // instance constructors

    public static SelectMapDialog newInstance(OSMMap selectedMap) {
        SelectMapDialog dialog = new SelectMapDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_MAP, selectedMap);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_SELECTED_MAP = "selectedMap";

    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;
    private OSMMap selectedMap;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        selectedMap = (OSMMap) getArguments().getSerializable(KEY_SELECTED_MAP);
        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
        }

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
                                    SendFeedbackDialog.FeedbackToken.MAP_REQUEST)
                                .show(getParentFragmentManager(), "SendFeedbackDialog");
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

    @Override public void onStart() {
        super.onStart();

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
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
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
                    fillListView(
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

    private void fillListView(ServerInstance serverInstance) {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {

            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    OSMMap newMap = (OSMMap) parent.getItemAtPosition(position);
                    if (newMap != null) {
                        Bundle result = new Bundle();
                        result.putSerializable(EXTRA_MAP, newMap);
                        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_MAP, result);
                        dismiss();
                    }
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
                        SelectMapDialog.this.getContext(), android.R.layout.select_dialog_singlechoice, osmMapList));
            // select list item
            if (selectedMap != null) {
                listViewItems.setItemChecked(
                        osmMapList.indexOf(selectedMap), true);
            }
        }
    }

}
