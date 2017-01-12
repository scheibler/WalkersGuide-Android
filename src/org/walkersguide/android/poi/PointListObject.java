package org.walkersguide.android.poi;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.basic.point.GPS;
import org.walkersguide.android.basic.point.Intersection;
import org.walkersguide.android.basic.point.POI;
import org.walkersguide.android.basic.point.Point;
import org.walkersguide.android.basic.point.Station;
import org.walkersguide.android.util.Constants;

public class PointListObject implements Comparable<PointListObject> {

    private POIProfile parentProfile;
    private Point point;

    public PointListObject(POIProfile parentProfile, JSONObject inputData) throws JSONException {
        this.parentProfile = parentProfile;
        if (inputData.getString("type").equals(Constants.TYPE.GPS)) {
            this.point = new GPS(parentProfile.getContext(), inputData);
        } else if (inputData.getString("type").equals(Constants.TYPE.INTERSECTION)) {
            this.point = new Intersection(parentProfile.getContext(), inputData);
        } else if (inputData.getString("type").equals(Constants.TYPE.POI)) {
            this.point = new POI(parentProfile.getContext(), inputData);
        } else if (inputData.getString("type").equals(Constants.TYPE.STATION)) {
            this.point = new Station(parentProfile.getContext(), inputData);
        } else {
            this.point = new Point(parentProfile.getContext(), inputData);
        }
    }

    public Point getPoint() {
        return this.point;
    }

    public int distanceFromCenter() {
        if (this.parentProfile.getCenter() != null) {
            return this.parentProfile.getCenter().distanceTo(this.point);
        }
        return -1;
    }

    public int bearingFromCenter() {
        if (this.parentProfile.getCenter() != null
                && this.parentProfile.getDirection() > -1) {
            int relativeDirection = this.parentProfile.getCenter().bearingTo(this.point)
                - this.parentProfile.getDirection();
            if (relativeDirection < 0) {
                relativeDirection += 360;
            }
            return relativeDirection;
        }
        return -1;
    }

    @Override public String toString() {
        return this.point.getName() + " (" + this.distanceFromCenter() + " meter, " + this.bearingFromCenter() + " grad)";
    }

	@Override public int hashCode() {
        return this.point.hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof PointListObject)) {
			return false;
        }
		PointListObject other = (PointListObject) obj;
        return this.point.equals(other.getPoint());
    }

    @Override public int compareTo(PointListObject other) {
        if (this.distanceFromCenter() < other.distanceFromCenter()) {
            return -1;
        } else if (this.distanceFromCenter() > other.distanceFromCenter())  {
            return 1;
        } else if (this.bearingFromCenter() < other.bearingFromCenter()) {
            return -1;
        } else if (this.bearingFromCenter() > other.bearingFromCenter()) {
            return 1;
        } else {
            return this.point.getName().compareTo(other.getPoint().getName());
        }
    }

}
