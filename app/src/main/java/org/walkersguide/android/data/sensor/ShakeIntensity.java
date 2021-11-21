package org.walkersguide.android.data.sensor;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;


public enum ShakeIntensity {

    VERY_WEAK(100, GlobalInstance.getStringResource(R.string.shakeIntensityVeryWeak)),
    WEAK(400, GlobalInstance.getStringResource(R.string.shakeIntensityWeak)),
    MEDIUM(700, GlobalInstance.getStringResource(R.string.shakeIntensityMedium)),
    STRONG(1000, GlobalInstance.getStringResource(R.string.shakeIntensityStrong)),
    VERY_STRONG(1300, GlobalInstance.getStringResource(R.string.shakeIntensityVeryStrong)),
    DISABLED(1000000, GlobalInstance.getStringResource(R.string.shakeIntensityDisabled));


    public int threshold;
    public String name;

    private ShakeIntensity(int threshold, String name) {
        this.threshold = threshold;
        this.name = name;
    }

    @Override public String toString() {
        return this.name;
    }

}
