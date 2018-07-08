package org.walkersguide.android.server;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.data.route.RouteObject;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.DownloadUtility;
import org.walkersguide.android.listener.RouteListener;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.RouteSettings;
import org.walkersguide.android.util.SettingsManager.ServerSettings;
import org.walkersguide.android.util.TTSWrapper;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

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
            this.returnCode = Constants.ID.OK;
            this.additionalMessage = "";
            this.connection = null;
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected Integer doInBackground(Void... params) {
            // check for map port
            ServerSettings serverSettings = SettingsManager.getInstance(context).getServerSettings();
            if (serverSettings.getSelectedMap() == null) {
                this.returnCode = Constants.ID.NO_MAP_SELECTED;
                return -1;
            }

            RouteSettings routeSettings = SettingsManager.getInstance(context).getRouteSettings();
            // start and destination
            PointWrapper startPoint = routeSettings.getStartPoint();
            if (startPoint.equals(PositionManager.getDummyLocation(context))) {
                this.returnCode = 1020;
                return -1;
            }
            PointWrapper destinationPoint = routeSettings.getDestinationPoint();
            if (destinationPoint.equals(PositionManager.getDummyLocation(context))) {
                this.returnCode = 1021;
                return -1;
            }

            String description = null;
            ArrayList<RouteObject> routeObjectList = new ArrayList<RouteObject>();
            if (! DownloadUtility.isInternetAvailable(context)) {
                this.returnCode = 1003;
            } else {
                try {
                    // start and destination
                    JSONArray jsonSourcePoints = new JSONArray();
                    jsonSourcePoints.put(startPoint.toJson());
                    jsonSourcePoints.put(destinationPoint.toJson());
                    // create parameter list
                    JSONObject requestJson = new JSONObject();
                    requestJson.put("source_points", jsonSourcePoints);
                    requestJson.put("indirection_factor", routeSettings.getIndirectionFactor());
                    requestJson.put("language", Locale.getDefault().getLanguage());
                    requestJson.put("session_id", ((GlobalInstance) context.getApplicationContext()).getSessionId());
                    System.out.println("xxx route request: " + requestJson.toString());
                    // create connection
                    connection = DownloadUtility.getHttpsURLConnectionObject(
                            context,
                            String.format("%1$s/get_route", serverSettings.getSelectedMap().getURL()),
                            requestJson);
                    cancelConnectionHandler.postDelayed(cancelConnection, 100);
                    connection.connect();
                    int responseCode = connection.getResponseCode();
                    cancelConnectionHandler.removeCallbacks(cancelConnection);
                    if (isCancelled()) {
                        returnCode = 1000;          // cancelled
                    } else if (responseCode != Constants.ID.OK) {
                        returnCode = 1010;          // server connection error
                    } else {
                        JSONObject jsonServerResponse = DownloadUtility.processServerResponse(connection);
                        if (jsonServerResponse.has("error")
                                && ! jsonServerResponse.getString("error").equals("")) {
                            this.additionalMessage = jsonServerResponse.getString("error");
                            returnCode = 1012;          // error from server
                        } else {
                            // get poi list
                            description= jsonServerResponse.getString("description");
                            JSONArray jsonRouteObjectList = jsonServerResponse.getJSONArray("route");
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
                        }
                    }
                } catch (CertificateException | IOException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException e) {
                    returnCode = 1010;          // server connection error
                } catch (JSONException e) {
                    returnCode = 1011;          // server response error
                } finally {
                    if (isCancelled()) {
                        returnCode = 1000;          // cancelled
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }

            if (returnCode == Constants.ID.OK
                    && ! routeObjectList.isEmpty()) {
                AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(context);
                try {
                    return accessDatabaseInstance.addRoute(
                            startPoint, destinationPoint, description, routeObjectList);
                } catch (JSONException e) {}
            }
            return -1;
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
                        Constants.ID.CANCELLED,
                        DownloadUtility.getErrorMessageForReturnCode(context, Constants.ID.CANCELLED, ""),
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
            if (this.requestRoute.getRouteId() == routeId) {
                this.requestRoute.updateListener(routeListener);
                return;
            } else {
                cancelRouteRequest();
            }
        }
        this.requestRoute = new RequestRoute(routeListener, routeId);
        this.requestRoute.execute();
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
            boolean success = accessDatabaseInstance.setCurrentObjectIndexOfRoute(
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
            boolean success = accessDatabaseInstance.setCurrentObjectIndexOfRoute(
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
        accessDatabaseInstance.setCurrentObjectIndexOfRoute(routeId, objectIndex);
        if (routeListener != null) {
            if (routeRequestInProgress()) {
                cancelRouteRequest();
            }
            requestRoute(routeListener, routeId);
        }
    }


    private class RequestRoute extends AsyncTask<Void, Void, Route> {

        private RouteListener routeListener;
        private int routeIdToRequest, returnCode;

        public RequestRoute(RouteListener routeListener, int routeId) {
            this.routeListener = routeListener;
            this.routeIdToRequest = routeId;
            this.returnCode = Constants.ID.OK;
        }

        @Override protected Route doInBackground(Void... params) {
            Route route = null;
            try {
                route = AccessDatabase.getInstance(context).getRoute(this.routeIdToRequest);
            } catch (JSONException e) {
                this.returnCode = 1023;
            }
            return route;
        }

        @Override protected void onPostExecute(Route route) {
            System.out.println("xxx request route: " + this.returnCode);
            if (this.routeListener != null) {
                this.routeListener.routeRequestFinished(
                        returnCode,
                        DownloadUtility.getErrorMessageForReturnCode(context, this.returnCode, ""),
                        route);
            }
        }

        @Override protected void onCancelled(Route route) {
            if (this.routeListener != null) {
                this.routeListener.routeRequestFinished(
                        Constants.ID.CANCELLED,
                        DownloadUtility.getErrorMessageForReturnCode(context, Constants.ID.CANCELLED, ""),
                        route);
            }
        }

        public int getRouteId() {
            return this.routeIdToRequest;
        }

        public void updateListener(RouteListener newRouteListener) {
            if (newRouteListener != null) {
                this.routeListener = newRouteListener;
            }
        }

        public void cancel() {
            this.cancel(true);
        }
    }

}
