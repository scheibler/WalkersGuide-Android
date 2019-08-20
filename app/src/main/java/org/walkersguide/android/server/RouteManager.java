package org.walkersguide.android.server;

import android.content.Context;

import android.os.AsyncTask;
import android.os.Handler;

import java.io.IOException;

import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.segment.Footway;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.data.route.RouteObject;
import org.walkersguide.android.data.route.WayClass;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.exception.ServerCommunicationException;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.RouteSettings;
import org.walkersguide.android.util.SettingsManager.ServerSettings;
import org.walkersguide.android.util.TTSWrapper;

import timber.log.Timber;


public class RouteManager {

    private Context context;
    private static RouteManager routeManagerInstance;
    private AccessDatabase accessDatabaseInstance;
    private SettingsManager settingsManagerInstance;
    private TTSWrapper ttsWrapperInstance;

    public static RouteManager getInstance(Context context) {
        if(routeManagerInstance == null){
            routeManagerInstance = new RouteManager(context.getApplicationContext());
        }
        return routeManagerInstance;
    }

    private RouteManager(Context context) {
        this.context = context;
        this.accessDatabaseInstance = AccessDatabase.getInstance(context);
        this.settingsManagerInstance = SettingsManager.getInstance(context);
        ttsWrapperInstance = TTSWrapper.getInstance(context);
    }


    /**
     * route calculation request
     */
    public interface RouteCalculationListener {
        public void routeCalculationFinished(Context context, int returnCode, int routeId);
    }

    private CalculateRoute calculateRoute = null;

    public void calculateRoute(RouteCalculationListener routeCalculationListener) {
        if (routeCalculationInProgress()) {
            cancelRouteCalculation();
        }
        this.calculateRoute = new CalculateRoute(routeCalculationListener);
        this.calculateRoute.execute();
    }

    public boolean routeCalculationInProgress() {
        if (this.calculateRoute != null
                && this.calculateRoute.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void cancelRouteCalculation() {
        if (routeCalculationInProgress()) {
            this.calculateRoute.cancel();
        }
    }


    private class CalculateRoute extends AsyncTask<Void, Void, Integer> {

        private RouteCalculationListener routeCalculationListener;
        private int returnCode;
        private HttpsURLConnection connection;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;

        public CalculateRoute(RouteCalculationListener routeCalculationListener) {
            this.routeCalculationListener = routeCalculationListener;
            this.returnCode = Constants.RC.OK;
            this.connection = null;
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected Integer doInBackground(Void... params) {
            RouteSettings routeSettings = SettingsManager.getInstance(context).getRouteSettings();
            ServerSettings serverSettings = SettingsManager.getInstance(context).getServerSettings();

            // server instance
            ServerInstance serverInstance = null;
            try {
                serverInstance = ServerUtility.getServerInstance(
                        context, serverSettings.getServerURL());
            } catch (ServerCommunicationException e) {
                this.returnCode = e.getReturnCode();
            } finally {
                if (returnCode != Constants.RC.OK) {
                    return -1;
                }
            }

            // create server param list
            JSONObject jsonServerParams = null;
            try {
                jsonServerParams = ServerUtility.createServerParamList(context);

                // start, via and destination points
                JSONArray jsonSourcePoints = new JSONArray();
                PointWrapper startPoint = routeSettings.getStartPoint();
                if (startPoint != null) {
                    jsonSourcePoints.put(startPoint.toJson());
                }
                for (PointWrapper viaPoint : routeSettings.getViaPointList()) {
                    if (viaPoint != null) {
                        jsonSourcePoints.put(viaPoint.toJson());
                    }
                }
                PointWrapper destinationPoint = routeSettings.getDestinationPoint();
                if (destinationPoint != null) {
                    jsonSourcePoints.put(destinationPoint.toJson());
                }
                jsonServerParams.put("source_points", jsonSourcePoints);

                // other params
                // excluded ways
                JSONArray jsonExcludedWays = new JSONArray();
                for (SegmentWrapper segmentWrapper : accessDatabaseInstance.getExcludedWaysList()) {
                    if (segmentWrapper.getSegment() instanceof Footway) {
                        jsonExcludedWays.put(
                                ((Footway) segmentWrapper.getSegment()).getWayId());
                    }
                }
                jsonServerParams.put("blocked_ways", jsonExcludedWays);
                // allowed way classes
                JSONObject jsonWayClassIdAndWeights = new JSONObject();
                for (WayClass wayClass : routeSettings.getWayClassList()) {
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
                        context,
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
                    // get route list
                    description= jsonServerResponse.getString("description");
                    jsonRouteObjectList = jsonServerResponse.getJSONArray("route");
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
                    return -1;
                }
            }

            int routeId = -1;
            ArrayList<RouteObject> routeObjectList = new ArrayList<RouteObject>();
            try {
                // parse route
                Timber.d("route length: %1$d", jsonRouteObjectList.length());
                for (int i=0; i<jsonRouteObjectList.length(); i+=2) {
                    Timber.d("r_object: %1$s", jsonRouteObjectList.getJSONObject(i).toString());
                    if (i == 0) {
                        // start object
                        routeObjectList.add(
                                new RouteObject(
                                    context,
                                    0,
                                    new JSONObject(Constants.DUMMY.FOOTWAY),
                                    jsonRouteObjectList.getJSONObject(i))
                                );
                    } else {
                        // all other route objects
                        routeObjectList.add(
                                new RouteObject(
                                    context,
                                    i/2,
                                    jsonRouteObjectList.getJSONObject(i-1),
                                    jsonRouteObjectList.getJSONObject(i))
                                );
                    }
                }
                if (routeObjectList.isEmpty()) {
                    throw new JSONException("empty route object list");
                }
                // add to database
                routeId = AccessDatabase.getInstance(context).addRoute(
                        routeSettings.getStartPoint(), routeSettings.getDestinationPoint(),
                        routeSettings.getViaPointList(), description, routeObjectList);
            } catch (JSONException e) {
                Timber.e("r_parsing: %1$s", e.getMessage());
                this.returnCode = Constants.RC.ROUTE_PARSING_ERROR;
            }
            return routeId;
        }

        @Override protected void onPostExecute(Integer routeId) {
            if (this.routeCalculationListener != null) {
                this.routeCalculationListener.routeCalculationFinished(
                        context, returnCode, routeId);
            }
        }

        @Override protected void onCancelled(Integer routeId) {
            if (this.routeCalculationListener != null) {
                this.routeCalculationListener.routeCalculationFinished(
                        context, Constants.RC.CANCELLED, -1);
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
                    ServerStatusManager.getInstance(context).cancelRunningRequestOnServer();
                    return;
                }
                cancelConnectionHandler.postDelayed(this, 100);
            }
        }
    }


    /**
     * request route from database
     */
    public interface RouteRequestListener {
        public void routeRequestFinished(Context context, int returnCode, Route route);
    }

    private RequestRoute requestRoute = null;

    public void requestRoute(RouteRequestListener routeRequestListener, int routeId) {
        if (routeRequestInProgress()) {
            if (routeRequestListener == null) {
                return;
            } else if (this.requestRoute.getRouteId() == routeId) {
                this.requestRoute.addListener(routeRequestListener);
                return;
            } else {
                cancelRouteRequest();
            }
        }
        this.requestRoute = new RequestRoute(routeRequestListener, routeId);
        this.requestRoute.execute();
    }

    public void invalidateRouteRequest(RouteRequestListener routeRequestListener) {
        if (routeRequestInProgress()) {
            this.requestRoute.removeListener(routeRequestListener);
        }
    }

    public boolean routeRequestInProgress() {
        if (this.requestRoute != null
                && this.requestRoute.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void cancelRouteRequest() {
        if (routeRequestInProgress()) {
            this.requestRoute.cancel();
        }
    }

    public void skipToPreviousRouteObject(RouteRequestListener routeRequestListener, int routeId) {
        int currentObjectIndex = accessDatabaseInstance.getCurrentObjectIndexOfRoute(routeId);
        if (currentObjectIndex != -1) {
            boolean success = accessDatabaseInstance.updateCurrentObjectIndexOfRoute(
                    routeId, currentObjectIndex-1);
            if (success && routeRequestListener != null) {
                if (routeRequestInProgress()) {
                    cancelRouteRequest();
                }
                requestRoute(routeRequestListener, routeId);
            }
        }
    }

    public void skipToNextRouteObject(RouteRequestListener routeRequestListener, int routeId) {
        int currentObjectIndex = accessDatabaseInstance.getCurrentObjectIndexOfRoute(routeId);
        if (currentObjectIndex != -1) {
            boolean success = accessDatabaseInstance.updateCurrentObjectIndexOfRoute(
                    routeId, currentObjectIndex+1);
            if (success && routeRequestListener != null) {
                if (routeRequestInProgress()) {
                    cancelRouteRequest();
                }
                requestRoute(routeRequestListener, routeId);
            }
        }
    }

    public void jumpToRouteObjectAtIndex(RouteRequestListener routeRequestListener, int routeId, int objectIndex) {
        accessDatabaseInstance.updateCurrentObjectIndexOfRoute(routeId, objectIndex);
        if (routeRequestListener != null) {
            if (routeRequestInProgress()) {
                cancelRouteRequest();
            }
            requestRoute(routeRequestListener, routeId);
        }
    }


    private class RequestRoute extends AsyncTask<Void, Void, Route> {

        private ArrayList<RouteRequestListener> routeRequestListenerList;
        private int routeIdToRequest, returnCode;

        public RequestRoute(RouteRequestListener routeRequestListener, int routeId) {
            this.routeRequestListenerList = new ArrayList<RouteRequestListener>();
            if (routeRequestListener != null) {
                this.routeRequestListenerList.add(routeRequestListener);
            }
            this.routeIdToRequest = routeId;
            this.returnCode = Constants.RC.OK;
        }

        @Override protected Route doInBackground(Void... params) {
            AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(context);
            if (accessDatabaseInstance.getRouteIdList(null).isEmpty()) {
                this.returnCode = Constants.RC.NO_ROUTE_CREATED;
                return null;
            }
            // check existence of poi profile
            if (! accessDatabaseInstance.getRouteIdList(null).contains(this.routeIdToRequest)) {
                this.returnCode = Constants.RC.NO_ROUTE_SELECTED;
                return null;
            }
            // load poi profile
            Route route = null;
            try {
                route = AccessDatabase.getInstance(context).getRoute(this.routeIdToRequest);
            } catch (JSONException e) {
                this.returnCode = Constants.RC.ROUTE_PARSING_ERROR;
            }
            return route;
        }

        @Override protected void onPostExecute(Route route) {
            for (RouteRequestListener routeRequestListener : this.routeRequestListenerList) {
                routeRequestListener.routeRequestFinished(
                        context, returnCode, route);
            }
        }

        @Override protected void onCancelled(Route route) {
            for (RouteRequestListener routeRequestListener : this.routeRequestListenerList) {
                routeRequestListener.routeRequestFinished(
                        context, Constants.RC.CANCELLED, null);
            }
        }

        public int getRouteId() {
            return this.routeIdToRequest;
        }

        public void addListener(RouteRequestListener newRouteRequestListener) {
            if (newRouteRequestListener != null
                    && ! this.routeRequestListenerList.contains(newRouteRequestListener)) {
                this.routeRequestListenerList.add(newRouteRequestListener);
            }
        }

        public void removeListener(RouteRequestListener newRouteRequestListener) {
            if (newRouteRequestListener != null
                    && this.routeRequestListenerList.contains(newRouteRequestListener)) {
                this.routeRequestListenerList.remove(newRouteRequestListener);
            }
        }

        public void cancel() {
            this.cancel(true);
        }
    }

}
