package org.walkersguide.android.data.angle.bearing;

import org.walkersguide.android.sensor.bearing.BearingSensorAccuracyRating;


import java.lang.System;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.util.Helper;
import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.util.GlobalInstance;
import java.lang.StringBuilder;


public class BearingSensorValue extends Bearing implements Serializable {
    private static final long serialVersionUID = 1l;

    public static final int OUTDATED_AFTER_MS = 30000;


    private long timestamp;
    // rating is optional and may be null
    private BearingSensorAccuracyRating accuracyRating;

    public BearingSensorValue(int degree, long timestamp, BearingSensorAccuracyRating accuracyRating) {
        super(degree);
        this.timestamp = timestamp;
        this.accuracyRating = accuracyRating;
    }

    public BearingSensorValue(JSONObject inputData) throws JSONException {
        super(inputData.getInt(KEY_DEGREE));
        this.timestamp = inputData.getLong(KEY_TIMESTAMP);
        this.accuracyRating = Helper.getNullableEnumFromJsonObject(
                inputData, KEY_ACCURACY_RATING, BearingSensorAccuracyRating.class);
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public BearingSensorAccuracyRating getAccuracyRating() {
        return this.accuracyRating;
    }

    public boolean isOutdated() {
        return (System.currentTimeMillis() - this.timestamp) > OUTDATED_AFTER_MS;
    }

    public String formatDetails() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(
                String.format(
                    "%1$s: %2$s",
                    GlobalInstance.getStringResource(R.string.labelBearingAccuracy),
                    this.accuracyRating != null
                    ? this.accuracyRating.getDisplayName()
                    : GlobalInstance.getStringResource(R.string.bearingSensorAccuracyUnknown))
                );
        if (isOutdated()) {
            stringBuilder.append(System.lineSeparator());
            stringBuilder.append(
                    GlobalInstance.getStringResource(R.string.labelBearingOutdated));
        }
        return stringBuilder.toString();
    }


    /**
     * json
     */
    public static final String KEY_DEGREE = "bearing";
    public static final String KEY_TIMESTAMP = "time";
    public static final String KEY_ACCURACY_RATING = "accuracyRating";

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_DEGREE, super.getDegree());
        jsonObject.put(KEY_TIMESTAMP, this.timestamp);
        if (this.accuracyRating != null) {
            jsonObject.put(KEY_ACCURACY_RATING, this.accuracyRating.name());
        }
        return jsonObject;
    }

}
