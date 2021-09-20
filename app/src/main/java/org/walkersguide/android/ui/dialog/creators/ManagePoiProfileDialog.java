package org.walkersguide.android.ui.dialog.creators;

import org.walkersguide.android.server.poi.PoiCategory;

import org.walkersguide.android.ui.dialog.selectors.SelectPoiCategoriesDialog;
import org.walkersguide.android.ui.dialog.selectors.SelectPoiCategoriesDialog.SelectPoiCategoriesListener;
import org.walkersguide.android.server.poi.PoiProfile;
import org.walkersguide.android.R;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.View;

import java.util.ArrayList;
import android.app.Activity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.view.KeyEvent;
import org.walkersguide.android.util.GlobalInstance;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import org.walkersguide.android.ui.listener.TextChangedListener;
import android.text.Editable;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.text.TextUtils;
import android.widget.Toast;


public class ManagePoiProfileDialog extends DialogFragment implements SelectPoiCategoriesListener {
    private static final String KEY_ACTION = "action";
    private static final String KEY_PROFILE = "profile";
    //
    private static final String KEY_PROFILE_NAME = "profileName";
    private static final String KEY_POI_CATEGORY_LIST = "poiCategoryList";
    private static final String KEY_INCLUDE_FAVORITES = "includeFavorites";


    public interface ManagePoiProfileListener {
        public void poiProfileCreated(PoiProfile profile);
        public void poiProfileModified(PoiProfile profile);
        public void poiProfileRemoved(PoiProfile profile);
    }


    private enum Action {
        NEW, MODIFY, REMOVE
    }


    private ManagePoiProfileListener listener;
    private Action action;
    private PoiProfile profile;

    private String profileName;
    private ArrayList<PoiCategory> poiCategoryList;
    private boolean includeFavorites;

    private Button buttonSelectPoiCategories;

    public static ManagePoiProfileDialog newProfile() {
        ManagePoiProfileDialog dialog = new ManagePoiProfileDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ACTION, Action.NEW);
        args.putSerializable(KEY_PROFILE, null);
        dialog.setArguments(args);
        return dialog;
    }

    public static ManagePoiProfileDialog modifyProfile(PoiProfile profile) {
        ManagePoiProfileDialog dialog = new ManagePoiProfileDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ACTION, Action.MODIFY);
        args.putSerializable(KEY_PROFILE, profile);
        dialog.setArguments(args);
        return dialog;
    }

    public static ManagePoiProfileDialog removeProfile(PoiProfile profile) {
        ManagePoiProfileDialog dialog = new ManagePoiProfileDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ACTION, Action.REMOVE);
        args.putSerializable(KEY_PROFILE, profile);
        dialog.setArguments(args);
        return dialog;
    }


    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof ManagePoiProfileListener) {
            listener = (ManagePoiProfileListener) getTargetFragment();
        } else if (context instanceof Activity
                && (Activity) context instanceof ManagePoiProfileListener) {
            listener = (ManagePoiProfileListener) context;
        }
    }

    @Override public void onDetach() {
        super.onDetach();
        listener = null;
    }


    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        action = (Action) getArguments().getSerializable(KEY_ACTION);
        profile = (PoiProfile) getArguments().getSerializable(KEY_PROFILE);
        //
        profileName = "";
        poiCategoryList = new ArrayList<PoiCategory>();
        includeFavorites = true;

        switch (action) {
            case NEW:
            case MODIFY:
                if(savedInstanceState != null) {
                    profileName = savedInstanceState.getString(KEY_PROFILE_NAME);
                    poiCategoryList = (ArrayList<PoiCategory>) savedInstanceState.getSerializable(KEY_POI_CATEGORY_LIST);
                    includeFavorites = savedInstanceState.getBoolean(KEY_INCLUDE_FAVORITES);
                } else if (profile != null) {
                    profileName = profile.getName();
                    poiCategoryList = profile.getPoiCategoryList();
                    includeFavorites = profile.getIncludeFavorites();
                }

                // custom view
                final ViewGroup nullParent = null;
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View view = inflater.inflate(R.layout.dialog_manage_poi_profile, nullParent);

                EditText editProfileName = (EditText) view.findViewById(R.id.editProfileName);
                editProfileName.setText(profileName);
                editProfileName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            InputMethodManager imm =(InputMethodManager) GlobalInstance.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                            return true;
                        }
                        return false;
                    }
                });
                editProfileName.addTextChangedListener(new TextChangedListener<EditText>(editProfileName) {
                    @Override public void onTextChanged(EditText view, Editable s) {
                        profileName = view.getText().toString();
                    }
                });

                buttonSelectPoiCategories = (Button) view.findViewById(R.id.buttonSelectPoiCategories);
                updateSelectPoiCategoriesButton();
                buttonSelectPoiCategories.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        SelectPoiCategoriesDialog dialog = SelectPoiCategoriesDialog.newInstance(poiCategoryList);
                        dialog.setTargetFragment(ManagePoiProfileDialog.this, 1);
                        dialog.show(getActivity().getSupportFragmentManager(), "SelectPoiCategoriesDialog");
                    }
                });

                Switch buttonIncludeFavorites = (Switch) view.findViewById(R.id.buttonIncludeFavorites);
                buttonIncludeFavorites.setChecked(includeFavorites);
                buttonIncludeFavorites.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                        includeFavorites = isChecked;
                    }
                });

                // create dialog
                return new AlertDialog.Builder(getActivity())
                    .setTitle(
                            action == Action.NEW
                            ? getResources().getString(R.string.newProfileDialogTitle)
                            : getResources().getString(R.string.modifyProfileDialogTitle))
                    .setView(view)
                    .setPositiveButton(
                            getResources().getString(R.string.dialogOK),
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

            case REMOVE:
                return new AlertDialog.Builder(getActivity())
                    .setMessage(
                            String.format(
                                getResources().getString(R.string.removeProfileDialogTitle),
                                profile.getName())
                            )
                    .setPositiveButton(
                            getResources().getString(R.string.dialogYes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                    .setNegativeButton(
                            getResources().getString(R.string.dialogNo),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                    .create();

            default:
                return null;
        }
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (executeAction()) {
                        dismiss();
                    }
                }
            });
            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(KEY_PROFILE_NAME, profileName);
        savedInstanceState.putSerializable(KEY_POI_CATEGORY_LIST, poiCategoryList);
        savedInstanceState.putBoolean(KEY_INCLUDE_FAVORITES, includeFavorites);
    }

    @Override public void poiCategoriesSelected(ArrayList<PoiCategory> newPoiCategoryList) {
        if (newPoiCategoryList != null) {
            poiCategoryList = newPoiCategoryList;
            updateSelectPoiCategoriesButton();
        }
    }

    private void updateSelectPoiCategoriesButton() {
        buttonSelectPoiCategories.setText(
                String.format(
                    getResources().getString(R.string.buttonSelectPoiCategories),
                    poiCategoryList.size())
                );
    }

    private boolean executeAction() {
        switch (action) {
            case NEW:
            case MODIFY:
                // empty profile name
                if (TextUtils.isEmpty(profileName)) {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(R.string.messageProfileNameMissing),
                            Toast.LENGTH_LONG).show();
                    return false;
                }

                // check for name duplicates
                for (PoiProfile existingProfile : PoiProfile.allProfiles()) {
                    if (this.profileName.equals(existingProfile.getName())
                            && (
                                   profile == null
                                || profile.getId() != existingProfile.getId())) {
                        Toast.makeText(
                                getActivity(),
                                String.format(
                                    getResources().getString(R.string.messageProfileExists), profileName),
                                Toast.LENGTH_LONG).show();
                        return false;
                    }
                }

                if (action == Action.NEW) {
                    profile = PoiProfile.create(
                            this.profileName, this.poiCategoryList, this.includeFavorites);
                    if (profile == null) {
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.messageCouldNotCreateProfile),
                                Toast.LENGTH_LONG).show();
                        return false;
                    } else if (listener != null) {
                        listener.poiProfileCreated(profile);
                    }

                } else {
                    boolean modifiedSuccessfully = profile.setValues(
                            this.profileName, this.poiCategoryList, this.includeFavorites);
                    if (! modifiedSuccessfully) {
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.messageCouldNotModifyProfile),
                                Toast.LENGTH_LONG).show();
                        return false;
                    } else if (listener != null) {
                        listener.poiProfileModified(profile);
                    }
                }

                return true;

            case REMOVE:
                boolean removedSuccessfully = profile.removeFromDatabase();
                if (! removedSuccessfully) {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(R.string.messageCouldNotRemoveProfile),
                            Toast.LENGTH_LONG).show();
                    return false;
                } else if (listener != null) {
                    listener.poiProfileRemoved(profile);
                }
                return true;

            default:
                return false;
        }
    }

}
