package org.walkersguide.android.data.object_with_id.segment;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.data.angle.Bearing;


public class RouteSegment extends Segment implements Serializable {
    private static final long serialVersionUID = 1l;


    /**
     * object creation helpers
     */

    public static RouteSegment create(IntersectionSegment sourceSegment,
            Bearing newBearing, Integer newDistance) throws JSONException {
        if (newBearing == null || newDistance == null) {
            throw new JSONException("bearing or distance missing");
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

        // refresh some entries
        jsonRouteSegment.put(Segment.KEY_TYPE, Segment.Type.FOOTWAY_ROUTE.toString());
        jsonRouteSegment.put(KEY_BEARING, newBearing.getDegree());
        jsonRouteSegment.put(KEY_DISTANCE, newDistance);
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
