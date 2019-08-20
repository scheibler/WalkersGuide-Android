package org.walkersguide.android.data.sensor.attribute;

import android.content.Context;

import java.lang.NullPointerException;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.data.sensor.threshold.BearingThreshold;


public class NewDirectionAttributes {

    public static class Builder {
        // variables
        private Context context;
        private JSONObject jsonNewDirectionAttributes;
        // mandatory params
        public Builder(Context context, Direction direction) {
            this.context = context;
            this.jsonNewDirectionAttributes = new JSONObject();
            try {
                jsonNewDirectionAttributes.put("direction", direction.toJson());
            } catch (JSONException | NullPointerException e) {}
        }
        // optional params
        public Builder setImmediateBearingThreshold(final BearingThreshold immediateBearingThreshold) {
            try {
                jsonNewDirectionAttributes.put("immediateBearingThresholdInDegrees", immediateBearingThreshold.getBearingThresholdInDegrees());
            } catch (JSONException e) {}
            return this;
        }
        public Builder setAggregatingBearingThreshold(final BearingThreshold aggregatingBearingThreshold) {
            try {
                jsonNewDirectionAttributes.put("aggregatingBearingThresholdInDegrees", aggregatingBearingThreshold.getBearingThresholdInDegrees());
            } catch (JSONException e) {}
            return this;
        }
        // build
        public NewDirectionAttributes build() {
            try {
                return new NewDirectionAttributes(this.context, this.jsonNewDirectionAttributes);
            } catch (JSONException e) {
                return null;
            }
        }
        // to json
        public JSONObject toJson() {
            return this.jsonNewDirectionAttributes;
        }
    }

    public static NewDirectionAttributes fromString(Context context, String directionDataString) {
        try {
            return new NewDirectionAttributes(
                    context, new JSONObject(directionDataString));
        } catch (JSONException | NullPointerException e) {
            return null;
        }
    }


    private Direction direction;
    private BearingThreshold immediateBearingThreshold;
    private BearingThreshold aggregatingBearingThreshold;

    public NewDirectionAttributes(Context context, JSONObject inputData) throws JSONException {
        // direction object
        this.direction = new Direction(
                context, inputData.getJSONObject("direction"));
        // immediate bearing threshold
        this.immediateBearingThreshold = BearingThreshold.ZERO_DEGREES;
        try {
            BearingThreshold immediateBearingThresholdFromJSON = BearingThreshold.lookupByBearingThreshold(
                    inputData.getInt("immediateBearingThresholdInDegrees"));
            if (immediateBearingThresholdFromJSON != null) {
                this.immediateBearingThreshold = immediateBearingThresholdFromJSON;
            }
        } catch (JSONException e) {}
        // aggregating bearing threshold
        this.aggregatingBearingThreshold = BearingThreshold.ZERO_DEGREES;
        try {
            BearingThreshold aggregatingBearingThresholdFromJSON= BearingThreshold.lookupByBearingThreshold(
                    inputData.getInt("aggregatingBearingThresholdInDegrees"));
            if (aggregatingBearingThresholdFromJSON != null) {
                this.aggregatingBearingThreshold = aggregatingBearingThresholdFromJSON;
            }
        } catch (JSONException e) {}
    }

    public Direction getDirection() {
        return this.direction;
    }

    public BearingThreshold getImmediateBearingThreshold() {
        return this.immediateBearingThreshold;
    }

    public BearingThreshold getAggregatingBearingThreshold() {
        return this.aggregatingBearingThreshold;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("direction", this.direction.toJson());
        jsonObject.put("immediateBearingThresholdInDegrees", this.immediateBearingThreshold.getBearingThresholdInDegrees());
        jsonObject.put("aggregatingBearingThresholdInDegrees", this.aggregatingBearingThreshold.getBearingThresholdInDegrees());
        return jsonObject;
    }

    @Override public String toString() {
        return String.format(
                "%1$d°, immediate: %2$s, aggregating: %3$s",
                this.direction.getBearing(),
                this.immediateBearingThreshold.toString(),
                this.aggregatingBearingThreshold.toString());
    }

}
