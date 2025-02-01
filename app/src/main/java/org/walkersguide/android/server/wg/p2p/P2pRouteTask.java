package org.walkersguide.android.server.wg.p2p;

import org.walkersguide.android.server.address.AddressException;
import org.walkersguide.android.R;
import org.walkersguide.android.database.profile.StaticProfile;
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
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.segment.RouteSegment;
import org.walkersguide.android.data.angle.Turn;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.server.address.ResolveCoordinatesTask;


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

        // if the start point is a nameless gps point then try to replace its name with the closest address nearby
        if (this.request.getReplaceNameOfStartPointWithClosestAddress()) {
            Timber.d("start point gps: %1$s", startPoint.getName());
            StreetAddress closestAddress = null;
            try {
                closestAddress = ResolveCoordinatesTask.getAddress(startPoint);
            } catch (AddressException e) {}
            if (closestAddress != null) {
                startPoint.setCustomNameInDatabase(
                        String.format(
                            "%1$s %2$s",
                            GlobalInstance.getStringResource(R.string.labelNearby),
                            closestAddress.formatAddressShortLength()));
                Timber.d("renamed to %1$s", startPoint.getName());
            }
        }

        // create server param list
        JSONObject jsonServerParams = null;
        try {
            jsonServerParams = WgUtility.createServerParamList();

            // start, via and destination points
            JSONArray jsonSourcePoints = new JSONArray();
            jsonSourcePoints.put(startPoint.toJson());
            Point viaPoint1 = this.request.getViaPoint1();
            if (viaPoint1 != null) {
                jsonSourcePoints.put(viaPoint1.toJson());
            }
            Point viaPoint2 = this.request.getViaPoint2();
            if (viaPoint2 != null) {
                jsonSourcePoints.put(viaPoint2.toJson());
            }
            Point viaPoint3 = this.request.getViaPoint3();
            if (viaPoint3 != null) {
                jsonSourcePoints.put(viaPoint3.toJson());
            }
            jsonSourcePoints.put(destinationPoint.toJson());
            jsonServerParams.put("source_points", jsonSourcePoints);

            // excluded ways
            JSONArray jsonExcludedWays = new JSONArray();
            DatabaseProfileRequest databaseProfileRequest = new DatabaseProfileRequest(
                    StaticProfile.excludedRoutingSegments(), null, SortMethod.ACCESSED_DESC);
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
            jsonServerParams.put(
                    "allowed_way_classes", this.wayClassWeightSettings.toJson());

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
            Route.Builder routeBuilder = new Route.Builder(
                    Route.Type.P2P_ROUTE, null, startPoint, destinationPoint)
                .setViaPoints(
                        this.request.getViaPoint1(),
                        this.request.getViaPoint2(),
                        this.request.getViaPoint3());
            routeBuilder.setDescription(
                    jsonServerResponse.getString("description"));

            JSONArray jsonFlatRouteObjectList = jsonServerResponse.getJSONArray("route");
            for (int i=0; i<jsonFlatRouteObjectList.length(); i+=2) {
                JSONObject jsonCurrent = jsonFlatRouteObjectList.getJSONObject(i);
                Point current = Point.fromJson(jsonCurrent);
                if (current == null) {
                    throw new JSONException("Parsing error");
                }

                if (i == 0) {
                    routeBuilder.addFirstRouteObject(current, true);
                    continue;
                }

                RouteSegment betweenPreviousAndCurrent = new RouteSegment(
                        jsonFlatRouteObjectList.getJSONObject(i-1));
                if (i == jsonFlatRouteObjectList.length() - 1) {
                    routeBuilder.addLastRouteObject(betweenPreviousAndCurrent, current, true);
                } else {
                    Turn turn = new Turn(jsonCurrent.getInt("turn"));
                    boolean isImportant =
                           current.equals(this.request.getViaPoint1())
                        || current.equals(this.request.getViaPoint2())
                        || current.equals(this.request.getViaPoint3());
                    routeBuilder.addRouteObject(betweenPreviousAndCurrent, current, turn, isImportant);
                }
            }

            route = routeBuilder.build();
        } catch (JSONException e) {
            throw new WgException(WgException.RC_BAD_RESPONSE);
        }

        if (! isCancelled()) {
            ServerTaskExecutor.sendP2pRouteTaskSuccessfulBroadcast(getId(), route);
        }
    }

}
