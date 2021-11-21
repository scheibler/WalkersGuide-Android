package org.walkersguide.android.data.route;

import org.walkersguide.android.R;
import org.walkersguide.android.util.StringUtility;
import org.walkersguide.android.util.GlobalInstance;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;


public class HikingTrail implements Serializable {
    private static final long serialVersionUID = 1l;

    private long relationId;
    private String name;
    private int distanceToClosest, distanceToStart, distanceToDestination;
    private String description, length, symbol;

    public HikingTrail(JSONObject jsonHikingTrail) throws JSONException {
        this.relationId = jsonHikingTrail.getLong(KEY_RELATION_ID);
        this.name = jsonHikingTrail.getString(KEY_NAME);
        // distances to current position
        this.distanceToClosest = jsonHikingTrail.getInt(KEY_DISTANCE_TO_CLOSEST);
        this.distanceToStart = jsonHikingTrail.getInt(KEY_DISTANCE_TO_START);
        this.distanceToDestination = jsonHikingTrail.getInt(KEY_DISTANCE_TO_DESTINATION);
        // optional
        this.description = StringUtility.getNullableStringFromJsonObject(jsonHikingTrail, KEY_DESCRIPTION);
        this.length = StringUtility.getNullableStringFromJsonObject(jsonHikingTrail, KEY_LENGTH);
        this.symbol = StringUtility.getNullableStringFromJsonObject(jsonHikingTrail, KEY_SYMBOL);
    }

    public long getRelationId() {
        return this.relationId;
    }

    public String getName() {
        return this.name;
    }

	public int getDistanceToClosest() {
		return this.distanceToClosest;
	}

    public int getDistanceToStart() {
        return this.distanceToStart;
    }

    public int getDistanceToDestination() {
        return this.distanceToDestination;
    }

    public String getDescription() {
        return this.description;
    }

    public String getLength() {
        return this.length;
    }

    public String getSymbol() {
        return this.symbol;
    }

    /**
     * super class methods
     */

    @Override public String toString() {
        return String.format(
                GlobalInstance.getStringResource(R.string.hikingTrailToString),
                this.name,
                GlobalInstance.getPluralResource(R.plurals.meter, this.distanceToClosest));
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + String.valueOf(this.relationId).hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof HikingTrail))
            return false;
        HikingTrail other = (HikingTrail) obj;
        return this.relationId == other.getRelationId();
    }


    /**
     * to json
     */

    private static final String KEY_RELATION_ID = "relation_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_DISTANCE_TO_CLOSEST = "distance_to_closest";
    private static final String KEY_DISTANCE_TO_START = "distance_to_start";
    private static final String KEY_DISTANCE_TO_DESTINATION = "distance_to_destination";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_LENGTH = "trail_length";
    private static final String KEY_SYMBOL = "symbol";

    public JSONObject toJson() throws JSONException {
        JSONObject jsonHikingTrail = new JSONObject();
        jsonHikingTrail.put(KEY_RELATION_ID, this.relationId);
        jsonHikingTrail.put(KEY_NAME, this.name);
        jsonHikingTrail.put(KEY_DISTANCE_TO_CLOSEST, this.distanceToClosest);
        jsonHikingTrail.put(KEY_DISTANCE_TO_START, this.distanceToStart);
        jsonHikingTrail.put(KEY_DISTANCE_TO_DESTINATION, this.distanceToDestination);
        // optional
        if (this.description != null) {
            jsonHikingTrail.put(KEY_DESCRIPTION, this.description);
        }
        if (this.length != null) {
            jsonHikingTrail.put(KEY_LENGTH, this.length);
        }
        if (this.symbol != null) {
            jsonHikingTrail.put(KEY_SYMBOL, this.symbol);
        }
        return jsonHikingTrail;
    }

}
