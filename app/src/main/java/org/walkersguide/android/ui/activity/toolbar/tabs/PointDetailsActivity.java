package org.walkersguide.android.ui.activity.toolbar.tabs;

import androidx.core.view.ViewCompat;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
import org.walkersguide.android.ui.activity.toolbar.AbstractTabsActivity;
import org.walkersguide.android.ui.activity.toolbar.AbstractTabsActivity.AbstractTabAdapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.MenuItem;
import android.view.View;

import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;


import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.basic.point.Intersection;
import org.walkersguide.android.data.basic.point.POI;
import org.walkersguide.android.data.basic.point.Station;
import org.walkersguide.android.data.sensor.attribute.NewDirectionAttributes;
import org.walkersguide.android.data.sensor.attribute.NewLocationAttributes;
import org.walkersguide.android.data.sensor.threshold.BearingThreshold;
import org.walkersguide.android.data.sensor.threshold.DistanceThreshold;
import org.walkersguide.android.helper.PointUtility;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.ui.fragment.object_list.IntersectionStructureFragment;
import org.walkersguide.android.ui.fragment.PointDetailsFragment;
import org.walkersguide.android.ui.fragment.pt.DeparturesFragment;
import org.walkersguide.android.ui.fragment.object_list.SimpleObjectListFragment;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.data.basic.point.Point;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;
    import org.walkersguide.android.ui.view.TextViewAndActionButton.LabelTextConfig;
import org.walkersguide.android.util.GlobalInstance;


public class PointDetailsActivity extends AbstractTabsActivity {
    private static final String KEY_POINT = "point";

    public enum Tab {
        DEPARTURES, DETAILS, ENTRANCES, INTERSECTION_STRUCTURE, PEDESTRIAN_CROSSINGS
    }


    public static void start(Context packageContext, Point point) {
        // add to all points profile
        AccessDatabase.getInstance().addObjectToDatabaseProfile(
                point, DatabasePointProfile.ALL_POINTS);
        // start activity
        Intent intent = new Intent(packageContext, PointDetailsActivity.class);
        intent.putExtra(KEY_POINT, point);
        packageContext.startActivity(intent);
    }


	// instance variables
    private DirectionManager directionManagerInstance;
    private PositionManager positionManagerInstance;
    private Point point;

    private TextView labelPointDistanceAndBearing;
    private Switch buttonPointSimulateLocation;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        directionManagerInstance = DirectionManager.getInstance(this);
        positionManagerInstance = PositionManager.getInstance(this);

        // load point
        point = (Point) getIntent().getExtras().getSerializable(KEY_POINT);
        if (point != null) {
            TextViewAndActionButton layoutSelectedPoint = (TextViewAndActionButton) findViewById(R.id.layoutSelectedPoint);
            layoutSelectedPoint.setOnLabelClickListener(new TextViewAndActionButton.OnLabelClickListener() {
                @Override public void onLabelClick(TextViewAndActionButton view) {
                    // nothing should happen here
                }
            });
            layoutSelectedPoint.configureView(point, LabelTextConfig.empty(true));

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
            ArrayList<PointDetailsActivity.Tab> tabList = new ArrayList<PointDetailsActivity.Tab>();
            tabList.add(Tab.DETAILS);
            int initialTabIndex = 0;

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
                initialTabIndex = tabList.indexOf(Tab.INTERSECTION_STRUCTURE);
            }

            initializeViewPagerAndTabLayout(
                    new TabAdapter(PointDetailsActivity.this, tabList),
                    initialTabIndex);
        }
    }

    /*
    @Override public boolean onMenuItemClick(MenuItem item) {
        PointUtility.putNewPoint(
                PointDetailsActivity.this, pointWrapper, item.getItemId());
        PlanRouteDialog.newInstance()
            .show(getSupportFragmentManager(), "PlanRouteDialog");
        return true;
    }*/

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
            filter.addAction(Constants.ACTION_NEW_LOCATION);
            filter.addAction(Constants.ACTION_NEW_DIRECTION);
            LocalBroadcastManager.getInstance(this).registerReceiver(newLocationAndDirectionReceiver, filter);
            // update ui
            updateDistanceAndBearingLabel();
        }
    }

    private void updateDistanceAndBearingLabel() {
        labelPointDistanceAndBearing.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelPointDistanceAndBearing),
                    GlobalInstance.getPluralResource(R.plurals.meter, point.distanceFromCurrentLocation()),
                    StringUtility.formatRelativeViewingDirection(point.bearingFromCurrentLocation()))
                );
    }


    /**
     * implement AbstractToolbarActivity and AbstractTabsActivity functions
     */

    public int getLayoutResourceId() {
		return R.layout.activity_point_details;
    }

    public void tabSelected(int tabIndex) {
    }


    /**
     * broadcast receiver
     */

    private BroadcastReceiver newLocationAndDirectionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)) {
                NewLocationAttributes newLocationAttributes = NewLocationAttributes.fromString(
                        intent.getStringExtra(Constants.ACTION_NEW_LOCATION_ATTRIBUTES));
                if (newLocationAttributes != null
                        && newLocationAttributes.getAggregatingDistanceThreshold().isAtLeast(DistanceThreshold.ZERO_METERS)) {
                    if (newLocationAttributes.getAggregatingDistanceThreshold().isAtLeast(DistanceThreshold.TEN_METERS)) {
                        ViewCompat.setAccessibilityLiveRegion(
                                labelPointDistanceAndBearing, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
                    }
                    updateDistanceAndBearingLabel();
                }
            } else if (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)) {
                NewDirectionAttributes newDirectionAttributes = NewDirectionAttributes.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_DIRECTION_ATTRIBUTES));
                if (newDirectionAttributes != null
                        && newDirectionAttributes.getAggregatingBearingThreshold().isAtLeast(BearingThreshold.TEN_DEGREES)) {
                    if (newDirectionAttributes.getAggregatingBearingThreshold().isAtLeast(BearingThreshold.TWENTY_DEGREES)) {
                        ViewCompat.setAccessibilityLiveRegion(
                                labelPointDistanceAndBearing, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                    }
                    updateDistanceAndBearingLabel();
                }
            }
            ViewCompat.setAccessibilityLiveRegion(
                    labelPointDistanceAndBearing, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        }
    };


    /**
     * fragment management
     */

	private class TabAdapter extends AbstractTabAdapter {
        private ArrayList<PointDetailsActivity.Tab> tabList;

        public TabAdapter(FragmentActivity activity, ArrayList<PointDetailsActivity.Tab> tabList) {
            super(activity);
            this.tabList = tabList;
        }

        @Override public Fragment createFragment(int position) {
            switch (this.tabList.get(position)) {
                case DETAILS:
                    return PointDetailsFragment.newInstance(point);
                case DEPARTURES:
                    if (point instanceof Station) {
                        return DeparturesFragment.newInstance(point.getLatitude(), point.getLongitude());
                    }
                    return null;
                case ENTRANCES:
                    if (point instanceof POI) {
                        return SimpleObjectListFragment.newInstance(
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
                        return SimpleObjectListFragment.newInstance(
                                ((Intersection) point).getPedestrianCrossingList());
                    }
                    return null;
                default:
                    return null;
            }
        }

		@Override public int getItemCount() {
			return this.tabList.size();
        }

        @Override public String getFragmentName(int position) {
            switch (this.tabList.get(position)) {
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
                default:
                    return "";
            }
        }
    }

}
