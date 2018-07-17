package org.walkersguide.android.ui.dialog;

import android.widget.LinearLayout;
import android.view.ViewGroup.LayoutParams;
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
import android.widget.Toast;
import java.util.ArrayList;
import org.walkersguide.android.ui.view.CheckBoxGroupView;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Button;
import java.util.Arrays;


public class SelectRoutingWayClassesDialog extends DialogFragment {

    // Store instance variables
    private SettingsManager settingsManagerInstance;
    private CheckBoxGroupView checkBoxGroupRoutingWayClasses;

    public static SelectRoutingWayClassesDialog newInstance() {
        SelectRoutingWayClassesDialog selectRoutingWayClassesDialog = new SelectRoutingWayClassesDialog();
        return selectRoutingWayClassesDialog;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_single_check_box_group, nullParent);

        checkBoxGroupRoutingWayClasses = (CheckBoxGroupView) view.findViewById(R.id.checkBoxGroup);
        for (String routingWayClassTag : Constants.RoutingWayClassValueArray) {
            CheckBox checkBox = new CheckBox(getActivity());
            checkBox.setId(routingWayClassTag.hashCode());
            checkBox.setLayoutParams(
                    new LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                    );
            checkBox.setChecked(
                    settingsManagerInstance.getRouteSettings().getWayClassList().contains(routingWayClassTag));
            checkBox.setTag(routingWayClassTag);
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    onStart();
                }
            });
            if (routingWayClassTag.equals(Constants.ROUTING_WAY_CLASS.BIG_STREETS)) {
                checkBox.setText(
                        getResources().getString(R.string.routingWayClassBigStreets));
            } else if (routingWayClassTag.equals(Constants.ROUTING_WAY_CLASS.SMALL_STREETS)) {
                checkBox.setText(
                        getResources().getString(R.string.routingWayClassSmallStreets));
            } else if (routingWayClassTag.equals(Constants.ROUTING_WAY_CLASS.PAVED_WAYS)) {
                checkBox.setText(
                        getResources().getString(R.string.routingWayClassPavedWays));
            } else if (routingWayClassTag.equals(Constants.ROUTING_WAY_CLASS.UNPAVED_WAYS)) {
                checkBox.setText(
                        getResources().getString(R.string.routingWayClassUnpavedWays));
            } else if (routingWayClassTag.equals(Constants.ROUTING_WAY_CLASS.STEPS)) {
                checkBox.setText(
                        getResources().getString(R.string.routingWayClassSteps));
            } else if (routingWayClassTag.equals(Constants.ROUTING_WAY_CLASS.UNCLASSIFIED_WAYS)) {
                checkBox.setText(
                        getResources().getString(R.string.routingWayClassUnclassifiedWays));
            } else {
                continue;
            }
            checkBoxGroupRoutingWayClasses.put(checkBox);
        }

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectRoutingWayClassesDialogTitle))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .setNeutralButton(
                    getResources().getString(R.string.dialogClear),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
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
                    ArrayList<String> allowedWayClassList = new ArrayList<String>();
                    for (CheckBox checkBox : checkBoxGroupRoutingWayClasses.getCheckedCheckBoxList()) {
                        String allowedWayClass = (String) checkBox.getTag();
                        if (Arrays.asList(Constants.RoutingWayClassValueArray).contains(allowedWayClass)) {
                            allowedWayClassList.add(allowedWayClass);
                        }
                    }
                    if (allowedWayClassList.isEmpty()) {
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.messageNoAllowedWayClassSelected),
                                Toast.LENGTH_LONG).show();
                    } else {
                        settingsManagerInstance.getRouteSettings().setWayClassList(allowedWayClassList);
                        dialog.dismiss();
                    }
                }
            });
            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (checkBoxGroupRoutingWayClasses.nothingChecked()) {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogAll));
            } else {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogClear));
            }
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (checkBoxGroupRoutingWayClasses.nothingChecked()) {
                        checkBoxGroupRoutingWayClasses.checkAll();
                    } else {
                        checkBoxGroupRoutingWayClasses.uncheckAll();
                    }
                    onStart();
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

}
