package org.walkersguide.android.ui.fragment;

import android.view.accessibility.AccessibilityEvent;
import org.walkersguide.android.ui.adapter.ProfileAdapter;
import org.walkersguide.android.ui.interfaces.ViewChangedListener;
import org.walkersguide.android.ui.view.ProfileView;
import org.walkersguide.android.ui.view.ProfileView.OnProfileDefaultActionListener;


import android.os.Bundle;


import android.view.View;

import android.widget.ListView;
import android.widget.TextView;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import java.util.ArrayList;
import android.content.BroadcastReceiver;
import android.content.Context;
import androidx.core.view.ViewCompat;
import android.widget.AbsListView;
import android.content.Intent;
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


    public static class BundleBuilder extends RootFragment.BundleBuilder {
        public BundleBuilder() {
            super();
            setSelectProfile(false);
        }

        public BundleBuilder setSelectProfile(boolean newState) {
            bundle.putBoolean(KEY_SELECT_PROFILE, newState);
            return this;
        }

        public Bundle build() {
            return bundle;
        }
    }


    // dialog
    private static final String KEY_SELECT_PROFILE = "selectProfile";
    private static final String KEY_LIST_POSITION = "listPosition";

    private boolean selectProfile;
    private int listPosition;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        selectProfile = getArguments().getBoolean(KEY_SELECT_PROFILE);

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

    private TextView labelHeading, labelEmptyListView;
	private ListView listViewProfile;

    @Override public String getDialogButtonText() {
        return getResources().getString(
                selectProfile ? R.string.dialogCancel : R.string.dialogClose);
    }

    @Override public int getLayoutResourceId() {
        return R.layout.layout_heading_and_list_view;
    }

	@Override public View configureView(View view, Bundle savedInstanceState) {
        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        ImageButton buttonAddProfile = (ImageButton) view.findViewById(R.id.buttonAdd);
        buttonAddProfile.setContentDescription(
                getContentDescriptionForAddProfileButton());
        buttonAddProfile.setVisibility(
                selectProfile ? View.GONE : View.VISIBLE);
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

    public boolean getSelectProfile() {
        return this.selectProfile;
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
        listPosition = -1;
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
        labelHeading.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);

        // fill list view
        listViewProfile.setAdapter(
                new ProfileAdapter(
                    ProfileListFragment.this.getContext(),
                    profileList,
                    selectProfile ? this : null,
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

}
