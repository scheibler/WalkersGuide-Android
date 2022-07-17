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
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Locale;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.sensor.DeviceSensorManager;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.walkersguide.android.util.GlobalInstance;


public abstract class ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;


    /**
     * id generation
     */
    public static final int NUMBER_OF_LOCAL_IDS = Integer.MAX_VALUE - 1;
    public static final long FIRST_LOCAL_ID = Long.MAX_VALUE - NUMBER_OF_LOCAL_IDS;

    public static long generateId() {
        return FIRST_LOCAL_ID + (new Random()).nextInt(NUMBER_OF_LOCAL_IDS);
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
    public static final String ACTION_NAME_CHANGED = "nameChanged";

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
        boolean success = AccessDatabase.getInstance().addObjectWithId(this, newName);
        if (success) {
            Intent nameChangedIntent = new Intent(ACTION_NAME_CHANGED);
            LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(nameChangedIntent);
        }
        return success;
    }

    // favorite
    public static final String ACTION_FAVORITE_STATUS_CHANGED = "favoriteStatusChanged";

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
            boolean success = getDefaultFavoritesProfile().add(this);
            if (success) {
                Intent favoriteStatusChangedIntent = new Intent(ACTION_FAVORITE_STATUS_CHANGED);
                LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(favoriteStatusChangedIntent);
            }
            return success;
        }
        return false;
    }

    public boolean removeFromFavorites() {
        if (hasDefaultFavoritesProfile()) {
            boolean success = getDefaultFavoritesProfile().remove(this);
            if (success) {
                Intent favoriteStatusChangedIntent = new Intent(ACTION_FAVORITE_STATUS_CHANGED);
                LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(favoriteStatusChangedIntent);
            }
            return success;
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
                return object1.getName().toLowerCase(Locale.ROOT).compareTo(
                        object2.getName().toLowerCase(Locale.ROOT));
            } else {
                return object2.getName().toLowerCase(Locale.ROOT).compareTo(
                        object1.getName().toLowerCase(Locale.ROOT));
            }
        }
    }


    public static class SortByBearingRelativeTo implements Comparator<ObjectWithId> {
        private Bearing initialViewingDirection;
        private int offsetInDegree;
        private boolean ascending;

        public static SortByBearingRelativeTo currentBearing(int offsetInDegree, boolean ascending) {
            return new SortByBearingRelativeTo(null, offsetInDegree, ascending);
        }

        public SortByBearingRelativeTo(Bearing initialViewingDirection, int offsetInDegree, boolean ascending) {
            this.initialViewingDirection = initialViewingDirection;
            this.offsetInDegree = offsetInDegree;
            this.ascending = ascending;
        }

        @Override public int compare(ObjectWithId object1, ObjectWithId object2) {
            // take the current bearing, if no viewing direction was loaded via constructor
            Bearing viewingDirection = this.initialViewingDirection != null
                ? this.initialViewingDirection
                : DeviceSensorManager.getInstance().getCurrentBearing();
            if (viewingDirection != null
                    && object1 instanceof Segment && object2 instanceof Segment) {
                RelativeBearing bearing1 = ((Segment) object1).getBearing().relativeTo(viewingDirection);;
                RelativeBearing bearing2 = ((Segment) object2).getBearing().relativeTo(viewingDirection);;
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
