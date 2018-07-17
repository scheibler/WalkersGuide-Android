package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.R;
import org.walkersguide.android.data.server.PublicTransportProvider;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;

public class SelectIndirectionFactorDialog extends DialogFragment {

    // Store instance variables
    private SettingsManager settingsManagerInstance;

    public static SelectIndirectionFactorDialog newInstance() {
        SelectIndirectionFactorDialog selectIndirectionFactorDialog = new SelectIndirectionFactorDialog();
        return selectIndirectionFactorDialog;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] formattedIndirectionFactorArray = new String[Constants.IndirectionFactorValueArray.length];
        int indexOfSelectedIndirectionFactor = -1;
        int index = 0;
        for (double indirectionFactor : Constants.IndirectionFactorValueArray) {
            formattedIndirectionFactorArray[index] = String.format("%1$.1f", indirectionFactor);
            if (indirectionFactor == settingsManagerInstance.getRouteSettings().getIndirectionFactor()) {
                indexOfSelectedIndirectionFactor = index;
            }
            index += 1;
        }

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectIndirectionFactorDialogTitle))
            .setSingleChoiceItems(
                    formattedIndirectionFactorArray,
                    indexOfSelectedIndirectionFactor,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            double newIndirectionFactor = -1.0;
                            try {
                                newIndirectionFactor = Constants.IndirectionFactorValueArray[which];
                            } catch (IndexOutOfBoundsException e) {
                                newIndirectionFactor = -1.0;
                            } finally {
                                if (newIndirectionFactor != -1.0) {
                                    settingsManagerInstance.getRouteSettings().setIndirectionFactor(newIndirectionFactor);
                                    Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                                }
                            }
                            dismiss();
                        }
                    }
                    )
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    }
                    )
            .create();
    }

}
