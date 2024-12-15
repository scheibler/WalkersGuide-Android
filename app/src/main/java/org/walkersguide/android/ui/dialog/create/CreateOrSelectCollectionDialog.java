package org.walkersguide.android.ui.dialog.create;

import org.walkersguide.android.database.profile.Collection;
import android.widget.CompoundButton.OnCheckedChangeListener;
import org.walkersguide.android.ui.view.ProfileView;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.ui.UiHelper;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;


import android.view.inputmethod.EditorInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.Toast;



import org.walkersguide.android.R;

import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import org.walkersguide.android.util.GlobalInstance;

import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import android.widget.RadioButton;
import org.walkersguide.android.data.Profile;
import android.widget.CompoundButton;
import org.walkersguide.android.ui.fragment.profile_list.CollectionListFragment;
import org.walkersguide.android.ui.fragment.ProfileListFragment;


public class CreateOrSelectCollectionDialog extends DialogFragment implements FragmentResultListener {
    public static final String REQUEST_SUCCESSFUL = "requestCreateOrSelectCollectionDialogSuccessful";
    public static final String EXTRA_SELECTED_COLLECTION = "selectedCollection";

    public static CreateOrSelectCollectionDialog newInstance(String suggestedCollectionName, boolean createNewCollectionOnly) {
        CreateOrSelectCollectionDialog dialog = new CreateOrSelectCollectionDialog();
        Bundle args = new Bundle();
        args.putString(KEY_SUGGESTED_COLLECTION_NAME, suggestedCollectionName);
        args.putBoolean(KEY_CREATE_NEW_COLLECTION_ONLY, createNewCollectionOnly);
        dialog.setArguments(args);
        return dialog;
    }

    // dialog
    private static final String KEY_SUGGESTED_COLLECTION_NAME = "suggestedCollectionName";
    private static final String KEY_CREATE_NEW_COLLECTION_ONLY = "createNewCollectionOnly";
    private static final String KEY_RADIO_BUTTON_NEW_COLLECTION_IS_CHECKED = "radioButtonNewCollectionIsChecked";
    private static final String KEY_SELECTED_EXISTING_DATABASE_PROFILE = "selectedExistingDatabaseProfile";

    private boolean radioButtonNewCollectionIsChecked;

    // new collection
    private RadioButton radioButtonNewCollection;
    private EditTextAndClearInputButton layoutNewCollectionName;

    // add to existing DatabaseProfile
    private RadioButton radioButtonExistingDatabaseProfile;
    private ProfileView layoutExistingDatabaseProfile;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        radioButtonNewCollectionIsChecked = savedInstanceState != null
            ? savedInstanceState.getBoolean(KEY_RADIO_BUTTON_NEW_COLLECTION_IS_CHECKED)
            : true;
        getChildFragmentManager()
            .setFragmentResultListener(
                    ProfileListFragment.REQUEST_SELECT_PROFILE, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(ProfileListFragment.REQUEST_SELECT_PROFILE)) {
            Profile selectedProfile = (Profile) bundle.getSerializable(ProfileListFragment.EXTRA_PROFILE);
            if (selectedProfile instanceof Collection) {
                setSelectedExistingDatabaseProfile((Collection) selectedProfile);
            }
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_or_select_database_profile, nullParent);

        // new collection
        radioButtonNewCollection = (RadioButton) view.findViewById(R.id.radioButtonNewCollection);
        radioButtonNewCollection.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    layoutNewCollectionName.setVisibility(View.VISIBLE);
                    // uncheck existing profile radio button
                    radioButtonExistingDatabaseProfile.setChecked(false);
                } else {
                    layoutNewCollectionName.setVisibility(View.GONE);
                }
                radioButtonNewCollectionIsChecked = isChecked;
            }
        });

        layoutNewCollectionName = (EditTextAndClearInputButton) view.findViewById(R.id.layoutNewCollectionName);
        layoutNewCollectionName.setLabelText(
                getResources().getString(R.string.layoutCollectionName));
        layoutNewCollectionName.setInputText(
                getArguments().getString(KEY_SUGGESTED_COLLECTION_NAME));
        layoutNewCollectionName.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        execute();
                    }
                });

        // existing database profile
        radioButtonExistingDatabaseProfile = (RadioButton) view.findViewById(R.id.radioButtonExistingDatabaseProfile);
        radioButtonExistingDatabaseProfile.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    layoutExistingDatabaseProfile.setVisibility(View.VISIBLE);
                    // uncheck new collection radio button
                    radioButtonNewCollection.setChecked(false);
                } else {
                    layoutExistingDatabaseProfile.setVisibility(View.GONE);
                }
            }
        });

        layoutExistingDatabaseProfile = (ProfileView) view.findViewById(R.id.layoutExistingDatabaseProfile);
        layoutExistingDatabaseProfile.setOnProfileDefaultActionListener(new ProfileView.OnProfileDefaultActionListener() {
            @Override public void onProfileDefaultActionClicked(Profile profile) {
                CollectionListFragment.selectProfile()
                    .show(getChildFragmentManager(), "CollectionListFragment");
            }
        });
        setSelectedExistingDatabaseProfile(
                savedInstanceState != null
                ? (DatabaseProfile) savedInstanceState.getSerializable(KEY_SELECTED_EXISTING_DATABASE_PROFILE)
                : null);

        // create dialog
        return new AlertDialog.Builder(getActivity())
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
        if(dialog == null) return;

        // positive button
        Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        buttonPositive.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                execute();
            }
        });

        // negative button
        Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        buttonNegative.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                dismiss();
            }
        });

        if (getArguments().getBoolean(KEY_CREATE_NEW_COLLECTION_ONLY)) {
            radioButtonNewCollectionIsChecked = true;
            radioButtonNewCollection.setVisibility(View.GONE);
            radioButtonExistingDatabaseProfile.setVisibility(View.GONE);
            layoutExistingDatabaseProfile.setVisibility(View.GONE);

        } else {
            layoutNewCollectionName.setVisibility(View.GONE);
            layoutExistingDatabaseProfile.setVisibility(View.GONE);
            if (radioButtonNewCollectionIsChecked) {
                radioButtonNewCollection.setChecked(true);
            } else {
                radioButtonExistingDatabaseProfile.setChecked(true);
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(KEY_RADIO_BUTTON_NEW_COLLECTION_IS_CHECKED, this.radioButtonNewCollectionIsChecked);
        savedInstanceState.putSerializable(KEY_SELECTED_EXISTING_DATABASE_PROFILE, layoutExistingDatabaseProfile.getProfile());
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }

    private void setSelectedExistingDatabaseProfile(DatabaseProfile profile) {
        layoutExistingDatabaseProfile.configureAsSingleObject(profile);
    }

    private void execute() {
        Collection collectionToAddObjectsTo = null;
        if (radioButtonNewCollectionIsChecked) {
            // create new collection

            final String newCollectionName = layoutNewCollectionName.getInputText();
            if (TextUtils.isEmpty(newCollectionName)) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.messageCollectionNameMissing),
                        Toast.LENGTH_LONG)
                    .show();
                return;
            }

            collectionToAddObjectsTo = Collection.create(newCollectionName, false);
            if (collectionToAddObjectsTo == null) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.messageCouldNotCreateCollection),
                        Toast.LENGTH_LONG)
                    .show();
                return;
            }

        } else {
            // add to existing database profile
            if (layoutExistingDatabaseProfile.getProfile() instanceof Collection) {
                collectionToAddObjectsTo = (Collection) layoutExistingDatabaseProfile.getProfile();
            }

            if (collectionToAddObjectsTo == null) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.messageNoProfileSelected),
                        Toast.LENGTH_LONG)
                    .show();
                return;
            }
        }

        Bundle result = new Bundle();
        result.putSerializable(EXTRA_SELECTED_COLLECTION, collectionToAddObjectsTo);
        getParentFragmentManager().setFragmentResult(REQUEST_SUCCESSFUL, result);
        dismiss();
    }
}
