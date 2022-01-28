package org.walkersguide.android.ui.fragment.object_list.extended;

import org.walkersguide.android.ui.fragment.ObjectListFragment.DialogMode;
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
import android.app.AlertDialog;


public class ObjectListFromDatabaseFragment extends ExtendedObjectListFragment implements FragmentResultListener {


	public static ObjectListFromDatabaseFragment createDialog(DatabaseProfile profile, boolean enableSelection) {
        return newInstance(
                enableSelection ? DialogMode.SELECT : DialogMode.DEFAULT,
                new DatabaseProfileRequest(profile),
                null);
    }

	public static ObjectListFromDatabaseFragment createFavoritesFragment() {
        return newInstance(
                null,
                new DatabaseProfileRequest(
                    FavoritesProfile.favoritePoints(), null, SortMethod.DISTANCE_ASC),
                ProfileGroup.FAVORITES);
    }

	public static ObjectListFromDatabaseFragment createPointHistoryFragment() {
        return newInstance(
                null,
                new DatabaseProfileRequest(DatabaseProfile.allPoints()),
                ProfileGroup.POINT_HISTORY);
    }

	public static ObjectListFromDatabaseFragment createRouteHistoryFragment() {
        return newInstance(
                null,
                new DatabaseProfileRequest(DatabaseProfile.allRoutes()),
                ProfileGroup.ROUTE_HISTORY);
    }

	private static ObjectListFromDatabaseFragment newInstance(
            DialogMode dialogMode, DatabaseProfileRequest request, ProfileGroup profileGroup) {
		ObjectListFromDatabaseFragment fragment = new ObjectListFromDatabaseFragment();
        Bundle args = ExtendedObjectListFragment.createArgsBundle(dialogMode, profileGroup);
        args.putSerializable(KEY_REQUEST, request);
        fragment.setArguments(args);
		return fragment;
    }


    // dialog
    private static final String KEY_REQUEST = "request";

    private DatabaseProfileRequest request;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectProfileDialog.REQUEST_SELECT_PROFILE, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectSortMethodDialog.REQUEST_SELECT_SORT_METHOD, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectProfileDialog.REQUEST_SELECT_PROFILE)) {
            Profile selectedProfile = (Profile) bundle.getSerializable(SelectProfileDialog.EXTRA_PROFILE);
            if (selectedProfile instanceof DatabaseProfile) {
                request.setProfile((DatabaseProfile) selectedProfile);
            }
        } else if (requestKey.equals(SelectSortMethodDialog.REQUEST_SELECT_SORT_METHOD)) {
            request.setSortMethod(
                    (SortMethod) bundle.getSerializable(SelectSortMethodDialog.EXTRA_SORT_METHOD));
        }
        resetListPosition();
        requestUiUpdate();
    }

    @Override public Profile getProfile() {
        return request.getProfile();
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
        } else {
            return "";
        }
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

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_database_point_list_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemSortMethod) {
            SelectSortMethodDialog.newInstance(request.getSortMethod())
                .show(getChildFragmentManager(), "SelectSortMethodDialog");
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }


    /**
     * pause and resume
     */

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setText(
                    getResources().getString(R.string.menuItemSortMethod));
            buttonNeutral.setVisibility(View.VISIBLE);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    SelectSortMethodDialog.newInstance(request.getSortMethod())
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
                                "%1$s\n%2$s",
                                GlobalInstance.getPluralResource(getPluralResourceId(), objectList.size()),
                                request.getSortMethod().toString()),
                            objectList,
                            ! (request.getProfile() instanceof FavoritesProfile));
                }
            });
        });
    }

    @Override public void requestMoreResults() {
    }

}
