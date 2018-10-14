package org.walkersguide.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Vibrator;

import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;

import android.text.format.DateFormat;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.Map;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.HistoryPointProfile;
import org.walkersguide.android.listener.ChildDialogCloseListener;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.view.CheckBoxGroupView;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;

public class SaveCurrentPositionDialog extends DialogFragment implements ChildDialogCloseListener {

    private AccessDatabase accessDatabaseInstance;
    private InputMethodManager imm;
    private PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;
    private Vibrator vibrator;
    private PointWrapper currentLocation;
    private TreeSet<Integer> checkedPOIProfileIds;

    // ui components
    private AutoCompleteTextView editName;
    private TextView labelGPSProvider, labelGPSAccuracy, labelGPSTime;
    private CheckBoxGroupView checkBoxGroupPOIProfiles;
    private TextView labelNoPOIProfile;

    public static SaveCurrentPositionDialog newInstance(TreeSet<Integer> checkedPOIProfileIds) {
        SaveCurrentPositionDialog saveCurrentPositionDialogInstance = new SaveCurrentPositionDialog();
        Bundle args = new Bundle();
        JSONArray jsonCheckedPOIProfileIdList = new JSONArray();
        for (Integer id : checkedPOIProfileIds) {
            jsonCheckedPOIProfileIdList.put(id);
        }
        args.putString("jsonCheckedPOIProfileIdList", jsonCheckedPOIProfileIdList.toString());
        saveCurrentPositionDialogInstance.setArguments(args);
        return saveCurrentPositionDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        accessDatabaseInstance = AccessDatabase.getInstance(context);
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		positionManagerInstance = PositionManager.getInstance(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        // listen for intents
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_LOCATION);
        filter.addAction(Constants.ACTION_SHAKE_DETECTED);
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        JSONArray jsonCheckedPOIProfileIdList = null;
        try {
            if (savedInstanceState != null) {
                jsonCheckedPOIProfileIdList = new JSONArray(
                        savedInstanceState.getString("jsonCheckedPOIProfileIdList"));
            } else {
                jsonCheckedPOIProfileIdList = new JSONArray(
                        getArguments().getString("jsonCheckedPOIProfileIdList"));
            }
        } catch (JSONException e) {
            jsonCheckedPOIProfileIdList = null;
        } finally {
            checkedPOIProfileIds = new TreeSet<Integer>();
            if (jsonCheckedPOIProfileIdList != null) {
                for (int i=0; i<jsonCheckedPOIProfileIdList.length(); i++) {
                    try {
                        checkedPOIProfileIds.add(jsonCheckedPOIProfileIdList.getInt(i));
                    } catch (JSONException e) {}
                }
            }
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_save_current_position, nullParent);

        // name sublayout
        editName = (AutoCompleteTextView) view.findViewById(R.id.editInput);
        editName.setHint(getResources().getString(R.string.editHintSaveCurrentPointName));
        editName.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // hide keyboard
                    editName.dismissDropDown();
                    imm.hideSoftInputFromWindow(editName.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
        // add auto complete suggestions
        ArrayAdapter<String> searchTermHistoryAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                settingsManagerInstance.getSearchTermHistory().getSearchTermList());
        editName.setAdapter(searchTermHistoryAdapter);

        ImageButton buttonClearInput = (ImageButton) view.findViewById(R.id.buttonClearInput);
        buttonClearInput.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // clear edit text
                editName.setText("");
                // show keyboard
                imm.showSoftInput(editName, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // current location sublayout
        labelGPSProvider = (TextView) view.findViewById(R.id.labelGPSProvider);
        labelGPSAccuracy = (TextView) view.findViewById(R.id.labelGPSAccuracy);
        labelGPSTime = (TextView) view.findViewById(R.id.labelGPSTime);
        ImageButton buttonRefreshLocation = (ImageButton) view.findViewById(R.id.buttonRefreshLocation);
        buttonRefreshLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                updateLocationData();
            }
        });

        // poi profiles sublayout
        checkBoxGroupPOIProfiles = (CheckBoxGroupView) view.findViewById(R.id.checkBoxGroup);
        for (Map.Entry<Integer,String> profile : accessDatabaseInstance.getPOIProfileMap().entrySet()) {
            CheckBox checkBox = new CheckBox(getActivity());
            checkBox.setId(profile.getKey());
            checkBox.setLayoutParams(
                    new LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                    );
            checkBox.setText(profile.getValue());
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    checkedPOIProfileIds = getCheckedItemsOfPOIProfilesCheckBoxGroup();
                    onStart();
                }
            });
            checkBoxGroupPOIProfiles.put(checkBox);
        }
        labelNoPOIProfile = (TextView) view.findViewById(R.id.labelNoPOIProfile);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.saveCurrentPositionDialogName))
            .setView(view)
            .setPositiveButton(
                getResources().getString(R.string.dialogOK),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
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
                })
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {

            // check boxes
            for (CheckBox checkBox : checkBoxGroupPOIProfiles.getCheckBoxList()) {
                checkBox.setChecked(
                        checkedPOIProfileIds.contains(checkBox.getId()));
            }

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    tryToSaveCurrentPosition();
                }
            });

            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (checkBoxGroupPOIProfiles.nothingChecked()) {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogAll));
            } else {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogClear));
            }
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    checkedPOIProfileIds = new TreeSet<Integer>();
                    if (checkBoxGroupPOIProfiles.nothingChecked()) {
                        checkedPOIProfileIds.addAll(accessDatabaseInstance.getPOIProfileMap().keySet());
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

        // show or hide no user poi profiles
        if (checkBoxGroupPOIProfiles.getCheckBoxList().isEmpty()) {
            labelNoPOIProfile.setVisibility(View.VISIBLE);
        } else {
            labelNoPOIProfile.setVisibility(View.GONE);
        }
        // update current location data
        updateLocationData();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        JSONArray jsonCheckedPOIProfileIdList = new JSONArray();
        for (Integer id : getCheckedItemsOfPOIProfilesCheckBoxGroup()) {
            jsonCheckedPOIProfileIdList.put(id);
        }
        savedInstanceState.putString("jsonCheckedPOIProfileIdList", jsonCheckedPOIProfileIdList.toString());
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        // unregister broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void childDialogClosed() {
        dismiss();
    }

    private void updateLocationData() {
        // clean up
        labelGPSProvider.setText(getResources().getString(R.string.labelGPSProvider));
        labelGPSAccuracy.setText(getResources().getString(R.string.labelGPSAccuracy));
        labelGPSTime.setText(getResources().getString(R.string.labelGPSTime));
        // new data
        currentLocation = positionManagerInstance.getCurrentLocation();
        if (! currentLocation.equals(PositionManager.getDummyLocation(getActivity()))) {
            GPS gpsLocation = (GPS) currentLocation.getPoint();
            if (gpsLocation.getNumberOfSatellites() >= 0) {
                labelGPSProvider.setText(
                        String.format(
                            "%1$s: %2$s, %3$s",
                            getResources().getString(R.string.labelGPSProvider),
                            gpsLocation.getProvider(),
                            getResources().getQuantityString(
                                R.plurals.satellite, gpsLocation.getNumberOfSatellites(), gpsLocation.getNumberOfSatellites()))
                        );
            } else {
                labelGPSProvider.setText(
                        String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.labelGPSProvider),
                            gpsLocation.getProvider())
                        );
            }
            if (gpsLocation.getAccuracy() >= 0.0) {
                labelGPSAccuracy.setText(
                        String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.labelGPSAccuracy),
                            getResources().getQuantityString(
                                R.plurals.meter,
                                Math.round(gpsLocation.getAccuracy()),
                                Math.round(gpsLocation.getAccuracy())))
                        );
            }
            if (gpsLocation.getTime() >= 0) {
                String formattedTime = DateFormat.getTimeFormat(getActivity()).format(
                        new Date(gpsLocation.getTime()));
                String formattedDate = DateFormat.getDateFormat(getActivity()).format(
                        new Date(gpsLocation.getTime()));
                if (formattedDate.equals(DateFormat.getDateFormat(getActivity()).format(new Date()))) {
                    labelGPSTime.setText(
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelGPSTime),
                                formattedTime)
                            );
                } else {
                    labelGPSTime.setText(
                            String.format(
                                "%1$s: %2$s, %3$s",
                                getResources().getString(R.string.labelGPSTime),
                                formattedDate,
                                formattedTime)
                            );
                }
            }
        }
    }

    private void tryToSaveCurrentPosition() {
        // hide keyboard
        editName.dismissDropDown();
        imm.hideSoftInputFromWindow(editName.getWindowToken(), 0);
        // name
        String name = editName.getText().toString().trim();
        if (name.equals("")) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageNameMissing),
                    Toast.LENGTH_LONG).show();
        } else if (currentLocation.equals(PositionManager.getDummyLocation(getActivity()))) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.errorNoLocationFound),
                    Toast.LENGTH_LONG).show();
        } else if (checkedPOIProfileIds.isEmpty()) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.errorNoPOIProfileSelected),
                    Toast.LENGTH_LONG).show();
        } else {
            PointWrapper userCreatedPoint = null;
            try {
                JSONObject jsonCurrentLocation = currentLocation.toJson();
                jsonCurrentLocation.put("name", name);
                userCreatedPoint = new PointWrapper(getActivity(), jsonCurrentLocation);
            } catch (JSONException e) {
                userCreatedPoint = null;
            } finally {
                if (userCreatedPoint != null) {
                    // add to search history
                    settingsManagerInstance.getSearchTermHistory().addSearchTerm(name);
                    accessDatabaseInstance.addFavoritePointToProfile(
                            userCreatedPoint, HistoryPointProfile.ID_USER_CREATED_POINTS);
                    for (Integer poiProfileIdToAdd : checkedPOIProfileIds) {
                        accessDatabaseInstance.addFavoritePointToProfile(userCreatedPoint, poiProfileIdToAdd);
                    }
                }
            }
            dismiss();
        }
    }

    private TreeSet<Integer> getCheckedItemsOfPOIProfilesCheckBoxGroup() {
        TreeSet<Integer> poiProfileList = new TreeSet<Integer>();
        for (CheckBox checkBox : checkBoxGroupPOIProfiles.getCheckedCheckBoxList()) {
            poiProfileList.add(checkBox.getId());
        }
        return poiProfileList;
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                vibrator.vibrate(250);
                updateLocationData();
            } else if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                    && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD3.ID) {
                updateLocationData();
            }
        }
    };

}
