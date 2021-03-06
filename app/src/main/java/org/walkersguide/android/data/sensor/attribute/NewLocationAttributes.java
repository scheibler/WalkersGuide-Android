package org.walkersguide.android.data.sensor.attribute;

import android.content.Context;

import java.lang.NullPointerException;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.sensor.threshold.DistanceThreshold;
import org.walkersguide.android.data.sensor.threshold.SpeedThreshold;


public class NewLocationAttributes {

    public static class Builder {
        // variables
        private Context context;
        private JSONObject jsonNewLocationAttributes;
        // mandatory params
        public Builder(Context context, PointWrapper location) {
            this.context = context;
            this.jsonNewLocationAttributes = new JSONObject();
            try {
                jsonNewLocationAttributes.put("location", location.toJson());
            } catch (JSONException | NullPointerException e) {}
        }
        // optional params
        public Builder setImmediateDistanceThreshold(final DistanceThreshold immediateDistanceThreshold) {
            try {
                jsonNewLocationAttributes.put("immediateDistanceThresholdInMeters", immediateDistanceThreshold.getDistanceThresholdInMeters());
            } catch (JSONException e) {}
            return this;
        }
        public Builder setAggregatingDistanceThreshold(final DistanceThreshold aggregatingDistanceThreshold) {
            try {
                jsonNewLocationAttributes.put("aggregatingDistanceThresholdInMeters", aggregatingDistanceThreshold.getDistanceThresholdInMeters());
            } catch (JSONException e) {}
            return this;
        }
        public Builder setSpeedThreshold(final SpeedThreshold speedThreshold) {
            try {
                jsonNewLocationAttributes.put("speedThresholdInKMH", speedThreshold.getSpeedThresholdInKMH());
            } catch (JSONException e) {}
            return this;
        }
        // build
        public NewLocationAttributes build() {
            try {
                return new NewLocationAttributes(this.context, this.jsonNewLocationAttributes);
            } catch (JSONException e) {
                return null;
            }
        }
        // to json
        public JSONObject toJson() {
            return this.jsonNewLocationAttributes;
        }
    }

    public static NewLocationAttributes fromString(Context context, String locationDataString) {
        try {
            return new NewLocationAttributes(
                    context, new JSONObject(locationDataString));
        } catch (JSONException | NullPointerException e) {
            return null;
        }
    }


    private PointWrapper location;
    private DistanceThreshold immediateDistanceThreshold;
    private DistanceThreshold aggregatingDistanceThreshold;
    private SpeedThreshold speedThreshold;

    public NewLocationAttributes(Context context, JSONObject inputData) throws JSONException {
        // location object
        this.location = new PointWrapper(
                context, inputData.getJSONObject("location"));
        // immediate distance threshold
        this.immediateDistanceThreshold = DistanceThreshold.ZERO_METERS;
        try {
            DistanceThreshold immediateDistanceThresholdFromJSON = DistanceThreshold.lookupByDistanceThreshold(
                    inputData.getInt("immediateDistanceThresholdInMeters"));
            if (immediateDistanceThresholdFromJSON != null) {
                this.immediateDistanceThreshold = immediateDistanceThresholdFromJSON;
            }
        } catch (JSONException e) {}
        // aggregating distance threshold
        this.aggregatingDistanceThreshold = DistanceThreshold.ZERO_METERS;
        try {
            DistanceThreshold aggregatingDistanceThresholdFromJSON= DistanceThreshold.lookupByDistanceThreshold(
                    inputData.getInt("aggregatingDistanceThresholdInMeters"));
            if (aggregatingDistanceThresholdFromJSON != null) {
                this.aggregatingDistanceThreshold = aggregatingDistanceThresholdFromJSON;
            }
        } catch (JSONException e) {}
        // speed threshold
        this.speedThreshold = SpeedThreshold.ZERO_KMH;
        try {
            SpeedThreshold speedThresholdFromJSON= SpeedThreshold.lookupBySpeedThreshold(
                    inputData.getInt("speedThresholdInKMH"));
            if (speedThresholdFromJSON != null) {
                this.speedThreshold = speedThresholdFromJSON;
            }
        } catch (JSONException e) {}
    }

    public PointWrapper getLocation() {
        return this.location;
    }

    public DistanceThreshold getImmediateDistanceThreshold() {
        return this.immediateDistanceThreshold;
    }

    public DistanceThreshold getAggregatingDistanceThreshold() {
        return this.aggregatingDistanceThreshold;
    }

    public SpeedThreshold getSpeedThreshold() {
        return this.speedThreshold;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("location", this.location.toJson());
        jsonObject.put("immediateDistanceThresholdInMeters", this.immediateDistanceThreshold.getDistanceThresholdInMeters());
        jsonObject.put("aggregatingDistanceThresholdInMeters", this.aggregatingDistanceThreshold.getDistanceThresholdInMeters());
        jsonObject.put("speedThresholdInKMH", this.speedThreshold.getSpeedThresholdInKMH());
        return jsonObject;
    }

    @Override public String toString() {
        return String.format(
                "%1$s, immediate: %2$s, aggregating: %3$s, speed: %4$s",
                this.location.getPoint().getName(),
                this.immediateDistanceThreshold.toString(),
                this.aggregatingDistanceThreshold.toString(),
        this.speedThreshold.toString());
    }

}
