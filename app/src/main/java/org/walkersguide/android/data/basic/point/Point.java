package org.walkersguide.android.data.basic.point;

import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.data.TactilePaving;
import org.walkersguide.android.data.Wheelchair;
import org.walkersguide.android.database.profiles.DatabasePointProfile;

import android.location.Location;
import android.location.LocationManager;


import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import java.io.Serializable;
import org.walkersguide.android.data.sensor.Direction;
import java.util.Map;
import java.util.HashMap;
import org.walkersguide.android.util.StringUtility;
import android.text.TextUtils;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.database.util.AccessDatabase;
import timber.log.Timber;


public class Point extends ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;


    /**
     * object creation helpers
     */

    public enum Type {
        POINT, ENTRANCE, GPS, INTERSECTION, PEDESTRIAN_CROSSING, POI, STATION, STREET_ADDRESS
    }

    public static Point create(JSONObject jsonPoint) throws JSONException {
        String type = jsonPoint.optString(KEY_TYPE, "").toUpperCase();
        if (Type.POINT.name().equals(type)) {
            return new Point(jsonPoint);
        } else if (Type.ENTRANCE.name().equals(type)) {
            return new Entrance(jsonPoint);
        } else if (Type.GPS.name().equals(type)) {
            return new GPS(jsonPoint);
        } else if (Type.INTERSECTION.name().equals(type)) {
            return new Intersection(jsonPoint);
        } else if (Type.PEDESTRIAN_CROSSING.name().equals(type)) {
            return new PedestrianCrossing(jsonPoint);
        } else if (Type.POI.name().equals(type)) {
            return new POI(jsonPoint);
        } else if (Type.STATION.name().equals(type)) {
            return new Station(jsonPoint);
        } else if (Type.STREET_ADDRESS.name().equals(type)) {
            return new StreetAddress(jsonPoint);
        } else {
            throw new JSONException("Invalid point type");
        }
    }

    public static Point load(long id) {
        return AccessDatabase.getInstance().getPoint(id);
    }


    public abstract static class Builder {
        public JSONObject inputData;
        public Builder(Type type, String name, String subType, double latitude, double longitude) {
            this.inputData = new JSONObject();
            try {
                this.inputData.put(KEY_TYPE, type.name().toLowerCase());
                this.inputData.put(KEY_NAME, name);
                this.inputData.put(KEY_SUB_TYPE, subType);
                this.inputData.put(KEY_LATITUDE, latitude);
                this.inputData.put(KEY_LONGITUDE, longitude);
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
    private double latitude, longitude;
    // optional params
    private String altName, oldName, note;
    private TactilePaving tactilePaving;
    private Wheelchair wheelchair;

    public Point(JSONObject inputData) throws JSONException {
        super(StringUtility.getNullableAndPositiveLongFromJsonObject(inputData, KEY_ID));

        // mandatory params
        this.name = inputData.getString(KEY_NAME);
        this.type = inputData.getString(KEY_TYPE);
        this.subType = inputData.getString(KEY_SUB_TYPE);
        this.latitude = inputData.getDouble(KEY_LATITUDE);
        this.longitude = inputData.getDouble(KEY_LONGITUDE);

        // optional parameters
        this.altName = StringUtility.getNullableStringFromJsonObject(inputData, KEY_ALT_NAME);
        this.oldName = StringUtility.getNullableStringFromJsonObject(inputData, KEY_OLD_NAME);
        this.note = StringUtility.getNullableStringFromJsonObject(inputData, KEY_NOTE);
        this.tactilePaving = TactilePaving.lookUpById(
                StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_TACTILE_PAVING));
        this.wheelchair = Wheelchair.lookUpById(
                StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_WHEELCHAIR));
    }


    /**
     * mandatory params
     */

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
            .getPointCustomName(this.getId());
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


    /**
     * optional params
     */

    public String getAltName() {
        return this.altName;
    }

    public String getOldName() {
        return this.oldName;
    }

    public String getNote() {
        return this.note;
    }

    public TactilePaving getTactilePaving() {
        return this.tactilePaving;
    }

    public Wheelchair getWheelchair() {
        return this.wheelchair;
    }


    /**
     * helper functions
     */

    public String formatCoordinates() {
        return String.format(
                "%1$s: %2$f, %3$f",
                GlobalInstance.getStringResource(R.string.labelGPSCoordinates),
                this.latitude,
                this.longitude);
    }

    public String formatLatitude() {
        return String.format(
                "%1$s: %2$f",
                GlobalInstance.getStringResource(R.string.labelGPSLatitude),
                this.latitude);
    }

    public String formatLongitude() {
        return String.format(
                "%1$s: %2$f",
                GlobalInstance.getStringResource(R.string.labelGPSLongitude),
                this.longitude);
    }

    public boolean isFavorite() {
        return super.isFavorite(DatabasePointProfile.FAVORITES);
    }

    public int distanceTo(Point other) {
        if (other != null) {
            return (int) Math.round(
                    this.getLocationObject().distanceTo(other.getLocationObject()));
        }
        return -1;
    }

    public int bearingTo(Point other) {
        if (other != null) {
            // bearing to north
            int absoluteDirection = (int) Math.round(
                    this.getLocationObject().bearingTo(other.getLocationObject()));
            if (absoluteDirection < 0) {
                absoluteDirection += 360;
            }
            return absoluteDirection;
        }
        return -1;
    }

    public Integer distanceFromCurrentLocation() {
        Point currentLocation = PositionManager.getInstance().getCurrentLocation();
        if (currentLocation != null) {
            return currentLocation.distanceTo(this);
        }
        return null;
    }

    public Integer bearingFromCurrentLocation() {
        return bearingFromCurrentLocation(
                DirectionManager.getInstance().getCurrentDirection());
    }

    public Integer bearingFromCurrentLocation(Direction currentDirection) {
        Point currentLocation = PositionManager.getInstance().getCurrentLocation();
        if (currentLocation != null && currentDirection != null) {
            int absoluteDirection = currentLocation.bearingTo(this);
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
     * super class
     */

    @Override public String toString() {
        String description = super.toString();
        // add distance and bearing from current location
        Integer distanceFromCurrentLocation = distanceFromCurrentLocation();
        Integer bearingFromCurrentLocation = bearingFromCurrentLocation();
        if (distanceFromCurrentLocation !=null && bearingFromCurrentLocation != null) {
            description += String.format(
                    "\n%1$s, %2$s",
                    GlobalInstance.getPluralResource(R.plurals.inMeters, distanceFromCurrentLocation),
                    StringUtility.formatRelativeViewingDirection(bearingFromCurrentLocation));
        }
        return description;
    }


    /**
     * to json
     */

    public static final String KEY_ID = "node_id";
    // mandatory params
    public static final String KEY_NAME = "name";
    public static final String KEY_TYPE = "type";
    public static final String KEY_SUB_TYPE = "sub_type";
    public static final String KEY_LATITUDE = "lat";
    public static final String KEY_LONGITUDE = "lon";
    // optional params
    public static final String KEY_TACTILE_PAVING = "tactile_paving";
    public static final String KEY_WHEELCHAIR = "wheelchair";
    public static final String KEY_ALT_NAME = "alt_name";
    public static final String KEY_OLD_NAME = "old_name";
    public static final String KEY_NOTE = "note";

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_ID, this.getId());

        // mandatory params
        jsonObject.put(KEY_NAME, this.name);
        jsonObject.put(KEY_TYPE, this.type);
        jsonObject.put(KEY_SUB_TYPE, this.subType);
        jsonObject.put(KEY_LATITUDE, this.latitude);
        jsonObject.put(KEY_LONGITUDE, this.longitude);

        // optional parameters
        if (this.altName != null) {
            jsonObject.put(KEY_ALT_NAME, this.altName);
        }
        if (this.oldName != null) {
            jsonObject.put(KEY_OLD_NAME, this.oldName);
        }
        if (this.note != null) {
            jsonObject.put(KEY_NOTE, this.note);
        }
        if (this.tactilePaving != null) {
            jsonObject.put(KEY_TACTILE_PAVING, this.tactilePaving.id);
        }
        if (this.wheelchair != null) {
            jsonObject.put(KEY_WHEELCHAIR, this.wheelchair.id);
        }

        return jsonObject;
    }

    public static JSONObject addNodeIdToJsonObject(JSONObject jsonPoint) throws JSONException {
        if (jsonPoint.isNull("node_id")) {
            jsonPoint.put("node_id", ObjectWithId.generateId());
        }
        return jsonPoint;
    }

}
