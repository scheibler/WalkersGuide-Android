package org.walkersguide.android.data.object_with_id;

import timber.log.Timber;
import org.walkersguide.android.data.ObjectWithId.Icon;
import org.walkersguide.android.util.GlobalInstance;

import org.walkersguide.android.data.angle.Bearing;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;

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
import org.walkersguide.android.sensor.DeviceSensorManager;


public class Segment extends ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;

    public enum Type {
        SEGMENT, FOOTWAY_INTERSECTION, FOOTWAY_ROUTE
    }


    /**
     * object creation helpers
     */

    public static Segment fromJson(JSONObject jsonObject) throws JSONException {
        return castToSegmentOrReturnNull(ObjectWithId.fromJson(jsonObject));
    }

    public static Segment load(long id) {
        return castToSegmentOrReturnNull(ObjectWithId.load(id));
    }

    private static Segment castToSegmentOrReturnNull(ObjectWithId objectWithId) {
        return objectWithId instanceof Segment ? (Segment) objectWithId : null;
    }


    /**
     * constructor
     */

    // mandatory params
    private String subType;
    private Bearing bearing;
    private Coordinates startCoordinates, endCoordinates;

    // optional params
    private Integer lanes, maxSpeed;
    private Double width;
    private String smoothness, surface;
    private Boolean bewareBicyclists, segregated, tram;
    private Sidewalk sidewalk;
    private TactilePaving tactilePaving;
    private Wheelchair wheelchair;

    public Segment(JSONObject inputData) throws JSONException {
        super(
                Helper.getNullableAndPositiveLongFromJsonObject(inputData, KEY_ID),
                Helper.getNullableEnumFromJsonObject(inputData, ObjectWithId.KEY_TYPE, Type.class),
                inputData);
        this.subType = inputData.getString(KEY_SUB_TYPE);
        this.bearing = new Bearing(inputData.getInt(KEY_BEARING));

        // start and end coordinates
        try {
            this.startCoordinates = new Coordinates(inputData.getJSONObject(KEY_START));
        } catch (JSONException e) {
            Timber.d("no start coordinates, using 0.0, 0.0");
            this.startCoordinates = new Coordinates(0.0, 0.0);
        }
        try {
            this.endCoordinates = new Coordinates(inputData.getJSONObject(KEY_END));
        } catch (JSONException e) {
            Timber.d("no end Coordinates, using startCoordinates=%1$s and angle=%2$s", this.startCoordinates, this.bearing);
            this.endCoordinates = Helper.calculateEndCoordinatesForStartCoordinatesAndAngle(this.startCoordinates, this.bearing);
        }
        //Timber.d("Segment: bearing=%1$d/%2$f, start=%3$s, end=%4$s", this.bearing.getDegree(), this.startCoordinates.bearingTo(this.endCoordinates), this.startCoordinates, this.endCoordinates);

        // optional parameters
        this.lanes = Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_LANES);
        this.maxSpeed = Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_MAX_SPEED);
        this.width = Helper.getNullableAndPositiveDoubleFromJsonObject(inputData, KEY_WIDTH);
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
     * super class methods
     */

    @Override public Type getType() {
        return (Type) super.getType();
    }

    @Override public Icon getIcon() {
        return Icon.SEGMENT;
    }

    @Override public Coordinates getCoordinates() {
        Point currentLocation = PositionManager.getInstance().getCurrentLocation();
        if (currentLocation != null) {
            int distanceToStartCoordinates = currentLocation.getCoordinates().distanceTo(this.startCoordinates);
            int distanceToEndCoordinates = currentLocation.getCoordinates().distanceTo(this.endCoordinates);
            return distanceToStartCoordinates < distanceToEndCoordinates
                ? this.startCoordinates
                : this.endCoordinates;
        }
        return null;
    }

    @Override public String toString() {
        return formatNameAndSubType();
    }


    public static class SortByBearingRelativeTo implements Comparator<Segment> {
        private Bearing initialViewingDirection;
        private int offsetInDegree;
        private boolean ascending;

        public static SortByBearingRelativeTo currentBearing(int offsetInDegree, boolean ascending) {
            return new SortByBearingRelativeTo(
                    DeviceSensorManager.getInstance().getCurrentBearing(),
                    offsetInDegree, ascending);
        }

        public SortByBearingRelativeTo(Bearing initialViewingDirection, int offsetInDegree, boolean ascending) {
            this.initialViewingDirection = initialViewingDirection;
            this.offsetInDegree = offsetInDegree;
            this.ascending = ascending;
        }

        @Override public int compare(Segment segment1, Segment segment2) {
            RelativeBearing bearing1 = segment1.getBearing().relativeTo(initialViewingDirection);;
            RelativeBearing bearing2 = segment2.getBearing().relativeTo(initialViewingDirection);;
            if (bearing1 != null && bearing2 != null) {
                if (this.offsetInDegree != 0) {
                    bearing1 = bearing1.shiftBy(this.offsetInDegree);
                    bearing2 = bearing2.shiftBy(this.offsetInDegree);
                }
                if (this.ascending) {
                    return bearing1.compareTo(bearing2);
                } else {
                    return bearing2.compareTo(bearing1);
                }
            }
            return 1;
        }
    }


    /**
     * to json
     */

    // mandatory params
    public static final String KEY_SUB_TYPE = "sub_type";
    public static final String KEY_BEARING = "bearing";
    public static final String KEY_START = "start";
    public static final String KEY_END = "end";

    public static final String KEY_ID = "way_id";
    // optional  params
    public static final String KEY_LANES = "lanes";
    public static final String KEY_MAX_SPEED = "maxspeed";
    public static final String KEY_WIDTH = "width";
    public static final String KEY_SMOOTHNESS = "smoothness";
    public static final String KEY_SURFACE = "surface";
    public static final String KEY_BEWARE_BICYCLISTS = "beware_bicyclists";
    public static final String KEY_SEGREGATED = "segregated";
    public static final String KEY_TRAM = "tram";
    public static final String KEY_SIDEWALK = "sidewalk";
    public static final String KEY_TACTILE_PAVING = "tactile_paving";
    public static final String KEY_WHEELCHAIR = "wheelchair";

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        jsonObject.put(KEY_ID, this.getId());

        // mandatory params
        jsonObject.put(KEY_SUB_TYPE, this.subType);
        jsonObject.put(KEY_BEARING, this.bearing.getDegree());
        jsonObject.put(KEY_START, this.startCoordinates.toJson());
        jsonObject.put(KEY_END, this.endCoordinates.toJson());

        if (this.lanes != null) {
            jsonObject.put(KEY_LANES, this.lanes);
        }
        if (this.maxSpeed != null) {
            jsonObject.put(KEY_MAX_SPEED, this.maxSpeed);
        }
        if (this.width != null) {
            jsonObject.put(KEY_WIDTH, this.width);
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

}
