package org.walkersguide.android.ui.dialog;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.google.common.primitives.Ints;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;


import android.os.Bundle;

import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;


import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;


import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import java.util.Map;
import android.widget.EditText;
import org.walkersguide.android.util.TextChangedListener;
import android.widget.RadioButton;
import android.text.Editable;
import java.util.ArrayList;
import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.ui.view.CheckBoxGroupView;
import android.widget.CheckBox;
import org.json.JSONArray;
import org.json.JSONException;
import android.text.InputType;


public class CreateOrEditPOIProfileDialog extends DialogFragment {

    // Store instance variables
    private AccessDatabase accessDatabaseInstance;
    private InputMethodManager imm;
    private SettingsManager settingsManagerInstance;
    private int poiProfileId;
    private String profileName;
    private ArrayList<POICategory> checkedCategoryList;

    // ui components
    private EditText editProfileName;
    private CheckBoxGroupView checkBoxGroupPOICategories;

    public static CreateOrEditPOIProfileDialog newInstance(int poiProfileId) {
        CreateOrEditPOIProfileDialog createOrEditPOIProfileDialogInstance = new CreateOrEditPOIProfileDialog();
        Bundle args = new Bundle();
        args.putInt("poiProfileId", poiProfileId);
        createOrEditPOIProfileDialogInstance.setArguments(args);
        return createOrEditPOIProfileDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        accessDatabaseInstance = AccessDatabase.getInstance(context);
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        settingsManagerInstance = SettingsManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            poiProfileId = savedInstanceState.getInt("poiProfileId");
            profileName = savedInstanceState.getString("profileName");
            // poi categories
            checkedCategoryList = new ArrayList<POICategory>();
            JSONArray jsonCheckedCategoryIdList = null;
            try {
                jsonCheckedCategoryIdList = new JSONArray(
                        savedInstanceState.getString("jsonCheckedCategoryIdList"));
            } catch (JSONException e) {
                jsonCheckedCategoryIdList = null;
            } finally {
                if (jsonCheckedCategoryIdList != null) {
                    for (int i=0; i<jsonCheckedCategoryIdList.length(); i++) {
                        POICategory poiCategory = null;
                        try {
                            poiCategory = accessDatabaseInstance.getPOICategory(jsonCheckedCategoryIdList.getInt(i));
                        } catch (JSONException e) {
                            poiCategory = null;
                        } finally {
                            if (poiCategory != null) {
                                checkedCategoryList.add(poiCategory);
                            }
                        }
                    }
                }
            }
        } else {
            poiProfileId = getArguments().getInt("poiProfileId", -1);
            profileName = accessDatabaseInstance.getNameOfPOIProfile(poiProfileId);
            checkedCategoryList = accessDatabaseInstance.getCategoryListOfPOIProfile(poiProfileId);
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_or_edit_poi_profile, nullParent);

        // dialog title
        String dialogTitle;
        if (accessDatabaseInstance.getPOIProfileMap().containsKey(poiProfileId)) {
            dialogTitle = getResources().getString(R.string.editProfileDialogTitle);
        } else {
            dialogTitle = getResources().getString(R.string.newProfileDialogTitle);
        }

        editProfileName = (EditText) view.findViewById(R.id.editInput);
        editProfileName.setText(profileName);
        editProfileName.setInputType(InputType.TYPE_CLASS_TEXT);
        editProfileName.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editProfileName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    createOrEditPOIProfile();
                    return true;
                }
                return false;
            }
        });
        editProfileName.addTextChangedListener(new TextChangedListener<EditText>(editProfileName) {
            @Override public void onTextChanged(EditText view, Editable s) {
                profileName = editProfileName.getText().toString();
            }
        });

        ImageButton buttonDelete = (ImageButton) view.findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // clear edit text
                editProfileName.setText("");
                // show keyboard
                imm.showSoftInput(editProfileName, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        checkBoxGroupPOICategories = (CheckBoxGroupView) view.findViewById(R.id.checkBoxGroupPOICategories);
        for (POICategory category : accessDatabaseInstance.getPOICategoryList()) {
            CheckBox checkBox = new CheckBox(getActivity());
            checkBox.setId(category.getId());
            checkBox.setLayoutParams(
                    new LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                    );
            checkBox.setText(category.getName());
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    checkedCategoryList = getCheckedItemsOfPOICategoriesCheckBoxGroup();
                    onStart();
                }
            });
            checkBoxGroupPOICategories.put(checkBox);
        }

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(dialogTitle)
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
            for (CheckBox checkBox : checkBoxGroupPOICategories.getCheckBoxList()) {
                checkBox.setChecked(
                        checkedCategoryList.contains(
                            accessDatabaseInstance.getPOICategory(checkBox.getId())));
            }

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    createOrEditPOIProfile();
                }
            });

            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (checkBoxGroupPOICategories.nothingChecked()) {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogAll));
            } else {
                buttonNeutral.setText(
                        getResources().getString(R.string.dialogClear));
            }
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (checkBoxGroupPOICategories.nothingChecked()) {
                        checkedCategoryList = accessDatabaseInstance.getPOICategoryList();
                    } else {
                        checkedCategoryList = new ArrayList<POICategory>();
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
        savedInstanceState.putInt("poiProfileId", poiProfileId);
        savedInstanceState.putString("profileName", profileName);
        JSONArray jsonCheckedCategoryIdList = new JSONArray();
        for (POICategory poiCategory : getCheckedItemsOfPOICategoriesCheckBoxGroup()) {
            jsonCheckedCategoryIdList.put(poiCategory.getId());
        }
        savedInstanceState.putString("jsonCheckedCategoryIdList", jsonCheckedCategoryIdList.toString());
    }

    private void createOrEditPOIProfile() {
        // profile name
        if (profileName.equals("")) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageProfileNameMissing),
                    Toast.LENGTH_LONG).show();
            return;
        } else {
            for(Map.Entry<Integer,String> profile : accessDatabaseInstance.getPOIProfileMap().entrySet()) {
                if (poiProfileId != profile.getKey()
                        && profileName.equals(profile.getValue())) {
                    Toast.makeText(
                            getActivity(),
                            String.format(getResources().getString(R.string.messageProfileExists), profileName),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
        if (checkedCategoryList.isEmpty()) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageError1033),
                    Toast.LENGTH_LONG).show();
            return;
        }
        // create or edit profile
        if (! accessDatabaseInstance.getPOIProfileMap().containsKey(poiProfileId)) {
            // create new profile
            int newProfileId = accessDatabaseInstance.addPOIProfile(profileName, checkedCategoryList);
            if (newProfileId > -1) {
                settingsManagerInstance.getPOIFragmentSettings().setSelectedPOIProfileId(newProfileId);
            } else {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageCouldNotCreateProfile),
                        Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            // edit existing profile
            boolean updateSuccessful = accessDatabaseInstance.updateNameAndCategoryListOfPOIProfile(
                    poiProfileId, profileName, checkedCategoryList);
            if (! updateSuccessful) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageCouldNotEditProfile),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
        Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        dismiss();
    }

    private ArrayList<POICategory> getCheckedItemsOfPOICategoriesCheckBoxGroup() {
        ArrayList<POICategory> poiCategoryList = new ArrayList<POICategory>();
        for (CheckBox checkBox : checkBoxGroupPOICategories.getCheckedCheckBoxList()) {
            POICategory category = accessDatabaseInstance.getPOICategory(checkBox.getId());
            if (category != null) {
                poiCategoryList.add(category);
            }
        }
        return poiCategoryList;
    }
}
