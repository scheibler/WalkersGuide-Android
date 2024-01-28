package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.util.WalkersGuideService;
import org.walkersguide.android.R;
import org.walkersguide.android.BuildConfig;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;


public class ChangelogDialog extends DialogFragment {
    public static final int VERSION_CODE = 27;

    public static ChangelogDialog newInstance() {
        ChangelogDialog dialog = new ChangelogDialog();
        return dialog;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_changelog, nullParent);

        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.changelogDialogTitle))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNeutralButton(
                    getResources().getString(R.string.buttonOpenUserManual),
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
                    dismiss();
                }
            });

            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(
                            Uri.parse(
                                getResources().getString(R.string.variableUserManualUrl)));
                    getActivity().startActivity(i);
                }
            });
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            WalkersGuideService.startService();
        }
    }

}
