package org.walkersguide.android.data;

import org.walkersguide.android.database.profile.FavoritesProfile;
import org.walkersguide.android.data.angle.RelativeBearing;
import java.util.Comparator;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.Segment;

import java.io.Serializable;
import org.walkersguide.android.util.Helper;
import java.util.Random;
import java.lang.Math;
import android.text.TextUtils;
import org.walkersguide.android.database.util.AccessDatabase;
import timber.log.Timber;
import org.json.JSONObject;
import org.json.JSONException;


public abstract class ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;


    /**
     * id generation
     */
    private static final long FIRST_LOCAL_ID = Long.MAX_VALUE - Integer.MAX_VALUE;

    public static long generateId() {
        return FIRST_LOCAL_ID + (new Random()).nextInt(Integer.MAX_VALUE);
    }


    /**
     * constructor
     */

    private long id;

    public ObjectWithId(Long idFromJson) {
        if (idFromJson == null) {
            this.id = generateId();
        } else {
            this.id = idFromJson;
        }
    }

    // id

    public long getId() {
        return this.id;
    }

    public Long getOsmId() {
        if (this.id < FIRST_LOCAL_ID) {
            return this.id;
        }
        return null;
    }

    // name

    public String getName() {
        String customName = getCustomName();
        if (! TextUtils.isEmpty(customName)) {
            return customName;
        }
        return getOriginalName();
    }

    public String getCustomName() {
        return AccessDatabase
            .getInstance()
            .getObjectWithIdCustomName(this);
    }

    public abstract String getOriginalName();

    public boolean rename(String newName) {
        return AccessDatabase.getInstance().addObjectWithId(this, newName);
    }

    // favorite

    public abstract FavoritesProfile getDefaultFavoritesProfile();

    public boolean hasDefaultFavoritesProfile() {
        return getDefaultFavoritesProfile() != null;
    }

    public boolean isFavorite() {
        if (hasDefaultFavoritesProfile()) {
            return getDefaultFavoritesProfile().contains(this);
        }
        return false;
    }

    public boolean addToFavorites() {
        if (hasDefaultFavoritesProfile()) {
            return getDefaultFavoritesProfile().add(this);
        }
        return false;
    }

    public boolean removeFromFavorites() {
        if (hasDefaultFavoritesProfile()) {
            return getDefaultFavoritesProfile().remove(this);
        }
        return false;
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

    public abstract JSONObject toJson() throws JSONException;

    public boolean save() {
        return AccessDatabase.getInstance().addObjectWithId(this);
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

    public static class SortByBearingRelativeToCurrentBearing implements Comparator<ObjectWithId> {
        private int offsetInDegree;
        private boolean ascending;
        public SortByBearingRelativeToCurrentBearing(int offsetInDegree, boolean ascending) {
            this.offsetInDegree = offsetInDegree;
            this.ascending = ascending;
        }
        @Override public int compare(ObjectWithId object1, ObjectWithId object2) {
            if (object1 instanceof Segment && object2 instanceof Segment) {
                RelativeBearing bearing1 = ((Segment) object1).getBearing().relativeToCurrentBearing();;
                RelativeBearing bearing2 = ((Segment) object2).getBearing().relativeToCurrentBearing();;
                if (bearing1 != null && bearing2 != null) {
                    if (this.offsetInDegree != 0) {
                        bearing1 = bearing1.shiftBy(this.offsetInDegree);
                        bearing2 = bearing2.shiftBy(this.offsetInDegree);
                    }
                    if (this.ascending) {
                        return bearing1.compareTo(bearing2);
                    } else {
                        return bearing2.compareTo(bearing1);
                    }
                }
            }
            return 1;
        }
    }

    public static class SortByDistanceRelativeToCurrentLocation implements Comparator<ObjectWithId> {
        private boolean ascending;
        public SortByDistanceRelativeToCurrentLocation(boolean ascending) {
            this.ascending = ascending;
        }
        @Override public int compare(ObjectWithId object1, ObjectWithId object2) {
            if (object1 instanceof Point && object2 instanceof Point) {
                Integer distance1 = ((Point) object1).distanceFromCurrentLocation();
                Integer distance2 = ((Point) object2).distanceFromCurrentLocation();
                if (distance1 != null && distance2 != null) {
                    if (this.ascending) {
                        return distance1.compareTo(distance2);
                    } else {
                        return distance2.compareTo(distance1);
                    }
                }
            }
            return 1;
        }
    }

}
