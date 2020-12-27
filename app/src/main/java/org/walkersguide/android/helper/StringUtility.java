package org.walkersguide.android.helper;

import androidx.core.content.ContextCompat;
import android.content.Context;

import android.graphics.Typeface;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Locale;

import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;
import org.json.JSONObject;
import org.json.JSONException;
import android.text.TextUtils;


public class StringUtility {

    /**
     * printable point and segment type
     */

    public static String formatPointType(Context context, String type) {
        if (type.equals(Constants.POINT.STATION)) {
            return context.getResources().getString(R.string.pointTypeStation);
        } else if (type.equals(Constants.POINT.ENTRANCE)) {
            return context.getResources().getString(R.string.pointTypeEntrance);
        } else if (type.equals(Constants.POINT.POI)) {
            return context.getResources().getString(R.string.pointTypePOI);
        } else if (type.equals(Constants.POINT.STREET_ADDRESS)) {
            return context.getResources().getString(R.string.pointTypeStreetAddress);
        } else if (type.equals(Constants.POINT.GPS)) {
            return context.getResources().getString(R.string.pointTypeGPS);
        } else if (type.equals(Constants.POINT.INTERSECTION)) {
            return context.getResources().getString(R.string.pointTypeIntersection);
        } else if (type.equals(Constants.POINT.PEDESTRIAN_CROSSING)) {
            return context.getResources().getString(R.string.pointTypePedestrianCrossing);
        } else {
            return context.getResources().getString(R.string.pointTypePoint);
        }
    }


    public static String formatSegmentType(Context context, String type) {
        if (type.equals(Constants.SEGMENT.INTERSECTION)) {
            return context.getResources().getString(R.string.segmentTypeIntersection);
        } else if (type.equals(Constants.SEGMENT.ROUTE)) {
            return context.getResources().getString(R.string.segmentTypeRoute);
        } else {
            return context.getResources().getString(R.string.segmentTypeFootway);
        }
    }


    /**
     * format directions
     */

    public static int getDirectionConstant(int direction) {
        if (direction < 0) {
            direction += 360;
        }
        if ((direction >= 0) && (direction < 23)) {
            return Constants.DIRECTION.NORTH;
        } else if ((direction >= 23) && (direction < 68)) {
            return Constants.DIRECTION.NORTH_EAST;
        } else if ((direction >= 68) && (direction < 113)) {
            return Constants.DIRECTION.EAST;
        } else if ((direction >= 113) && (direction < 158)) {
            return Constants.DIRECTION.SOUTH_EAST;
        } else if ((direction >= 158) && (direction < 203)) {
            return Constants.DIRECTION.SOUTH;
        } else if ((direction >= 203) && (direction < 248)) {
            return Constants.DIRECTION.SOUTH_WEST;
        } else if ((direction >= 248) && (direction < 293)) {
            return Constants.DIRECTION.WEST;
        } else if ((direction >= 293) && (direction < 338)) {
            return Constants.DIRECTION.NORTH_WEST;
        } else {
            return Constants.DIRECTION.NORTH;
        }
    }

    public static String formatInstructionDirection(Context context, int direction) {
        if (direction < 0) {
            direction += 360;
        }
        switch (getDirectionConstant(direction)) {
            case Constants.DIRECTION.NORTH:
                return context.getResources().getString(R.string.directionCross);
            case Constants.DIRECTION.NORTH_EAST:
                return context.getResources().getString(R.string.directionTurnRightSlightly);
            case Constants.DIRECTION.EAST:
                return context.getResources().getString(R.string.directionTurnRight);
            case Constants.DIRECTION.SOUTH_EAST:
                return context.getResources().getString(R.string.directionTurnRightStrongly);
            case Constants.DIRECTION.SOUTH:
                return context.getResources().getString(R.string.directionTurnRound);
            case Constants.DIRECTION.SOUTH_WEST:
                return context.getResources().getString(R.string.directionTurnLeftStrongly);
            case Constants.DIRECTION.WEST:
                return context.getResources().getString(R.string.directionTurnLeft);
            case Constants.DIRECTION.NORTH_WEST:
                return context.getResources().getString(R.string.directionTurnLeftSlightly);
            default:
                return context.getResources().getString(R.string.directionStraightforward);
        }
    }

    public static String formatRelativeViewingDirection(Context context, int direction) {
        if (direction < 0) {
            direction += 360;
        }
        switch (getDirectionConstant(direction)) {
            case Constants.DIRECTION.NORTH:
                return context.getResources().getString(R.string.directionStraightforward);
            case Constants.DIRECTION.NORTH_EAST:
                return context.getResources().getString(R.string.directionRightSlightly);
            case Constants.DIRECTION.EAST:
                return context.getResources().getString(R.string.directionRight);
            case Constants.DIRECTION.SOUTH_EAST:
                return context.getResources().getString(R.string.directionRightStrongly);
            case Constants.DIRECTION.SOUTH:
                return context.getResources().getString(R.string.directionBehind);
            case Constants.DIRECTION.SOUTH_WEST:
                return context.getResources().getString(R.string.directionLeftStrongly);
            case Constants.DIRECTION.WEST:
                return context.getResources().getString(R.string.directionLeft);
            case Constants.DIRECTION.NORTH_WEST:
                return context.getResources().getString(R.string.directionLeftSlightly);
            default:
                return context.getResources().getString(R.string.directionStraightforward);
        }
    }

    public static String formatGeographicDirection(Context context, int direction) {
        if (direction < 0) {
            direction += 360;
        }
        switch (getDirectionConstant(direction)) {
            case Constants.DIRECTION.NORTH:
                return context.getResources().getString(R.string.directionNorth);
            case Constants.DIRECTION.NORTH_EAST:
                return context.getResources().getString(R.string.directionNorthEast);
            case Constants.DIRECTION.EAST:
                return context.getResources().getString(R.string.directionEast);
            case Constants.DIRECTION.SOUTH_EAST:
                return context.getResources().getString(R.string.directionSouthEast);
            case Constants.DIRECTION.SOUTH:
                return context.getResources().getString(R.string.directionSouth);
            case Constants.DIRECTION.SOUTH_WEST:
                return context.getResources().getString(R.string.directionSouthWest);
            case Constants.DIRECTION.WEST:
                return context.getResources().getString(R.string.directionWest);
            case Constants.DIRECTION.NORTH_WEST:
                return context.getResources().getString(R.string.directionNorthWest);
            default:
                return context.getResources().getString(R.string.directionNorth);
        }
    }


    /**
     * poi profile
     */

    public static String formatProfileSortCriteria(Context context, int sortCriteria) {
        switch (sortCriteria) {
            case Constants.SORT_CRITERIA.NAME_ASC:
                return context.getResources().getString(R.string.sortNameAsc);
            case Constants.SORT_CRITERIA.NAME_DESC:
                return context.getResources().getString(R.string.sortNameDesc);
            case Constants.SORT_CRITERIA.DISTANCE_ASC:
                return context.getResources().getString(R.string.sortDistanceAsc);
            case Constants.SORT_CRITERIA.DISTANCE_DESC:
                return context.getResources().getString(R.string.sortDistanceDesc);
            case Constants.SORT_CRITERIA.ORDER_ASC:
                return context.getResources().getString(R.string.sortOrderAsc);
            case Constants.SORT_CRITERIA.ORDER_DESC:
                return context.getResources().getString(R.string.sortOrderDesc);
            default:
                return "";
        }
    }


    /**
     * format strings
     */

    public static String formatHoursMinutes(long timestamp) {
        SimpleDateFormat hoursMinutesFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return hoursMinutesFormat.format(new Date(timestamp));
    }

    public static SpannableString boldAndRed(Context context, String text) {
        return boldAndRed(context, text, 0, text.length());
    }

    public static SpannableString boldAndRed(Context context, String text, int begin, int end) {
        if (begin < 0 || begin >= text.length()) {
            begin = 0;
        }
        if (end < 0 || end >= text.length()) {
            end = text.length();
        }
        SpannableString spanString = new SpannableString(text);
        spanString.setSpan(
                new StyleSpan(
                    Typeface.BOLD),
                begin, end, 0);
        spanString.setSpan(
                new ForegroundColorSpan(
                    ContextCompat.getColor(context, R.color.heading)),
                begin, end, 0);
        return spanString;
    }


    /**
     * json
     */

    public static Integer getNullableAndPositiveIntegerFromJsonObject(JSONObject jsonObject, String key) {
        if (! jsonObject.isNull(key)) {
            try {
                int integerFromJson = jsonObject.getInt(key);
                if (integerFromJson > 0) {
                    return integerFromJson;
                }
            } catch (JSONException e) {}
        }
        return null;
    }

    public static String getNullableStringFromJsonObject(JSONObject jsonObject, String key) {
        if (! jsonObject.isNull(key)) {
            try {
                String stringFromJson = jsonObject.getString(key);
                if (! TextUtils.isEmpty(stringFromJson)) {
                    return stringFromJson;
                }
            } catch (JSONException e) {}
        }
        return null;
    }

}
