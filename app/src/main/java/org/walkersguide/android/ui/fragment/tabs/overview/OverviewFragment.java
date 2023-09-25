package org.walkersguide.android.ui.fragment.tabs.overview;

import org.walkersguide.android.ui.interfaces.ViewChangedListener;
import org.walkersguide.android.data.profile.MutableProfile;
import org.walkersguide.android.ui.fragment.profile_list.PoiProfileListFragment;
import org.walkersguide.android.ui.adapter.PinnedObjectsAdapter;
import org.walkersguide.android.ui.adapter.PinnedObjectsAdapter.OnAddButtonClick;
import org.walkersguide.android.database.profile.StaticProfile;
import androidx.core.view.MenuProvider;
import org.walkersguide.android.ui.fragment.profile_list.CollectionListFragment;
import org.walkersguide.android.ui.fragment.HistoryFragment;
import org.walkersguide.android.R;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
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
import org.walkersguide.android.data.object_with_id.Point;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.MainActivityController;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentResultListener;
import android.content.Context;
import android.widget.Button;
import androidx.lifecycle.Lifecycle;
import timber.log.Timber;
import org.walkersguide.android.ui.view.ResolveCurrentAddressView;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.widget.ExpandableListView;
import android.content.BroadcastReceiver;
import org.walkersguide.android.data.Profile;
import android.widget.BaseExpandableListAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import org.walkersguide.android.ui.fragment.object_list.ExtendedObjectListFragment;
import android.content.Intent;
import android.widget.PopupMenu;
import org.walkersguide.android.util.GlobalInstance;


public class OverviewFragment extends Fragment
        implements FragmentResultListener, MenuProvider, OnAddButtonClick, ViewChangedListener {
    private final static String KEY_LIST_POSITION = "listPosition";

	public static OverviewFragment newInstance() {
		OverviewFragment fragment = new OverviewFragment();
		return fragment;
	}


    private MainActivityController mainActivityController;
    private ResolveCurrentAddressView layoutClosestAddress;

    // pinned object list
    private int listPosition;
    private ExpandableListView listViewPinnedObjects;
    private TextView labelNoPinnedObjectsHint;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            listPosition = 0;
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
                    && StaticProfile.pinnedPointsAndRoutes().add(selectedObjectWithId)) {
                requestUiUpdate();
            }
        }
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            if (activity instanceof MainActivity) {
                mainActivityController = (MainActivityController) ((MainActivity) activity);
            }
        }
    }

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_overview, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        layoutClosestAddress = (ResolveCurrentAddressView) view.findViewById(R.id.layoutClosestAddress);

        listViewPinnedObjects = (ExpandableListView) view.findViewById(R.id.expandableListView);
        labelNoPinnedObjectsHint = (TextView) view.findViewById(R.id.labelNoPinnedObjectsHint);

        Button buttonCollections = (Button) view.findViewById(R.id.buttonCollections);
        buttonCollections.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.addFragment(
                        CollectionListFragment.createFragment());
            }
        });

        Button buttonHistory = (Button) view.findViewById(R.id.buttonHistory);
        buttonHistory.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.addFragment(
                        HistoryFragment.newInstance());
            }
        });
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

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_LIST_POSITION, listPosition);
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_overview_fragment, menu);
    }

    @Override public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemRefresh) {
            listPosition = 0;
            requestUiUpdate();

        } else if (item.getItemId() == R.id.menuItemClearPinnedObjectsOnlyProfiles
                || item.getItemId() == R.id.menuItemClearPinnedObjectsBoth) {
            AccessDatabase.getInstance().clearPinnedProfileList();
            requestUiUpdate();

        } else if (item.getItemId() == R.id.menuItemClearPinnedObjectsOnlyPointsAndRoutes
                || item.getItemId() == R.id.menuItemClearPinnedObjectsBoth) {
            AccessDatabase.getInstance().clearDatabaseProfile(StaticProfile.pinnedPointsAndRoutes());
            requestUiUpdate();

        } else {
            return false;
        }
        return true;
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
            if (intent.getAction().equals(ViewChangedListener.ACTION_OBJECT_WITH_ID_LIST_CHANGED)
                    || intent.getAction().equals(ViewChangedListener.ACTION_PROFILE_LIST_CHANGED)) {
                requestUiUpdate();
            }
        }
    };


    private void requestUiUpdate() {
        layoutClosestAddress.requestAddressForCurrentLocation();

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
                        new DatabaseProfileRequest(StaticProfile.pinnedPointsAndRoutes()));
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
        listViewPinnedObjects.setAdapter(
                new PinnedObjectsAdapter(
                    OverviewFragment.this.getContext(), this, profileList, objectList));

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
