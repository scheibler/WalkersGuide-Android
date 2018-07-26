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

public class SaveCurrentPositionDialog extends DialogFragment implements ChildDialogCloseListener {

    private AccessDatabase accessDatabaseInstance;
    private InputMethodManager imm;
    private PositionManager positionManagerInstance;
    private PointWrapper currentLocation;

    // ui components
    private TextView labelGPSProvider, labelGPSAccuracy, labelGPSTime;
    private CheckBoxGroupView checkBoxGroupFavoritesProfiles;
    private EditText editName;

    public static SaveCurrentPositionDialog newInstance() {
        SaveCurrentPositionDialog saveCurrentPositionDialogInstance = new SaveCurrentPositionDialog();
        return saveCurrentPositionDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        accessDatabaseInstance = AccessDatabase.getInstance(context);
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		positionManagerInstance = PositionManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_save_current_position, nullParent);

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
            if (profile.getKey() >= FavoritesProfile.ID_FIRST_USER_CREATED_PROFILE) {
                CheckBox checkBox = new CheckBox(getActivity());
                checkBox.setId(profile.getKey());
                checkBox.setLayoutParams(
                        new LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT)
                        );
                checkBox.setText(profile.getValue());
                checkBox.setChecked(false);
                checkBoxGroupFavoritesProfiles.put(checkBox);
            }
        }
        ImageButton buttonClearFavoritesProfiles = (ImageButton) view.findViewById(R.id.buttonClearFavoritesProfiles);
        buttonClearFavoritesProfiles.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                checkBoxGroupFavoritesProfiles.uncheckAll();
            }
        });
        if (! checkBoxGroupFavoritesProfiles.getCheckBoxList().isEmpty()) {
            TextView labelNoUserCreatedFavoritesProfiles = (TextView) view.findViewById(R.id.labelNoUserCreatedFavoritesProfiles);
            labelNoUserCreatedFavoritesProfiles.setVisibility(View.GONE);
        }

        // name sublayout
        editName = (EditText) view.findViewById(R.id.editInput);
        editName.setInputType(InputType.TYPE_CLASS_TEXT);
        editName.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editName.setHint(getResources().getString(R.string.editHintSaveCurrentPointName));
        editName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    tryToSaveCurrentPosition();
                    return true;
                }
                return false;
            }
        });
        ImageButton buttonDelete = (ImageButton) view.findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // clear edit text
                editName.setText("");
                // show keyboard
                imm.showSoftInput(editName, InputMethodManager.SHOW_IMPLICIT);
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
            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    tryToSaveCurrentPosition();
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
        updateLocationData();
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
                    getResources().getString(R.string.messageError1004),
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
                    accessDatabaseInstance.addPointToFavoritesProfile(userCreatedPoint, FavoritesProfile.ID_USER_CREATED_POINTS);
                    for (CheckBox checkBox : checkBoxGroupFavoritesProfiles.getCheckBoxList()) {
                        if (checkBox.isChecked()) {
                            accessDatabaseInstance.addPointToFavoritesProfile(userCreatedPoint, checkBox.getId());
                        }
                    }
                }
            }
            dismiss();
        }
    }

}
