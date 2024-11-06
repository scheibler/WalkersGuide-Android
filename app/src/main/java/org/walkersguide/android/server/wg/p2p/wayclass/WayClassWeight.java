package org.walkersguide.android.server.wg.p2p.wayclass;

import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;


public enum WayClassWeight {

    STRONGLY_PREFER(
            0.25, GlobalInstance.getStringResource(R.string.wayClassWeightStronglyPrefer)),
    PREFER(
            0.5, GlobalInstance.getStringResource(R.string.wayClassWeightPrefer)),
    SLIGHTLY_PREFER(
            0.75, GlobalInstance.getStringResource(R.string.wayClassWeightSlightlyPrefer)),
    NEUTRAL(
            1.0, GlobalInstance.getStringResource(R.string.wayClassWeightNeutral)),
    SLIGHTLY_AVOID(
            1.33, GlobalInstance.getStringResource(R.string.wayClassWeightSlightlyAvoid)),
    AVOID(
            2.0, GlobalInstance.getStringResource(R.string.wayClassWeightAvoid)),
    STRONGLY_AVOID(
            4.0, GlobalInstance.getStringResource(R.string.wayClassWeightStronglyAvoid)),
    EXCLUDE(
            -1.0, GlobalInstance.getStringResource(R.string.wayClassWeightExclude));


    public double weight;
    public String label;

    private WayClassWeight(double weight, String label) {
        this.weight = weight;
        this.label = label;
    }

    @Override public String toString() {
        return this.label;
    }

}
