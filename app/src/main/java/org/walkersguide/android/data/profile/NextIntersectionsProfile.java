package org.walkersguide.android.data.profile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.util.Constants;

import android.content.Context;

import com.google.common.primitives.Ints;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import java.util.ArrayList;
import org.walkersguide.android.data.basic.point.Intersection;


public class NextIntersectionsProfile extends PointProfile {

    // id of next intersections profile
    public static final int ID_NEXT_INTERSECTIONS = -2;

    private long nodeId, wayId, nextNodeId;

    public NextIntersectionsProfile(Context context, long nodeId, long wayId, long nextNodeId) throws JSONException {
        super(
                context,
                ID_NEXT_INTERSECTIONS,
                context.getResources().getString(R.string.fpNameNextIntersections),
                PositionManager.getDummyLocation(context).toJson(),
                Constants.DUMMY.DIRECTION,
                new JSONArray());
        // node and way ids
        this.nodeId = nodeId;
        this.wayId = wayId;
        this.nextNodeId = nextNodeId;
    }

    public int getSortCriteria() {
        return Constants.SORT_CRITERIA.NONE;
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

    public PointWrapper getNextIntersection() {
        if (! super.getPointProfileObjectList().isEmpty()) {
            return super.getPointProfileObjectList().get(0);
        }
        return null;
    }

    public PointWrapper getNextBigIntersection() {
        for (PointWrapper pointWrapper : super.getPointProfileObjectList()) {
            if (pointWrapper.getPoint() instanceof Intersection
                    && ((Intersection) pointWrapper.getPoint()).getNumberOfStreetsWithName() > 1) {
                return pointWrapper;
            }
        }
        return null;
    }

    public PointWrapper getLastIntersection() {
        if (! super.getPointProfileObjectList().isEmpty()) {
            return super.getPointProfileObjectList().get(super.getPointProfileObjectList().size()-1);
        }
        return null;
    }

}
