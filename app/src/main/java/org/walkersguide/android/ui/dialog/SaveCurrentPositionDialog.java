package org.walkersguide.android.ui.dialog;

import java.util.Date;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.FavoritesProfile;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.listener.ChildDialogCloseListener;
import org.walkersguide.android.ui.dialog.CreateOrEditFavoritesProfileDialog;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.view.CheckBoxGroupView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.TreeSet;
import android.content.IntentFilter;
import org.walkersguide.android.util.Constants;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Vibrator;
import org.walkersguide.android.util.SettingsManager;
import android.widget.AutoCompleteTextView;
import android.content.BroadcastReceiver;
import android.widget.ArrayAdapter;
import org.json.JSONArray;
import android.content.Intent;

public class SaveCurrentPositionDialog extends DialogFragment implements ChildDialogCloseListener {

    private AccessDatabase accessDatabaseInstance;
    private InputMethodManager imm;
    private PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;
    private Vibrator vibrator;
    private PointWrapper currentLocation;
    private TreeSet<Integer> checkedFavoritesProfileIds;

    // ui components
    private AutoCompleteTextView editName;
    private TextView labelGPSProvider, labelGPSAccuracy, labelGPSTime;
    private CheckBoxGroupView checkBoxGroupFavoritesProfiles;
    private TextView labelNoUserCreatedFavoritesProfiles;

    public static SaveCurrentPositionDialog newInstance(TreeSet<Integer> checkedFavoritesProfileIds) {
        SaveCurrentPositionDialog saveCurrentPositionDialogInstance = new SaveCurrentPositionDialog();
        Bundle args = new Bundle();
        JSONArray jsonCheckedFavoritesProfileIdList = new JSONArray();
        for (Integer id : checkedFavoritesProfileIds) {
            jsonCheckedFavoritesProfileIdList.put(id);
        }
        args.putString("jsonCheckedFavoritesProfileIdList", jsonCheckedFavoritesProfileIdList.toString());
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
        filter.addAction(Constants.ACTION_UPDATE_UI);
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        JSONArray jsonCheckedFavoritesProfileIdList = null;
        try {
            if (savedInstanceState != null) {
                jsonCheckedFavoritesProfileIdList = new JSONArray(
                        savedInstanceState.getString("jsonCheckedFavoritesProfileIdList"));
            } else {
                jsonCheckedFavoritesProfileIdList = new JSONArray(
                        getArguments().getString("jsonCheckedFavoritesProfileIdList"));
            }
        } catch (JSONException e) {
            jsonCheckedFavoritesProfileIdList = null;
        } finally {
            checkedFavoritesProfileIds = new TreeSet<Integer>();
            if (jsonCheckedFavoritesProfileIdList != null) {
                for (int i=0; i<jsonCheckedFavoritesProfileIdList.length(); i++) {
                    try {
                        checkedFavoritesProfileIds.add(jsonCheckedFavoritesProfileIdList.getInt(i));
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
                    tryToSaveCurrentPosition();
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

        ImageButton buttonDelete = (ImageButton) view.findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
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

        // favorites profiles sublayout
        checkBoxGroupFavoritesProfiles = (CheckBoxGroupView) view.findViewById(R.id.checkBoxGroup);
        for (Map.Entry<Integer,String> profile : accessDatabaseInstance.getFavoritesProfileMap().entrySet()) {
            if (profile.getKey() < FavoritesProfile.ID_FIRST_USER_CREATED_PROFILE) {
                continue;
            }
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
                    checkedFavoritesProfileIds = getCheckedItemsOfFavoritesProfilesCheckBoxGroup();
                    onStart();
                }
            });
            checkBoxGroupFavoritesProfiles.put(checkBox);
        }
        labelNoUserCreatedFavoritesProfiles = (TextView) view.findViewById(R.id.labelNoUserCreatedFavoritesProfiles);

        Button buttonAddFavoritesProfile = (Button) view.findViewById(R.id.buttonAddFavoritesProfile);
        buttonAddFavoritesProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                CreateOrEditFavoritesProfileDialog.newInstance(-1)
                    .show(getActivity().getSupportFragmentManager(), "CreateOrEditFavoritesProfileDialog");
            }
        });

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
            for (CheckBox checkBox : checkBoxGroupFavoritesProfiles.getCheckBoxList()) {
                checkBox.setChecked(
                        checkedFavoritesProfileIds.contains(checkBox.getId()));
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
            if (checkBoxGroupFavoritesProfiles.nothingChecked()) {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogAll));
            } else {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogClear));
            }
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    checkedFavoritesProfileIds = new TreeSet<Integer>();
                    if (checkBoxGroupFavoritesProfiles.nothingChecked()) {
                        checkedFavoritesProfileIds.addAll(accessDatabaseInstance.getFavoritesProfileMap().keySet());
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

        // show or hide no user favorites profiles
        if (checkBoxGroupFavoritesProfiles.getCheckBoxList().isEmpty()) {
            labelNoUserCreatedFavoritesProfiles.setVisibility(View.VISIBLE);
        } else {
            labelNoUserCreatedFavoritesProfiles.setVisibility(View.GONE);
        }
        // update current location data
        updateLocationData();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        JSONArray jsonCheckedFavoritesProfileIdList = new JSONArray();
        for (Integer id : getCheckedItemsOfFavoritesProfilesCheckBoxGroup()) {
            jsonCheckedFavoritesProfileIdList.put(id);
        }
        savedInstanceState.putString("jsonCheckedFavoritesProfileIdList", jsonCheckedFavoritesProfileIdList.toString());
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
                            "%1$s: %2$s, %3$d %4$s",
                            getResources().getString(R.string.labelGPSProvider),
                            gpsLocation.getProvider(),
                            gpsLocation.getNumberOfSatellites(),
                            getResources().getString(R.string.unitSatellites))
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
                            "%1$s: %2$d %3$s",
                            getResources().getString(R.string.labelGPSAccuracy),
                            Math.round(gpsLocation.getAccuracy()),
                            getResources().getString(R.string.unitMeters))
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
                    accessDatabaseInstance.addPointToFavoritesProfile(userCreatedPoint, FavoritesProfile.ID_USER_CREATED_POINTS);
                    for (Integer favoritesProfileIdToAdd : checkedFavoritesProfileIds) {
                        accessDatabaseInstance.addPointToFavoritesProfile(userCreatedPoint, favoritesProfileIdToAdd);
                    }
                }
            }
            dismiss();
        }
    }

    private TreeSet<Integer> getCheckedItemsOfFavoritesProfilesCheckBoxGroup() {
        TreeSet<Integer> favoritesProfileList = new TreeSet<Integer>();
        for (CheckBox checkBox : checkBoxGroupFavoritesProfiles.getCheckedCheckBoxList()) {
            favoritesProfileList.add(checkBox.getId());
        }
        return favoritesProfileList;
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_UPDATE_UI)) {
                onStart();
            } else if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                vibrator.vibrate(250);
                updateLocationData();
            } else if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                    && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD3.ID) {
                updateLocationData();
            }
        }
    };

}
