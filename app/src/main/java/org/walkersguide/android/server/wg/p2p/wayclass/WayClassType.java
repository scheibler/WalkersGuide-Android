package org.walkersguide.android.server.wg.p2p.wayclass;

import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;


public enum WayClassType {

    BIG_STREETS(GlobalInstance.getStringResource(R.string.wayClassTypeBigStreets)),
    SMALL_STREETS(GlobalInstance.getStringResource(R.string.wayClassTypeSmallStreets)),
    PAVED_WAYS(GlobalInstance.getStringResource(R.string.wayClassTypePavedWays)),
    UNPAVED_WAYS(GlobalInstance.getStringResource(R.string.wayClassTypeUnpavedWays)),
    STEPS(GlobalInstance.getStringResource(R.string.wayClassTypeSteps)),
    UNCLASSIFIED_WAYS(GlobalInstance.getStringResource(R.string.wayClassTypeUnclassifiedWays));


    public String label;

    private WayClassType(String label) {
        this.label = label;
    }

    @Override public String toString() {
        return this.label;
    }

}
