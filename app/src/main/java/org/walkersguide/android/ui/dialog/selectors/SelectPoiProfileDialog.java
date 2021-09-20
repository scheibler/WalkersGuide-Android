package org.walkersguide.android.ui.dialog.selectors;

import org.walkersguide.android.ui.dialog.creators.ManagePoiProfileDialog;
import org.walkersguide.android.ui.dialog.creators.ManagePoiProfileDialog.ManagePoiProfileListener;
import org.walkersguide.android.server.poi.PoiCategory;

import org.walkersguide.android.ui.dialog.selectors.SelectPoiCategoriesDialog;
import org.walkersguide.android.ui.dialog.selectors.SelectPoiCategoriesDialog.SelectPoiCategoriesListener;
import org.walkersguide.android.server.poi.PoiProfile;
import org.walkersguide.android.R;
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
import org.walkersguide.android.database.util.AccessDatabase;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Button;
import android.view.KeyEvent;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import android.view.Menu;
import org.walkersguide.android.util.GlobalInstance;
import android.view.MenuItem;
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
import android.widget.CheckedTextView;


public class SelectPoiProfileDialog extends DialogFragment implements ManagePoiProfileListener {
    private static final String KEY_SELECTED_PROFILE = "selectedProfile";
    private static final String KEY_SELECTED_PROFILE_MODIFIED = "selectedProfileModified";

    public interface SelectPoiProfileListener {
        public void poiProfileSelected(PoiProfile newProfile);
    }


    // Store instance variables
    private SelectPoiProfileListener listener;
    private PoiProfile selectedProfile;
    private boolean selectedProfileModified;

    public static SelectPoiProfileDialog newInstance(PoiProfile selectedProfile) {
        SelectPoiProfileDialog dialog= new SelectPoiProfileDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_PROFILE, selectedProfile);
        dialog.setArguments(args);
        return dialog;
    }


    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof SelectPoiProfileListener) {
            listener = (SelectPoiProfileListener) getTargetFragment();
        } else if (context instanceof Activity
                && (Activity) context instanceof SelectPoiProfileListener) {
            listener = (SelectPoiProfileListener) context;
                }
    }

    @Override public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // set attributes
        selectedProfile = (PoiProfile) getArguments().getSerializable(KEY_SELECTED_PROFILE);
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
                    getResources().getString(R.string.buttonNewProfile),
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
            .setOnKeyListener(
                    new Dialog.OnKeyListener() {
                        @Override public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                closeDialog();
                                return true;
                            }
                            return false;
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
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    ManagePoiProfileDialog npDialog = ManagePoiProfileDialog.newProfile();
                    npDialog.setTargetFragment(SelectPoiProfileDialog.this, 1);
                    npDialog.show(getActivity().getSupportFragmentManager(), "ManagePoiProfileDialog");
                }
            });

            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    closeDialog();
                }
            });

            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    PoiProfile newProfile = (PoiProfile) parent.getItemAtPosition(position);
                    if (newProfile != null && listener != null) {
                        listener.poiProfileSelected(newProfile);
                    }
                    dismiss();
                }
            });
            listViewItems.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                    PoiProfile newProfile = (PoiProfile) parent.getItemAtPosition(position);
                    if (newProfile != null) {
                        showProfileContextMenu(selectedProfile, view);
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

    private void fillProfilesListView() {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setAdapter(
                    new PoiProfileAdapter(
                        SelectPoiProfileDialog.this.getContext(),
                        PoiProfile.allProfiles(),
                        selectedProfile));
        }
    }

    private void closeDialog() {
        if (listener != null && selectedProfileModified) {
            listener.poiProfileSelected(selectedProfile);
        }
    }


    /**
     * list menus
     */
    private static final int MENU_ITEM_EDIT_PROFILE = 1;
    private static final int MENU_ITEM_REMOVE_PROFILE = 2;

    private void showProfileContextMenu(final PoiProfile profile, View view) {
        PopupMenu profileContextMenu = new PopupMenu(getActivity(), view);
        profileContextMenu.getMenu().add(
                Menu.NONE, MENU_ITEM_EDIT_PROFILE, 1, GlobalInstance.getStringResource(R.string.menuItemEdit));
        profileContextMenu.getMenu().add(
                Menu.NONE, MENU_ITEM_REMOVE_PROFILE, 2, GlobalInstance.getStringResource(R.string.menuItemRemove));

        profileContextMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case MENU_ITEM_EDIT_PROFILE:
                        ManagePoiProfileDialog mpDialog = ManagePoiProfileDialog.modifyProfile(profile);
                        mpDialog.setTargetFragment(SelectPoiProfileDialog.this, 1);
                        mpDialog.show(getActivity().getSupportFragmentManager(), "ManagePoiProfileDialog");
                        return true;
                    case MENU_ITEM_REMOVE_PROFILE:
                        ManagePoiProfileDialog rpDialog = ManagePoiProfileDialog.removeProfile(profile);
                        rpDialog.setTargetFragment(SelectPoiProfileDialog.this, 1);
                        rpDialog.show(getActivity().getSupportFragmentManager(), "ManagePoiProfileDialog");
                        return true;
                    default:
                        return false;
                }
            }
        });

        profileContextMenu.show();
    }


    /**
     * ManagePoiProfileListener
     */

    @Override public void poiProfileCreated(PoiProfile profile) {
        if (selectedProfile == null) {
            selectedProfile = profile;
            selectedProfileModified = true;
        }
        fillProfilesListView();
    }

    @Override public void poiProfileModified(PoiProfile profile) {
        if (profile.equals(selectedProfile)) {
            selectedProfileModified = true;
        }
        fillProfilesListView();
    }

    @Override public void poiProfileRemoved(PoiProfile profile) {
        if (profile.equals(selectedProfile)) {
            selectedProfile = null;
            selectedProfileModified = true;
        }
        fillProfilesListView();
    }


    /**
     * PoiProfile adapter
     */

    private class PoiProfileAdapter extends ArrayAdapter<PoiProfile> {

        private Context context;
        private ArrayList<PoiProfile> profileList;
        private PoiProfile selectedProfile;

        public PoiProfileAdapter(Context context, ArrayList<PoiProfile> profileList, PoiProfile selectedProfile) {
            super(context, R.layout.layout_checked_text_view_and_action_button);
            this.context = context;
            this.profileList = profileList;
            this.selectedProfile = selectedProfile;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            PoiProfile profile = getItem(position);

            // load item layout
            EntryHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(this.context);
                convertView = inflater.inflate(R.layout.layout_checked_text_view_and_action_button, parent, false);
                holder = new EntryHolder();
                holder.labelPoiProfile = (CheckedTextView) convertView.findViewById(R.id.labelPoiProfile);
                holder.buttonActionFor = (ImageButton) convertView.findViewById(R.id.buttonActionFor);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }

            // label
            if (holder.labelPoiProfile != null) {
                holder.labelPoiProfile.setText(profile.toString());
                holder.labelPoiProfile.setChecked(
                        profile.equals(this.selectedProfile));
            }

            // action button
            boolean showActionForButton = true;
            if (holder.buttonActionFor != null) {
                if (showActionForButton) {
                    holder.buttonActionFor.setContentDescription(
                            String.format(
                                "%1$s %2$s",
                                context.getResources().getString(R.string.buttonActionFor),
                                profile.getName())
                            );
                    holder.buttonActionFor.setTag(profile);
                    holder.buttonActionFor.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            PoiProfile newProfile = (PoiProfile) view.getTag();
                            if (newProfile != null) {
                                showProfileContextMenu(newProfile, view);
                            }
                        }
                    });
                    holder.buttonActionFor.setVisibility(View.VISIBLE);
                } else {
                    holder.buttonActionFor.setVisibility(View.GONE);
                }
            }

            return convertView;
        }

        @Override public int getCount() {
            if (this.profileList != null) {
                return this.profileList.size();
            }
            return 0;
        }

        @Override public PoiProfile getItem(int position) {
            if (this.profileList != null) {
                return this.profileList.get(position);
            }
            return null;
        }


        private class EntryHolder {
            public CheckedTextView labelPoiProfile;
            private ImageButton buttonActionFor;
        }
    }

}
