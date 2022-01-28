package org.walkersguide.android.sensor.position;

import java.io.Serializable;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.sensor.PositionManager;


public class AcceptNewPosition implements Serializable {
    private static final long serialVersionUID = 1l;

    public static AcceptNewPosition newInstanceForTtsAnnouncement() {
        return new AcceptNewPosition(20, 10);
    }

    private final int distanceThreshold, timeThreshold;

    private Point lastAcceptedPoint;
    private long lastAcceptedPointTimestamp;

    private AcceptNewPosition(int distanceThresholdInMeters, int timeThresholdInSeconds) {
        this.distanceThreshold = distanceThresholdInMeters;
        this.timeThreshold = timeThresholdInSeconds;
        this.lastAcceptedPoint = PositionManager.getInstance().getCurrentLocation();
        this.lastAcceptedPointTimestamp = System.currentTimeMillis();
    }

    public Point getLastPoint() {
        return this.lastAcceptedPoint;
    }

    public boolean updatePoint(Point newPoint) {
        if (checkPoint(newPoint)) {
            this.lastAcceptedPoint = newPoint;
            this.lastAcceptedPointTimestamp = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    private  boolean checkPoint(Point newPoint) {
        if (newPoint == null) {
            return false;
        } else if (this.lastAcceptedPoint == null) {
            return true;
        } else if (this.lastAcceptedPoint.distanceTo(newPoint) < this.distanceThreshold) {
            return false;
        } else if (System.currentTimeMillis() - this.lastAcceptedPointTimestamp < this.timeThreshold * 1000l) {
            return false;
        }
        return true;
    }

}
