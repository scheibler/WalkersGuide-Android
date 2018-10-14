package org.walkersguide.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;

import android.text.Editable;
import android.text.InputType;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.R;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.ui.view.CheckBoxGroupView;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.TextChangedListener;
import android.widget.Switch;
import android.widget.CompoundButton;


public class CreateOrEditPOIProfileDialog extends DialogFragment {

    // Store instance variables
    private AccessDatabase accessDatabaseInstance;
    private InputMethodManager imm;
    private ServerInstance serverInstance;
    private int poiProfileId;
    private String profileName;
    private boolean includeFavorites;
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
        serverInstance = ServerStatusManager.getInstance(context).getServerInstance();
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            poiProfileId = savedInstanceState.getInt("poiProfileId");
            profileName = savedInstanceState.getString("profileName");
            includeFavorites = savedInstanceState.getBoolean("includeFavorites");
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
                        try {
                            checkedCategoryList.add(
                                    new POICategory(
                                        getActivity(), jsonCheckedCategoryIdList.getString(i)));
                        } catch (JSONException e) {}
                    }
                }
            }
        } else {
            poiProfileId = getArguments().getInt("poiProfileId", -1);
            profileName = accessDatabaseInstance.getNameOfPOIProfile(poiProfileId);
            if (poiProfileId == -1) {
                includeFavorites = true;
            } else {
                includeFavorites = accessDatabaseInstance.getFavoriteIdListOfPOIProfile(poiProfileId).contains(poiProfileId);
            }
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
                    imm.hideSoftInputFromWindow(editProfileName.getWindowToken(), 0);
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

        Switch buttonIncludeFavorites = (Switch) view.findViewById(R.id.buttonIncludeFavorites);
        buttonIncludeFavorites.setChecked(includeFavorites);
        buttonIncludeFavorites.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                includeFavorites = isChecked;
            }
        });

        checkBoxGroupPOICategories = (CheckBoxGroupView) view.findViewById(R.id.checkBoxGroupPOICategories);
        if (serverInstance != null) {
            for (POICategory category : serverInstance.getSupportedPOICCategoryList()) {
                CheckBox checkBox = new CheckBox(getActivity());
                checkBox.setTag(category.getId());
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
                            new POICategory(getActivity(), (String) checkBox.getTag())));
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
                    if (checkBoxGroupPOICategories.nothingChecked()
                            && serverInstance != null) {
                        checkedCategoryList = serverInstance.getSupportedPOICCategoryList();
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
        savedInstanceState.putBoolean("includeFavorites", includeFavorites);
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
        // create or edit profile
        if (! accessDatabaseInstance.getPOIProfileMap().containsKey(poiProfileId)) {
            // create new profile
            int newProfileId = accessDatabaseInstance.addPOIProfile(profileName);
            if (newProfileId == -1) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageCouldNotCreateProfile),
                        Toast.LENGTH_LONG).show();
                return;
            }
            poiProfileId = newProfileId;
        }
        // update profile fields
        ArrayList<Integer> favorite_id_list = new ArrayList<Integer>();
        if (includeFavorites) {
            favorite_id_list.add(poiProfileId);
        }
        boolean updateSuccessful = accessDatabaseInstance.updateNameAndCategoriesOfPOIProfile(
                poiProfileId, profileName, favorite_id_list, checkedCategoryList);
        if (! updateSuccessful) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageCouldNotEditProfile),
                    Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(Constants.ACTION_NEW_POI_PROFILE);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        dismiss();
    }

    private ArrayList<POICategory> getCheckedItemsOfPOICategoriesCheckBoxGroup() {
        ArrayList<POICategory> poiCategoryList = new ArrayList<POICategory>();
        for (CheckBox checkBox : checkBoxGroupPOICategories.getCheckedCheckBoxList()) {
            poiCategoryList.add(
                    new POICategory(getActivity(), (String) checkBox.getTag()));
        }
        return poiCategoryList;
    }

}
