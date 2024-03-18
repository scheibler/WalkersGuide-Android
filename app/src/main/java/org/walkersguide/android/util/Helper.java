package org.walkersguide.android.util;

import org.walkersguide.android.R;
import org.walkersguide.android.data.object_with_id.common.Coordinates;
import timber.log.Timber;

import org.json.JSONObject;
import org.json.JSONException;
import android.text.TextUtils;
import android.os.Vibrator;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.VibrationEffect;
import android.content.Context;
import java.util.ArrayList;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.Turn;
import org.walkersguide.android.data.Angle;
import androidx.core.util.Pair;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.walkersguide.android.data.ObjectWithId;
import java.util.Locale;


public class Helper {

    public static ArrayList<ObjectWithId> filterObjectWithIdListByViewingDirection(
            ArrayList<? extends ObjectWithId> objectList, int minAngle, int maxAngle) {
        ArrayList<ObjectWithId> filteredObjectList = new ArrayList<ObjectWithId>();
        for (ObjectWithId object : objectList) {
            Bearing bearingFromCurrentLocation = object.bearingFromCurrentLocation();
            if (bearingFromCurrentLocation != null
                    && bearingFromCurrentLocation.relativeToCurrentBearing().withinRange(minAngle, maxAngle)) {
                filteredObjectList.add(object);
            }
        }
        return filteredObjectList;
    }

    public static ArrayList<Point> filterPointListByTurnValueAndImportantIntersections(ArrayList<? extends Point> pointList) {
        ArrayList<Point> filteredPointList = new ArrayList<Point>();

        // first point must always be added
        filteredPointList.add(pointList.get(0));

        for (int i=1; i<pointList.size()-1; i++) {
            Point previous = filteredPointList.get(filteredPointList.size()-1);
            Point current = pointList.get(i);
            Point next = pointList.get(i+1);

            // get bearings between segments
            Bearing bearingFromPreviousToCurrent = previous.bearingTo(current);
            Bearing bearingFromCurrentToNext = current.bearingTo(next);

            // calculate turn between these two segments
            Turn turn = bearingFromPreviousToCurrent.turnTo(bearingFromCurrentToNext);

            // current is an important intersection
            boolean isImportantIntersection = false;
            if (current instanceof Intersection) {
                isImportantIntersection = ((Intersection) current).isImportant();
            }
            Timber.d("%1$s - %2$s = %3$s, isImportant=%4$s", bearingFromPreviousToCurrent, bearingFromCurrentToNext, turn, isImportantIntersection);

            // skip the current point, if:
            // - it wasn't added manually
            // - and the point is no important intersection
            // - and turn == cross
            if (       current.hasCustomName()      // hack to determine, if the point was added by the user
                    || turn.getInstruction() != Turn.Instruction.CROSS
                    || isImportantIntersection) {
                filteredPointList.add(current);
            }
        }

        // and the last point must always be added too
        filteredPointList.add(pointList.get(pointList.size()-1));

        return filteredPointList;
    }


    /**
     * format strings
     */

    public static String formatStringListWithFillWordAnd(ArrayList<String> stringList) {
        if (stringList == null || stringList.isEmpty()) { return ""; }
        String result = stringList.get(0);
        for (int i=1; i<stringList.size(); i++) {
            result += i+1 == stringList.size()      // check if last list item
                ? String.format(" %1$s ", GlobalInstance.getStringResource(R.string.fillingWordAnd))
                : ", ";
            result += stringList.get(i);
        }
        return result;
    }

    public static String formatYesOrNo(boolean value) {
        return value
            ? GlobalInstance.getStringResource(R.string.dialogYes)
            : GlobalInstance.getStringResource(R.string.dialogNo);
    }


    /**
     * date and time
     */
    private static final String ISO_8601_FORMAT1 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String ISO_8601_FORMAT2 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static Date parseTimestamp(String timestamp) {
        try {
            return (new SimpleDateFormat(ISO_8601_FORMAT1)).parse(timestamp);
        } catch (Exception e) {}
        try {
            return (new SimpleDateFormat(ISO_8601_FORMAT2)).parse(timestamp);
        } catch (Exception e) {}
        return null;
    }


    /**
     * geometry functions
     */

    public static Pair<Float,Float> getScaledEndCoordinatesForLineWithAngle(Angle angle) {
        float scaledX = 0.0f, scaledY = 0.0f;

        if (angle.withinRange(316, 45)) {
            scaledX = (float) Math.tan( Math.toRadians(angle.getDegree()) );
            scaledY = 1.0f;
        } else if (angle.withinRange(46, 135)) {
            scaledX = 1.0f;
            scaledY = (float) Math.tan( Math.toRadians(90-angle.getDegree()) );
        } else if (angle.withinRange(136, 225)) {
            scaledX = (float) Math.tan( Math.toRadians(-angle.getDegree()) );
            scaledY = -1.0f;
        } else if (angle.withinRange(226, 315)) {
            scaledX = -1.0f;
            scaledY = (float) Math.tan( Math.toRadians(angle.getDegree()-90) );
        }

        return Pair.create(scaledX, scaledY);
    }

    public static Coordinates calculateEndCoordinatesForStartCoordinatesAndAngle(Coordinates startCoordinates, Angle angle) {
        Pair<Float,Float> scaledEndCoordinates = getScaledEndCoordinatesForLineWithAngle(angle);
        return new Coordinates(
                startCoordinates.getLatitude() + (scaledEndCoordinates.second / 10000),
                startCoordinates.getLongitude() + (scaledEndCoordinates.first / 10000));
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
                if (doubleFromJson >= 0) {
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
                if (integerFromJson >= 0) {
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
                if (longFromJson >= 0) {
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
            try {
                return jsonObject.getInt(key) != 0;
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

    public static JSONObject putNullableBooleanToJsonObject(
            JSONObject jsonObject, String key, Boolean value) throws JSONException {
        return putNullableBooleanToJsonObject(jsonObject, key, value, false);
    }

    public static JSONObject putNullableBooleanToJsonObject(
            JSONObject jsonObject, String key, Boolean value, boolean asInt) throws JSONException {
        if (value != null) {
            if (asInt) {
                jsonObject.put(key, value ? 1 : 0);
            } else {
                jsonObject.put(key, value);
            }
        }
        return jsonObject;
    }

    public static <T extends Enum<T>> T getNullableEnumFromJsonObject(
            JSONObject jsonObject, String key, Class<T> enumClass) {
        String enumValue = getNullableStringFromJsonObject(jsonObject, key);
        if (enumValue != null) {
            try {
                return Enum.valueOf(enumClass, enumValue.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {}
        }
        return null;
    }

    public static <T extends Enum<T>> JSONObject putNullableEnumToJsonObject(
            JSONObject jsonObject, String key, T value) throws JSONException {
        if (value != null) {
            jsonObject.put(key, value.name());
        }
        return jsonObject;
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

    public static boolean compareArrayLists(ArrayList<? extends Object> list1, ArrayList<? extends Object> list2) {
        if (list1 == null) {
            return list2 == null;
        } else if (list2 == null) {
            return list1 == null;
        } else if (list1.size() != list2.size()) {
            return false;
        }
        for (Object object : list1) {
            if (! list2.contains(object)) {
                return false;
            }
        }
        for (Object object : list2) {
            if (! list1.contains(object)) {
                return false;
            }
        }
        return true;
    }


    /**
     * vibration
     */
    // duration constants
    public static final long VIBRATION_DURATION_SHORT = 50;
    public static final long VIBRATION_DURATION_LONG = 250;
    // intensity constants
    public static final int VIBRATION_INTENSITY_WEAK = 90;
    public static final int VIBRATION_INTENSITY_DEFAULT = 128;
    public static final int VIBRATION_INTENSITY_STRONG = 180;

    @TargetApi(Build.VERSION_CODES.O)
    public static void vibrateOnce(long duration) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrateOnce(duration, VibrationEffect.DEFAULT_AMPLITUDE);
        } else {
            vibrateOnce(duration, VIBRATION_INTENSITY_DEFAULT);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void vibrateOnce(long duration, int amplitude) {
        Vibrator vibrator = (Vibrator) GlobalInstance.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(
                        VibrationEffect.createOneShot(duration, amplitude));
            } else {
                vibrator.vibrate(duration);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void vibratePattern(long[] timings) {
        Vibrator vibrator = (Vibrator) GlobalInstance.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(
                        VibrationEffect.createWaveform(timings, -1));
            } else {
                vibrator.vibrate(timings, -1);
            }
        }
    }

}
