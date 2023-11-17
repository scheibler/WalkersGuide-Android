package org.walkersguide.android.server.wg.street_course;

import org.walkersguide.android.R;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import java.io.Serializable;
import org.walkersguide.android.util.GlobalInstance;


public class StreetCourseRequest implements Serializable {
    private static final long serialVersionUID = 1l;


    private IntersectionSegment intersectionSegment;

    public StreetCourseRequest(IntersectionSegment intersectionSegment) {
        this.intersectionSegment = intersectionSegment;
    }
        /*
        this.routeName = String.format(
                GlobalInstance.getStringResource(R.string.routeNameWayCourse),
                intersectionSegment.getName(),
                intersectionSegment.getIntersectionName());*/

    public String getStreetCourseName() {
        return String.format(
                GlobalInstance.getStringResource(R.string.routeNameStreetCourse),
                this.intersectionSegment.getName(),
                this.intersectionSegment.getBearing().getOrientation());
    }

    public String getStreetCourseDescription() {
        return String.format(
                GlobalInstance.getStringResource(R.string.routeDescriptionStreetCourse),
                this.intersectionSegment.getIntersectionName(),
                this.intersectionSegment.getName(),
                this.intersectionSegment.getBearing().getOrientation());
    }

    public long getNodeId() {
        return this.intersectionSegment.getIntersectionNodeId();
    }

    public long getWayId() {
        return this.intersectionSegment.getId();
    }

    public long getNextNodeId() {
        return this.intersectionSegment.getNextNodeId();
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Long.valueOf(this.getNodeId()).hashCode();
        result = prime * result + Long.valueOf(this.getWayId()).hashCode();
        result = prime * result + Long.valueOf(this.getNextNodeId()).hashCode();
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
        return this.getNodeId() == other.getNodeId()
            && this.getWayId() == other.getWayId()
            && this.getNextNodeId() == other.getNextNodeId();
    }

}
