package org.walkersguide.android.ui.fragment.profile_list;

import android.widget.Toast;



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
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.ui.dialog.create.CreateOrSelectCollectionDialog;
import org.walkersguide.android.database.profile.Collection;


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
                    CreateOrSelectCollectionDialog.REQUEST_SUCCESSFUL, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(CreateOrSelectCollectionDialog.REQUEST_SUCCESSFUL)) {
            if (bundle.getSerializable(CreateOrSelectCollectionDialog.EXTRA_SELECTED_COLLECTION) instanceof Collection) {
                // the newly created profile was already inserted into the database in the CreateOrSelectCollectionDialog
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
        CreateOrSelectCollectionDialog.newInstance(null, true)
            .show(getChildFragmentManager(), "CreateOrSelectCollectionDialog");
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
