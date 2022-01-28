package org.walkersguide.android.server.wg.p2p.wayclass;

import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;


public enum WayClassWeight {

    VERY_PREFERABLE(
            0.25, GlobalInstance.getStringResource(R.string.wayClassWeightVeryPreferable)),
    PREFERABLE(
            0.5, GlobalInstance.getStringResource(R.string.wayClassWeightPreferable)),
    SLIGHTLY_PREFERABLE(
            0.75, GlobalInstance.getStringResource(R.string.wayClassWeightSlightlyPreferable)),
    NEUTRAL(
            1.0, GlobalInstance.getStringResource(R.string.wayClassWeightNeutral)),
    SLIGHTLY_NEGLIGIBLE(
            1.33, GlobalInstance.getStringResource(R.string.wayClassWeightSlightlyNegligible)),
    NEGLIGIBLE(
            2.0, GlobalInstance.getStringResource(R.string.wayClassWeightNegligible)),
    VERY_NEGLIGIBLE(
            4.0, GlobalInstance.getStringResource(R.string.wayClassWeightVeryNegligible)),
    IGNORE(
            -1.0, GlobalInstance.getStringResource(R.string.wayClassWeightIgnore));


    public double weight;
    public String name;

    private WayClassWeight(double weight, String name) {
        this.weight = weight;
        this.name = name;
    }

    @Override public String toString() {
        return this.name;
    }

}
