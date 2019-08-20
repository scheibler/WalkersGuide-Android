package org.walkersguide.android.data.sensor.threshold;

import java.util.HashMap;
import java.util.Map;


public enum BearingThreshold {
    ZERO_DEGREES(0),
    FIVE_DEGREES(5),
    TEN_DEGREES(10),
    FIFTEEN_DEGREES(15),
    TWENTY_DEGREES(20);

    private int bearingThresholdInDegrees;
    private static final Map<Integer,BearingThreshold> valuesByBearingThreshold;

    static {
        valuesByBearingThreshold = new HashMap<Integer,BearingThreshold>();
        for(BearingThreshold bearingThreshold : BearingThreshold.values()) {
            valuesByBearingThreshold.put(
                    bearingThreshold.getBearingThresholdInDegrees(), bearingThreshold);
        }
    }

    public static BearingThreshold lookupByBearingThreshold(int bearingThresholdInDegrees) {
        return valuesByBearingThreshold.get(bearingThresholdInDegrees);
    }

    private BearingThreshold(int bearingThresholdInDegrees) {
        this.bearingThresholdInDegrees = bearingThresholdInDegrees;
    }

    public int getBearingThresholdInDegrees() {
        return this.bearingThresholdInDegrees;
    }

    // compare

    public boolean isAtLeast(BearingThreshold otherBearingThreshold) {
        return this.bearingThresholdInDegrees >= otherBearingThreshold.getBearingThresholdInDegrees();
    }

    public boolean isAtMost(BearingThreshold otherBearingThreshold) {
        return this.bearingThresholdInDegrees <= otherBearingThreshold.getBearingThresholdInDegrees();
    }

}
