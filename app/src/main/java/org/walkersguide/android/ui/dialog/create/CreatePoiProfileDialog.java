package org.walkersguide.android.ui.dialog.create;

import org.walkersguide.android.database.profile.Collection;
import org.walkersguide.android.server.wg.poi.PoiProfile;

import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import org.walkersguide.android.ui.UiHelper;
import org.walkersguide.android.server.wg.poi.PoiCategory;

import org.walkersguide.android.ui.dialog.select.SelectCollectionsDialog;
import org.walkersguide.android.ui.dialog.select.SelectPoiCategoriesDialog;
import org.walkersguide.android.R;
import androidx.appcompat.app.AlertDialog;
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
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.util.GlobalInstance;


public class CreatePoiProfileDialog extends DialogFragment implements FragmentResultListener {
    public static final String REQUEST_CREATE_POI_PROFILE_WAS_SUCCESSFUL = "requestCreatePoiProfileWasSuccessful";


    // instance constructors

    public static CreatePoiProfileDialog newInstance() {
        CreatePoiProfileDialog dialog = new CreatePoiProfileDialog();
        return dialog;
    }


    // dialog
    private static final String KEY_ACTION = "action";
    private static final String KEY_PROFILE = "profile";
    //
    private static final String KEY_PROFILE_NAME = "profileName";
    private static final String KEY_POI_CATEGORY_LIST = "poiCategoryList";
    private static final String KEY_COLLECTION_LIST = "collectionList";

    private ArrayList<PoiCategory> selectedPoiCategoryList;
    private ArrayList<Collection> selectedCollectionList;

    private EditTextAndClearInputButton layoutProfileName;
    private Button buttonSelectPoiCategories, buttonSelectCollections;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectPoiCategoriesDialog.REQUEST_SELECT_POI_CATEGORIES, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectCollectionsDialog.REQUEST_SELECT_COLLECTIONS, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectPoiCategoriesDialog.REQUEST_SELECT_POI_CATEGORIES)) {
            selectedPoiCategoryList = (ArrayList<PoiCategory>) bundle.getSerializable(SelectPoiCategoriesDialog.EXTRA_POI_CATEGORY_LIST);
            updateButtonText();
        } else if (requestKey.equals(SelectCollectionsDialog.REQUEST_SELECT_COLLECTIONS)) {
            selectedCollectionList = (ArrayList<Collection>) bundle.getSerializable(SelectCollectionsDialog.EXTRA_COLLECTION_LIST);
            updateButtonText();
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        selectedPoiCategoryList = savedInstanceState != null
            ? (ArrayList<PoiCategory>) savedInstanceState.getSerializable(KEY_POI_CATEGORY_LIST)
            : new ArrayList<PoiCategory>();
        selectedCollectionList = savedInstanceState != null
            ? (ArrayList<Collection>) savedInstanceState.getSerializable(KEY_COLLECTION_LIST)
            : new ArrayList<Collection>();

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_poi_profile, nullParent);

        layoutProfileName = (EditTextAndClearInputButton) view.findViewById(R.id.layoutProfileName);
        layoutProfileName.setInputText(
                savedInstanceState != null
                ? savedInstanceState.getString(KEY_PROFILE_NAME)
                : "");
        layoutProfileName.setLabelText(getResources().getString(R.string.labelProfileName));
        layoutProfileName.setEditorAction(
                EditorInfo.IME_ACTION_DONE,
                new EditTextAndClearInputButton.OnSelectedActionClickListener() {
                    @Override public void onSelectedActionClicked() {
                        UiHelper.hideKeyboard(CreatePoiProfileDialog.this);
                    }
                });

        buttonSelectPoiCategories = (Button) view.findViewById(R.id.buttonSelectPoiCategories);
        buttonSelectPoiCategories.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectPoiCategoriesDialog.newInstance(selectedPoiCategoryList)
                    .show(getChildFragmentManager(), "SelectPoiCategoriesDialog");
            }
        });

        buttonSelectCollections = (Button) view.findViewById(R.id.buttonSelectCollections);
        buttonSelectCollections.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectCollectionsDialog.newInstance(selectedCollectionList)
                    .show(getChildFragmentManager(), "SelectCollectionsDialog");
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.createPoiProfileDialogTitle))
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
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    tryToCreatePoiProfile();
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

        updateButtonText();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(
                KEY_PROFILE_NAME, layoutProfileName.getInputText());
        savedInstanceState.putSerializable(
                KEY_POI_CATEGORY_LIST, selectedPoiCategoryList);
        savedInstanceState.putSerializable(
                KEY_COLLECTION_LIST, selectedCollectionList);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }

    private void updateButtonText() {
        buttonSelectPoiCategories.setText(
                GlobalInstance.getPluralResource(
                    R.plurals.poiCategorySelected, selectedPoiCategoryList.size()));
        buttonSelectCollections.setText(
                GlobalInstance.getPluralResource(
                    R.plurals.collectionSelected, selectedCollectionList.size()));
    }

    private void tryToCreatePoiProfile() {
        String profileName = layoutProfileName.getInputText();
        if (TextUtils.isEmpty(profileName)) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messagePoiProfileNameMissing),
                    Toast.LENGTH_LONG).show();
            return;
        }

        PoiProfile newPoiProfile = PoiProfile.create(
                profileName, selectedPoiCategoryList, selectedCollectionList);
        if (newPoiProfile == null) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.messageCouldNotCreatePoiProfile),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Bundle result = new Bundle();
        getParentFragmentManager().setFragmentResult(REQUEST_CREATE_POI_PROFILE_WAS_SUCCESSFUL, result);
        dismiss();
    }

}
