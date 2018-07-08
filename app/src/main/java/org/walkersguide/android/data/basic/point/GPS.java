package org.walkersguide.android.data.basic.point;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class GPS extends Point {

    private double altitude;
    private float accuracy, bearing, speed;
    private int numberOfSatellites;
    private long time;
    private String provider;

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
        // bearing
        this.bearing = -1.0f;
        try {
            this.bearing = Double.valueOf(inputData.getDouble("bearing")).floatValue();
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
    }

    public float getAccuracy() {
        return this.accuracy;
    }

    public double getAltitude() {
        return this.altitude;
    }

    public float getBearing() {
        return this.bearing;
    }

    public String getProvider() {
        return this.provider;
    }

    public int getNumberOfSatellites() {
        return this.numberOfSatellites;
    }

    public float getSpeed() {
        return this.speed;
    }

    public long getTime() {
        return this.time;
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
        if (this.bearing >= 0.0) {
            try {
                jsonObject.put("bearing", this.bearing);
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
        return jsonObject;
    }

    @Override public String toString() {
        return super.toString();
    }

}
