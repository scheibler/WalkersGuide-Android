package org.walkersguide.android.data.basic.segment;

import android.content.Context;

import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.helper.StringUtility;

public class IntersectionSegment extends Footway {

    private String intersectionName;
    private long intersectionNodeId, nextNodeId;
    private boolean partOfPreviousRouteSegment, partOfNextRouteSegment;

    public IntersectionSegment(Context context, JSONObject inputData) throws JSONException {
        super(context, inputData);
        this.intersectionName = inputData.getString("intersection_name");
        this.intersectionNodeId = -1;
        try {
            long intersectionNodeIdValue = inputData.getLong("intersectionNodeId");
            if (intersectionNodeIdValue > 0) {
                this.intersectionNodeId = intersectionNodeIdValue;
            }
        } catch (JSONException e) {}
        this.nextNodeId = -1;
        try {
            long nextNodeIdValue = inputData.getLong("next_node_id");
            if (nextNodeIdValue > 0) {
                this.nextNodeId = nextNodeIdValue;
            }
        } catch (JSONException e) {}
        // part of previous or next route segment
        try {
            this.partOfPreviousRouteSegment = inputData.getBoolean("part_of_previous_route_segment");
        } catch (JSONException e) {
            this.partOfPreviousRouteSegment = false;
        }
        try {
            this.partOfNextRouteSegment = inputData.getBoolean("part_of_next_route_segment");
        } catch (JSONException e) {
            this.partOfNextRouteSegment = false;
        }
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
        return this.partOfPreviousRouteSegment;
    }

    public boolean isPartOfNextRouteSegment() {
        return this.partOfNextRouteSegment;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        jsonObject.put("intersection_name", this.intersectionName);
        if (this.intersectionNodeId > -1) {
            try {
                jsonObject.put("intersectionNodeId", this.intersectionNodeId);
            } catch (JSONException e) {}
        }
        if (this.nextNodeId > -1) {
            try {
                jsonObject.put("next_node_id", this.nextNodeId);
            } catch (JSONException e) {}
        }
        if (this.partOfPreviousRouteSegment) {
            try {
                jsonObject.put("part_of_previous_route_segment", this.partOfPreviousRouteSegment);
            } catch (JSONException e) {}
        }
        if (this.partOfNextRouteSegment) {
            try {
                jsonObject.put("part_of_next_route_segment", this.partOfNextRouteSegment);
            } catch (JSONException e) {}
        }
        return jsonObject;
    }

    @Override public String toString() {
        if (super.bearingFromCurrentDirection() != null) {
            return String.format(
                    "%1$s: %2$s",
                    StringUtility.formatRelativeViewingDirection(
                        super.getContext(), super.bearingFromCurrentDirection()),
                    super.toString());
        }
        return super.toString();
    }

	@Override public int hashCode() {
        int hash = super.hashCode();
		hash = hash * 31 + this.intersectionName.hashCode();
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
        return super.equals(((Footway) other))
            && this.intersectionName.equals(other.getIntersectionName())
            && this.nextNodeId == other.getNextNodeId();
    }


    public static class SortByBearingFromCurrentDirection implements Comparator<IntersectionSegment> {
        private int offsetInDegree;
        public SortByBearingFromCurrentDirection(int offsetInDegree) {
            this.offsetInDegree = offsetInDegree;
        }
        @Override public int compare(IntersectionSegment object1, IntersectionSegment object2) {
            if (object1.bearingFromCurrentDirection() != null
                    && object2.bearingFromCurrentDirection() != null) {
                int directionWithOffset1 = (object1.bearingFromCurrentDirection() + this.offsetInDegree) % 360;
                int directionWithOffset2 = (object2.bearingFromCurrentDirection() + this.offsetInDegree) % 360;
                return Integer.valueOf(directionWithOffset1).compareTo(directionWithOffset2);
            }
            return 0;
        }
    }

}
