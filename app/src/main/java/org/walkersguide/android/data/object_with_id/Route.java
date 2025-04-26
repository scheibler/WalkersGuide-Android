package org.walkersguide.android.data.object_with_id;

import org.walkersguide.android.data.object_with_id.common.Coordinates;
import org.walkersguide.android.data.ObjectWithId.Icon;

import org.walkersguide.android.data.angle.Turn;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.data.object_with_id.segment.RouteSegment;
import org.walkersguide.android.data.object_with_id.route.RouteObject;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.database.util.SQLiteHelper;
import android.text.TextUtils;
import java.util.Collections;
import timber.log.Timber;
import androidx.core.util.Pair;


public class Route extends ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;

    public enum Type {
        P2P_ROUTE(false),
        STREET_COURSE(false),
        GPX_TRACK(true),
        RECORDED_ROUTE(true);

        public boolean reversible;
        private Type(boolean reversible) {
            this.reversible = reversible;
        }
    }


    /**
     * object creation helpers
     */

    public static Route fromJson(JSONObject jsonObject) throws JSONException {
        return castToRouteOrReturnNull(ObjectWithId.fromJson(jsonObject));
    }

    public static Route load(long id) {
        return castToRouteOrReturnNull(ObjectWithId.load(id));
    }

    private static Route castToRouteOrReturnNull(ObjectWithId objectWithId) {
        return objectWithId instanceof Route ? (Route) objectWithId : null;
    }


    // from a list of points (gpx track, recorded route, street course)

    public static class PointListItem implements Serializable {
        public Point point;
        public boolean isImportant;
        public PointListItem(Point point, boolean isImportant) {
            this.point = point;
            this.isImportant = isImportant;
        }
    }

    public static Route fromPointList(Type routeType, String routeName, ArrayList<PointListItem> pointListItems) throws JSONException {
        return fromPointList(routeType, routeName, null, false, pointListItems);
    }

    public static Route fromPointList(Type routeType, String routeName, String routeDescription,
            boolean reversed, ArrayList<PointListItem> pointListItems) throws JSONException {
        Route.Builder routeBuilder = new Route.Builder(
                routeType,
                routeName,
                pointListItems.get(0).point,
                pointListItems.get(pointListItems.size()-1).point)
            .setReversed(reversed);
        if (! TextUtils.isEmpty(routeDescription)) {
            routeBuilder.setDescription(routeDescription);
        }

        IntersectionSegment cachedSourceSegment = null;
        for (int i=0; i<pointListItems.size(); i++) {

            Point current = pointListItems.get(i).point;
            boolean isImportant = pointListItems.get(i).isImportant;
            if (i == 0) {
                routeBuilder.addFirstRouteObject(current, isImportant);
                continue;
            }

            Point previous = pointListItems.get(i-1).point;
            if (previous instanceof Intersection) {
                Intersection previousIntersection = (Intersection) previous;
                // update cached source segment
                IntersectionSegment segmentNextNodeId = previousIntersection.findMatchingIntersectionSegmentFor(current.getId());
                if (segmentNextNodeId != null) {
                    cachedSourceSegment = segmentNextNodeId;
                } else {
                    cachedSourceSegment = previousIntersection
                        .findClosestIntersectionSegmentTo(previous.bearingTo(current), 5);
                }
            }

            RouteSegment betweenPreviousAndCurrent = RouteSegment.create(cachedSourceSegment, previous, current);
            if (i == pointListItems.size() - 1) {
                routeBuilder.addLastRouteObject(betweenPreviousAndCurrent, current, isImportant);
            } else {
                Turn turn = betweenPreviousAndCurrent.getBearing().turnTo(
                        current.bearingTo(pointListItems.get(i+1).point));
                routeBuilder.addRouteObject(betweenPreviousAndCurrent, current, turn, isImportant);
            }
        }

        return routeBuilder.build();
    }

    public static Route reverse(Route route) throws JSONException {
        if (route == null || ! route.isReversible()) {
            throw new JSONException("Route is null or not reversible");
        }

        // extract points and reverse list
        ArrayList<PointListItem> reversedPointListItems = new ArrayList<PointListItem>();
        for (RouteObject routeObject : route.getRouteObjectList()) {
            reversedPointListItems.add(
                    new PointListItem(routeObject.getPoint(), routeObject.getIsImportant()));
        }
        Collections.reverse(reversedPointListItems);

        Route reversedRoute = Route.fromPointList(
                route.getType(), route.getOriginalName(), null, ! route.isReversed(), reversedPointListItems);
        Timber.d("reversed route with custom name %1$s and id=%2$d / %3$d", route.getOriginalName(), route.getId(), reversedRoute.getId());
        if (route.hasCustomNameInDatabase()) {
            reversedRoute.setCustomNameInDatabase(route.getCustomNameFromDatabase());
        }
        return reversedRoute;
    }


    // builder

    public static class Builder extends ObjectWithId.Builder {
        public int totalDistance, numberOfIntersections;

        public Builder(Type type, String name, Point startPoint, Point destinationPoint) throws JSONException {
            super(
                    type,
                    TextUtils.isEmpty(name)
                    ? String.format(
                        "%1$s: %2$s\n%3$s: %4$s",
                        GlobalInstance.getStringResource(R.string.labelPrefixStart),
                        startPoint.getName(),
                        GlobalInstance.getStringResource(R.string.labelPrefixDestination),
                        destinationPoint.getName())
                    : name);
            inputData.put(KEY_START_POINT, startPoint.toJson());
            inputData.put(KEY_DESTINATION_POINT, destinationPoint.toJson());
            inputData.put(KEY_ROUTE_OBJECT_LIST, new JSONArray());
            this.totalDistance = 0;
            this.numberOfIntersections = 0;
        }

        public Builder setReversed(final boolean reversed) throws JSONException {
            inputData.put(KEY_REVERSED, reversed);
            return this;
        }

        public Builder setViaPoints(final Point via1, final Point via2, final Point via3) throws JSONException {
            if (via1 != null) {
                inputData.put(KEY_VIA_POINT_1, via1.toJson());
            }
            if (via2 != null) {
                inputData.put(KEY_VIA_POINT_2, via2.toJson());
            }
            if (via3 != null) {
                inputData.put(KEY_VIA_POINT_3, via3.toJson());
            }
            return this;
        }

        public Builder addFirstRouteObject(final Point point, boolean isImportant) throws JSONException {
            inputData
                .getJSONArray(KEY_ROUTE_OBJECT_LIST)
                .put((new RouteObject(true, false, isImportant, null, point, null)).toJson());
            return this;
        }

        public Builder addRouteObject(RouteSegment segment, Point point,
                Turn turn, boolean isImportant) throws JSONException {
            inputData
                .getJSONArray(KEY_ROUTE_OBJECT_LIST)
                .put((new RouteObject(false, false, isImportant, segment, point, turn)).toJson());
            this.totalDistance += segment.getDistance();
            if (point instanceof Intersection) {
                this.numberOfIntersections += 1;
            }
            return this;
        }

        public Builder addLastRouteObject(RouteSegment segment, Point point, boolean isImportant) throws JSONException {
            inputData
                .getJSONArray(KEY_ROUTE_OBJECT_LIST)
                .put((new RouteObject(false, true, isImportant, segment, point, null)).toJson());
            this.totalDistance += segment.getDistance();
            return this;
        }

        public Route build() throws JSONException {
            inputData.put(
                    KEY_ID, generateId(inputData.getJSONArray(KEY_ROUTE_OBJECT_LIST)));
            if (inputData.isNull(ObjectWithId.KEY_DESCRIPTION)) {
                setDescription(
                        String.format(
                            GlobalInstance.getStringResource(R.string.routeDescriptionDefault),
                            GlobalInstance.getPluralResource(R.plurals.meter, this.totalDistance),
                            this.numberOfIntersections > 0
                            ? GlobalInstance.getPluralResource(
                                R.plurals.intersection, this.numberOfIntersections)
                            : GlobalInstance.getPluralResource(
                                R.plurals.point, inputData.getJSONArray(KEY_ROUTE_OBJECT_LIST).length()))
                        );
            }
            return new Route(inputData);
        }
    }

    public static long generateId(JSONArray jsonRouteObjectList) throws JSONException {
        return SQLiteHelper.createDatabaseV10RouteId(jsonRouteObjectList);
    }


    /**
     * constructor
     */

    private Point startPoint, destinationPoint;
    private Point viaPoint1, viaPoint2, viaPoint3;
    private boolean reversed;
    private ArrayList<RouteObject> routeObjectList;

    public Route(JSONObject inputData) throws JSONException {
        super(
                Helper.getNullableAndPositiveLongFromJsonObject(inputData, KEY_ID),
                Helper.getNullableEnumFromJsonObject(inputData, ObjectWithId.KEY_TYPE, Type.class),
                inputData);
        this.reversed = false;
        try {
            this.reversed = inputData.getBoolean(KEY_REVERSED);
        } catch (JSONException e) {}

        // start, destination and via points
        this.startPoint = Point.fromJson(inputData.getJSONObject(KEY_START_POINT));
        this.destinationPoint = Point.fromJson(inputData.getJSONObject(KEY_DESTINATION_POINT));
        if (! inputData.isNull(KEY_VIA_POINT_1)) {
            this.viaPoint1 = Point.fromJson(inputData.getJSONObject(KEY_VIA_POINT_1));
        }
        if (! inputData.isNull(KEY_VIA_POINT_2)) {
            this.viaPoint2 = Point.fromJson(inputData.getJSONObject(KEY_VIA_POINT_2));
        }
        if (! inputData.isNull(KEY_VIA_POINT_3)) {
            this.viaPoint3 = Point.fromJson(inputData.getJSONObject(KEY_VIA_POINT_3));
        }

        // route object list
        this.routeObjectList = new ArrayList<RouteObject>();
        JSONArray jsonRouteObjectList = getJsonRouteObjectListWithStartAndEndRouteSegmentCoordinates(
                inputData.getJSONArray(KEY_ROUTE_OBJECT_LIST));
        for (int j=0; j<jsonRouteObjectList.length(); j++) {
            this.routeObjectList.add(
                    new RouteObject(
                        jsonRouteObjectList.getJSONObject(j)));
        }
        if (this.routeObjectList.isEmpty()) {
            throw new JSONException("Parsing error: Route is empty");
        }
    }

    public String formatNameAndSubType() {
        return getName();
    }

    public boolean isReversible() {
        return getType().reversible;
    }

    public boolean isReversed() {
        return this.reversed;
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
                    currentRouteObject.getTurn().getInstruction(),
                    currentRouteObject.getPoint().getName());
        }
    }

    public String formatArrivalAtPointMessage() {
        RouteObject currentRouteObject = getCurrentRouteObject();
        if (currentRouteObject.getIsFirstRouteObject()) {
            return GlobalInstance.getStringResource(R.string.messageArrivedAtRouteStart);
        } else if (currentRouteObject.getIsLastRouteObject()) {
            return GlobalInstance.getStringResource(R.string.messageArrivedAtRouteDestination);
        } else {
            RouteObject nextRouteObject = this.routeObjectList.get(getCurrentPosition()+1);
            if (nextRouteObject.getTurn() != null) {
                return String.format(
                        GlobalInstance.getStringResource(R.string.messageArrivedAtRoutePointWithNextTurn),
                        getCurrentPosition()+1,
                        currentRouteObject.getTurn().getInstruction(),
                        nextRouteObject.formatSegmentInstruction(),
                        nextRouteObject.getTurn().getInstruction());
            } else {
                return String.format(
                        GlobalInstance.getStringResource(R.string.messageArrivedAtRoutePoint),
                        getCurrentPosition()+1,
                        currentRouteObject.getTurn().getInstruction(),
                        nextRouteObject.formatSegmentInstruction());
            }
        }
    }

    public int getElapsedLength() {
        return getLengthUntil(getCurrentPosition());
    }

    public int getTotalLength() {
        return getLengthUntil(getRouteObjectList().size()-1);
    }

    private int getLengthUntil(int maxPosition) {
        int position = 0, length = 0;
        for (RouteObject routeObject : getRouteObjectList()) {
            if (position <= maxPosition) {
                RouteSegment routeSegment = routeObject.getSegment();
                // first route segment is null
                if (routeSegment != null) {
                    length += routeSegment.getDistance();
                }
            }
            position += 1;
        }
        return length;
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

    public boolean hasViaPoint() {
        return getViaPoint1() != null || getViaPoint2() != null || getViaPoint3() != null;
    }


    /**
     * super class methods
     */

    @Override public Type getType() {
        return (Type) super.getType();
    }

    @Override public Icon getIcon() {
        return Icon.ROUTE;
    }

    @Override public Coordinates getCoordinates() {
        Integer distanceFromStartPoint = this.startPoint.distanceFromCurrentLocation();
        Integer distanceFromDestinationPoint = this.destinationPoint.distanceFromCurrentLocation();
        if (distanceFromStartPoint != null && distanceFromDestinationPoint != null) {
            return distanceFromStartPoint < distanceFromDestinationPoint
                ? this.startPoint.getCoordinates()
                : this.destinationPoint.getCoordinates();
        }
        return null;
    }

    @Override public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.getName());
        if (isReversed()) {
            stringBuilder.append(System.lineSeparator());
            stringBuilder.append(
                    GlobalInstance.getStringResource(R.string.labelOppositeDirection));
        }
        return stringBuilder.toString();
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

    public RouteObject getClosestRouteObjectFromCurrentLocation() {
        Pair<RouteObject,Integer> closest = null;
        for (RouteObject routeObject : this.routeObjectList) {
            Integer distanceFromCurrentLocation = routeObject.getPoint().distanceFromCurrentLocation();
            if (distanceFromCurrentLocation != null
                    && (closest == null || distanceFromCurrentLocation < closest.second)) {
                closest = Pair.create(routeObject, distanceFromCurrentLocation);
            }
        }
        return closest != null ? closest.first : null;
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
    public static final String KEY_REVERSED = "reversed";
    public static final String KEY_START_POINT = "start_point";
    public static final String KEY_DESTINATION_POINT = "destination_point";
    public static final String KEY_VIA_POINT_1 = "via_point_1";
    public static final String KEY_VIA_POINT_2 = "via_point_2";
    public static final String KEY_VIA_POINT_3 = "via_point_3";
    public static final String KEY_ROUTE_OBJECT_LIST = "instructions";

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        jsonObject.put(KEY_ID, this.getId());
        jsonObject.put(KEY_REVERSED, this.reversed);

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


    private static JSONArray getJsonRouteObjectListWithStartAndEndRouteSegmentCoordinates(
            JSONArray jsonRouteObjectList) throws JSONException {
        JSONArray newJsonRouteObjectList = new JSONArray();
        newJsonRouteObjectList.put(
                jsonRouteObjectList.getJSONObject(0));

        for (int j=1; j<jsonRouteObjectList.length(); j++) {
            JSONObject jsonPreviousRouteObject = jsonRouteObjectList.getJSONObject(j-1);
            JSONObject jsonCurrentRouteObject = jsonRouteObjectList.getJSONObject(j);

            JSONObject jsonCurrentRouteSegment = jsonCurrentRouteObject.getJSONObject(RouteObject.KEY_SEGMENT);
            Coordinates startCoordinates = null, endCoordinates = null;
            try {

                // check if the route segment is missing its coordinates
                startCoordinates = new Coordinates(
                        jsonCurrentRouteSegment.getJSONObject(Segment.KEY_START));
                endCoordinates = new Coordinates(
                        jsonCurrentRouteSegment.getJSONObject(Segment.KEY_END));

            } catch (JSONException e) {
                //Timber.d("coordinates missing");

                startCoordinates = Point.fromJson(
                        jsonPreviousRouteObject.getJSONObject(RouteObject.KEY_POINT))
                    .getCoordinates();
                jsonCurrentRouteSegment.put(
                        Segment.KEY_START, startCoordinates.toJson());

                endCoordinates = Point.fromJson(
                        jsonCurrentRouteObject.getJSONObject(RouteObject.KEY_POINT))
                    .getCoordinates();
                jsonCurrentRouteSegment.put(
                        Segment.KEY_END, endCoordinates.toJson());

                jsonCurrentRouteObject.put(
                        RouteObject.KEY_SEGMENT, jsonCurrentRouteSegment);
            }

            newJsonRouteObjectList.put(jsonCurrentRouteObject);
        }

        return newJsonRouteObjectList;
    }

}
