package org.walkersguide.android.ui.dialog.select;

import org.walkersguide.android.ui.dialog.create.ImportGpxFileDialog;
import org.walkersguide.android.ui.fragment.profile_list.CollectionListFragment;
import org.walkersguide.android.database.profile.StaticProfile;
import org.walkersguide.android.data.profile.MutableProfile;
import org.walkersguide.android.ui.fragment.ProfileListFragment;
import org.walkersguide.android.ui.fragment.profile_list.PoiProfileListFragment;
import org.walkersguide.android.ui.dialog.create.SaveCurrentLocationDialog;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
import org.walkersguide.android.ui.fragment.object_list.extended.PoiListFromServerFragment;
import org.walkersguide.android.ui.dialog.create.EnterAddressDialog;
import org.walkersguide.android.ui.dialog.create.EnterCoordinatesDialog;
import org.walkersguide.android.ui.dialog.create.PointFromCoordinatesLinkDialog;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;


import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;


import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import java.util.Arrays;
import org.walkersguide.android.util.GlobalInstance;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.ui.fragment.ObjectListFragment;
import org.walkersguide.android.ui.dialog.WhereAmIDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.util.SettingsManager;
import android.widget.Toast;
import org.walkersguide.android.ui.dialog.template.EnterStringDialog;
import org.walkersguide.android.database.profile.Collection;



public class SelectProfileFromMultipleSourcesDialog extends DialogFragment implements FragmentResultListener {
    public static final String REQUEST_SELECT_PROFILE = "selectProfileFromMultipleSources";
    public static final String EXTRA_PROFILE = "profileFromMultipleSources";
    public static final String EXTRA_TARGET = "targetForProfileFromMultipleSources";


    // instance constructors

    public static SelectProfileFromMultipleSourcesDialog newInstance(Target target) {
        SelectProfileFromMultipleSourcesDialog dialog = new SelectProfileFromMultipleSourcesDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_TARGET, target);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_TARGET = "target";

    public enum Target {
        CREATE_COLLECTION, ADD_TO_PINNED_PROFILES, SET_AS_TRACKED_PROFILE, SAVE_CURRENT_LOCATION, GPX_FILE_IMPORT
    }

    private Target target;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getChildFragmentManager()
            .setFragmentResultListener(
                    CreateEmptyCollectionDialog.REQUEST_CREATE_EMPTY_COLLECTION_WAS_SUCCESSFUL, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ProfileListFragment.REQUEST_SELECT_PROFILE, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ImportGpxFileDialog.REQUEST_IMPORT_OF_GPX_FILE_WAS_SUCCESSFUL, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(CreateEmptyCollectionDialog.REQUEST_CREATE_EMPTY_COLLECTION_WAS_SUCCESSFUL)) {
            profileSelected((DatabaseProfile) bundle.getSerializable(CreateEmptyCollectionDialog.EXTRA_EMPTY_COLLECTION));
        } else if (requestKey.equals(ProfileListFragment.REQUEST_SELECT_PROFILE)) {
            profileSelected((Profile) bundle.getSerializable(ProfileListFragment.EXTRA_PROFILE));
        } else if (requestKey.equals(ImportGpxFileDialog.REQUEST_IMPORT_OF_GPX_FILE_WAS_SUCCESSFUL)) {
            profileSelected((DatabaseProfile) bundle.getSerializable(ImportGpxFileDialog.EXTRA_GPX_FILE_PROFILE));
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        target = (Target) getArguments().getSerializable(KEY_TARGET);

        String dialogTitle = null;
        switch (target) {
            case CREATE_COLLECTION:
                dialogTitle = getResources().getString(R.string.selectProfileFromMultipleSourcesDialogTitleCreate);
                break;
            case ADD_TO_PINNED_PROFILES:
                dialogTitle = getResources().getString(R.string.selectProfileFromMultipleSourcesDialogTitlePin);
                break;
            case SET_AS_TRACKED_PROFILE:
                dialogTitle = getResources().getString(R.string.selectProfileFromMultipleSourcesDialogTitleTrack);
                break;
            default:
                dialogTitle = getResources().getString(R.string.selectProfileFromMultipleSourcesDialogTitle);
        }

        return new AlertDialog.Builder(getActivity())
            .setTitle(dialogTitle)
            .setItems(
                    new String[]{getResources().getString(R.string.messagePleaseWait)},
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

            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    executeAction(
                            (SourceAction) parent.getItemAtPosition(position));
                }
            });

            // fill listview
            ArrayList<SourceAction> sourceActionList = new ArrayList<SourceAction>();
            switch (target) {
                case CREATE_COLLECTION:
                    sourceActionList.add(SourceAction.EMPTY_COLLECTION);
                    sourceActionList.add(SourceAction.FROM_GPX_FILE);
                    break;
                case ADD_TO_PINNED_PROFILES:
                    sourceActionList.add(SourceAction.COLLECTIONS);
                    sourceActionList.add(SourceAction.POI_PROFILES);
                    sourceActionList.add(SourceAction.FROM_GPX_FILE);
                    break;
                case SET_AS_TRACKED_PROFILE:
                    sourceActionList.add(SourceAction.COLLECTIONS);
                    sourceActionList.add(SourceAction.POI_PROFILES);
                    break;
                case SAVE_CURRENT_LOCATION:
                    sourceActionList.add(SourceAction.STATIC_PROFILE_PINNED_OBJECTS_WITH_ID);
                    sourceActionList.add(SourceAction.STATIC_PROFILE_TRACKED_OBJECTS_WITH_ID);
                    sourceActionList.add(SourceAction.COLLECTIONS);
                    break;
                case GPX_FILE_IMPORT:
                    sourceActionList.add(SourceAction.COLLECTIONS);
                    break;
            }

            listViewItems.setAdapter(
                    new ArrayAdapter<SourceAction>(
                        getActivity(), android.R.layout.simple_list_item_1, sourceActionList));

            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });
        }
    }


    /**
     * SourceAction
     */

    private enum SourceAction {

        STATIC_PROFILE_PINNED_OBJECTS_WITH_ID(StaticProfile.pinnedObjectsWithId().getName()),
        STATIC_PROFILE_TRACKED_OBJECTS_WITH_ID(StaticProfile.trackedObjectsWithId().getName()),
        COLLECTIONS(GlobalInstance.getStringResource(R.string.profileSelectFromCollections)),
        POI_PROFILES(GlobalInstance.getStringResource(R.string.profileSelectFromPoiProfiles)),
        EMPTY_COLLECTION(GlobalInstance.getStringResource(R.string.profileSelectFromEmptyCollection)),
        FROM_GPX_FILE(GlobalInstance.getStringResource(R.string.profileSelectFromGpxFile));

        private String name;

        private SourceAction(String name) {
            this.name = name;
        }

        @Override public String toString() {
            return this.name;
        }
    }

    private void executeAction(SourceAction action) {
        switch (action) {

            case STATIC_PROFILE_PINNED_OBJECTS_WITH_ID:
                profileSelected(StaticProfile.pinnedObjectsWithId());
                break;

            case STATIC_PROFILE_TRACKED_OBJECTS_WITH_ID:
                profileSelected(StaticProfile.trackedObjectsWithId());
                break;

            case COLLECTIONS:
                CollectionListFragment.selectProfile()
                    .show(getChildFragmentManager(), "CollectionListFragment");
                break;

            case POI_PROFILES:
                PoiProfileListFragment.selectProfile()
                    .show(getChildFragmentManager(), "PoiProfileListFragment");
                break;

            case EMPTY_COLLECTION:
                CreateEmptyCollectionDialog.newInstance()
                    .show(getChildFragmentManager(), "CreateEmptyCollectionDialog");
                break;

            case FROM_GPX_FILE:
                ImportGpxFileDialog.newInstance(
                        target == Target.ADD_TO_PINNED_PROFILES ? true : false)
                    .show(getChildFragmentManager(), "ImportGpxFileDialog");
                break;
        }
    }

    private void profileSelected(Profile profile) {
        if (profile == null) {
            Toast.makeText(
                    getActivity(),
                    GlobalInstance.getStringResource(R.string.messageNoProfileSelected),
                    Toast.LENGTH_LONG)
                .show();
            return;
        }

        switch (target) {

            case ADD_TO_PINNED_PROFILES:
                if (! (profile instanceof MutableProfile)) {
                    Toast.makeText(
                            getActivity(),
                            GlobalInstance.getStringResource(R.string.messageProfileIncompatibleTargetAddToPinnedProfiles),
                            Toast.LENGTH_LONG)
                        .show();
                    return;
                }
                break;

            case SAVE_CURRENT_LOCATION:
                if (! (profile instanceof DatabaseProfile)) {
                    Toast.makeText(
                            getActivity(),
                            GlobalInstance.getStringResource(R.string.messageProfileIncompatibleTargetSaveCurrentLocation),
                            Toast.LENGTH_LONG)
                        .show();
                    return;
                }
                break;

            case GPX_FILE_IMPORT:
                if (! (profile instanceof DatabaseProfile)) {
                    Toast.makeText(
                            getActivity(),
                            GlobalInstance.getStringResource(R.string.messageProfileIncompatibleTargetGpxFileImport),
                            Toast.LENGTH_LONG)
                        .show();
                    return;
                }
                break;
        }

        Bundle result = new Bundle();
        result.putSerializable(EXTRA_TARGET, target);
        result.putSerializable(EXTRA_PROFILE, profile);
        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_PROFILE, result);
        dismiss();
    }


    public static class CreateEmptyCollectionDialog extends EnterStringDialog {
        public static final String REQUEST_CREATE_EMPTY_COLLECTION_WAS_SUCCESSFUL = "requestCreateEmptyCollectionWasSuccessful";
    public static final String EXTRA_EMPTY_COLLECTION = "emptyCollection";


        public static CreateEmptyCollectionDialog newInstance() {
            CreateEmptyCollectionDialog dialog = new CreateEmptyCollectionDialog();
            return dialog;
        }


        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            setDialogTitle(
                    getResources().getString(R.string.layoutCollectionName));
            setMissingInputMessage(
                    getResources().getString(R.string.messageCollectionNameMissing));
            return super.onCreateDialog(savedInstanceState);
        }

        @Override public void execute(String input) {
            Collection emptyCollection = Collection.create(input, false);
            if (emptyCollection == null) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageCouldNotCreateCollection),
                        Toast.LENGTH_LONG).show();
                return;
            }

            Bundle result = new Bundle();
            result.putSerializable(EXTRA_EMPTY_COLLECTION, emptyCollection);
            getParentFragmentManager().setFragmentResult(REQUEST_CREATE_EMPTY_COLLECTION_WAS_SUCCESSFUL, result);
            dismiss();
        }
    }

}
