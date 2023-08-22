package org.walkersguide.android.ui.dialog.select;

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


public class EnterRouteNameDialog extends DialogFragment {
    public static final String REQUEST_ENTER_ROUTE_NAME = "requestEnterRouteName";
    public static final String EXTRA_ROUTE_NAME = "extraRouteName";


    public static EnterRouteNameDialog newInstance() {
        EnterRouteNameDialog dialog = new EnterRouteNameDialog();
        return dialog;
    }


    // dialog
    private static final String KEY_ROUTE_NAME = "routeName";

    private EditTextAndClearInputButton layoutRouteName;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        layoutRouteName = new EditTextAndClearInputButton(getActivity());
        layoutRouteName.setInputText(
                savedInstanceState != null
                ? savedInstanceState.getString(KEY_ROUTE_NAME)
                : "");
        layoutRouteName.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        tryToCloseDialog();
                    }
                });

        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.enterRouteNameDialogTitle))
            .setView(layoutRouteName)
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
                    tryToCloseDialog();
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

        layoutRouteName.showKeyboard();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(KEY_ROUTE_NAME, layoutRouteName.getInputText());
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }

    private void tryToCloseDialog() {
        String routeName = layoutRouteName.getInputText();
        if (TextUtils.isEmpty(routeName)) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageRecordedRouteNameIsMissing),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Bundle result = new Bundle();
        result.putSerializable(EXTRA_ROUTE_NAME, routeName);
        getParentFragmentManager().setFragmentResult(REQUEST_ENTER_ROUTE_NAME, result);
        dismiss();
    }

}
