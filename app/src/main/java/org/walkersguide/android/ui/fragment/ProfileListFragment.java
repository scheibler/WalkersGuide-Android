package org.walkersguide.android.ui.fragment;

import org.walkersguide.android.ui.interfaces.ViewChangedListener;
import org.walkersguide.android.ui.view.ProfileView;
import org.walkersguide.android.ui.view.ProfileView.OnProfileDefaultActionListener;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import android.os.Bundle;


import android.view.View;
import android.view.ViewGroup;

import android.widget.ListView;
import android.widget.TextView;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import java.util.ArrayList;
import android.content.BroadcastReceiver;
import android.content.Context;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import androidx.core.view.ViewCompat;
import android.widget.AbsListView;
import android.content.Intent;
import android.widget.BaseAdapter;
import org.walkersguide.android.ui.fragment.RootFragment;
import org.walkersguide.android.data.Profile;
import android.widget.ImageButton;


public abstract class ProfileListFragment extends RootFragment
    implements ProfileView.OnProfileDefaultActionListener, ViewChangedListener {
    public static final String REQUEST_SELECT_PROFILE = "selectProfile";
    public static final String EXTRA_PROFILE = "profile";

    public abstract int getPluralResourceId();
    public abstract String getContentDescriptionForAddProfileButton();
    public abstract String getEmptyProfileListMessage();
    public abstract void addProfileButtonClicked(View view);
    public abstract void requestUiUpdate();


    public static class BundleBuilder {
        protected Bundle bundle = new Bundle();

        public BundleBuilder() {
            bundle.putSerializable(KEY_DIALOG_MODE, DialogMode.DISABLED);
        }

        public BundleBuilder setIsDialog(boolean enableSelection) {
            bundle.putSerializable(
                    KEY_DIALOG_MODE,
                    enableSelection ? DialogMode.SELECT : DialogMode.DEFAULT);
            return this;
        }

        public Bundle build() {
            return bundle;
        }
    }


    // dialog
    private static final String KEY_DIALOG_MODE = "dialogMode";
    private static final String KEY_LIST_POSITION = "listPosition";

    private enum DialogMode {
        DISABLED, DEFAULT, SELECT
    }

    protected DialogMode dialogMode;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        dialogMode = (DialogMode) getArguments().getSerializable(KEY_DIALOG_MODE);
        setShowsDialog(dialogMode != DialogMode.DISABLED);

        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            listPosition = 0;
        }
    }

    @Override public void onProfileDefaultActionClicked(Profile profile) {
        Bundle result = new Bundle();
        result.putSerializable(EXTRA_PROFILE, profile);
        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_PROFILE, result);
        dismiss();
    }


    /**
     * create view
     */

    private int listPosition;

    private TextView labelHeading, labelEmptyListView;
	private ListView listViewProfile;

    @Override public String getDialogButtonText() {
        return getResources().getString(
                dialogMode == DialogMode.SELECT
                ? R.string.dialogCancel
                : R.string.dialogClose);
    }

    @Override public int getLayoutResourceId() {
        return R.layout.layout_heading_and_list_view;
    }

	@Override public View configureView(View view, Bundle savedInstanceState) {
        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        ImageButton buttonAddProfile = (ImageButton) view.findViewById(R.id.buttonAdd);
        buttonAddProfile.setContentDescription(
                getContentDescriptionForAddProfileButton());
        buttonAddProfile.setVisibility(View.VISIBLE);
        buttonAddProfile.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                addProfileButtonClicked(view);
            }
        });

        listViewProfile = (ListView) view.findViewById(R.id.listView);
        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        listViewProfile.setEmptyView(labelEmptyListView);

        return view;
    }

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_LIST_POSITION,  listPosition);
    }

    public ProfileAdapter getListAdapter() {
        if (listViewProfile.getAdapter() != null) {
            return (ProfileAdapter) listViewProfile.getAdapter();
        }
        return null;
    }

    public int getListPosition() {
        return this.listPosition;
    }

    public void resetListPosition() {
        listPosition = 0;
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        unregisterViewChangedBroadcastReceiver(viewChangedBroadcastReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        registerViewChangedBroadcastReceiver(viewChangedBroadcastReceiver);
        requestUiUpdate();
    }

    private BroadcastReceiver viewChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ViewChangedListener.ACTION_PROFILE_LIST_CHANGED)) {
                requestUiUpdate();
            }
        }
    };


    /**
     * responses
     */

    public void prepareRequest() {
        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelHeading.setTag(null);
        labelHeading.setText(
                GlobalInstance.getPluralResource(getPluralResourceId(), 0));

        // list view
        listViewProfile.setAdapter(null);
        listViewProfile.setOnScrollListener(null);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setText(
                GlobalInstance.getStringResource(R.string.messagePleaseWait));
    }

    public void populateUiAfterRequestWasSuccessful(ArrayList<? extends Profile> profileList) {
        labelHeading.setText(
                GlobalInstance.getPluralResource(
                    getPluralResourceId(), profileList.size()));

        // fill list view
        listViewProfile.setAdapter(
                new ProfileAdapter(
                    ProfileListFragment.this.getContext(),
                    profileList,
                    dialogMode == DialogMode.SELECT ? this : null,
                    true));
        labelEmptyListView.setText(getEmptyProfileListMessage());

        // list position
        listViewProfile.setSelection(listPosition);
        listViewProfile.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });
    }


    /**
     * Profile adapter
     */

    public static class ProfileAdapter extends BaseAdapter {

        private Context context;
        private ArrayList<? extends Profile> profileList;
        private OnProfileDefaultActionListener onProfileDefaultActionListener;
        private boolean showContextMenuItemRemove;

        public ProfileAdapter(Context context, ArrayList<? extends Profile> profileList,
                OnProfileDefaultActionListener onProfileDefaultActionListener,
                boolean showContextMenuItemRemove) {
            this.context = context;
            this.profileList = profileList;
            this.onProfileDefaultActionListener = onProfileDefaultActionListener;
            this.showContextMenuItemRemove = showContextMenuItemRemove;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            ProfileView layoutProfileView = null;
            if (convertView == null) {
                layoutProfileView = new ProfileView(this.context);
                layoutProfileView.setLayoutParams(
                        new LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            } else {
                layoutProfileView = (ProfileView) convertView;
            }

            layoutProfileView.setOnProfileDefaultActionListener(
                    onProfileDefaultActionListener != null ? onProfileDefaultActionListener : null);
            layoutProfileView.configure(
                    getItem(position), false, showContextMenuItemRemove);
            return layoutProfileView;
        }

        @Override public int getCount() {
            return this.profileList.size();
        }

        @Override public Profile getItem(int position) {
            return this.profileList.get(position);
        }

        @Override public long getItemId(int position) {
            return position;
        }

        public boolean isEmpty() {
            return this.profileList.isEmpty();
        }


        private class EntryHolder {
            public ProfileView layoutProfileView;
        }
    }

}
