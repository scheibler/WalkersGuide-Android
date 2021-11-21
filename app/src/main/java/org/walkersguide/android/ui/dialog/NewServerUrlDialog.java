package org.walkersguide.android.ui.dialog;

import android.widget.LinearLayout.LayoutParams;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import androidx.fragment.app.DialogFragment;

import android.text.InputType;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;

import android.widget.Button;
import android.widget.EditText;


import org.walkersguide.android.ui.listener.TextChangedListener;
import android.text.Editable;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.server.util.ServerInstance;
import org.walkersguide.android.R;
import android.content.Context;

import android.os.Bundle;

import android.view.View;

import android.widget.TextView;


import java.util.concurrent.ExecutorService;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.walkersguide.android.server.util.ServerCommunicationException;
import org.walkersguide.android.server.util.ServerUtility;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;


public class NewServerUrlDialog extends DialogFragment {
    public static final String REQUEST_NEW_SERVER_URL = "newServerUrl";
    public static final String EXTRA_URL = "url";


    // instance constructors

    public static NewServerUrlDialog newInstance(String selectedUrl) {
        NewServerUrlDialog dialog = new NewServerUrlDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_URL, selectedUrl);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_SELECTED_URL = "selectedUrl";

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Future getServerInstanceRequest;

    private String selectedUrl;

    private EditText editServerURL;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            selectedUrl = savedInstanceState.getString(KEY_SELECTED_URL);
        } else {
            selectedUrl = getArguments().getString(KEY_SELECTED_URL);
        }

        editServerURL = new EditText(NewServerUrlDialog.this.getContext());
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        editServerURL.setLayoutParams(lp);
        editServerURL.setText(selectedUrl);
        editServerURL.selectAll();
        editServerURL.setHint(getResources().getString(R.string.editHintServerURL));
        editServerURL.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editServerURL.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editServerURL.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm =(InputMethodManager) GlobalInstance.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    tryToContactServer();
                    return true;
                }
                return false;
            }
        });
        editServerURL.addTextChangedListener(new TextChangedListener<EditText>(editServerURL) {
            @Override public void onTextChanged(EditText view, Editable s) {
                selectedUrl = view.getText().toString();
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.newServerDialogTitle))
            .setView(editServerURL)
            .setPositiveButton(
                    getResources().getString(R.string.dialogDone),
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
                    tryToContactServer();
                }
            });
            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    editServerURL.setText(BuildConfig.SERVER_URL);
                }
            });
            buttonNeutral.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    editServerURL.setText(BuildConfig.SERVER_URL_DEV);
                    return true;
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
        savedInstanceState.putString(KEY_SELECTED_URL, selectedUrl);
    }

    private void tryToContactServer() {
        if (getServerInstanceRequest == null || getServerInstanceRequest.isDone()) {
            getServerInstanceRequest = this.executorService.submit(() -> {
                try {
                    final ServerInstance serverInstance = ServerUtility.getServerInstance(selectedUrl);
                    handler.post(() -> {
                        Bundle result = new Bundle();
                        result.putSerializable(EXTRA_URL, selectedUrl);
                        getParentFragmentManager().setFragmentResult(REQUEST_NEW_SERVER_URL, result);
                        dismiss();
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
