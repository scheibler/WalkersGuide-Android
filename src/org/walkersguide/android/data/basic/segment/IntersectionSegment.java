package org.walkersguide.android.data.basic.segment;

import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.helper.StringUtility;

import android.content.Context;

public class IntersectionSegment extends Footway {

    private String intersectionName;

    public IntersectionSegment(Context context, JSONObject inputData) throws JSONException {
        super(context, inputData);
        this.intersectionName = inputData.getString("intersection_name");
    }

    public String getIntersectionName() {
        return this.intersectionName;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        jsonObject.put("intersection_name", this.intersectionName);
        return jsonObject;
    }

    @Override public String toString() {
        if (super.bearingFromCurrentDirection() > -1) {
            return String.format(
                    "%1$s: %2$s",
                    StringUtility.formatInstructionDirection(
                        super.getContext(), super.bearingFromCurrentDirection()),
                    super.toString());
        }
        return super.toString();
    }

	@Override public int hashCode() {
        int hash = super.hashCode();
		hash = hash * 31 + this.intersectionName.hashCode();
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
            && this.intersectionName.equals(other.getIntersectionName());
    }


    public static class SortByBearingFromCurrentDirection implements Comparator<IntersectionSegment> {
        private int offsetInDegree;
        public SortByBearingFromCurrentDirection(int offsetInDegree) {
            this.offsetInDegree = offsetInDegree;
        }
        @Override public int compare(IntersectionSegment object1, IntersectionSegment object2) {
            int directionWithOffset1 = (object1.bearingFromCurrentDirection() + this.offsetInDegree) % 360;
            int directionWithOffset2 = (object2.bearingFromCurrentDirection() + this.offsetInDegree) % 360;
            return Integer.valueOf(directionWithOffset1).compareTo(directionWithOffset2);
        }
    }

}
