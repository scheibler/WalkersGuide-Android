package org.walkersguide.android.data.basic.segment;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.R;


public class RouteSegment extends Footway {

    private int distance;

    public RouteSegment(Context context, JSONObject inputData) throws JSONException {
        super(context, inputData);
        this.distance = inputData.getInt("distance");
    }

    public int getDistance() {
        return this.distance;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        jsonObject.put("distance", this.distance);
        return jsonObject;
    }

    @Override public String toString() {
        return String.format(
                super.getContext().getResources().getString(R.string.routeSegmentToString),
                super.getContext().getResources().getQuantityString(
                    R.plurals.meter, this.distance, this.distance),
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
        return (super.equals(((Footway) other))
            && this.distance == other.getDistance());
    }

}
