package org.walkersguide.android.sensor.bearing;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;


public enum BearingSensor {

    COMPASS(GlobalInstance.getStringResource(R.string.bearingSensorCompass)),
    SATELLITE(GlobalInstance.getStringResource(R.string.bearingSensorSatellite));


    private String name;

    private BearingSensor(String name) {
        this.name = name;
    }

    @Override public String toString() {
        return this.name;
    }

}
