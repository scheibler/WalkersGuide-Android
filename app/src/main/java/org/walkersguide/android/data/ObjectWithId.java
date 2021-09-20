package org.walkersguide.android.data;

import java.util.Comparator;
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.data.basic.segment.Segment;

import java.io.Serializable;
import org.walkersguide.android.helper.StringUtility;
import java.util.Random;
import java.lang.Math;
import android.text.TextUtils;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.database.DatabaseProfile;


public abstract class ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;
    private static final long FIRST_LOCAL_ID = (long) Math.pow(2, 60);

    public static long generateId() {
        return FIRST_LOCAL_ID + (long) (new Random()).nextInt() - 1;
    }


    private long id;

    public ObjectWithId(Long idFromJson) {
        if (idFromJson == null) {
            this.id = generateId();
        } else {
            this.id = idFromJson;
        }
    }

    public long getId() {
        return this.id;
    }

    public Long getOsmId() {
        if (this.id < FIRST_LOCAL_ID) {
            return this.id;
        }
        return null;
    }

    public abstract String getName();

    public abstract String getSubType();

    public abstract boolean isFavorite();

    protected boolean isFavorite(DatabaseProfile favoritesProfile) {
        return AccessDatabase
            .getInstance()
            .getDatabaseProfileListFor(this)
            .contains(favoritesProfile);
    }

    @Override public String toString() {
        String customOrOriginalName = getName();
        if (! TextUtils.isEmpty(getSubType())
                && ! customOrOriginalName.equals(getSubType())) {
            return String.format("%1$s (%2$s)", customOrOriginalName, getSubType());
        }
        return customOrOriginalName;
    }

	@Override public int hashCode() {
        return new Long(this.id).hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof ObjectWithId)) {
			return false;
        }
		ObjectWithId other = (ObjectWithId) obj;
        return this.id == other.getId();
    }


    /**
     * comparators
     */

    public static class SortByName implements Comparator<ObjectWithId> {
        private boolean ascending;
        public SortByName(boolean ascending) {
            this.ascending = ascending;
        }
        @Override public int compare(ObjectWithId object1, ObjectWithId object2) {
            if (this.ascending) {
                return object1.getName().compareTo(object2.getName());
            } else {
                return object2.getName().compareTo(object1.getName());
            }
        }
    }

    public static class SortByBearingFromCurrentDirection implements Comparator<ObjectWithId> {
        private int offsetInDegree;
        private boolean ascending;
        public SortByBearingFromCurrentDirection(int offsetInDegree, boolean ascending) {
            this.offsetInDegree = offsetInDegree;
            this.ascending = ascending;
        }
        @Override public int compare(ObjectWithId object1, ObjectWithId object2) {
            if (object1 instanceof Segment && object2 instanceof Segment) {
                Segment segment1 = (Segment) object1;
                Segment segment2 = (Segment) object2;
                if (segment1.bearingFromCurrentDirection() != null
                        && segment2.bearingFromCurrentDirection() != null) {
                    Integer directionWithOffset1 = (segment1.bearingFromCurrentDirection() + this.offsetInDegree) % 360;
                    Integer directionWithOffset2 = (segment2.bearingFromCurrentDirection() + this.offsetInDegree) % 360;
                    if (this.ascending) {
                        return directionWithOffset1.compareTo(directionWithOffset2);
                    } else {
                        return directionWithOffset2.compareTo(directionWithOffset1);
                    }
                }
            }
            return 0;
        }
    }

    public static class SortByDistanceFromCurrentDirection implements Comparator<ObjectWithId> {
        private boolean ascending;
        public SortByDistanceFromCurrentDirection(boolean ascending) {
            this.ascending = ascending;
        }
        @Override public int compare(ObjectWithId object1, ObjectWithId object2) {
            if (object1 instanceof Point && object2 instanceof Point) {
                Point point1 = (Point) object1;
                Point point2 = (Point) object2;
                if (point1.distanceFromCurrentLocation() != null
                        && point2.distanceFromCurrentLocation() != null) {
                    Integer distancePoint1 = point1.distanceFromCurrentLocation();
                    Integer distancePoint2 = point2.distanceFromCurrentLocation();
                    if (this.ascending) {
                        return distancePoint1.compareTo(distancePoint2);
                    } else {
                        return distancePoint2.compareTo(distancePoint1);
                    }
                }
            }
            return 0;
        }
    }

}
