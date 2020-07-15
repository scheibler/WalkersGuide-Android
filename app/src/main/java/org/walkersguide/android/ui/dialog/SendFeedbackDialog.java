package org.walkersguide.android.ui.dialog;

import timber.log.Timber;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;

import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.server.ServerStatusManager.SendFeedbackListener;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog.ChildDialogCloseListener;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;


public class SendFeedbackDialog extends DialogFragment
    implements ChildDialogCloseListener, SendFeedbackListener {

    public enum Token {
        QUESTION, MAP_REQUEST, PT_PROVIDER_REQUEST
    }

    private InputMethodManager imm;
    private ServerStatusManager serverStatusManagerInstance;

    private Token token;
    private boolean closeDialog;

    private EditText editSenderEmailAddress, editMessage;
    private TextView labelMessage;

    public static SendFeedbackDialog newInstance(Token token) {
        SendFeedbackDialog sendFeedbackDialogInstance = new SendFeedbackDialog();
        Bundle args = new Bundle();
        args.putSerializable("token", token);
        sendFeedbackDialogInstance.setArguments(args);
        return sendFeedbackDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        serverStatusManagerInstance = ServerStatusManager.getInstance(context);
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            token = (Token) savedInstanceState.getSerializable("token");
            closeDialog = savedInstanceState.getBoolean("closeDialog");
        } else {
            token = (Token) getArguments().getSerializable("token");
            closeDialog = false;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_send_feedback, nullParent);

        editSenderEmailAddress = (EditText) view.findViewById(R.id.editSenderEmailAddress);
        editMessage = (EditText) view.findViewById(R.id.editMessage);
        editMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    tryToSendFeedback();
                    return true;
                }
                return false;
            }
        });

        String dialogTitle = null;
        TextView labelMessage = (TextView) view.findViewById(R.id.labelMessage);
        switch (this.token) {
            case MAP_REQUEST:
                dialogTitle = getResources().getString(R.string.sendFeedbackDialogTitleMissingMap);
                labelMessage.setText(
                        getResources().getString(R.string.labelMessageMissingMap));
                break;
            case PT_PROVIDER_REQUEST:
                dialogTitle = getResources().getString(R.string.sendFeedbackDialogTitleMissingPtProvider);
                labelMessage.setText(
                        getResources().getString(R.string.labelMessageMissingPtProvider));
                break;
            default:
                dialogTitle = getResources().getString(R.string.sendFeedbackDialogTitle);
                labelMessage.setText(
                        getResources().getString(R.string.labelMessage));
                editMessage.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
                editMessage.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                editMessage.setSingleLine(false);
                break;
        }

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(dialogTitle)
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
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
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    tryToSendFeedback();
                }
            });
            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dialog.dismiss();
                }
            });
        }
    }

    @Override public void onStop() {
        super.onStop();
        serverStatusManagerInstance.invalidateSendFeedbackRequest(SendFeedbackDialog.this);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable("token", token);
        savedInstanceState.putBoolean("closeDialog", closeDialog);
    }

    @Override public void childDialogClosed() {
        if (closeDialog) {
            dismiss();
        }
    }

    private void tryToSendFeedback() {
        serverStatusManagerInstance.requestSendFeedback(
                SendFeedbackDialog.this,
                this.token.name().toLowerCase(),
                editMessage.getText().toString().trim(),
                editSenderEmailAddress.getText().toString().trim());
    }

	@Override public void sendFeedbackRequestFinished(int returnCode) {
        String returnMessage = null;
        if (returnCode == Constants.RC.OK) {
            closeDialog = true;
            returnMessage = getResources().getString(R.string.messageSendFeedbackSuccessful);
        } else {
            returnMessage = ServerUtility.getErrorMessageForReturnCode(
                    SendFeedbackDialog.this.getContext(), returnCode);
        }
        // show dialog
        if (isAdded()) {
            SimpleMessageDialog simpleMessageDialogInstance = SimpleMessageDialog.newInstance(returnMessage);
            simpleMessageDialogInstance.setTargetFragment(SendFeedbackDialog.this, 1);
            simpleMessageDialogInstance.show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
        }
    }

}
