package org.walkersguide.android.data.basic.wrapper;

import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.Entrance;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.point.Intersection;
import org.walkersguide.android.data.basic.point.POI;
import org.walkersguide.android.data.basic.point.PedestrianCrossing;
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.data.basic.point.Station;
import org.walkersguide.android.data.basic.point.StreetAddress;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;

import android.content.Context;

public class PointWrapper {

    private Context context;
    private Point point;

    public PointWrapper(Context context, JSONObject inputData) throws JSONException {
        this.context = context;
        if (inputData.getString("type").equals(Constants.POINT.STATION)) {
            this.point = new Station(context, inputData);
        } else if (inputData.getString("type").equals(Constants.POINT.ENTRANCE)) {
            this.point = new Entrance(context, inputData);
        } else if (inputData.getString("type").equals(Constants.POINT.POI)) {
            this.point = new POI(context, inputData);
        } else if (inputData.getString("type").equals(Constants.POINT.STREET_ADDRESS)) {
            this.point = new StreetAddress(context, inputData);
        } else if (inputData.getString("type").equals(Constants.POINT.GPS)) {
            this.point = new GPS(context, inputData);
        } else if (inputData.getString("type").equals(Constants.POINT.INTERSECTION)) {
            this.point = new Intersection(context, inputData);
        } else if (inputData.getString("type").equals(Constants.POINT.PEDESTRIAN_CROSSING)) {
            this.point = new PedestrianCrossing(context, inputData);
        } else {
            this.point = new Point(context, inputData);
        }
    }

    public Context getContext() {
        return this.context;
    }

    public Point getPoint() {
        return this.point;
    }

    public String pointToString() {
        if (point instanceof Station) {
            return ((Station) point).toString();
        } else if (point instanceof Entrance) {
            return ((Entrance) point).toString();
        } else if (point instanceof POI) {
            return ((POI) point).toString();
        } else if (point instanceof StreetAddress) {
            return ((StreetAddress) point).toString();
        } else if (point instanceof GPS) {
            return ((GPS) point).toString();
        } else if (point instanceof Intersection) {
            return ((Intersection) point).toString();
        } else if (point instanceof PedestrianCrossing) {
            return ((PedestrianCrossing) point).toString();
        } else {
            return ((Point) point).toString();
        }
    }

    public int distanceTo(PointWrapper other) {
        if (other != null) {
            return this.point.distanceTo(other.getPoint());
        }
        return -1;
    }

    public int bearingTo(PointWrapper other) {
        if (other != null) {
            return this.point.bearingTo(other.getPoint());
        }
        return -1;
    }

    public int distanceFromCurrentLocation() {
        PointWrapper currentLocation = PositionManager.getInstance(context).getCurrentLocation();
        if (currentLocation != null) {
            return currentLocation.distanceTo(this);
        }
        return -1;
    }

    public int bearingFromCurrentLocation() {
        PointWrapper currentLocation = PositionManager.getInstance(context).getCurrentLocation();
        if (currentLocation != null) {
            int absoluteDirection = currentLocation.bearingTo(this);
            // take the current viewing direction into account
            int relativeDirection = absoluteDirection
                - DirectionManager.getInstance(context).getCurrentDirection();
            if (relativeDirection < 0) {
                relativeDirection += 360;
            }
            return relativeDirection;
        }
        return -1;
    }

    public JSONObject toJson() throws JSONException {
        if (point instanceof Station) {
            return ((Station) point).toJson();
        } else if (point instanceof Entrance) {
            return ((Entrance) point).toJson();
        } else if (point instanceof POI) {
            return ((POI) point).toJson();
        } else if (point instanceof StreetAddress) {
            return ((StreetAddress) point).toJson();
        } else if (point instanceof GPS) {
            return ((GPS) point).toJson();
        } else if (point instanceof Intersection) {
            return ((Intersection) point).toJson();
        } else if (point instanceof PedestrianCrossing) {
            return ((PedestrianCrossing) point).toJson();
        } else {
            return ((Point) point).toJson();
        }
    }

    @Override public String toString() {
        if (distanceFromCurrentLocation() > -1 && bearingFromCurrentLocation() > -1) {
            return String.format(
                    context.getResources().getString(R.string.pointListObjectDescription),
                    this.pointToString(),
                    distanceFromCurrentLocation(),
                    StringUtility.formatInstructionDirection(
                        context, bearingFromCurrentLocation())
                    );
        }
        return pointToString();
    }

	@Override public int hashCode() {
        return this.point.hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof PointWrapper)) {
			return false;
        }
		PointWrapper other = (PointWrapper) obj;
        return this.point.equals(other.getPoint());
    }

    public static class SortByDistanceFromCurrentPosition implements Comparator<PointWrapper> {
        @Override public int compare(PointWrapper object1, PointWrapper object2) {
            if (object1.distanceFromCurrentLocation() < object2.distanceFromCurrentLocation()) {
                return -1;
            } else if (object1.distanceFromCurrentLocation() > object2.distanceFromCurrentLocation())  {
                return 1;
            } else if (object1.bearingFromCurrentLocation() < object2.bearingFromCurrentLocation()) {
                return -1;
            } else if (object1.bearingFromCurrentLocation() > object2.bearingFromCurrentLocation()) {
                return 1;
            } else {
                return object1.point.getName().compareTo(object2.getPoint().getName());
            }
        }
    }

}
