package org.walkersguide.android.ui.fragment.tabs;

import org.walkersguide.android.ui.view.DistanceAndBearingView;
import androidx.core.view.MenuProvider;
import org.walkersguide.android.database.profile.static_profile.HistoryProfile;


import android.os.Bundle;

import androidx.fragment.app.Fragment;



import java.util.ArrayList;


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
import org.walkersguide.android.sensor.PositionManager;
import timber.log.Timber;
import org.walkersguide.android.data.ObjectWithId;
import android.view.View;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.ui.fragment.tabs.object_details.StreetCourseFragment;
import org.walkersguide.android.ui.fragment.tabs.object_details.RouteDetailsFragment;
import org.walkersguide.android.ui.fragment.tabs.object_details.SegmentDetailsFragment;
import org.walkersguide.android.server.wg.street_course.StreetCourseRequest;
import org.walkersguide.android.ui.fragment.TabLayoutFragment.AbstractTabAdapter;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.lifecycle.Lifecycle;
import org.walkersguide.android.util.SettingsManager;


public class ObjectDetailsTabLayoutFragment extends TabLayoutFragment implements MenuProvider {
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


    private SettingsManager settingsManagerInstance;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsManagerInstance = SettingsManager.getInstance();
    }


    /**
     * layout
     */

    private ObjectWithId object;

    private DistanceAndBearingView labelDistanceAndBearing;

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

            labelDistanceAndBearing = (DistanceAndBearingView) view.findViewById(R.id.labelDistanceAndBearing);
            labelDistanceAndBearing.setVisibility(View.GONE);
        }

        return view;
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
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
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_object_details_tab_layout_fragment, menu);
    }

    @Override public void onPrepareMenu(@NonNull Menu menu) {
        MenuItem menuItemShowPreciseBearingValues = menu.findItem(R.id.menuItemShowPreciseBearingValues);
        if (menuItemShowPreciseBearingValues != null) {
            menuItemShowPreciseBearingValues.setChecked(
                    settingsManagerInstance.getShowPreciseBearingValues());
            menuItemShowPreciseBearingValues.setVisible(
                    object instanceof Point);
        }
    }

    @Override public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemShowPreciseBearingValues) {
            settingsManagerInstance.setShowPreciseBearingValues(
                    ! settingsManagerInstance.getShowPreciseBearingValues());
            if (object != null) {
                // update distance label
                PositionManager.getInstance().requestCurrentLocation();
            }
        } else {
            return false;
        }
        return true;
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
    }

    @Override public void onResume() {
        super.onResume();
        if (object instanceof Point) {
            labelDistanceAndBearing.setObjectWithId(object);
            labelDistanceAndBearing.setVisibility(View.VISIBLE);
        }
    }


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
                        return getResources().getString(R.string.fragmentStreetCourseName);
                }
            }
            return null;
        }

        @Override public String getFragmentDescription(int position) {
            Tab tab = getTab(position);
            if (tab != null) {
                switch (tab) {
                    case STREET_COURSE:
                        if (object instanceof IntersectionSegment) {
                            return (new StreetCourseRequest((IntersectionSegment) object)).getStreetCourseName();
                        }
                }
            }
            return super.getFragmentDescription(position);
        }
    }

}
