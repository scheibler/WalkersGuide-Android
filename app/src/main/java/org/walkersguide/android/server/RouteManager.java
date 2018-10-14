package org.walkersguide.android.server;

import android.content.Context;

import android.os.AsyncTask;
import android.os.Handler;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Locale;

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
import org.walkersguide.android.helper.DownloadUtility;
import org.walkersguide.android.listener.RouteListener;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.RouteSettings;
import org.walkersguide.android.util.SettingsManager.ServerSettings;
import org.walkersguide.android.util.TTSWrapper;


public class RouteManager {

    private Context context;
    private static RouteManager routeManagerInstance;
    private AccessDatabase accessDatabaseInstance;
    private SettingsManager settingsManagerInstance;
    private TTSWrapper ttsWrapperInstance;
    private CalculateRoute calculateRoute;
    private RequestRoute requestRoute;

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
        this.calculateRoute = null;
        this.requestRoute = null;
    }

    public void calculateRoute(RouteListener routeListener) {
        if (routeCalculationInProgress()) {
            cancelRouteCalculation();
        }
        this.calculateRoute = new CalculateRoute(routeListener);
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

        private RouteListener routeListener;
        private int returnCode;
        private String additionalMessage;
        private HttpsURLConnection connection;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;

        public CalculateRoute(RouteListener routeListener) {
            this.routeListener = routeListener;
            this.returnCode = Constants.RC.OK;
            this.additionalMessage = "";
            this.connection = null;
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected Integer doInBackground(Void... params) {
            RouteSettings routeSettings = SettingsManager.getInstance(context).getRouteSettings();
            ServerSettings serverSettings = SettingsManager.getInstance(context).getServerSettings();
            // start and destination
            PointWrapper startPoint = routeSettings.getStartPoint();
            if (startPoint.equals(PositionManager.getDummyLocation(context))) {
                this.returnCode = Constants.RC.NO_ROUTE_START_POINT;
                return -1;
            }
            PointWrapper destinationPoint = routeSettings.getDestinationPoint();
            if (destinationPoint.equals(PositionManager.getDummyLocation(context))) {
                this.returnCode = Constants.RC.NO_ROUTE_DESTINATION_POINT;
                return -1;
            }

            // internet connection and server instance
            // check for internet connection
            if (! DownloadUtility.isInternetAvailable(context)) {
                this.returnCode = Constants.RC.NO_INTERNET_CONNECTION;
                return -1;
            }
            // get server instance
            ServerInstance serverInstance = null;
            try {
                serverInstance = ServerStatusManager.getInstance(context)
                    .getServerInstanceForURL(serverSettings.getServerURL());
            } catch (ServerCommunicationException e) {
                this.returnCode = e.getReturnCode();
                return -1;
            }

            // check selected map
            if (serverSettings.getSelectedMap() == null) {
                this.returnCode = Constants.RC.NO_MAP_SELECTED;
                return null;
            }

            String description = null;
            JSONArray jsonRouteObjectList = null;
            try {
                // start and destination
                JSONArray jsonSourcePoints = new JSONArray();
                jsonSourcePoints.put(startPoint.toJson());
                for (PointWrapper viaPoint : routeSettings.getViaPointList()) {
                    if (! viaPoint.equals(PositionManager.getDummyLocation(context))) {
                        jsonSourcePoints.put(viaPoint.toJson());
                    }
                }
                jsonSourcePoints.put(destinationPoint.toJson());
                // excluded ways
                JSONArray jsonExcludedWays = new JSONArray();
                for (SegmentWrapper segmentWrapper : accessDatabaseInstance.getExcludedWaysList()) {
                    if (segmentWrapper.getSegment() instanceof Footway) {
                        jsonExcludedWays.put(
                                ((Footway) segmentWrapper.getSegment()).getWayId());
                    }
                }
                // allowed way classes
                JSONArray jsonAllowedWayClassList = new JSONArray();
                for (WayClass wayClass : routeSettings.getWayClassList()) {
                    jsonAllowedWayClassList.put(wayClass.getId());
                }
                // create parameter list
                JSONObject requestJson = new JSONObject();
                requestJson.put("source_points", jsonSourcePoints);
                requestJson.put("blocked_ways", jsonExcludedWays);
                requestJson.put("allowed_way_classes", jsonAllowedWayClassList);
                requestJson.put("indirection_factor", routeSettings.getIndirectionFactor());
                requestJson.put("language", Locale.getDefault().getLanguage());
                requestJson.put("logging_allowed", serverSettings.getLogQueriesOnServer());
                requestJson.put("map_id", serverSettings.getSelectedMap().getId());
                requestJson.put("session_id", ((GlobalInstance) context.getApplicationContext()).getSessionId());
                System.out.println("xxx route request: " + requestJson.toString());
                // create connection
                connection = DownloadUtility.getHttpsURLConnectionObject(
                        context,
                        DownloadUtility.generateServerCommand(
                            serverInstance.getServerURL(), Constants.SERVER_COMMAND.GET_ROUTE),
                        requestJson);
                cancelConnectionHandler.postDelayed(cancelConnection, 100);
                connection.connect();
                int responseCode = connection.getResponseCode();
                cancelConnectionHandler.removeCallbacks(cancelConnection);
                if (isCancelled()) {
                    this.returnCode = Constants.RC.CANCELLED;
                } else if (responseCode != Constants.RC.OK) {
                    this.returnCode = Constants.RC.SERVER_CONNECTION_ERROR;
                } else {
                    JSONObject jsonServerResponse = DownloadUtility.processServerResponseAsJSONObject(connection);
                    if (jsonServerResponse.has("error")
                            && ! jsonServerResponse.getString("error").equals("")) {
                        this.additionalMessage = jsonServerResponse.getString("error");
                        this.returnCode = Constants.RC.SERVER_RESPONSE_ERROR_WITH_EXTRA_DATA;
                    } else {
                        // get route list
                        description= jsonServerResponse.getString("description");
                        jsonRouteObjectList = jsonServerResponse.getJSONArray("route");
                    }
                }
            } catch (IOException e) {
                this.returnCode = Constants.RC.SERVER_CONNECTION_ERROR;
            } catch (JSONException e) {
                this.returnCode = Constants.RC.SERVER_RESPONSE_ERROR;
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
                for (int i=0; i<jsonRouteObjectList.length(); i+=2) {
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
                // add to database
                routeId = AccessDatabase.getInstance(context).addRoute(
                            startPoint, destinationPoint,
                            SettingsManager.getInstance(context).getRouteSettings().getViaPointList(),
                            description, routeObjectList);
            } catch (JSONException e) {
                this.returnCode = Constants.RC.ROUTE_PARSING_ERROR;
            }
            return routeId;
        }

        @Override protected void onPostExecute(Integer routeId) {
            System.out.println("xxx add route: rc: " + returnCode + "   routeId" + routeId);
            if (this.routeListener != null) {
                this.routeListener.routeCalculationFinished(
                        returnCode,
                        DownloadUtility.getErrorMessageForReturnCode(context, this.returnCode, ""),
                        routeId);
            }
        }

        @Override protected void onCancelled(Integer routeId) {
            System.out.println("xxx add route: cancelled");
            if (this.routeListener != null) {
                this.routeListener.routeCalculationFinished(
                        Constants.RC.CANCELLED,
                        DownloadUtility.getErrorMessageForReturnCode(context, Constants.RC.CANCELLED, ""),
                        routeId);
            }
        }

        public void cancel() {
            this.cancel(true);
        }

        private class CancelConnection implements Runnable {
            public void run() {
                if (isCancelled()) {
                    System.out.println("xxx add Route: cancel connection in runnable");
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

    public void requestRoute(RouteListener routeListener, int routeId) {
        if (routeRequestInProgress()) {
            if (routeListener == null) {
                return;
            } else if (this.requestRoute.getRouteId() == routeId) {
                this.requestRoute.addListener(routeListener);
                return;
            } else {
                cancelRouteRequest();
            }
        }
        this.requestRoute = new RequestRoute(routeListener, routeId);
        this.requestRoute.execute();
    }

    public void invalidateRouteRequest(RouteListener routeListener) {
        if (routeRequestInProgress()) {
            this.requestRoute.removeListener(routeListener);
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

    public void skipToPreviousRouteObject(RouteListener routeListener, int routeId) {
        int currentObjectIndex = accessDatabaseInstance.getCurrentObjectIndexOfRoute(routeId);
        if (currentObjectIndex != -1) {
            boolean success = accessDatabaseInstance.updateCurrentObjectIndexOfRoute(
                    routeId, currentObjectIndex-1);
            if (! success) {
                ttsWrapperInstance.speak(
                        context.getResources().getString(R.string.messageAlreadyFirstObject), true, true);
            } else if (routeListener != null) {
                if (routeRequestInProgress()) {
                    cancelRouteRequest();
                }
                requestRoute(routeListener, routeId);
            }
        }
    }

    public void skipToNextRouteObject(RouteListener routeListener, int routeId) {
        int currentObjectIndex = accessDatabaseInstance.getCurrentObjectIndexOfRoute(routeId);
        if (currentObjectIndex != -1) {
            boolean success = accessDatabaseInstance.updateCurrentObjectIndexOfRoute(
                    routeId, currentObjectIndex+1);
            if (! success) {
                ttsWrapperInstance.speak(
                        context.getResources().getString(R.string.messageAlreadyLastObject), true, true);
            } else if (routeListener != null) {
                if (routeRequestInProgress()) {
                    cancelRouteRequest();
                }
                requestRoute(routeListener, routeId);
            }
        }
    }

    public void jumpToRouteObjectAtIndex(RouteListener routeListener, int routeId, int objectIndex) {
        accessDatabaseInstance.updateCurrentObjectIndexOfRoute(routeId, objectIndex);
        if (routeListener != null) {
            if (routeRequestInProgress()) {
                cancelRouteRequest();
            }
            requestRoute(routeListener, routeId);
        }
    }


    private class RequestRoute extends AsyncTask<Void, Void, Route> {

        private ArrayList<RouteListener> routeListenerList;
        private int routeIdToRequest, returnCode;

        public RequestRoute(RouteListener routeListener, int routeId) {
            this.routeListenerList = new ArrayList<RouteListener>();
            if (routeListener != null) {
                this.routeListenerList.add(routeListener);
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
            for (RouteListener routeListener : this.routeListenerList) {
                routeListener.routeRequestFinished(
                        returnCode,
                        DownloadUtility.getErrorMessageForReturnCode(context, this.returnCode, ""),
                        route);
            }
        }

        @Override protected void onCancelled(Route route) {
            for (RouteListener routeListener : this.routeListenerList) {
                routeListener.routeRequestFinished(
                        Constants.RC.CANCELLED,
                        DownloadUtility.getErrorMessageForReturnCode(context, Constants.RC.CANCELLED, ""),
                        route);
            }
        }

        public int getRouteId() {
            return this.routeIdToRequest;
        }

        public void addListener(RouteListener newRouteListener) {
            if (newRouteListener != null
                    && ! this.routeListenerList.contains(newRouteListener)) {
                this.routeListenerList.add(newRouteListener);
            }
        }

        public void removeListener(RouteListener newRouteListener) {
            if (newRouteListener != null
                    && this.routeListenerList.contains(newRouteListener)) {
                this.routeListenerList.remove(newRouteListener);
            }
        }

        public void cancel() {
            this.cancel(true);
        }
    }

}
