package org.walkersguide.android.ui.dialog.select;

import org.walkersguide.android.ui.dialog.create.CreateOrSelectCollectionDialog;
import org.walkersguide.android.ui.fragment.profile_list.CollectionListFragment;
import org.walkersguide.android.database.profile.StaticProfile;
import org.walkersguide.android.data.profile.MutableProfile;
import org.walkersguide.android.ui.fragment.ProfileListFragment;
import org.walkersguide.android.ui.fragment.profile_list.PoiProfileListFragment;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.database.DatabaseProfile;
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


import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;
import android.widget.Toast;
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
        ADD_TO_PINNED_PROFILES, SET_AS_TRACKED_PROFILE, SAVE_CURRENT_LOCATION
    }

    private Target target;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getChildFragmentManager()
            .setFragmentResultListener(
                    ProfileListFragment.REQUEST_SELECT_PROFILE, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(ProfileListFragment.REQUEST_SELECT_PROFILE)) {
            profileSelected((Profile) bundle.getSerializable(ProfileListFragment.EXTRA_PROFILE));
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        target = (Target) getArguments().getSerializable(KEY_TARGET);

        String dialogTitle = null;
        switch (target) {
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
                case ADD_TO_PINNED_PROFILES:
                case SET_AS_TRACKED_PROFILE:
                    sourceActionList.add(SourceAction.COLLECTIONS);
                    sourceActionList.add(SourceAction.POI_PROFILES);
                    break;
                case SAVE_CURRENT_LOCATION:
                    sourceActionList.add(SourceAction.STATIC_PROFILE_PINNED_OBJECTS_WITH_ID);
                    sourceActionList.add(SourceAction.STATIC_PROFILE_TRACKED_OBJECTS_WITH_ID);
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
        POI_PROFILES(GlobalInstance.getStringResource(R.string.profileSelectFromPoiProfiles));

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
        }

        Bundle result = new Bundle();
        result.putSerializable(EXTRA_TARGET, target);
        result.putSerializable(EXTRA_PROFILE, profile);
        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_PROFILE, result);
        dismiss();
    }

}
