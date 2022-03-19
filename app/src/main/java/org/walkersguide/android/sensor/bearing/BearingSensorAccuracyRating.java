package org.walkersguide.android.sensor.bearing;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;


public enum BearingSensorAccuracyRating {

    LOW(1, GlobalInstance.getStringResource(R.string.bearingSensorAccuracyLow)),
    MEDIUM(2, GlobalInstance.getStringResource(R.string.bearingSensorAccuracyMedium)),
    HIGH(3, GlobalInstance.getStringResource(R.string.bearingSensorAccuracyHigh));


    public static BearingSensorAccuracyRating lookUpById(Integer id) {
        if (id != null) {
            for (BearingSensorAccuracyRating rating : BearingSensorAccuracyRating.values()) {
                if (rating.id == id) {
                    return rating;
                }
            }
        }
        return null;
    }


    public int id;
    public String name;

    private BearingSensorAccuracyRating(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override public String toString() {
        return this.name;
    }

}
