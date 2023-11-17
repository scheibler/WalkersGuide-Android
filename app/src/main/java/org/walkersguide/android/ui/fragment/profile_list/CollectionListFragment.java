package org.walkersguide.android.ui.fragment.profile_list;

import org.walkersguide.android.ui.dialog.select.SelectProfileFromMultipleSourcesDialog;



import org.walkersguide.android.R;



import android.os.Bundle;






import java.util.ArrayList;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executors;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.ui.fragment.ProfileListFragment;
import android.view.View;


public class CollectionListFragment extends ProfileListFragment implements FragmentResultListener {

	public static CollectionListFragment selectProfile() {
		CollectionListFragment fragment = new CollectionListFragment();
        fragment.setArguments(
                new BundleBuilder()
                .setSelectProfile(true)
                .build());
		return fragment;
    }

	public static CollectionListFragment newInstance() {
		CollectionListFragment fragment = new CollectionListFragment();
        fragment.setArguments(
                new BundleBuilder()
                    .build());
		return fragment;
    }


	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectProfileFromMultipleSourcesDialog.REQUEST_SELECT_PROFILE, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectProfileFromMultipleSourcesDialog.REQUEST_SELECT_PROFILE)) {
            SelectProfileFromMultipleSourcesDialog.Target profileTarget = (SelectProfileFromMultipleSourcesDialog.Target)
                bundle.getSerializable(SelectProfileFromMultipleSourcesDialog.EXTRA_TARGET);
            Profile selectedProfile = (Profile) bundle.getSerializable(SelectProfileFromMultipleSourcesDialog.EXTRA_PROFILE);
            if (profileTarget == SelectProfileFromMultipleSourcesDialog.Target.CREATE_COLLECTION
                    && selectedProfile != null) {
                // the newly created profile was already inserted into the database in the CreateEmptyCollectionDialog or ImportGpxFileDialog
                // so just refresh the ui
                requestUiUpdate();
            }
        }
    }

    @Override public String getTitle() {
        return getSelectProfile()
            ? getResources().getString(R.string.fragmentCollectionListNameSelect)
            : getResources().getString(R.string.fragmentCollectionListName);
    }

    @Override public int getPluralResourceId() {
        return R.plurals.collection;
    }

    @Override public String getContentDescriptionForAddProfileButton() {
        return getResources().getString(R.string.buttonAddCollection);
    }

    @Override public String getEmptyProfileListMessage() {
        return getResources().getString(R.string.labelEmptyCollectionList);
    }

    @Override public void addProfileButtonClicked(View view) {
        SelectProfileFromMultipleSourcesDialog.newInstance(
                SelectProfileFromMultipleSourcesDialog.Target.CREATE_COLLECTION)
            .show(getChildFragmentManager(), "SelectProfileFromMultipleSourcesDialog");
    }

    @Override public void prepareRequest() {
        super.prepareRequest();
    }

    @Override public void requestUiUpdate() {
        this.prepareRequest();

        Executors.newSingleThreadExecutor().execute(() -> {
            final ArrayList<? extends Profile> profileList = AccessDatabase
                .getInstance()
                .getCollectionList();
            (new Handler(Looper.getMainLooper())).post(() -> {
                if (isAdded()) {
                    super.populateUiAfterRequestWasSuccessful(profileList);
                }
            });
        });
    }

}
