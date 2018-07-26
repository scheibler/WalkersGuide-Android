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


public class SelectShakeIntensityDialog extends DialogFragment {

    // Store instance variables
    private SettingsManager settingsManagerInstance;

    public static SelectShakeIntensityDialog newInstance() {
        SelectShakeIntensityDialog selectShakeIntensityDialogInstance = new SelectShakeIntensityDialog();
        return selectShakeIntensityDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        int indexOfSelectedShakeIntensity = -1;
        String[] formattedShakeIntensityArray = new String[Constants.ShakeIntensityValueArray.length];
        for (int i=0; i<Constants.ShakeIntensityValueArray.length; i++) {
            switch (Constants.ShakeIntensityValueArray[i]) {
                case Constants.SHAKE_INTENSITY.DISABLED:
                    formattedShakeIntensityArray[i] = getResources().getString(R.string.shakeIntensityDisabled);
                    break;
                case Constants.SHAKE_INTENSITY.VERY_WEAK:
                    formattedShakeIntensityArray[i] = getResources().getString(R.string.shakeIntensityVeryWeak);
                    break;
                case Constants.SHAKE_INTENSITY.WEAK:
                    formattedShakeIntensityArray[i] = getResources().getString(R.string.shakeIntensityWeak);
                    break;
                case Constants.SHAKE_INTENSITY.MEDIUM:
                    formattedShakeIntensityArray[i] = getResources().getString(R.string.shakeIntensityMedium);
                    break;
                case Constants.SHAKE_INTENSITY.STRONG:
                    formattedShakeIntensityArray[i] = getResources().getString(R.string.shakeIntensityStrong);
                    break;
                case Constants.SHAKE_INTENSITY.VERY_STRONG:
                    formattedShakeIntensityArray[i] = getResources().getString(R.string.shakeIntensityVeryStrong);
                    break;
                default:
                    formattedShakeIntensityArray[i] = String.valueOf(Constants.ShakeIntensityValueArray[i]);
                    break;
            }
            if (Constants.ShakeIntensityValueArray[i] == settingsManagerInstance.getGeneralSettings().getShakeIntensity()) {
                indexOfSelectedShakeIntensity = i;
            }
        }

        return  new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectShakeIntensityDialogTitle))
            .setSingleChoiceItems(
                    formattedShakeIntensityArray,
                    indexOfSelectedShakeIntensity,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            int selectedShakeIntensity = -1;
                            try {
                                selectedShakeIntensity = Constants.ShakeIntensityValueArray[which];
                            } catch (ArrayIndexOutOfBoundsException e) {
                                selectedShakeIntensity = -1;
                            } finally {
                                if (selectedShakeIntensity > -1) {
                                    settingsManagerInstance.getGeneralSettings().setShakeIntensity(selectedShakeIntensity);
                                    Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                                }
                            }
                            dismiss();
                        }
                    })
        .setNegativeButton(
                getResources().getString(R.string.dialogCancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
        .create();
    }

}
