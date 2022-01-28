package org.walkersguide.android.data.object_with_id.segment;

import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import java.lang.StringBuilder;
import org.walkersguide.android.data.angle.RelativeBearing;
import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.util.Helper;
import java.io.Serializable;
import org.walkersguide.android.data.object_with_id.Segment;


public class IntersectionSegment extends Segment implements Serializable {
    private static final long serialVersionUID = 1l;

    private String intersectionName;
    private long intersectionNodeId, nextNodeId;
    private Boolean partOfPreviousRouteSegment, partOfNextRouteSegment;

    public IntersectionSegment(JSONObject inputData) throws JSONException {
        super(inputData);
        this.intersectionName = inputData.getString(KEY_INTERSECTION_NAME);
        this.intersectionNodeId = inputData.getLong(KEY_INTERSECTION_NODE_ID);
        this.nextNodeId = inputData.getLong(KEY_NEXT_NODE_ID);
        this.partOfPreviousRouteSegment = Helper.getNullableBooleanFromJsonObject(inputData, KEY_PART_OF_PREVIOUS_ROUTE_SEGMENT);
        this.partOfNextRouteSegment = Helper.getNullableBooleanFromJsonObject(inputData, KEY_PART_OF_NEXT_ROUTE_SEGMENT);
    }

    public String getIntersectionName() {
        return this.intersectionName;
    }

    public long getIntersectionNodeId() {
        return this.intersectionNodeId;
    }

    public long getNextNodeId() {
        return this.nextNodeId;
    }

    public boolean isPartOfPreviousRouteSegment() {
        if (this.partOfPreviousRouteSegment != null) {
            return this.partOfPreviousRouteSegment;
        }
        return false;
    }

    public boolean isPartOfNextRouteSegment() {
        if (this.partOfNextRouteSegment != null) {
            return this.partOfNextRouteSegment;
        }
        return false;
    }

    @Override public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        RelativeBearing relativeBearing = getBearing().relativeToCurrentBearing();;
        if (relativeBearing != null) {
            stringBuilder.append(
                    String.format("%1$s: ", relativeBearing.getDirection()));
        }
        stringBuilder.append(super.toString());

        if (isPartOfPreviousRouteSegment() || isPartOfNextRouteSegment()) {
            stringBuilder.append(System.lineSeparator());
            if (isPartOfPreviousRouteSegment()) {
                stringBuilder.append(
                        GlobalInstance.getStringResource(R.string.labelPartOfPreviousRouteSegment));
            } else {
                stringBuilder.append(
                        GlobalInstance.getStringResource(R.string.labelPartOfNextRouteSegment));
            }
        }
        return stringBuilder.toString();
    }

	@Override public int hashCode() {
        int hash = super.hashCode();
		hash = hash * 31 + Long.valueOf(this.intersectionNodeId).hashCode();
		hash = hash * 31 + Long.valueOf(this.nextNodeId).hashCode();
		return hash;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof IntersectionSegment)) {
			return false;
        }
		IntersectionSegment other = (IntersectionSegment) obj;
        return super.equals(((Segment) other))
            && this.intersectionNodeId == other.getIntersectionNodeId()
            && this.nextNodeId == other.getNextNodeId();
    }


    /**
     * to json
     */

    public static final String KEY_INTERSECTION_NAME = "intersection_name";
    public static final String KEY_INTERSECTION_NODE_ID = "intersection_node_id";
    public static final String KEY_NEXT_NODE_ID = "next_node_id";
    public static final String KEY_PART_OF_PREVIOUS_ROUTE_SEGMENT = "part_of_previous_route_segment";
    public static final String KEY_PART_OF_NEXT_ROUTE_SEGMENT = "part_of_next_route_segment";

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        jsonObject.put(KEY_INTERSECTION_NAME, this.intersectionName);
        jsonObject.put(KEY_INTERSECTION_NODE_ID, this.intersectionNodeId);
        jsonObject.put(KEY_NEXT_NODE_ID, this.nextNodeId);
        if (this.partOfPreviousRouteSegment != null) {
            jsonObject.put(KEY_PART_OF_PREVIOUS_ROUTE_SEGMENT, this.partOfPreviousRouteSegment);
        }
        if (this.partOfNextRouteSegment != null) {
            jsonObject.put(KEY_PART_OF_NEXT_ROUTE_SEGMENT, this.partOfNextRouteSegment);
        }
        return jsonObject;
    }

}
