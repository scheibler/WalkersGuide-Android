package org.walkersguide.android.ui.dialog;

import androidx.activity.OnBackPressedCallback;
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
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;
import org.walkersguide.android.ui.UiHelper;
import timber.log.Timber;

public class SimpleMessageDialog extends DialogFragment {
    public static final String REQUEST_DIALOG_CLOSED = "dialogClosed";
    public static final int PERMISSION_REQUEST_APP_SETTINGS = 2109;


    // instance constructors

    public static SimpleMessageDialog newInstance(String message) {
        SimpleMessageDialog dialog = new SimpleMessageDialog();
        Bundle args = new Bundle();
        args.putString(KEY_MESSAGE, message);
        args.putBoolean(KEY_SHOW_APP_INFO_BUTTON, false);
        dialog.setArguments(args);
        return dialog;
    }

    public static SimpleMessageDialog newInstanceWithAppInfoButton(String message) {
        SimpleMessageDialog dialog = new SimpleMessageDialog();
        Bundle args = new Bundle();
        args.putString(KEY_MESSAGE, message);
        args.putBoolean(KEY_SHOW_APP_INFO_BUTTON, true);
        dialog.setArguments(args);
        return dialog;
    }

    public static SimpleMessageDialog newInstanceWithPublicTransportDataSourceText() {
        SimpleMessageDialog dialog = new SimpleMessageDialog();
        Bundle args = new Bundle();
        args.putBoolean(KEY_PUBLIC_TRANSPORT_DATA_SOURCE_TEXT, true);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_SHOW_APP_INFO_BUTTON = "showAppInfoButton";
    private static final String KEY_PUBLIC_TRANSPORT_DATA_SOURCE_TEXT = "publicTransportDataSourceText";

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_single_text_view, nullParent);
        TextView labelSimpleMessage = (TextView) view.findViewById(R.id.label);
        labelSimpleMessage.setText(
                getArguments().getBoolean(KEY_PUBLIC_TRANSPORT_DATA_SOURCE_TEXT)
                ? UiHelper.getPublicTransportDataSourceText()
                : getArguments().getString(KEY_MESSAGE));

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                close();
            }
        };
        getActivity().getOnBackPressedDispatcher().addCallback(this, callback);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            close();
                        }
                    });

        if (getArguments().getBoolean(KEY_SHOW_APP_INFO_BUTTON)) {
            dialogBuilder.setNeutralButton(
                    getResources().getString(R.string.dialogAppInfo),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent appInfoIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            appInfoIntent.setData(
                                    Uri.fromParts("package", getContext().getPackageName(), null));
                            getActivity().startActivityForResult(
                                    appInfoIntent, PERMISSION_REQUEST_APP_SETTINGS);
                            dialog.dismiss();
                        }
                    });
        }

        return dialogBuilder.create();
    }

    private void close() {
        Timber.d("close");
        Bundle result = new Bundle();
        getParentFragmentManager().setFragmentResult(REQUEST_DIALOG_CLOSED, result);
        dismiss();
    }

}
