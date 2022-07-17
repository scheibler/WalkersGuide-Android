package org.walkersguide.android.data.object_with_id;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.database.profile.FavoritesProfile;

import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.data.object_with_id.segment.RouteSegment;
import org.walkersguide.android.data.angle.Bearing;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;

import org.walkersguide.android.R;
import org.walkersguide.android.data.object_with_id.common.TactilePaving;
import org.walkersguide.android.data.object_with_id.common.Wheelchair;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.util.GlobalInstance;
import java.util.HashMap;
import java.util.Map;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.util.AccessDatabase;
import android.text.TextUtils;
import java.util.Locale;


public abstract class Segment extends ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;


    /**
     * object creation helpers
     */

    protected enum Type {
        FOOTWAY_INTERSECTION, FOOTWAY_ROUTE;

        public static Type fromString(String name) {
            for (Type type : Type.values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }

        @Override public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public static Segment create(JSONObject jsonSegment) throws JSONException {
        Type type = Type.fromString(jsonSegment.optString(KEY_TYPE, ""));
        if (type != null) {
            switch (type) {
                case FOOTWAY_INTERSECTION:
                    return new IntersectionSegment(jsonSegment);
                case FOOTWAY_ROUTE:
                    return new RouteSegment(jsonSegment);
            }
        }
        throw new JSONException("Invalid segment type");
    }

    public static Segment load(long id) {
        ObjectWithId object = AccessDatabase.getInstance().getObjectWithId(id);
        if (object instanceof Segment) {
            return (Segment) object;
        }
        return null;
    }


    /**
     * constructor
     */

    // mandatory params
    private String name, type, subType;
    private Bearing bearing;

    // optional params
    private Integer lanes, maxSpeed;
    private Double width;
    private String description, smoothness, surface;
    private Boolean bewareBicyclists, segregated, tram;
    private Sidewalk sidewalk;
    private TactilePaving tactilePaving;
    private Wheelchair wheelchair;

    public Segment(JSONObject inputData) throws JSONException {
        super(Helper.getNullableAndPositiveLongFromJsonObject(inputData, KEY_ID));

        this.name = inputData.getString("name");
        this.type = inputData.getString("type");
        this.subType = inputData.getString("sub_type");
        this.bearing = new Bearing(inputData.getInt(KEY_BEARING));

        // optional parameters
        this.lanes = Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_LANES);
        this.maxSpeed = Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_MAX_SPEED);
        this.width = Helper.getNullableAndPositiveDoubleFromJsonObject(inputData, KEY_WIDTH);
        this.description = Helper.getNullableStringFromJsonObject(inputData, KEY_DESCRIPTION);
        this.smoothness = Helper.getNullableStringFromJsonObject(inputData, KEY_SMOOTHNESS);
        this.surface = Helper.getNullableStringFromJsonObject(inputData, KEY_SURFACE);

        // int to bool
        Integer bewareBicyclistsInteger = Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_BEWARE_BICYCLISTS);
        if (bewareBicyclistsInteger != null) {
            this.bewareBicyclists = bewareBicyclistsInteger == 1 ? true : false;
        } else {
            this.bewareBicyclists = null;
        }
        Integer segregatedInteger = Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_SEGREGATED);
        if (segregatedInteger != null) {
            this.segregated = segregatedInteger == 1 ? true : false;
        } else {
            this.segregated = null;
        }
        Integer tramInteger = Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_TRAM);
        if (tramInteger != null) {
            this.tram = tramInteger == 1 ? true : false;
        } else {
            this.tram = null;
        }

        this.sidewalk = Sidewalk.lookUpById(
                Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_SIDEWALK));
        this.tactilePaving = TactilePaving.lookUpById(
                Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_TACTILE_PAVING));
        this.wheelchair = Wheelchair.lookUpById(
                Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_WHEELCHAIR));
    }

    // mandatory

    public String getOriginalName() {
        return this.name;
    }

    public Bearing getBearing() {
        return this.bearing;
    }

    public String getSubType() {
        return this.subType;
    }

    public String formatNameAndSubType() {
        String customOrOriginalName = getName();
        if (! TextUtils.isEmpty(getSubType())
                && ! customOrOriginalName.toLowerCase(Locale.getDefault())
                        .contains(getSubType().toLowerCase(Locale.getDefault()))) {
            return String.format("%1$s (%2$s)", customOrOriginalName, getSubType());
        }
        return customOrOriginalName;
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

    public Boolean getBewareBicyclists() {
        return this.bewareBicyclists;
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
     * include or exclude from route calculation
     */
    public static final String ACTION_EXCLUDED_FROM_ROUTING_STATUS_CHANGED = "excludedFromRoutingStatusChanged";

    public boolean isExcludedFromRouting() {
        return DatabaseProfile.excludedRoutingSegments().contains(this);
    }

    public boolean excludeFromRouting() {
        boolean success = DatabaseProfile.excludedRoutingSegments().add(this);
        if (success) {
            Intent excludedFromRoutingStatusChangedIntent = new Intent(ACTION_EXCLUDED_FROM_ROUTING_STATUS_CHANGED);
            LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(excludedFromRoutingStatusChangedIntent);
        }
        return success;
    }

    public boolean includeIntoRouting() {
        boolean success = DatabaseProfile.excludedRoutingSegments().remove(this);
        if (success) {
            Intent excludedFromRoutingStatusChangedIntent = new Intent(ACTION_EXCLUDED_FROM_ROUTING_STATUS_CHANGED);
            LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(excludedFromRoutingStatusChangedIntent);
        }
        return success;
    }


    /**
     * super class methods
     */

    @Override public FavoritesProfile getDefaultFavoritesProfile() {
        return null;
    }

    @Override public String toString() {
        return formatNameAndSubType();
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
    public static final String KEY_BEWARE_BICYCLISTS = "beware_bicyclists";
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
        jsonObject.put(KEY_BEARING, this.bearing.getDegree());

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
        if (this.bewareBicyclists != null) {
            jsonObject.put(KEY_BEWARE_BICYCLISTS, this.bewareBicyclists ? 1 : 0);
        }
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
