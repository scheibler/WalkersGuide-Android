package org.walkersguide.android.data.object_with_id;

import org.walkersguide.android.database.profile.FavoritesProfile;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.util.GlobalInstance;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.util.AccessDatabase;


public class HikingTrail extends ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;


    /**
     * object creation helpers
     */

    public static HikingTrail create(JSONObject jsonHikingTrail) throws JSONException {
        return new HikingTrail(jsonHikingTrail);
    }

    public static HikingTrail load(long id) {
        ObjectWithId object = AccessDatabase.getInstance().getObjectWithId(id);
        if (object instanceof HikingTrail) {
            return (HikingTrail) object;
        }
        return null;
    }


    /**
     * constructor
     */

    private String name;
    private int distanceToClosest, distanceToStart, distanceToDestination;
    private String description, length, symbol;

    public HikingTrail(JSONObject inputData) throws JSONException {
        super(Helper.getNullableAndPositiveLongFromJsonObject(inputData, KEY_ID));
        this.name = inputData.getString(KEY_NAME);

        // distances to current position
        this.distanceToClosest = inputData.getInt(KEY_DISTANCE_TO_CLOSEST);
        this.distanceToStart = inputData.getInt(KEY_DISTANCE_TO_START);
        this.distanceToDestination = inputData.getInt(KEY_DISTANCE_TO_DESTINATION);

        // optional
        this.description = Helper.getNullableStringFromJsonObject(inputData, KEY_DESCRIPTION);
        this.length = Helper.getNullableStringFromJsonObject(inputData, KEY_LENGTH);
        this.symbol = Helper.getNullableStringFromJsonObject(inputData, KEY_SYMBOL);
    }

    public String getOriginalName() {
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

    @Override public FavoritesProfile getDefaultFavoritesProfile() {
        return null;
    }

    @Override public String toString() {
        return String.format(
                GlobalInstance.getStringResource(R.string.hikingTrailToString),
                getName(),
                GlobalInstance.getPluralResource(R.plurals.meter, this.distanceToClosest));
    }


    /**
     * to json
     */

    private static final String KEY_ID = "relation_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_DISTANCE_TO_CLOSEST = "distance_to_closest";
    private static final String KEY_DISTANCE_TO_START = "distance_to_start";
    private static final String KEY_DISTANCE_TO_DESTINATION = "distance_to_destination";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_LENGTH = "trail_length";
    private static final String KEY_SYMBOL = "symbol";

    public JSONObject toJson() throws JSONException {
        JSONObject jsonHikingTrail = new JSONObject();
        jsonHikingTrail.put(KEY_ID, this.getId());
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
