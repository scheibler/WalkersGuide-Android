package org.walkersguide.android.ui.fragment.object_list;

import org.walkersguide.android.database.profiles.DatabaseRouteProfile;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.database.DatabaseProfileManager;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.database.DatabaseProfileManager.DatabaseProfileRequestListener;
import org.walkersguide.android.ui.dialog.selectors.SelectDatabaseProfileDialog;
import org.walkersguide.android.ui.dialog.selectors.SelectDatabaseProfileDialog.SelectDatabaseProfileListener;

import org.walkersguide.android.data.ObjectWithId;

import org.walkersguide.android.ui.dialog.selectors.SelectDatabaseProfileDialog;

import org.walkersguide.android.ui.dialog.selectors.SelectSortMethodDialog;
import org.walkersguide.android.ui.dialog.selectors.SelectSortMethodDialog.SelectSortMethodListener;
import org.walkersguide.android.R;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import timber.log.Timber;


import android.os.Bundle;

import androidx.core.view.ViewCompat;


import android.view.View;



import java.util.ArrayList;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.ui.fragment.ObjectListFragment;
import org.walkersguide.android.database.profiles.DatabaseSegmentProfile;
import org.walkersguide.android.database.profiles.DatabasePointProfile;


public class ObjectListFromDatabaseFragment extends ObjectListFragment
        implements DatabaseProfileRequestListener, SelectDatabaseProfileListener, SelectSortMethodListener {
    private static final String KEY_REQUEST = "request";

    private DatabaseProfileRequest request;

	public static ObjectListFromDatabaseFragment createDialog(
            DatabaseProfileRequest request, boolean showSelectProfileButton) {
        return newInstance(request, showSelectProfileButton, true);
    }

	public static ObjectListFromDatabaseFragment createFragment(
            DatabaseProfileRequest request, boolean showSelectProfileButton) {
        return newInstance(request, showSelectProfileButton, false);
    }

    private static ObjectListFromDatabaseFragment newInstance(
            DatabaseProfileRequest request, boolean showSelectProfileButton, boolean isDialog) {
		ObjectListFromDatabaseFragment fragment = new ObjectListFromDatabaseFragment();
        Bundle args = ObjectListFragment.createArgsBundle(isDialog, showSelectProfileButton, true);
        args.putSerializable(KEY_REQUEST, request);
        fragment.setArguments(args);
		return fragment;
	}


    /**
     * menu
     */

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_database_point_list_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemSortMethod:
                SelectSortMethodDialog dialog = SelectSortMethodDialog.newInstance(request.getSortMethod());
                dialog.setTargetFragment(ObjectListFromDatabaseFragment.this, 1);
                dialog.show(getActivity().getSupportFragmentManager(), "SelectSortMethodDialog");
                return true;
        }
        return super.onOptionsItemSelected(item);
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
        editSearch.setText(request.getSearchTerm());
        return view;
    }

    @Override public void clickedButtonSelectProfile() {
        SelectDatabaseProfileDialog dialog = null;
        if (request.getProfile() instanceof DatabasePointProfile) {
            dialog = SelectDatabaseProfileDialog.pointProfiles(
                    (DatabasePointProfile) request.getProfile());
        } else if (request.getProfile() instanceof DatabaseRouteProfile) {
            dialog = SelectDatabaseProfileDialog.routeProfiles(
                    (DatabaseRouteProfile) request.getProfile());
        } else if (request.getProfile() instanceof DatabaseSegmentProfile) {
            dialog = SelectDatabaseProfileDialog.segmentProfiles(
                    (DatabaseSegmentProfile) request.getProfile());
        }
        if (dialog != null) {
            dialog.setTargetFragment(ObjectListFromDatabaseFragment.this, 1);
            dialog.show(getActivity().getSupportFragmentManager(), "SelectDatabaseProfileDialog");
        }
    }

    @Override public void longClickedButtonSelectProfile() {
    }

    @Override public void clickedLabelEmptyListView() {
    }

    @Override public void searchTermChanged(String newSearchTerm) {
        request.setSearchTerm(newSearchTerm);
    }

    @Override public String getDialogTitle() {
        if (request.getProfile() != null
                && buttonSelectProfile.getVisibility() == View.GONE) {
            return request.getProfile().getName();
        } else if (request.getProfile() instanceof DatabasePointProfile) {
            return getResources().getString(R.string.menuItemLastPoints);
        } else if (request.getProfile() instanceof DatabaseRouteProfile) {
            return getResources().getString(R.string.menuItemLastRoutes);
        } else if (request.getProfile() instanceof DatabaseSegmentProfile) {
            return getResources().getString(R.string.menuItemLastSegments);
        } else {
            return "";
        }
    }


    /**
     * listeners
     */

    @Override public void databaseProfileSelected(DatabaseProfile newProfile) {
        if (newProfile != null) {
            request.setProfile(newProfile);
            resetListPosition();
            requestUiUpdate();
        }
    }

    @Override public void sortMethodSelected(SortMethod newSortMethod) {
        if (newSortMethod != null) {
            request.setSortMethod(newSortMethod);
            resetListPosition();
            requestUiUpdate();
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_REQUEST, request);
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        Timber.d("onPause");
        DatabaseProfileManager.getInstance().invalidateDatabaseProfileRequest((ObjectListFromDatabaseFragment) this);
    }

    @Override public void onResume() {
        super.onResume();
        Timber.d("onResume");
        requestUiUpdate();
    }


    /**
     * point list request and response
     */

    public void requestUiUpdate() {
        prepareRequest();
        DatabaseProfileManager.getInstance().startDatabaseProfileRequest((ObjectListFromDatabaseFragment) this, request);
    }

    @Override public void prepareRequest() {
        super.prepareRequest();
        buttonSelectProfile.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.buttonSelectProfile),
                    request.getProfile().getName())
                );
    }

    @Override public void databaseProfileRequestSuccessful(ArrayList<ObjectWithId> objectList) {
        super.updateRefreshButtonAfterRequestWasFinished();
        super.updateListViewAfterRequestWasSuccessful(objectList);

        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        String heading = null;
        if (request.hasSearchTerm()) {
            heading = GlobalInstance.getPluralResource(R.plurals.result, objectList.size());
        } else if (request.getProfile() instanceof DatabasePointProfile) {
            heading = GlobalInstance.getPluralResource(R.plurals.point, objectList.size());
        } else if (request.getProfile() instanceof DatabaseRouteProfile) {
            heading = GlobalInstance.getPluralResource(R.plurals.route, objectList.size());
        } else if (request.getProfile() instanceof DatabaseSegmentProfile) {
            heading = GlobalInstance.getPluralResource(R.plurals.way, objectList.size());
        } else {
            heading = GlobalInstance.getPluralResource(R.plurals.object, objectList.size());
        }
        heading += String.format("\n%1$s", request.getSortMethod().toString());
        labelHeading.setText(heading);
    }

    @Override public void databaseProfileRequestFailed(int returnCode) {
        super.updateRefreshButtonAfterRequestWasFinished();
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        labelEmptyListView.setText(
                ServerUtility.getErrorMessageForReturnCode(returnCode));
    }

}
