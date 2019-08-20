package org.walkersguide.android.data.basic.point;

import android.content.Context;

import android.text.format.DateFormat;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;


public class GPS extends Point {

    public static class Builder {
        // variables
        private Context context;
        private JSONObject jsonNewLocation;

        // mandatory params
        public Builder(Context context, double latitude, double longitude, long time) {
            this.context = context;
            this.jsonNewLocation = new JSONObject();
            try {
                jsonNewLocation.put("name", context.getResources().getString(R.string.currentLocationName));
            } catch (JSONException e) {}
            try {
                jsonNewLocation.put("type", Constants.POINT.GPS);
            } catch (JSONException e) {}
            try {
                jsonNewLocation.put("sub_type", context.getResources().getString(R.string.currentLocationName));
            } catch (JSONException e) {}
            try {
                jsonNewLocation.put("lat", latitude);
            } catch (JSONException e) {}
            try {
                jsonNewLocation.put("lon", longitude);
            } catch (JSONException e) {}
            try {
                jsonNewLocation.put("time", time);
            } catch (JSONException e) {}
        }

        // optional params
        public Builder setProvider(final String provider) {
            try {
                jsonNewLocation.put("provider", provider);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setAccuracy(final float accuracy) {
            try {
                jsonNewLocation.put("accuracy", accuracy);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setAltitude(final double altitude) {
            try {
                jsonNewLocation.put("altitude", altitude);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setNumberOfSatellites(final int numberOfSatellites) {
            try {
                jsonNewLocation.put("number_of_satellites", numberOfSatellites);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setSpeed(final float speed) {
            try {
                jsonNewLocation.put("speed", speed);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setDirection(final Direction direction) {
            try {
                jsonNewLocation.put("direction", direction.toJson());
            } catch (JSONException e) {}
            return this;
        }

        // build
        public GPS build() {
            try {
                return new GPS(this.context, this.jsonNewLocation);
            } catch (JSONException e) {
                return null;
            }
        }
        // to json
        public JSONObject toJson() {
            return this.jsonNewLocation;
        }
    }


    private double altitude;
    private float accuracy, speed;
    private int numberOfSatellites;
    private long time;
    private String provider;
    private Direction direction;

    public GPS(Context context, JSONObject inputData) throws JSONException {
        // point super constructor
        super(context, inputData);
        // accuracy
        this.accuracy = -1.0f;
        try {
            this.accuracy = Double.valueOf(inputData.getDouble("accuracy")).floatValue();
        } catch (JSONException e) {}
        // altitude
        this.altitude = -1.0;
        try {
            this.altitude = inputData.getDouble("altitude");
        } catch (JSONException e) {}
        // provider
        this.provider = "";
        try {
            this.provider = inputData.getString("provider");
        } catch (JSONException e) {}
        // number of satellites
        this.numberOfSatellites = -1;
        try {
            this.numberOfSatellites = inputData.getInt("number_of_satellites");
        } catch (JSONException e) {}
        // speed
        this.speed = -1.0f;
        try {
            this.speed = Double.valueOf(inputData.getDouble("speed")).floatValue();
        } catch (JSONException e) {}
        // time
        this.time = -1l;
        try {
            this.time = inputData.getLong("time");
        } catch (JSONException e) {}

        // direction
        this.direction = null;
        try {
            this.direction = new Direction(
                    context, inputData.getJSONObject("direction"));
        } catch (JSONException e) {
            this.direction = null;
        } finally {
            if (this.direction == null) {
                // legacy
                int bearing = -1;
                try {
                    bearing = inputData.getInt("bearing");
                } catch (JSONException e) {}
                direction = new Direction.Builder(context, bearing)
                    .setTime(this.time)
                    .build();
            }
        }
    }

    public float getAccuracy() {
        return this.accuracy;
    }

    public String formatAccuracyInMeters() {
        if (this.accuracy >= 0.0) {
            return String.format(
                    "%1$s: %2$s",
                    super.getContext().getResources().getString(R.string.labelGPSAccuracy),
                    super.getContext().getResources().getQuantityString(
                        R.plurals.meter,
                        Math.round(this.accuracy),
                        Math.round(this.accuracy))
                    );
        }
        return super.getContext().getResources().getString(R.string.labelGPSAccuracy);
    }

    public double getAltitude() {
        return this.altitude;
    }

    public String formatAltitudeInMeters() {
        if (this.altitude >= 0.0) {
            return String.format(
                    "%1$s: %2$s",
                    super.getContext().getResources().getString(R.string.labelGPSAltitude),
                    super.getContext().getResources().getQuantityString(
                        R.plurals.meter,
                        (int) Math.round(this.altitude),
                        (int) Math.round(this.altitude)));
        }
        return super.getContext().getResources().getString(R.string.labelGPSAltitude);
    }

    public String getProvider() {
        return this.provider;
    }

    public int getNumberOfSatellites() {
        return this.numberOfSatellites;
    }

    public String formatProviderAndNumberOfSatellites() {
        if (this.numberOfSatellites >= 0) {
            return String.format(
                    "%1$s: %2$s, %3$s",
                    super.getContext().getResources().getString(R.string.labelGPSProvider),
                    this.provider,
                    super.getContext().getResources().getQuantityString(
                        R.plurals.satellite,
                        this.numberOfSatellites,
                        this.numberOfSatellites)
                    );
        }
        return String.format(
                "%1$s: %2$s",
                super.getContext().getResources().getString(R.string.labelGPSProvider),
                this.provider);
    }

    public float getSpeed() {
        return this.speed;
    }

    public String formatSpeedInKMH() {
        if (this.speed >= 0.0) {
            return String.format(
                    "%1$s: %2$.1f km/h",
                    super.getContext().getResources().getString(R.string.labelGPSSpeed),
                    this.speed);
        }
        return super.getContext().getResources().getString(R.string.labelGPSSpeed);
    }

    public long getTime() {
        return this.time;
    }

    public String formatTimestamp() {
        if (this.time >= 0) {
            String formattedTime = DateFormat.getTimeFormat(super.getContext()).format(new Date(this.time));
            String formattedDate = DateFormat.getDateFormat(super.getContext()).format(new Date(this.time));
            if (formattedDate.equals(DateFormat.getDateFormat(super.getContext()).format(new Date()))) {
                return String.format(
                        "%1$s: %2$s",
                        super.getContext().getResources().getString(R.string.labelGPSTime),
                        formattedTime);
            } else {
                return String.format(
                        "%1$s: %2$s, %3$s",
                        super.getContext().getResources().getString(R.string.labelGPSTime),
                        formattedDate,
                        formattedTime);
            }
        }
        return super.getContext().getResources().getString(R.string.labelGPSTime);
    }

    public Direction getDirection() {
        return this.direction;
    }

    public String formatBearingInDegrees() {
        if (this.direction != null) {
            return String.format(
                    "%1$s: %2$s",
                    super.getContext().getResources().getString(R.string.labelGPSBearing),
                    this.direction.toString());
        }
        return super.getContext().getResources().getString(R.string.labelGPSBearing);
    }

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        if (this.accuracy >= 0.0) {
            try {
                jsonObject.put("accuracy", this.accuracy);
            } catch (JSONException e) {}
        }
        if (this.altitude >= 0.0) {
            try {
                jsonObject.put("altitude", this.altitude);
            } catch (JSONException e) {}
        }
        if (! this.provider.equals("")) {
            try {
                jsonObject.put("provider", this.provider);
            } catch (JSONException e) {}
        }
        if (this.numberOfSatellites >= 0) {
            try {
                jsonObject.put("number_of_satellites", this.numberOfSatellites);
            } catch (JSONException e) {}
        }
        if (this.speed >= 0.0) {
            try {
                jsonObject.put("speed", this.speed);
            } catch (JSONException e) {}
        }
        if (this.time >= 0l) {
            try {
                jsonObject.put("time", this.time);
            } catch (JSONException e) {}
        }
        if (this.direction != null) {
            try {
                jsonObject.put("direction", this.direction.toJson());
            } catch (JSONException e) {}
        }
        return jsonObject;
    }

    @Override public String toString() {
        return super.toString();
    }

}
