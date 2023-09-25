package org.walkersguide.android.data;

import org.walkersguide.android.R;
import org.walkersguide.android.data.object_with_id.common.ObjectClass;
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
import org.walkersguide.android.data.object_with_id.Route;
import java.util.ArrayList;


public abstract class ObjectWithId implements Serializable {
    private static final long serialVersionUID = 1l;

    public static ObjectWithId create(ObjectClass objectClass, JSONObject jsonObject) throws JSONException {
        if (objectClass != null) {
            switch (objectClass) {
                case POINT:
                    return Point.create(jsonObject);
                case ROUTE:
                    return Route.create(jsonObject);
                case SEGMENT:
                    return Segment.create(jsonObject);
            }
        }
        throw new JSONException("Invalid object class");
    }


    /**
     * id generation
     */
    public static final int NUMBER_OF_LOCAL_IDS = Integer.MAX_VALUE - 1;
    public static final long FIRST_LOCAL_ID = Long.MAX_VALUE - NUMBER_OF_LOCAL_IDS;

    public static long generateId() {
        return FIRST_LOCAL_ID + (new Random()).nextInt(NUMBER_OF_LOCAL_IDS);
    }

    public static String summarizeObjectListContents(ArrayList<ObjectWithId> objectList) {
        // count
        int points = 0, segments = 0, routes = 0;
        for (ObjectWithId object : objectList) {
            if (object instanceof Point) {
                points++;
            } else if (object instanceof Segment) {
                segments++;
            } else if (object instanceof Route) {
                routes++;
            }
        }
        // format strings
        ArrayList<String> stringList = new ArrayList<String>();
        if (segments > 0) {
            stringList.add(
                    GlobalInstance.getPluralResource(R.plurals.way, segments));
        }
        if (routes > 0) {
            stringList.add(
                    GlobalInstance.getPluralResource(R.plurals.route, routes));
        }
        if (points > 0                      // must come last
                || stringList.isEmpty()) {  // special case for the empty list: return string "0 points"
            stringList.add(
                    0,      // put on top of the stringList
                    GlobalInstance.getPluralResource(R.plurals.point, points));
        }
        // concatenate and return
        String result = stringList.get(0);
        for (int i=1; i<stringList.size(); i++) {
            result += i+1 == stringList.size()      // check if last list item
                ? String.format(" %1$s ", GlobalInstance.getStringResource(R.string.fillingWordAnd))
                : ", ";
            result += stringList.get(i);
        }
        return result;
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

    public abstract String formatNameAndSubType();

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

    public boolean wasRenamed() {
        return ! TextUtils.isEmpty(getCustomName());
    }

    public abstract ObjectClass getObjectClass();


    // database

    public boolean addToDatabase() {
        return AccessDatabase.getInstance().addObjectWithId(this);
    }

    public boolean removeFromDatabase() {
        return AccessDatabase.getInstance().removeObjectWithId(this);
    }


    // super class methods

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
