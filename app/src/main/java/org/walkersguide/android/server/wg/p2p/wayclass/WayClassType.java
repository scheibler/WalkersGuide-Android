package org.walkersguide.android.server.wg.p2p.wayclass;

import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;


public enum WayClassType {

    BIG_STREETS(
            "big_streets",
            GlobalInstance.getStringResource(R.string.wayClassTypeBigStreets),
            WayClassWeight.SLIGHTLY_PREFER),
    SMALL_STREETS(
            "small_streets",
            GlobalInstance.getStringResource(R.string.wayClassTypeSmallStreets),
            WayClassWeight.STRONGLY_PREFER),
    PAVED_WAYS(
            "paved_ways",
            GlobalInstance.getStringResource(R.string.wayClassTypePavedWays),
            WayClassWeight.NEUTRAL),
    UNPAVED_WAYS(
            "unpaved_ways",
            GlobalInstance.getStringResource(R.string.wayClassTypeUnpavedWays),
            WayClassWeight.AVOID),
    STEPS(
            "steps",
            GlobalInstance.getStringResource(R.string.wayClassTypeSteps),
            WayClassWeight.SLIGHTLY_AVOID),
    UNCLASSIFIED_WAYS(
            "unclassified_ways",
            GlobalInstance.getStringResource(R.string.wayClassTypeUnclassifiedWays),
            WayClassWeight.AVOID);


    public String id, name;
    public WayClassWeight defaultWeight;

    private WayClassType(String id, String name, WayClassWeight defaultWeight) {
        this.id = id;
        this.name = name;
        this.defaultWeight = defaultWeight;
    }

    @Override public String toString() {
        return this.name;
    }

}
