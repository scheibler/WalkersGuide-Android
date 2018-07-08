package org.walkersguide.android.ui.activity;

import java.lang.ref.WeakReference;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.segment.Footway;
import org.walkersguide.android.data.basic.segment.IntersectionSegment;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.ui.fragment.NextIntersectionsFragment;
import org.walkersguide.android.ui.fragment.SegmentDetailsFragment;
import org.walkersguide.android.util.Constants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class SegmentDetailsActivity extends AbstractActivity {

	// instance variables
    private DirectionManager directionManagerInstance;
    private SegmentWrapper segmentWrapper;

    // activity ui components
	private ViewPager mViewPager;
    private TabLayout tabLayout;
    private int recentFragment;
    private LinearLayout layoutFootwaySpecific;
    private TextView labelSegmentAbsoluteBearing, labelSegmentRelativeBearing;

    // footway and transport segments
	private SegmentPagerAdapter segmentPagerAdapter;
	public FragmentCommunicator segmentDetailsFragmentCommunicator;

	// intersection segment
	private IntersectionSegmentPagerAdapter intersectionSegmentPagerAdapter;
	public FragmentCommunicator nextIntersectionsFragmentCommunicator;

	// fragment handler
	private Handler onFragmentEnabledHandler;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_segment_details);
        directionManagerInstance = DirectionManager.getInstance(this);

        // load segment
        try {
    		if (savedInstanceState != null) {
                segmentWrapper = new SegmentWrapper(
                        this, new JSONObject(savedInstanceState.getString("jsonSegmentSerialized")));
            } else {
                segmentWrapper = new SegmentWrapper(
		                this, new JSONObject(getIntent().getExtras().getString("jsonSegmentSerialized", "")));
            }
        } catch (JSONException e) {
            segmentWrapper = null;
        }

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(
                getResources().getString(R.string.segmentDetailsActivityTitle));

        if (segmentWrapper != null) {
            // name, type and bearing
    		TextView labelSegmentName = (TextView) findViewById(R.id.labelSegmentName);
            labelSegmentName.setText(
                    String.format(
                        getResources().getString(R.string.labelSegmentName),
                        segmentWrapper.getSegment().getName())
                    );
    		TextView labelSegmentType = (TextView) findViewById(R.id.labelSegmentType);
            labelSegmentType.setText(
                    String.format(
                        getResources().getString(R.string.labelSegmentType),
                        segmentWrapper.getSegment().getSubType())
                    );
    		layoutFootwaySpecific = (LinearLayout) findViewById(R.id.layoutFootwaySpecific);

            // ViewPager and TabLayout
            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.addOnPageChangeListener(new TabLayoutOnPageChangeListenerBugFree(tabLayout));
            tabLayout = (TabLayout) findViewById(R.id.tab_layout);

            if (segmentWrapper.getSegment() instanceof Footway) {
                layoutFootwaySpecific.setVisibility(View.VISIBLE);
                // bearing label
        		labelSegmentAbsoluteBearing = (TextView) findViewById(R.id.labelSegmentAbsoluteBearing);
        		labelSegmentRelativeBearing = (TextView) findViewById(R.id.labelSegmentRelativeBearing);

                // exclude from routing
        		Switch buttonSegmentExcludeFromRouting = (Switch) findViewById(R.id.buttonSegmentExcludeFromRouting);
                buttonSegmentExcludeFromRouting.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                    }
                });

                // simulate direction
        		Switch buttonSegmentSimulateDirection = (Switch) findViewById(R.id.buttonSegmentSimulateDirection);
                if (directionManagerInstance.getDirectionSource() == Constants.DIRECTION_SOURCE.SIMULATION
                        &&directionManagerInstance.getCurrentDirection() == ((Footway) segmentWrapper.getSegment()).getBearing()) {
                    buttonSegmentSimulateDirection.setChecked(true);
                }
                buttonSegmentSimulateDirection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                        if (isChecked) {
                            directionManagerInstance.setSimulatedDirection(
                                    ((Footway) segmentWrapper.getSegment()).getBearing());
                            directionManagerInstance.setDirectionSource(
                                    Constants.DIRECTION_SOURCE.SIMULATION);
                        } else {
                            directionManagerInstance.setDirectionSource(
                                    directionManagerInstance.getPreviousDirectionSource());
                        }
                    }
                });

                if (segmentWrapper.getSegment() instanceof IntersectionSegment) {
                    // set fragments for intersection segment
                    intersectionSegmentPagerAdapter = new IntersectionSegmentPagerAdapter(this);
                    mViewPager.setAdapter(intersectionSegmentPagerAdapter);
                    tabLayout.setupWithViewPager(mViewPager);
                    tabLayout.setVisibility(View.VISIBLE);
                    // set open fragment
                	if (savedInstanceState != null) {
            	    	recentFragment = savedInstanceState.getInt("recentFragment", 0);
                    } else {
                        recentFragment = Constants.INTERSECTION_SEGMENT_FRAGMENT.DETAILS;
                    }

                } else {
                    // set fragments for other footway types
                    segmentPagerAdapter = new SegmentPagerAdapter(this);
                    mViewPager.setAdapter(segmentPagerAdapter);
                    tabLayout.setVisibility(View.GONE);
                    // set open fragment
                	if (savedInstanceState != null) {
        	        	recentFragment = savedInstanceState.getInt("recentFragment", 0);
                    } else {
                        recentFragment = Constants.SEGMENT_FRAGMENT.DETAILS;
                    }
                }

            } else {
                // set fragments for other segment types
                layoutFootwaySpecific.setVisibility(View.GONE);
                segmentPagerAdapter = new SegmentPagerAdapter(this);
                mViewPager.setAdapter(segmentPagerAdapter);
                tabLayout.setVisibility(View.GONE);
                // set open fragment
                if (savedInstanceState != null) {
                    recentFragment = savedInstanceState.getInt("recentFragment", 0);
                } else {
                    recentFragment = Constants.SEGMENT_FRAGMENT.DETAILS;
                }
            }

	    	// initialize handler for enabling fragment and open recent one
    		onFragmentEnabledHandler = new Handler();
            mViewPager.setCurrentItem(recentFragment);
        }
    }

	@Override public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
        if (segmentWrapper != null) {
            try {
                savedInstanceState.putString("jsonSegmentSerialized", segmentWrapper.toJson().toString());
            } catch (JSONException e) {
    	    	savedInstanceState.putString("jsonSegmentSerialized", "");
            }
        } else {
    	    savedInstanceState.putString("jsonSegmentSerialized", "");
        }
    	savedInstanceState.putInt("recentFragment", recentFragment);
	}

    @Override public void onPause() {
        super.onPause();
        if (segmentWrapper != null) {
            if (segmentWrapper.getSegment() instanceof Footway) {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(newLocationAndDirectionReceiver);
            }
	    	leaveActiveFragment();
        }
    }

    @Override public void onResume() {
        super.onResume();
        if (segmentWrapper != null) {
            if (segmentWrapper.getSegment() instanceof Footway) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(Constants.ACTION_NEW_DIRECTION);
                LocalBroadcastManager.getInstance(this).registerReceiver(newLocationAndDirectionReceiver, filter);
                // request current direction
                directionManagerInstance.requestCurrentDirection();
            }
            enterActiveFragment();
        }
    }


    /**
     * broadcast receiver
     */

    private BroadcastReceiver newLocationAndDirectionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)
                    && (
                           labelSegmentAbsoluteBearing.getText().equals("")
                        || labelSegmentRelativeBearing.getText().equals("")
                        || intent.getIntExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_THRESHOLD_ID, -1) >= DirectionManager.THRESHOLD1.ID)
                    ) {
                // update bearing labels
                // absolute bearing
                int absoluteBearing = ((Footway) segmentWrapper.getSegment()).getBearing();
                labelSegmentAbsoluteBearing.setText(
                        String.format(
                            getResources().getString(R.string.labelSegmentAbsoluteBearing),
                            absoluteBearing,
                            StringUtility.formatGeographicDirection(
                                context, absoluteBearing))
                        );
                // relative bearing
                int relativeBearing = ((Footway) segmentWrapper.getSegment()).bearingFromCurrentDirection();
                labelSegmentRelativeBearing.setText(
                        String.format(
                            getResources().getString(R.string.labelSegmentRelativeBearing),
                            StringUtility.formatInstructionDirection(
                                context, relativeBearing))
                        );
            }
        }
    };


    /**
     * fragment management
     */

    private void leaveActiveFragment() {
        if (intersectionSegmentPagerAdapter != null) {
            switch (recentFragment) {
                case Constants.INTERSECTION_SEGMENT_FRAGMENT.DETAILS:
                    if (segmentDetailsFragmentCommunicator != null) {
                        segmentDetailsFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                case Constants.INTERSECTION_SEGMENT_FRAGMENT.NEXT_INTERSECTIONS:
                    if (nextIntersectionsFragmentCommunicator != null) {
                        nextIntersectionsFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                default:
                    break;
            }
        } else if (segmentPagerAdapter != null) {
            switch (recentFragment) {
                case Constants.SEGMENT_FRAGMENT.DETAILS:
                    if (segmentDetailsFragmentCommunicator != null) {
                        segmentDetailsFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                default:
                    break;
            }
        }
    }

	private void enterActiveFragment() {
		onFragmentEnabledHandler.postDelayed(new OnFragmentEnabledUpdater(recentFragment), 0);
	}

	private class OnFragmentEnabledUpdater implements Runnable {
		private static final int NUMBER_OF_RETRIES = 5;
		private int counter;
		private int currentFragment;

		public OnFragmentEnabledUpdater(int currentFragment) {
			this.counter = 0;
			this.currentFragment = currentFragment;
		}

        @Override public void run() {
            if (intersectionSegmentPagerAdapter != null) {
                switch (currentFragment) {
                    case Constants.INTERSECTION_SEGMENT_FRAGMENT.DETAILS:
                        if (segmentDetailsFragmentCommunicator != null) {
                            segmentDetailsFragmentCommunicator.onFragmentDisabled();
                            return;
                        }
                        break;
                    case Constants.INTERSECTION_SEGMENT_FRAGMENT.NEXT_INTERSECTIONS:
                        if (nextIntersectionsFragmentCommunicator != null) {
                            nextIntersectionsFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    default:
                        return;
                }
            } else if (segmentPagerAdapter != null) {
                switch (currentFragment) {
                    case Constants.SEGMENT_FRAGMENT.DETAILS:
                        if (segmentDetailsFragmentCommunicator != null) {
                            segmentDetailsFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    default:
                        return;
                }
            } else {
                return;
            }
            if (counter < NUMBER_OF_RETRIES) {
                counter += 1;
                onFragmentEnabledHandler.postDelayed(this, 100);
            }
        }
    }


	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */

	public class SegmentPagerAdapter extends FragmentStatePagerAdapter {

		public SegmentPagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case Constants.SEGMENT_FRAGMENT.DETAILS:
                    return SegmentDetailsFragment.newInstance(segmentWrapper);
                default:
                    return null;
            }
        }

		@Override public CharSequence getPageTitle(int position) {
            switch (position) {
                case Constants.SEGMENT_FRAGMENT.DETAILS:
    				return getResources().getString(R.string.fragmentSegmentDetailsName);
                default:
                    return "";
            }
		}

		@Override public int getCount() {
			return Constants.SegmentFragmentValueArray.length;
		}
	}


	public class IntersectionSegmentPagerAdapter extends FragmentStatePagerAdapter {

		public IntersectionSegmentPagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case Constants.INTERSECTION_SEGMENT_FRAGMENT.DETAILS:
                    return SegmentDetailsFragment.newInstance(segmentWrapper);
                case Constants.INTERSECTION_SEGMENT_FRAGMENT.NEXT_INTERSECTIONS:
                    return NextIntersectionsFragment.newInstance(segmentWrapper);
                default:
                    return null;
            }
        }

		@Override public CharSequence getPageTitle(int position) {
            switch (position) {
                case Constants.INTERSECTION_SEGMENT_FRAGMENT.DETAILS:
    				return getResources().getString(R.string.fragmentIntersectionWaysName);
                case Constants.INTERSECTION_SEGMENT_FRAGMENT.NEXT_INTERSECTIONS:
    				return getResources().getString(R.string.fragmentNextIntersectionsName);
                default:
                    return "";
            }
		}

		@Override public int getCount() {
			return Constants.IntersectionSegmentFragmentValueArray.length;
		}
	}


    private class TabLayoutOnPageChangeListenerBugFree implements ViewPager.OnPageChangeListener {

        private final WeakReference<TabLayout> mTabLayoutRef;
        private int mPreviousScrollState;
        private int mScrollState;

        public TabLayoutOnPageChangeListenerBugFree(TabLayout tabLayout) {
            mTabLayoutRef = new WeakReference<TabLayout>(tabLayout);
        }

        @Override public void onPageScrollStateChanged(int state) {
            mPreviousScrollState = mScrollState;
            mScrollState = state;
        }

        @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            final TabLayout tabLayout = mTabLayoutRef.get();
            if (tabLayout != null) {
                final boolean updateText = (mScrollState == ViewPager.SCROLL_STATE_DRAGGING)
                    || (mScrollState == ViewPager.SCROLL_STATE_SETTLING
                            && mPreviousScrollState == ViewPager.SCROLL_STATE_DRAGGING);
                tabLayout.setScrollPosition(position, positionOffset, updateText);
            }
        }

        @Override public void onPageSelected(int position) {
            if (recentFragment != position) {
                leaveActiveFragment();
                final TabLayout tabLayout = mTabLayoutRef.get();
                if (tabLayout != null) {
                    tabLayout.getTabAt(position).select();
                }
                recentFragment = position;
                enterActiveFragment();
            }
        }
    }

}
