package org.walkersguide.android.ui.activity;

import java.lang.ref.WeakReference;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.Intersection;
import org.walkersguide.android.data.basic.point.PointWrapper;
import org.walkersguide.android.data.basic.wrapper.Station;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.fragment.DeparturesFragment;
import org.walkersguide.android.ui.fragment.EntrancesFragment;
import org.walkersguide.android.ui.fragment.IntersectionWaysFragment;
import org.walkersguide.android.ui.fragment.PedestrianCrossingsFragment;
import org.walkersguide.android.ui.fragment.PointDetailsFragment;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class PointDetailsActivity extends AbstractActivity {

	// instance variables
    private DirectionManager directionManagerInstance;
    private PositionManager positionManagerInstance;
    private PointWrapper pointWrapper;

    // activity ui components
	private ViewPager mViewPager;
    private TabLayout tabLayout;
    private int recentFragment;
    private TextView labelPointDistanceAndBearing;

    // point, address, entrance, gps,
	private PointPagerAdapter pointPagerAdapter;
	public FragmentCommunicator pointDetailsFragmentCommunicator;

	// intersection
	private IntersectionPagerAdapter intersectionPagerAdapter;
	public FragmentCommunicator intersectionWaysFragmentCommunicator;
	public FragmentCommunicator pedestrianCrossingsFragmentCommunicator;

    // station
	private StationPagerAdapter stationPagerAdapter;
	public FragmentCommunicator departuresFragmentCommunicator;
	public FragmentCommunicator entrancesFragmentCommunicator;

	// fragment handler
	private Handler onFragmentEnabledHandler;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_point_details);
        directionManagerInstance = DirectionManager.getInstance(this);
        positionManagerInstance = PositionManager.getInstance(this);

        // load point
        try {
    		if (savedInstanceState != null) {
                pointWrapper = new PointWrapper(
                        this, new JSONObject(savedInstanceState.getString("jsonPointSerialized")));
            } else {
                pointWrapper = new PointWrapper(
		                this, new JSONObject(getIntent().getExtras().getString("jsonPointSerialized", "")));
            }
        } catch (JSONException e) {
            pointWrapper = null;
        }

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(
                getResources().getString(R.string.pointDetailsActivityTitle));

        if (pointWrapper != null) {
            // name, type and distance
    		TextView labelPointName = (TextView) findViewById(R.id.labelPointName);
            labelPointName.setText(
                    String.format(
                        getResources().getString(R.string.labelPointName),
                        pointWrapper.getPoint().getName())
                    );
    		TextView labelPointType = (TextView) findViewById(R.id.labelPointType);
            labelPointType.setText(
                    String.format(
                        getResources().getString(R.string.labelPointType),
                        pointWrapper.getPoint().getSubType())
                    );
    		labelPointDistanceAndBearing = (TextView) findViewById(R.id.labelPointDistanceAndBearing);
            // add to favorites
    		Button buttonPointFavorite = (Button) findViewById(R.id.buttonPointFavorite);
	    	buttonPointFavorite.setOnClickListener(new View.OnClickListener() {
		    	public void onClick(View view) {
                }
            });

            // simulate location
            Switch buttonPointSimulateLocation = (Switch) findViewById(R.id.buttonPointSimulateLocation);
            if (positionManagerInstance.getLocationSource() == Constants.LOCATION_SOURCE.SIMULATION
                    && pointWrapper.equals(positionManagerInstance.getCurrentLocation())) {
                buttonPointSimulateLocation.setChecked(true);
            }
            buttonPointSimulateLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                    if (isChecked) {
                        positionManagerInstance.setSimulatedLocation(pointWrapper);
                        positionManagerInstance.setLocationSource(
                                Constants.LOCATION_SOURCE.SIMULATION);
                    } else {
                        positionManagerInstance.setLocationSource(
                                Constants.LOCATION_SOURCE.GPS);
                    }
                }
            });

            // more options
    		Button buttonMore = (Button) findViewById(R.id.buttonMore);
	    	buttonMore.setOnClickListener(new View.OnClickListener() {
		    	public void onClick(View view) {
                }
            });

            // ViewPager and TabLayout
            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.addOnPageChangeListener(new TabLayoutOnPageChangeListenerBugFree(tabLayout));
            tabLayout = (TabLayout) findViewById(R.id.tab_layout);

            if (pointWrapper.getPoint() instanceof Station) {
                // set fragments for station
                stationPagerAdapter = new StationPagerAdapter(this);
                mViewPager.setAdapter(stationPagerAdapter);
                tabLayout.setupWithViewPager(mViewPager);
                tabLayout.setVisibility(View.VISIBLE);
                // set open fragment
            	if (savedInstanceState != null) {
        	    	recentFragment = savedInstanceState.getInt("recentFragment", 0);
                } else {
                    recentFragment = Constants.STATION_FRAGMENT.DETAILS;
                }
            } else if (pointWrapper.getPoint() instanceof Intersection) {
                // set fragments for intersection
                intersectionPagerAdapter = new IntersectionPagerAdapter(this);
                mViewPager.setAdapter(intersectionPagerAdapter);
                tabLayout.setupWithViewPager(mViewPager);
                tabLayout.setVisibility(View.VISIBLE);
                // set open fragment
            	if (savedInstanceState != null) {
        	    	recentFragment = savedInstanceState.getInt("recentFragment", 0);
                } else {
                    recentFragment = Constants.INTERSECTION_FRAGMENT.INTERSECTION_WAYS;
                }
            } else {
                // set fragments for other point types
                pointPagerAdapter = new PointPagerAdapter(this);
                mViewPager.setAdapter(pointPagerAdapter);
                tabLayout.setVisibility(View.GONE);
                // set open fragment
            	if (savedInstanceState != null) {
        	    	recentFragment = savedInstanceState.getInt("recentFragment", 0);
                } else {
                    recentFragment = Constants.POINT_FRAGMENT.DETAILS;
                }
            }

	    	// initialize handler for enabling fragment and open recent one
    		onFragmentEnabledHandler = new Handler();
            mViewPager.setCurrentItem(recentFragment);
        }
    }

	@Override public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
        if (pointWrapper != null) {
            try {
                savedInstanceState.putString("jsonPointSerialized", pointWrapper.toJson().toString());
            } catch (JSONException e) {
    	    	savedInstanceState.putString("jsonPointSerialized", "");
            }
        } else {
    	    savedInstanceState.putString("jsonPointSerialized", "");
        }
    	savedInstanceState.putInt("recentFragment", recentFragment);
	}

    @Override public void onPause() {
        super.onPause();
        if (pointWrapper != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(newLocationAndDirectionReceiver);
	    	leaveActiveFragment();
        }
    }

    @Override public void onResume() {
        super.onResume();
        if (pointWrapper != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_NEW_LOCATION);
            filter.addAction(Constants.ACTION_NEW_DIRECTION);
            LocalBroadcastManager.getInstance(this).registerReceiver(newLocationAndDirectionReceiver, filter);

            // request current location and direction values
            directionManagerInstance.requestCurrentDirection();
            positionManagerInstance.requestCurrentLocation();
            enterActiveFragment();
        }
    }


    /**
     * broadcast receiver
     */

    private BroadcastReceiver newLocationAndDirectionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (
                    (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                        && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD1.ID)
                    || (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)
                        && intent.getIntExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_THRESHOLD_ID, -1) >= DirectionManager.THRESHOLD1.ID)
                    ) {
                // update distance and bearing label
                labelPointDistanceAndBearing.setText(
                        String.format(
                            getResources().getString(R.string.labelPointDistanceAndBearing),
                            pointWrapper.distanceFromCurrentLocation(),
                            StringUtility.formatInstructionDirection(
                                context, pointWrapper.bearingFromCurrentLocation()))
                        );
            }
        }
    };


    /**
     * fragment management
     */

    private void leaveActiveFragment() {
        if (intersectionPagerAdapter != null) {
            switch (recentFragment) {
                case Constants.INTERSECTION_FRAGMENT.INTERSECTION_WAYS:
                    if (intersectionWaysFragmentCommunicator != null) {
                        intersectionWaysFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                case Constants.INTERSECTION_FRAGMENT.PEDESTRIAN_CROSSINGS:
                    if (pedestrianCrossingsFragmentCommunicator != null) {
                        pedestrianCrossingsFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                default:
                    break;
            }
        } else if (pointPagerAdapter != null) {
            switch (recentFragment) {
                case Constants.POINT_FRAGMENT.DETAILS:
                    if (pointDetailsFragmentCommunicator != null) {
                        pointDetailsFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                default:
                    break;
            }
        } else if (stationPagerAdapter != null) {
            switch (recentFragment) {
                case Constants.STATION_FRAGMENT.DEPARTURES:
                    if (departuresFragmentCommunicator != null) {
                        departuresFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                case Constants.STATION_FRAGMENT.DETAILS:
                    if (pointDetailsFragmentCommunicator != null) {
                        pointDetailsFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                case Constants.STATION_FRAGMENT.ENTRANCES:
                    if (entrancesFragmentCommunicator != null) {
                        entrancesFragmentCommunicator.onFragmentDisabled();
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
            if (intersectionPagerAdapter != null) {
                switch (currentFragment) {
                    case Constants.INTERSECTION_FRAGMENT.INTERSECTION_WAYS:
                        if (intersectionWaysFragmentCommunicator != null) {
                            intersectionWaysFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    case Constants.INTERSECTION_FRAGMENT.PEDESTRIAN_CROSSINGS:
                        if (pedestrianCrossingsFragmentCommunicator != null) {
                            pedestrianCrossingsFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    default:
                        return;
                }
            } else if (pointPagerAdapter != null) {
                switch (currentFragment) {
                    case Constants.POINT_FRAGMENT.DETAILS:
                        if (pointDetailsFragmentCommunicator != null) {
                            pointDetailsFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    default:
                        return;
                }
            } else if (stationPagerAdapter != null) {
                switch (recentFragment) {
                    case Constants.STATION_FRAGMENT.DEPARTURES:
                        if (departuresFragmentCommunicator != null) {
                            departuresFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    case Constants.STATION_FRAGMENT.DETAILS:
                        if (pointDetailsFragmentCommunicator != null) {
                            pointDetailsFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    case Constants.STATION_FRAGMENT.ENTRANCES:
                        if (entrancesFragmentCommunicator != null) {
                            entrancesFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    default:
                        break;
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

	public class PointPagerAdapter extends FragmentStatePagerAdapter {

		public PointPagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case Constants.POINT_FRAGMENT.DETAILS:
                    return PointDetailsFragment.newInstance(pointWrapper);
                default:
                    return null;
            }
        }

		@Override public CharSequence getPageTitle(int position) {
            switch (position) {
                case Constants.POINT_FRAGMENT.DETAILS:
    				return getResources().getString(R.string.fragmentPointDetailsName);
                default:
                    return "";
            }
		}

		@Override public int getCount() {
			return Constants.PointFragmentValueArray.length;
		}
	}


	public class IntersectionPagerAdapter extends FragmentStatePagerAdapter {

		public IntersectionPagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case Constants.INTERSECTION_FRAGMENT.INTERSECTION_WAYS:
                    return IntersectionWaysFragment.newInstance(pointWrapper);
                case Constants.INTERSECTION_FRAGMENT.PEDESTRIAN_CROSSINGS:
                    return PedestrianCrossingsFragment.newInstance(pointWrapper);
                default:
                    return null;
            }
        }

		@Override public CharSequence getPageTitle(int position) {
            switch (position) {
                case Constants.INTERSECTION_FRAGMENT.INTERSECTION_WAYS:
    				return getResources().getString(R.string.fragmentIntersectionWaysName);
                case Constants.INTERSECTION_FRAGMENT.PEDESTRIAN_CROSSINGS:
    				return getResources().getString(R.string.fragmentPedestrianCrossingsName);
                default:
                    return "";
            }
		}

		@Override public int getCount() {
			return Constants.IntersectionFragmentValueArray.length;
		}
	}


	public class StationPagerAdapter extends FragmentStatePagerAdapter {

		public StationPagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case Constants.STATION_FRAGMENT.DEPARTURES:
                    return DeparturesFragment.newInstance(pointWrapper);
                case Constants.STATION_FRAGMENT.DETAILS:
                    return PointDetailsFragment.newInstance(pointWrapper);
                case Constants.STATION_FRAGMENT.ENTRANCES:
                    return EntrancesFragment.newInstance(pointWrapper);
                default:
                    return null;
            }
        }

		@Override public CharSequence getPageTitle(int position) {
            switch (position) {
                case Constants.STATION_FRAGMENT.DEPARTURES:
    				return getResources().getString(R.string.fragmentDeparturesName);
                case Constants.STATION_FRAGMENT.DETAILS:
    				return getResources().getString(R.string.fragmentPointDetailsName);
                case Constants.STATION_FRAGMENT.ENTRANCES:
    				return getResources().getString(R.string.fragmentEntrancesName);
                default:
                    return "";
            }
		}

		@Override public int getCount() {
			return Constants.StationFragmentValueArray.length;
		}
	}


    /**
     * A custom Page Change Listener which fixes the bug described here:
     * https://code.google.com/p/android/issues/detail?id=183123
     **/

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
