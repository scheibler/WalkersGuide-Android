package org.walkersguide.android.util;


public class Constants {

    public interface DUMMY {
        public static final String FOOTWAY =
            "{\"name\":\"Dummy Segment\", \"bearing\":0, \"distance\":0, \"type\":\"footway\", \"sub_type\":\"Dummy Segment\"}";
    }

    public interface SERVER_COMMAND {
        public static final String GET_ROUTE = "get_route";
        public static final String GET_NEXT_INTERSECTIONS_FOR_WAY = "get_next_intersections_for_way";
        public static final String GET_POI = "get_poi";
        public static final String GET_HIKING_TRAILS = "get_hiking_trails";
        public static final String SEND_FEEDBACK = "send_feedback";
        public static final String CANCEL_REQUEST = "cancel_request";
        public static final String GET_STATUS = "get_status";
    }

    public interface RC {
        // return codes from walkersguide server
        //
        // errors caused by client request
        public static final int OK = 200;
        public static final int BAD_REQUEST = 400;
        public static final int REQUEST_IN_PROGRESS = 429;
        // errors caused by the server
        public static final int INTERNAL_SERVER_ERROR = 500;
        public static final int BAD_GATEWAY = 502;
        public static final int SERVICE_UNAVAILABLE = 503;
        // walkersguide specific errors
        public static final int CANCELLED_BY_CLIENT = 550;
        // map
        public static final int MAP_LOADING_FAILED = 555;
        public static final int WRONG_MAP_SELECTED = 556;
        public static final int MAP_OUTDATED = 557;
        // poi
        public static final int NO_POI_TAGS_SELECTED = 560;
        // route calculation
        public static final int START_OR_DESTINATION_MISSING = 570;
        public static final int START_AND_DESTINATION_TOO_FAR_AWAY = 571;
        public static final int TOO_MANY_WAY_CLASSES_IGNORED = 572;
        public static final int NO_ROUTE_BETWEEN_START_AND_DESTINATION = 573;

        // local return codes thrown by the app
        //
        // general
        public static final int CANCELLED = 1000;
        public static final int NO_LOCATION_FOUND = 1001;
        public static final int NO_DIRECTION_FOUND = 1002;

        // client side server connection errors
        public static final int CONNECTION_FAILED = 1010;
        public static final int BAD_RESPONSE = 1011;
        public static final int NO_SERVER_URL = 1012;
        public static final int NO_INTERNET_CONNECTION = 1013;
        public static final int API_CLIENT_OUTDATED = 1014;
        public static final int API_SERVER_OUTDATED = 1015;
        public static final int NO_MAP_LIST = 1016;

        // address manager
        public static final int NO_COORDINATES_FOR_ADDRESS = 1050;
        public static final int NO_ADDRESS_FOR_COORDINATES = 1051;
        public static final int NEITHER_COORDINATES_NOR_ADDRESS = 1052;
        public static final int GOOGLE_MAPS_QUOTA_EXCEEDED = 1053;
        public static final int ADDRESS_PROVIDER_NOT_SUPPORTED = 1054;

        // poi manager
        public static final int NO_POI_PROFILE_CREATED = 1060;
        public static final int NO_POI_PROFILE_SELECTED = 1061;
        public static final int POI_PROFILE_PARSING_ERROR = 1062;
        public static final int UNSUPPORTED_POI_REQUEST_ACTION = 1063;

        // route manager
        public static final int NO_ROUTE_CREATED = 1070;
        public static final int NO_ROUTE_SELECTED = 1071;
        public static final int ROUTE_PARSING_ERROR = 1072;
    }

    public interface POINT_SELECT_FROM {
        public static final int CURRENT_LOCATION = 0;
        public static final int ENTER_ADDRESS = 1;
        public static final int ENTER_COORDINATES = 2;
        public static final int FROM_HISTORY_POINTS = 3;
        public static final int FROM_POI = 4;
    }

    public final static int[] PointSelectFromValueArray = {
        POINT_SELECT_FROM.CURRENT_LOCATION, POINT_SELECT_FROM.ENTER_ADDRESS, POINT_SELECT_FROM.ENTER_COORDINATES,
        POINT_SELECT_FROM.FROM_HISTORY_POINTS, POINT_SELECT_FROM.FROM_POI
    };

    public final static int[] PointSelectFromValueArrayWithoutCurrentLocation = {
        POINT_SELECT_FROM.ENTER_ADDRESS, POINT_SELECT_FROM.ENTER_COORDINATES,
        POINT_SELECT_FROM.FROM_HISTORY_POINTS, POINT_SELECT_FROM.FROM_POI
    };

    public interface POINT_PUT_INTO {
        public static final int NOWHERE = -1;
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
        public static final int NONE = -1;
        public static final int NAME_ASC = 0;
        public static final int NAME_DESC = 1;
        public static final int DISTANCE_ASC = 2;
        public static final int DISTANCE_DESC = 3;
        public static final int ORDER_ASC = 4;
        public static final int ORDER_DESC = 5;
    }


    /**
     * fragments, used in activities
     *
     * Fragments: MainActivity
     */

    public interface MAIN_FRAGMENT {
        public static final int ROUTER = 0;
        public static final int POI = 1;
    }

    public final static int[] MainActivityFragmentValueArray = {
        MAIN_FRAGMENT.ROUTER, MAIN_FRAGMENT.POI
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
     * AddressManager
     */

    public interface ADDRESS_PROVIDER {
        public static final String OSM = "osm";
        public static final String GOOGLE = "google";
    }

    public final static String[] AddressProviderValueArray = {
        ADDRESS_PROVIDER.OSM
    };


    /**
     * Direction and Direction manager
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
    }

    public final static int[] DirectionSourceValueArray = {
        DIRECTION_SOURCE.COMPASS, DIRECTION_SOURCE.GPS
    };

    public interface DIRECTION_ACCURACY_RATING {
        public static final int UNKNOWN = 0;
        public static final int LOW = 1;
        public static final int MEDIUM = 2;
        public static final int HIGH = 3;
    }

    public final static int[] DirectionAccuracyRatingValueArray = {
        DIRECTION_ACCURACY_RATING.LOW, DIRECTION_ACCURACY_RATING.MEDIUM, DIRECTION_ACCURACY_RATING.HIGH
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
     * POI
     */

    public interface POI_CATEGORY {
        public static final String TRANSPORT_BUS_TRAM = "transport_bus_tram";
        public static final String TRANSPORT_TRAIN_LIGHTRAIL_SUBWAY = "transport_train_lightrail_subway";
        public static final String TRANSPORT_AIRPORT_FERRY_AERIALWAY = "transport_airport_ferry_aerialway";
        public static final String TRANSPORT_TAXI = "transport_taxi";
        public static final String FOOD = "food";
        public static final String TOURISM = "tourism";
        public static final String NATURE = "nature";
        public static final String SHOP = "shop";
        public static final String EDUCATION = "education";
        public static final String HEALTH = "health";
        public static final String ENTERTAINMENT = "entertainment";
        public static final String FINANCE = "finance";
        public static final String PUBLIC_SERVICE = "public_service";
        public static final String ALL_BUILDINGS_WITH_NAME = "all_buildings_with_name";
        public static final String ENTRANCE = "entrance";
        public static final String SURVEILLANCE = "surveillance";
        public static final String BRIDGE = "bridge";
        public static final String BENCH = "bench";
        public static final String TRASH = "trash";
        public static final String NAMED_INTERSECTION = "named_intersection";
        public static final String OTHER_INTERSECTION = "other_intersection";
        public static final String PEDESTRIAN_CROSSINGS = "pedestrian_crossings";
    }


    /**
     * public transport provider
     */

    public interface PUBLIC_TRANSPORT_PROVIDER {
        public static final String DB = "DB";
        public static final String RT = "RT";
        public static final String VVO = "VVO";
    }


    /**
     * route
     */

    public interface ROUTING_WAY_CLASS_ID {
        public static final String BIG_STREETS = "big_streets";
        public static final String SMALL_STREETS = "small_streets";
        public static final String PAVED_WAYS = "paved_ways";
        public static final String UNPAVED_WAYS = "unpaved_ways";
        public static final String STEPS = "steps";
        public static final String UNCLASSIFIED_WAYS = "unclassified_ways";
    }

    public final static String[] RoutingWayClassIdValueArray = {
        ROUTING_WAY_CLASS_ID.BIG_STREETS, ROUTING_WAY_CLASS_ID.SMALL_STREETS,
        ROUTING_WAY_CLASS_ID.PAVED_WAYS, ROUTING_WAY_CLASS_ID.UNPAVED_WAYS,
        ROUTING_WAY_CLASS_ID.STEPS, ROUTING_WAY_CLASS_ID.UNCLASSIFIED_WAYS
    };

    public interface ROUTING_WAY_CLASS_WEIGHT {
        public static final double VERY_PREFERABLE = 0.25;
        public static final double PREFERABLE = 0.5;
        public static final double SLIGHTLY_PREFERABLE= 0.75;
        public static final double NEUTRAL = 1.0;
        public static final double SLIGHTLY_NEGLIGIBLE = 1.33;
        public static final double NEGLIGIBLE = 2.0;
        public static final double VERY_NEGLIGIBLE = 4.0;
        public static final double IGNORE = -1.0;
    }

    public final static double[] RoutingWayClassWeightValueArray = {
        ROUTING_WAY_CLASS_WEIGHT.VERY_PREFERABLE, ROUTING_WAY_CLASS_WEIGHT.PREFERABLE,
        ROUTING_WAY_CLASS_WEIGHT.SLIGHTLY_PREFERABLE, ROUTING_WAY_CLASS_WEIGHT.NEUTRAL,
        ROUTING_WAY_CLASS_WEIGHT.SLIGHTLY_NEGLIGIBLE, ROUTING_WAY_CLASS_WEIGHT.NEGLIGIBLE,
        ROUTING_WAY_CLASS_WEIGHT.VERY_NEGLIGIBLE, ROUTING_WAY_CLASS_WEIGHT.IGNORE
    };


    /**
     * actions
     */

    /** update ui **/

    public static final String ACTION_UPDATE_UI = "update_ui";

    /** history point profile changes **/

    public static final String ACTION_HISTORY_POINT_PROFILE_SELECTED = "history_point_profile_selected";

    /** poi profile changes **/

    public static final String ACTION_POI_PROFILE_CREATED = "poi_profile_created";
    public static final String ACTION_POI_PROFILE_MODIFIED = "poi_profile_modified";
    public static final String ACTION_POI_PROFILE_REMOVED = "poi_profile_removed";
    public static final String ACTION_POI_PROFILE_SELECTED = "poi_profile_selected";

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
    public static final String ACTION_NEW_DIRECTION_ATTRIBUTES = "new_direction_attributes_in_json_format";

    public static final String ACTION_NEW_COMPASS_DIRECTION = "new_compass_direction";
    public static final String ACTION_NEW_COMPASS_DIRECTION_OBJECT = "new_compass_direction_object_in_json_format";

    public static final String ACTION_NEW_GPS_DIRECTION = "new_gps_direction";
    public static final String ACTION_NEW_GPS_DIRECTION_OBJECT = "new_gps_direction_object_in_json_format";

    public static final String ACTION_NEW_SIMULATED_DIRECTION = "new_simulated_direction";
    public static final String ACTION_NEW_SIMULATED_DIRECTION_OBJECT = "new_simulated_direction_object_in_json_format";

    /** location **/

    public static final String ACTION_LOCATION_PROVIDER_DISABLED = "location_provider_disabled";

    public static final String ACTION_LOCATION_PERMISSION_DENIED = "location_permission_denied";

    public static final String ACTION_NEW_LOCATION = "new_location";
    public static final String ACTION_NEW_LOCATION_ATTRIBUTES = "new_location_attributes_in_json_format";

    public static final String ACTION_NEW_GPS_LOCATION = "new_gps_location";
    public static final String ACTION_NEW_GPS_LOCATION_OBJECT = "new_gps_location_object_in_json_format";

    public static final String ACTION_NEW_SIMULATED_LOCATION = "new_simulated_location";
    public static final String ACTION_NEW_SIMULATED_LOCATION_OBJECT = "new_simulated_location_object_in_json_format";

    /** shake detection **/

    public static final String ACTION_SHAKE_DETECTED = "shake_detected";
    public interface ACTION_SHAKE_DETECTED_ATTR {
    }

}
