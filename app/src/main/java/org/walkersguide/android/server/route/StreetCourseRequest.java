package org.walkersguide.android.server.route;

import java.io.Serializable;


public class StreetCourseRequest implements Serializable {
    private static final long serialVersionUID = 1l;


    private long nodeId, wayId, nextNodeId;

    public StreetCourseRequest(long nodeId, long wayId, long nextNodeId) {
        this.nodeId = nodeId;
        this.wayId = wayId;
        this.nextNodeId = nextNodeId;
    }

    public long getNodeId() {
        return this.nodeId;
    }

    public long getWayId() {
        return this.wayId;
    }

    public long getNextNodeId() {
        return this.nextNodeId;
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Long.valueOf(this.nodeId).hashCode();
        result = prime * result + Long.valueOf(this.wayId).hashCode();
        result = prime * result + Long.valueOf(this.nextNodeId).hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof StreetCourseRequest))
            return false;
        StreetCourseRequest other = (StreetCourseRequest) obj;
        return this.nodeId == other.getNodeId()
            && this.wayId == other.getWayId()
            && this.nextNodeId == other.getNextNodeId();
    }

}
