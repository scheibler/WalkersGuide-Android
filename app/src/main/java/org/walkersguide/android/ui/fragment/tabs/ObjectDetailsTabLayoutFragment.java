package org.walkersguide.android.ui.fragment.tabs;

import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.sensor.bearing.AcceptNewBearing;
import org.walkersguide.android.sensor.position.AcceptNewPosition;

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
import org.walkersguide.android.ui.fragment.TabLayoutFragment;
import org.walkersguide.android.ui.fragment.tabs.details.PointDetailsFragment;
import org.walkersguide.android.ui.fragment.pt.DeparturesFragment;
import org.walkersguide.android.ui.fragment.object_list.simple.EntranceListFragment;
import org.walkersguide.android.ui.fragment.object_list.simple.IntersectionStructureFragment;
import org.walkersguide.android.ui.fragment.object_list.simple.PedestrianCrossingListFragment;
import org.walkersguide.android.data.object_with_id.Point;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.sensor.PositionManager;
import timber.log.Timber;
import org.walkersguide.android.data.ObjectWithId;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.NavUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.data.object_with_id.HikingTrail;
import org.walkersguide.android.ui.fragment.tabs.details.HikingTrailDetailsFragment;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.ui.fragment.tabs.details.RouteDetailsFragment;
import org.walkersguide.android.ui.fragment.tabs.details.SegmentDetailsFragment;
import org.walkersguide.android.server.wg.street_course.StreetCourseRequest;
import org.walkersguide.android.ui.fragment.TabLayoutFragment.AbstractTabAdapter;


public class ObjectDetailsTabLayoutFragment extends TabLayoutFragment {
    private static final String KEY_OBJECT = "object";

	public static ObjectDetailsTabLayoutFragment details(ObjectWithId object) {
        return newInstance(object, null);
	}

	public static ObjectDetailsTabLayoutFragment departures(Station station) {
        return newInstance(station, Tab.DEPARTURES);
	}

	public static ObjectDetailsTabLayoutFragment entrances(POI poi) {
        return newInstance(poi, Tab.ENTRANCES);
	}

	public static ObjectDetailsTabLayoutFragment streetCourse(IntersectionSegment intersectionSegment) {
        return newInstance(intersectionSegment, Tab.STREET_COURSE);
	}

	private static ObjectDetailsTabLayoutFragment newInstance(ObjectWithId object, Tab selectedTab) {
        // add object to database
        if (object instanceof Intersection) {
            DatabaseProfile.intersectionPoints().add((Intersection) object);
        } else if (object instanceof Station) {
            DatabaseProfile.stationPoints().add((Station) object);
        } else if (object instanceof Point) {
            DatabaseProfile.allPoints().add((Point) object);
        }

        // create fragment
		ObjectDetailsTabLayoutFragment fragment = new ObjectDetailsTabLayoutFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_OBJECT, object);
        args.putSerializable(KEY_SELECTED_TAB, selectedTab);
        fragment.setArguments(args);
		return fragment;
    }


    /**
     * layout
     */

    private ObjectWithId object;

    private TextView labelDetails;

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_object_details, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        // load object
        object = (ObjectWithId) getArguments().getSerializable(KEY_OBJECT);
        if (object != null) {
            TextViewAndActionButton layoutObject = (TextViewAndActionButton) view.findViewById(R.id.layoutObject);
            layoutObject.setOnObjectDefaultActionListener(new TextViewAndActionButton.OnObjectDefaultActionListener() {
                @Override public void onObjectDefaultAction(TextViewAndActionButton view) {
                    // show context menu instead of details again (prevent loop)
                    TextView label = view.getLabel();
                    ObjectWithId objectWithId = view.getObject();
                    if (label != null && objectWithId != null) {
                        view.showContextMenu(label, objectWithId);
                    }
                }
            }, false);
            layoutObject.configureAsSingleObject(
                    object, object.formatNameAndSubType());

            // details label
    		labelDetails = (TextView) view.findViewById(R.id.labelDetails);
            labelDetails.setVisibility(View.GONE);

            // prepare tab list
            ArrayList<Tab> tabList = new ArrayList<Tab>();
            if (! (object instanceof Intersection)) {
                tabList.add(Tab.DETAILS);
            }

            if (object instanceof Station) {
                tabList.add(Tab.DEPARTURES);
            }

            if (object instanceof POI && ((POI) object).hasEntrance()) {
                tabList.add(Tab.ENTRANCES);
            }

            if (object instanceof Intersection) {
                tabList.add(Tab.INTERSECTION_STRUCTURE);
                if (((Intersection) object).hasPedestrianCrossing()) {
                    tabList.add(Tab.PEDESTRIAN_CROSSINGS);
                }
            }

            if (object instanceof IntersectionSegment) {
                tabList.add(Tab.STREET_COURSE);
            }

            initializeViewPagerAndTabLayout(
                    new TabAdapter(ObjectDetailsTabLayoutFragment.this, tabList));
        }
    }


    /**
     * pause and resume
     */
    private AcceptNewPosition acceptNewPositionForTtsAnnouncement;

    @Override public void onPause() {
        super.onPause();
        if (object instanceof Point) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(newLocationAndDirectionReceiverForPoints);
        } else if (object instanceof Segment) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(newDirectionReceiverForSegments);
        }
    }

    @Override public void onResume() {
        super.onResume();

        if (object instanceof Point) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(PositionManager.ACTION_NEW_LOCATION);
            filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(newLocationAndDirectionReceiverForPoints, filter);
            // request current location to update the ui
            labelDetails.setVisibility(View.VISIBLE);
            acceptNewPositionForTtsAnnouncement = AcceptNewPosition.newInstanceForTtsAnnouncement();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override public void run() {
                    // wait, until onResume is finished and the ui has focus
                    PositionManager.getInstance().requestCurrentLocation();
                }
            }, 200);

        } else if (object instanceof Route) {
            labelDetails.setText(((Route) object).getDescription());
            labelDetails.setVisibility(View.VISIBLE);

        } else if (object instanceof Segment) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(newDirectionReceiverForSegments, filter);
            // request current direction to update the ui
            labelDetails.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override public void run() {
                    // wait, until onResume is finished and the ui has focus
                    DeviceSensorManager.getInstance().requestCurrentBearing();
                }
            }, 200);
        }
    }


    /**
     * broadcast receiver
     */

    // point: distance and bearing

    private BroadcastReceiver newLocationAndDirectionReceiverForPoints = new BroadcastReceiver() {
        // distance label
        private AcceptNewPosition acceptNewPositionForDistanceLabel = AcceptNewPosition.newInstanceForDistanceLabelUpdate();
        private AcceptNewBearing acceptNewBearing = AcceptNewBearing.newInstanceForDistanceLabelUpdate();

        private TTSWrapper ttsWrapperInstance = TTSWrapper.getInstance();

        @Override public void onReceive(Context context, Intent intent) {
            if (! getActivity().hasWindowFocus()) {
                if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)
                        && intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION) != null
                        && intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false)) {
                    updateDistanceAndBearingLabel();
                }
                return;
            }

            if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                Point currentLocation = (Point) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION);
                if (currentLocation != null) {
                    if (intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false)
                            || acceptNewPositionForDistanceLabel.updatePoint(currentLocation)) {
                        updateDistanceAndBearingLabel();
                    }
                    if (acceptNewPositionForTtsAnnouncement.updatePoint(currentLocation)) {
                        ttsWrapperInstance.announce(
                                ((Point) object).formatDistanceAndRelativeBearingFromCurrentLocation(R.plurals.meter));
                    }
                }

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                Bearing currentBearing = (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (currentBearing != null
                        && acceptNewBearing.updateBearing(currentBearing)) {
                    updateDistanceAndBearingLabel();
                }
            }
        }

        private void updateDistanceAndBearingLabel() {
            labelDetails.setText(
                    String.format(
                        GlobalInstance.getStringResource(R.string.labelPointDistanceAndBearing),
                        ((Point) object).formatDistanceAndRelativeBearingFromCurrentLocation(R.plurals.meter))
                    );
        }
    };

    // segments: bearing

    private BroadcastReceiver newDirectionReceiverForSegments = new BroadcastReceiver() {
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

        private void updateDirectionLabel() {
            labelDetails.setText(
                    String.format(
                        GlobalInstance.getStringResource(R.string.labelSegmentDirection),
                        ((Segment) object).getBearing().relativeToCurrentBearing().getDirection())
                    );
        }
    };


    /**
     * fragment management
     */

    public enum Tab {
        DEPARTURES, DETAILS, ENTRANCES, INTERSECTION_STRUCTURE, PEDESTRIAN_CROSSINGS, STREET_COURSE
    }


	private class TabAdapter extends AbstractTabAdapter {

        public TabAdapter(Fragment fragment, ArrayList<Tab> tabList) {
            super(fragment, tabList);
        }

        @Override public Fragment createFragment(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case DETAILS:
                        if (object instanceof Point) {
                            return PointDetailsFragment.newInstance((Point) object);
                        } else if (object instanceof HikingTrail) {
                            return HikingTrailDetailsFragment.newInstance((HikingTrail) object);
                        } else if (object instanceof Route) {
                            return RouteDetailsFragment.newInstance((Route) object);
                        } else if (object instanceof Segment) {
                            return SegmentDetailsFragment.newInstance((Segment) object);
                        }
                        break;
                    case DEPARTURES:
                        if (object instanceof Station) {
                            Station station = (Station) object;
                            return DeparturesFragment.newInstance(
                                    station.getLatitude(), station.getLongitude());
                        }
                        break;
                    case ENTRANCES:
                        if (object instanceof POI) {
                            POI poi = (POI) object;
                            return EntranceListFragment.newInstance(
                                    poi.getEntranceList());
                        }
                        break;
                    case INTERSECTION_STRUCTURE:
                        if (object instanceof Intersection) {
                            Intersection intersection = (Intersection) object;
                            return IntersectionStructureFragment.newInstance(
                                    intersection.getSegmentList());
                        }
                        break;
                    case PEDESTRIAN_CROSSINGS:
                        if (object instanceof Intersection) {
                            Intersection intersection = (Intersection) object;
                            return PedestrianCrossingListFragment.newInstance(
                                    intersection.getPedestrianCrossingList());
                        }
                        break;
                    case STREET_COURSE:
                        if (object instanceof IntersectionSegment) {
                            IntersectionSegment intersectionSegment = (IntersectionSegment) object;
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
                        if (object instanceof Point) {
                            return getResources().getString(R.string.fragmentPointDetailsName);
                        } else if (object instanceof HikingTrail) {
                            return getResources().getString(R.string.fragmentHikingTrailDetailsName);
                        } else if (object instanceof Route) {
                            return getResources().getString(R.string.fragmentRouteDetailsName);
                        } else if (object instanceof Segment) {
                            return getResources().getString(R.string.fragmentSegmentDetailsName);
                        } else {
                            return "";
                        }
                    case DEPARTURES:
                        return getResources().getString(R.string.fragmentDeparturesName);
                    case ENTRANCES:
                        return getResources().getString(R.string.fragmentEntrancesName);
                    case INTERSECTION_STRUCTURE:
                        return getResources().getString(R.string.fragmentIntersectionStructureName);
                    case PEDESTRIAN_CROSSINGS:
                        return getResources().getString(R.string.fragmentPedestrianCrossingsName);
                    case STREET_COURSE:
                        return getResources().getString(R.string.fragmentStreetCourseDetailsName);
                }
            }
            return "";
        }
    }

}
