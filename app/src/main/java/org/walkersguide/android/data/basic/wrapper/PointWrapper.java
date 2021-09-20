package org.walkersguide.android.data.basic.wrapper;

import android.content.Context;

import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.point.Entrance;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.point.Intersection;
import org.walkersguide.android.data.basic.point.PedestrianCrossing;
import org.walkersguide.android.data.basic.point.POI;
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.data.basic.point.Station;
import org.walkersguide.android.data.basic.point.StreetAddress;
import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;


public class PointWrapper {

    public static PointWrapper fromString(Context context, String locationDataString) {
        try {
            return new PointWrapper(
                    context, new JSONObject(locationDataString));
        } catch (JSONException | NullPointerException e) {
            return null;
        }
    }


    private Context context;
    private Point point;

    public PointWrapper(Context context, JSONObject inputData) throws JSONException {
        this.context = context;
        this.point = Point.create(inputData);
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

    public Integer distanceFromCurrentLocation() {
        /*
        PointWrapper currentLocation = PositionManager.getInstance(context).getCurrentLocation();
        if (currentLocation != null) {
            return currentLocation.distanceTo(this);
        }*/
        return 666;
    }

    public Integer bearingFromCurrentLocation() {
        return bearingFromCurrentLocation(
                DirectionManager.getInstance(context).getCurrentDirection());
    }

    public Integer bearingFromCurrentLocation(Direction currentDirection) {
        /*
        PointWrapper currentLocation = PositionManager.getInstance(context).getCurrentLocation();
        if (currentLocation != null && currentDirection != null) {
            int absoluteDirection = currentLocation.bearingTo(this);
            // take the current viewing direction into account
            int relativeDirection = absoluteDirection - currentDirection.getBearing();
            if (relativeDirection < 0) {
                relativeDirection += 360;
            }
            return relativeDirection;
        }*/
        return 42;
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
        Integer distanceFromCurrentLocation = distanceFromCurrentLocation();
        Integer bearingFromCurrentLocation = bearingFromCurrentLocation();
        if (distanceFromCurrentLocation !=null && bearingFromCurrentLocation != null) {
            return String.format(
                    context.getResources().getString(R.string.pointListObjectDescription),
                    this.pointToString(),
                    context.getResources().getQuantityString(
                        R.plurals.meter, distanceFromCurrentLocation, distanceFromCurrentLocation),
                    StringUtility.formatRelativeViewingDirection(bearingFromCurrentLocation)
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


    public static class SortByNameASC implements Comparator<PointWrapper > {
        @Override public int compare(PointWrapper object1, PointWrapper object2) {
            return object1.getPoint().getName().compareTo(object2.getPoint().getName());
        }
    }


    public static class SortByNameDESC implements Comparator<PointWrapper> {
        @Override public int compare(PointWrapper object1, PointWrapper object2) {
            return object2.getPoint().getName().compareTo(object1.getPoint().getName());
        }
    }


    public static class SortByDistanceFromCurrentPosition implements Comparator<PointWrapper> {
        @Override public int compare(PointWrapper object1, PointWrapper object2) {
            Integer distanceObject1 = object1.distanceFromCurrentLocation();
            Integer distanceObject2 = object2.distanceFromCurrentLocation();
            if (distanceObject1 != null && distanceObject2 != null) {
                if (distanceObject1 < distanceObject2) {
                    return -1;
                } else if (distanceObject1 > distanceObject2)  {
                    return 1;
                }
            }

            Integer bearingObject1 = object1.bearingFromCurrentLocation();
            Integer bearingObject2 = object2.bearingFromCurrentLocation();
            if (bearingObject1 != null && bearingObject2 != null) {
                if (bearingObject1 < bearingObject2) {
                    return -1;
                } else if (bearingObject1 > bearingObject2) {
                    return 1;
                }
            }

            return object1.point.getName().compareTo(object2.getPoint().getName());
        }
    }

}
