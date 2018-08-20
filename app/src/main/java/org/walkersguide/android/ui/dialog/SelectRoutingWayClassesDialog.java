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
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.data.route.WayClass;
import org.json.JSONArray;
import org.json.JSONException;


public class SelectRoutingWayClassesDialog extends DialogFragment {

    // Store instance variables
    private SettingsManager settingsManagerInstance;
    private ServerInstance serverInstance;
    private ArrayList<WayClass> checkedWayClassList;

    // ui components
    private CheckBoxGroupView checkBoxGroupRoutingWayClasses;

    public static SelectRoutingWayClassesDialog newInstance() {
        SelectRoutingWayClassesDialog selectRoutingWayClassesDialog = new SelectRoutingWayClassesDialog();
        return selectRoutingWayClassesDialog;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
        serverInstance = ServerStatusManager.getInstance(context).getServerInstance();
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            checkedWayClassList = new ArrayList<WayClass>();
            JSONArray jsonCheckedWayClassIdList = null;
            try {
                jsonCheckedWayClassIdList = new JSONArray(
                        savedInstanceState.getString("jsonCheckedWayClassIdList"));
            } catch (JSONException e) {
                jsonCheckedWayClassIdList = null;
            } finally {
                if (jsonCheckedWayClassIdList != null) {
                    for (int i=0; i<jsonCheckedWayClassIdList.length(); i++) {
                        try {
                            checkedWayClassList.add(
                                    new WayClass(
                                        getActivity(), jsonCheckedWayClassIdList.getString(i)));
                        } catch (JSONException e) {}
                    }
                }
            }
        } else {
            checkedWayClassList = settingsManagerInstance.getRouteSettings().getWayClassList();
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_single_check_box_group, nullParent);

        checkBoxGroupRoutingWayClasses = (CheckBoxGroupView) view.findViewById(R.id.checkBoxGroup);
        if (serverInstance != null) {
            for (WayClass wayClass : serverInstance.getSupportedWayClassList()) {
                CheckBox checkBox = new CheckBox(getActivity());
                checkBox.setTag(wayClass.getId());
                checkBox.setLayoutParams(
                        new LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT)
                        );
                checkBox.setText(wayClass.getName());
                checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        checkedWayClassList = getCheckedItemsOfWayClassCheckBoxGroup();
                        onStart();
                    }
                });
                checkBoxGroupRoutingWayClasses.put(checkBox);
            }
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
            // check boxes
            for (CheckBox checkBox : checkBoxGroupRoutingWayClasses.getCheckBoxList()) {
                checkBox.setChecked(
                        checkedWayClassList.contains(
                            new WayClass(getActivity(), (String) checkBox.getTag())));
            }

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (checkedWayClassList.isEmpty()) {
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.messageNoAllowedWayClassSelected),
                                Toast.LENGTH_LONG).show();
                    } else {
                        settingsManagerInstance.getRouteSettings().setWayClassList(checkedWayClassList);
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
                    if (checkBoxGroupRoutingWayClasses.nothingChecked()
                            && serverInstance != null) {
                        checkedWayClassList = serverInstance.getSupportedWayClassList();
                    } else {
                        checkedWayClassList = new ArrayList<WayClass>();
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

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        JSONArray jsonCheckedWayClassIdList = new JSONArray();
        for (WayClass wayClass : getCheckedItemsOfWayClassCheckBoxGroup()) {
            jsonCheckedWayClassIdList.put(wayClass.getId());
        }
        savedInstanceState.putString("jsonCheckedWayClassIdList", jsonCheckedWayClassIdList.toString());
    }

    private ArrayList<WayClass> getCheckedItemsOfWayClassCheckBoxGroup() {
        ArrayList<WayClass> wayClassList = new ArrayList<WayClass>();
        for (CheckBox checkBox : checkBoxGroupRoutingWayClasses.getCheckedCheckBoxList()) {
            wayClassList.add(
                    new WayClass(getActivity(), (String) checkBox.getTag()));
        }
        return wayClassList;
    }

}
