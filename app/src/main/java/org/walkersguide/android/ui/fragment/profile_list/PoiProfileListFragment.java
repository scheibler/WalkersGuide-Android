package org.walkersguide.android.ui.fragment.profile_list;

import android.widget.Toast;
import org.walkersguide.android.ui.dialog.create.CreatePoiProfileDialog;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.SortMethod;

import org.walkersguide.android.data.ObjectWithId;

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
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.ui.fragment.ProfileListFragment;
import timber.log.Timber;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import org.walkersguide.android.server.wg.poi.PoiProfile;


public class PoiProfileListFragment extends ProfileListFragment implements FragmentResultListener {

	public static PoiProfileListFragment createDialog(boolean enableSelection) {
		PoiProfileListFragment fragment = new PoiProfileListFragment();
        fragment.setArguments(
                new BundleBuilder()
                .setIsDialog(enableSelection)
                .build());
		return fragment;
    }

	public static PoiProfileListFragment createFragment() {
		PoiProfileListFragment fragment = new PoiProfileListFragment();
        fragment.setArguments(
                new BundleBuilder()
                    .build());
		return fragment;
    }


	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getChildFragmentManager()
            .setFragmentResultListener(
                    CreatePoiProfileDialog.REQUEST_CREATE_POI_PROFILE_WAS_SUCCESSFUL, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(CreatePoiProfileDialog.REQUEST_CREATE_POI_PROFILE_WAS_SUCCESSFUL)) {
            // the newly created profile was already inserted into the database in the CreatePoiProfileDialog
            // so just refresh the ui
            requestUiUpdate();
        }
    }

    @Override public String getTitle() {
        return getResources().getString(R.string.fragmentPoiProfileListName);
    }

    @Override public int getPluralResourceId() {
        return R.plurals.pointProfile;
    }

    @Override public String getContentDescriptionForAddProfileButton() {
        return getResources().getString(R.string.buttonAddPoiProfile);
    }

    @Override public String getEmptyProfileListMessage() {
        return getResources().getString(R.string.labelEmptyPoiProfileList);
    }

    @Override public void addProfileButtonClicked(View view) {
        CreatePoiProfileDialog.newInstance()
                .show(getChildFragmentManager(), "CreatePoiProfileDialog");
    }

    @Override public void prepareRequest() {
        super.prepareRequest();
    }

    @Override public void requestUiUpdate() {
        this.prepareRequest();

        Executors.newSingleThreadExecutor().execute(() -> {
            final ArrayList<? extends Profile> profileList = AccessDatabase
                .getInstance()
                .getPoiProfileList();
            (new Handler(Looper.getMainLooper())).post(() -> {
                if (isAdded()) {
                    super.populateUiAfterRequestWasSuccessful(profileList);
                }
            });
        });
    }

}
