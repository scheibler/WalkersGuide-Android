package org.walkersguide.android.data.basic.point;

import android.content.Context;

import android.location.Location;
import android.location.LocationManager;

import com.google.common.primitives.Ints;

import org.json.JSONException;
import org.json.JSONObject;


public class Point {

    private Context context;
    private String name, type, subType;
    private Location location;
    private long nodeId;
    private int tactilePaving, wheelchair;

    public Point(Context context, JSONObject inputData) throws JSONException {
        this.context = context;
        // name, type and subtype
        this.name = inputData.getString("name");
        this.type = inputData.getString("type");
        this.subType = inputData.getString("sub_type");
        // location object
        this.location = new Location(LocationManager.GPS_PROVIDER);
        this.location.setLatitude(inputData.getDouble("lat"));
        this.location.setLongitude(inputData.getDouble("lon"));
        // optional parameters
        this.nodeId = -1;
        try {
            this.nodeId = inputData.getLong("node_id");
        } catch (JSONException e) {}
        this.tactilePaving = -1;
        try {
            int tactilePavingValue = inputData.getInt("tactile_paving");
            if (Ints.contains(TactilePavingValueArray, tactilePavingValue)) {
                this.tactilePaving = tactilePavingValue;
            }
        } catch (JSONException e) {}
        this.wheelchair = -1;
        try {
            int wheelchairValue = inputData.getInt("wheelchair");
            if (Ints.contains(WheelchairValueArray, wheelchairValue)) {
                this.wheelchair = wheelchairValue;
            }
        } catch (JSONException e) {}
    }

    public Context getContext() {
        return this.context;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public String getSubType() {
        return this.subType;
    }


    public Location getLocationObject() {
        return this.location;
    }

    public double getLatitude() {
        return this.location.getLatitude();
    }

    public double getLongitude() {
        return this.location.getLongitude();
    }

    public long getNodeId() {
        return this.nodeId;
    }

    /**
     * tactile paving
     *  -1: unknown (default)
     *   0: no
     *   1: yes
     */

    public interface TACTILE_PAVING {
        public static final int NO = 0;
        public static final int YES = 1;
    }

    public final static int[] TactilePavingValueArray = {
        TACTILE_PAVING.NO, TACTILE_PAVING.YES
    };

    public int getTactilePaving() {
        return this.tactilePaving;
    }

    /**
     * wheelchair accessibility
     *  -1: unknown (default)
     *   0: no
     *   1: limited
     *   2: yes
     */

    public interface WHEELCHAIR {
        public static final int NO = 0;
        public static final int LIMITED = 1;
        public static final int YES = 2;
    }

    public final static int[] WheelchairValueArray = {
        WHEELCHAIR.NO, WHEELCHAIR.LIMITED, WHEELCHAIR.YES
    };

    public int getWheelchair() {
        return this.wheelchair;
    }


    /**
     * helper functions
     */

    public int distanceTo(Point other) {
        if (other != null) {
            return (int) Math.round(
                    this.location.distanceTo(other.getLocationObject()));
        }
        return -1;
    }

    public int bearingTo(Point other) {
        if (other != null) {
            // bearing to north
            int absoluteDirection = (int) Math.round(
                    this.location.bearingTo(other.getLocationObject()));
            if (absoluteDirection < 0) {
                absoluteDirection += 360;
            }
            return absoluteDirection;
        }
        return -1;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        // name, type and subtype
        jsonObject.put("name", this.name);
        jsonObject.put("type", this.type);
        jsonObject.put("sub_type", this.subType);
        // location
        jsonObject.put("lat", this.location.getLatitude());
        jsonObject.put("lon", this.location.getLongitude());
        // optional parameters
        if (this.nodeId > -1) {
            try {
                jsonObject.put("node_id", this.nodeId);
            } catch (JSONException e) {}
        }
        if (this.tactilePaving > -1) {
            try {
                jsonObject.put("tactile_paving", this.tactilePaving);
            } catch (JSONException e) {}
        }
        if (this.wheelchair > -1) {
            try {
                jsonObject.put("wheelchair", this.wheelchair);
            } catch (JSONException e) {}
        }
        return jsonObject;
    }

    @Override public String toString() {
        if (this.subType.equals("")
                || this.name.equals(this.subType)) {
            return this.name;
        }
        return String.format("%1$s (%2$s)", this.name, this.subType);
    }

	@Override public int hashCode() {
        int hash = 17;
		hash = hash * 31 + this.type.hashCode();
		hash = hash * 31 + Double.valueOf(this.location.getLatitude()).hashCode();
		hash = hash * 31 + Double.valueOf(this.location.getLongitude()).hashCode();
		return hash;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof Point)) {
			return false;
        }
		Point other = (Point) obj;
        return this.type.equals(other.getType())
            && this.getLatitude() == other.getLatitude()
            && this.getLongitude() == other.getLongitude();
    }

}
