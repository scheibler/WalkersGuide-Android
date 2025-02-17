package org.walkersguide.android.ui.fragment.tabs.overview;

import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.data.profile.MutableProfile;
import org.walkersguide.android.ui.adapter.PinnedObjectsAdapter;
import org.walkersguide.android.ui.adapter.PinnedObjectsAdapter.OnAddButtonClick;
import org.walkersguide.android.database.profile.StaticProfile;
import androidx.core.view.MenuProvider;
import org.walkersguide.android.R;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import android.widget.AbsListView;
import java.util.concurrent.Executors;
import org.walkersguide.android.data.ObjectWithId;
import java.util.ArrayList;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.database.DatabaseProfileRequest;
import android.os.Handler;
import android.os.Looper;
import org.walkersguide.android.ui.dialog.select.SelectProfileFromMultipleSourcesDialog;
import org.walkersguide.android.ui.dialog.select.SelectObjectWithIdFromMultipleSourcesDialog;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.fragment.app.FragmentResultListener;
import android.content.Context;
import androidx.lifecycle.Lifecycle;
import android.widget.ExpandableListView;
import android.content.BroadcastReceiver;
import org.walkersguide.android.data.Profile;
import android.widget.BaseExpandableListAdapter;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.ui.fragment.ViewChangedListenerFragment;
import android.os.Build;


public class PinFragment extends ViewChangedListenerFragment
        implements FragmentResultListener, MenuProvider, OnAddButtonClick {
    private final static String KEY_GROUP_PROFILES_EXPANDED = "groupProfilesExpanded";
    private final static String KEY_GROUP_OBJECTS_WITH_ID_EXPANDED = "groupObjectsWithIdExpanded";

    public static PinFragment newInstance() {
        PinFragment fragment = new PinFragment();
        return fragment;
    }


    // pinned object list
    private boolean groupProfilesExpanded, groupObjectsWithIdExpanded;

    private ExpandableListView listViewPinnedObjects;
    private TextView labelNoPinnedObjectsHint;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            groupProfilesExpanded = savedInstanceState.getBoolean(KEY_GROUP_PROFILES_EXPANDED);
            groupObjectsWithIdExpanded = savedInstanceState.getBoolean(KEY_GROUP_OBJECTS_WITH_ID_EXPANDED);
        } else {
            groupProfilesExpanded = true;
            groupObjectsWithIdExpanded = true;
        }

        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectProfileFromMultipleSourcesDialog.REQUEST_SELECT_PROFILE, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectObjectWithIdFromMultipleSourcesDialog.REQUEST_SELECT_OBJECT_WITH_ID, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectProfileFromMultipleSourcesDialog.REQUEST_SELECT_PROFILE)) {
            SelectProfileFromMultipleSourcesDialog.Target profileTarget = (SelectProfileFromMultipleSourcesDialog.Target)
                bundle.getSerializable(SelectProfileFromMultipleSourcesDialog.EXTRA_TARGET);
            Profile selectedProfile = (Profile) bundle.getSerializable(SelectProfileFromMultipleSourcesDialog.EXTRA_PROFILE);
            if (profileTarget == SelectProfileFromMultipleSourcesDialog.Target.ADD_TO_PINNED_PROFILES
                    && selectedProfile instanceof MutableProfile
                    && ((MutableProfile) selectedProfile).setPinned(true)) {
                requestUiUpdate();
            }

        } else if (requestKey.equals(SelectObjectWithIdFromMultipleSourcesDialog.REQUEST_SELECT_OBJECT_WITH_ID)) {
            SelectObjectWithIdFromMultipleSourcesDialog.Target objectWithIdTarget = (SelectObjectWithIdFromMultipleSourcesDialog.Target)
                bundle.getSerializable(SelectObjectWithIdFromMultipleSourcesDialog.EXTRA_TARGET);
            ObjectWithId selectedObjectWithId = (ObjectWithId) bundle.getSerializable(SelectObjectWithIdFromMultipleSourcesDialog.EXTRA_OBJECT_WITH_ID);
            if (objectWithIdTarget == SelectObjectWithIdFromMultipleSourcesDialog.Target.ADD_TO_PINNED_POINTS_AND_ROUTES
                    && StaticProfile.pinnedObjectsWithId().addObject(selectedObjectWithId)) {
                requestUiUpdate();
            }
        }
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pin, container, false);
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        listViewPinnedObjects = (ExpandableListView) view.findViewById(R.id.expandableListView);
        listViewPinnedObjects .setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override public boolean onGroupClick(ExpandableListView expandableListView, View view, int groupPosition, long l) {
                // negate that boolean, cause the isGroupExpanded function returns the state before click / toggle
                boolean expanded = ! expandableListView.isGroupExpanded(groupPosition);

                String groupName = null;
                switch (groupPosition) {
                    case PinnedObjectsAdapter.ID_PROFILES:
                        groupProfilesExpanded = expanded;
                        groupName = getResources().getString(R.string.groupNameProfiles);
                        break;
                    case PinnedObjectsAdapter.ID_OBJECTS:
                        groupObjectsWithIdExpanded = expanded;
                        groupName = getResources().getString(R.string.groupNamePointsAndRoutes);
                        break;
                }

                if (groupName != null
                        && Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    TTSWrapper.getInstance().screenReader(
                            String.format(
                                "%1$s %2$s",
                                groupName,
                                expanded
                                ? getResources().getString(R.string.stateExpanded)
                                : getResources().getString(R.string.stateCollapsed))
                            );
                    Helper.vibrateOnce(
                            Helper.VIBRATION_DURATION_SHORT, Helper.VIBRATION_INTENSITY_WEAK);
                }
                return false;
            }
        });

        labelNoPinnedObjectsHint = (TextView) view.findViewById(R.id.labelNoPinnedObjectsHint);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(KEY_GROUP_PROFILES_EXPANDED, groupProfilesExpanded);
        savedInstanceState.putBoolean(KEY_GROUP_OBJECTS_WITH_ID_EXPANDED, groupObjectsWithIdExpanded);
    }

    @Override public void onAddPinnedProfileButtonClicked(View view) {
        SelectProfileFromMultipleSourcesDialog.newInstance(
                SelectProfileFromMultipleSourcesDialog.Target.ADD_TO_PINNED_PROFILES)
            .show(getChildFragmentManager(), "SelectProfileFromMultipleSourcesDialog");
    }

    @Override public void onAddPinnedObjectButtonClicked(View view) {
        SelectObjectWithIdFromMultipleSourcesDialog.newInstance(
                SelectObjectWithIdFromMultipleSourcesDialog.Target.ADD_TO_PINNED_POINTS_AND_ROUTES)
            .show(getChildFragmentManager(), "SelectObjectWithIdFromMultipleSourcesDialog");
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_pin_fragment, menu);
    }

    @Override public boolean onMenuItemSelected(@NonNull MenuItem item) {
        boolean consumed = false;

        if (item.getItemId() == R.id.menuItemClearPinnedObjectsOnlyProfiles
                || item.getItemId() == R.id.menuItemClearPinnedObjectsBoth) {
            AccessDatabase.getInstance().clearPinnedProfileList();
            consumed = true;
        }
        if (item.getItemId() == R.id.menuItemClearPinnedObjectsOnlyPointsAndRoutes
                || item.getItemId() == R.id.menuItemClearPinnedObjectsBoth) {
            AccessDatabase.getInstance().clearDatabaseProfile(StaticProfile.pinnedObjectsWithId());
            consumed = true;
        }

        if (consumed) {
            requestUiUpdate();
            return true;
        } else {
            return false;
        }
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceSensorManager.ACTION_SHAKE_DETECTED);
        LocalBroadcastManager
            .getInstance(getActivity())
            .registerReceiver(mMessageReceiver, filter);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DeviceSensorManager.ACTION_SHAKE_DETECTED)) {
                Helper.vibrateOnce(Helper.VIBRATION_DURATION_LONG);
                requestUiUpdate();
            }
        }
    };


    public void requestUiUpdate() {
        listViewPinnedObjects.setAdapter((BaseExpandableListAdapter) null);
        listViewPinnedObjects.setOnScrollListener(null);
        labelNoPinnedObjectsHint.setVisibility(View.GONE);

        Executors.newSingleThreadExecutor().execute(() -> {
            final ArrayList<Profile> profileList = AccessDatabase
                .getInstance()
                .getPinnedProfileList();
            final ArrayList<ObjectWithId> objectList = AccessDatabase
                .getInstance()
                .getObjectListFor(
                        new DatabaseProfileRequest(StaticProfile.pinnedObjectsWithId()));
            (new Handler(Looper.getMainLooper())).post(() -> {
                if (isAdded()) {
                    loadPinnedObjectsSuccessful(profileList, objectList);
                    if (profileList.isEmpty() && objectList.isEmpty()) {
                        labelNoPinnedObjectsHint.setVisibility(View.VISIBLE);
                    }
                }
            });
        });
    }

    private void loadPinnedObjectsSuccessful(ArrayList<Profile> profileList, ArrayList<ObjectWithId> objectList) {
        PinnedObjectsAdapter adapter = new PinnedObjectsAdapter(
                PinFragment.this.getContext(), this, profileList, objectList);
        listViewPinnedObjects.setAdapter(adapter);

        // expand groups
        if (groupProfilesExpanded) {
            listViewPinnedObjects.expandGroup(PinnedObjectsAdapter.ID_PROFILES);
        } else {
            listViewPinnedObjects.collapseGroup(PinnedObjectsAdapter.ID_PROFILES);
        }
        if (groupObjectsWithIdExpanded) {
            listViewPinnedObjects.expandGroup(PinnedObjectsAdapter.ID_OBJECTS);
        } else {
            listViewPinnedObjects.collapseGroup(PinnedObjectsAdapter.ID_OBJECTS);
        }

        // list position
        listViewPinnedObjects.setSelection(listPosition);
        listViewPinnedObjects.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });
    }

}
