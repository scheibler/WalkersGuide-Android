package org.walkersguide.android.sensor.position;

import java.io.Serializable;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.sensor.PositionManager;
import timber.log.Timber;
import org.walkersguide.android.util.SettingsManager;


public class AcceptNewPosition implements Serializable {
    private static final long serialVersionUID = 1l;

    public static AcceptNewPosition newInstanceForObjectListUpdate() {
        return new AcceptNewPosition(
                50, 30, PositionManager.getInstance().getCurrentLocation());
    }

    public static AcceptNewPosition newInstanceForTtsAnnouncement() {
        return new AcceptNewPosition(
                SettingsManager.getInstance().getTtsSettings().getDistanceAnnouncementInterval(),
                10,
                PositionManager.getInstance().getCurrentLocation());
    }

    public static AcceptNewPosition newInstanceForDistanceLabelUpdate() {
        return new AcceptNewPosition(3, 3, null);
    }


    private final int distanceThreshold, timeThreshold;

    private Point lastAcceptedPoint;
    private long lastAcceptedPointTimestamp;

    public AcceptNewPosition(int distanceThresholdInMeters, int timeThresholdInSeconds, Point initAcceptedPoint) {
        this.distanceThreshold = distanceThresholdInMeters;
        this.timeThreshold = timeThresholdInSeconds;
        this.lastAcceptedPoint = initAcceptedPoint;
        this.lastAcceptedPointTimestamp = 0l;
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
