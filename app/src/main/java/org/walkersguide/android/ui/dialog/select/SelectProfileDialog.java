package org.walkersguide.android.ui.dialog.select;

import org.walkersguide.android.data.profile.Profile;
import org.walkersguide.android.database.profile.FavoritesProfile;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.data.profile.ProfileGroup;

import org.walkersguide.android.ui.dialog.create.ManagePoiProfileDialog;
import org.walkersguide.android.ui.dialog.create.ManagePoiProfileDialog.Action;

import org.walkersguide.android.R;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Button;
import android.view.KeyEvent;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import android.view.Menu;
import org.walkersguide.android.util.GlobalInstance;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.widget.CheckedTextView;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import org.walkersguide.android.util.SettingsManager;
import android.widget.BaseAdapter;
import timber.log.Timber;


public class SelectProfileDialog extends DialogFragment implements FragmentResultListener {
    public static final String REQUEST_SELECT_PROFILE = "selectProfile";
    public static final String EXTRA_PROFILE = "profile";


    // instance constructors

    public static SelectProfileDialog newInstance(ProfileGroup profileGroup) {
        return newInstance(profileGroup, null);
    }

    public static SelectProfileDialog newInstance(ProfileGroup profileGroup, Profile selectedProfile) {
        SelectProfileDialog dialog= new SelectProfileDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_GROUP, profileGroup);
        args.putSerializable(KEY_SELECTED_PROFILE, selectedProfile);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_GROUP = "profileGroup";
    private static final String KEY_SELECTED_PROFILE = "selectedProfile";
    private static final String KEY_SELECTED_PROFILE_MODIFIED = "selectedProfileModified";

    private ProfileGroup profileGroup;
    private Profile selectedProfile;
    private boolean selectedProfileModified;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ManagePoiProfileDialog.REQUEST_MANAGE_POI_PROFILE, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(ManagePoiProfileDialog.REQUEST_MANAGE_POI_PROFILE)) {
            Action action = (Action) bundle.getSerializable(ManagePoiProfileDialog.EXTRA_ACTION);
            PoiProfile newProfile = (PoiProfile) bundle.getSerializable(ManagePoiProfileDialog.EXTRA_POI_PROFILE);
            if (action == Action.CREATE) {
                if (selectedProfile == null) {
                    selectedProfile = newProfile;
                    selectedProfileModified = true;
                }
            } else if (action == Action.MODIFY) {
                if (newProfile != null
                        && newProfile.equals(selectedProfile)) {
                    selectedProfileModified = true;
                }
            } else if (action == Action.REMOVE) {
                if (newProfile != null
                        && newProfile.equals(selectedProfile)) {
                    selectedProfile = null;
                    selectedProfileModified = true;
                }
            }
            fillProfilesListView();
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        profileGroup = (ProfileGroup) getArguments().getSerializable(KEY_GROUP);
        selectedProfile = (Profile) getArguments().getSerializable(KEY_SELECTED_PROFILE);
        if(savedInstanceState != null) {
            selectedProfileModified = savedInstanceState.getBoolean(KEY_SELECTED_PROFILE_MODIFIED);
        } else {
            selectedProfileModified = false;
        }

        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectProfileDialogTitle))
            .setItems(
                    new String[]{getResources().getString(R.string.messagePleaseWait)},
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .setNeutralButton(
                    getResources().getString(R.string.dialogNew),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogClose),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {

            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setVisibility(
                    profileGroup == ProfileGroup.POI ? View.VISIBLE : View.GONE);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    ManagePoiProfileDialog.createProfile()
                        .show(getChildFragmentManager(), "ManagePoiProfileDialog");
                }
            });

            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });

            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    Profile newProfile = (Profile) parent.getItemAtPosition(position);
                    Timber.d("onClick: %1$s", newProfile);
                    if (newProfile != null) {
                        Bundle result = new Bundle();
                        result.putSerializable(EXTRA_PROFILE, newProfile);
                        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_PROFILE, result);
                    }
                    dismiss();
                }
            });
            listViewItems.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                    if (profileGroup == ProfileGroup.POI) {
                        showProfileContextMenu(
                                (Profile) parent.getItemAtPosition(position), view);
                        return true;
                    }
                    return false;
                }
            });
        }

        fillProfilesListView();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(KEY_SELECTED_PROFILE_MODIFIED, selectedProfileModified);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (selectedProfileModified
                && ! getActivity().isChangingConfigurations()) {
            Bundle result = new Bundle();
            result.putSerializable(EXTRA_PROFILE, selectedProfile);
            getParentFragmentManager().setFragmentResult(REQUEST_SELECT_PROFILE, result);
        }
    }

    private void fillProfilesListView() {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            ArrayList<? extends Profile> profileList = null;
            if (profileGroup == ProfileGroup.FAVORITES) {
                profileList = FavoritesProfile.favoritesProfileList();
            } else if (profileGroup == ProfileGroup.POINT_HISTORY) {
                profileList = DatabaseProfile.pointHistoryProfileList();
            } else if (profileGroup == ProfileGroup.ROUTE_HISTORY) {
                profileList = DatabaseProfile.routeHistoryProfileList();
            } else if (profileGroup == ProfileGroup.POI) {
                profileList = PoiProfile.allProfiles();
            }

            if (profileList != null) {
                ListView listViewItems = (ListView) dialog.getListView();
                listViewItems.setAdapter(
                        new ProfileAdapter(
                            SelectProfileDialog.this.getContext(), profileList, selectedProfile));
                listViewItems.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            }
        }
    }


    /**
     * list menus
     */
    private static final int MENU_ITEM_EDIT_PROFILE = 1;
    private static final int MENU_ITEM_REMOVE_PROFILE = 2;

    private void showProfileContextMenu(Profile profile, View view) {
        if (profile instanceof PoiProfile) {
            final PoiProfile poiProfile = (PoiProfile) profile;

            PopupMenu profileContextMenu = new PopupMenu(getActivity(), view);
            profileContextMenu.getMenu().add(
                    Menu.NONE, MENU_ITEM_EDIT_PROFILE, 1, GlobalInstance.getStringResource(R.string.poiProfileMenuItemEdit));
            profileContextMenu.getMenu().add(
                    Menu.NONE, MENU_ITEM_REMOVE_PROFILE, 2, GlobalInstance.getStringResource(R.string.poiProfileMenuItemRemove));

            profileContextMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case MENU_ITEM_EDIT_PROFILE:
                            ManagePoiProfileDialog.modifyProfile(poiProfile)
                                .show(getChildFragmentManager(), "ManagePoiProfileDialog");
                            return true;
                        case MENU_ITEM_REMOVE_PROFILE:
                            ManagePoiProfileDialog.removeProfile(poiProfile)
                                .show(getChildFragmentManager(), "ManagePoiProfileDialog");
                            return true;
                        default:
                            return false;
                    }
                }
            });

            profileContextMenu.show();
        }
    }


    /**
     * profile adapter
     */

    private class ProfileAdapter extends BaseAdapter {

        private Context context;
        private ArrayList<? extends Profile> profileList;
        private Profile selectedProfile;

        public ProfileAdapter(Context context, ArrayList<? extends Profile> profileList, Profile selectedProfile) {
            this.context = context;
            this.profileList = profileList;
            this.selectedProfile = selectedProfile;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Profile profile = getItem(position);

            // load item layout
            EntryHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(this.context);
                convertView = inflater.inflate(R.layout.layout_checked_text_view_and_action_button, parent, false);
                holder = new EntryHolder();
                holder.labelProfile = (CheckedTextView) convertView.findViewById(R.id.labelProfile);
                holder.buttonActionFor = (ImageButton) convertView.findViewById(R.id.buttonActionFor);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }

            // label
            holder.labelProfile.setText(profile.toString());
            holder.labelProfile.setChecked(
                    profile.equals(this.selectedProfile));

            // action button
            if (profileGroup == ProfileGroup.POI
                    && SettingsManager.getInstance().getShowActionButton()) {
                holder.buttonActionFor.setContentDescription(
                        String.format(
                            "%1$s %2$s",
                            context.getResources().getString(R.string.buttonActionFor),
                            profile.getName())
                        );
                holder.buttonActionFor.setTag(profile);
                holder.buttonActionFor.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        showProfileContextMenu((Profile) view.getTag(), view);
                    }
                });
                holder.buttonActionFor.setVisibility(View.VISIBLE);
            } else {
                holder.buttonActionFor.setVisibility(View.GONE);
            }

            return convertView;
        }

        @Override public int getCount() {
            if (this.profileList != null) {
                return this.profileList.size();
            }
            return 0;
        }

        @Override public Profile getItem(int position) {
            if (this.profileList != null) {
                return this.profileList.get(position);
            }
            return null;
        }


        @Override public long getItemId(int position) {
            return position;
        }

        private class EntryHolder {
            public CheckedTextView labelProfile;
            private ImageButton buttonActionFor;
        }
    }

}
