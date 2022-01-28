package org.walkersguide.android.ui.activity.toolbar.tabs;

import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.sensor.bearing.AcceptNewQuadrant;
import org.walkersguide.android.sensor.position.AcceptNewPosition;
import androidx.core.view.ViewCompat;
import org.walkersguide.android.ui.activity.toolbar.TabLayoutActivity;
import org.walkersguide.android.ui.activity.toolbar.TabLayoutActivity.AbstractTabAdapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;


import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.POI;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.poi.Station;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.fragment.details.PointDetailsFragment;
import org.walkersguide.android.ui.fragment.pt.DeparturesFragment;
import org.walkersguide.android.ui.fragment.object_list.simple.EntranceListFragment;
import org.walkersguide.android.ui.fragment.object_list.simple.IntersectionStructureFragment;
import org.walkersguide.android.ui.fragment.object_list.simple.PedestrianCrossingListFragment;
import org.walkersguide.android.data.object_with_id.Point;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.sensor.PositionManager;
import timber.log.Timber;


public class PointDetailsActivity extends TabLayoutActivity {
    public static final String KEY_POINT = "point";


    public static void start(Context packageContext, Point point) {
        startAtTab(packageContext, point, null);
    }

    public static void startAtTab(Context packageContext, Point point, Tab tab) {
        // add to database profile
        if (point instanceof Intersection) {
            DatabaseProfile.intersectionPoints().add((Intersection) point);
        } else if (point instanceof Station) {
            DatabaseProfile.stationPoints().add((Station) point);
        } else {
            DatabaseProfile.allPoints().add(point);
        }
        // start activity
        Intent intent = new Intent(packageContext, PointDetailsActivity.class);
        intent.putExtra(KEY_POINT, point);
        intent.putExtra(KEY_SELECTED_TAB, tab);
        packageContext.startActivity(intent);
    }


    private Point point;

    private TextView labelPointDistanceAndBearing;

    @Override public int getLayoutResourceId() {
		return R.layout.activity_point_details;
    }

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // load point
        point = (Point) getIntent().getExtras().getSerializable(KEY_POINT);
        if (point != null) {
            TextViewAndActionButton layoutSelectedPoint = (TextViewAndActionButton) findViewById(R.id.layoutSelectedPoint);
            layoutSelectedPoint.setOnObjectDefaultActionListener(new TextViewAndActionButton.OnObjectDefaultActionListener() {
                @Override public void onObjectDefaultAction(TextViewAndActionButton view) {
                    // nothing should happen here
                }
            }, false);
            layoutSelectedPoint.configureAsSingleObject(point, point.getName());

            // type and distance
    		TextView labelPointType = (TextView) findViewById(R.id.labelPointType);
            labelPointType.setText(
                    String.format(
                        getResources().getString(R.string.labelPointType),
                        point.getSubType())
                    );
    		labelPointDistanceAndBearing = (TextView) findViewById(R.id.labelPointDistanceAndBearing);
            ViewCompat.setAccessibilityLiveRegion(
                    labelPointDistanceAndBearing, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);

            // prepare tab list
            ArrayList<Tab> tabList = new ArrayList<Tab>();
            tabList.add(Tab.DETAILS);

            if (point instanceof POI
                    || point instanceof Station) {
                POI poiOrStation = (POI) point;
                if (poiOrStation instanceof Station) {
                    tabList.add(Tab.DEPARTURES);
                }
                if (! poiOrStation.getEntranceList().isEmpty()) {
                    tabList.add(Tab.ENTRANCES);
                }

            } else if (point instanceof Intersection) {
                Intersection intersection = (Intersection) point;
                tabList.add(Tab.INTERSECTION_STRUCTURE);
                if (! intersection.getPedestrianCrossingList().isEmpty()) {
                    tabList.add(Tab.PEDESTRIAN_CROSSINGS);
                }
            }

            initializeViewPagerAndTabLayout(
                    new TabAdapter(PointDetailsActivity.this, tabList));
        }
    }

    @Override public void onPause() {
        super.onPause();
        if (point != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(newLocationAndDirectionReceiver);
        }
    }

    @Override public void onResume() {
        super.onResume();
        if (point != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(PositionManager.ACTION_NEW_LOCATION);
            filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
            LocalBroadcastManager.getInstance(this).registerReceiver(newLocationAndDirectionReceiver, filter);
            // update ui
            updateDistanceAndBearingLabel();
        }
    }

    private void updateDistanceAndBearingLabel() {
        labelPointDistanceAndBearing.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelPointDistanceAndBearing),
                    point.formatDistanceAndRelativeBearingFromCurrentLocation())
                );
        ViewCompat.setAccessibilityLiveRegion(
                labelPointDistanceAndBearing, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
    }


    /**
     * broadcast receiver
     */

    private BroadcastReceiver newLocationAndDirectionReceiver = new BroadcastReceiver() {
        private AcceptNewPosition ttsAnnouncement = AcceptNewPosition.newInstanceForTtsAnnouncement();
        private AcceptNewQuadrant acceptNewQuadrant = AcceptNewQuadrant.newInstanceForObjectListSort();

        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                if (ttsAnnouncement.updatePoint(
                            (Point) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION))) {
                    ViewCompat.setAccessibilityLiveRegion(
                            labelPointDistanceAndBearing, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
                }
                updateDistanceAndBearingLabel();

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                Bearing currentBearing = (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (currentBearing != null
                        && acceptNewQuadrant.updateQuadrant(currentBearing.getQuadrant())) {
                    updateDistanceAndBearingLabel();
                }
            }
        }
    };


    /**
     * fragment management
     */

    public enum Tab {
        DEPARTURES, DETAILS, ENTRANCES, INTERSECTION_STRUCTURE, PEDESTRIAN_CROSSINGS
    }


	private class TabAdapter extends AbstractTabAdapter {

        public TabAdapter(FragmentActivity activity, ArrayList<Tab> tabList) {
            super(activity, tabList);
        }

        @Override public Fragment createFragment(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case DETAILS:
                        return PointDetailsFragment.newInstance(point);
                    case DEPARTURES:
                        if (point instanceof Station) {
                            return DeparturesFragment.newInstance(point.getLatitude(), point.getLongitude());
                        }
                        return null;
                    case ENTRANCES:
                        if (point instanceof POI) {
                            return EntranceListFragment.newInstance(
                                    ((POI) point).getEntranceList());
                        }
                        return null;
                    case INTERSECTION_STRUCTURE:
                        if (point instanceof Intersection) {
                            return IntersectionStructureFragment.newInstance(
                                    ((Intersection) point).getSegmentList());
                        }
                        return null;
                    case PEDESTRIAN_CROSSINGS:
                        if (point instanceof Intersection) {
                            return PedestrianCrossingListFragment.newInstance(
                                    ((Intersection) point).getPedestrianCrossingList());
                        }
                        return null;
                }
            }
            return null;
        }

        @Override public String getFragmentName(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case DETAILS:
                        return getResources().getString(R.string.fragmentPointDetailsName);
                    case DEPARTURES:
                        return getResources().getString(R.string.fragmentDeparturesName);
                    case ENTRANCES:
                        return getResources().getString(R.string.fragmentEntrancesName);
                    case INTERSECTION_STRUCTURE:
                        return getResources().getString(R.string.fragmentIntersectionStructureName);
                    case PEDESTRIAN_CROSSINGS:
                        return getResources().getString(R.string.fragmentPedestrianCrossingsName);
                }
            }
            return "";
        }
    }

}
