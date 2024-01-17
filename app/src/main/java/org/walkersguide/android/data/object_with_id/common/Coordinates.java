package org.walkersguide.android.data.object_with_id.common;

import android.location.Location;
import android.location.LocationManager;
import java.io.Serializable;
import timber.log.Timber;
import org.walkersguide.android.data.ObjectWithId.Icon;
import org.walkersguide.android.util.GlobalInstance;

import org.walkersguide.android.data.angle.Bearing;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.R;
import org.walkersguide.android.data.object_with_id.common.Coordinates;
import org.walkersguide.android.data.object_with_id.common.TactilePaving;
import org.walkersguide.android.data.object_with_id.common.Wheelchair;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.util.GlobalInstance;
import java.util.HashMap;
import java.util.Map;
import org.walkersguide.android.data.ObjectWithId;
import android.text.TextUtils;
import java.util.Locale;
import org.walkersguide.android.sensor.PositionManager;
import java.util.Comparator;
import org.walkersguide.android.data.angle.RelativeBearing;


public class Coordinates implements Serializable {
    private static final long serialVersionUID = 1l;

    private double latitude, longitude;

    public Coordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Coordinates(JSONObject inputData) throws JSONException {
        this.latitude = inputData.getDouble(KEY_LATITUDE);
        this.longitude = inputData.getDouble(KEY_LONGITUDE);
    }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public Location getLocationObject() {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(this.latitude);
        location.setLongitude(this.longitude);
        return location;
    }

    public Integer distanceTo(Coordinates other) {
        if (other != null) {
            return Integer.valueOf(
                    (int) Math.round(
                        this.getLocationObject().distanceTo(other.getLocationObject())));
        }
        return null;
    }

    public Bearing bearingTo(Coordinates other) {
        if (other != null) {
            return new Bearing(
                    (int) Math.round(
                        this.getLocationObject().bearingTo(other.getLocationObject())));
        }
        return null;
    }

    @Override public String toString() {
        return String.format(
                Locale.getDefault(), "%1$f, %2$f", this.latitude, this.longitude);
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Double.valueOf(this.latitude).hashCode();
        result = prime * result + Double.valueOf(this.longitude).hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (! (obj instanceof Coordinates)) {
            return false;
        }
        Coordinates other = (Coordinates) obj;
        return this.latitude == other.getLatitude()
            && this.longitude == other.getLongitude();
    }


    // json
    public static final String KEY_LATITUDE = "lat";
    public static final String KEY_LONGITUDE = "lon";

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_LATITUDE, this.latitude);
        jsonObject.put(KEY_LONGITUDE, this.longitude);
        return jsonObject;
    }

}
