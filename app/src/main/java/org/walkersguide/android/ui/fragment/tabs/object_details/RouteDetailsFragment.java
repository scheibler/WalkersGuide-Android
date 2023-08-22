package org.walkersguide.android.ui.fragment.tabs.object_details;

import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.ui.view.RouteObjectView;
import org.walkersguide.android.server.wg.street_course.StreetCourseRequest;

import androidx.core.view.ViewCompat;
import android.content.Context;

import android.os.Bundle;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.select.SelectMapDialog;
import org.walkersguide.android.util.GlobalInstance;
import androidx.fragment.app.Fragment;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.BaseAdapter;
import org.walkersguide.android.data.object_with_id.route.RouteObject;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.database.util.AccessDatabase;
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
import org.walkersguide.android.ui.activity.MainActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import timber.log.Timber;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import org.walkersguide.android.util.Helper;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;


public class RouteDetailsFragment extends Fragment
        implements FragmentResultListener, MenuProvider, OnRefreshListener {


    // instance constructors
    private static final String KEY_ROUTE = "route";
    private static final String KEY_STREET_COURSE_REQUEST = "streetCourseRequest";

	public static RouteDetailsFragment newInstance(Route route) {
        DatabaseProfile.allRoutes().add(route);
		RouteDetailsFragment fragment = new RouteDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ROUTE, route);
        fragment.setArguments(args);
		return fragment;
	}

	public static RouteDetailsFragment streetCourse(StreetCourseRequest request) {
		RouteDetailsFragment fragment = new RouteDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_STREET_COURSE_REQUEST, request);
        fragment.setArguments(args);
		return fragment;
	}


    // fragment
    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_LIST_POSITION = "listPosition";

    private SettingsManager settingsManagerInstance;
    private ServerTaskExecutor serverTaskExecutorInstance;

    private StreetCourseRequest request;
    private long taskId;

    private Route route;
    private int listPosition;

    private SwipeRefreshLayout swipeRefreshListView, swipeRefreshEmptyTextView;
    private ListView listViewRoute;
    private TextView labelHeading, labelEmptyListView;


	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsManagerInstance = SettingsManager.getInstance();
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();

        // fragment result listener
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectMapDialog.REQUEST_SELECT_MAP, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectMapDialog.REQUEST_SELECT_MAP)) {
            settingsManagerInstance.setSelectedMap(
                    (OSMMap) bundle.getSerializable(SelectMapDialog.EXTRA_MAP));
            requestStreetCourse();
        }
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        if (modeStreetCourse()) {
            menuInflater.inflate(R.menu.menu_toolbar_route_details_fragment_street_course, menu);
        }
    }

    @Override public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemRefresh) {
            if (modeStreetCourse()) {
                if (serverTaskExecutorInstance.taskInProgress(taskId)) {
                    serverTaskExecutorInstance.cancelTask(taskId);
                } else {
                    requestStreetCourse();
                }
            }
        } else if (item.getItemId() == R.id.menuItemLoadRoute) {
            if (route != null) {
                MainActivity.loadRoute(
                        RouteDetailsFragment.this.getContext(), route);
            }
        } else {
            return false;
        }
        return true;
    }


    /**
     * create view
     */

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.layout_heading_and_list_view_without_add_button, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        request = (StreetCourseRequest) getArguments().getSerializable(KEY_STREET_COURSE_REQUEST);
        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
            route = (Route) savedInstanceState.getSerializable(KEY_ROUTE);
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
            route = (Route) getArguments().getSerializable(KEY_ROUTE);
            listPosition = route != null ? route.getCurrentPosition() : 0;
        }
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);

        swipeRefreshListView = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshListView);
        swipeRefreshListView.setOnRefreshListener(this);
        listViewRoute = (ListView) view.findViewById(R.id.listView);

        swipeRefreshEmptyTextView = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshEmptyTextView);
        swipeRefreshEmptyTextView.setOnRefreshListener(this);
        listViewRoute.setEmptyView(swipeRefreshEmptyTextView);
        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
    }

    /**
     * pause and resume
     */


    @Override public void onRefresh() {
        if (modeStreetCourse()
                && ! serverTaskExecutorInstance.taskInProgress(taskId)) {
            Helper.vibrateOnce(
                    Helper.VIBRATION_DURATION_SHORT, Helper.VIBRATION_INTENSITY_WEAK);
            requestStreetCourse();
        }
    }

    @Override public void onResume() {
        super.onResume();

        if (route != null) {
            showRoute();

        } else if (modeStreetCourse()) {
            IntentFilter localIntentFilter = new IntentFilter();
            localIntentFilter.addAction(ServerTaskExecutor.ACTION_STREET_COURSE_TASK_SUCCESSFUL);
            localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
            localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);
            requestStreetCourse();
        }
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
        savedInstanceState.putSerializable(KEY_ROUTE,  route);
        savedInstanceState.putInt(KEY_LIST_POSITION,  listPosition);
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

    private boolean modeStreetCourse() {
        return request != null;
    }

    private void requestStreetCourse() {
        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelHeading.setText(
                GlobalInstance.getPluralResource(R.plurals.point, 0));

        // list view
        listViewRoute.setAdapter(null);
        listViewRoute.setOnScrollListener(null);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setText(
                GlobalInstance.getStringResource(R.string.messagePleaseWait));

        // start request
        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            swipeRefreshListView.setRefreshing(true);
            swipeRefreshEmptyTextView.setRefreshing(true);
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
                    route = (Route) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_ROUTE);
                    ViewCompat.setAccessibilityLiveRegion(
                            labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                    listPosition = 0;
                    showRoute();

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {
                    route = null;
                    labelEmptyListView.setText(
                            GlobalInstance.getStringResource(R.string.errorReqRequestCancelled));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    WgException wgException = (WgException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (wgException != null) {
                        if (wgException.showMapDialog()) {
                            SelectMapDialog.newInstance(
                                    settingsManagerInstance.getSelectedMap())
                                .show(getChildFragmentManager(), "SelectMapDialog");
                        } else {
                            ViewCompat.setAccessibilityLiveRegion(
                                    labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                        }
                        labelEmptyListView.setText(wgException.getMessage());
                    }
                }

                swipeRefreshListView.setRefreshing(false);
                swipeRefreshEmptyTextView.setRefreshing(false);
            }
        }
    };


    /**
     * show route
     */

    private void showRoute() {
        labelHeading.setText(
                GlobalInstance.getPluralResource(
                    R.plurals.point, route.getRouteObjectList().size()));
        listViewRoute.setAdapter(
                new RouteObjectAdapter(
                    RouteDetailsFragment.this.getContext(),
                    route.getRouteObjectList()));
        labelEmptyListView.setText("");

        // list position
        listViewRoute.setSelection(listPosition);
        listViewRoute.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });
    }


    private class RouteObjectAdapter extends BaseAdapter {

        private Context context;
        private ArrayList<RouteObject> routeObjectList;

        public RouteObjectAdapter(Context context, ArrayList<RouteObject> routeObjectList) {
            this.context = context;
            this.routeObjectList = routeObjectList;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            RouteObjectView layoutRouteObject = null;
            if (convertView == null) {
                layoutRouteObject = new RouteObjectView(this.context);
                layoutRouteObject.setLayoutParams(
                        new LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            } else {
                layoutRouteObject = (RouteObjectView) convertView;
            }

            boolean showSelectedRouteObjectLabel = ! modeStreetCourse()
                && getItem(position).equals(route.getCurrentRouteObject());
            layoutRouteObject.configureAsListItem(getItem(position), showSelectedRouteObjectLabel);
            return layoutRouteObject;
        }

        @Override public int getCount() {
            if (this.routeObjectList != null) {
                return this.routeObjectList.size();
            }
            return 0;
        }

        @Override public RouteObject getItem(int position) {
            if (this.routeObjectList != null) {
                return this.routeObjectList.get(position);
            }
            return null;
        }

        @Override public long getItemId(int position) {
            return position;
        }

        private class EntryHolder {
            public RouteObjectView layoutRouteObject;
        }
    }

}
