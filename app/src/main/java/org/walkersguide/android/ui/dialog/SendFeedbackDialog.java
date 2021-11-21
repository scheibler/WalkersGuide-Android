package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.server.util.ServerInstance;
import org.walkersguide.android.server.util.ServerCommunicationException;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.TextView;


import org.json.JSONException;

import org.walkersguide.android.server.util.ServerUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.util.GlobalInstance;

import java.util.concurrent.ExecutorService;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;
import android.text.TextUtils;


public class SendFeedbackDialog extends DialogFragment implements FragmentResultListener {

    public enum Token {
        QUESTION, MAP_REQUEST, PT_PROVIDER_REQUEST
    }

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Future sendFeedbackRequest;
    private InputMethodManager imm;

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


	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imm = (InputMethodManager) GlobalInstance.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SimpleMessageDialog.REQUEST_DIALOG_CLOSED, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SimpleMessageDialog.REQUEST_DIALOG_CLOSED)) {
            if (closeDialog) {
                dismiss();
            }
        }
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
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable("token", token);
        savedInstanceState.putBoolean("closeDialog", closeDialog);
    }

    private void tryToSendFeedback() {
        if (sendFeedbackRequest == null || sendFeedbackRequest.isDone()) {
            sendFeedbackRequest = this.executorService.submit(() -> {
                try {
                    JSONObject jsonServerParams = null;
                    try {
                        jsonServerParams = ServerUtility.createServerParamList();
                        // mandatory
                        jsonServerParams.put(
                                "token", this.token.name().toLowerCase());
                        jsonServerParams.put(
                                "message", editMessage.getText().toString().trim());
                        // optional
                        String sender = editSenderEmailAddress.getText().toString().trim();
                        if (! TextUtils.isEmpty(sender)) {
                            jsonServerParams.put("sender", sender);
                        }
                    } catch (JSONException e) {
                        throw new ServerCommunicationException(Constants.RC.BAD_REQUEST);
                    }

                    HttpsURLConnection connection = null;
                    ServerCommunicationException scException = null;
                    try {
                        ServerInstance serverInstance = ServerUtility.getServerInstance(
                                SettingsManager.getInstance().getServerURL());
                        connection = ServerUtility.connectToServer(
                                String.format(
                                    "%1$s/%2$s", serverInstance.getServerURL(), Constants.SERVER_COMMAND.SEND_FEEDBACK),
                                jsonServerParams);

                    } catch (ServerCommunicationException e) {
                        scException = e;
                    } finally {
                        ServerUtility.cleanUp(connection, null);
                        if (scException != null) {
                            throw scException;
                        }
                    }

                    closeDialog = true;
                    handler.post(() -> {
                        SimpleMessageDialog.newInstance(
                                getResources().getString(R.string.messageSendFeedbackSuccessful))
                            .show(getChildFragmentManager(), "SimpleMessageDialog");
                    });

                } catch (ServerCommunicationException e) {
                    final ServerCommunicationException scException = e;
                    handler.post(() -> {
                        SimpleMessageDialog.newInstance(
                                ServerUtility.getErrorMessageForReturnCode(scException.getReturnCode()))
                            .show(getChildFragmentManager(), "SimpleMessageDialog");
                    });
                }
            });
        }
    }

}
