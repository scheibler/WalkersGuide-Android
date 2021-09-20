package org.walkersguide.android.ui.activity.toolbar.tabs;

import org.walkersguide.android.server.route.StreetCourseRequest;
import androidx.core.view.ViewCompat;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;
    import org.walkersguide.android.ui.view.TextViewAndActionButton.LabelTextConfig;
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


import android.view.View;

import android.widget.TextView;


import org.walkersguide.android.data.basic.segment.IntersectionSegment;
import org.walkersguide.android.data.sensor.attribute.NewDirectionAttributes;
import org.walkersguide.android.data.sensor.threshold.BearingThreshold;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.ui.fragment.routing.RouteDetailsFragment;
import org.walkersguide.android.ui.fragment.SegmentDetailsFragment;
import org.walkersguide.android.util.Constants;
import java.util.ArrayList;
import org.walkersguide.android.data.basic.segment.Segment;
import org.walkersguide.android.util.GlobalInstance;


public class SegmentDetailsActivity extends AbstractTabsActivity {
    private static final String KEY_SEGMENT = "segment";

    public enum Tab {
        DETAILS, STREET_COURSE
    }


    public static void start(Context packageContext, Segment segment) {
        // start activity
        Intent intent = new Intent(packageContext, SegmentDetailsActivity.class);
        intent.putExtra(KEY_SEGMENT, segment);
        packageContext.startActivity(intent);
    }


	// instance variables
    private DirectionManager directionManagerInstance;
    private Segment segment;

    // activity ui components
    private TextView labelSegmentDirection;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        directionManagerInstance = DirectionManager.getInstance(this);

        // load segment
        segment = (Segment) getIntent().getExtras().getSerializable(KEY_SEGMENT);
        if (segment != null) {
            TextViewAndActionButton layoutSelectedSegment = (TextViewAndActionButton) findViewById(R.id.layoutSelectedSegment);
            layoutSelectedSegment.setOnLabelClickListener(new TextViewAndActionButton.OnLabelClickListener() {
                @Override public void onLabelClick(TextViewAndActionButton view) {
                    // nothing should happen here
                }
            });
            layoutSelectedSegment.configureView(segment, LabelTextConfig.empty(true));

            // type and bearing
    		TextView labelSegmentType = (TextView) findViewById(R.id.labelSegmentType);
            labelSegmentType.setText(
                    String.format(
                        getResources().getString(R.string.labelSegmentType),
                        segment.getSubType())
                    );
        	labelSegmentDirection = (TextView) findViewById(R.id.labelSegmentDirection);
            ViewCompat.setAccessibilityLiveRegion(
                    labelSegmentDirection, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);

            // prepare tab list
            ArrayList<SegmentDetailsActivity.Tab> tabList = new ArrayList<SegmentDetailsActivity.Tab>();
            tabList.add(Tab.DETAILS);
            int initialTabIndex = 0;
            if (segment instanceof IntersectionSegment) {
                tabList.add(Tab.STREET_COURSE);
            }

            initializeViewPagerAndTabLayout(
                    new TabAdapter(SegmentDetailsActivity.this, tabList),
                    initialTabIndex);
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
            filter.addAction(Constants.ACTION_NEW_DIRECTION);
            LocalBroadcastManager.getInstance(this).registerReceiver(newLocationAndDirectionReceiver, filter);
            // update ui
            updateDirectionLabel();
        }
    }

    private void updateDirectionLabel() {
        labelSegmentDirection.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelSegmentDirection),
                    StringUtility.formatRelativeViewingDirection(segment.bearingFromCurrentDirection()))
                );
    }


    /**
     * implement AbstractToolbarActivity and AbstractTabsActivity functions
     */

    public int getLayoutResourceId() {
		return R.layout.activity_segment_details;
    }

    public void tabSelected(int tabIndex) {
    }


    /**
     * broadcast receiver
     */

    private BroadcastReceiver newLocationAndDirectionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)) {
                NewDirectionAttributes newDirectionAttributes = NewDirectionAttributes.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_DIRECTION_ATTRIBUTES));
                if (newDirectionAttributes != null
                        && newDirectionAttributes.getAggregatingBearingThreshold().isAtLeast(BearingThreshold.TEN_DEGREES)) {
                    if (newDirectionAttributes.getAggregatingBearingThreshold().isAtLeast(BearingThreshold.TWENTY_DEGREES)) {
                        ViewCompat.setAccessibilityLiveRegion(
                                labelSegmentDirection, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                    }
                    updateDirectionLabel();
                }
            }
            ViewCompat.setAccessibilityLiveRegion(
                    labelSegmentDirection, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        }
    };


    /**
     * fragment management
     */


	private class TabAdapter extends AbstractTabAdapter {
        private ArrayList<SegmentDetailsActivity.Tab> tabList;

        public TabAdapter(FragmentActivity activity, ArrayList<SegmentDetailsActivity.Tab> tabList) {
            super(activity);
            this.tabList = tabList;
        }

        @Override public Fragment createFragment(int position) {
            switch (this.tabList.get(position)) {
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
    				return getResources().getString(R.string.fragmentSegmentDetailsName);
                case STREET_COURSE:
    				return getResources().getString(R.string.fragmentNextIntersectionsName);
                default:
                    return "";
            }
        }
	}

}
