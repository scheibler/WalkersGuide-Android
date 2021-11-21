package org.walkersguide.android.data.route;

import org.walkersguide.android.database.profiles.DatabaseRouteProfile;
import android.content.Context;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.util.StringUtility;
import org.walkersguide.android.util.GlobalInstance;
import timber.log.Timber;
import org.walkersguide.android.data.basic.segment.Segment;
import org.walkersguide.android.data.basic.segment.RouteSegment;


public class Route extends ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;

    /**
     * object creation helpers
     */

    public static Route create(JSONObject jsonRoute) throws JSONException {
        return new Route(jsonRoute);
    }

    public static Route load(long id) {
        return AccessDatabase.getInstance().getRoute(id);
    }


    // builder

    public static class Builder {
        public JSONObject inputData;
        public Builder(Point startPoint, Point destination_point) throws JSONException {
            this.inputData = new JSONObject();
            this.inputData.put(KEY_START_POINT, startPoint.toJson());
            this.inputData.put(KEY_DESTINATION_POINT, destination_point.toJson());
            this.inputData.put(KEY_ROUTE_OBJECT_LIST, new JSONArray());
        }

        public Builder addFirstRouteObject(final Point point) throws JSONException {
            inputData
                .getJSONArray(KEY_ROUTE_OBJECT_LIST)
                .put((new RouteObject(true, false, null, point, null)).toJson());
            return this;
        }
        public Builder addRouteObject(RouteSegment segment, Point point, Integer turn) throws JSONException {
            inputData
                .getJSONArray(KEY_ROUTE_OBJECT_LIST)
                .put((new RouteObject(false, false, segment, point, turn)).toJson());
            return this;
        }
        public Builder addLastRouteObject(RouteSegment segment, Point point) throws JSONException {
            inputData
                .getJSONArray(KEY_ROUTE_OBJECT_LIST)
                .put((new RouteObject(false, true, segment, point, null)).toJson());
            return this;
        }

        public Route build() throws JSONException {
            inputData.put(KEY_ID, Route.createDatabaseV10RouteId(inputData.getJSONArray(KEY_ROUTE_OBJECT_LIST)));
            inputData.put(KEY_DESCRIPTION, "");
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
        super(StringUtility.getNullableAndPositiveLongFromJsonObject(inputData, KEY_ID));
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

    public String getName() {
        return String.format(
                "%1$s: %2$s\n%3$s: %4$s",
                GlobalInstance.getStringResource(R.string.buttonStartPoint),
                this.startPoint.getName(),
                GlobalInstance.getStringResource(R.string.buttonDestinationPoint),
                this.destinationPoint.getName());
    }

    public String getSubType() {
        return "";
    }

    public String getDescription() {
        return this.description;
    }

    public ArrayList<RouteObject> getRouteObjectList() {
        return this.routeObjectList;
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
     * controls
     */

    public boolean isFavorite() {
        return super.isFavorite(DatabaseRouteProfile.FAVORITES);
    }

    public int getCurrentPosition() {
        int currentPosition = AccessDatabase.getInstance().getRouteCurrentPosition(this.getId());
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
            return AccessDatabase.getInstance().addRoute(this, getCurrentPosition() - 1);
        }
        return false;
    }

    public boolean hasNextRouteObject() {
        return getCurrentPosition() < this.routeObjectList.size() - 1;
    }

    public boolean skipToNextRouteObject() {
        if (hasNextRouteObject()) {
            return AccessDatabase.getInstance().addRoute(this, getCurrentPosition() + 1);
        }
        return false;
    }

    public boolean jumpToRouteObjectAt(int newPosition) {
        if (newPosition >= 0 && newPosition < this.routeObjectList.size()) {
            return AccessDatabase.getInstance().addRoute(this, newPosition);
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
        for (int i=0; i<jsonRouteObjectList.length(); i++) {
            long nodeId = jsonRouteObjectList.getJSONObject(i).getJSONObject("point").getLong("node_id");
            result = prime * result + new Long(nodeId).hashCode();
        }
        return result;
    }

}
