package org.walkersguide.android.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.data.profile.NextIntersectionsProfile;
import org.walkersguide.android.data.profile.POIProfile;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.DownloadUtility;
import org.walkersguide.android.listener.NextIntersectionsListener;
import org.walkersguide.android.listener.POIProfileListener;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import org.walkersguide.android.data.profile.SearchFavoritesProfile;
import org.walkersguide.android.exception.ServerCommunicationException;


public class POIManager {

    // actions
    public static final int ACTION_MORE_RESULTS = 411;
    public static final int ACTION_UPDATE = 412;

    private Context context;
    private static POIManager poiManagerInstance;
    private SettingsManager settingsManagerInstance;
    private RequestPOI requestPOI;

    public static POIManager getInstance(Context context) {
        if(poiManagerInstance == null){
            poiManagerInstance = new POIManager(context.getApplicationContext());
        }
        return poiManagerInstance;
    }

    private POIManager(Context context) {
        this.context = context;
        this.settingsManagerInstance = SettingsManager.getInstance(context);
        this.requestPOI = null;
        // listen for new locations
        LocalBroadcastManager.getInstance(context).registerReceiver(
                newLocationReceiver,
                new IntentFilter(Constants.ACTION_NEW_LOCATION));
    }


    /**
     * get poi profiles
     */

    public void requestPOIProfile(
            POIProfileListener profileListener, int profileId, int requestAction) {
        if (requestInProgress()) {
            if (profileListener == null) {
                return;
            } else if (this.requestPOI.getPOIProfileId() == profileId
                    && this.requestPOI.getRequestAction() == requestAction) {
                this.requestPOI.addListener(profileListener);
                return;
            } else {
                cancelRequest();
            }
        }
        this.requestPOI = new RequestPOI(profileListener, profileId, requestAction);
        this.requestPOI.execute();
    }

    public void invalidateRequest(POIProfileListener profileListener) {
        if (requestInProgress()) {
            this.requestPOI.removeListener(profileListener);
        }
    }

    public boolean requestInProgress() {
        if (this.requestPOI != null
                && this.requestPOI.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void cancelRequest() {
        if (requestInProgress()) {
            this.requestPOI.cancel();
        }
    }


    private class RequestPOI extends AsyncTask<Void, Void, POIProfile> {

        private ArrayList<POIProfileListener> poiProfileListenerList;
        private int poiProfileIdToRequest, requestAction, returnCode;
        private boolean resetListPosition;
        private String additionalMessage;
        private HttpsURLConnection connection;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;
        private long t1;

        public RequestPOI(POIProfileListener poiProfileListener, int poiProfileId, int requestAction) {
            this.poiProfileListenerList = new ArrayList<POIProfileListener>();
            if (poiProfileListener != null) {
                this.poiProfileListenerList.add(poiProfileListener);
            }
            this.poiProfileIdToRequest = poiProfileId;
            this.requestAction = requestAction;
            this.resetListPosition = false;
            this.returnCode = Constants.RC.OK;
            this.additionalMessage = "";
            this.connection = null;
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected POIProfile doInBackground(Void... params) {
            ServerSettings serverSettings = SettingsManager.getInstance(context).getServerSettings();

            // load poi profile
            AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(context);
            POIProfile poiProfile = accessDatabaseInstance.getPOIProfile(this.poiProfileIdToRequest);
            if (poiProfile == null) {
                this.returnCode = Constants.RC.NO_POI_PROFILE_SELECTED;
                return null;
            } else if (poiProfile.getPOICategoryList().isEmpty()) {
                this.returnCode = Constants.RC.NO_POI_CATEGORY_SELECTED;
                return null;
            }

            // get current location
            PositionManager positionManagerInstance = PositionManager.getInstance(context);
            PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
            if (currentLocation.equals(PositionManager.getDummyLocation(context))) {
                this.returnCode = Constants.RC.NO_LOCATION_FOUND;
                return null;
            }

            // get current direction
            DirectionManager directionManagerInstance = DirectionManager.getInstance(context);
            int currentDirection = directionManagerInstance.getCurrentDirection();
            if (currentDirection == Constants.DUMMY.DIRECTION) {
                this.returnCode = Constants.RC.NO_DIRECTION_FOUND;
                return null;
            }

            ArrayList<PointProfileObject> pointList = poiProfile.getPointProfileObjectList();
            boolean distanceToLastPOIProfileCenterPointTooLarge = 
                currentLocation.distanceTo(poiProfile.getCenter()) > (poiProfile.getLookupRadius() / 2);

            if (pointList == null
                    || poiProfile.getCenter() == null
                    || distanceToLastPOIProfileCenterPointTooLarge
                    || this.requestAction == ACTION_MORE_RESULTS) {

                // internet connection and server instance
                // check for internet connection
                if (! DownloadUtility.isInternetAvailable(context)) {
                    this.returnCode = Constants.RC.NO_INTERNET_CONNECTION;
                    return null;
                }
                // get server instance
                ServerInstance serverInstance = null;
                try {
                    serverInstance = ServerStatusManager.getInstance(context)
                        .getServerInstanceForURL(serverSettings.getServerURL());
                } catch (ServerCommunicationException e) {
                    this.returnCode = e.getReturnCode();
                    return null;
                }

                // check selected map
                if (serverSettings.getSelectedMap() == null) {
                    this.returnCode = Constants.RC.NO_MAP_SELECTED;
                    return null;
                }

                // get radius and number of results
                int radius;
                int numberOfResults;
                switch (this.requestAction) {
                    case ACTION_UPDATE:
                        radius = poiProfile.getInitialRadius();
                        numberOfResults = poiProfile.getInitialNumberOfResults();
                        break;
                    case ACTION_MORE_RESULTS:
                        if (distanceToLastPOIProfileCenterPointTooLarge) {
                            // new location therefore start at initial values
                            radius = poiProfile.getInitialRadius();
                            numberOfResults = poiProfile.getInitialNumberOfResults();
                        } else {
                            // more or less the same position again therefore increase radius,
                            // number or results or both
                            radius = poiProfile.getRadius();
                            numberOfResults = poiProfile.getNumberOfResults();
                            if (radius == poiProfile.getLookupRadius()) {
                                radius += poiProfile.getInitialRadius();
                                if ((numberOfResults - poiProfile.getLookupNumberOfResults()) < 20) {
                                    numberOfResults += poiProfile.getInitialNumberOfResults();
                                }
                            } else {
                                numberOfResults += poiProfile.getInitialNumberOfResults();
                                if ((radius - poiProfile.getLookupRadius()) < 200) {
                                    radius += poiProfile.getInitialRadius();
                                }
                            }
                        }
                        break;
                    default:
                        this.returnCode = Constants.RC.UNSUPPORTED_POI_REQUEST_ACTION;
                        return null;
                }

                // poi category tags
                JSONArray jsonPOICategoryTagList = new JSONArray();
                for (POICategory poiCategory : poiProfile.getPOICategoryList()) {
                    jsonPOICategoryTagList.put(poiCategory.getId());
                } 

                // request poi from server
                JSONArray jsonPointList = null;
                try {
                    // create parameter list
                    JSONObject requestJson = new JSONObject();
                    requestJson.put("lat", currentLocation.getPoint().getLatitude());
                    requestJson.put("lon", currentLocation.getPoint().getLongitude());
                    requestJson.put("radius", radius);
                    requestJson.put("number_of_results", numberOfResults);
                    requestJson.put("tags", jsonPOICategoryTagList);
                    requestJson.put("search", poiProfile.getSearchTerm());
                    requestJson.put("language", Locale.getDefault().getLanguage());
                    requestJson.put("logging_allowed", serverSettings.getLogQueriesOnServer());
                    requestJson.put("map_id", serverSettings.getSelectedMap().getId());
                    requestJson.put("session_id", ((GlobalInstance) context.getApplicationContext()).getSessionId());
                    System.out.println("xxx poi request: " + requestJson.toString());
                    // create connection
                    connection = DownloadUtility.getHttpsURLConnectionObject(
                            context,
                            DownloadUtility.generateServerCommand(
                                serverInstance.getServerURL(), Constants.SERVER_COMMAND.GET_POI),
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
                            // get poi list
                            jsonPointList = jsonServerResponse.getJSONArray("poi");
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
                        return null;
                    }
                }

                // parse points
                // set new radius and number of results
                poiProfile.setRadiusAndNumberOfResults(radius, numberOfResults);
                // load point list
                pointList = new ArrayList<PointProfileObject>();
                for (int i=0; i<jsonPointList.length(); i++) {
                    try {
                        pointList.add(
                                new PointProfileObject(
                                    context, poiProfile, jsonPointList.getJSONObject(i))
                                );
                    } catch (JSONException e) {}
                }
                poiProfile.setPointProfileObjectList(pointList);
            }

            if (this.requestAction == ACTION_UPDATE
                    && currentLocation.distanceTo(poiProfile.getCenter()) > PositionManager.THRESHOLD3.DISTANCE) {
                this.resetListPosition = true;
            }

            // update location and direction
            poiProfile.setCenterAndDirection(currentLocation, currentDirection);
            return poiProfile;
        }

        @Override protected void onPostExecute(POIProfile poiProfile) {
            System.out.println("xxx poiManager: " + this.returnCode + "     resetListPosition: " + resetListPosition + "    id: "  + this.poiProfileIdToRequest + "     listener: " + this.poiProfileListenerList);
            for (POIProfileListener poiProfileListener : this.poiProfileListenerList) {
                poiProfileListener.poiProfileRequestFinished(
                        this.returnCode,
                        DownloadUtility.getErrorMessageForReturnCode(context, this.returnCode, this.additionalMessage),
                        poiProfile,
                        this.resetListPosition);
            }
        }

        @Override protected void onCancelled(POIProfile poiProfile) {
            System.out.println("xxx cancel - poiManager: " + this.returnCode + "     resetListPosition: " + resetListPosition + "    id: "  + this.poiProfileIdToRequest);
            for (POIProfileListener poiProfileListener : this.poiProfileListenerList) {
                poiProfileListener.poiProfileRequestFinished(
                        Constants.RC.CANCELLED,
                        DownloadUtility.getErrorMessageForReturnCode(context, Constants.RC.CANCELLED, ""),
                        poiProfile,
                        false);
            }
        }

        public int getPOIProfileId() {
            return this.poiProfileIdToRequest;
        }

        public int getRequestAction() {
            return this.requestAction;
        }

        public void addListener(POIProfileListener newPOIProfileListener) {
            if (newPOIProfileListener != null
                    && ! this.poiProfileListenerList.contains(newPOIProfileListener)) {
                this.poiProfileListenerList.add(newPOIProfileListener);
            }
        }

        public void removeListener(POIProfileListener newPOIProfileListener) {
            if (newPOIProfileListener != null
                    && this.poiProfileListenerList.contains(newPOIProfileListener)) {
                this.poiProfileListenerList.remove(newPOIProfileListener);
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
     * next intersections
     */
    private RequestNextIntersections requestNextIntersections;
    private NextIntersectionsProfile lastNextIntersectionsProfile;

    public void requestNextIntersections(
            NextIntersectionsListener profileListener, long nodeId, long wayId, long nextNodeId) {
        if (nextIntersectionsRequestInProgress()) {
            if (profileListener == null) {
                return;
            } else if (this.requestNextIntersections.getNodeId() == nodeId
                    && this.requestNextIntersections.getWayId() == wayId
                    && this.requestNextIntersections.getNextNodeId() == nextNodeId) {
                this.requestNextIntersections.addListener(profileListener);
                return;
            } else {
                cancelRequest();
            }
        }
        this.requestNextIntersections = new RequestNextIntersections(profileListener, nodeId, wayId, nextNodeId);
        this.requestNextIntersections.execute();
    }

    public void invalidateNextIntersectionsRequest(NextIntersectionsListener profileListener) {
        if (nextIntersectionsRequestInProgress()) {
            this.requestNextIntersections.removeListener(profileListener);
        }
    }

    public boolean nextIntersectionsRequestInProgress() {
        if (this.requestNextIntersections != null
                && this.requestNextIntersections.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void cancelNextIntersectionsRequest() {
        if (nextIntersectionsRequestInProgress()) {
            this.requestNextIntersections.cancel();
        }
        this.requestNextIntersections = null;
    }

    public void destroyNextIntersectionsInstance() {
        this.cancelNextIntersectionsRequest();
        this.lastNextIntersectionsProfile = null;
    }


    private class RequestNextIntersections extends AsyncTask<Void, Void, NextIntersectionsProfile> {

        private ArrayList<NextIntersectionsListener> nextIntersectionsListenerList;
        private long nodeId, wayId, nextNodeId;
        private int returnCode;
        private boolean resetListPosition;
        private String additionalMessage;
        private HttpsURLConnection connection;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;
        private long t1;

        public RequestNextIntersections(NextIntersectionsListener nextIntersectionsListener,
                long nodeId, long wayId, long nextNodeId) {
            this.nextIntersectionsListenerList = new ArrayList<NextIntersectionsListener>();
            if (nextIntersectionsListener != null) {
                this.nextIntersectionsListenerList.add(nextIntersectionsListener);
            }
            this.nodeId = nodeId;
            this.wayId = wayId;
            this.nextNodeId = nextNodeId;
            this.resetListPosition = false;
            this.returnCode = Constants.RC.OK;
            this.additionalMessage = "";
            this.connection = null;
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected NextIntersectionsProfile doInBackground(Void... params) {
            ServerSettings serverSettings = SettingsManager.getInstance(context).getServerSettings();

            NextIntersectionsProfile nextIntersectionsProfile = null;
            try {
                nextIntersectionsProfile = new NextIntersectionsProfile(
                        context, this.nodeId, this.wayId, this.nextNodeId);
            } catch (JSONException e) {
                nextIntersectionsProfile = null;
            } finally {
                if (nextIntersectionsProfile == null) {
                    this.returnCode = Constants.RC.NO_POI_PROFILE_SELECTED;
                    return null;
                }
            }

            // get current location
            PositionManager positionManagerInstance = PositionManager.getInstance(context);
            PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
            if (currentLocation.equals(PositionManager.getDummyLocation(context))) {
                this.returnCode = Constants.RC.NO_LOCATION_FOUND;
                return null;
            }

            // get current direction
            DirectionManager directionManagerInstance = DirectionManager.getInstance(context);
            int currentDirection = directionManagerInstance.getCurrentDirection();
            if (currentDirection == Constants.DUMMY.DIRECTION) {
                this.returnCode = Constants.RC.NO_DIRECTION_FOUND;
                return null;
            }

            ArrayList<PointProfileObject> nextIntersectionsPointList = null;
            if (lastNextIntersectionsProfile != null
                    && lastNextIntersectionsProfile.getNodeId() == this.nodeId
                    && lastNextIntersectionsProfile.getWayId() == this.wayId
                    && lastNextIntersectionsProfile.getNextNodeId() == this.nextNodeId) {
                System.out.println("xxx next intersections from cache");
                // load data from cache
                nextIntersectionsPointList = lastNextIntersectionsProfile.getPointProfileObjectList();

            } else {
                // request next intersections from server
                this.resetListPosition = true;

                // check selected map
                if (serverSettings.getSelectedMap() == null) {
                    this.returnCode = Constants.RC.NO_MAP_SELECTED;
                    return null;
                }

                // internet connection and server instance
                // check for internet connection
                if (! DownloadUtility.isInternetAvailable(context)) {
                    this.returnCode = Constants.RC.NO_INTERNET_CONNECTION;
                    return null;
                }
                // get server instance
                ServerInstance serverInstance = null;
                try {
                    serverInstance = ServerStatusManager.getInstance(context)
                        .getServerInstanceForURL(serverSettings.getServerURL());
                } catch (ServerCommunicationException e) {
                    this.returnCode = e.getReturnCode();
                    return null;
                }

                // start request
                JSONArray jsonPointList = null;
                try {
                    // create parameter list
                    JSONObject requestJson = new JSONObject();
                    requestJson.put("node_id", this.nodeId);
                    requestJson.put("way_id", this.wayId);
                    requestJson.put("next_node_id", this.nextNodeId);
                    requestJson.put("logging_allowed", serverSettings.getLogQueriesOnServer());
                    requestJson.put("language", Locale.getDefault().getLanguage());
                    requestJson.put("map_id", serverSettings.getSelectedMap().getId());
                    requestJson.put("session_id", ((GlobalInstance) context.getApplicationContext()).getSessionId());
                    System.out.println("xxx next intersections request: " + requestJson.toString());
                    // create connection
                    connection = DownloadUtility.getHttpsURLConnectionObject(
                            context,
                            DownloadUtility.generateServerCommand(
                                serverInstance.getServerURL(), Constants.SERVER_COMMAND.GET_NEXT_INTERSECTIONS_FOR_WAY),
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
                            // get poi list
                            jsonPointList = jsonServerResponse.getJSONArray("next_intersections");
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
                        return null;
                    }
                }

                // load point list
                nextIntersectionsPointList = new ArrayList<PointProfileObject>();
                for (int i=0; i<jsonPointList.length(); i++) {
                    try {
                        nextIntersectionsPointList.add(
                                new PointProfileObject(
                                    context, nextIntersectionsProfile, jsonPointList.getJSONObject(i))
                                );
                    } catch (JSONException e) {}
                }
                nextIntersectionsProfile.setPointProfileObjectList(nextIntersectionsPointList);
            }

            // update location and direction
            nextIntersectionsProfile.setCenterAndDirection(currentLocation, currentDirection);
            lastNextIntersectionsProfile = nextIntersectionsProfile;
            return nextIntersectionsProfile;
        }

        @Override protected void onPostExecute(NextIntersectionsProfile nextIntersectionsProfile) {
            System.out.println("xxx next intersections: " + this.returnCode + "     resetListPosition: " + resetListPosition);
            for (NextIntersectionsListener nextIntersectionsListener : this.nextIntersectionsListenerList) {
                nextIntersectionsListener.nextIntersectionsRequestFinished(
                        this.returnCode,
                        DownloadUtility.getErrorMessageForReturnCode(context, this.returnCode, this.additionalMessage),
                        nextIntersectionsProfile,
                        this.resetListPosition);
            }
        }

        @Override protected void onCancelled(NextIntersectionsProfile nextIntersectionsProfile) {
            System.out.println("xxx next intersections: cancelled     resetListPosition: " + resetListPosition);
            for (NextIntersectionsListener nextIntersectionsListener : this.nextIntersectionsListenerList) {
                nextIntersectionsListener.nextIntersectionsRequestFinished(
                        Constants.RC.CANCELLED,
                        DownloadUtility.getErrorMessageForReturnCode(context, Constants.RC.CANCELLED, ""),
                        nextIntersectionsProfile,
                        false);
            }
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

        public void addListener(NextIntersectionsListener newNextIntersectionsListener) {
            if (newNextIntersectionsListener != null
                    && ! this.nextIntersectionsListenerList.contains(newNextIntersectionsListener)) {
                this.nextIntersectionsListenerList.add(newNextIntersectionsListener);
            }
        }

        public void removeListener(NextIntersectionsListener newNextIntersectionsListener) {
            if (newNextIntersectionsListener != null
                    && this.nextIntersectionsListenerList.contains(newNextIntersectionsListener)) {
                this.nextIntersectionsListenerList.remove(newNextIntersectionsListener);
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


    private BroadcastReceiver newLocationReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                    && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD3.ID
                    && ! intent.getBooleanExtra(Constants.ACTION_NEW_LOCATION_ATTR.BOOL_AT_HIGH_SPEED, false)
                    && ! requestInProgress()) {
                System.out.println("xxx request poi profile with threshold3: " + settingsManagerInstance.getPOIFragmentSettings().getSelectedPOIProfileId());
                requestPOIProfile(
                        null, settingsManagerInstance.getPOIFragmentSettings().getSelectedPOIProfileId(), ACTION_UPDATE);
            }
        }
    };

}
