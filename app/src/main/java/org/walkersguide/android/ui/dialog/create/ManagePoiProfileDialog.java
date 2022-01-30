package org.walkersguide.android.ui.dialog.create;

import org.walkersguide.android.server.wg.poi.PoiProfile;

import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.ui.UiHelper;
import org.walkersguide.android.server.wg.poi.PoiCategory;

import org.walkersguide.android.ui.dialog.select.SelectPoiCategoriesDialog;
import org.walkersguide.android.R;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.View;

import java.util.ArrayList;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import timber.log.Timber;


public class ManagePoiProfileDialog extends DialogFragment implements FragmentResultListener {
    public static final String REQUEST_MANAGE_POI_PROFILE = "managePoiProfile";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_POI_PROFILE = "poiProfile";


    // instance constructors

    public static ManagePoiProfileDialog createProfile() {
        ManagePoiProfileDialog dialog = new ManagePoiProfileDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ACTION, Action.CREATE);
        args.putSerializable(KEY_PROFILE, null);
        dialog.setArguments(args);
        return dialog;
    }

    public static ManagePoiProfileDialog modifyProfile(long profileId) {
        ManagePoiProfileDialog dialog = new ManagePoiProfileDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ACTION, Action.MODIFY);
        args.putSerializable(KEY_PROFILE, PoiProfile.load(profileId));
        dialog.setArguments(args);
        return dialog;
    }

    public static ManagePoiProfileDialog removeProfile(long profileId) {
        ManagePoiProfileDialog dialog = new ManagePoiProfileDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ACTION, Action.REMOVE);
        args.putSerializable(KEY_PROFILE, PoiProfile.load(profileId));
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_ACTION = "action";
    private static final String KEY_PROFILE = "profile";
    //
    private static final String KEY_PROFILE_NAME = "profileName";
    private static final String KEY_POI_CATEGORY_LIST = "poiCategoryList";
    private static final String KEY_INCLUDE_FAVORITES = "includeFavorites";

    public enum Action {
        CREATE, MODIFY, REMOVE
    }

    private Action action;
    private PoiProfile profile;

    private EditTextAndClearInputButton layoutProfileName;
    private Button buttonSelectPoiCategories;
    private SwitchCompat switchIncludeFavorites;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectPoiCategoriesDialog.REQUEST_SELECT_POI_CATEGORIES, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectPoiCategoriesDialog.REQUEST_SELECT_POI_CATEGORIES)) {
            updateSelectPoiCategoriesButton(
                    (ArrayList<PoiCategory>) bundle.getSerializable(SelectPoiCategoriesDialog.EXTRA_POI_CATEGORY_LIST));
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        action = (Action) getArguments().getSerializable(KEY_ACTION);
        profile = (PoiProfile) getArguments().getSerializable(KEY_PROFILE);

        String profileName = "";
        ArrayList<PoiCategory> poiCategoryList = new ArrayList<PoiCategory>();
        boolean includeFavorites = false;

        switch (action) {
            case CREATE:
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

                layoutProfileName = (EditTextAndClearInputButton) view.findViewById(R.id.layoutProfileName);
                layoutProfileName.setInputText(profileName);
                layoutProfileName.setLabelText(getResources().getString(R.string.labelProfileName));
                layoutProfileName.setEditorAction(
                        EditorInfo.IME_ACTION_DONE,
                        new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                            @Override public void onSelectedActionClicked() {
                                UiHelper.hideKeyboard(ManagePoiProfileDialog.this);
                            }
                        });

                buttonSelectPoiCategories = (Button) view.findViewById(R.id.buttonSelectPoiCategories);
                updateSelectPoiCategoriesButton(poiCategoryList);
                buttonSelectPoiCategories.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        SelectPoiCategoriesDialog.newInstance(
                                (ArrayList<PoiCategory>) view.getTag())
                            .show(getChildFragmentManager(), "SelectPoiCategoriesDialog");
                    }
                });

                switchIncludeFavorites = (SwitchCompat) view.findViewById(R.id.switchIncludeFavorites);
                switchIncludeFavorites.setChecked(includeFavorites);

                // create dialog
                return new AlertDialog.Builder(getActivity())
                    .setTitle(
                            action == Action.CREATE
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
                    executeAction();
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
        savedInstanceState.putString(
                KEY_PROFILE_NAME, layoutProfileName.getInputText());
        savedInstanceState.putSerializable(
                KEY_POI_CATEGORY_LIST, (ArrayList<PoiCategory>) buttonSelectPoiCategories.getTag());
        savedInstanceState.putBoolean(
                KEY_INCLUDE_FAVORITES, switchIncludeFavorites.isChecked());
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }

    private void updateSelectPoiCategoriesButton(ArrayList<PoiCategory> poiCategoryList) {
        buttonSelectPoiCategories.setTag(poiCategoryList);
        buttonSelectPoiCategories.setText(
                String.format(
                    getResources().getString(R.string.buttonSelectPoiCategories),
                    poiCategoryList.size())
                );
    }

    private void executeAction() {
        switch (action) {
            case CREATE:
            case MODIFY:
                String profileName = layoutProfileName.getInputText();
                ArrayList<PoiCategory> poiCategoryList = (ArrayList<PoiCategory>) buttonSelectPoiCategories.getTag();
                boolean includeFavorites = switchIncludeFavorites.isChecked();

                // empty profile name
                if (TextUtils.isEmpty(profileName)) {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(R.string.messageProfileNameMissing),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // check for name duplicates
                for (PoiProfile existingProfile : PoiProfile.allProfiles()) {
                    if (profileName.equals(existingProfile.getName())
                            && (
                                   profile == null
                                || profile.getId() != existingProfile.getId())) {
                        Toast.makeText(
                                getActivity(),
                                String.format(
                                    getResources().getString(R.string.messageProfileExists), profileName),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                if (action == Action.CREATE) {
                    profile = PoiProfile.create(
                            profileName, poiCategoryList, includeFavorites);
                    if (profile == null) {
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.messageCouldNotCreateProfile),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                } else {
                    boolean modifiedSuccessfully = profile.setValues(
                            profileName, poiCategoryList, includeFavorites);
                    if (! modifiedSuccessfully) {
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.messageCouldNotModifyProfile),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                break;

            case REMOVE:
                boolean removedSuccessfully = profile.removeFromDatabase();
                if (! removedSuccessfully) {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(R.string.messageCouldNotRemoveProfile),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                break;

            default:
                return;
        }

        Bundle result = new Bundle();
        result.putSerializable(EXTRA_ACTION, action);
        result.putSerializable(EXTRA_POI_PROFILE, profile);
        getParentFragmentManager().setFragmentResult(REQUEST_MANAGE_POI_PROFILE, result);
        dismiss();
    }

}
