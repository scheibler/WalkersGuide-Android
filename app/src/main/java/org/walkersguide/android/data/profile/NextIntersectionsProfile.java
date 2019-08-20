package org.walkersguide.android.data.profile;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;

import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;


public class NextIntersectionsProfile extends PointProfile {

    private long nodeId, wayId, nextNodeId;

    public NextIntersectionsProfile(Context context, long nodeId, long wayId, long nextNodeId) throws JSONException {
        super(
                context,
                -2,
                context.getResources().getString(R.string.fpNameNextIntersections),
                null,
                null,
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

}
