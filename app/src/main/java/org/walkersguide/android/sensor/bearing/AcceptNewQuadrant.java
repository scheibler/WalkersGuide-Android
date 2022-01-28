package org.walkersguide.android.sensor.bearing;

import java.io.Serializable;
import org.walkersguide.android.data.Angle.Quadrant;


public class AcceptNewQuadrant implements Serializable {
    private static final long serialVersionUID = 1l;

    public static AcceptNewQuadrant newInstanceForObjectListSort() {
        return new AcceptNewQuadrant(2);
    }

    private final int timeThreshold;

    private Quadrant lastAcceptedQuadrant;
    private long lastAcceptedQuadrantTimestamp;

    private AcceptNewQuadrant(int timeThresholdInSeconds) {
        this.timeThreshold = timeThresholdInSeconds;
        this.lastAcceptedQuadrant = Quadrant.Q0;
        this.lastAcceptedQuadrantTimestamp = System.currentTimeMillis();
    }

    public Quadrant getLastQuadrant() {
        return this.lastAcceptedQuadrant;
    }

    public boolean updateQuadrant(Quadrant newQuadrant) {
        if (checkQuadrant(newQuadrant)) {
            this.lastAcceptedQuadrant = newQuadrant;
            this.lastAcceptedQuadrantTimestamp = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    private  boolean checkQuadrant(Quadrant newQuadrant) {
        if (newQuadrant == null) {
            return false;
        } else if (this.lastAcceptedQuadrant == null) {
            return true;
        } else if (this.lastAcceptedQuadrant.equals(newQuadrant)) {
            return false;
        } else if (System.currentTimeMillis() - this.lastAcceptedQuadrantTimestamp < this.timeThreshold * 1000l) {
            return false;
        }
        return true;
    }

}
