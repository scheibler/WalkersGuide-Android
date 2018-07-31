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


public class CreateOrEditFavoritesProfileDialog extends DialogFragment {

    // Store instance variables
    private AccessDatabase accessDatabaseInstance;
    private InputMethodManager imm;
    private SettingsManager settingsManagerInstance;
    private int favoritesProfileId;
    private String profileName;
    private int sortCriteria;

    // ui components
    private EditText editProfileName;
    private RadioGroup radioGroupFavoritesProfileSortCriteria;

    public static CreateOrEditFavoritesProfileDialog newInstance(int favoritesProfileId) {
        CreateOrEditFavoritesProfileDialog createOrEditFavoritesProfileDialogInstance = new CreateOrEditFavoritesProfileDialog();
        Bundle args = new Bundle();
        args.putInt("favoritesProfileId", favoritesProfileId);
        createOrEditFavoritesProfileDialogInstance.setArguments(args);
        return createOrEditFavoritesProfileDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        accessDatabaseInstance = AccessDatabase.getInstance(context);
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        settingsManagerInstance = SettingsManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            favoritesProfileId = savedInstanceState.getInt("favoritesProfileId");
            profileName = savedInstanceState.getString("profileName");
            sortCriteria = savedInstanceState.getInt("sortCriteria");
        } else {
            favoritesProfileId = getArguments().getInt("favoritesProfileId", -1);
            profileName = accessDatabaseInstance.getNameOfFavoritesProfile(favoritesProfileId);
            sortCriteria = accessDatabaseInstance.getSortCriteriaOfFavoritesProfile(favoritesProfileId);
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_or_edit_favorites_profile, nullParent);

        // dialog title
        String dialogTitle;
        if (accessDatabaseInstance.getFavoritesProfileMap().containsKey(favoritesProfileId)) {
            dialogTitle = getResources().getString(R.string.editProfileDialogTitle);
        } else {
            dialogTitle = getResources().getString(R.string.newProfileDialogTitle);
        }

        editProfileName = (EditText) view.findViewById(R.id.editInput);
        editProfileName.setText(profileName);
        editProfileName.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editProfileName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    createOrEditFavoritesProfile();
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

        radioGroupFavoritesProfileSortCriteria = (RadioGroup) view.findViewById(R.id.radioGroup);
        radioGroupFavoritesProfileSortCriteria.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                sortCriteria = checkedId;
            }
        });

        for (int sortCriteriaId : Constants.FavoritesProfileSortCriteriaValueArray) {
            RadioButton radioButton = new RadioButton(getActivity());
            radioButton.setId(sortCriteriaId);
            radioButton.setLayoutParams(
                    new LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                    );
            switch (sortCriteriaId) {
                case Constants.SORT_CRITERIA.DISTANCE_ASC:
                    radioButton.setText(getResources().getString(R.string.radioButtonSortDistanceAsc));
                    break;
                case Constants.SORT_CRITERIA.DISTANCE_DESC:
                    radioButton.setText(getResources().getString(R.string.radioButtonSortDistanceDesc));
                    break;
                case Constants.SORT_CRITERIA.NAME_ASC:
                    radioButton.setText(getResources().getString(R.string.radioButtonSortNameAsc));
                    break;
                case Constants.SORT_CRITERIA.NAME_DESC:
                    radioButton.setText(getResources().getString(R.string.radioButtonSortNameDesc));
                    break;
                case Constants.SORT_CRITERIA.ORDER_ASC:
                    radioButton.setText(getResources().getString(R.string.radioButtonSortOrderAsc));
                    break;
                case Constants.SORT_CRITERIA.ORDER_DESC:
                    radioButton.setText(getResources().getString(R.string.radioButtonSortOrderDesc));
                    break;
                default:
                    radioButton = null;
                    break;
            }
            if (radioButton != null) {
                radioGroupFavoritesProfileSortCriteria.addView(radioButton);
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
            // check radio group
            radioGroupFavoritesProfileSortCriteria.check(sortCriteria);
            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    createOrEditFavoritesProfile();
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
        savedInstanceState.putInt("favoritesProfileId", favoritesProfileId);
        savedInstanceState.putString("profileName", profileName);
        savedInstanceState.putInt("sort_criteria", sortCriteria);
    }

    private void createOrEditFavoritesProfile() {
        // profile name
        if (profileName.equals("")) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageProfileNameMissing),
                    Toast.LENGTH_LONG).show();
            return;
        } else {
            for(Map.Entry<Integer,String> profile : accessDatabaseInstance.getFavoritesProfileMap().entrySet()) {
                if (favoritesProfileId != profile.getKey()
                        && profileName.equals(profile.getValue())) {
                    Toast.makeText(
                            getActivity(),
                            String.format(getResources().getString(R.string.messageProfileExists), profileName),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
        // profile sort criteria
        if (! Ints.contains(Constants.FavoritesProfileSortCriteriaValueArray, sortCriteria)) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageNoSortCriteria),
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (! accessDatabaseInstance.getFavoritesProfileMap().containsKey(favoritesProfileId)) {
            // create new profile
            int newProfileId = accessDatabaseInstance.addFavoritesProfile(profileName, sortCriteria);
            if (newProfileId > -1) {
                settingsManagerInstance.getFavoritesFragmentSettings().setSelectedFavoritesProfileId(newProfileId);
            } else {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageCouldNotCreateProfile),
                        Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            // edit existing profile
            boolean updateSuccessful = accessDatabaseInstance.updateNameAndSortCriteriaOfFavoritesProfile(
                    favoritesProfileId, profileName, sortCriteria);
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

}
