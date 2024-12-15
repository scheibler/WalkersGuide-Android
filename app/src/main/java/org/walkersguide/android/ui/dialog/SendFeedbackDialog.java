package org.walkersguide.android.ui.dialog;

import android.widget.Toast;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.SendFeedbackTask;
import org.walkersguide.android.ui.UiHelper;
import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.server.wg.WgException;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;



import org.walkersguide.android.R;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;

import timber.log.Timber;
import android.text.TextUtils;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;


public class SendFeedbackDialog extends DialogFragment implements FragmentResultListener {
    public static final String REQUEST_MESSAGE_SENT_SUCCESSFUL = "request.messageSentSuccessful";

    public enum FeedbackToken {
        QUESTION, MAP_REQUEST, PT_PROVIDER_REQUEST
    }


    private static final String KEY_TOKEN = "token";
    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_CLOSE_DIALOG = "closeDialog";

    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;

    private FeedbackToken token;
    private boolean closeDialog;

    private EditTextAndClearInputButton layoutSender, layoutMessage;

    public static SendFeedbackDialog newInstance(FeedbackToken token) {
        SendFeedbackDialog dialog = new SendFeedbackDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_TOKEN, token);
        dialog.setArguments(args);
        return dialog;
    }


    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
        getChildFragmentManager()
            .setFragmentResultListener(
                    SimpleMessageDialog.REQUEST_DIALOG_CLOSED, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SimpleMessageDialog.REQUEST_DIALOG_CLOSED)) {
            if (closeDialog) {
                getParentFragmentManager().setFragmentResult(REQUEST_MESSAGE_SENT_SUCCESSFUL, new Bundle());
                dismiss();
            }
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        token = (FeedbackToken) getArguments().getSerializable(KEY_TOKEN);
        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
            closeDialog = savedInstanceState.getBoolean(KEY_CLOSE_DIALOG);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
            closeDialog = false;
        }

        String dialogTitle = null;
        if (token != null) {
            switch (this.token) {
                case MAP_REQUEST:
                    dialogTitle = getResources().getString(R.string.sendFeedbackDialogTitleMissingMap);
                    break;
                case PT_PROVIDER_REQUEST:
                    dialogTitle = getResources().getString(R.string.sendFeedbackDialogTitleMissingPtProvider);
                    break;
                case QUESTION:
                    dialogTitle = getResources().getString(R.string.sendFeedbackDialogTitle);
                    break;
            }
        }
        if (dialogTitle == null) {
            return null;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_send_feedback, nullParent);

        layoutSender = (EditTextAndClearInputButton) view.findViewById(R.id.layoutSender);
        layoutSender.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layoutSender.setLabelText(
                getResources().getString(R.string.labelSenderEmailAddress));
        layoutSender.setEditorAction(
                EditorInfo.IME_ACTION_NEXT,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        layoutMessage.requestFocus();
                    }
                });

        layoutMessage = (EditTextAndClearInputButton) view.findViewById(R.id.layoutMessage);
        switch (token) {
            case MAP_REQUEST:
                layoutMessage.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
                layoutMessage.setLabelText(
                        getResources().getString(R.string.labelMessageMissingMap));
                break;
            case PT_PROVIDER_REQUEST:
                layoutMessage.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
                layoutMessage.setLabelText(
                        getResources().getString(R.string.labelMessageMissingPtProvider));
                break;
            case QUESTION:
                layoutMessage.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                layoutMessage.setLabelText(
                        getResources().getString(R.string.labelMessage));
                break;
        }
        layoutMessage.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        tryToSendFeedback();
                    }
                });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(dialogTitle)
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogSend),
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
                        tryToSendFeedback();
                    }
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
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SEND_FEEDBACK_TASK_SUCCESSFUL);
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
        savedInstanceState.putBoolean(KEY_CLOSE_DIALOG, closeDialog);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            Timber.d("onDestroy");
            serverTaskExecutorInstance.cancelTask(taskId);
            UiHelper.hideKeyboard(this);
        }
    }

    private void tryToSendFeedback() {
        final String sender = layoutSender.getInputText();
        final String message = layoutMessage.getInputText();
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageFeedbackMessageIsEmpty),
                    Toast.LENGTH_LONG).show();
            return;
        }

        updatePositiveButtonText(true);
        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            taskId = serverTaskExecutorInstance.executeTask(
                    new SendFeedbackTask(token, sender, message));
        }
    }

    private void updatePositiveButtonText(boolean requestInProgress) {
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setText(
                    requestInProgress
                    ? getResources().getString(R.string.dialogCancel)
                    : getResources().getString(R.string.dialogSend));
        }
    }


    // background task results

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_SEND_FEEDBACK_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    Timber.e("wrong task");
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_SEND_FEEDBACK_TASK_SUCCESSFUL)) {
                    closeDialog = true;
                    SimpleMessageDialog.newInstance(
                            context.getResources().getString(R.string.messageSendFeedbackSuccessful))
                        .show(getChildFragmentManager(), "SimpleMessageDialog");

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
