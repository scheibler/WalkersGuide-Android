package org.walkersguide.android.data.basic.segment;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.util.GlobalInstance;


public class RouteSegment extends Segment implements Serializable {
    private static final long serialVersionUID = 1l;


    /**
     * builder
     */

    public static class Builder extends Segment.Builder {
        public Builder(int bearing, int distance) {
            super(
                    Segment.Type.ROUTE,
                    "Wegstück",
                    "Luftlinie",
                    //GlobalInstance.getStringResource(R.string.currentLocationName),
                    bearing);
            try {
                super.inputData.put(KEY_DISTANCE, distance);
            } catch (JSONException e) {}
        }
        // build
        public RouteSegment build() {
            RouteSegment segment = null;
            try {
                segment = new RouteSegment(super.inputData);
            } catch (JSONException e) {
                segment = null;
            }
            return segment;
        }
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
