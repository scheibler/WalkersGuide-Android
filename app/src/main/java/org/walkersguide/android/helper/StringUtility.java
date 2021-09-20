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
import org.walkersguide.android.util.GlobalInstance;
import org.json.JSONObject;
import org.json.JSONException;
import android.text.TextUtils;


public class StringUtility {

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

    public static String formatInstructionDirection(int direction) {
        if (direction < 0) {
            direction += 360;
        }
        switch (getDirectionConstant(direction)) {
            case Constants.DIRECTION.NORTH:
                return GlobalInstance.getStringResource(R.string.directionCross);
            case Constants.DIRECTION.NORTH_EAST:
                return GlobalInstance.getStringResource(R.string.directionTurnRightSlightly);
            case Constants.DIRECTION.EAST:
                return GlobalInstance.getStringResource(R.string.directionTurnRight);
            case Constants.DIRECTION.SOUTH_EAST:
                return GlobalInstance.getStringResource(R.string.directionTurnRightStrongly);
            case Constants.DIRECTION.SOUTH:
                return GlobalInstance.getStringResource(R.string.directionTurnRound);
            case Constants.DIRECTION.SOUTH_WEST:
                return GlobalInstance.getStringResource(R.string.directionTurnLeftStrongly);
            case Constants.DIRECTION.WEST:
                return GlobalInstance.getStringResource(R.string.directionTurnLeft);
            case Constants.DIRECTION.NORTH_WEST:
                return GlobalInstance.getStringResource(R.string.directionTurnLeftSlightly);
            default:
                return GlobalInstance.getStringResource(R.string.directionStraightforward);
        }
    }

    public static String formatRelativeViewingDirection(int direction) {
        if (direction < 0) {
            direction += 360;
        }
        switch (getDirectionConstant(direction)) {
            case Constants.DIRECTION.NORTH:
                return GlobalInstance.getStringResource(R.string.directionStraightforward);
            case Constants.DIRECTION.NORTH_EAST:
                return GlobalInstance.getStringResource(R.string.directionRightSlightly);
            case Constants.DIRECTION.EAST:
                return GlobalInstance.getStringResource(R.string.directionRight);
            case Constants.DIRECTION.SOUTH_EAST:
                return GlobalInstance.getStringResource(R.string.directionRightStrongly);
            case Constants.DIRECTION.SOUTH:
                return GlobalInstance.getStringResource(R.string.directionBehind);
            case Constants.DIRECTION.SOUTH_WEST:
                return GlobalInstance.getStringResource(R.string.directionLeftStrongly);
            case Constants.DIRECTION.WEST:
                return GlobalInstance.getStringResource(R.string.directionLeft);
            case Constants.DIRECTION.NORTH_WEST:
                return GlobalInstance.getStringResource(R.string.directionLeftSlightly);
            default:
                return GlobalInstance.getStringResource(R.string.directionStraightforward);
        }
    }

    public static String formatGeographicDirection(int direction) {
        if (direction < 0) {
            direction += 360;
        }
        switch (getDirectionConstant(direction)) {
            case Constants.DIRECTION.NORTH:
                return GlobalInstance.getStringResource(R.string.directionNorth);
            case Constants.DIRECTION.NORTH_EAST:
                return GlobalInstance.getStringResource(R.string.directionNorthEast);
            case Constants.DIRECTION.EAST:
                return GlobalInstance.getStringResource(R.string.directionEast);
            case Constants.DIRECTION.SOUTH_EAST:
                return GlobalInstance.getStringResource(R.string.directionSouthEast);
            case Constants.DIRECTION.SOUTH:
                return GlobalInstance.getStringResource(R.string.directionSouth);
            case Constants.DIRECTION.SOUTH_WEST:
                return GlobalInstance.getStringResource(R.string.directionSouthWest);
            case Constants.DIRECTION.WEST:
                return GlobalInstance.getStringResource(R.string.directionWest);
            case Constants.DIRECTION.NORTH_WEST:
                return GlobalInstance.getStringResource(R.string.directionNorthWest);
            default:
                return GlobalInstance.getStringResource(R.string.directionNorth);
        }
    }


    /**
     * format strings
     */

    public static String formatHoursMinutes(long timestamp) {
        SimpleDateFormat hoursMinutesFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return hoursMinutesFormat.format(new Date(timestamp));
    }

    public static SpannableString boldAndRed(String text) {
        return boldAndRed(text, 0, text.length());
    }

    public static SpannableString boldAndRed(String text, int begin, int end) {
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
                    ContextCompat.getColor(GlobalInstance.getContext(), R.color.heading)),
                begin, end, 0);
        return spanString;
    }


    /**
     * json
     */

    public static Float getNullableAndPositiveFloatFromJsonObject(JSONObject jsonObject, String key) {
        Double nullableAndPositiveDouble = getNullableAndPositiveDoubleFromJsonObject(jsonObject, key);
        if (nullableAndPositiveDouble != null) {
            return new Float(nullableAndPositiveDouble);
        }
        return null;
    }

    public static Double getNullableAndPositiveDoubleFromJsonObject(JSONObject jsonObject, String key) {
        if (! jsonObject.isNull(key)) {
            try {
                double doubleFromJson = jsonObject.getDouble(key);
                if (doubleFromJson > 0) {
                    return doubleFromJson;
                }
            } catch (JSONException e) {}
        }
        return null;
    }

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

    public static Long getNullableAndPositiveLongFromJsonObject(JSONObject jsonObject, String key) {
        if (! jsonObject.isNull(key)) {
            try {
                long longFromJson = jsonObject.getLong(key);
                if (longFromJson > 0) {
                    return longFromJson;
                }
            } catch (JSONException e) {}
        }
        return null;
    }

    public static Boolean getNullableBooleanFromJsonObject(JSONObject jsonObject, String key) {
        if (! jsonObject.isNull(key)) {
            try {
                return jsonObject.getBoolean(key);
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


    /**
     * compare objects
     */

    public static boolean compareObjects(Object object1, Object object2) {
        if (object1 == null) {
            return object2 == null;
        }
        return object1.equals(object2);
    }

}
