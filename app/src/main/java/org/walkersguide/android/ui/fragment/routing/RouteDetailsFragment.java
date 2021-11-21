package org.walkersguide.android.ui.fragment.routing;

import org.walkersguide.android.database.profiles.DatabaseRouteProfile;
import org.walkersguide.android.server.route.StreetCourseRequest;

import org.walkersguide.android.server.route.RouteManager;
import org.walkersguide.android.server.route.RouteManager.StreetCourseRequestListener;
    import org.walkersguide.android.ui.view.TextViewAndActionButton.LabelTextConfig;
import androidx.core.view.ViewCompat;
import android.content.Context;

import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


import org.walkersguide.android.server.util.ServerUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.dialog.selectors.SelectMapDialog;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import androidx.fragment.app.Fragment;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import org.walkersguide.android.ui.view.TextViewAndActionButton;
import android.widget.BaseAdapter;
import org.walkersguide.android.data.route.RouteObject;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.server.util.OSMMap;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.util.SettingsManager;
import androidx.annotation.NonNull;


public class RouteDetailsFragment extends Fragment implements FragmentResultListener, StreetCourseRequestListener {
    private static final String KEY_ROUTE = "route";
    private static final String KEY_STREET_COURSE_REQUEST = "streetCourseRequest";
    private static final String KEY_LIST_POSITION = "listPosition";

	// Store instance variables
    private RouteManager routeManagerInstance;
    private SettingsManager settingsManagerInstance;
    private StreetCourseRequest request;
    private Route route;
    private int listPosition;

    // query in progress
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private Vibrator vibrator;

    // ui components
    private TextViewAndActionButton layoutStartPoint, layoutDestinationPoint;
    private TextView labelDescription;
    private ImageButton buttonRefresh;
    private ListView listViewRoute;
    private TextView labelHeading, labelEmptyListView;


	public static RouteDetailsFragment newInstance(Route route) {
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


	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        routeManagerInstance = RouteManager.getInstance();
        settingsManagerInstance = SettingsManager.getInstance();
        // progress updater
        this.progressHandler = new Handler();
        this.progressUpdater = new ProgressUpdater();
        this.vibrator = (Vibrator) GlobalInstance.getContext().getSystemService(Context.VIBRATOR_SERVICE);
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
     * create view
     */

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_route_details, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        request = (StreetCourseRequest) getArguments().getSerializable(KEY_STREET_COURSE_REQUEST);
        if (savedInstanceState != null) {
            route = (Route) savedInstanceState.getSerializable(KEY_ROUTE);
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            route = (Route) getArguments().getSerializable(KEY_ROUTE);
            listPosition = 0;
        }

        layoutStartPoint = (TextViewAndActionButton) view.findViewById(R.id.layoutStartPoint);
        layoutDestinationPoint = (TextViewAndActionButton) view.findViewById(R.id.layoutDestinationPoint);
        labelDescription = (TextView) view.findViewById(R.id.labelDescription);

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (request != null) {
                    if (routeManagerInstance.streetCourseRequestInProgress()) {
                        routeManagerInstance.cancelStreetCourseRequest();
                    } else {
                        requestStreetCourse();
                    }
                }
            }
        });

        listViewRoute = (ListView) view.findViewById(R.id.listView);
        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        listViewRoute.setEmptyView(labelEmptyListView);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_ROUTE,  route);
        savedInstanceState.putInt(KEY_LIST_POSITION,  listPosition);
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        routeManagerInstance.invalidateStreetCourseRequest(RouteDetailsFragment.this);
        progressHandler.removeCallbacks(progressUpdater);
    }

    @Override public void onResume() {
        super.onResume();
        layoutStartPoint.setVisibility(View.GONE);
        layoutDestinationPoint.setVisibility(View.GONE);
        labelDescription.setVisibility(View.GONE);
        if (request != null) {
            requestStreetCourse();
        } else if (route != null) {
            layoutStartPoint.setVisibility(View.VISIBLE);
            layoutDestinationPoint.setVisibility(View.VISIBLE);
            labelDescription.setVisibility(View.VISIBLE);
            showRoute();
        }
    }

    private void showRoute() {
        layoutStartPoint.configureView(route.getStartPoint(), LabelTextConfig.start(true));
        layoutDestinationPoint.configureView(route.getDestinationPoint(), LabelTextConfig.destination(true));
        labelDescription.setText(route.getDescription());
        labelHeading.setText(
                GlobalInstance.getPluralResource(
                    R.plurals.point, route.getRouteObjectList().size()));
        listViewRoute.setAdapter(
                new RouteObjectAdapter(
                    RouteDetailsFragment.this.getContext(), route.getRouteObjectList()));
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
        private static final int ID_SEGMENT = 1;
        private static final int ID_POINT = 2;

        private Context context;
        private ArrayList<RouteObject> routeObjectList;

        public RouteObjectAdapter(Context context, ArrayList<RouteObject> routeObjectList) {
            this.context = context;
            this.routeObjectList = routeObjectList;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            RouteObject routeObject = getItem(position);
            LinearLayout layoutRouteObject = null;

            if (convertView == null) {
                LayoutParams lp = new LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                // create linear layout
                layoutRouteObject = new LinearLayout(this.context);
                layoutRouteObject.setLayoutParams(lp);
                layoutRouteObject.setOrientation(LinearLayout.VERTICAL);
                // segment
                TextViewAndActionButton layoutSegment = new TextViewAndActionButton(this.context);
                layoutSegment.setId(ID_SEGMENT);
                layoutSegment.setLayoutParams(lp);
                layoutRouteObject.addView(layoutSegment);
                // point
                TextViewAndActionButton layoutPoint = new TextViewAndActionButton(this.context);
                layoutPoint.setId(ID_POINT);
                layoutPoint.setLayoutParams(lp);
                layoutRouteObject.addView(layoutPoint);
            } else {
                layoutRouteObject = (LinearLayout) convertView;
            }

            // segment
            TextViewAndActionButton layoutSegment = (TextViewAndActionButton) layoutRouteObject.findViewById(ID_SEGMENT);
            if (routeObject.getSegment() != null) {
                layoutSegment.configureView(
                        routeObject.getSegment(), LabelTextConfig.empty(false));
                layoutSegment.setVisibility(View.VISIBLE);
            } else {
                layoutSegment.setVisibility(View.GONE);
            }
            // point
            TextViewAndActionButton layoutPoint = (TextViewAndActionButton) layoutRouteObject.findViewById(ID_POINT);
            layoutPoint.configureView(
                    routeObject.getPoint(), LabelTextConfig.empty(false));

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
    }


    //

    private void requestStreetCourse() {
        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelHeading.setText(
                GlobalInstance.getPluralResource(R.plurals.point, 0));
        buttonRefresh.setContentDescription(
                GlobalInstance.getStringResource(R.string.buttonCancel));
        buttonRefresh.setImageResource(R.drawable.cancel);

        // list view
        listViewRoute.setAdapter(null);
        listViewRoute.setOnScrollListener(null);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setText(
                GlobalInstance.getStringResource(R.string.messagePleaseWait));

        // start request
        progressHandler.postDelayed(progressUpdater, 2000);
        routeManagerInstance.startStreetCourseRequest(RouteDetailsFragment.this, request);
    }

    @Override public void streetCourseRequestSuccessful(Route newRoute) {
        AccessDatabase.getInstance().addObjectToDatabaseProfile(newRoute, DatabaseRouteProfile.STREET_COURSES);
        route = newRoute;
        listPosition = 0;
        resetRefreshButtonAndCancelProgressUpdater();
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        showRoute();
    }

    @Override public void streetCourseRequestFailed(int returnCode) {
        route = null;
        resetRefreshButtonAndCancelProgressUpdater();
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        labelEmptyListView.setText(
                ServerUtility.getErrorMessageForReturnCode(returnCode));
        // show select map dialog
        if (isAdded()
                && (
                    returnCode == Constants.RC.MAP_LOADING_FAILED
                    || returnCode == Constants.RC.WRONG_MAP_SELECTED)
           ) {
            SelectMapDialog.newInstance(
                    settingsManagerInstance.getSelectedMap())
                .show(getChildFragmentManager(), "SelectMapDialog");
           }
    }

    private void resetRefreshButtonAndCancelProgressUpdater() {
        buttonRefresh.setContentDescription(
                GlobalInstance.getStringResource(R.string.buttonRefresh));
        buttonRefresh.setImageResource(R.drawable.refresh);
        progressHandler.removeCallbacks(progressUpdater);
    }


    private class ProgressUpdater implements Runnable {
        public void run() {
            vibrator.vibrate(50);
            progressHandler.postDelayed(this, 2000);
        }
    }

}
