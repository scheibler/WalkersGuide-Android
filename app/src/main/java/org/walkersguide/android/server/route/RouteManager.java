package org.walkersguide.android.server.route;

import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.profiles.DatabaseSegmentProfile;
import android.content.Context;

import android.os.AsyncTask;
import android.os.Handler;

import java.io.IOException;

import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.data.route.RouteObject;
import org.walkersguide.android.server.route.WayClass;
import org.walkersguide.android.server.util.ServerInstance;
import org.walkersguide.android.server.util.ServerCommunicationException;
import org.walkersguide.android.server.util.ServerUtility;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.PlanRouteSettings;

import timber.log.Timber;
import org.walkersguide.android.data.basic.segment.Segment;
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.basic.segment.RouteSegment;


public class RouteManager {

    /**
     * singleton
     */

    private static RouteManager managerInstance;

    public static RouteManager getInstance() {
        if (managerInstance == null){
            managerInstance = getInstanceSynchronized();
        }
        return managerInstance;
    }

    private static synchronized RouteManager getInstanceSynchronized() {
        if (managerInstance == null){
            managerInstance = new RouteManager();
        }
        return managerInstance;
    }

    private RouteManager() {
    }


    /**
     * calculate route
     */

    public interface RouteCalculationListener {
        public void routeCalculationSuccessful(Route route);
        public void routeCalculationFailed(int returnCode);
    }

    private RouteCalculationTask serverTask = null;

    public void startRouteCalculation(RouteCalculationListener listener) {
        if (routeCalculationInProgress()) {
            cancelRouteCalculation();
        }
        this.serverTask = new RouteCalculationTask(listener);
        this.serverTask.execute();
    }

    public boolean routeCalculationInProgress() {
        if (this.serverTask != null
                && this.serverTask.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void updateRouteCalculationListener(RouteCalculationListener listener) {
        if (routeCalculationInProgress()) {
            this.serverTask.updateListener(listener);
        }
    }

    public void cancelRouteCalculation() {
        if (routeCalculationInProgress()) {
            this.serverTask.cancel();
        }
    }


    private static class RouteCalculationTask extends AsyncTask<Void, Void, Route> {

        private RouteCalculationListener listener;
        private int returnCode;
        private HttpsURLConnection connection;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;

        public RouteCalculationTask(RouteCalculationListener listener) {
            this.listener = listener;
            this.returnCode = Constants.RC.OK;
            this.connection = null;
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected Route doInBackground(Void... params) {
            Context context = GlobalInstance.getContext();

            // server instance
            ServerInstance serverInstance = null;
            try {
                serverInstance = ServerUtility.getServerInstance(
                        context, SettingsManager.getInstance().getServerURL());
            } catch (ServerCommunicationException e) {
                this.returnCode = e.getReturnCode();
            } finally {
                if (returnCode != Constants.RC.OK) {
                    return null;
                }
            }

            // points from route settings
            PlanRouteSettings planRouteSettings = SettingsManager.getInstance().getPlanRouteSettings();
            Point startPoint = planRouteSettings.getStartPoint();
            Point destinationPoint = planRouteSettings.getDestinationPoint();
            if (startPoint == null || destinationPoint == null) {
                this.returnCode = Constants.RC.START_OR_DESTINATION_MISSING;
                return null;
            }
            Point viaPoint1 = planRouteSettings.getViaPoint1();
            Point viaPoint2 = planRouteSettings.getViaPoint2();
            Point viaPoint3 = planRouteSettings.getViaPoint3();

            // create server param list
            JSONObject jsonServerParams = null;
            try {
                jsonServerParams = ServerUtility.createServerParamList();

                // start, via and destination points
                JSONArray jsonSourcePoints = new JSONArray();
                jsonSourcePoints.put(startPoint.toJson());
                JSONArray jsonViaPointList = createViaPointJsonArray(viaPoint1, viaPoint2, viaPoint3);
                for (int i=0; i<jsonViaPointList.length(); i++) {
                    jsonSourcePoints.put(jsonViaPointList.getJSONObject(i));
                }
                jsonSourcePoints.put(destinationPoint.toJson());
                jsonServerParams.put("source_points", jsonSourcePoints);
                Timber.d("jsonServerParams: %1$s", jsonServerParams.toString());

                // other params
                // excluded ways
                JSONArray jsonExcludedWays = new JSONArray();
                DatabaseProfileRequest databaseProfileRequest = new DatabaseProfileRequest(
                        DatabaseSegmentProfile.EXCLUDED_FROM_ROUTING, null, SortMethod.ACCESSED_DESC);
                for (ObjectWithId objectWithId : AccessDatabase.getInstance().getObjectWithIdListFor(databaseProfileRequest)) {
                    if (objectWithId instanceof Segment) {
                        Segment segmentToExclude = (Segment) objectWithId;
                        jsonExcludedWays.put(segmentToExclude.getId());
                    }
                }
                if (jsonExcludedWays.length() > 0) {
                    jsonServerParams.put("blocked_ways", jsonExcludedWays);
                }
                // allowed way classes
                JSONObject jsonWayClassIdAndWeights = new JSONObject();
                for (WayClass wayClass : planRouteSettings.getWayClassList()) {
                    jsonWayClassIdAndWeights.put(wayClass.getId(), wayClass.getWeight());
                }
                jsonServerParams.put("allowed_way_classes", jsonWayClassIdAndWeights);
            } catch (JSONException e) {
                jsonServerParams = new JSONObject();
            }

            // start request
            String description = null;
            JSONArray jsonRouteObjectList = null;
            try {
                connection = ServerUtility.getHttpsURLConnectionObject(
                        String.format(
                            "%1$s/%2$s", serverInstance.getServerURL(), Constants.SERVER_COMMAND.GET_ROUTE),
                        jsonServerParams);
                cancelConnectionHandler.postDelayed(cancelConnection, 100);
                connection.connect();
                returnCode = connection.getResponseCode();
                cancelConnectionHandler.removeCallbacks(cancelConnection);
                if (isCancelled()) {
                    this.returnCode = Constants.RC.CANCELLED;
                } else if (returnCode == Constants.RC.OK) {
                    JSONObject jsonServerResponse = ServerUtility.processServerResponseAsJSONObject(connection);
                    // extract description
                    description = jsonServerResponse.getString("description");
                    // convert flat route object list
                    jsonRouteObjectList = new JSONArray();
                    JSONArray jsonFlatRouteObjectList = jsonServerResponse.getJSONArray("route");
                    for (int i=0; i<jsonFlatRouteObjectList.length(); i+=2) {
                        JSONObject jsonRouteObject = new JSONObject();
                        if (i > 0) {
                            jsonRouteObject.put("segment", jsonFlatRouteObjectList.getJSONObject(i-1));
                        }
                        jsonRouteObject.put("point", jsonFlatRouteObjectList.getJSONObject(i));
                        jsonRouteObjectList.put(jsonRouteObject);
                    }
                }
            } catch (IOException e) {
                this.returnCode = Constants.RC.CONNECTION_FAILED;
            } catch (JSONException e) {
                this.returnCode = Constants.RC.BAD_RESPONSE;
            } catch (ServerCommunicationException e) {
                this.returnCode = e.getReturnCode();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (this.returnCode != Constants.RC.OK) {
                    return null;
                }
            }

            Route route = null;
            try {
                route = new Route(
                        Route.convertRouteFromWebserverApiV4ToV5(
                            startPoint.toJson(),
                            destinationPoint.toJson(),
                            createViaPointJsonArray(viaPoint1, viaPoint2, viaPoint3),
                            description,
                            jsonRouteObjectList));
            } catch (JSONException e) {
                Timber.e("route parsing error: %1$s", e.getMessage());
                this.returnCode = Constants.RC.BAD_RESPONSE;
            }
            return route;
        }

        @Override protected void onPostExecute(Route route) {
            if (this.listener != null) {
                if (this.returnCode == Constants.RC.OK) {
                    this.listener.routeCalculationSuccessful(route);
                } else {
                    this.listener.routeCalculationFailed(this.returnCode);
                }
            }
        }

        @Override protected void onCancelled(Route route) {
            if (this.listener != null) {
                this.listener.routeCalculationFailed(Constants.RC.CANCELLED);
            }
        }

        public void cancel() {
            this.cancel(true);
        }

        public void updateListener(RouteCalculationListener newListener) {
            this.listener = newListener;
        }

        private JSONArray createViaPointJsonArray(Point viaPoint1, Point viaPoint2, Point viaPoint3) throws JSONException {
            JSONArray jsonViaPointList = new JSONArray();
            if (viaPoint1 != null) {
                jsonViaPointList.put(viaPoint1.toJson());
            }
            if (viaPoint2 != null) {
                jsonViaPointList.put(viaPoint2.toJson());
            }
            if (viaPoint3 != null) {
                jsonViaPointList.put(viaPoint3.toJson());
            }
            return jsonViaPointList;
        }


        private class CancelConnection implements Runnable {
            public void run() {
                if (isCancelled()) {
                    if (connection != null) {
                        try {
                            connection.disconnect();
                        } catch (Exception e) {}
                    }
                    // send cancel request
                    try {
                        ServerUtility.cancelRunningRequest();
                    } catch (ServerCommunicationException e) {}
                    return;
                }
                cancelConnectionHandler.postDelayed(this, 100);
            }
        }
    }


    /**
     * street course request
     */

    public interface StreetCourseRequestListener {
        public void streetCourseRequestSuccessful(Route route);
        public void streetCourseRequestFailed(int returnCode);
    }

    private StreetCourseTask streetCourseTask = null;

    public void startStreetCourseRequest(StreetCourseRequestListener listener, StreetCourseRequest request) {
        if (streetCourseRequestInProgress()) {
            if (listener == null) {
                return;
            } else if (streetCourseTask.getStreetCourseRequest().equals(request)) {
                streetCourseTask.addListener(listener);
                return;
            } else {
                cancelStreetCourseRequest();
            }
        }
        streetCourseTask = new StreetCourseTask(listener, request);
        streetCourseTask.execute();
    }

    public void invalidateStreetCourseRequest(StreetCourseRequestListener listener) {
        if (streetCourseRequestInProgress()) {
            streetCourseTask.removeListener(listener);
        }
    }

    public boolean streetCourseRequestInProgress() {
        if (streetCourseTask != null
                && streetCourseTask.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void cancelStreetCourseRequest() {
        if (streetCourseRequestInProgress()) {
            streetCourseTask.cancel();
        }
    }


    private static class StreetCourseTask extends AsyncTask<Void, Void, Route> {

        private ArrayList<StreetCourseRequestListener> listenerList;
        private StreetCourseRequest request;
        private int returnCode;
        private HttpsURLConnection connection;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;

        public StreetCourseTask(StreetCourseRequestListener listener, StreetCourseRequest request) {
            this.listenerList = new ArrayList<StreetCourseRequestListener>();
            if (listener != null) {
                this.listenerList.add(listener);
            }
            this.request = request;
            this.returnCode = Constants.RC.OK;
            this.connection = null;
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected Route doInBackground(Void... params) {
            Context context = GlobalInstance.getContext();

            // server instance
            ServerInstance serverInstance = null;
            try {
                serverInstance = ServerUtility.getServerInstance(
                        context, SettingsManager.getInstance().getServerURL());
            } catch (ServerCommunicationException e) {
                this.returnCode = e.getReturnCode();
            } finally {
                if (returnCode != Constants.RC.OK) {
                    return null;
                }
            }

            // create server param list
            JSONObject jsonServerParams = null;
            try {
                jsonServerParams = ServerUtility.createServerParamList();
                jsonServerParams.put("node_id", this.request.getNodeId());
                jsonServerParams.put("way_id", request.getWayId());
                jsonServerParams.put("next_node_id", request.getNextNodeId());
            } catch (JSONException e) {
                jsonServerParams = new JSONObject();
            }

            // start request
            ArrayList<Point> pointList = new ArrayList<Point>();
            try {
                connection = ServerUtility.getHttpsURLConnectionObject(
                        String.format(
                            "%1$s/%2$s", serverInstance.getServerURL(), Constants.SERVER_COMMAND.GET_NEXT_INTERSECTIONS_FOR_WAY),
                        jsonServerParams);
                cancelConnectionHandler.postDelayed(cancelConnection, 100);
                connection.connect();
                returnCode = connection.getResponseCode();
                cancelConnectionHandler.removeCallbacks(cancelConnection);
                if (isCancelled()) {
                    this.returnCode = Constants.RC.CANCELLED;
                } else if (returnCode == Constants.RC.OK) {
                    JSONObject jsonServerResponse = ServerUtility.processServerResponseAsJSONObject(connection);
                    // get poi list
                    JSONArray jsonPointList = jsonServerResponse.getJSONArray("next_intersections");
                    for (int i=0; i<jsonPointList.length(); i++) {
                        pointList.add(
                                Point.create(jsonPointList.getJSONObject(i)));
                    }
                    if (pointList.size() <= 1) {
                        this.returnCode = Constants.RC.BAD_RESPONSE;
                    }
                }
            } catch (IOException e) {
                this.returnCode = Constants.RC.CONNECTION_FAILED;
            } catch (JSONException e) {
                this.returnCode = Constants.RC.BAD_RESPONSE;
            } catch (ServerCommunicationException e) {
                this.returnCode = e.getReturnCode();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (this.returnCode != Constants.RC.OK) {
                    return null;
                }
            }

            // create route
            Route route = null;
            try {
                Route.Builder routeBuilder = new Route.Builder(
                        pointList.get(0), pointList.get(pointList.size()-1));
                for (int i=0; i<pointList.size(); i++) {
                    Point current = pointList.get(i);
                    if (i == 0) {
                        routeBuilder.addFirstRouteObject(current);
                        continue;
                    }

                    Point previous = pointList.get(i-1);
                    RouteSegment betweenPreviousAndCurrent = new RouteSegment.Builder(
                            previous.distanceTo(current), previous.bearingTo(current))
                        .build();
                    if (i == pointList.size() - 1) {
                        routeBuilder.addLastRouteObject(betweenPreviousAndCurrent, current);
                        continue;
                    }

                    Point next = pointList.get(i+1);
                    RouteSegment betweenCurrentAndNext = new RouteSegment.Builder(
                            current.distanceTo(next), current.bearingTo(next))
                        .build();
                    Integer turn = betweenPreviousAndCurrent.getBearing() - betweenCurrentAndNext.getBearing();
                    if (turn < 0) {
                        turn += 360;
                    }
                    routeBuilder.addRouteObject(betweenPreviousAndCurrent, current, turn);
                }

                route = routeBuilder.build();
            } catch (JSONException e) {
                this.returnCode = Constants.RC.BAD_RESPONSE;
            }

            return route;
        }

        @Override protected void onPostExecute(Route route) {
            for (StreetCourseRequestListener listener : this.listenerList) {
                if (this.returnCode == Constants.RC.OK) {
                    listener.streetCourseRequestSuccessful(route);
                } else {
                    listener.streetCourseRequestFailed(returnCode);
                }
            }
        }

        @Override protected void onCancelled(Route route) {
            for (StreetCourseRequestListener listener : this.listenerList) {
                listener.streetCourseRequestFailed(Constants.RC.CANCELLED);
            }
        }

        public StreetCourseRequest getStreetCourseRequest() {
            return this.request;
        }

        public void addListener(StreetCourseRequestListener newListener) {
            if (newListener != null
                    && ! this.listenerList.contains(newListener)) {
                this.listenerList.add(newListener);
            }
        }

        public void removeListener(StreetCourseRequestListener newListener) {
            if (newListener != null
                    && this.listenerList.contains(newListener)) {
                this.listenerList.remove(newListener);
            }
        }

        public void cancel() {
            this.cancel(true);
        }

        private class CancelConnection implements Runnable {
            public void run() {
                if (isCancelled()) {
                    if (connection != null) {
                        try {
                            connection.disconnect();
                        } catch (Exception e) {}
                    }
                    // send cancel request
                    try {
                        ServerUtility.cancelRunningRequest();
                    } catch (ServerCommunicationException e) {}
                    return;
                }
                cancelConnectionHandler.postDelayed(this, 100);
            }
        }
    }

}
