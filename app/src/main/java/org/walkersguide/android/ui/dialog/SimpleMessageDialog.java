package org.walkersguide.android.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;

import android.support.v4.app.DialogFragment;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import org.walkersguide.android.R;

public class SimpleMessageDialog extends DialogFragment {

    public interface ChildDialogCloseListener {
        public void childDialogClosed();
    }

    private ChildDialogCloseListener childDialogCloseListener;

    public static SimpleMessageDialog newInstance(String message) {
        SimpleMessageDialog simpleMessageDialogInstance = new SimpleMessageDialog();
        Bundle args = new Bundle();
        args.putString("message", message);
        simpleMessageDialogInstance.setArguments(args);
        return simpleMessageDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof ChildDialogCloseListener) {
            childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
        } else if (context instanceof Activity
                && (Activity) context instanceof ChildDialogCloseListener) {
            childDialogCloseListener = (ChildDialogCloseListener) context;
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_single_text_view, nullParent);
		TextView labelSimpleMessage = (TextView) view.findViewById(R.id.label);
        labelSimpleMessage.setText(getArguments().getString("message"));
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

    @Override public void onStop() {
        super.onStop();
        childDialogCloseListener = null;
    }

    private void close() {
        if (childDialogCloseListener != null) {
            childDialogCloseListener.childDialogClosed();
        }
        dismiss();
    }

}
