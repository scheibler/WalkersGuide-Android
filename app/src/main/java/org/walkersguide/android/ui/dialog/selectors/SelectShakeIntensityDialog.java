package org.walkersguide.android.ui.dialog.selectors;

import org.walkersguide.android.data.sensor.ShakeIntensity;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.View;


import org.walkersguide.android.R;

import java.util.ArrayList;
import android.widget.ListView;
import android.widget.AdapterView;
import java.util.Arrays;
import android.widget.ArrayAdapter;


public class SelectShakeIntensityDialog extends DialogFragment {
    public static final String REQUEST_SELECT_SHAKE_INTENSITY = "selectShakeIntensity";
    public static final String EXTRA_SHAKE_INTENSITY = "shakeIntensity";

    // instance constructors

    public static SelectShakeIntensityDialog newInstance(ShakeIntensity selectedShakeIntensity) {
        SelectShakeIntensityDialog dialog = new SelectShakeIntensityDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_SHAKE_INTENSITY, selectedShakeIntensity);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_SELECTED_SHAKE_INTENSITY = "selectedShakeIntensity";

    private ShakeIntensity selectedShakeIntensity;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        selectedShakeIntensity = (ShakeIntensity) getArguments().getSerializable(KEY_SELECTED_SHAKE_INTENSITY);
        return  new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectShakeIntensityDialogTitle))
            .setItems(
                    new String[]{getResources().getString(R.string.messagePleaseWait)},
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
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

    @Override public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {

            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    ShakeIntensity newShakeIntensity = (ShakeIntensity) parent.getItemAtPosition(position);
                    if (newShakeIntensity != null) {
                        Bundle result = new Bundle();
                        result.putSerializable(EXTRA_SHAKE_INTENSITY, newShakeIntensity);
                        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_SHAKE_INTENSITY, result);
                        dismiss();
                    }
                }
            });

            // fill listview
            ArrayList<ShakeIntensity> shakeIntensityList = new ArrayList<ShakeIntensity>(Arrays.asList(ShakeIntensity.values()));
            listViewItems.setAdapter(
                    new ArrayAdapter<ShakeIntensity>(
                        getActivity(), android.R.layout.select_dialog_singlechoice, shakeIntensityList));

            // select list item
            if (selectedShakeIntensity != null) {
                listViewItems.setItemChecked(
                        shakeIntensityList.indexOf(selectedShakeIntensity), true);
            }
        }
    }

}
