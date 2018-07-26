package org.walkersguide.android.util;

public class Constants {

    public interface DEFAULT {
        public static final String SERVER_URL = "https://scheibler-dresden.homelinux.org:19000";
        public static final int SUPPORTED_MAP_VERSION = 6;
    }

    public interface DUMMY {
        public static final int DIRECTION = 0;
        public static final String LOCATION =
            "{\"name\":\"Dummy location\", \"lat\":0.0, \"lon\":0.0, \"type\":\"gps\", \"sub_type\":\"Dummy Location\"}";
        public static final String FOOTWAY =
            "{\"name\":\"Dummy Segment\", \"bearing\":0, \"distance\":0, \"type\":\"footway\", \"sub_type\":\"Dummy Segment\"}";
    }

    public interface ID {
        public static final int OK = 200;
        public static final int CANCELLED = 1000;
        public static final int NO_MAP_SELECTED = 1006;
    }

    public interface POINT_SELECT_FROM {
        public static final int CURRENT_LOCATION = 0;
        public static final int ENTER_ADDRESS = 1;
        public static final int FROM_FAVORITES = 2;
        public static final int FROM_POI = 3;
    }

    public final static int[] PointSelectFromValueArray = {
        POINT_SELECT_FROM.CURRENT_LOCATION, POINT_SELECT_FROM.ENTER_ADDRESS,
        POINT_SELECT_FROM.FROM_FAVORITES, POINT_SELECT_FROM.FROM_POI
    };

    public final static int[] PointSelectFromValueArrayWithoutCurrentLocation = {
        POINT_SELECT_FROM.ENTER_ADDRESS, POINT_SELECT_FROM.FROM_FAVORITES, POINT_SELECT_FROM.FROM_POI
    };

    public interface POINT_PUT_INTO {
        public static final int START = 0;
        public static final int DESTINATION = 1;
        public static final int SIMULATION = 2;
        public static final int VIA = 10;
    }

    public interface POINT {
        public static final String POINT = "point";
        public static final String ENTRANCE = "entrance";
        public static final String GPS = "gps";
        public static final String INTERSECTION = "intersection";
        public static final String PEDESTRIAN_CROSSING = "pedestrian_crossing";
        public static final String POI = "poi";
        public static final String STATION = "station";
        public static final String STREET_ADDRESS = "street_address";
    }

    public interface SEGMENT {
        public static final String FOOTWAY = "footway";
        public static final String INTERSECTION = "footway_intersection";
        public static final String ROUTE = "footway_route";
    }

    public interface SORT_CRITERIA {
        public static final int NAME_ASC = 0;
        public static final int NAME_DESC = 1;
        public static final int DISTANCE_ASC = 2;
        public static final int DISTANCE_DESC = 3;
        public static final int ORDER_ASC = 4;
        public static final int ORDER_DESC = 5;
    }

    public final static int[] FavoritesProfileSortCriteriaValueArray = {
        SORT_CRITERIA.NAME_ASC, SORT_CRITERIA.NAME_DESC, SORT_CRITERIA.DISTANCE_ASC,
        SORT_CRITERIA.DISTANCE_DESC, SORT_CRITERIA.ORDER_ASC, SORT_CRITERIA.ORDER_DESC
    };

    public final static int[] SearchFavoritesProfileSortCriteriaValueArray = {
        SORT_CRITERIA.NAME_ASC, SORT_CRITERIA.NAME_DESC, SORT_CRITERIA.DISTANCE_ASC, SORT_CRITERIA.DISTANCE_DESC
    };


    /**
     * fragments, used in activities
     *
     * Fragments: MainActivity
     */

    public interface MAIN_FRAGMENT {
        public static final int FAVORITE = 0;
        public static final int ROUTER = 1;
        public static final int POI = 2;
    }

    public final static int[] MainActivityFragmentValueArray = {
        MAIN_FRAGMENT.FAVORITE, MAIN_FRAGMENT.ROUTER, MAIN_FRAGMENT.POI
    };


    /**
     * Fragments: PointDetailsActivity
     */

    public interface POINT_DETAILS_ACTIVITY_EXTRA {
        public static final String JSON_POINT_SERIALIZED = "jsonPointSerialized";
    }

    public interface POINT_FRAGMENT {
        public static final int DETAILS = 0;
    }

    public final static int[] PointFragmentValueArray = {
        POINT_FRAGMENT.DETAILS
    };

    public interface INTERSECTION_FRAGMENT {
        public static final int INTERSECTION_WAYS = 0;
        public static final int DETAILS = 1;
        public static final int PEDESTRIAN_CROSSINGS = 2;
    }

    public final static int[] IntersectionFragmentValueArray = {
        INTERSECTION_FRAGMENT.INTERSECTION_WAYS, INTERSECTION_FRAGMENT.DETAILS, INTERSECTION_FRAGMENT.PEDESTRIAN_CROSSINGS
    };

    public interface POI_FRAGMENT {
        public static final int DETAILS = 0;
        public static final int ENTRANCES = 1;
    }

    public final static int[] POIFragmentValueArray = {
        POI_FRAGMENT.DETAILS, POI_FRAGMENT.ENTRANCES
    };

    public interface STATION_FRAGMENT {
        public static final int DEPARTURES = 0;
        public static final int DETAILS = 1;
        public static final int ENTRANCES = 2;
    }

    public final static int[] StationFragmentValueArray = {
        STATION_FRAGMENT.DEPARTURES, STATION_FRAGMENT.DETAILS, STATION_FRAGMENT.ENTRANCES
    };

    /**
     * Fragments: SegmentDetailsActivity
     */

    public interface SEGMENT_DETAILS_ACTIVITY_EXTRA {
        public static final String JSON_SEGMENT_SERIALIZED = "jsonSegmentSerialized";
    }

    public interface SEGMENT_FRAGMENT {
        public static final int DETAILS = 0;
    }

    public final static int[] SegmentFragmentValueArray = {
        SEGMENT_FRAGMENT.DETAILS
    };

    public interface INTERSECTION_SEGMENT_FRAGMENT {
        public static final int DETAILS = 0;
        public static final int NEXT_INTERSECTIONS = 1;
    }

    public final static int[] IntersectionSegmentFragmentValueArray = {
        INTERSECTION_SEGMENT_FRAGMENT.DETAILS, INTERSECTION_SEGMENT_FRAGMENT.NEXT_INTERSECTIONS
    };


    /**
     * Direction manager
     */

    public interface DIRECTION {
        public static final int NORTH_WEST = 0;
        public static final int NORTH = 1;
        public static final int NORTH_EAST = 2;
        public static final int EAST = 3;
        public static final int SOUTH_EAST = 4;
        public static final int SOUTH = 5;
        public static final int SOUTH_WEST = 6;
        public static final int WEST = 7;
    }

    public interface DIRECTION_SOURCE {
        public static final int COMPASS = 1;
        public static final int GPS = 2;
        public static final int SIMULATION = 3;
    }

    public final static int[] DirectionSourceValueArray = {
        DIRECTION_SOURCE.COMPASS, DIRECTION_SOURCE.GPS, DIRECTION_SOURCE.SIMULATION
    };

    public interface SHAKE_INTENSITY {
        public static final int VERY_WEAK = 100;
        public static final int WEAK = 400;
        public static final int MEDIUM = 700;
        public static final int STRONG = 1000;
        public static final int VERY_STRONG = 1300;
        public static final int DISABLED = 1000000000;
    }

    public final static int[] ShakeIntensityValueArray = {
        SHAKE_INTENSITY.VERY_WEAK, SHAKE_INTENSITY.WEAK, SHAKE_INTENSITY.MEDIUM,
        SHAKE_INTENSITY.STRONG, SHAKE_INTENSITY.VERY_STRONG, SHAKE_INTENSITY.DISABLED
    };


    /**
     * position manager
     */

    public interface LOCATION_SOURCE {
        public static final int GPS = 0;
        public static final int SIMULATION = 1;
    }

    public final static int[] LocationSourceValueArray = {
        LOCATION_SOURCE.GPS, LOCATION_SOURCE.SIMULATION
    };


    /**
     * route
     */

    public final static double[] IndirectionFactorValueArray = {
        1.0, 1.5, 2.0, 3.0, 4.0
    };

    public interface ROUTING_WAY_CLASS {
        public static final String BIG_STREETS = "big_streets";
        public static final String SMALL_STREETS = "small_streets";
        public static final String PAVED_WAYS = "paved_ways";
        public static final String UNPAVED_WAYS = "unpaved_ways";
        public static final String STEPS = "steps";
        public static final String UNCLASSIFIED_WAYS = "unclassified_ways";
    }

    public final static String[] RoutingWayClassValueArray = {
        ROUTING_WAY_CLASS.BIG_STREETS, ROUTING_WAY_CLASS.SMALL_STREETS, ROUTING_WAY_CLASS.PAVED_WAYS,
        ROUTING_WAY_CLASS.UNPAVED_WAYS, ROUTING_WAY_CLASS.STEPS, ROUTING_WAY_CLASS.UNCLASSIFIED_WAYS
    };


    /**
     * actions
     */

    /** update ui **/

    public static final String ACTION_UPDATE_UI = "update_ui";

    /** server status **/

    public static final String ACTION_SERVER_STATUS_UPDATED = "server_status_updated";
    public interface ACTION_SERVER_STATUS_UPDATED_ATTR {
        // return code
        public static final String INT_RETURN_CODE = "return_code";
        // return message
        public static final String STRING_RETURN_MESSAGE = "return_message";
    }

    /** direction **/

    public static final String ACTION_NEW_DIRECTION = "new_direction";
    public interface ACTION_NEW_DIRECTION_ATTR {
        public static final String INT_DIRECTION_IN_DEGREE = "direction_in_degree";
        // direction source: compass, GPS or fixed geographic direction
        public static final String INT_SOURCE = "direction_source";
        // direction threshold id (see DirectionManager.THRESHOLDX interfaces
        public static final String INT_THRESHOLD_ID = "direction_threshold_id";
    }

    public static final String ACTION_NEW_COMPASS_DIRECTION = "new_compass_direction";
    public interface ACTION_NEW_COMPASS_DIRECTION_ATTR {
        public static final String INT_DIRECTION_IN_DEGREE = "direction_in_degree";
    }

    public static final String ACTION_NEW_GPS_DIRECTION = "new_gps_direction";
    public interface ACTION_NEW_GPS_DIRECTION_ATTR {
        public static final String INT_DIRECTION_IN_DEGREE = "direction_in_degree";
    }

    public static final String ACTION_NEW_SIMULATED_DIRECTION = "new_simulated_direction";
    public interface ACTION_NEW_SIMULATED_DIRECTION_ATTR {
        public static final String INT_DIRECTION_IN_DEGREE = "direction_in_degree";
    }

    /** location **/

    public static final String ACTION_NEW_LOCATION = "new_location";
    public interface ACTION_NEW_LOCATION_ATTR {
        public static final String STRING_POINT_SERIALIZED = "jsonPointSerialized";
        // location source: GPS or simulation
        public static final String INT_SOURCE = "location_source";
        // location threshold id (see PositionManager.THRESHOLDX interfaces
        public static final String INT_THRESHOLD_ID = "location_threshold_id";
        // traveling at high speed
        public static final String BOOL_AT_HIGH_SPEED = "at_high_speed";
    }

    public static final String ACTION_NEW_GPS_LOCATION = "new_gps_location";
    public interface ACTION_NEW_GPS_LOCATION_ATTR {
        public static final String STRING_POINT_SERIALIZED = "jsonPointSerialized";
    }

    public static final String ACTION_NEW_SIMULATED_LOCATION = "new_simulated_location";
    public interface ACTION_NEW_SIMULATED_LOCATION_ATTR {
        public static final String STRING_POINT_SERIALIZED = "jsonPointSerialized";
    }

    /** shake detection **/

    public static final String ACTION_SHAKE_DETECTED = "shake_detected";
    public interface ACTION_SHAKE_DETECTED_ATTR {
    }

}
