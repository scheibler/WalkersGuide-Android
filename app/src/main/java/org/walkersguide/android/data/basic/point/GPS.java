package org.walkersguide.android.data.basic.point;

import android.text.format.DateFormat;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;
import java.lang.StringBuilder;
import java.io.Serializable;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.StringUtility;


public class GPS extends Point {
    private static final long serialVersionUID = 1l;

    // location outdated
    public static final int LOCATION_OUTDATED_AFTER_IN_MS = 300000;

    public static class Builder extends Point.Builder {
        public Builder(double latitude, double longitude, long timestamp) {
            super(
                    Point.Type.GPS,
                    String.format("%1$f, %2$f", latitude, longitude),
                    GlobalInstance.getStringResource(R.string.currentLocationName),
                    latitude,
                    longitude);
            try {
                super.inputData.put(KEY_TIMESTAMP, timestamp);
            } catch (JSONException e) {}
        }

        // optional params
        public Builder setProvider(final String provider) {
            try {
                super.inputData.put(KEY_PROVIDER, provider);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setAccuracy(final float accuracy) {
            try {
                super.inputData.put(KEY_ACCURACY, accuracy);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setAltitude(final double altitude) {
            try {
                super.inputData.put(KEY_ALTITUDE, altitude);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setNumberOfSatellites(final int numberOfSatellites) {
            try {
                super.inputData.put(KEY_NUMBER_OF_SATELLITES, numberOfSatellites);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setSpeed(final float speed) {
            try {
                super.inputData.put(KEY_SPEED, speed);
            } catch (JSONException e) {}
            return this;
        }
        public Builder setDirection(final Direction direction) {
            try {
                super.inputData.put(KEY_DIRECTION, direction.toJson());
            } catch (JSONException e) {}
            return this;
        }

        // build
        public GPS build() {
            GPS gps = null;
            try {
                gps = new GPS(super.inputData);
            } catch (JSONException e) {
                gps = null;
            }
            return gps;
        }
    }


    /**
     * constructor
     */


    private Long timestamp;
    private Double altitude;
    private Float accuracy, speed;
    private Integer numberOfSatellites;
    private String provider;
    private Direction direction;

    public GPS(JSONObject inputData) throws JSONException {
        super(inputData);
        this.timestamp = inputData.getLong(KEY_TIMESTAMP);

        this.altitude = StringUtility.getNullableAndPositiveDoubleFromJsonObject(inputData, KEY_ALTITUDE);
        this.accuracy = StringUtility.getNullableAndPositiveFloatFromJsonObject(inputData, KEY_ACCURACY);
        this.speed = StringUtility.getNullableAndPositiveFloatFromJsonObject(inputData, KEY_SPEED);
        this.numberOfSatellites = StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_NUMBER_OF_SATELLITES);
        this.provider = StringUtility.getNullableStringFromJsonObject(inputData, KEY_PROVIDER);

        this.direction = null;
        try {
            this.direction = new Direction(
                    GlobalInstance.getContext(), inputData.getJSONObject(KEY_DIRECTION));
        } catch (JSONException e) {
            this.direction = null;
        } finally {
            if (this.direction == null) {
                // legacy
                Integer bearing = StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, "bearing");
                if (bearing != null) {
                    direction = new Direction.Builder(GlobalInstance.getContext(), bearing)
                        .setTime(this.timestamp)
                        .build();
                }
            }
        }
    }


    /**
     * getters and setters
     */

    public boolean isOutdated() {
        return (System.currentTimeMillis() - this.timestamp) > LOCATION_OUTDATED_AFTER_IN_MS;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public Double getAltitude() {
        return this.altitude;
    }

    public Float getAccuracy() {
        return this.accuracy;
    }

    public Float getSpeed() {
        return this.speed;
    }

    public String getProvider() {
        return this.provider;
    }

    public Integer getNumberOfSatellites() {
        return this.numberOfSatellites;
    }

    public Direction getDirection() {
        return this.direction;
    }


    /**
     * formatter
     */

    public String formatTimestamp() {
        String formattedTime = DateFormat
            .getTimeFormat(GlobalInstance.getContext())
            .format(new Date(this.timestamp));
        String formattedDate = DateFormat
            .getDateFormat(GlobalInstance.getContext())
            .format(new Date(this.timestamp));
        String formattedDateToday = DateFormat
            .getDateFormat(GlobalInstance.getContext())
            .format(new Date());

        if (formattedDate.equals(formattedDateToday)) {
            // today: only show formatted time
            return String.format(
                    "%1$s: %2$s",
                    GlobalInstance.getStringResource(R.string.labelGPSTime),
                    formattedTime);
        }
        return String.format(
                "%1$s: %2$s, %3$s",
                GlobalInstance.getStringResource(R.string.labelGPSTime),
                formattedDate,
                formattedTime);
    }

    public String formatAltitudeInMeters() {
        if (this.altitude != null) {
            return String.format(
                    "%1$s: %2$s",
                    GlobalInstance.getStringResource(R.string.labelGPSAltitude),
                    GlobalInstance.getPluralResource(R.plurals.meter, (int) Math.round(this.altitude)));
        }
        return GlobalInstance.getStringResource(R.string.labelGPSAltitude);
    }

    public String formatAccuracyInMeters() {
        if (this.accuracy != null) {
            return String.format(
                    "%1$s: %2$s",
                    GlobalInstance.getStringResource(R.string.labelGPSAccuracy),
                    GlobalInstance.getPluralResource(R.plurals.meter, Math.round(this.accuracy)));
        }
        return GlobalInstance.getStringResource(R.string.labelGPSAccuracy);
    }

    public String formatSpeedInKMH() {
        if (this.speed != null) {
            return String.format(
                    "%1$s: %2$.1f km/h",
                    GlobalInstance.getStringResource(R.string.labelGPSSpeed),
                    this.speed);
        }
        return GlobalInstance.getStringResource(R.string.labelGPSSpeed);
    }

    public String formatProviderAndNumberOfSatellites() {
        if (this.provider != null) {
            if (this.numberOfSatellites != null) {
                return String.format(
                        "%1$s: %2$s, %3$s",
                        GlobalInstance.getStringResource(R.string.labelGPSProvider),
                        this.provider,
                        GlobalInstance.getPluralResource(R.plurals.satellite, this.numberOfSatellites));
            }
            return String.format(
                    "%1$s: %2$s",
                    GlobalInstance.getStringResource(R.string.labelGPSProvider),
                    this.provider);
        }
        return GlobalInstance.getStringResource(R.string.labelGPSProvider);
    }

    public String formatBearingInDegrees() {
        if (this.direction != null) {
            return String.format(
                    "%1$s: %2$s",
                    GlobalInstance.getStringResource(R.string.labelGPSBearing),
                    this.direction.toString());
        }
        return GlobalInstance.getStringResource(R.string.labelGPSBearing);
    }

    public String getShortStatusMessage() {
        StringBuilder locationDescriptionBuilder = new StringBuilder();
        locationDescriptionBuilder.append(this.formatAccuracyInMeters());
        if (this.isOutdated()) {
            locationDescriptionBuilder.append(
                    String.format(", %1$s", GlobalInstance.getStringResource(R.string.toolbarSensorDataOutdated)));
        }
        return locationDescriptionBuilder.toString();
    }


    /**
     * to json
     */

    // mandatory params
    public static final String KEY_TIMESTAMP = "time";
    // optional params
    public static final String KEY_PROVIDER = "provider";
    public static final String KEY_ACCURACY = "accuracy";
    public static final String KEY_ALTITUDE = "altitude";
    public static final String KEY_NUMBER_OF_SATELLITES = "number_of_satellites";
    public static final String KEY_SPEED = "speed";
    public static final String KEY_DIRECTION = "direction";

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        jsonObject.put(KEY_TIMESTAMP, this.timestamp);

        if (this.accuracy != null) {
            try {
                jsonObject.put(KEY_ACCURACY, this.accuracy);
            } catch (JSONException e) {}
        }
        if (this.altitude != null) {
            try {
                jsonObject.put(KEY_ALTITUDE, this.altitude);
            } catch (JSONException e) {}
        }
        if (this.provider != null) {
            try {
                jsonObject.put(KEY_PROVIDER, this.provider);
            } catch (JSONException e) {}
        }
        if (this.numberOfSatellites != null) {
            try {
                jsonObject.put(KEY_NUMBER_OF_SATELLITES, this.numberOfSatellites);
            } catch (JSONException e) {}
        }
        if (this.speed != null) {
            try {
                jsonObject.put(KEY_SPEED, this.speed);
            } catch (JSONException e) {}
        }
        if (this.direction != null) {
            try {
                jsonObject.put(KEY_DIRECTION, this.direction.toJson());
            } catch (JSONException e) {}
        }

        return jsonObject;
    }

}
