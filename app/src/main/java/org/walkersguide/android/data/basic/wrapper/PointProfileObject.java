package org.walkersguide.android.data.basic.wrapper;

import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.sensor.DirectionManager;

import android.content.Context;
import org.walkersguide.android.data.profile.PointProfile;

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

    public int distanceFromCenter() {
        if (this.parentProfile != null
                && this.parentProfile.getCenter() != null) {
            return this.parentProfile.getCenter().getPoint().distanceTo(super.getPoint());
        }
        return -1;
    }

    public int bearingFromCenter() {
        if (this.parentProfile != null
                && this.parentProfile.getCenter() != null) {
            int absoluteDirection = this.parentProfile.getCenter().getPoint().bearingTo(super.getPoint());
            // take the current viewing direction into account
            int relativeDirection = absoluteDirection
                - DirectionManager.getInstance(super.getContext()).getCurrentDirection();
            if (relativeDirection < 0) {
                relativeDirection += 360;
            }
            return relativeDirection;
        }
        return -1;
    }

    @Override public String toString() {
        if (distanceFromCenter() > -1 && bearingFromCenter() > -1) {
            return String.format(
                    super.getContext().getResources().getString(R.string.pointListObjectDescription),
                    super.pointToString(),
                    distanceFromCenter(),
                    StringUtility.formatInstructionDirection(
                        super.getContext(), bearingFromCenter())
                    );
        }
        return super.toString();
    }

    public static class SortByDistanceFromCenterASC implements Comparator<PointProfileObject> {
        @Override public int compare(PointProfileObject object1, PointProfileObject object2) {
            if (object1.distanceFromCenter() < object2.distanceFromCenter()) {
                return -1;
            } else if (object1.distanceFromCenter() > object2.distanceFromCenter())  {
                return 1;
            } else if (object1.bearingFromCenter() < object2.bearingFromCenter()) {
                return -1;
            } else if (object1.bearingFromCenter() > object2.bearingFromCenter()) {
                return 1;
            } else {
                return object1.getPoint().getName().compareTo(object2.getPoint().getName());
            }
        }
    }

    public static class SortByDistanceFromCenterDESC implements Comparator<PointProfileObject> {
        @Override public int compare(PointProfileObject object1, PointProfileObject object2) {
            if (object2.distanceFromCenter() < object1.distanceFromCenter()) {
                return -1;
            } else if (object2.distanceFromCenter() > object1.distanceFromCenter())  {
                return 1;
            } else if (object2.bearingFromCenter() < object1.bearingFromCenter()) {
                return -1;
            } else if (object2.bearingFromCenter() > object1.bearingFromCenter()) {
                return 1;
            } else {
                return object2.getPoint().getName().compareTo(object1.getPoint().getName());
            }
        }
    }

    public static class SortByNameASC implements Comparator<PointProfileObject> {
        @Override public int compare(PointProfileObject object1, PointProfileObject object2) {
            return object1.getPoint().getName().compareTo(object2.getPoint().getName());
        }
    }

    public static class SortByNameDESC implements Comparator<PointProfileObject> {
        @Override public int compare(PointProfileObject object1, PointProfileObject object2) {
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
