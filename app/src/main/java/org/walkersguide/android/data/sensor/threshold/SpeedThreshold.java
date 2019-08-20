package org.walkersguide.android.data.sensor.threshold;

import java.util.HashMap;
import java.util.Map;


public enum SpeedThreshold {
    ZERO_KMH(0),
    FOUR_KMH(4),
    SEVEN_KMH(7);

    private int speedThresholdInKMH;
    private static final Map<Integer,SpeedThreshold> valuesBySpeedThreshold;

    static {
        valuesBySpeedThreshold = new HashMap<Integer,SpeedThreshold>();
        for(SpeedThreshold speedThreshold : SpeedThreshold.values()) {
            valuesBySpeedThreshold.put(
                    speedThreshold.getSpeedThresholdInKMH(), speedThreshold);
        }
    }

    public static SpeedThreshold lookupBySpeedThreshold(int speedThresholdInKMH) {
        return valuesBySpeedThreshold.get(speedThresholdInKMH);
    }

    private SpeedThreshold(int speedThresholdInKMH) {
        this.speedThresholdInKMH = speedThresholdInKMH;
    }

    public int getSpeedThresholdInKMH() {
        return this.speedThresholdInKMH;
    }

    // compare

    public boolean isAtLeast(SpeedThreshold otherSpeedThreshold) {
        return this.speedThresholdInKMH >= otherSpeedThreshold.getSpeedThresholdInKMH();
    }

    public boolean isAtMost(SpeedThreshold otherSpeedThreshold) {
        return this.speedThresholdInKMH <= otherSpeedThreshold.getSpeedThresholdInKMH();
    }

}
