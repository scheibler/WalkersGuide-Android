package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.R;
import org.walkersguide.android.google.AddressManager;
import org.walkersguide.android.listener.AddressListener;
import org.walkersguide.android.util.Constants;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class EnterAddressDialog extends DialogFragment implements AddressListener {

    // Store instance variables
    private InputMethodManager imm;
    private AddressManager addressManagerRequest;
    private int pointPutInto;
    private EditText editAddress;

    public static EnterAddressDialog newInstance(int pointPutInto) {
        EnterAddressDialog enterAddressDialogInstance = new EnterAddressDialog();
        Bundle args = new Bundle();
        args.putInt("pointPutInto", pointPutInto);
        enterAddressDialogInstance.setArguments(args);
        return enterAddressDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        addressManagerRequest = null;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        pointPutInto = getArguments().getInt("pointPutInto");

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_single_edit_text, nullParent);

        editAddress = (EditText) view.findViewById(R.id.editInput);
        editAddress.setHint(getResources().getString(R.string.editHintAddress));
        editAddress.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    tryToGetCoordinatesForAddress();
                    return true;
                }
                return false;
            }
        });

        ImageButton buttonDelete = (ImageButton) view.findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // clear edit text
                editAddress.setText("");
                // show keyboard
                imm.showSoftInput(editAddress, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.enterAddressDialogName))
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
                    tryToGetCoordinatesForAddress();
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
        // show keyboard
        new Handler().postDelayed(
                new Runnable() {
                    @Override public void run() {
                        imm.showSoftInput(editAddress, InputMethodManager.SHOW_IMPLICIT);
                    }
                }, 50);
    }

    private void tryToGetCoordinatesForAddress() {
        String address = editAddress.getText().toString();
        if (address.equals("")) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageAddressMissing),
                    Toast.LENGTH_LONG).show();
        } else {
            addressManagerRequest = new AddressManager(
                    getActivity(), EnterAddressDialog.this, pointPutInto, address);
            addressManagerRequest.execute();
        }
    }

    @Override public void addressRequestFinished(int returnCode, String returnMessage) {
        if (returnCode == Constants.ID.OK) {
            Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            dismiss();
        } else {
            SimpleMessageDialog.newInstance(returnMessage)
                .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
        }
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (addressManagerRequest != null
                && addressManagerRequest.getStatus() != AsyncTask.Status.FINISHED) {
            addressManagerRequest.cancel();
        }
    }

}
