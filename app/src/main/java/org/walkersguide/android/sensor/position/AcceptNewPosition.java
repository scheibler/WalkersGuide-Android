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
                30, 15000l, PositionManager.getInstance().getCurrentLocation());
    }

    public static AcceptNewPosition newInstanceForDistanceLabelUpdate() {
        return new AcceptNewPosition(2, 1000l, null);
    }

    public static AcceptNewPosition newInstanceForTtsAnnouncement() {
        return new AcceptNewPosition(
                SettingsManager.getInstance().getTtsSettings().getDistanceAnnouncementInterval(),
                10000l,
                PositionManager.getInstance().getCurrentLocation());
    }

    public static AcceptNewPosition newInstanceForTtsAnnouncementOnFocus() {
        return new AcceptNewPosition(
                4, 3000l, PositionManager.getInstance().getCurrentLocation());
    }


    private final int distanceThreshold;
    private final long timeThreshold;

    private Point lastAcceptedPoint;
    private long lastAcceptedPointTimestamp;

    public AcceptNewPosition(int distanceThresholdInMeters, long timeThresholdInMs, Point initAcceptedPoint) {
        this.distanceThreshold = distanceThresholdInMeters;
        this.timeThreshold = timeThresholdInMs;
        this.lastAcceptedPoint = initAcceptedPoint;
        this.lastAcceptedPointTimestamp = 0l;
    }

    public boolean updatePoint(Point newPoint, boolean parentViewInBackground, boolean newPointIsImportant) {
        return updatePoint(newPoint, parentViewInBackground, newPointIsImportant, true);
    }

    public boolean updatePoint(Point newPoint, boolean parentViewInBackground,
            boolean newPointIsImportant, boolean allowOrdinaryPoints) {
        if (newPoint == null) { return false; }

        boolean mustUpdate = false;
        if (parentViewInBackground) {
            mustUpdate = newPointIsImportant;
        } else if (newPointIsImportant) {
            mustUpdate = true;
        } else if (allowOrdinaryPoints) {
            mustUpdate = checkPoint(newPoint);
        }

        if (mustUpdate) {
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
        } else if (System.currentTimeMillis() - this.lastAcceptedPointTimestamp < this.timeThreshold) {
            return false;
        }
        return true;
    }

}
