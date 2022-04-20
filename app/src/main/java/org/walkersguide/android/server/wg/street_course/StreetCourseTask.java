package org.walkersguide.android.server.wg.street_course;

import org.walkersguide.android.R;
import org.walkersguide.android.data.angle.Turn;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.server.wg.WgException;
import org.walkersguide.android.server.ServerUtility;
import org.walkersguide.android.server.wg.WgUtility;
import org.walkersguide.android.server.ServerTask;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.json.JSONObject;
import org.json.JSONException;
import org.walkersguide.android.data.object_with_id.Route;
import java.util.ArrayList;
import org.walkersguide.android.data.object_with_id.Point;
import org.json.JSONArray;
import org.walkersguide.android.data.object_with_id.segment.RouteSegment;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.util.GlobalInstance;
import java.util.Locale;


public class StreetCourseTask extends ServerTask {

    private StreetCourseRequest request;

    public StreetCourseTask(StreetCourseRequest request) {
        this.request = request;
    }

    @Override public void execute() throws WgException {
        JSONObject jsonServerParams = null;
        try {
            jsonServerParams = WgUtility.createServerParamList();
            jsonServerParams.put("node_id", this.request.getNodeId());
            jsonServerParams.put("way_id", request.getWayId());
            jsonServerParams.put("next_node_id", request.getNextNodeId());
        } catch (JSONException e) {
            throw new WgException(WgException.RC_BAD_REQUEST);
        }

        ServerInstance serverInstance = WgUtility.getServerInstanceForServerUrlFromSettings();
        JSONObject jsonServerResponse = ServerUtility.performRequestAndReturnJsonObject(
                String.format(
                    "%1$s/get_next_intersections_for_way", serverInstance.getServerURL()),
                jsonServerParams,
                WgException.class);

        Route route = null;
        try {
            // parse json point list
            ArrayList<Point> pointList = new ArrayList<Point>();
            JSONArray jsonPointList = jsonServerResponse.getJSONArray("next_intersections");
            for (int i=0; i<jsonPointList.length(); i++) {
                pointList.add(
                        Point.create(jsonPointList.getJSONObject(i)));
            }
            if (pointList.size() <= 1) {
                throw new WgException(WgException.RC_BAD_RESPONSE);
            }
            route = Route.fromPointList(pointList, false);
        } catch (JSONException e) {
            throw new WgException(WgException.RC_BAD_RESPONSE);
        }

        if (! isCancelled()) {
            ServerTaskExecutor.sendStreetCourseTaskSuccessfulBroadcast(getId(), route);
        }
    }

}
