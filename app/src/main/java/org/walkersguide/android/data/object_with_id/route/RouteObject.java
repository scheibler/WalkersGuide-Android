package org.walkersguide.android.data.object_with_id.route;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.data.angle.Turn;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.segment.RouteSegment;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.angle.Bearing;
import java.util.ArrayList;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import java.util.Collections;
import android.text.TextUtils;
import org.walkersguide.android.data.Angle;
import org.walkersguide.android.data.ObjectWithId;


public class RouteObject implements Serializable {
    private static final long serialVersionUID = 1l;

    private boolean isFirstRouteObject, isLastRouteObject, isImportant;
    private RouteSegment segment;
    private Point point;
    private Turn turn;

    public RouteObject(boolean isFirstRouteObject, boolean isLastRouteObject, boolean isImportant,
            RouteSegment segment, Point point, Turn turn) {
        this.isFirstRouteObject = isFirstRouteObject;
        this.isLastRouteObject = isLastRouteObject;
        this.isImportant = isImportant;
        this.segment = segment;
        this.point = point;
        this.turn = turn;
    }

    public RouteObject(JSONObject inputData) throws JSONException {
        this.isFirstRouteObject = inputData.getBoolean(KEY_IS_FIRST_ROUTE_OBJECT);
        this.isLastRouteObject = inputData.getBoolean(KEY_IS_LAST_ROUTE_OBJECT);
        this.isImportant = inputData.optBoolean(KEY_IS_IMPORTANT, false);

        // segment and point
        if (this.isFirstRouteObject) {
            this.segment = null;
        } else {
            this.segment = new RouteSegment(inputData.getJSONObject(KEY_SEGMENT));
        }
        this.point = Point.fromJson(inputData.getJSONObject(KEY_POINT));

        // turn
        if (this.isFirstRouteObject || this.isLastRouteObject) {
            this.turn = null;
        } else {
            this.turn = new Turn(inputData.getInt(KEY_TURN));
        }
    }

    public boolean getIsFirstRouteObject() {
        return this.isFirstRouteObject;
    }

    public boolean getIsLastRouteObject() {
        return this.isLastRouteObject;
    }

    public boolean getIsImportant() {
        return this.isImportant;
    }

    public RouteSegment getSegment() {
        return this.segment;
    }

    public Point getPoint() {
        return this.point;
    }

    public Turn getTurn() {
        return this.turn;
    }

    public String formatSegmentInstruction() {
        if (this.isFirstRouteObject) {
            return "";
        } else if (segment.getBewareBicyclists() != null
                && this.segment.getBewareBicyclists()) {
            return String.format(
                    "%1$s\n%2$s",
                    this.segment.toString(),
                    GlobalInstance.getStringResource(R.string.routeSegmentBewareCyclists));
        } else {
            return this.segment.toString();
        }
    }

    public String formatPointInstruction() {
        if (this.isFirstRouteObject) {
            return String.format(
                    GlobalInstance.getStringResource(R.string.routePointInstructionStart),
                    this.point.getName());
        } else if (this.isLastRouteObject) {
            return String.format(
                    GlobalInstance.getStringResource(R.string.routePointInstructionDestination),
                    this.point.getName());
        } else if (this.turn.getInstruction() == Turn.Instruction.CROSS) {
            return String.format(
                    GlobalInstance.getStringResource(R.string.routePointInstructionIntermediateCross),
                    this.turn.getInstruction(),
                    this.point.getName());
        } else {
            return String.format(
                    GlobalInstance.getStringResource(R.string.routePointInstructionIntermediateTurn),
                    this.turn.getInstruction(),
                    this.point.getName());
        }
    }

    @Override public int hashCode() {
        return this.point.hashCode();
    }

    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (! (obj instanceof RouteObject)) {
            return false;
        }
        RouteObject other = (RouteObject) obj;
        return this.point.equals(other.getPoint());
    }


    /**
     * to json
     */

    public static final String KEY_IS_FIRST_ROUTE_OBJECT = "is_first_route_object";
    public static final String KEY_IS_LAST_ROUTE_OBJECT = "is_last_route_object";
    public static final String KEY_IS_IMPORTANT = "is_important";
    public static final String KEY_POINT = "point";
    public static final String KEY_SEGMENT = "segment";
    public static final String KEY_TURN = "turn";

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_IS_FIRST_ROUTE_OBJECT, this.isFirstRouteObject);
        jsonObject.put(KEY_IS_LAST_ROUTE_OBJECT, this.isLastRouteObject);
        jsonObject.put(KEY_IS_IMPORTANT, this.isImportant);
        if (this.segment != null) {
            jsonObject.put(KEY_SEGMENT, this.segment.toJson());
        }
        jsonObject.put(KEY_POINT, this.point.toJson());
        if (this.turn != null) {
            jsonObject.put(KEY_TURN, this.turn.getDegree());
        }
        return jsonObject;
    }

}
