package org.walkersguide.android.ui.dialog.selectors;

import org.walkersguide.android.database.profiles.DatabaseRouteProfile;
import org.walkersguide.android.database.profiles.DatabaseSegmentProfile;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
import org.walkersguide.android.R;
import org.walkersguide.android.database.SortMethod;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import android.app.Activity;
import java.util.Arrays;


public class SelectDatabaseProfileDialog extends DialogFragment {
    private static final String KEY_PROFILE_LIST = "profileList";
    private static final String KEY_SELECTED_PROFILE = "selectedProfile";

    public interface SelectDatabaseProfileListener {
        public void databaseProfileSelected(DatabaseProfile newProfile);
    }


    // Store instance variables
    private SelectDatabaseProfileListener listener;
    private ArrayList<DatabaseProfile> profileList;
    private DatabaseProfile selectedProfile;

    public static SelectDatabaseProfileDialog pointProfiles(DatabaseProfile selectedProfile) {
        ArrayList<DatabaseProfile> profileList = new ArrayList<DatabaseProfile>(Arrays.asList(DatabasePointProfile.values()));
        return newInstance(profileList, selectedProfile);
    }

    public static SelectDatabaseProfileDialog routeProfiles(DatabaseProfile selectedProfile) {
        ArrayList<DatabaseProfile> profileList = new ArrayList<DatabaseProfile>(Arrays.asList(DatabaseRouteProfile.values()));
        return newInstance(profileList, selectedProfile);
    }

    public static SelectDatabaseProfileDialog segmentProfiles(DatabaseProfile selectedProfile) {
        ArrayList<DatabaseProfile> profileList = new ArrayList<DatabaseProfile>(Arrays.asList(DatabaseSegmentProfile.values()));
        return newInstance(profileList, selectedProfile);
    }

    private static SelectDatabaseProfileDialog newInstance(
            ArrayList<DatabaseProfile> profileList, DatabaseProfile selectedProfile) {
        SelectDatabaseProfileDialog dialog= new SelectDatabaseProfileDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_PROFILE_LIST, profileList);
        args.putSerializable(KEY_SELECTED_PROFILE, selectedProfile);
        dialog.setArguments(args);
        return dialog;
    }


    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof SelectDatabaseProfileListener) {
            listener = (SelectDatabaseProfileListener) getTargetFragment();
        } else if (context instanceof Activity
                && (Activity) context instanceof SelectDatabaseProfileListener) {
            listener = (SelectDatabaseProfileListener) context;
                }
    }

    @Override public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        profileList = (ArrayList<DatabaseProfile>) getArguments().getSerializable(KEY_PROFILE_LIST);
        selectedProfile = (DatabaseProfile) getArguments().getSerializable(KEY_SELECTED_PROFILE);

        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectProfileDialogTitle))
            .setItems(
                    new String[]{getResources().getString(R.string.messagePleaseWait)},
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    }
                    )
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {

            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    DatabaseProfile newProfile = (DatabaseProfile) parent.getItemAtPosition(position);
                    if (newProfile != null) {
                        if (listener != null) {
                            listener.databaseProfileSelected(newProfile);
                        }
                    }
                    dismiss();
                }
            });

            // fill listview
            listViewItems.setAdapter(
                    new ArrayAdapter<DatabaseProfile>(
                        getActivity(), android.R.layout.select_dialog_singlechoice, profileList));
            listViewItems.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            // select list item
            if (selectedProfile != null) {
                listViewItems.setItemChecked(
                        profileList.indexOf(selectedProfile), true);
            }
        }
    }

}
