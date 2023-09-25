package org.walkersguide.android.ui.fragment.profile_list;

import org.walkersguide.android.ui.dialog.create.ImportGpxFileDialog;

import org.walkersguide.android.database.profile.Collection;
import org.walkersguide.android.ui.dialog.template.EnterStringDialog;
import org.walkersguide.android.ui.dialog.create.CreatePoiProfileDialog;


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
import org.walkersguide.android.server.wg.poi.PoiProfile;
import android.view.View;
import android.app.Dialog;
import android.widget.PopupMenu;
import android.view.Menu;
import org.walkersguide.android.util.GlobalInstance;
import android.view.MenuItem;
import android.widget.Toast;


public class CollectionListFragment extends ProfileListFragment implements FragmentResultListener {

	public static CollectionListFragment createDialog(boolean enableSelection) {
		CollectionListFragment fragment = new CollectionListFragment();
        fragment.setArguments(
                new BundleBuilder()
                .setIsDialog(enableSelection)
                .build());
		return fragment;
    }

	public static CollectionListFragment createFragment() {
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
                    CreateEmptyCollectionDialog.REQUEST_CREATE_EMPTY_COLLECTION_WAS_SUCCESSFUL, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ImportGpxFileDialog.REQUEST_IMPORT_OF_GPX_FILE_WAS_SUCCESSFUL, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(CreateEmptyCollectionDialog.REQUEST_CREATE_EMPTY_COLLECTION_WAS_SUCCESSFUL)) {
            // the newly created profile was already inserted into the database in the CreateEmptyCollectionDialog
            // so just refresh the ui
            requestUiUpdate();
        } else if (requestKey.equals(ImportGpxFileDialog.REQUEST_IMPORT_OF_GPX_FILE_WAS_SUCCESSFUL)) {
            // same for the ImportGpxFileDialog
            requestUiUpdate();
        }
    }

    @Override public String getTitle() {
        return getResources().getString(R.string.fragmentCollectionListName);
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
        final int MENU_ITEM_EMPTY_COLLECTION = 1;
        final int MENU_ITEM_FROM_GPX_FILE = 2;

        PopupMenu contextMenu = new PopupMenu(view.getContext(), view);
        contextMenu.getMenu().add(
                Menu.NONE, MENU_ITEM_EMPTY_COLLECTION, 1,
                GlobalInstance.getStringResource(R.string.contextMenuItemCollectionListEmptyCollection));
        contextMenu.getMenu().add(
                Menu.NONE, MENU_ITEM_FROM_GPX_FILE, 2,
                GlobalInstance.getStringResource(R.string.contextMenuItemCollectionListFromGpxFile));

        contextMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == MENU_ITEM_EMPTY_COLLECTION) {
                    CreateEmptyCollectionDialog.newInstance()
                        .show(getChildFragmentManager(), "CreateEmptyCollectionDialog");
                } else if (item.getItemId() == MENU_ITEM_FROM_GPX_FILE) {
                    ImportGpxFileDialog.newInstance()
                        .show(getChildFragmentManager(), "ImportGpxFileDialog");
                } else {
                    return false;
                }
                return true;
            }
        });

        contextMenu.show();
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


    public static class CreateEmptyCollectionDialog extends EnterStringDialog {
        public static final String REQUEST_CREATE_EMPTY_COLLECTION_WAS_SUCCESSFUL = "requestCreateEmptyCollectionWasSuccessful";


        public static CreateEmptyCollectionDialog newInstance() {
            CreateEmptyCollectionDialog dialog = new CreateEmptyCollectionDialog();
            return dialog;
        }


        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            setDialogTitle(
                    getResources().getString(R.string.layoutCollectionName));
            setMissingInputMessage(
                    getResources().getString(R.string.messageCollectionNameMissing));
            return super.onCreateDialog(savedInstanceState);
        }

        @Override public void execute(String input) {
            Collection emptyCollection = Collection.create(input, false);
            if (emptyCollection == null) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageCouldNotCreateCollection),
                        Toast.LENGTH_LONG).show();
                return;
            }

            Bundle result = new Bundle();
            getParentFragmentManager().setFragmentResult(REQUEST_CREATE_EMPTY_COLLECTION_WAS_SUCCESSFUL, result);
            dismiss();
        }
    }

}
