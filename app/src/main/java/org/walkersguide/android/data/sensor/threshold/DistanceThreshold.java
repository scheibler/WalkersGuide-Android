package org.walkersguide.android.data.sensor.threshold;

import java.util.HashMap;
import java.util.Map;


public enum DistanceThreshold {
    ZERO_METERS(0),
    TEN_METERS(10),
    TWENTY_METERS(20),
    FIFTY_METERS(50),
    ONE_HUNDRED_METERS(100),
    TWO_HUNDRED_FIFTY_METERS(250);

    private int distanceThresholdInMeters;
    private static final Map<Integer,DistanceThreshold> valuesByDistanceThreshold;

    static {
        valuesByDistanceThreshold = new HashMap<Integer,DistanceThreshold>();
        for(DistanceThreshold distanceThreshold : DistanceThreshold.values()) {
            valuesByDistanceThreshold.put(
                    distanceThreshold.getDistanceThresholdInMeters(), distanceThreshold);
        }
    }

    public static DistanceThreshold lookupByDistanceThreshold(int distanceThresholdInMeters) {
        return valuesByDistanceThreshold.get(distanceThresholdInMeters);
    }

    private DistanceThreshold(int distanceThresholdInMeters) {
        this.distanceThresholdInMeters = distanceThresholdInMeters;
    }

    public int getDistanceThresholdInMeters() {
        return this.distanceThresholdInMeters;
    }

    // compare

    public boolean isAtLeast(DistanceThreshold otherDistanceThreshold) {
        return this.distanceThresholdInMeters >= otherDistanceThreshold.getDistanceThresholdInMeters();
    }

    public boolean isAtMost(DistanceThreshold otherDistanceThreshold) {
        return this.distanceThresholdInMeters <= otherDistanceThreshold.getDistanceThresholdInMeters();
    }

}
