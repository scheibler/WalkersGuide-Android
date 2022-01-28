package org.walkersguide.android.server.wg.p2p;

import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.data.profile.ProfileGroup;
import org.walkersguide.android.server.wg.p2p.wayclass.WayClassType;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.server.wg.WgException;
import org.walkersguide.android.server.ServerUtility;
import org.walkersguide.android.server.wg.WgUtility;
import org.walkersguide.android.server.ServerTask;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.json.JSONObject;
import org.json.JSONException;
import timber.log.Timber;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.data.object_with_id.Point;
import org.json.JSONArray;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.database.SortMethod;


public class P2pRouteTask extends ServerTask {

    private P2pRouteRequest request;
    private WayClassWeightSettings wayClassWeightSettings;

    public P2pRouteTask(P2pRouteRequest request, WayClassWeightSettings wayClassWeightSettings) {
        this.request = request;
        this.wayClassWeightSettings = wayClassWeightSettings;
    }

    @Override public void execute() throws WgException {
        Point startPoint = this.request.getStartPoint();
        Point destinationPoint = this.request.getDestinationPoint();
        if (startPoint == null || destinationPoint == null) {
            throw new WgException(WgException.RC_START_OR_DESTINATION_MISSING);
        }

        // create server param list
        JSONObject jsonServerParams = null;
        try {
            jsonServerParams = WgUtility.createServerParamList();

            // start, via and destination points
            JSONArray jsonSourcePoints = new JSONArray();
            jsonSourcePoints.put(startPoint.toJson());
            JSONArray jsonViaPointList = createViaPointJsonArray();
            for (int i=0; i<jsonViaPointList.length(); i++) {
                jsonSourcePoints.put(jsonViaPointList.getJSONObject(i));
            }
            jsonSourcePoints.put(destinationPoint.toJson());
            jsonServerParams.put("source_points", jsonSourcePoints);

            // excluded ways
            JSONArray jsonExcludedWays = new JSONArray();
            DatabaseProfileRequest databaseProfileRequest = new DatabaseProfileRequest(
                    DatabaseProfile.excludedRoutingSegments(), null, SortMethod.ACCESSED_DESC);
            for (ObjectWithId objectWithId : AccessDatabase.getInstance().getObjectListFor(databaseProfileRequest)) {
                if (objectWithId instanceof Segment) {
                    Segment segmentToExclude = (Segment) objectWithId;
                    jsonExcludedWays.put(segmentToExclude.getId());
                }
            }
            if (jsonExcludedWays.length() > 0) {
                jsonServerParams.put("blocked_ways", jsonExcludedWays);
            }

            // allowed way classes
            JSONObject jsonWayClassTypeAndWeightMappings = new JSONObject();
            for (WayClassType type : WayClassType.values()) {
                jsonWayClassTypeAndWeightMappings.put(
                        type.id,
                        wayClassWeightSettings.getWeightFor(type).weight);
            }
            jsonServerParams.put(
                    "allowed_way_classes", jsonWayClassTypeAndWeightMappings);

            Timber.d("jsonServerParams: %1$s", jsonServerParams.toString());
        } catch (JSONException e) {
            throw new WgException(WgException.RC_BAD_REQUEST);
        }

        ServerInstance serverInstance = WgUtility.getServerInstanceForServerUrlFromSettings();
        JSONObject jsonServerResponse = ServerUtility.performRequestAndReturnJsonObject(
                String.format(
                    "%1$s/get_route", serverInstance.getServerURL()),
                jsonServerParams,
                WgException.class);

        Route route = null;
        try {
            // convert flat route object list
            JSONArray jsonRouteObjectList = new JSONArray();
            JSONArray jsonFlatRouteObjectList = jsonServerResponse.getJSONArray("route");
            for (int i=0; i<jsonFlatRouteObjectList.length(); i+=2) {
                JSONObject jsonRouteObject = new JSONObject();
                if (i > 0) {
                    jsonRouteObject.put("segment", jsonFlatRouteObjectList.getJSONObject(i-1));
                }
                jsonRouteObject.put("point", jsonFlatRouteObjectList.getJSONObject(i));
                jsonRouteObjectList.put(jsonRouteObject);
            }

            route = new Route(
                    Route.convertRouteFromWebserverApiV4ToV5(
                        startPoint.toJson(),
                        destinationPoint.toJson(),
                        createViaPointJsonArray(),
                        jsonServerResponse.getString("description"),
                        jsonRouteObjectList));
        } catch (JSONException e) {
            throw new WgException(WgException.RC_BAD_RESPONSE);
        }

        if (! isCancelled()) {
            ServerTaskExecutor.sendP2pRouteTaskSuccessfulBroadcast(getId(), route);
        }
    }

    private JSONArray createViaPointJsonArray() throws JSONException {
        JSONArray jsonViaPointList = new JSONArray();
        Point viaPoint1 = this.request.getViaPoint1();
        if (viaPoint1 != null) {
            jsonViaPointList.put(viaPoint1.toJson());
        }
        Point viaPoint2 = this.request.getViaPoint2();
        if (viaPoint2 != null) {
            jsonViaPointList.put(viaPoint2.toJson());
        }
        Point viaPoint3 = this.request.getViaPoint3();
        if (viaPoint3 != null) {
            jsonViaPointList.put(viaPoint3.toJson());
        }
        return jsonViaPointList;
    }

}
