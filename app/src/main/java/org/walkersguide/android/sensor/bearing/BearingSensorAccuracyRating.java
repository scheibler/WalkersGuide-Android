package org.walkersguide.android.sensor.bearing;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;


public enum BearingSensorAccuracyRating {

    LOW, MEDIUM, HIGH;


    public String getDisplayName() {
        switch (this) {
            case LOW: return GlobalInstance.getStringResource(R.string.bearingSensorAccuracyLow);
            case MEDIUM: return GlobalInstance.getStringResource(R.string.bearingSensorAccuracyMedium);
            case HIGH: return GlobalInstance.getStringResource(R.string.bearingSensorAccuracyHigh);
            default: return name();
        }
    }

    @Override public String toString() {
        return this.getDisplayName();
    }

}
