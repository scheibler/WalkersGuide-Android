package org.walkersguide.android.ui.fragment.object_list.extended;

import org.walkersguide.android.data.profile.Profile;
import org.walkersguide.android.data.object_with_id.HikingTrail;
import org.walkersguide.android.ui.fragment.object_list.ExtendedObjectListFragment;


import org.walkersguide.android.R;

import timber.log.Timber;
import android.content.Context;


import android.os.Bundle;



import android.view.View;



import java.util.ArrayList;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.server.wg.status.OSMMap;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.util.SettingsManager;

import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.HikingTrailsTask;
import org.walkersguide.android.server.wg.WgException;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.Intent;
import org.walkersguide.android.sensor.PositionManager;
import android.view.Menu;
import android.view.MenuItem;


public class HikingTrailListFromServerFragment extends ExtendedObjectListFragment {

	public static HikingTrailListFromServerFragment newInstance() {
		HikingTrailListFromServerFragment fragment = new HikingTrailListFromServerFragment();
        fragment.setArguments(
                new ExtendedObjectListFragment.BundleBuilder().build());
		return fragment;
    }


    // fragment / dialog
    private static final String KEY_TASK_ID = "taskId";

    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
    }

    @Override public Profile getProfile() {
        return null;
    }


    /**
     * create view
     */

	@Override public View configureView(View view, Bundle savedInstanceState) {
        view = super.configureView(view, savedInstanceState);

        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
        }

        return view;
    }

    @Override public void onSearchTermChanged(String newSearchTerm) {
    }

    @Override public String getDialogTitle() {
        return getResources().getString(R.string.fragmentHikingTrailListName);
    }

    @Override public int getPluralResourceId() {
        return R.plurals.hikingTrail;
    }

    @Override public String getEmptyObjectListMessage() {
        return getResources().getString(R.string.labelNoHikingTrailsNearby);
    }


    /**
     * menu
     */

    @Override public void onPrepareMenu(@NonNull Menu menu) {
        super.onPrepareMenu(menu);
        // refresh
        MenuItem menuItemRefresh = menu.findItem(R.id.menuItemRefresh);
        menuItemRefresh.setVisible(true);
    }


    /**
     * pause and resume
     */

    @Override public void onResume() {
        super.onResume();
        Timber.d("onResume");

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_HIKING_TRAILS_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);
    }

    @Override public void onPause() {
        super.onPause();
        Timber.d("onPause");
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            serverTaskExecutorInstance.cancelTask(taskId);
        }
    }

    @Override public boolean isUiUpdateRequestInProgress() {
        return serverTaskExecutorInstance.taskInProgress(taskId);
    }

    @Override public void prepareRequest() {
        super.prepareRequest();
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
        if (serverTaskExecutorInstance.taskInProgress(taskId)) {
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

        taskId = serverTaskExecutorInstance.executeTask(
                new HikingTrailsTask(currentLocation));
    }

    @Override public void requestMoreResults() {
    }

    // background task results

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_HIKING_TRAILS_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_HIKING_TRAILS_TASK_SUCCESSFUL)) {
                    hikingTrailsTaskSuccessful(
                            (ArrayList<HikingTrail>) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_HIKING_TRAIL_LIST));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {
                    HikingTrailListFromServerFragment.super.populateUiAfterRequestFailed(
                            GlobalInstance.getStringResource(R.string.errorReqRequestCancelled));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    WgException wgException = (WgException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (wgException != null) {
                        HikingTrailListFromServerFragment.super.populateUiAfterRequestFailed(wgException.getMessage());
                    }
                }
            }
        }
    };

    private void hikingTrailsTaskSuccessful(ArrayList<HikingTrail> hikingTrailList) {
        resetListPosition();
        super.populateUiAfterRequestWasSuccessful(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelHeadingSecondLineRadius),
                    GlobalInstance.getPluralResource(
                        R.plurals.kiloMeter, HikingTrailsTask.DEFAULT_TRAIL_RADIUS / 1000)),
                hikingTrailList, false, false);
    }

}
