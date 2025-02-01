package org.walkersguide.android.server.wg.p2p;

import org.walkersguide.android.sensor.PositionManager;
import java.io.Serializable;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.object_with_id.point.GPS;


public class P2pRouteRequest implements Serializable {
    private static final long serialVersionUID = 1l;

    public static P2pRouteRequest getDefault() {
        return new P2pRouteRequest(null, null, null, null, null);
    }


    private Long startPointId, destinationPointId;
    private Long viaPoint1Id, viaPoint2Id, viaPoint3Id;
    private boolean replaceNameOfStartPointWithClosestAddress;

    public P2pRouteRequest(Long startPointId, Long destinationPointId,
            Long viaPoint1Id, Long viaPoint2Id, Long viaPoint3Id) {
        this.startPointId = startPointId;
        this.destinationPointId = destinationPointId;
        this.viaPoint1Id = viaPoint1Id;
        this.viaPoint2Id = viaPoint2Id;
        this.viaPoint3Id = viaPoint3Id;
        this.replaceNameOfStartPointWithClosestAddress = false;
    }

    public Point getStartPoint() {
        return getPoint(startPointId);
    }

    public boolean getReplaceNameOfStartPointWithClosestAddress() {
        return this.replaceNameOfStartPointWithClosestAddress;
    }

    public void setStartPoint(Point newStartPoint) {
        this.startPointId = setPoint(newStartPoint);
        this.replaceNameOfStartPointWithClosestAddress = newStartPoint instanceof GPS
            && newStartPoint.equals(PositionManager.getInstance().getGPSLocation());
    }

    public Point getDestinationPoint() {
        return getPoint(destinationPointId);
    }

    public void setDestinationPoint(Point newDestinationPoint) {
        this.destinationPointId = setPoint(newDestinationPoint);
    }

    public Point getViaPoint1() {
        return getPoint(viaPoint1Id);
    }

    public void setViaPoint1(Point newViaPoint1) {
        this.viaPoint1Id = setPoint(newViaPoint1);
    }

    public Point getViaPoint2() {
        return getPoint(viaPoint2Id);
    }

    public void setViaPoint2(Point newViaPoint2) {
        this.viaPoint2Id = setPoint(newViaPoint2);
    }

    public Point getViaPoint3() {
        return getPoint(viaPoint3Id);
    }

    public void setViaPoint3(Point newViaPoint3) {
        this.viaPoint3Id = setPoint(newViaPoint3);
    }

    public void swapStartAndDestinationPoints() {
        Point tempPoint = getStartPoint();
        setStartPoint(getDestinationPoint());
        setDestinationPoint(tempPoint);
    }

    public boolean hasViaPoint() {
        return getViaPoint1() != null || getViaPoint2() != null || getViaPoint3() != null;
    }

    public void clearViaPointList() {
        setViaPoint1(null);
        setViaPoint2(null);
        setViaPoint3(null);
    }

    private Point getPoint(Long id) {
        if (id != null) {
            return Point.load(id);
        }
        return null;
    }

    private Long setPoint(Point newPoint) {
        if (newPoint != null
                && newPoint.saveToDatabase()) {
            return newPoint.getId();
        }
        return null;
    }

}
