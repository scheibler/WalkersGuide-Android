package org.walkersguide.android.data;

import java.text.Collator;
import org.walkersguide.android.data.object_with_id.common.Coordinates;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.Entrance;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.object_with_id.point.PedestrianCrossing;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.POI;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.poi.Station;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;

import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.data.object_with_id.segment.RouteSegment;
import org.walkersguide.android.R;
import org.walkersguide.android.data.angle.RelativeBearing;
import java.util.Comparator;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.Segment;

import java.io.Serializable;
import org.walkersguide.android.util.Helper;
import java.util.Random;
import android.text.TextUtils;
import org.walkersguide.android.database.util.AccessDatabase;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Locale;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.Route;
import java.util.ArrayList;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.database.profile.Collection;
import org.walkersguide.android.database.DatabaseProfile;


public abstract class ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;

    public static class ObjectWithIdParams {
        public long id;
        public String data, customName, userAnnotation;
    }


    public enum Icon {

        POINT(
                R.drawable.image_object_type_point,
                GlobalInstance.getStringResource(R.string.objectIconPoint)),
        SEGMENT(
                R.drawable.image_object_type_segment,
                GlobalInstance.getStringResource(R.string.objectIconSegment)),
        ROUTE(
                R.drawable.image_object_type_route,
                GlobalInstance.getStringResource(R.string.objectIconRoute));

        public int resId;
        public String name;

        private Icon(int resId, String name) {
            this.resId = resId;
            this.name = name;
        }

        @Override public String toString() {
            return this.name;
        }
    }


    public static String summarizeObjectListContents(ArrayList<ObjectWithId> objectList) {
        // count
        int points = 0, segments = 0, routes = 0;
        for (ObjectWithId object : objectList) {
            if (object instanceof Point) {
                points++;
            } else if (object instanceof Segment) {
                segments++;
            } else if (object instanceof Route) {
                routes++;
            }
        }
        // format strings
        ArrayList<String> stringList = new ArrayList<String>();
        if (segments > 0) {
            stringList.add(
                    GlobalInstance.getPluralResource(R.plurals.way, segments));
        }
        if (routes > 0) {
            stringList.add(
                    GlobalInstance.getPluralResource(R.plurals.route, routes));
        }
        if (points > 0                      // must come last
                || stringList.isEmpty()) {  // special case for the empty list: return string "0 points"
            stringList.add(
                    0,      // put on top of the stringList
                    GlobalInstance.getPluralResource(R.plurals.point, points));
        }
        // concatenate and return
        return Helper.formatStringListWithFillWordAnd(stringList);
    }


    /**
     * object creation helpers
     */

    public static ObjectWithId fromJson(JSONObject jsonObject) throws JSONException {
        Point.Type pointType = Helper.getNullableEnumFromJsonObject(
                jsonObject, KEY_TYPE, Point.Type.class);
        if (pointType != null) {
            switch (pointType) {
                case POINT:
                    return new Point(jsonObject);
                case ENTRANCE:
                    return new Entrance(jsonObject);
                case GPS:
                    return new GPS(jsonObject);
                case INTERSECTION:
                    return new Intersection(jsonObject);
                case PEDESTRIAN_CROSSING:
                    return new PedestrianCrossing(jsonObject);
                case POI:
                    return new POI(jsonObject);
                case STATION:
                    return new Station(jsonObject);
                case STREET_ADDRESS:
                    return new StreetAddress(jsonObject);
            }
        }

        Segment.Type segmentType = Helper.getNullableEnumFromJsonObject(
                jsonObject, KEY_TYPE, Segment.Type.class);
        if (segmentType != null) {
            switch (segmentType) {
                case SEGMENT:
                    return new Segment(jsonObject);
                case FOOTWAY_INTERSECTION:
                    return new IntersectionSegment(jsonObject);
                case FOOTWAY_ROUTE:
                    return new RouteSegment(jsonObject);
            }
        }

        Route.Type routeType = Helper.getNullableEnumFromJsonObject(
                jsonObject, KEY_TYPE, Route.Type.class);
        if (routeType != null) {
            switch (routeType) {
                case P2P_ROUTE:
                case STREET_COURSE:
                case GPX_TRACK:
                case RECORDED_ROUTE:
                    return new Route(jsonObject);
            }
        }

        throw new JSONException(
                String.format(
                    "fromJson: Missing or invalid type: %1$s",
                    jsonObject.optString(KEY_TYPE, ""))
                );
    }

    public static ObjectWithId load(long id) {
        return AccessDatabase.getInstance().getObjectWithId(id);
    }


    public abstract static class Builder {
        protected JSONObject inputData;

        public Builder(Enum<?> type, String name) throws JSONException {
            this.inputData = new JSONObject();
            this.inputData.put(KEY_TYPE, typeToString(type));
            this.inputData.put(KEY_NAME, name);
        }

        public Builder setName(final String name) throws JSONException {
            this.inputData.put(KEY_NAME, name);
            return this;
        }

        public Builder setDescription(final String description) throws JSONException {
            this.inputData.put(KEY_DESCRIPTION, description);
            return this;
        }

        public abstract ObjectWithId build() throws JSONException;
    }


    /**
     * id generation
     */
    public static final int NUMBER_OF_LOCAL_IDS = Integer.MAX_VALUE - 1;
    public static final long FIRST_LOCAL_ID = Long.MAX_VALUE - NUMBER_OF_LOCAL_IDS;

    public static long generateId() {
        return FIRST_LOCAL_ID + (new Random()).nextInt(NUMBER_OF_LOCAL_IDS);
    }


    /**
     * constructor
     */

    private long id;
    private Enum type;
    private String name, description;

    public ObjectWithId(Long idFromJson, Enum<?> typeFromJson, JSONObject inputData) throws JSONException {
        if (typeFromJson == null) {
            throw new JSONException("Missing or invalid type");
        }
        this.id = idFromJson != null ? idFromJson : generateId();
        this.type = typeFromJson;
        // extract from json object
        // name is mandatory
        this.name = inputData.getString(KEY_NAME);
        // description is optional
        this.description = Helper.getNullableStringFromJsonObject(inputData, KEY_DESCRIPTION);
    }

    // id and type

    public long getId() {
        return this.id;
    }

    public boolean  hasOsmId() {
        return getOsmId() != null;
    }

    public Long getOsmId() {
        if (this.id < FIRST_LOCAL_ID) {
            return this.id;
        }
        return null;
    }

    public Enum getType() {
        return this.type;
    }


    // icon, name and description

    public abstract Icon getIcon();

    public abstract String formatNameAndSubType();

    public String getName() {
        String customName = getCustomName();
        if (! TextUtils.isEmpty(customName)) {
            return customName;
        }
        return getOriginalName();
    }

    public String getOriginalName() {
        return this.name;
    }

    public String getCustomName() {
        ObjectWithIdParams params = AccessDatabase.getInstance().getObjectWithIdParams(getId());
        if (params != null) {
            return params.customName;
        }
        return null;
    }

    public boolean hasCustomName() {
        return ! TextUtils.isEmpty(getCustomName());
    }

    public boolean rename(String newName) {
        ObjectWithIdParams params = null;
        try {
            params = createParams();
        } catch (JSONException e) {
            return false;
        }
        params.customName = newName;
        return AccessDatabase
            .getInstance()
            .addOrUpdateObjectWithId(params);
    }

    public String getDescription() {
        return this.description;
    }


    // user annotation

    public String getUserAnnotation() {
        ObjectWithIdParams params = AccessDatabase.getInstance().getObjectWithIdParams(getId());
        if (params != null) {
            return params.userAnnotation;
        }
        return null;
    }

    public boolean hasUserAnnotation() {
        return ! TextUtils.isEmpty(getUserAnnotation());
    }

    public boolean setUserAnnotation(String newAnnotation) {
        ObjectWithIdParams params = null;
        try {
            params = createParams();
        } catch (JSONException e) {
            return false;
        }
        params.userAnnotation = newAnnotation;
        return AccessDatabase
            .getInstance()
            .addOrUpdateObjectWithId(params);
    }


    // distance and bearing

    public abstract Coordinates getCoordinates();

    public String formatRelativeBearingFromCurrentLocation(boolean showPreciseBearingValues) {
        RelativeBearing relativeBearing = relativeBearingFromCurrentLocation();
        if (relativeBearing != null) {
            String output = relativeBearing.getDirection().toString();
            if (showPreciseBearingValues) {
                output += " ";
                output += String.format(
                        Locale.ROOT,
                        GlobalInstance.getStringResource(R.string.preciseBearingValues),
                        relativeBearing.getDegree());
            }
            return output;
        }
        return "";
    }

    public String formatDistanceAndRelativeBearingFromCurrentLocation(int distancePluralResourceId) {
        return formatDistanceAndRelativeBearingFromCurrentLocation(distancePluralResourceId, false);
    }

    public String formatDistanceAndRelativeBearingFromCurrentLocation(
            int distancePluralResourceId, boolean showPreciseBearingValues) {
        Integer distance = distanceFromCurrentLocation();
        String relativeBearingFromCurrentLocationFormatted = formatRelativeBearingFromCurrentLocation(showPreciseBearingValues);
        if (distance != null
                && ! TextUtils.isEmpty(relativeBearingFromCurrentLocationFormatted)) {
            return String.format(
                    Locale.getDefault(),
                    "%1$s, %2$s",
                    GlobalInstance.getPluralResource(distancePluralResourceId, distance),
                    relativeBearingFromCurrentLocationFormatted);
        }
        return "";
    }

    public Integer distanceFromCurrentLocation() {
        Point currentLocation = PositionManager.getInstance().getCurrentLocation();
        if (currentLocation != null) {
            return currentLocation.distanceTo(this);
        }
        return null;
    }

    public Integer distanceTo(ObjectWithId other) {
        if (other != null) {
            return this.getCoordinates().distanceTo(other.getCoordinates());
        }
        return null;
    }

    public Bearing bearingFromCurrentLocation() {
        Point currentLocation = PositionManager.getInstance().getCurrentLocation();
        if (currentLocation != null) {
            return currentLocation.bearingTo(this);
        }
        return null;
    }

    public RelativeBearing relativeBearingFromCurrentLocation() {
        Bearing bearing = bearingFromCurrentLocation();
        if (bearing != null) {
            return bearing.relativeToCurrentBearing();
        }
        return null;
    }

    public Bearing bearingTo(ObjectWithId other) {
        if (other != null) {
            return this.getCoordinates().bearingTo(other.getCoordinates());
        }
        return null;
    }


    // database

    public boolean isInDatabase() {
        return AccessDatabase.getInstance().getObjectWithIdParams(getId()) != null;
    }

    public boolean saveToDatabase() {
        try {
            return AccessDatabase.getInstance().addOrUpdateObjectWithId(createParams());
        } catch (JSONException e) {
            return false;
        }
    }

    public boolean removeFromDatabase() {
        return AccessDatabase.getInstance().removeObjectWithId(this);
    }

    private ObjectWithIdParams createParams() throws JSONException {
        ObjectWithIdParams params = new ObjectWithIdParams();
        params.id = getId();
        params.data = toJson().toString();
        String customName = getCustomName();
        params.customName = customName != null ? customName : "";
        String userAnnotation = getUserAnnotation();
        params.userAnnotation = userAnnotation != null ? userAnnotation : "";
        return params;
    }


    // part of collections

    public ArrayList<Collection> getSelectedCollectionList() {
        ArrayList<DatabaseProfile> selectedDatabaseProfileList = AccessDatabase.getInstance().getDatabaseProfileListFor(this);
        ArrayList<Collection> selectedCollectionList = new ArrayList<Collection>();
        for (Collection collection : AccessDatabase.getInstance().getCollectionList()) {
            if (selectedDatabaseProfileList.contains(collection)) {
                selectedCollectionList.add(collection);
            }
        }
        return selectedCollectionList;
    }

    public void setSelectedCollectionList(ArrayList<Collection> newSelectedCollectionList) {
        ArrayList<DatabaseProfile> selectedDatabaseProfileList = AccessDatabase.getInstance().getDatabaseProfileListFor(this);
        for (Collection collection : AccessDatabase.getInstance().getCollectionList()) {
            if (selectedDatabaseProfileList.contains(collection)
                    && ! newSelectedCollectionList.contains(collection)) {
                collection.removeObject(this);
            } else if (! selectedDatabaseProfileList.contains(collection)
                    && newSelectedCollectionList.contains(collection)) {
                collection.addObject(this);
            }
        }
    }


    // super class methods

    @Override public int hashCode() {
        return Long.valueOf(this.id).hashCode();
    }

    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (! (obj instanceof ObjectWithId)) {
            return false;
        }
        ObjectWithId other = (ObjectWithId) obj;
        return this.id == other.getId();
    }


    // json
    //
    // mandatory params
    protected static final String KEY_NAME = "name";
    protected static final String KEY_TYPE = "type";
    // optional
    protected static final String KEY_DESCRIPTION = "description";

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_NAME, this.name);
        jsonObject.put(KEY_TYPE, typeToString(this.type));
        if (this.description != null) {
            jsonObject.put(KEY_DESCRIPTION, this.description);
        }
        return jsonObject;
    }

    private static String typeToString(Enum<?> type) {
        return type.name().toLowerCase(Locale.ROOT);
    }


    /**
     * comparators
     */

    public static class SortByName implements Comparator<ObjectWithId> {
        private boolean ascending;

        public SortByName(boolean ascending) {
            this.ascending = ascending;
        }

        @Override public int compare(ObjectWithId object1, ObjectWithId object2) {
            Collator collator = Collator.getInstance();
            collator.setStrength(Collator.PRIMARY);
            return this.ascending
                ? collator.compare(object1.getName(), object2.getName())
                : collator.compare(object2.getName(), object1.getName());
        }
    }


    public static class SortByDistanceRelativeToCurrentLocation implements Comparator<ObjectWithId> {
        private boolean ascending;

        public SortByDistanceRelativeToCurrentLocation(boolean ascending) {
            this.ascending = ascending;
        }

        @Override public int compare(ObjectWithId object1, ObjectWithId object2) {
            Integer distance1 = object1.distanceFromCurrentLocation();
            Integer distance2 = object2.distanceFromCurrentLocation();
            if (distance1 != null && distance2 != null) {
                if (this.ascending) {
                    return distance1.compareTo(distance2);
                } else {
                    return distance2.compareTo(distance1);
                }
            }
            return 1;
        }
    }

}
