package org.walkersguide.android.util;






import org.json.JSONObject;
import org.json.JSONException;
import android.text.TextUtils;
import android.os.Vibrator;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.VibrationEffect;
import android.content.Context;
import java.util.ArrayList;


public class Helper {

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

    public static boolean compareArrayLists(ArrayList<? extends Object> list1, ArrayList<? extends Object> list2) {
        if (list1.size() != list2.size()) {
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
