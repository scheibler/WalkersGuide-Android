package org.walkersguide.android.ui.fragment.object_list.extended;

import org.walkersguide.android.ui.dialog.select.SelectObjectWithIdFromMultipleSourcesDialog;
import android.widget.Toast;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.ObjectTypeFilter;
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
import org.walkersguide.android.database.profile.StaticProfile;
import timber.log.Timber;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import org.walkersguide.android.database.profile.Collection;
import org.walkersguide.android.database.profile.static_profile.HistoryProfile;
import androidx.fragment.app.DialogFragment;
import android.text.TextUtils;
import android.widget.ListView;
import android.widget.AdapterView;
import java.util.Arrays;
import android.widget.ArrayAdapter;


public class ObjectListFromDatabaseFragment extends ExtendedObjectListFragment implements FragmentResultListener {


    public static class BundleBuilder extends ExtendedObjectListFragment.BundleBuilder {
        public BundleBuilder(DatabaseProfileRequest request) {
            super();
            bundle.putSerializable(KEY_REQUEST, request);
        }
    }

	public static ObjectListFromDatabaseFragment selectObjectWithId(DatabaseProfile profile) {
		ObjectListFromDatabaseFragment fragment = new ObjectListFromDatabaseFragment();
        fragment.setArguments(
                new BundleBuilder(
                    new DatabaseProfileRequest(profile))
                .setSelectObjectWithId(true)
                .build());
		return fragment;
    }

	public static ObjectListFromDatabaseFragment newInstance(DatabaseProfile profile) {
		ObjectListFromDatabaseFragment fragment = new ObjectListFromDatabaseFragment();
        fragment.setArguments(
                new BundleBuilder(
                    new DatabaseProfileRequest(profile))
                .build());
		return fragment;
    }


    // dialog
    private static final String KEY_REQUEST = "request";

    private DatabaseProfileRequest request;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            request = (DatabaseProfileRequest) savedInstanceState.getSerializable(KEY_REQUEST);
        } else {
            request = (DatabaseProfileRequest) getArguments().getSerializable(KEY_REQUEST);
        }

        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectObjectTypeFilterDialog.REQUEST_SELECT_OBJECT_TYPE_FILTER, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectSortMethodDialog.REQUEST_SELECT_SORT_METHOD, this, this);
        if (isAddButtonVisible()) {
            getChildFragmentManager()
                .setFragmentResultListener(
                        SelectObjectWithIdFromMultipleSourcesDialog.REQUEST_SELECT_OBJECT_WITH_ID, this, this);
        }
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectObjectTypeFilterDialog.REQUEST_SELECT_OBJECT_TYPE_FILTER)) {
            request.setObjectTypeFilter(
                    (ObjectTypeFilter) bundle.getSerializable(SelectObjectTypeFilterDialog.EXTRA_OBJECT_TYPE_FILTER));
            resetListPosition();
            requestUiUpdate();
        } else if (requestKey.equals(SelectSortMethodDialog.REQUEST_SELECT_SORT_METHOD)) {
            request.setSortMethod(
                    (SortMethod) bundle.getSerializable(SelectSortMethodDialog.EXTRA_SORT_METHOD));
            resetListPosition();
            requestUiUpdate();
        } else if (requestKey.equals(SelectObjectWithIdFromMultipleSourcesDialog.REQUEST_SELECT_OBJECT_WITH_ID)) {
            SelectObjectWithIdFromMultipleSourcesDialog.Target objectWithIdTarget = (SelectObjectWithIdFromMultipleSourcesDialog.Target)
                bundle.getSerializable(SelectObjectWithIdFromMultipleSourcesDialog.EXTRA_TARGET);
            ObjectWithId selectedObjectWithId = (ObjectWithId) bundle.getSerializable(SelectObjectWithIdFromMultipleSourcesDialog.EXTRA_OBJECT_WITH_ID);
            if (objectWithIdTarget == SelectObjectWithIdFromMultipleSourcesDialog.Target.ADD_TO_COLLECTION
                    && this.request.getProfile().addObject(selectedObjectWithId)) {
                requestUiUpdate();
            }
        } else {
            super.onFragmentResult(requestKey, bundle);
        }
    }

    @Override public Profile getProfile() {
        return request != null ?  request.getProfile() : null;
    }


    /**
     * create view
     */

	@Override public View configureView(View view, Bundle savedInstanceState) {
        view = super.configureView(view, savedInstanceState);
        super.updateSearchTerm(request.getSearchTerm());
        return view;
    }

    @Override public void onSearchTermChanged(String newSearchTerm) {
        request.setSearchTerm(newSearchTerm);
    }

    @Override public String getTitle() {
        if (request.hasProfile()) {
            return getSelectObjectWithId()
                ? String.format(
                        getResources().getString(R.string.labelPleaseSelectFrom),
                        request.getProfile().getName())
                : request.getProfile().getName();
        }
        return null;
    }

    @Override public int getPluralResourceId() {
        if (request.hasSearchTerm()) {
            return R.plurals.result;
        } else if (request.hasProfile()) {
            return request.getProfile().getPluralResId();
        }
        return R.plurals.object;
    }

    @Override public boolean isAddButtonVisible() {
        return ! getSelectObjectWithId()
            && this.request.getProfile() instanceof Collection;
    }

    @Override public void addObjectWithIdButtonClicked(View view) {
        SelectObjectWithIdFromMultipleSourcesDialog.newInstance(
                SelectObjectWithIdFromMultipleSourcesDialog.Target.ADD_TO_COLLECTION)
            .show(getChildFragmentManager(), "SelectObjectWithIdFromMultipleSourcesDialog");
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
        // filter in collections
        MenuItem menuItemObjectTypeFilter = menu.findItem(R.id.menuItemObjectTypeFilter);
        menuItemObjectTypeFilter.setVisible(
                this.request != null && this.request.getProfile() instanceof Collection);
        // sort method
        MenuItem menuItemSortMethod = menu.findItem(R.id.menuItemSortMethod);
        menuItemSortMethod.setVisible(
                this.request != null && this.request.hasProfile());
        // show auto update and viewing direction filter
        boolean isHistoryProfile = this.request != null
            && this.request.getProfile() instanceof HistoryProfile;
        MenuItem menuItemAutoUpdate = menu.findItem(R.id.menuItemAutoUpdate);
        menuItemAutoUpdate.setVisible(! isHistoryProfile);
        MenuItem menuItemFilterResult = menu.findItem(R.id.menuItemFilterResult);
        menuItemFilterResult.setVisible(! isHistoryProfile);
        // clear profile
        MenuItem menuItemClearProfile = menu.findItem(R.id.menuItemClearProfile);
        menuItemClearProfile.setVisible(
                this.request != null && this.request.getProfile() instanceof StaticProfile);
    }

    @Override public boolean onMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemObjectTypeFilter) {
            SelectObjectTypeFilterDialog.newInstance(request.getObjectTypeFilter())
                .show(getChildFragmentManager(), "SelectObjectTypeFilterDialog");

        } else if (item.getItemId() == R.id.menuItemSortMethod) {
            SelectSortMethodDialog.newInstance(
                    request.getProfile().getSortMethodList(),
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
                            request.getProfile().getSortMethodList(),
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
                ArrayList<String> headingSecondLinePartList = new ArrayList<String>();
                if (request.hasObjectTypeFilter()
                        && request.getObjectTypeFilter() != ObjectTypeFilter.ALL) {
                    headingSecondLinePartList.add(
                            String.format(
                                GlobalInstance.getStringResource(R.string.labelHeadingSecondLineObjectTypeFilter),
                                request.getObjectTypeFilter().toString()));
                }
                if (! request.hasDefaultSortMethod()) {
                    headingSecondLinePartList.add(
                            String.format(
                                GlobalInstance.getStringResource(R.string.labelHeadingSecondLineSortMethod),
                                request.getSortMethod().toString()));
                }

                if (isAdded()) {
                    super.populateUiAfterRequestWasSuccessful(
                            TextUtils.join(", ", headingSecondLinePartList), objectList);
                }
            });
        });
    }

    @Override public void requestMoreResults() {
    }


    public static class SelectObjectTypeFilterDialog extends DialogFragment {
        public static final String REQUEST_SELECT_OBJECT_TYPE_FILTER = "selectObjectTypeFilter";
        public static final String EXTRA_OBJECT_TYPE_FILTER = "objectTypeFilter";

        public static SelectObjectTypeFilterDialog newInstance(ObjectTypeFilter selectedObjectTypeFilter) {
            SelectObjectTypeFilterDialog dialog= new SelectObjectTypeFilterDialog();
            Bundle args = new Bundle();
            args.putSerializable(KEY_SELECTED_OBJECT_TYPE_FILTER, selectedObjectTypeFilter);
            dialog.setArguments(args);
            return dialog;
        }

        // dialog
        private static final String KEY_SELECTED_OBJECT_TYPE_FILTER = "selectedObjectTypeFilter";

        private ObjectTypeFilter selectedObjectTypeFilter;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            selectedObjectTypeFilter = (ObjectTypeFilter) getArguments().getSerializable(KEY_SELECTED_OBJECT_TYPE_FILTER);

            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectObjectTypeFilterDialogTitle))
                .setItems(
                        new String[]{getResources().getString(R.string.messagePleaseWait)},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                        )
                .setNegativeButton(
                        getResources().getString(R.string.dialogCancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss();
                            }
                        }
                        )
                .create();
        }

        @Override public void onStart() {
            super.onStart();
            AlertDialog dialog = (AlertDialog) getDialog();
            if (dialog != null) {

                ListView listViewItems = (ListView) dialog.getListView();
                listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                        ObjectTypeFilter newObjectTypeFilter = (ObjectTypeFilter) parent.getItemAtPosition(position);
                        if (newObjectTypeFilter != null) {
                            Bundle result = new Bundle();
                            result.putSerializable(EXTRA_OBJECT_TYPE_FILTER, newObjectTypeFilter);
                            getParentFragmentManager().setFragmentResult(REQUEST_SELECT_OBJECT_TYPE_FILTER, result);
                        }
                        dismiss();
                    }
                });

                // fill listview
                ArrayList<ObjectTypeFilter> objectTypeFilterList = new ArrayList<>(Arrays.asList(ObjectTypeFilter.values()));
                listViewItems.setAdapter(
                        new ArrayAdapter<ObjectTypeFilter>(
                            getActivity(), android.R.layout.select_dialog_singlechoice, objectTypeFilterList));
                listViewItems.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

                // select list item
                if (selectedObjectTypeFilter != null) {
                    listViewItems.setItemChecked(
                            objectTypeFilterList.indexOf(selectedObjectTypeFilter), true);
                }
            }
        }
    }

}
