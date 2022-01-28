package org.walkersguide.android.data.object_with_id;

import org.walkersguide.android.database.profile.FavoritesProfile;

import org.walkersguide.android.data.angle.Turn;
import android.content.Context;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.util.GlobalInstance;
import timber.log.Timber;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.data.object_with_id.segment.RouteSegment;
import org.walkersguide.android.data.object_with_id.route.RouteObject;
import org.walkersguide.android.data.object_with_id.point.Intersection;


public class Route extends ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;

    /**
     * object creation helpers
     */

    public static Route create(JSONObject jsonRoute) throws JSONException {
        return new Route(jsonRoute);
    }

    public static Route load(long id) {
        ObjectWithId object = AccessDatabase.getInstance().getObjectWithId(id);
        if (object instanceof Route) {
            return (Route) object;
        }
        return null;
    }


    // builder

    public static class Builder {
        public JSONObject inputData;
        public int totalDistance, numberOfIntersections;

        public Builder(Point startPoint, Point destination_point) throws JSONException {
            this.inputData = new JSONObject();
            this.inputData.put(KEY_START_POINT, startPoint.toJson());
            this.inputData.put(KEY_DESTINATION_POINT, destination_point.toJson());
            this.inputData.put(KEY_ROUTE_OBJECT_LIST, new JSONArray());
            this.totalDistance = 0;
            this.numberOfIntersections = 0;
        }

        public Builder addFirstRouteObject(final Point point) throws JSONException {
            this.inputData
                .getJSONArray(KEY_ROUTE_OBJECT_LIST)
                .put((new RouteObject(true, false, null, point, null)).toJson());
            return this;
        }

        public Builder addRouteObject(RouteSegment segment, Point point, Turn turn) throws JSONException {
            this.inputData
                .getJSONArray(KEY_ROUTE_OBJECT_LIST)
                .put((new RouteObject(false, false, segment, point, turn)).toJson());
            this.totalDistance += segment.getDistance();
            if (point instanceof Intersection) {
                this.numberOfIntersections += 1;
            }
            return this;
        }

        public Builder addLastRouteObject(RouteSegment segment, Point point) throws JSONException {
            this.inputData
                .getJSONArray(KEY_ROUTE_OBJECT_LIST)
                .put((new RouteObject(false, true, segment, point, null)).toJson());
            this.totalDistance += segment.getDistance();
            return this;
        }

        public Route build() throws JSONException {
            inputData.put(KEY_ID, Route.createDatabaseV10RouteId(inputData.getJSONArray(KEY_ROUTE_OBJECT_LIST)));
            inputData.put(
                    KEY_DESCRIPTION,
                    String.format(
                        GlobalInstance.getStringResource(R.string.descriptionStreetCourse),
                        GlobalInstance.getPluralResource(R.plurals.meter, this.totalDistance),
                        this.numberOfIntersections));
            return new Route(inputData);
        }
    }


    /**
     * constructor
     */

    private Point startPoint, destinationPoint;
    private Point viaPoint1, viaPoint2, viaPoint3;
    private String description;
    private ArrayList<RouteObject> routeObjectList;

    public Route(JSONObject inputData) throws JSONException {
        super(Helper.getNullableAndPositiveLongFromJsonObject(inputData, KEY_ID));
        this.description = inputData.getString(KEY_DESCRIPTION);

        // start, destination and via points
        this.startPoint = Point.create(inputData.getJSONObject(KEY_START_POINT));
        this.destinationPoint = Point.create(inputData.getJSONObject(KEY_DESTINATION_POINT));
        if (! inputData.isNull(KEY_VIA_POINT_1)) {
            this.viaPoint1 = Point.create(inputData.getJSONObject(KEY_VIA_POINT_1));
        }
        if (! inputData.isNull(KEY_VIA_POINT_2)) {
            this.viaPoint2 = Point.create(inputData.getJSONObject(KEY_VIA_POINT_2));
        }
        if (! inputData.isNull(KEY_VIA_POINT_3)) {
            this.viaPoint3 = Point.create(inputData.getJSONObject(KEY_VIA_POINT_3));
        }

        // route object list
        this.routeObjectList = new ArrayList<RouteObject>();
        JSONArray jsonRouteObjectList = inputData.getJSONArray(KEY_ROUTE_OBJECT_LIST);
        for (int j=0; j<jsonRouteObjectList.length(); j++) {
            this.routeObjectList.add(
                    new RouteObject(
                        jsonRouteObjectList.getJSONObject(j)));
        }
    }

    public String getOriginalName() {
        return String.format(
                "%1$s: %2$s\n%3$s: %4$s",
                GlobalInstance.getStringResource(R.string.labelPrefixStart),
                this.startPoint.getName(),
                GlobalInstance.getStringResource(R.string.labelPrefixDestination),
                this.destinationPoint.getName());
    }

    public String getDescription() {
        return this.description;
    }

    public ArrayList<RouteObject> getRouteObjectList() {
        return this.routeObjectList;
    }

    public String formatShortlyBeforeArrivalAtPointMessage() {
        RouteObject currentRouteObject = getCurrentRouteObject();
        if (currentRouteObject.getIsFirstRouteObject()) {
            return GlobalInstance.getStringResource(R.string.messageAlmostArrivedAtRouteStart);
        } else if (currentRouteObject.getIsLastRouteObject()) {
            return GlobalInstance.getStringResource(R.string.messageAlmostArrivedAtRouteDestination);
        } else {
            return String.format(
                    GlobalInstance.getStringResource(R.string.messageAlmostArrivedAtRoutePoint),
                    currentRouteObject.getTurn().getInstruction());
        }
    }

    public String formatArrivalAtPointMessage() {
        RouteObject currentRouteObject = getCurrentRouteObject();
        if (currentRouteObject.getIsFirstRouteObject()) {
            return GlobalInstance.getStringResource(R.string.messageArrivedAtRouteStart);
        } else if (currentRouteObject.getIsLastRouteObject()) {
            return GlobalInstance.getStringResource(R.string.messageArrivedAtRouteDestination);
        } else {
            return String.format(
                    GlobalInstance.getStringResource(R.string.messageArrivedAtRoutePoint),
                    getCurrentPosition()+1,
                    currentRouteObject.getTurn().getInstruction());
        }
    }


    /**
     * start, destination and via points
     */

    public Point getStartPoint() {
        return this.startPoint;
    }

    public Point getDestinationPoint() {
        return this.destinationPoint;
    }

    public Point getViaPoint1() {
        return this.viaPoint1;
    }

    public Point getViaPoint2() {
        return this.viaPoint2;
    }

    public Point getViaPoint3() {
        return this.viaPoint3;
    }


    /**
     * super class methods
     */

    @Override public FavoritesProfile getDefaultFavoritesProfile() {
        return FavoritesProfile.favoriteRoutes();
    }

    @Override public String toString() {
        return getName();
    }


    /**
     * controls
     */

    public int getCurrentPosition() {
        int currentPosition = GlobalInstance.getInstance().getRouteCurrentPosition(this);
        if (currentPosition >= 0 && currentPosition < this.routeObjectList.size()) {
            return currentPosition;
        }
        return 0;
    }

    public RouteObject getCurrentRouteObject() {
        return this.routeObjectList.get(getCurrentPosition());
    }

    public boolean hasPreviousRouteObject() {
        return getCurrentPosition() > 0;
    }

    public boolean skipToPreviousRouteObject() {
        if (hasPreviousRouteObject()) {
            GlobalInstance.getInstance().setRouteCurrentPosition(this, getCurrentPosition() - 1);
            return true;
        }
        return false;
    }

    public boolean hasNextRouteObject() {
        return getCurrentPosition() < this.routeObjectList.size() - 1;
    }

    public boolean skipToNextRouteObject() {
        if (hasNextRouteObject()) {
            GlobalInstance.getInstance().setRouteCurrentPosition(this, getCurrentPosition() + 1);
            return true;
        }
        return false;
    }

    public boolean jumpToRouteObject(RouteObject newRouteObject) {
        return jumpToRouteObjectAt(this.routeObjectList.indexOf(newRouteObject));
    }

    public boolean jumpToRouteObjectAt(int newPosition) {
        if (newPosition >= 0 && newPosition < this.routeObjectList.size()) {
            GlobalInstance.getInstance().setRouteCurrentPosition(this, newPosition);
            return true;
        }
        return false;
    }


    /**
     * to json
     */

    public static final String KEY_ID = "route_id";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_START_POINT = "start_point";
    public static final String KEY_DESTINATION_POINT = "destination_point";
    public static final String KEY_VIA_POINT_1 = "via_point_1";
    public static final String KEY_VIA_POINT_2 = "via_point_2";
    public static final String KEY_VIA_POINT_3 = "via_point_3";
    public static final String KEY_ROUTE_OBJECT_LIST = "instructions";

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_ID, this.getId());
        jsonObject.put(KEY_DESCRIPTION, this.description);

        jsonObject.put(KEY_START_POINT, this.startPoint.toJson());
        jsonObject.put(KEY_DESTINATION_POINT, this.destinationPoint.toJson());
        if (this.viaPoint1 != null) {
            jsonObject.put(KEY_VIA_POINT_1, this.viaPoint1.toJson());
        }
        if (this.viaPoint2 != null) {
            jsonObject.put(KEY_VIA_POINT_2, this.viaPoint2.toJson());
        }
        if (this.viaPoint3 != null) {
            jsonObject.put(KEY_VIA_POINT_3, this.viaPoint3.toJson());
        }

        JSONArray jsonRouteObjectList = new JSONArray();
        for (RouteObject routeObject : this.routeObjectList) {
            jsonRouteObjectList.put(routeObject.toJson());
        }
        jsonObject.put(KEY_ROUTE_OBJECT_LIST, jsonRouteObjectList);

        return jsonObject;
    }


    /**
     * convert webserver api 4 to api 5
     */

    public static JSONObject convertRouteFromWebserverApiV4ToV5(
            JSONObject jsonStartPoint, JSONObject jsonDestinationPoint, JSONArray jsonViaPointList,
            String description, JSONArray jsonOldRouteObjectList) throws JSONException {
        JSONObject jsonRoute = new JSONObject();

        jsonRoute.put(
                "start_point", Point.addNodeIdToJsonObject(jsonStartPoint));
        jsonRoute.put(
                "destination_point", Point.addNodeIdToJsonObject(jsonDestinationPoint));
        if (jsonViaPointList != null) {
            if (jsonViaPointList.length() > 0) {
                jsonRoute.put(
                        "via_point_1", Point.addNodeIdToJsonObject(jsonViaPointList.getJSONObject(0)));
            }
            if (jsonViaPointList.length() > 1) {
                jsonRoute.put(
                        "via_point_2", Point.addNodeIdToJsonObject(jsonViaPointList.getJSONObject(1)));
            }
            if (jsonViaPointList.length() > 2) {
                jsonRoute.put(
                        "via_point_3", Point.addNodeIdToJsonObject(jsonViaPointList.getJSONObject(2)));
            }
        }

        JSONArray jsonRouteObjectList = new JSONArray();
        for (int i=0; i<jsonOldRouteObjectList.length(); i++) {
            boolean isFirstRouteObject = i == 0 ? true : false;
            boolean isLastRouteObject = i == (jsonOldRouteObjectList.length() - 1) ? true : false;

            JSONObject jsonRouteObject = new JSONObject();
            jsonRouteObject.put("is_first_route_object", isFirstRouteObject);
            jsonRouteObject.put("is_last_route_object", isLastRouteObject);

            // segment
            if (! isFirstRouteObject) {
                JSONObject jsonSegment = Segment.addWayIdToJsonObject(
                        jsonOldRouteObjectList.getJSONObject(i).getJSONObject("segment"));
                jsonRouteObject.put("segment", jsonSegment);
            }

            // point
            JSONObject jsonPoint = Point.addNodeIdToJsonObject(
                    jsonOldRouteObjectList.getJSONObject(i).getJSONObject("point"));
            // extract turn value
            if (! isFirstRouteObject && ! isLastRouteObject) {
                jsonRouteObject.put("turn", jsonPoint.getInt("turn"));
            }
            // cleanup point
            jsonPoint.remove("turn");
            // add
            jsonRouteObject.put("point", jsonPoint);

            jsonRouteObjectList.put(jsonRouteObject);
        }
        jsonRoute.put("instructions", jsonRouteObjectList);

        jsonRoute.put("route_id", createDatabaseV10RouteId(jsonRouteObjectList));
        jsonRoute.put("description", description);
        return jsonRoute;
    }

    public static long createDatabaseV10RouteId(JSONArray jsonRouteObjectList) throws JSONException {
        final int prime = 31;
        int result = 1;
        for (int i=1; i<jsonRouteObjectList.length(); i++) {
            // start at index '1' is intentional, route object at '0' has no segment
            int distance = jsonRouteObjectList
                .getJSONObject(i)
                .getJSONObject("segment")
                .getInt("distance");
            result = prime * result + distance;
        }
        result = prime * result + jsonRouteObjectList.length();
        return result;
    }

}
