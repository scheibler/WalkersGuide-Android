package org.walkersguide.android.ui.dialog.edit;

import org.walkersguide.android.ui.UiHelper;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import androidx.fragment.app.DialogFragment;

import android.text.InputType;

import android.view.inputmethod.EditorInfo;

import android.widget.Button;


import org.walkersguide.android.ui.view.EditTextAndClearInputButton;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.R;
import android.content.Context;

import android.os.Bundle;

import android.view.View;




import org.walkersguide.android.ui.dialog.SimpleMessageDialog;

import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.status.ServerStatusTask;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.server.wg.WgException;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.Intent;


public class ChangeServerUrlDialog extends DialogFragment {
    public static final String REQUEST_SERVER_URL_CHANGED = "serverUrlChanged";
    public static final String EXTRA_URL = "url";


    // instance constructors

    public static ChangeServerUrlDialog newInstance(String selectedUrl) {
        ChangeServerUrlDialog dialog = new ChangeServerUrlDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_NEW_SERVER_URL, selectedUrl);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_NEW_SERVER_URL = "newServerUrl";

    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;

    private EditTextAndClearInputButton layoutServerUrl;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        String newServerUrl;
        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
            newServerUrl = savedInstanceState.getString(KEY_NEW_SERVER_URL);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
            newServerUrl = "";
            if (getArguments() != null) {
                newServerUrl = getArguments().getString(KEY_NEW_SERVER_URL);
            }
        }

        layoutServerUrl = new EditTextAndClearInputButton(getActivity());
        layoutServerUrl.setHint(getResources().getString(R.string.editHintServerURL));
        layoutServerUrl.setInputText(newServerUrl);
        layoutServerUrl.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        layoutServerUrl.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        tryToContactServer();
                    }
                });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.changeServerUrlDialogTitle))
            .setView(layoutServerUrl)
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNeutralButton(
                    getResources().getString(R.string.dialogDefault),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogBack),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            updatePositiveButtonText(
                    serverTaskExecutorInstance.taskInProgress(taskId));
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (serverTaskExecutorInstance.taskInProgress(taskId)) {
                        serverTaskExecutorInstance.cancelTask(taskId);
                    } else {
                        tryToContactServer();
                    }
                }
            });

            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
                        layoutServerUrl.setInputText(BuildConfig.SERVER_URL);
                    }
                }
            });
            buttonNeutral.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
                        layoutServerUrl.setInputText(BuildConfig.SERVER_URL_DEV);
                    }
                    return true;
                }
            });

            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });
        }

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_STATUS_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
        savedInstanceState.putString(KEY_NEW_SERVER_URL, layoutServerUrl.getInputText());
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            serverTaskExecutorInstance.cancelTask(taskId, false);
            UiHelper.hideKeyboard(this);
        }
    }

    private void tryToContactServer() {
        updatePositiveButtonText(true);
        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            taskId = serverTaskExecutorInstance.executeTask(
                    new ServerStatusTask(layoutServerUrl.getInputText()));
        }
    }

    private void updatePositiveButtonText(boolean requestInProgress) {
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setText(
                    requestInProgress
                    ? getResources().getString(R.string.dialogCancel)
                    : getResources().getString(R.string.dialogOK));
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
                    ServerInstance serverInstance = (ServerInstance) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_SERVER_INSTANCE);
                    if (serverInstance != null) {
                        Bundle result = new Bundle();
                        result.putSerializable(EXTRA_URL, serverInstance.getServerURL());
                        getParentFragmentManager().setFragmentResult(REQUEST_SERVER_URL_CHANGED, result);
                        dismiss();
                    }

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    WgException wgException = (WgException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (wgException != null) {
                        SimpleMessageDialog.newInstance(wgException.getMessage())
                            .show(getChildFragmentManager(), "SimpleMessageDialog");
                    }
                }

                updatePositiveButtonText(false);
            }
        }
    };

}
