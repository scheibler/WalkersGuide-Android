package org.walkersguide.android.data.basic.segment;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.sensor.DirectionManager;

import android.content.Context;

import com.google.common.primitives.Ints;

public class Footway extends Segment {

    private String surface;
    private int bearing, lanes, segregated, sidewalk, tactilePaving, tram, wheelchair;
    private long wayId;
    private double width;

    public Footway(Context context, JSONObject inputData) throws JSONException {
        super(context, inputData);
        this.bearing = inputData.getInt("bearing");
        this.surface = "";
        try {
            this.surface = inputData.getString("surface");
        } catch (JSONException e) {}
        this.lanes = -1;
        try {
            int lanesValue = inputData.getInt("lanes");
            if (lanesValue > 0) {
                this.lanes = lanesValue;
            }
        } catch (JSONException e) {}
        this.segregated = -1;
        try {
            int segregatedValue = inputData.getInt("segregated");
            if (Ints.contains(SegregatedValueArray, segregatedValue)) {
                this.segregated = segregatedValue;
            }
        } catch (JSONException e) {}
        this.sidewalk = -1;
        try {
            int sidewalkValue = inputData.getInt("sidewalk");
            if (Ints.contains(SidewalkValueArray, sidewalkValue)) {
                this.sidewalk = sidewalkValue;
            }
        } catch (JSONException e) {}
        this.tactilePaving = -1;
        try {
            int tactilePavingValue = inputData.getInt("tactile_paving");
            if (Ints.contains(TactilePavingValueArray, tactilePavingValue)) {
                this.tactilePaving = tactilePavingValue;
            }
        } catch (JSONException e) {}
        this.tram = -1;
        try {
            int tramValue = inputData.getInt("tram");
            if (Ints.contains(TramValueArray, tramValue)) {
                this.tram = tramValue;
            }
        } catch (JSONException e) {}
        this.wheelchair = -1;
        try {
            int wheelchairValue = inputData.getInt("wheelchair");
            if (Ints.contains(WheelchairValueArray, wheelchairValue)) {
                this.wheelchair = wheelchairValue;
            }
        } catch (JSONException e) {}
        this.wayId = -1;
        try {
            long wayIdValue = inputData.getInt("way_id");
            if (wayIdValue > 0) {
                this.wayId = wayIdValue;
            }
        } catch (JSONException e) {}
        this.width = -1.0;
        try {
            int widthValue = inputData.getInt("width");
            if (widthValue > 0) {
                this.width = widthValue;
            }
        } catch (JSONException e) {}
    }

    public int getBearing() {
        return this.bearing;
    }

    public String getSurface() {
        return this.surface;
    }

    public int getLanes() {
        return this.lanes;
    }

    public long getWayId() {
        return this.wayId;
    }

    public double getWidth() {
        return this.width;
    }


    /**
     * segregated
     *  -1: unknown (default)
     *   0: no
     *   1: yes
     */

    public interface SEGREGATED {
        public static final int NO = 0;
        public static final int YES = 1;
    }

    public final static int[] SegregatedValueArray = {
        SEGREGATED.NO, SEGREGATED.YES
    };

    public int getSegregated() {
        return this.segregated;
    }


    /**
     * sidewalk
     *  -1: unknown (default)
     *   0: no
     *   1: left
     *   2: right
     *   3: both
     */

    public interface SIDEWALK {
        public static final int NO = 0;
        public static final int LEFT = 1;
        public static final int RIGHT = 2;
        public static final int BOTH = 3;
    }

    public final static int[] SidewalkValueArray = {
        SIDEWALK.NO, SIDEWALK.LEFT, SIDEWALK.RIGHT, SIDEWALK.BOTH
    };

    public int getSidewalk() {
        return this.sidewalk;
    }


    /**
     * tactile paving
     *  -1: unknown (default)
     *   0: no
     *   1: yes
     */

    public interface TACTILE_PAVING {
        public static final int NO = 0;
        public static final int YES = 1;
    }

    public final static int[] TactilePavingValueArray = {
        TACTILE_PAVING.NO, TACTILE_PAVING.YES
    };

    public int getTactilePaving() {
        return this.tactilePaving;
    }


    /**
     * tram
     *  -1: unknown (default)
     *   0: no
     *   1: yes
     */

    public interface TRAM {
        public static final int NO = 0;
        public static final int YES = 1;
    }

    public final static int[] TramValueArray = {
        TRAM.NO, TRAM.YES
    };

    public int getTram() {
        return this.tram;
    }


    /**
     * wheelchair accessibility
     *  -1: unknown (default)
     *   0: no
     *   1: limited
     *   2: yes
     */

    public interface WHEELCHAIR {
        public static final int NO = 0;
        public static final int LIMITED = 1;
        public static final int YES = 2;
    }

    public final static int[] WheelchairValueArray = {
        WHEELCHAIR.NO, WHEELCHAIR.LIMITED, WHEELCHAIR.YES
    };

    public int getWheelchair() {
        return this.wheelchair;
    }


    /**
     * helper functions
     */

    public int bearingFromCurrentDirection() {
        int absoluteDirection = this.bearing;
        // take the current viewing direction into account
        int relativeDirection = absoluteDirection
            - DirectionManager.getInstance(super.getContext()).getCurrentDirection();
        if (relativeDirection < 0) {
            relativeDirection += 360;
        }
        return relativeDirection;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        jsonObject.put("bearing", this.bearing);
        if (! this.surface.equals("")) {
            try {
                jsonObject.put("surface", this.surface);
            } catch (JSONException e) {}
        }
        if (this.lanes > -1) {
            try {
                jsonObject.put("lanes", this.lanes);
            } catch (JSONException e) {}
        }
        if (this.segregated > -1) {
            try {
                jsonObject.put("segregated", this.segregated);
            } catch (JSONException e) {}
        }
        if (this.sidewalk > -1) {
            try {
                jsonObject.put("sidewalk", this.sidewalk);
            } catch (JSONException e) {}
        }
        if (this.tactilePaving > -1) {
            try {
                jsonObject.put("tactile_paving", this.tactilePaving);
            } catch (JSONException e) {}
        }
        if (this.tram > -1) {
            try {
                jsonObject.put("tram", this.tram);
            } catch (JSONException e) {}
        }
        if (this.wheelchair > -1) {
            try {
                jsonObject.put("wheelchair", this.wheelchair);
            } catch (JSONException e) {}
        }
        if (this.wayId > -1) {
            try {
                jsonObject.put("way_id", this.wayId);
            } catch (JSONException e) {}
        }
        if (this.width > 0) {
            try {
                jsonObject.put("width", this.width);
            } catch (JSONException e) {}
        }
        return jsonObject;
    }

    @Override public String toString() {
        return super.toString();
    }

	@Override public int hashCode() {
        int hash = super.hashCode();
		hash = hash * 31 + this.bearing;
		return hash;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof Footway)) {
			return false;
        }
		Footway other = (Footway) obj;
        return super.equals(((Segment) other))
            && this.bearing == other.getBearing();
    }

}
