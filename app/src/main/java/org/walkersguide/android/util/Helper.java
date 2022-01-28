package org.walkersguide.android.util;






import org.json.JSONObject;
import org.json.JSONException;
import android.text.TextUtils;


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

}
