package org.walkersguide.android.data.basic.wrapper;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.data.basic.segment.Footway;
import org.walkersguide.android.data.basic.segment.IntersectionSegment;
import org.walkersguide.android.data.basic.segment.Segment;
import org.walkersguide.android.util.Constants;

import android.content.Context;

public class SegmentWrapper {

    private Context context;
    private Segment segment;

    public SegmentWrapper(Context context, JSONObject inputData) throws JSONException {
        this.context = context;
        if (inputData.getString("type").equals(Constants.SEGMENT.INTERSECTION)) {
            this.segment = new IntersectionSegment(context, inputData);
        } else {
            this.segment = new Footway(context, inputData);
        }
    }

    public Segment getSegment() {
        return this.segment;
    }

    public JSONObject toJson() throws JSONException {
        if (segment instanceof IntersectionSegment) {
            return ((IntersectionSegment) segment).toJson();
        } else {
            return ((Footway) segment).toJson();
        }
    }

    @Override public String toString() {
        if (segment instanceof IntersectionSegment) {
            return ((IntersectionSegment) segment).toString();
        } else {
            return ((Footway) segment).toString();
        }
    }

	@Override public int hashCode() {
        return this.segment.hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof SegmentWrapper)) {
			return false;
        }
		SegmentWrapper other = (SegmentWrapper) obj;
        return this.segment.equals(other.getSegment());
    }

}
