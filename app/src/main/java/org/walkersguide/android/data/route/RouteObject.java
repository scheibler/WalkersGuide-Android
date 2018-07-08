package org.walkersguide.android.data.route;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.util.Constants;

import android.content.Context;

public class RouteObject {

    public static SegmentWrapper getDummyRouteSegment(Context context) {
        SegmentWrapper dummyRouteSegment = null;
        try {
            dummyRouteSegment = new SegmentWrapper(context, new JSONObject(Constants.DUMMY.FOOTWAY));
        } catch (JSONException e) {}
        return dummyRouteSegment;
    }

    private Context context;
    private int index;
    private SegmentWrapper segment;
    private RoutePoint point;

    public RouteObject(Context context, int index, JSONObject inputData) throws JSONException {
        this.context = context;
        this.index = index;
        this.segment = new SegmentWrapper(context, inputData.getJSONObject("segment"));
        this.point = new RoutePoint(context, inputData.getJSONObject("point"));
    }

    /** from flat route list **/
    public RouteObject(Context context, int index, JSONObject jsonSegment, JSONObject jsonPoint) throws JSONException {
        this.context = context;
        this.index = index;
        this.segment = new SegmentWrapper(context, jsonSegment);
        this.point = new RoutePoint(context, jsonPoint);
    }

    public int getIndex() {
        return this.index;
    }

    public SegmentWrapper getRouteSegment() {
        return this.segment;
    }

    public PointWrapper getRoutePoint() {
        return this.point;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("segment", this.segment.toJson());
        jsonObject.put("point", this.point.toJson());
        return jsonObject;
    }

    @Override public String toString() {
        if (this.index == 0) {
            return String.format(
                    this.context.getResources().getString(R.string.firstRouteObjectToString),
                    this.point.toString());
        } else {
            return String.format(
                    this.context.getResources().getString(R.string.otherRouteObjectsToString),
                    this.index+1,
                    this.segment.toString(),
                    this.point.toString());
        }
    }

	@Override public int hashCode() {
        int hash = 17;
		hash = hash * 31 + this.segment.hashCode();
		hash = hash * 31 + this.point.hashCode();
        return hash;
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
        return (this.segment.equals(other.getRouteSegment())
                && this.point.equals(other.getRoutePoint()));
    }

}
