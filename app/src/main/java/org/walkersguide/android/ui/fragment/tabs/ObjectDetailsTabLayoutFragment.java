package org.walkersguide.android.ui.fragment.tabs;

import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.database.profile.static_profile.HistoryProfile;
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
import org.walkersguide.android.ui.fragment.tabs.object_details.PointDetailsFragment;
import org.walkersguide.android.ui.fragment.pt.DeparturesFragment;
import org.walkersguide.android.ui.fragment.object_list.simple.EntranceListFragment;
import org.walkersguide.android.ui.fragment.object_list.simple.IntersectionStructureFragment;
import org.walkersguide.android.ui.fragment.object_list.simple.PedestrianCrossingListFragment;
import org.walkersguide.android.data.object_with_id.Point;
    import org.walkersguide.android.ui.view.ObjectWithIdView;
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
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.ui.fragment.tabs.object_details.StreetCourseFragment;
import org.walkersguide.android.ui.fragment.tabs.object_details.RouteDetailsFragment;
import org.walkersguide.android.ui.fragment.tabs.object_details.SegmentDetailsFragment;
import org.walkersguide.android.server.wg.street_course.StreetCourseRequest;
import org.walkersguide.android.ui.fragment.TabLayoutFragment.AbstractTabAdapter;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import androidx.fragment.app.DialogFragment;
import android.text.TextUtils;
import org.walkersguide.android.ui.UiHelper;
import androidx.annotation.Nullable;


public class ObjectDetailsTabLayoutFragment extends TabLayoutFragment {
    private static final String KEY_OBJECT = "object";

    public static ObjectDetailsTabLayoutFragment details(ObjectWithId object) {
        return newInstance(object, Tab.DETAILS);
    }

    public static ObjectDetailsTabLayoutFragment departures(Station station) {
        return newInstance(station, Tab.DEPARTURES);
    }

    public static ObjectDetailsTabLayoutFragment entrances(POI poi) {
        return newInstance(poi, Tab.ENTRANCES);
    }

    public static ObjectDetailsTabLayoutFragment pedestrianCrossings(Intersection intersection) {
        return newInstance(intersection, Tab.PEDESTRIAN_CROSSINGS);
    }

    public static ObjectDetailsTabLayoutFragment streetCourse(IntersectionSegment intersectionSegment) {
        return newInstance(intersectionSegment, Tab.STREET_COURSE);
    }

    private static ObjectDetailsTabLayoutFragment newInstance(ObjectWithId object, Tab selectedTab) {
        // add object to history
        if (object instanceof Intersection) {
            HistoryProfile.intersectionPoints().addObject((Intersection) object);
        } else if (object instanceof Station) {
            HistoryProfile.stationPoints().addObject((Station) object);
        } else if (object instanceof StreetAddress) {
            HistoryProfile.addressPoints().addObject((StreetAddress) object);
        } else if (object instanceof Point) {
            HistoryProfile.allPoints().addObject((Point) object);
        } else if (object instanceof Route) {
            HistoryProfile.allRoutes().addObject((Route) object);
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

    private TextView labelDistanceAndBearing;

    @Override public int getLayoutResourceId() {
        return R.layout.fragment_object_details;
    }

    @Override public View configureView(View view, Bundle savedInstanceState) {
        view = super.configureView(view, savedInstanceState);

        // load object
        object = (ObjectWithId) getArguments().getSerializable(KEY_OBJECT);
        Timber.d("configureView: object=%1$s", object);
        if (object != null) {

            ObjectWithIdView layoutObject = (ObjectWithIdView) view.findViewById(R.id.layoutObject);
            layoutObject.setOnDefaultObjectActionListener(new ObjectWithIdView.OnDefaultObjectActionListener() {
            @Override public void onDefaultObjectActionClicked(ObjectWithIdView view, View subView, ObjectWithId objectWithId) {
                    if (objectWithId != null) {
                        view.showContextMenu(subView, objectWithId);
                    }
                }
            }, false);
            layoutObject.configureAsSingleObject(object);

            labelDistanceAndBearing = (TextView) view.findViewById(R.id.labelDistanceAndBearing);
            labelDistanceAndBearing.setVisibility(View.GONE);
        }

        return view;
    }

    @Override public void onStart() {
        super.onStart();

        if (object != null) {
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
                if (((Intersection) object).hasPedestrianCrossings()) {
                    tabList.add(Tab.PEDESTRIAN_CROSSINGS);
                }
            }
            if (object instanceof IntersectionSegment) {
                tabList.add(Tab.STREET_COURSE);
            }

            initializeViewPagerAndTabLayout(new TabAdapter(tabList));
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
        Timber.d("onResume: object=%1$s", object);

        if (object instanceof Point) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(PositionManager.ACTION_NEW_LOCATION);
            filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(newLocationAndDirectionReceiverForPoints, filter);
            // request current location to update the ui
            labelDistanceAndBearing.setVisibility(View.VISIBLE);
            acceptNewPositionForTtsAnnouncement = AcceptNewPosition.newInstanceForTtsAnnouncement();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override public void run() {
                    Timber.d("runnable get current location");
                    // wait, until onResume is finished and the ui has focus
                    PositionManager.getInstance().requestCurrentLocation();
                }
            }, 200);

        } else if (object instanceof Segment) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(newDirectionReceiverForSegments, filter);
            // request current direction to update the ui
            labelDistanceAndBearing.setVisibility(View.VISIBLE);
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
            if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                boolean announceViaTts = false;
                if (acceptNewPositionForDistanceLabel.updatePoint(
                            (Point) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION),
                            UiHelper.isInBackground(ObjectDetailsTabLayoutFragment.this),
                            intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false))) {
                    updateDistanceAndBearingLabel();
                    announceViaTts = labelDistanceAndBearing.isAccessibilityFocused();
                }
                if (acceptNewPositionForTtsAnnouncement.updatePoint(
                            (Point) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION), false, false)) {
                    if (! UiHelper.isInBackground(ObjectDetailsTabLayoutFragment.this)) {
                        announceViaTts = true;
                    }
                }
                if (announceViaTts) {
                    ttsWrapperInstance.announce(
                            ((Point) object).formatDistanceAndRelativeBearingFromCurrentLocation(R.plurals.meter));
                }

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                if (acceptNewBearing.updateBearing(
                            (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING),
                            UiHelper.isInBackground(ObjectDetailsTabLayoutFragment.this),
                            intent.getBooleanExtra(DeviceSensorManager.EXTRA_IS_IMPORTANT, false))) {
                    updateDistanceAndBearingLabel();
                }
            }
        }

        private void updateDistanceAndBearingLabel() {
            labelDistanceAndBearing.setText(
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
                if (acceptNewBearing.updateBearing(
                            (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING),
                            UiHelper.isInBackground(ObjectDetailsTabLayoutFragment.this),
                            intent.getBooleanExtra(DeviceSensorManager.EXTRA_IS_IMPORTANT, false))) {
                    updateDirectionLabel();
                }
            }
        }

        private void updateDirectionLabel() {
            labelDistanceAndBearing.setText(
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

        public TabAdapter(ArrayList<Tab> tabList) {
            super(tabList);
        }

        @Override public Enum<?> getDefaultTab() {
            return Tab.DETAILS;
        }

        @Override public Fragment getFragment(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case DETAILS:
                        if (object instanceof Point) {
                            return PointDetailsFragment.newInstance((Point) object);
                        } else if (object instanceof Route) {
                            return RouteDetailsFragment.newInstance((Route) object);
                        } else if (object instanceof Segment) {
                            return SegmentDetailsFragment.newInstance((Segment) object);
                        }
                        break;
                    case DEPARTURES:
                        if (object instanceof Station) {
                            Station station = (Station) object;
                            return DeparturesFragment.embedded(
                                    station.getId(),
                                    station.getCoordinates().getLatitude(),
                                    station.getCoordinates().getLongitude());
                        }
                        break;
                    case ENTRANCES:
                        if (object instanceof POI) {
                            POI poi = (POI) object;
                            return EntranceListFragment.embedded(
                                    poi.getEntranceList());
                        }
                        break;
                    case INTERSECTION_STRUCTURE:
                        if (object instanceof Intersection) {
                            return IntersectionStructureFragment.embedded((Intersection) object);
                        }
                        break;
                    case PEDESTRIAN_CROSSINGS:
                        if (object instanceof Intersection) {
                            Intersection intersection = (Intersection) object;
                            return PedestrianCrossingListFragment.embedded(
                                    intersection.getPedestrianCrossingList());
                        }
                        break;
                    case STREET_COURSE:
                        if (object instanceof IntersectionSegment) {
                            return StreetCourseFragment.newInstance(
                                    new StreetCourseRequest((IntersectionSegment) object));
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
                        return object instanceof IntersectionSegment
                            ? (new StreetCourseRequest((IntersectionSegment) object)).getStreetCourseName()
                            : getResources().getString(R.string.fragmentStreetCourseName);
                }
            }
            return null;
        }
    }

}
