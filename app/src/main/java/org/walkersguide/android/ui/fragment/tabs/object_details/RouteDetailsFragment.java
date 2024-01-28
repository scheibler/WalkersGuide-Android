package org.walkersguide.android.ui.fragment.tabs.object_details;

import org.walkersguide.android.ui.view.UserAnnotationView;
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
import org.walkersguide.android.util.Helper;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;


public class RouteDetailsFragment extends Fragment {


    public static RouteDetailsFragment newInstance(Route route) {
        RouteDetailsFragment fragment = new RouteDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ROUTE, route);
        fragment.setArguments(args);
        return fragment;
    }


    // fragment
    private static final String KEY_ROUTE = "route";
    private static final String KEY_LIST_POSITION = "listPosition";

    private Route route;
    private int listPosition;

    private TextView labelDescription, labelHeading;
    private UserAnnotationView layoutUserAnnotation;
    private ListView listViewRouteObjects;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        route = (Route) getArguments().getSerializable(KEY_ROUTE);
        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            listPosition = route != null ? route.getCurrentPosition() : 0;
        }
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_route_details, container, false);
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        labelDescription = (TextView) view.findViewById(R.id.labelDescription);
        layoutUserAnnotation = (UserAnnotationView) view.findViewById(R.id.layoutUserAnnotation);
        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        listViewRouteObjects = (ListView) view.findViewById(R.id.listViewRouteObjects);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_LIST_POSITION,  listPosition);
    }


    /**
     * pause and resume
     */


    @Override public void onPause() {
        super.onPause();
    }

    @Override public void onResume() {
        super.onResume();
        if (route == null) {
            return;
        }

        labelDescription.setText(route.getDescription());
        layoutUserAnnotation.setObjectWithId(route);
        labelHeading.setText(
                GlobalInstance.getPluralResource(
                    R.plurals.point, route.getRouteObjectList().size()));

        listViewRouteObjects.setAdapter(
                new RouteObjectAdapter(
                    RouteDetailsFragment.this.getContext(), route));

        // list position
        listViewRouteObjects.setSelection(listPosition);
        listViewRouteObjects.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });
    }


    private static class RouteObjectAdapter extends BaseAdapter {

        private Context context;
        private Route route;

        public RouteObjectAdapter(Context context, Route route) {
            this.context = context;
            this.route = route;
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

            boolean isSelected = getItem(position).equals(route.getCurrentRouteObject());
            layoutRouteObject.configureAsListItem(getItem(position), position+1, isSelected);
            return layoutRouteObject;
        }

        @Override public int getCount() {
            if (this.route != null) {
                return this.route.getRouteObjectList().size();
            }
            return 0;
        }

        @Override public RouteObject getItem(int position) {
            if (this.route != null) {
                return this.route.getRouteObjectList().get(position);
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
