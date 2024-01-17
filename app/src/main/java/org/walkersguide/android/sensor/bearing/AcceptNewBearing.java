package org.walkersguide.android.sensor.bearing;

import java.io.Serializable;
import org.walkersguide.android.data.Angle.Quadrant;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.sensor.DeviceSensorManager;


public class AcceptNewBearing implements Serializable {
    private static final long serialVersionUID = 1l;

    public static AcceptNewBearing newInstanceForObjectListUpdate() {
        return new AcceptNewBearing(45, 1);
    }

    public static AcceptNewBearing newInstanceForDistanceLabelUpdate() {
        return new AcceptNewBearing(23, 1);
    }


    private final int angleThreshold, timeThreshold;

    private Bearing lastAcceptedBearing;
    private long lastAcceptedBearingTimestamp;

    public AcceptNewBearing(int angleeThresholdInDegree, int timeThresholdInSeconds) {
        this.angleThreshold = angleeThresholdInDegree;
        this.timeThreshold = timeThresholdInSeconds;
        this.lastAcceptedBearing = DeviceSensorManager.getInstance().getCurrentBearing();
        this.lastAcceptedBearingTimestamp = 0l;
    }

    public boolean updateBearing(Bearing newBearing) {
        if (checkBearing(newBearing)) {
            this.lastAcceptedBearing = newBearing;
            this.lastAcceptedBearingTimestamp = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public  boolean checkBearing(Bearing newBearing) {
        if (newBearing == null) {
            return false;
        } else if (this.lastAcceptedBearing == null) {
            return true;
        } else if (this.lastAcceptedBearing.differenceTo(newBearing) < angleThreshold) {
            return false;
        } else if (System.currentTimeMillis() - this.lastAcceptedBearingTimestamp < this.timeThreshold * 1000l) {
            return false;
        }
        return true;
    }

}
