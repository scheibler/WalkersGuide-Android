package org.walkersguide.android.ui.fragment.object_list.extended;

import org.walkersguide.android.ui.dialog.select.SelectCollectionsDialog;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.data.Angle;
import org.walkersguide.android.ui.fragment.object_list.ExtendedObjectListFragment;
import org.walkersguide.android.server.wg.poi.PoiCategory;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.server.wg.poi.PoiProfileRequest;
import org.walkersguide.android.server.wg.poi.PoiProfileResult;

import org.walkersguide.android.ui.dialog.select.SelectPoiCategoriesDialog;

import org.walkersguide.android.R;

import timber.log.Timber;
import android.content.Context;


import android.os.Bundle;



import android.view.View;



import java.util.ArrayList;
import org.walkersguide.android.data.object_with_id.Point;
import android.os.Handler;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.server.wg.status.OSMMap;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.util.SettingsManager;

import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.poi.PoiProfileTask;
import org.walkersguide.android.server.wg.WgException;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.Intent;
import android.os.Looper;
import org.walkersguide.android.sensor.PositionManager;
import android.text.TextUtils;
import org.walkersguide.android.util.Helper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import org.walkersguide.android.database.profile.Collection;
import org.walkersguide.android.ui.fragment.ObjectListFragment;


public class PoiListFromServerFragment extends ExtendedObjectListFragment
        implements FragmentResultListener, Runnable {


    public static class BundleBuilder extends ObjectListFragment.BundleBuilder {
        public BundleBuilder(PoiProfileRequest request) {
            super();
            bundle.putSerializable(KEY_REQUEST, request);
        }
    }

    public static PoiListFromServerFragment selectObjectWithId(PoiProfile profile) {
        PoiListFromServerFragment fragment = new PoiListFromServerFragment();
        fragment.setArguments(
                new BundleBuilder(
                    new PoiProfileRequest(profile))
                .setSelectObjectWithId(true)
                .build());
        return fragment;
    }

    public static PoiListFromServerFragment newInstance(PoiProfile profile) {
        PoiListFromServerFragment fragment = new PoiListFromServerFragment();
        fragment.setArguments(
                new BundleBuilder(
                    new PoiProfileRequest(profile))
                .build());
        return fragment;
    }


    // fragment / dialog
    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_REQUEST = "request";

    private ServerTaskExecutor serverTaskExecutorInstance;
    private Handler progressHandler = new Handler(Looper.getMainLooper());

    private long taskId;
    private PoiProfileRequest request;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();

        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
            request = (PoiProfileRequest) savedInstanceState.getSerializable(KEY_REQUEST);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
            request = (PoiProfileRequest) getArguments().getSerializable(KEY_REQUEST);
        }

        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectPoiCategoriesDialog.REQUEST_SELECT_POI_CATEGORIES, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectCollectionsDialog.REQUEST_SELECT_COLLECTIONS, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectPoiCategoriesDialog.REQUEST_SELECT_POI_CATEGORIES)) {
            Timber.d("onFragmentResult: categories selected");
            request.getProfile().setPoiCategoryList(
                    (ArrayList<PoiCategory>) bundle.getSerializable(SelectPoiCategoriesDialog.EXTRA_POI_CATEGORY_LIST));
            resetListPosition();
            requestUiUpdate();
        } else if (requestKey.equals(SelectCollectionsDialog.REQUEST_SELECT_COLLECTIONS)) {
            request.getProfile().setCollectionList(
                    (ArrayList<Collection>) bundle.getSerializable(SelectCollectionsDialog.EXTRA_COLLECTION_LIST));
            resetListPosition();
            requestUiUpdate();
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
    private Button buttonSelectPoiCategories, buttonSelectCollections;

    @Override public View configureView(View view, Bundle savedInstanceState) {
        view = super.configureView(view, savedInstanceState);
        super.updateSearchTerm(request.getSearchTerm());

        // show buttons
        ((LinearLayout) view.findViewById(R.id.layoutPoiListFromServerFragment))
            .setVisibility(View.VISIBLE);

        buttonSelectPoiCategories = (Button) view.findViewById(R.id.buttonSelectPoiCategories);
        buttonSelectPoiCategories.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (request.hasProfile()) {
                    SelectPoiCategoriesDialog.newInstance(
                            request.getProfile().getPoiCategoryList())
                        .show(getChildFragmentManager(), "SelectPoiCategoriesDialog");
                }
            }
        });

        buttonSelectCollections = (Button) view.findViewById(R.id.buttonSelectCollections);
        buttonSelectCollections.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (request.hasProfile()) {
                    SelectCollectionsDialog.newInstance(
                            request.getProfile().getCollectionList())
                        .show(getChildFragmentManager(), "SelectCollectionsDialog");
                }
            }
        });

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
        }
        return R.plurals.poi;
    }


    /**
     * menu
     */

    @Override public void onPrepareMenu(@NonNull Menu menu) {
        super.onPrepareMenu(menu);
        // refresh
        MenuItem menuItemRefresh = menu.findItem(R.id.menuItemRefresh);
        menuItemRefresh.setVisible(true);
        // show auto update
        MenuItem menuItemAutoUpdate = menu.findItem(R.id.menuItemAutoUpdate);
        menuItemAutoUpdate.setVisible(true);
        // viewing direction filter
        MenuItem menuItemFilterResult = menu.findItem(R.id.menuItemFilterResult);
        menuItemFilterResult.setVisible(true);
    }


    /**
     * pause and resume
     */

    @Override public void onResume() {
        super.onResume();
        Timber.d("onResume");

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_POI_PROFILE_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);
    }

    @Override public void onPause() {
        super.onPause();
        Timber.d("onPause");
        progressHandler.removeCallbacks(PoiListFromServerFragment.this);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
        savedInstanceState.putSerializable(KEY_REQUEST, request);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            serverTaskExecutorInstance.cancelTask(taskId, true);
        }
    }

    @Override public boolean isUiUpdateRequestInProgress() {
        return serverTaskExecutorInstance.taskInProgress(taskId);
    }

    @Override public void prepareRequest() {
        super.prepareRequest();

        int numberOfSelectedPoiCategories = request.hasProfile()
            ? request.getProfile().getPoiCategoryList().size() : 0;
        buttonSelectPoiCategories.setText(
                GlobalInstance.getPluralResource(
                    R.plurals.category, numberOfSelectedPoiCategories));
        buttonSelectPoiCategories.setContentDescription(
                String.format(
                    "%1$s: %2$s",
                    GlobalInstance.getPluralResource(
                        R.plurals.poiCategorySelected, numberOfSelectedPoiCategories),
                    request.hasProfile()
                    ? TextUtils.join(", ", request.getProfile().getPoiCategoryList())
                    : "")
                );

        int numberOfSelectedCollections = request.hasProfile()
            ? request.getProfile().getCollectionList().size() : 0;
        buttonSelectCollections.setText(
                GlobalInstance.getPluralResource(
                    R.plurals.collection, numberOfSelectedCollections));
        buttonSelectCollections.setContentDescription(
                String.format(
                    "%1$s: %2$s",
                    GlobalInstance.getPluralResource(
                        R.plurals.collectionSelected, numberOfSelectedCollections),
                    request.hasProfile()
                    ? TextUtils.join(", ", request.getProfile().getCollectionList())
                    : "")
                );
    }

    @Override public void swipeToRefreshDetected() {
        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            super.swipeToRefreshDetected();
        }
    }

    @Override public void refreshMenuItemClicked() {
        if (serverTaskExecutorInstance.taskInProgress(taskId)) {
            serverTaskExecutorInstance.cancelTask(taskId, true);
        } else {
            super.refreshMenuItemClicked();
        }
    }

    @Override public void requestUiUpdate() {
        startPoiProfileTask(PoiProfileTask.RequestAction.UPDATE);
    }

    @Override public void requestMoreResults() {
        startPoiProfileTask(PoiProfileTask.RequestAction.MORE_RESULTS);
    }

    private void startPoiProfileTask(PoiProfileTask.RequestAction action) {
        if (serverTaskExecutorInstance.taskInProgress(taskId)) {
            Timber.d("cancel previous task");
            serverTaskExecutorInstance.cancelTask(taskId, true);
        }
        this.prepareRequest();

        // get current position
        Point currentLocation = PositionManager.getInstance().getCurrentLocation();
        if (currentLocation == null) {
            super.populateUiAfterRequestFailed(
                    getResources().getString(R.string.errorNoLocationFound));
            return;
        }

        progressHandler.postDelayed(PoiListFromServerFragment.this, 2000);
        taskId = serverTaskExecutorInstance.executeTask(
                new PoiProfileTask(request, action, currentLocation));
    }

    // background task results

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_POI_PROFILE_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_POI_PROFILE_TASK_SUCCESSFUL)) {
                    poiProfileTaskSuccessful(
                            (PoiProfileResult) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_POI_PROFILE_RESULT));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {
                    PoiListFromServerFragment.super.populateUiAfterRequestFailed(
                            GlobalInstance.getStringResource(R.string.errorReqRequestCancelled));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    WgException wgException = (WgException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (wgException != null) {
                        PoiListFromServerFragment.super.populateUiAfterRequestFailed(wgException.getMessage());
                    }
                }

                progressHandler.removeCallbacks(PoiListFromServerFragment.this);
            }
        }
    };

    private void poiProfileTaskSuccessful(PoiProfileResult result) {
        if (result.getResetListPosition()) {
            resetListPosition();
        }
        super.populateUiAndShowMoreResultsFooterAfterRequestWasSuccessful(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelHeadingSecondLineRadius),
                    GlobalInstance.getPluralResource(R.plurals.meter, result.getLookupRadius())),
                result.getAllObjectList());
    }

    // progress vibration

    @Override public void run() {
        Helper.vibrateOnce(
                Helper.VIBRATION_DURATION_SHORT, Helper.VIBRATION_INTENSITY_WEAK);
        progressHandler.postDelayed(PoiListFromServerFragment.this, 2000);
    }

}
