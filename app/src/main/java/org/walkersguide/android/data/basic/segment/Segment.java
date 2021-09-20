package org.walkersguide.android.data.basic.segment;

import org.walkersguide.android.database.profiles.DatabaseSegmentProfile;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;

import org.walkersguide.android.R;
import org.walkersguide.android.data.TactilePaving;
import org.walkersguide.android.data.Wheelchair;
import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.util.GlobalInstance;
import java.util.HashMap;
import java.util.Map;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.util.AccessDatabase;
import android.text.TextUtils;


public abstract class Segment extends ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;


    /**
     * object creation helpers
     */

    public enum Type {
        INTERSECTION, ROUTE
    }

    public static Segment create(JSONObject jsonSegment) throws JSONException {
        String type = jsonSegment.optString(KEY_TYPE, "").toUpperCase();
        if (Type.INTERSECTION.name().equals(type)) {
            return new IntersectionSegment(jsonSegment);
        } else if (Type.ROUTE.name().equals(type)) {
            return new RouteSegment(jsonSegment);
        } else {
            throw new JSONException("Invalid segment type");
        }
    }

    public static Segment load(long segmentId) {
        return AccessDatabase.getInstance().getSegment(segmentId);
    }

    public abstract static class Builder {
        public JSONObject inputData;
        public Builder(Type type, String name, String subType, int bearing) {
            this.inputData = new JSONObject();
            try {
                this.inputData.put(KEY_TYPE, type.name().toLowerCase());
                this.inputData.put(KEY_NAME, name);
                this.inputData.put(KEY_SUB_TYPE, subType);
                this.inputData.put(KEY_BEARING, bearing);
            } catch (JSONException e) {}
        }
        public Builder overwriteName(final String name) {
            try {
                inputData.put(KEY_NAME, name);
            } catch (JSONException e) {}
            return this;
        }
    }


    /**
     * constructor
     */

    // mandatory params
    private String name, type, subType;
    private int bearing;

    // optional params
    private Integer lanes, maxSpeed;
    private Double width;
    private String description, smoothness, surface;
    private Boolean segregated, tram;
    private Sidewalk sidewalk;
    private TactilePaving tactilePaving;
    private Wheelchair wheelchair;

    public Segment(JSONObject inputData) throws JSONException {
        super(StringUtility.getNullableAndPositiveLongFromJsonObject(inputData, KEY_ID));

        this.name = inputData.getString("name");
        this.type = inputData.getString("type");
        this.subType = inputData.getString("sub_type");
        this.bearing = inputData.getInt(KEY_BEARING);

        // optional parameters
        this.lanes = StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_LANES);
        this.maxSpeed = StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_MAX_SPEED);
        this.width = StringUtility.getNullableAndPositiveDoubleFromJsonObject(inputData, KEY_WIDTH);
        this.description = StringUtility.getNullableStringFromJsonObject(inputData, KEY_DESCRIPTION);
        this.smoothness = StringUtility.getNullableStringFromJsonObject(inputData, KEY_SMOOTHNESS);
        this.surface = StringUtility.getNullableStringFromJsonObject(inputData, KEY_SURFACE);

        // int to bool
        Integer segregatedInteger = StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_SEGREGATED);
        if (segregatedInteger != null) {
            this.segregated = segregatedInteger == 1 ? true : false;
        } else {
            this.segregated = null;
        }
        Integer tramInteger = StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_TRAM);
        if (tramInteger != null) {
            this.tram = tramInteger == 1 ? true : false;
        } else {
            this.tram = null;
        }

        this.sidewalk = Sidewalk.lookUpById(
                StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_SIDEWALK));
        this.tactilePaving = TactilePaving.lookUpById(
                StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_TACTILE_PAVING));
        this.wheelchair = Wheelchair.lookUpById(
                StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_WHEELCHAIR));
    }

    // mandatory

    public String getName() {
        String customName = getCustomName();
        if (! TextUtils.isEmpty(customName)) {
            return customName;
        }
        return getOriginalName();
    }

    public String getCustomName() {
        return AccessDatabase
            .getInstance()
            .getSegmentCustomName(this.getId());
    }

    public String getOriginalName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public String getSubType() {
        return this.subType;
    }

    public int getBearing() {
        return this.bearing;
    }


    // optional

    public Integer getLanes() {
        return this.lanes;
    }

    public Integer getMaxSpeed() {
        return this.maxSpeed;
    }

    public Double getWidth() {
        return this.width;
    }

    public String getDescription() {
        return this.description;
    }

    public String getSmoothness() {
        return this.smoothness;
    }

    public String getSurface() {
        return this.surface;
    }

    public Boolean getSegregated() {
        return this.segregated;
    }

    public Boolean getTramOnStreet() {
        return this.tram;
    }

    public TactilePaving getTactilePaving() {
        return this.tactilePaving;
    }

    public Wheelchair getWheelchair() {
        return this.wheelchair;
    }


    /**
     * sidewalk
     */

    public enum Sidewalk {
        NO(0, GlobalInstance.getStringResource(R.string.sidewalkNo)),
        LEFT(1, GlobalInstance.getStringResource(R.string.sidewalkLeft)),
        RIGHT(2, GlobalInstance.getStringResource(R.string.sidewalkRight)),
        BOTH(3, GlobalInstance.getStringResource(R.string.sidewalkBoth));

        // lookup by id
        private static final Map<Integer,Sidewalk> valuesById;
        static {
            valuesById = new HashMap<Integer,Sidewalk>();
            for(Sidewalk sidewalk : Sidewalk.values()) {
                valuesById.put(sidewalk.id, sidewalk);
            }
        }

        public static Sidewalk lookUpById(Integer id) {
            return valuesById.get(id);
        }

        // constructor
        public int id;
        public String name;

        private Sidewalk(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override public String toString() {
            return this.name;
        }
    }

    public Sidewalk getSidewalk() {
        return this.sidewalk;
    }


    /**
     * helper functions
     */

    public boolean isFavorite() {
        return super.isFavorite(DatabaseSegmentProfile.FAVORITES);
    }

    public boolean isExcludedFromRouting() {
        return AccessDatabase
            .getInstance()
            .getDatabaseProfileListFor(this)
            .contains(DatabaseSegmentProfile.EXCLUDED_FROM_ROUTING);
    }

    public Integer bearingFromCurrentDirection() {
        return bearingFromCurrentDirection(
                DirectionManager.getInstance().getCurrentDirection());
    }

    public Integer bearingFromCurrentDirection(Direction currentDirection) {
        if (currentDirection != null) {
            int absoluteDirection = this.bearing;
            // take the current viewing direction into account
            int relativeDirection = absoluteDirection - currentDirection.getBearing();
            if (relativeDirection < 0) {
                relativeDirection += 360;
            }
            return relativeDirection;
        }
        return null;
    }


    /**
     * to json
     */

    // mandatory params
    public static final String KEY_NAME = "name";
    public static final String KEY_TYPE = "type";
    public static final String KEY_SUB_TYPE = "sub_type";
    public static final String KEY_BEARING = "bearing";

    public static final String KEY_ID = "way_id";
    // optional  params
    public static final String KEY_LANES = "lanes";
    public static final String KEY_MAX_SPEED = "maxspeed";
    public static final String KEY_WIDTH = "width";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_SMOOTHNESS = "smoothness";
    public static final String KEY_SURFACE = "surface";
    public static final String KEY_SEGREGATED = "segregated";
    public static final String KEY_TRAM = "tram";
    public static final String KEY_SIDEWALK = "sidewalk";
    public static final String KEY_TACTILE_PAVING = "tactile_paving";
    public static final String KEY_WHEELCHAIR = "wheelchair";

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_ID, this.getId());

        // mandatory params
        jsonObject.put(KEY_NAME, this.name);
        jsonObject.put(KEY_TYPE, this.type);
        jsonObject.put(KEY_SUB_TYPE, this.subType);
        jsonObject.put(KEY_BEARING, this.bearing);

        if (this.lanes != null) {
            jsonObject.put(KEY_LANES, this.lanes);
        }
        if (this.maxSpeed != null) {
            jsonObject.put(KEY_MAX_SPEED, this.maxSpeed);
        }
        if (this.width != null) {
            jsonObject.put(KEY_WIDTH, this.width);
        }
        if (this.description != null) {
            jsonObject.put(KEY_DESCRIPTION, this.description);
        }
        if (this.smoothness != null) {
            jsonObject.put(KEY_SMOOTHNESS, this.smoothness);
        }
        if (this.surface != null) {
            jsonObject.put(KEY_SURFACE, this.surface);
        }

        // bool to int
        if (this.segregated != null) {
            jsonObject.put(KEY_SEGREGATED, this.segregated ? 1 : 0);
        }
        if (this.tram != null) {
            jsonObject.put(KEY_TRAM, this.tram ? 1 : 0);
        }

        // enums
        if (this.sidewalk != null) {
            jsonObject.put(KEY_SIDEWALK, this.sidewalk.id);
        }
        if (this.tactilePaving != null) {
            jsonObject.put(KEY_TACTILE_PAVING, this.tactilePaving.id);
        }
        if (this.wheelchair != null) {
            jsonObject.put(KEY_WHEELCHAIR, this.wheelchair.id);
        }

        return jsonObject;
    }

    public static JSONObject addWayIdToJsonObject(JSONObject jsonSegment) throws JSONException {
        if (jsonSegment.isNull("way_id")) {
            jsonSegment.put("way_id", ObjectWithId.generateId());
        }
        return jsonSegment;
    }

}
