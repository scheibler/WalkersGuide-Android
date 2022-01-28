package org.walkersguide.android.ui.dialog.edit;

import org.walkersguide.android.data.ObjectWithId;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.inputmethod.EditorInfo;
import android.view.View;

import android.widget.Button;

import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.R;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.ui.UiHelper;
import android.widget.Toast;
import org.walkersguide.android.data.object_with_id.Route;


public class RenameObjectDialog extends DialogFragment {
    public static final String REQUEST_RENAME_OBJECT_SUCCESSFUL = "renameObjectSuccessful";


    // instance constructors

    public static RenameObjectDialog newInstance(ObjectWithId selectedObject) {
        RenameObjectDialog dialog = new RenameObjectDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_OBJECT, selectedObject);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_SELECTED_OBJECT = "selectedObject";
    private static final String KEY_NEW_NAME = "newName";

    private ObjectWithId selectedObject;

    private EditTextAndClearInputButton layoutNewName;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        selectedObject = (ObjectWithId) getArguments().getSerializable(KEY_SELECTED_OBJECT);
        if (selectedObject != null) {
            String newName;
            if(savedInstanceState != null) {
                newName = savedInstanceState.getString(KEY_NEW_NAME);
            } else {
                newName = selectedObject.getName();
            }

            layoutNewName = new EditTextAndClearInputButton(getActivity());
            layoutNewName.setInputText(newName);
            layoutNewName.setEditorAction(
                    EditorInfo.IME_ACTION_DONE,
                    new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                        @Override public void onSelectedActionClicked() {
                            tryToRenameObject();
                        }
                    });

            String dialogTitle;
            if (selectedObject instanceof Point) {
                dialogTitle = getResources().getString(R.string.renamePointDialogTitle);
            } else if (selectedObject instanceof Route) {
                dialogTitle = getResources().getString(R.string.renameRouteDialogTitle);
            } else if (selectedObject instanceof Segment) {
                dialogTitle = getResources().getString(R.string.renameSegmentDialogTitle);
            } else {
                dialogTitle = "";
            }

            return new AlertDialog.Builder(getActivity())
                .setTitle(dialogTitle)
                .setView(layoutNewName)
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
        return null;
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    tryToRenameObject();
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
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(KEY_NEW_NAME, layoutNewName.getInputText());
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }

    private void tryToRenameObject() {
        if (selectedObject.rename(layoutNewName.getInputText())) {
            // send result via fragment result api and dismiss
            Bundle result = new Bundle();
            getParentFragmentManager().setFragmentResult(REQUEST_RENAME_OBJECT_SUCCESSFUL, result);
            dismiss();
        } else {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageRenameObjectFailed),
                    Toast.LENGTH_LONG).show();
        }
    }

}
