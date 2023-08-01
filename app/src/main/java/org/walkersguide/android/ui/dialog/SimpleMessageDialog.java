package org.walkersguide.android.ui.dialog;

import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import org.walkersguide.android.R;

public class SimpleMessageDialog extends DialogFragment {
    public static final String REQUEST_DIALOG_CLOSED = "dialogClosed";


    // instance constructors

    public static SimpleMessageDialog newInstance(String message) {
        SimpleMessageDialog dialog = new SimpleMessageDialog();
        Bundle args = new Bundle();
        args.putString(KEY_MESSAGE, message);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_MESSAGE = "message";

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_single_text_view, nullParent);
		TextView labelSimpleMessage = (TextView) view.findViewById(R.id.label);
        labelSimpleMessage.setText(
                getArguments().getString(KEY_MESSAGE));

        return new AlertDialog.Builder(getActivity())
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            close();
                        }
                    })
            .setOnKeyListener(
                    new Dialog.OnKeyListener() {
                        @Override public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                close();
                                return true;
                            }
                            return false;
                        }
                    })
            .create();
    }

    private void close() {
        Bundle result = new Bundle();
        getParentFragmentManager().setFragmentResult(REQUEST_DIALOG_CLOSED, result);
        dismiss();
    }

}
