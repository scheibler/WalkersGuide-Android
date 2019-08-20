package org.walkersguide.android.data.basic.wrapper;

import android.content.Context;

import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.PointProfile;
import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DirectionManager;


public class PointProfileObject extends PointWrapper {

    private PointProfile parentProfile;
    private long order;

    public PointProfileObject(Context context, PointProfile parentProfile, JSONObject inputData) throws JSONException {
        super(context, inputData);
        this.parentProfile = parentProfile;
        this.order = -1l;
        try {
            this.order = inputData.getLong("order");
        } catch (JSONException e) {}
    }

    public long getOrder() {
        return this.order;
    }

    public Integer distanceFromCenter() {
        if (this.parentProfile != null
                && this.parentProfile.getCenter() != null) {
            return this.parentProfile.getCenter().getPoint().distanceTo(super.getPoint());
        }
        return null;
    }

    public Integer bearingFromCenter() {
        Direction currentDirection = DirectionManager.getInstance(super.getContext()).getCurrentDirection();
        if (currentDirection != null
                && this.parentProfile != null
                && this.parentProfile.getCenter() != null) {
            int absoluteDirection = this.parentProfile.getCenter().getPoint().bearingTo(super.getPoint());
            // take the current viewing direction into account
            int relativeDirection = absoluteDirection - currentDirection.getBearing();
            if (relativeDirection < 0) {
                relativeDirection += 360;
            }
            return relativeDirection;
        }
        return null;
    }

    @Override public String toString() {
        Integer distanceFromCenter = distanceFromCenter();
        Integer bearingFromCenter = bearingFromCenter();
        if (distanceFromCenter != null && bearingFromCenter != null) {
            return String.format(
                    super.getContext().getResources().getString(R.string.pointListObjectDescription),
                    super.pointToString(),
                    super.getContext().getResources().getQuantityString(
                        R.plurals.meter, distanceFromCenter, distanceFromCenter),
                    StringUtility.formatRelativeViewingDirection(
                        super.getContext(), bearingFromCenter)
                    );
        }
        return super.toString();
    }


    public static class SortByDistanceFromCenterASC implements Comparator<PointProfileObject> {
        @Override public int compare(PointProfileObject object1, PointProfileObject object2) {
            Integer distanceObject1 = object1.distanceFromCenter();
            Integer distanceObject2 = object2.distanceFromCenter();
            if (distanceObject1 != null && distanceObject2 != null) {
                if (distanceObject1 < distanceObject2) {
                    return -1;
                } else if (distanceObject1 > distanceObject2)  {
                    return 1;
                }
            }

            Integer bearingObject1 = object1.bearingFromCenter();
            Integer bearingObject2 = object1.bearingFromCenter();
            if (bearingObject1 != null && bearingObject2 != null) {
                if (bearingObject1 < bearingObject2) {
                    return -1;
                } else if (bearingObject1 > bearingObject2) {
                    return 1;
                }
            }

            return object1.getPoint().getName().compareTo(object2.getPoint().getName());
        }
    }


    public static class SortByDistanceFromCenterDESC implements Comparator<PointProfileObject> {
        @Override public int compare(PointProfileObject object1, PointProfileObject object2) {
            Integer distanceObject1 = object1.distanceFromCenter();
            Integer distanceObject2 = object2.distanceFromCenter();
            if (distanceObject2 != null && distanceObject1 != null) {
                if (distanceObject2 < distanceObject1) {
                    return -1;
                } else if (distanceObject2 > distanceObject1)  {
                    return 1;
                }
            }

            Integer bearingObject1 = object1.bearingFromCenter();
            Integer bearingObject2 = object1.bearingFromCenter();
            if (bearingObject2 != null && bearingObject1 != null) {
                if (bearingObject2 < bearingObject1) {
                    return -1;
                } else if (bearingObject2 > bearingObject1) {
                    return 1;
                }
            }

            return object2.getPoint().getName().compareTo(object1.getPoint().getName());
        }
    }


    public static class SortByOrderASC implements Comparator<PointProfileObject> {
        @Override public int compare(PointProfileObject object1, PointProfileObject object2) {
            if (object1.getOrder() < object2.getOrder()) {
                return -1;
            } else if (object1.getOrder() > object2.getOrder()) {
                return 1;
            } else {
                return object1.getPoint().getName().compareTo(object2.getPoint().getName());
            }
        }
    }


    public static class SortByOrderDESC implements Comparator<PointProfileObject> {
        @Override public int compare(PointProfileObject object1, PointProfileObject object2) {
            if (object2.getOrder() < object1.getOrder()) {
                return -1;
            } else if (object2.getOrder() > object1.getOrder()) {
                return 1;
            } else {
                return object2.getPoint().getName().compareTo(object1.getPoint().getName());
            }
        }
    }

}
