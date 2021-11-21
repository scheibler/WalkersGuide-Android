package org.walkersguide.android.data.sensor;

import android.content.Context;

import com.google.common.primitives.Ints;

import java.lang.NullPointerException;
import java.lang.System;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.util.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;


public class Direction {

    // bearing accuracy thresholds for gps direction in degrees
    public static final int GPS_DIRECTION_HIGH_ACCURACY_THRESHOLD = 15;
    public static final int GPS_DIRECTION_LOW_ACCURACY_THRESHOLD = 30;
    // direction outdated
    public static final int DIRECTION_OUTDATED_AFTER_IN_MS = 30000;

    public static class Builder {
        // variables
        private Context context;
        private JSONObject jsonNewDirection;
        // mandatory params
        public Builder(Context context, int bearing) {
            this.context = context;
            this.jsonNewDirection = new JSONObject();
            try {
                jsonNewDirection.put("bearing", bearing);
            } catch (JSONException e) {}
        }
        // optional params
        public Builder setAccuracyRating(final int accuracyRating) {
            try {
                this.jsonNewDirection.put("accuracyRating", accuracyRating);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setTime(final long time) {
            try {
                jsonNewDirection.put("time", time);
            } catch (JSONException e) {}
            return this;
        }
        // build
        public Direction build() {
            try {
                return new Direction(this.context, this.jsonNewDirection);
            } catch (JSONException e) {
                return null;
            }
        }
        // to json
        public JSONObject toJson() {
            return this.jsonNewDirection;
        }
    }

    public static Direction fromString(Context context, String directionDataString) {
        try {
            return new Direction(
                    context, new JSONObject(directionDataString));
        } catch (JSONException | NullPointerException e) {
            return null;
        }
    }


    private Context context;
    private int bearing;
    private long time;
    private int accuracyRating;

    public Direction(Context context, JSONObject inputData) throws JSONException {
        this.context = context;
        // bearing
        int bearingFromJSON = inputData.getInt("bearing");
        if (bearingFromJSON >= 0 && bearingFromJSON < 360) {
            this.bearing = bearingFromJSON;
        } else {
            throw new JSONException(
                    context.getResources().getString(R.string.errorDirectionOutOfRange));
        }
        // time and accuracy are optional
        this.time = -1l;
        try {
            this.time = inputData.getLong("time");
        } catch (JSONException e) {}
        // accuracy
        this.accuracyRating = Constants.DIRECTION_ACCURACY_RATING.UNKNOWN;
        try {
            int accuracyRatingFromJSON = inputData.getInt("accuracyRating");
            if (Ints.contains(Constants.DirectionAccuracyRatingValueArray, accuracyRatingFromJSON)) {
                this.accuracyRating = accuracyRatingFromJSON;
            }
        } catch (JSONException e) {}
    }

    public int getBearing() {
        return this.bearing;
    }

    public long getTime() {
        return this.time;
    }

    public int getAccuracyRating() {
        return this.accuracyRating;
    }

    public boolean isOutdated() {
        if (this.time > -1l
                && (System.currentTimeMillis() - this.time) > DIRECTION_OUTDATED_AFTER_IN_MS) {
            return true;
        }
        return false;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("bearing", this.bearing);
        jsonObject.put("time", this.time);
        jsonObject.put("accuracyRating", this.accuracyRating);
        return jsonObject;
    }

    @Override public String toString() {
        return String.format(
                "%1$d° (%2$s)",
                this.bearing,
                StringUtility.formatGeographicDirection(this.bearing));
    }

	@Override public int hashCode() {
        int hash = 17;
		hash = hash * 31 + Integer.valueOf(this.bearing).hashCode();
		return hash;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof Direction)) {
			return false;
        }
		Direction other = (Direction) obj;
        return this.bearing == other.getBearing();
    }


    public static String formatAccuracyRating(Context context, int accuracyRating) {
        switch (accuracyRating) {
            case Constants.DIRECTION_ACCURACY_RATING.LOW:
                return context.getResources().getString(R.string.directionAccuracyLow);
            case Constants.DIRECTION_ACCURACY_RATING.MEDIUM:
                return context.getResources().getString(R.string.directionAccuracyMedium);
            case Constants.DIRECTION_ACCURACY_RATING.HIGH:
                return context.getResources().getString(R.string.directionAccuracyHigh);
            default:
                return "";
        }
    }

}
