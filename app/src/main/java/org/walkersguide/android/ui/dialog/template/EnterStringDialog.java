package org.walkersguide.android.ui.dialog.template;

import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import android.text.TextUtils;

import android.view.View;
import android.view.inputmethod.EditorInfo;

import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.fragment.app.DialogFragment;

import org.walkersguide.android.R;
import org.walkersguide.android.ui.UiHelper;
import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.util.GlobalInstance;
import android.text.InputType;


public abstract class EnterStringDialog extends DialogFragment {

    private String initialInput = "";
    public void setInitialInput(String initialInput) {
        this.initialInput = initialInput;
    }

    private boolean multiLine = false;
    public void setMultiLine(boolean multiLine) {
        this.multiLine = multiLine;
    }

    private String dialogTitle = "";
    public void setDialogTitle(String dialogTitle) {
        this.dialogTitle = dialogTitle;
    }

    private String positiveButtonText = GlobalInstance.getStringResource(R.string.dialogOK);
    public void setPositiveButtonText(String positiveButtonText) {
        this.positiveButtonText = positiveButtonText;
    }

    private String missingInputMessage = "";
    public void setMissingInputMessage(String missingInputMessage) {
        this.missingInputMessage = missingInputMessage;
    }

    private String neutralButtonLabel = "", newTextToBeInsertedOnNeutralButtonClick = "";
    public void setNeutralButton(String buttonLabel, String newTextToBeInsertedOnClick) {
        this.neutralButtonLabel = buttonLabel;
        this.newTextToBeInsertedOnNeutralButtonClick = newTextToBeInsertedOnClick;
    }


    // dialog
    private static final String KEY_INPUT = "input";

    private EditTextAndClearInputButton layoutInput;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        layoutInput = new EditTextAndClearInputButton(getActivity());
        if (this.multiLine) {
            layoutInput.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        }
        layoutInput.setInputText(
                savedInstanceState != null
                ? savedInstanceState.getString(KEY_INPUT)
                : initialInput);
        layoutInput.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        checkInput();
                    }
                });

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
            .setTitle(dialogTitle)
            .setView(layoutInput)
            .setPositiveButton(
                    positiveButtonText,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

        if (! TextUtils.isEmpty(neutralButtonLabel)) {
            dialogBuilder.setNeutralButton(
                    neutralButtonLabel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
        }

        return dialogBuilder.create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    checkInput();
                }
            });

            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    layoutInput.setInputText(newTextToBeInsertedOnNeutralButtonClick);
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

        layoutInput.showKeyboard();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(KEY_INPUT, layoutInput.getInputText());
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }

    private void checkInput() {
        String input = layoutInput.getInputText();
        if (TextUtils.isEmpty(input)
                && ! TextUtils.isEmpty(missingInputMessage)) {
            Toast.makeText(
                    getActivity(),
                    missingInputMessage,
                    Toast.LENGTH_LONG).show();
        } else {
            execute(input);
        }
    }

    public abstract void execute(String input);

}
