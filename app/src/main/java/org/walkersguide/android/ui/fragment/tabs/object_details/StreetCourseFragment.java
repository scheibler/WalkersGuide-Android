package org.walkersguide.android.ui.fragment.tabs.object_details;

import org.walkersguide.android.ui.fragment.tabs.object_details.RouteDetailsFragment;
import org.walkersguide.android.server.wg.street_course.StreetCourseRequest;

import androidx.core.view.ViewCompat;
import android.content.Context;

import android.os.Bundle;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;



import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.select.SelectMapDialog;
import org.walkersguide.android.util.GlobalInstance;
import androidx.fragment.app.Fragment;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.server.wg.status.OSMMap;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.util.SettingsManager;
import androidx.annotation.NonNull;

import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.street_course.StreetCourseTask;
import org.walkersguide.android.server.wg.WgException;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import org.walkersguide.android.ui.fragment.tabs.routes.NavigateFragment;


public class StreetCourseFragment extends Fragment implements FragmentResultListener, MenuProvider {


    public static StreetCourseFragment newInstance(StreetCourseRequest request) {
        StreetCourseFragment fragment = new StreetCourseFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_REQUEST, request);
        fragment.setArguments(args);
        return fragment;
    }


    // fragment
    private static final String KEY_REQUEST = "request";
    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_STREET_COURSE = "streetCourse";
    private static final String KEY_SHOW_NAVIGATION = "showNavigation";

    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;
    private StreetCourseRequest request;

    private Route streetCourse;
    private boolean showNavigation;

    private TextView labelStreetCourseRequestStatus;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();

        request = (StreetCourseRequest) getArguments().getSerializable(KEY_REQUEST);
        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
            streetCourse = (Route) savedInstanceState.getSerializable(KEY_STREET_COURSE);
            showNavigation = savedInstanceState.getBoolean(KEY_SHOW_NAVIGATION);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
            streetCourse = null;
            showNavigation = false;
        }

        // fragment result listener
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectMapDialog.REQUEST_SELECT_MAP, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectMapDialog.REQUEST_SELECT_MAP)) {
            SettingsManager.getInstance().setSelectedMap(
                    (OSMMap) bundle.getSerializable(SelectMapDialog.EXTRA_MAP));
            requestStreetCourse();
        }
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_street_course_fragment, menu);
    }

    @Override public void onPrepareMenu(@NonNull Menu menu) {
        boolean streetCourseLoadedSuccessfully = this.streetCourse != null;

        MenuItem menuItemRefresh = menu.findItem(R.id.menuItemRefresh);
        menuItemRefresh.setTitle(
                serverTaskExecutorInstance.taskInProgress(taskId)
                ? getResources().getString(R.string.menuItemCancel)
                : getResources().getString(R.string.menuItemRefresh));
        menuItemRefresh.setVisible(! streetCourseLoadedSuccessfully);

        MenuItem menuItemWalkStreetCourse = menu.findItem(R.id.menuItemWalkStreetCourse);
        menuItemWalkStreetCourse.setChecked(showNavigation);
        menuItemWalkStreetCourse.setVisible(streetCourseLoadedSuccessfully);
    }

    @Override public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemRefresh) {
            if (serverTaskExecutorInstance.taskInProgress(taskId)) {
                serverTaskExecutorInstance.cancelTask(taskId);
            } else {
                requestStreetCourse();
            }
        } else if (item.getItemId() == R.id.menuItemWalkStreetCourse) {
            showNavigation = ! showNavigation;
            updateUi();
        } else {
            return false;
        }
        return true;
    }


    /**
     * create view
     */

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_street_course, container, false);
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        labelStreetCourseRequestStatus = (TextView) view.findViewById(R.id.labelStreetCourseRequestStatus);
    }

    /**
     * pause and resume
     */


    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onResume() {
        super.onResume();

        if (streetCourse != null) {
            updateUi();

        } else {
            IntentFilter localIntentFilter = new IntentFilter();
            localIntentFilter.addAction(ServerTaskExecutor.ACTION_STREET_COURSE_TASK_SUCCESSFUL);
            localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
            localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);
            requestStreetCourse();
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
        savedInstanceState.putSerializable(KEY_STREET_COURSE,  streetCourse);
        savedInstanceState.putBoolean(KEY_SHOW_NAVIGATION, showNavigation);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            serverTaskExecutorInstance.cancelTask(taskId);
        }
    }


    /*
     * street course request
     */

    private void requestStreetCourse() {
        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelStreetCourseRequestStatus, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelStreetCourseRequestStatus.setText(
                GlobalInstance.getStringResource(R.string.messagePleaseWait));
        labelStreetCourseRequestStatus.setVisibility(View.VISIBLE);

        // start request
        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            taskId = serverTaskExecutorInstance.executeTask(new StreetCourseTask(request));
        }
    }


    // background task results

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_STREET_COURSE_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_STREET_COURSE_TASK_SUCCESSFUL)) {
                    streetCourse = (Route) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_ROUTE);
                    labelStreetCourseRequestStatus.setVisibility(View.GONE);
                    updateUi();

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {
                    streetCourse = null;
                    labelStreetCourseRequestStatus.setText(
                            GlobalInstance.getStringResource(R.string.errorReqRequestCancelled));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    WgException wgException = (WgException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (wgException != null) {
                        if (wgException.showMapDialog()) {
                            SelectMapDialog.newInstance(
                                    SettingsManager.getInstance().getSelectedMap())
                                .show(getChildFragmentManager(), "SelectMapDialog");
                        } else {
                            ViewCompat.setAccessibilityLiveRegion(
                                    labelStreetCourseRequestStatus, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                        }
                        labelStreetCourseRequestStatus.setText(wgException.getMessage());
                    }
                }
            }
        }
    };


    private void updateUi() {
        String tag = showNavigation ? "Navigate" : "RouteDetails";
        Fragment fragment = showNavigation
            ? NavigateFragment.newInstance(streetCourse, false)
            : RouteDetailsFragment.newInstance(streetCourse);

        // only replace, if the fragment is not already attached
        if (getChildFragmentManager().findFragmentByTag(tag) == null) {
            getChildFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragmentContainerStreetCourse, fragment, tag)
                .commit();
        }
    }

}
