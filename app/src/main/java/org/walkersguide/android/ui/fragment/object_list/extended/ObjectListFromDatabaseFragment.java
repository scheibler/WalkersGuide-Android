package org.walkersguide.android.ui.fragment.object_list.extended;

import android.widget.Toast;
import org.walkersguide.android.data.profile.ProfileGroup;
import org.walkersguide.android.database.profile.FavoritesProfile;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.SortMethod;

import org.walkersguide.android.data.ObjectWithId;

import org.walkersguide.android.ui.dialog.select.SelectProfileDialog;
import org.walkersguide.android.ui.dialog.select.SelectSortMethodDialog;
import org.walkersguide.android.R;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


import android.os.Bundle;



import android.view.View;



import java.util.ArrayList;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.ui.fragment.object_list.ExtendedObjectListFragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executors;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.profile.Profile;
import org.walkersguide.android.database.DatabaseProfile;
import timber.log.Timber;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;


public class ObjectListFromDatabaseFragment extends ExtendedObjectListFragment implements FragmentResultListener {


    public static class BundleBuilder extends ExtendedObjectListFragment.BundleBuilder {
        public BundleBuilder(DatabaseProfileRequest request) {
            super();
            bundle.putSerializable(KEY_REQUEST, request);
        }
    }

	public static ObjectListFromDatabaseFragment createDialog(DatabaseProfile profile, boolean enableSelection) {
		ObjectListFromDatabaseFragment fragment = new ObjectListFromDatabaseFragment();
        fragment.setArguments(
                new BundleBuilder(
                    new DatabaseProfileRequest(profile))
                .setIsDialog(enableSelection)
                .build());
		return fragment;
    }

	public static ObjectListFromDatabaseFragment createFragment(DatabaseProfile profile, SortMethod sortMethod) {
		ObjectListFromDatabaseFragment fragment = new ObjectListFromDatabaseFragment();
        fragment.setArguments(
                new BundleBuilder(
                    new DatabaseProfileRequest(profile, null, sortMethod))
                .build());
		return fragment;
    }

	public static ObjectListFromDatabaseFragment createPointHistoryFragment() {
		ObjectListFromDatabaseFragment fragment = new ObjectListFromDatabaseFragment();
        fragment.setArguments(
                new BundleBuilder(
                    new DatabaseProfileRequest(DatabaseProfile.allPoints()))
                .setProfileGroup(ProfileGroup.POINT_HISTORY)
                .build());
		return fragment;
    }

	public static ObjectListFromDatabaseFragment createRouteHistoryFragment() {
		ObjectListFromDatabaseFragment fragment = new ObjectListFromDatabaseFragment();
        fragment.setArguments(
                new BundleBuilder(
                    new DatabaseProfileRequest(DatabaseProfile.allRoutes()))
                .setProfileGroup(ProfileGroup.ROUTE_HISTORY)
                .build());
		return fragment;
    }


    // dialog
    private static final String KEY_REQUEST = "request";

    private DatabaseProfileRequest request;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectSortMethodDialog.REQUEST_SELECT_SORT_METHOD, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectSortMethodDialog.REQUEST_SELECT_SORT_METHOD)) {
            request.setSortMethod(
                    (SortMethod) bundle.getSerializable(SelectSortMethodDialog.EXTRA_SORT_METHOD));
            resetListPosition();
            requestUiUpdate();

        } else {
            super.onFragmentResult(requestKey, bundle);
        }
    }

    @Override public Profile getProfile() {
        return request.getProfile();
    }

    @Override public void selectNewProfile(Profile newProfile) {
        if (newProfile == null) {
            request.setProfile(null);
        } else if (newProfile instanceof DatabaseProfile) {
            request.setProfile((DatabaseProfile) newProfile);
        }
        resetListPosition();
        requestUiUpdate();
    }


    /**
     * create view
     */

	@Override public View configureView(View view, Bundle savedInstanceState) {
        view = super.configureView(view, savedInstanceState);

        if (savedInstanceState != null) {
            request = (DatabaseProfileRequest) savedInstanceState.getSerializable(KEY_REQUEST);
        } else {
            request = (DatabaseProfileRequest) getArguments().getSerializable(KEY_REQUEST);
        }

        super.updateSearchTerm(request.getSearchTerm());
        return view;
    }

    @Override public void onSearchTermChanged(String newSearchTerm) {
        request.setSearchTerm(newSearchTerm);
    }

    @Override public String getDialogTitle() {
        if (request.hasProfile()) {
            return request.getProfile().getName();
        }
        return null;
    }

    @Override public int getPluralResourceId() {
        if (request.hasSearchTerm()) {
            return R.plurals.result;
        } else if (request.hasProfile()) {
            if (request.getProfile() instanceof FavoritesProfile) {
                return R.plurals.favorite;
            } else if (request.getProfile().isForPoints()) {
                return R.plurals.point;
            } else if (request.getProfile().isForRoutes()) {
                return R.plurals.route;
            } else if (request.getProfile().isForSegments()) {
                return R.plurals.way;
            }
        }
        return R.plurals.object;
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        super.onCreateMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.menu_toolbar_object_list_from_database_fragment, menu);
    }

    @Override public void onPrepareMenu(@NonNull Menu menu) {
        super.onPrepareMenu(menu);
        boolean isPointDatabaseProfile = request != null
            && request.hasProfile()
            && request.getProfile().isForPoints();
        // show auto update
        MenuItem menuItemAutoUpdate = menu.findItem(R.id.menuItemAutoUpdate);
        menuItemAutoUpdate.setVisible(isPointDatabaseProfile);
        // viewing direction filter
        MenuItem menuItemFilterResult = menu.findItem(R.id.menuItemFilterResult);
        menuItemFilterResult.setVisible(isPointDatabaseProfile);
        // show announce object ahead
        MenuItem menuItemAnnounceObjectAhead = menu.findItem(R.id.menuItemAnnounceObjectAhead);
        menuItemAnnounceObjectAhead.setVisible(isPointDatabaseProfile);
    }

    @Override public boolean onMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemSortMethod) {
            if (request == null || request.getProfile() == null) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.messageNoProfileSelected),
                        Toast.LENGTH_LONG)
                    .show();
                return true;
            }
            SelectSortMethodDialog.newInstance(
                    request.getProfile().getSupportedSortMethodList(),
                    request.getSortMethod())
                .show(getChildFragmentManager(), "SelectSortMethodDialog");

        } else if (item.getItemId() == R.id.menuItemClearProfile) {
            if (request == null || request.getProfile() == null) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.messageNoProfileSelected),
                        Toast.LENGTH_LONG)
                    .show();
                return true;
            }
            Dialog clearProfileDialog = new AlertDialog.Builder(getActivity())
                .setMessage(
                        String.format(
                            getResources().getString(R.string.clearProfileDialogTitle),
                            request.getProfile().getName())
                        )
                .setPositiveButton(
                        getResources().getString(R.string.dialogYes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                AccessDatabase.getInstance().clearDatabaseProfile(request.getProfile());
                                resetListPosition();
                                requestUiUpdate();
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogNo),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .create();
            clearProfileDialog.show();

        } else {
            return super.onMenuItemSelected(item);
        }
        return true;
    }


    /**
     * pause and resume
     */

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setText(
                    getResources().getString(R.string.menuItemSortMethod));
            buttonNeutral.setVisibility(
                    request != null && request.hasProfile() ? View.VISIBLE : View.GONE);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    SelectSortMethodDialog.newInstance(
                            request.getProfile().getSupportedSortMethodList(),
                            request.getSortMethod())
                        .show(getChildFragmentManager(), "SelectSortMethodDialog");
                }
            });
        }
    }

    @Override public void onResume() {
        super.onResume();
        Timber.d("onResume");
    }

    @Override public void onPause() {
        super.onPause();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_REQUEST, request);
    }

    @Override public boolean isUiUpdateRequestInProgress() {
        return false;
    }

    @Override public void prepareRequest() {
        super.prepareRequest();
    }

    @Override public void requestUiUpdate() {
        this.prepareRequest();

        Executors.newSingleThreadExecutor().execute(() -> {
            final ArrayList<ObjectWithId> objectList = AccessDatabase
                .getInstance()
                .getObjectListFor(request);
            (new Handler(Looper.getMainLooper())).post(() -> {
                if (isAdded()) {
                    super.populateUiAfterRequestWasSuccessful(
                            String.format(
                                GlobalInstance.getStringResource(R.string.labelHeadingSecondLineSortMethod),
                                request.getSortMethod().toString()),
                            objectList,
                            ! (request.getProfile() instanceof FavoritesProfile),
                               getProfileGroup() == ProfileGroup.POINT_HISTORY
                            || getProfileGroup() == ProfileGroup.ROUTE_HISTORY);
                }
            });
        });
    }

    @Override public void requestMoreResults() {
    }

}
