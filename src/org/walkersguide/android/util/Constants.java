package org.walkersguide.android.util;

public class Constants {

    public interface ID {
        public static final int OK = 200;
    }

    public interface SPEED {
        public static final float HIGH = 5.0f;
    }

    public interface TYPE {
        public static final String POINT = "point";
        public static final String GPS = "gps";
        public static final String INTERSECTION = "intersection";
        public static final String POI = "poi";
        public static final String STATION = "station";
    }


    /**
     * fragments, used in main activity
     */

    public interface FRAGMENT {
        public static final int SEARCH = 0;
        public static final int FAVORITE = 1;
        public static final int ROUTER = 2;
        public static final int POI = 3;
        public static final int LOCATION_AND_DIRECTION = 4;
    }

    public final static int[] MainActivityFragmentValueArray = {
        FRAGMENT.SEARCH, FRAGMENT.FAVORITE, FRAGMENT.ROUTER,
        FRAGMENT.POI, FRAGMENT.LOCATION_AND_DIRECTION
    };


    /**
     * Direction manager
     */

    public interface DIRECTION_SOURCE {
        public static final int COMPASS = 1;
        public static final int GPS = 2;
        public static final int MANUAL = 3;
    }

    public final static int[] DirectionSourceValueArray = {
        DIRECTION_SOURCE.COMPASS, DIRECTION_SOURCE.GPS, DIRECTION_SOURCE.MANUAL
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
     * actions
     */

    public static final String ACTION_NEW_DIRECTION = "new_direction";
    public interface ACTION_NEW_DIRECTION_ATTR {
        // update thresholds
        //  0: every new direction
        //  1: threshold bigger than 22 degree
        public static final String INT_UPDATE_THRESHOLD = "update_direction_threshold";
    }

    public static final String ACTION_NEW_COMPASS_DIRECTION = "new_compass_direction";
    public interface ACTION_NEW_COMPASS_DIRECTION_ATTR {
    }

    public static final String ACTION_NEW_LOCATION = "new_location";
    public interface ACTION_NEW_LOCATION_ATTR {
        // update thresholds
        //  0: every new location
        //  1: every 50 meters
        public static final String INT_UPDATE_THRESHOLD = "update_location_threshold";
        // traveling at high speed
        //  0: no
        //  1: yes
        public static final String INT_AT_HIGH_SPEED = "at_high_speed";
    }

    public static final String ACTION_NEW_GPS_POSITION = "new_gps_position";
    public interface ACTION_NEW_GPS_POSITION_ATTR {
    }

    public static final String ACTION_POI_PROFILE_UPDATED = "poi_profile_updated";
    public interface ACTION_POI_PROFILE_UPDATED_ATTR {
        // poi profile id
        public static final String INT_POI_PROFILE_ID = "poi_profile_id";
        // return code
        public static final String INT_RETURN_CODE = "return_code";
        // return message
        public static final String STRING_RETURN_MESSAGE = "return_message";
    }

    public static final String ACTION_SHAKE_DETECTED = "shake_detected";
    public interface ACTION_SHAKE_DETECTED_ATTR {
    }

}
