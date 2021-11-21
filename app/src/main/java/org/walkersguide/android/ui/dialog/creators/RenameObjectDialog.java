package org.walkersguide.android.ui.dialog.creators;

import android.widget.LinearLayout.LayoutParams;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;
import android.widget.EditText;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.ui.listener.TextChangedListener;
import android.text.Editable;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatActivity;
import org.walkersguide.android.data.basic.segment.Segment;
import timber.log.Timber;
import android.text.InputType;

public class RenameObjectDialog extends DialogFragment {
    public static final String REQUEST_RENAME_OBJECT = "renameObject";
    public static final String EXTRA_SUCCESSFUL = "successful";


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
    private static final String KEY_CUSTOM_NAME = "customName";

    private ObjectWithId selectedObject;
    private String customName;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        selectedObject = (ObjectWithId) getArguments().getSerializable(KEY_SELECTED_OBJECT);
        if (selectedObject != null) {
            if(savedInstanceState != null) {
                customName = savedInstanceState.getString(KEY_CUSTOM_NAME);
            } else {
                customName = selectedObject.getName();
            }

            EditText editCustomName = new EditText(RenameObjectDialog.this.getContext());
            LayoutParams lp = new LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            editCustomName.setLayoutParams(lp);
            editCustomName.setText(customName);
            editCustomName.selectAll();
            //editCustomName.setInputType(InputType.TYPE_TEXT_VARIATION_NORMAL);
            //editCustomName.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editCustomName.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            editCustomName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        InputMethodManager imm =(InputMethodManager) GlobalInstance.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        tryToRenameObject();
                        return true;
                    }
                    return false;
                }
            });
            editCustomName.addTextChangedListener(new TextChangedListener<EditText>(editCustomName) {
                @Override public void onTextChanged(EditText view, Editable s) {
                    customName = view.getText().toString();
                }
            });

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(
                        selectedObject instanceof Point
                        ? getResources().getString(R.string.renamePointDialogTitle)
                        : getResources().getString(R.string.renameSegmentDialogTitle))
                .setView(editCustomName)
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
                    dialog.dismiss();
                }
            });
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(KEY_CUSTOM_NAME, customName);
    }

    private void tryToRenameObject() {
        boolean successful = false;
        if (selectedObject instanceof Point) {
            successful = AccessDatabase.getInstance().addPoint((Point) selectedObject, customName);
        } else if (selectedObject instanceof Segment) {
            successful = AccessDatabase.getInstance().addSegment((Segment) selectedObject, customName);
        }
        // send result via fragment result api
        Bundle result = new Bundle();
        result.putBoolean(EXTRA_SUCCESSFUL, successful);
        getParentFragmentManager().setFragmentResult(REQUEST_RENAME_OBJECT, result);
        // additionally send a broadcast (required by TextViewAndActionButton)
        Intent intent = new Intent(REQUEST_RENAME_OBJECT);
        intent.putExtra(EXTRA_SUCCESSFUL, successful);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        // disniss dialog
        dismiss();
    }

}
