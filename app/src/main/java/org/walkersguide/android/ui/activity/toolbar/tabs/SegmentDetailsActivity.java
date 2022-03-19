package org.walkersguide.android.ui.activity.toolbar.tabs;

import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.sensor.bearing.AcceptNewBearing;
import org.walkersguide.android.server.wg.street_course.StreetCourseRequest;
import androidx.core.view.ViewCompat;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;



import android.widget.TextView;


import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.ui.fragment.details.RouteDetailsFragment;
import org.walkersguide.android.ui.fragment.details.SegmentDetailsFragment;
import java.util.ArrayList;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.ui.activity.toolbar.TabLayoutActivity;


public class SegmentDetailsActivity extends TabLayoutActivity {
    private static final String KEY_SEGMENT = "segment";


    public static void start(Context packageContext, Segment segment) {
        startAtTab(packageContext, segment, null);
    }

    public static void startAtTab(Context packageContext, Segment segment, Tab tab) {
        // start activity
        Intent intent = new Intent(packageContext, SegmentDetailsActivity.class);
        intent.putExtra(KEY_SEGMENT, segment);
        intent.putExtra(KEY_SELECTED_TAB, tab);
        packageContext.startActivity(intent);
    }


    private Segment segment;

    private TextView labelSegmentDirection;

    @Override public int getLayoutResourceId() {
		return R.layout.activity_segment_details;
    }

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // load segment
        segment = (Segment) getIntent().getExtras().getSerializable(KEY_SEGMENT);
        if (segment != null) {
            TextViewAndActionButton layoutSelectedSegment = (TextViewAndActionButton) findViewById(R.id.layoutSelectedSegment);
            layoutSelectedSegment.setOnObjectDefaultActionListener(new TextViewAndActionButton.OnObjectDefaultActionListener() {
                @Override public void onObjectDefaultAction(TextViewAndActionButton view) {
                    // nothing should happen here
                }
            }, false);
            layoutSelectedSegment.configureAsSingleObject(
                    segment, segment.formatNameAndSubType());

            // bearing
        	labelSegmentDirection = (TextView) findViewById(R.id.labelSegmentDirection);

            // prepare tab list
            ArrayList<Tab> tabList = new ArrayList<Tab>();
            tabList.add(Tab.DETAILS);
            if (segment instanceof IntersectionSegment) {
                tabList.add(Tab.STREET_COURSE);
            }

            initializeViewPagerAndTabLayout(
                    new TabAdapter(SegmentDetailsActivity.this, tabList));
        }
    }

    @Override public void onPause() {
        super.onPause();
        if (segment != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(newLocationAndDirectionReceiver);
        }
    }

    @Override public void onResume() {
        super.onResume();
        if (segment != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
            LocalBroadcastManager.getInstance(this).registerReceiver(newLocationAndDirectionReceiver, filter);
            // request current direction to update the ui
            DeviceSensorManager.getInstance().requestCurrentBearing();
        }
    }

    private void updateDirectionLabel() {
        labelSegmentDirection.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelSegmentDirection),
                    segment.getBearing().relativeToCurrentBearing().getDirection())
                );
    }


    /**
     * broadcast receiver
     */

    private BroadcastReceiver newLocationAndDirectionReceiver = new BroadcastReceiver() {
        private AcceptNewBearing acceptNewBearing = AcceptNewBearing.newInstanceForDistanceLabelUpdate();

        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                Bearing currentBearing = (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (currentBearing != null
                        && acceptNewBearing.updateBearing(currentBearing)) {
                    updateDirectionLabel();
                }
            }
        }
    };


    /**
     * fragment management
     */

    public enum Tab {
        DETAILS, STREET_COURSE
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
                        return SegmentDetailsFragment.newInstance(segment);
                    case STREET_COURSE:
                        if (segment instanceof IntersectionSegment) {
                            IntersectionSegment intersectionSegment = (IntersectionSegment) segment;
                            return RouteDetailsFragment.streetCourse(
                                    new StreetCourseRequest(
                                        intersectionSegment.getIntersectionNodeId(),
                                        intersectionSegment.getId(),
                                        intersectionSegment.getNextNodeId()));
                        }
                        break;
                }
            }
            return null;
        }

        @Override public String getFragmentName(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case DETAILS:
                        return getResources().getString(R.string.fragmentSegmentDetailsName);
                    case STREET_COURSE:
                        return getResources().getString(R.string.fragmentStreetCourseDetailsName);
                }
            }
            return "";
        }
	}

}
