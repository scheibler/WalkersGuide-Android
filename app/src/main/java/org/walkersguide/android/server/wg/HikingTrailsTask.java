package org.walkersguide.android.server.wg;

import org.walkersguide.android.data.object_with_id.HikingTrail;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.server.wg.WgException;
import org.walkersguide.android.server.ServerUtility;
import org.walkersguide.android.server.wg.WgUtility;
import org.walkersguide.android.server.ServerTask;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import org.walkersguide.android.data.object_with_id.Point;
import org.json.JSONArray;


public class HikingTrailsTask extends ServerTask {
    public static final int DEFAULT_TRAIL_RADIUS = 25000;

    private Point currentLocation;

    public HikingTrailsTask(Point currentLocation) {
        this.currentLocation = currentLocation;
    }

    @Override public void execute() throws WgException {
        // check current location
        if (this.currentLocation == null) {
            throw new WgException(WgException.RC_BAD_REQUEST);
        }

        // create server param list
        JSONObject jsonServerParams = null;
        try {
            jsonServerParams = WgUtility.createServerParamList();
            jsonServerParams.put("lat", this.currentLocation.getLatitude());
            jsonServerParams.put("lon", this.currentLocation.getLongitude());
            jsonServerParams.put("radius", DEFAULT_TRAIL_RADIUS);
        } catch (JSONException e) {
            throw new WgException(WgException.RC_BAD_REQUEST);
        }

        ServerInstance serverInstance = WgUtility.getServerInstanceForServerUrlFromSettings();
        JSONObject jsonServerResponse = ServerUtility.performRequestAndReturnJsonObject(
                String.format(
                    "%1$s/get_hiking_trails", serverInstance.getServerURL()),
                jsonServerParams,
                WgException.class);

        ArrayList<HikingTrail> hikingTrailList = new ArrayList<HikingTrail>();
        try {
            JSONArray jsonHikingTrailList = jsonServerResponse.getJSONArray("hiking_trails");
            for (int i=0; i<jsonHikingTrailList.length(); i++) {
                hikingTrailList.add(
                        new HikingTrail(
                            jsonHikingTrailList.getJSONObject(i)));
            }
        } catch (JSONException e) {
            throw new WgException(WgException.RC_BAD_RESPONSE);
        }

        if (! isCancelled()) {
            ServerTaskExecutor.sendHikingTrailsTaskSuccessfulBroadcast(getId(), hikingTrailList);
        }
    }

}
