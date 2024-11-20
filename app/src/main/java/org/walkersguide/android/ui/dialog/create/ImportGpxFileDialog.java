package org.walkersguide.android.ui.dialog.create;

import org.walkersguide.android.util.gpx.GpxFileReader;
import org.walkersguide.android.util.gpx.GpxFileReader.GpxFileParseResult;
import org.walkersguide.android.util.gpx.GpxFileReader.GpxFileParseException;
import androidx.core.view.ViewCompat;
import org.walkersguide.android.ui.dialog.select.SelectProfileFromMultipleSourcesDialog;
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

import java.util.ArrayList;


import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;

import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.walkersguide.android.util.GlobalInstance;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import org.walkersguide.android.data.ObjectWithId;
import android.widget.RadioButton;
import org.walkersguide.android.data.Profile;
import android.widget.CompoundButton;


public class ImportGpxFileDialog extends DialogFragment implements FragmentResultListener {
    public static final String REQUEST_IMPORT_OF_GPX_FILE_WAS_SUCCESSFUL = "requestImportOfGpxFileWasSuccessful";
    public static final String EXTRA_GPX_FILE_PROFILE = "gpxFileProfile";


    // instance constructors

    public static ImportGpxFileDialog newInstance(Uri uri, boolean pinCollection) {
        ImportGpxFileDialog dialog = new ImportGpxFileDialog();
        Bundle args = new Bundle();
        args.putParcelable(KEY_URI, uri);
        args.putBoolean(KEY_PIN_COLLECTION, pinCollection);
        dialog.setArguments(args);
        return dialog;
    }

    // dialog
    private static final String KEY_URI = "uri";
    private static final String KEY_PIN_COLLECTION = "pinCollection";
    private static final String KEY_FILE_PICKING_IN_PROGRESS = "filePickingInProgress";
    private static final String KEY_GPX_FILE_NAME = "gpxFileName";
    private static final String KEY_OBJECT_LIST = "objectList";
    private static final String KEY_RADIO_BUTTON_NEW_COLLECTION_IS_CHECKED = "radioButtonNewCollectionIsChecked";
    private static final String KEY_SELECTED_EXISTING_DATABASE_PROFILE = "selectedExistingDatabaseProfile";

    private boolean pinCollection, filePickingInProgress;
    private String gpxFileName;
    private ArrayList<ObjectWithId> objectList;
    private boolean radioButtonNewCollectionIsChecked;

    private TextView labelImportResult;
    // new collection
    private RadioButton radioButtonNewCollection;
    private EditTextAndClearInputButton layoutNewCollectionName;
    // add to existing DatabaseProfile
    private RadioButton radioButtonExistingDatabaseProfile;
    private ProfileView layoutExistingDatabaseProfile;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectProfileFromMultipleSourcesDialog.REQUEST_SELECT_PROFILE, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SimpleMessageDialog.REQUEST_DIALOG_CLOSED, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectProfileFromMultipleSourcesDialog.REQUEST_SELECT_PROFILE)) {
            SelectProfileFromMultipleSourcesDialog.Target profileTarget = (SelectProfileFromMultipleSourcesDialog.Target)
                bundle.getSerializable(SelectProfileFromMultipleSourcesDialog.EXTRA_TARGET);
            Profile selectedProfile = (Profile) bundle.getSerializable(SelectProfileFromMultipleSourcesDialog.EXTRA_PROFILE);
            if (profileTarget == SelectProfileFromMultipleSourcesDialog.Target.GPX_FILE_IMPORT
                    && selectedProfile instanceof DatabaseProfile) {
                setSelectedExistingDatabaseProfile((DatabaseProfile) selectedProfile);
                updateUI();
            }
        } else if (requestKey.equals(SimpleMessageDialog.REQUEST_DIALOG_CLOSED)) {
            dismiss();
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        pinCollection = getArguments().getBoolean(KEY_PIN_COLLECTION);

        if (savedInstanceState != null) {
            filePickingInProgress = savedInstanceState.getBoolean(KEY_FILE_PICKING_IN_PROGRESS);
            gpxFileName = savedInstanceState.getString(KEY_GPX_FILE_NAME);
            objectList = (ArrayList<ObjectWithId>) savedInstanceState.getSerializable(KEY_OBJECT_LIST);
            radioButtonNewCollectionIsChecked = savedInstanceState.getBoolean(KEY_RADIO_BUTTON_NEW_COLLECTION_IS_CHECKED);
        } else {
            filePickingInProgress = false;
            gpxFileName = null;
            objectList = null;
            radioButtonNewCollectionIsChecked = true;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_import_gpx_file, nullParent);

        labelImportResult = (TextView) view.findViewById(R.id.labelImportResult);

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
                SelectProfileFromMultipleSourcesDialog.newInstance(
                        SelectProfileFromMultipleSourcesDialog.Target.GPX_FILE_IMPORT)
                    .show(getChildFragmentManager(), "SelectProfileFromMultipleSourcesDialog");
            }
        });
        setSelectedExistingDatabaseProfile(
                savedInstanceState != null
                ? (DatabaseProfile) savedInstanceState.getSerializable(KEY_SELECTED_EXISTING_DATABASE_PROFILE)
                : null);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(
                    pinCollection
                    ? getResources().getString(R.string.importGpxFileDialogTitlePinned)
                    : getResources().getString(R.string.importGpxFileDialogTitle))
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

            if (gpxFileName == null) {
                if (filePickingInProgress) {
                    return;
                }

                ViewCompat.setAccessibilityLiveRegion(
                        labelImportResult, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
                labelImportResult.setText(
                        getResources().getString(R.string.messagePleaseWait));
                radioButtonNewCollection.setVisibility(View.GONE);
                layoutNewCollectionName.setVisibility(View.GONE);
                radioButtonExistingDatabaseProfile.setVisibility(View.GONE);
                layoutExistingDatabaseProfile.setVisibility(View.GONE);
                buttonPositive.setVisibility(View.GONE);

                Uri uri = getArguments().getParcelable(KEY_URI);
                if (uri == null) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/*");
                    startActivityForResult(intent, RC_SELECT_GPX_FILE);
                    filePickingInProgress = true;
                } else {
                    processUri(uri);
                }

            } else {
                updateUI();
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(KEY_FILE_PICKING_IN_PROGRESS, this.filePickingInProgress);
        if (this.gpxFileName != null) {
            savedInstanceState.putString(KEY_GPX_FILE_NAME, this.gpxFileName);
        }
        if (this.objectList != null) {
            savedInstanceState.putSerializable(KEY_OBJECT_LIST, this.objectList);
        }
        savedInstanceState.putBoolean(KEY_RADIO_BUTTON_NEW_COLLECTION_IS_CHECKED, this.radioButtonNewCollectionIsChecked);
        savedInstanceState.putSerializable(KEY_SELECTED_EXISTING_DATABASE_PROFILE, layoutExistingDatabaseProfile.getProfile());
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            UiHelper.hideKeyboard(this);
        }
    }

    private void updateUI() {
        labelImportResult.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelImportResult),
                    this.gpxFileName,
                    ObjectWithId.summarizeObjectListContents(this.objectList))
                );

        if (radioButtonNewCollectionIsChecked) {
            radioButtonNewCollection.setChecked(true);
        } else {
            radioButtonExistingDatabaseProfile.setChecked(true);
        }
        if (! pinCollection) {
            radioButtonNewCollection.setVisibility(View.VISIBLE);
            radioButtonExistingDatabaseProfile.setVisibility(View.VISIBLE);
        }

        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
        }
    }

    private void setSelectedExistingDatabaseProfile(DatabaseProfile profile) {
        layoutExistingDatabaseProfile.configureAsSingleObject(profile);
    }

    private void execute() {
        if (objectList == null) {
            return;
        }

        DatabaseProfile profileToAddObjectsTo = null;
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

            profileToAddObjectsTo = Collection.create(newCollectionName, pinCollection);
            if (profileToAddObjectsTo == null) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.messageCouldNotCreateCollection),
                        Toast.LENGTH_LONG)
                    .show();
                return;
            }

        } else {
            // add to existing database profile
            if (layoutExistingDatabaseProfile.getProfile() instanceof DatabaseProfile) {
                profileToAddObjectsTo = (DatabaseProfile) layoutExistingDatabaseProfile.getProfile();
            }

            if (profileToAddObjectsTo == null) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.messageNoProfileSelected),
                        Toast.LENGTH_LONG)
                    .show();
                return;
            }
        }

        for (ObjectWithId objectToAdd : objectList) {
            profileToAddObjectsTo.addObject(objectToAdd);
        }
        Toast.makeText(
                getActivity(),
                String.format(
                    GlobalInstance.getStringResource(R.string.messageAddObjectsToDatabaseProfileWasSuccessful),
                    ObjectWithId.summarizeObjectListContents(this.objectList),
                    profileToAddObjectsTo.getName()),
                Toast.LENGTH_LONG)
            .show();

        dismiss();
        Bundle result = new Bundle();
        result.putSerializable(EXTRA_GPX_FILE_PROFILE, profileToAddObjectsTo);
        getParentFragmentManager().setFragmentResult(REQUEST_IMPORT_OF_GPX_FILE_WAS_SUCCESSFUL, result);
    }


    // select gpx file
    private static final int BUFFER_SIZE = 1024;
    private static final int RC_SELECT_GPX_FILE = 31;

    @Override public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        filePickingInProgress = false;
        if (requestCode != RC_SELECT_GPX_FILE
                || resultCode != AppCompatActivity.RESULT_OK
                || resultData == null) {
            dismiss();
            return;
        }
        processUri(resultData.getData());
    }

    private void processUri(final Uri uri) {
        ViewCompat.setAccessibilityLiveRegion(
                labelImportResult, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);

        Executors.newSingleThreadExecutor().execute(() -> {

            GpxFileReader gpxFileReader = new GpxFileReader(uri);
            try {
                final GpxFileParseResult parseResult = gpxFileReader.read();
                (new Handler(Looper.getMainLooper())).post(() -> {
                    if (isAdded()) {
                        this.gpxFileName = parseResult.gpxFileName;
                        this.objectList = parseResult.objectList;
                        this.layoutNewCollectionName.setInputText(parseResult.getCollectionName());
                        updateUI();
                    }
                });

            } catch (GpxFileParseException e) {
                final GpxFileParseException parseException = e;
                (new Handler(Looper.getMainLooper())).post(() -> {
                    if (isAdded()) {
                        this.gpxFileName = null;
                        this.objectList = null;
                        this.labelImportResult.setText(parseException.getMessage());
                    }
                });
            }
        });
    }

}
