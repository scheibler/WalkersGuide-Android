package org.walkersguide.android.data.object_with_id.segment;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.object_with_id.Point;


public class RouteSegment extends Segment implements Serializable {
    private static final long serialVersionUID = 1l;


    /**
     * object creation helpers
     */

    public static RouteSegment create(IntersectionSegment sourceSegment, Point start, Point end) throws JSONException {
        if (start == null || end == null) {
            throw new JSONException("start or end point missing");
        }

        JSONObject jsonRouteSegment;
        if (sourceSegment != null) {
            jsonRouteSegment = sourceSegment.toJson();
        } else {
            jsonRouteSegment = new JSONObject();
            jsonRouteSegment.put(
                    KEY_NAME, GlobalInstance.getStringResource(R.string.routeSegmentNameUnknown));
            jsonRouteSegment.put(KEY_SUB_TYPE, "");
        }
        jsonRouteSegment.put(KEY_TYPE, Segment.Type.FOOTWAY_ROUTE.toString());

        // refresh some entries
        jsonRouteSegment.put(
                KEY_START, start.getCoordinates().toJson());
        jsonRouteSegment.put(
                KEY_END, end.getCoordinates().toJson());
        jsonRouteSegment.put(
                KEY_BEARING, start.bearingTo(end).getDegree());
        jsonRouteSegment.put(
                KEY_DISTANCE, start.distanceTo(end));
        return new RouteSegment(jsonRouteSegment);
    }


    /**
     * constructor
     */

    private int distance;

    public RouteSegment(JSONObject inputData) throws JSONException {
        super(inputData);
        this.distance = inputData.getInt(KEY_DISTANCE);
    }

    public int getDistance() {
        return this.distance;
    }

    @Override public String toString() {
        return String.format(
                GlobalInstance.getStringResource(R.string.routeSegmentToString),
                GlobalInstance.getPluralResource(R.plurals.meter, this.distance),
                super.toString());
    }

	@Override public int hashCode() {
        int hash = super.hashCode();
		hash = hash * 31 + this.distance;
		return hash;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof RouteSegment)) {
			return false;
        }
		RouteSegment other = (RouteSegment) obj;
        return (super.equals(((Segment) other))
            && this.distance == other.getDistance());
    }


    /**
     * to json
     */

    public static final String KEY_DISTANCE = "distance";

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        jsonObject.put(KEY_DISTANCE, this.distance);
        return jsonObject;
    }

}
