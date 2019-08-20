package org.walkersguide.android.server;

import android.content.Context;

import android.os.AsyncTask;
import android.os.Handler;

import android.text.TextUtils;

import java.io.IOException;

import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.data.profile.HistoryPointProfile;
import org.walkersguide.android.data.profile.NextIntersectionsProfile;
import org.walkersguide.android.data.profile.POIProfile;
import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.data.sensor.threshold.DistanceThreshold;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.exception.ServerCommunicationException;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;


public class POIManager {

    // actions
    public static final int ACTION_MORE_RESULTS = 411;
    public static final int ACTION_UPDATE = 412;

    private Context context;
    private static POIManager poiManagerInstance;
    private SettingsManager settingsManagerInstance;

    public static POIManager getInstance(Context context) {
        if(poiManagerInstance == null){
            poiManagerInstance = new POIManager(context.getApplicationContext());
        }
        return poiManagerInstance;
    }

    private POIManager(Context context) {
        this.context = context;
        this.settingsManagerInstance = SettingsManager.getInstance(context);
    }


    /**
     * get poi profiles
     */
    public interface POIProfileListener {
	    public void poiProfileRequestFinished(Context context, int returnCode, POIProfile poiProfile, boolean resetListPosition);
    }

    private RequestPOI requestPOI = null;

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
        private HttpsURLConnection connection;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;

        public RequestPOI(POIProfileListener poiProfileListener, int poiProfileId, int requestAction) {
            this.poiProfileListenerList = new ArrayList<POIProfileListener>();
            if (poiProfileListener != null) {
                this.poiProfileListenerList.add(poiProfileListener);
            }
            this.poiProfileIdToRequest = poiProfileId;
            this.requestAction = requestAction;
            this.resetListPosition = false;
            this.returnCode = Constants.RC.OK;
            this.connection = null;
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected POIProfile doInBackground(Void... params) {
            AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(context);
            ServerSettings serverSettings = SettingsManager.getInstance(context).getServerSettings();

            // does any poi profile exist?
            if (accessDatabaseInstance.getPOIProfileMap().keySet().isEmpty()) {
                this.returnCode = Constants.RC.NO_POI_PROFILE_CREATED;
                return null;
            }
            // does the selected poi profile exist?
            if (! accessDatabaseInstance.getPOIProfileMap().containsKey(this.poiProfileIdToRequest)) {
                this.returnCode = Constants.RC.NO_POI_PROFILE_SELECTED;
                return null;
            }

            // load poi profile
            POIProfile poiProfile = accessDatabaseInstance.getPOIProfile(this.poiProfileIdToRequest);
            if (poiProfile == null) {
                this.returnCode = Constants.RC.POI_PROFILE_PARSING_ERROR;
                return null;
            }

            // get current location
            PositionManager positionManagerInstance = PositionManager.getInstance(context);
            PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
            if (currentLocation == null) {
                this.returnCode = Constants.RC.NO_LOCATION_FOUND;
                return null;
            }

            // get current direction
            DirectionManager directionManagerInstance = DirectionManager.getInstance(context);
            Direction currentDirection = directionManagerInstance.getCurrentDirection();
            if (currentDirection == null) {
                this.returnCode = Constants.RC.NO_DIRECTION_FOUND;
                return null;
            }

            // get poi profile data
            ArrayList<PointProfileObject> pointList = poiProfile.getPointProfileObjectList();
            boolean distanceToLastPOIProfileCenterPointTooLarge = 
                   poiProfile.getCenter() == null
                || currentLocation.distanceTo(poiProfile.getCenter()) > (poiProfile.getLookupRadius() / 2);
            if (this.requestAction == ACTION_UPDATE
                    && currentLocation.distanceTo(poiProfile.getCenter()) > DistanceThreshold.ONE_HUNDRED_METERS.getDistanceThresholdInMeters()) {
                this.resetListPosition = true;
            }

            // new values for radius and number of results
            int radius;
            int numberOfResults;
            switch (this.requestAction) {
                case ACTION_UPDATE:
                case ACTION_MORE_RESULTS:
                    if (distanceToLastPOIProfileCenterPointTooLarge) {
                        // new location
                        // (re)start with initial values
                        radius = poiProfile.getInitialRadius();
                        numberOfResults = poiProfile.getInitialNumberOfResults();
                    } else {
                        // more or less the same position again
                        radius = poiProfile.getRadius();
                        numberOfResults = poiProfile.getNumberOfResults();
                        // increase radius, number or results or both
                        if (this.requestAction == ACTION_MORE_RESULTS) {
                            //          num_low num_hgh
                            //  rad_low r++,--- ---,n++
                            //  rad_hgh r++,--- r++,n++
                            if (((float) poiProfile.getLookupNumberOfResults() / poiProfile.getNumberOfResults()) > 0.66) {
                                // num_hgh: increase number of results
                                numberOfResults += poiProfile.getInitialNumberOfResults();
                                if (((float) poiProfile.getLookupRadius() / poiProfile.getRadius()) > 0.33) {
                                    // rad_hgh: increase radius
                                    radius += poiProfile.getInitialRadius();
                                }
                            } else {
                                // num_low: increase radius
                                radius += poiProfile.getInitialRadius();
                            }
                        }
                    }
                    break;
                default:
                    this.returnCode = Constants.RC.UNSUPPORTED_POI_REQUEST_ACTION;
                    return null;
            }

            if (! poiProfile.getPOICategoryList().isEmpty()
                    && (
                           pointList == null
                        || distanceToLastPOIProfileCenterPointTooLarge
                        || this.requestAction == ACTION_MORE_RESULTS)
                    ) {

                // server instance
                ServerInstance serverInstance = null;
                try {
                    serverInstance = ServerUtility.getServerInstance(
                            context, serverSettings.getServerURL());
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
                    jsonServerParams = ServerUtility.createServerParamList(context);
                    jsonServerParams.put("lat", currentLocation.getPoint().getLatitude());
                    jsonServerParams.put("lon", currentLocation.getPoint().getLongitude());
                    jsonServerParams.put("radius", radius);
                    jsonServerParams.put("number_of_results", numberOfResults);
                    jsonServerParams.put("search", poiProfile.getSearchTerm());
                    // tags
                    JSONArray jsonPOICategoryTagList = new JSONArray();
                    for (POICategory poiCategory : poiProfile.getPOICategoryList()) {
                        jsonPOICategoryTagList.put(poiCategory.getId());
                    } 
                    jsonServerParams.put("tags", jsonPOICategoryTagList);
                } catch (JSONException e) {
                    jsonServerParams = new JSONObject();
                }

                // start request
                JSONArray jsonPOIList = null;
                try {
                    connection = ServerUtility.getHttpsURLConnectionObject(
                            context,
                            String.format(
                                "%1$s/%2$s", serverInstance.getServerURL(), Constants.SERVER_COMMAND.GET_POI),
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
                        jsonPOIList = jsonServerResponse.getJSONArray("poi");
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

                // parse points
                pointList = new ArrayList<PointProfileObject>();
                for (int i=0; i<jsonPOIList.length(); i++) {
                    try {
                        pointList.add(
                                new PointProfileObject(
                                    context, poiProfile, jsonPOIList.getJSONObject(i))
                                );
                    } catch (JSONException e) {}
                }
            }
            // update poi profile
            poiProfile.setRadiusNumberOfResultsCenterDirectionAndPointListAndUpdateInDatabase(
                    radius, numberOfResults, currentLocation, currentDirection, pointList);

            // include points from favorite ids
            for (Integer favoriteId : poiProfile.getFavoriteIdList()) {
                JSONArray jsonFavoritesList = accessDatabaseInstance.getJSONFavoritePointListOfProfile(favoriteId);
                for (int i=0; i<jsonFavoritesList.length(); i++) {
                    PointProfileObject object = null;
                    try {
                        object = new PointProfileObject(
                                context, poiProfile, jsonFavoritesList.getJSONObject(i));
                    } catch (JSONException e) {
                        object = null;
                    } finally {
                        if (object != null
                                && ! pointList.contains(object)
                                && object.distanceTo(poiProfile.getCenter()) <= poiProfile.getLookupRadius()) {
                            if (! TextUtils.isEmpty(poiProfile.getSearchTerm())) {
                                // search
                                boolean matches = true;
                                for (String word : poiProfile.getSearchTerm().split("\\s")) {
                                    if (! object.toString().toLowerCase().contains(word.toLowerCase())) {
                                        matches = false;
                                        break;
                                    }
                                }
                                if (matches) {
                                    pointList.add(object);
                                }
                            } else {
                                pointList.add(object);
                            }
                        }
                    }
                }
            }

            // update location and direction and return
            poiProfile.setCenterDirectionAndPointList(
                    currentLocation, currentDirection, pointList);
            return poiProfile;
        }

        @Override protected void onPostExecute(POIProfile poiProfile) {
            for (POIProfileListener poiProfileListener : this.poiProfileListenerList) {
                poiProfileListener.poiProfileRequestFinished(
                        context, returnCode, poiProfile, resetListPosition);
            }
        }

        @Override protected void onCancelled(POIProfile poiProfile) {
            for (POIProfileListener poiProfileListener : this.poiProfileListenerList) {
                poiProfileListener.poiProfileRequestFinished(
                        context, Constants.RC.CANCELLED, null, false);
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
     * historyPoint  profiles
     */
    public interface HistoryPointProfileListener {
	    public void historyPointProfileRequestFinished(Context context, int returnCode, HistoryPointProfile historyPointProfile, boolean resetListPosition);
    }

    private RequestHistoryPointProfile requestHistoryPointProfile = null;

    public void requestHistoryPointProfile(HistoryPointProfileListener profileListener, int profileId) {
        if (historyPointProfileRequestInProgress()) {
            if (profileListener == null) {
                return;
            } else if (this.requestHistoryPointProfile.getHistoryPointProfileId() == profileId) {
                this.requestHistoryPointProfile.addListener(profileListener);
                return;
            } else {
                cancelHistoryPointProfileRequest();
            }
        }
        this.requestHistoryPointProfile = new RequestHistoryPointProfile(profileListener, profileId);
        this.requestHistoryPointProfile.execute();
    }

    public void invalidateHistoryPointProfileRequest(HistoryPointProfileListener profileListener) {
        if (historyPointProfileRequestInProgress()) {
            this.requestHistoryPointProfile.removeListener(profileListener);
        }
    }

    public boolean historyPointProfileRequestInProgress() {
        if (this.requestHistoryPointProfile != null
                && this.requestHistoryPointProfile.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void cancelHistoryPointProfileRequest() {
        if (historyPointProfileRequestInProgress()) {
            this.requestHistoryPointProfile.cancel();
        }
    }


    private class RequestHistoryPointProfile extends AsyncTask<Void, Void, HistoryPointProfile> {

        private ArrayList<HistoryPointProfileListener> historyPointProfileListenerList;
        private int historyPointProfileIdToRequest, returnCode;
        private boolean resetListPosition;

        public RequestHistoryPointProfile(HistoryPointProfileListener profileListener, int historyPointProfileIdToRequest) {
            this.historyPointProfileListenerList = new ArrayList<HistoryPointProfileListener>();
            if (profileListener != null) {
                this.historyPointProfileListenerList.add(profileListener);
            }
            this.historyPointProfileIdToRequest = historyPointProfileIdToRequest;
            this.returnCode = Constants.RC.OK;
            this.resetListPosition = false;
        }

        @Override protected HistoryPointProfile doInBackground(Void... params) {
            // load historyPoint profile
            AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(context);
            HistoryPointProfile historyPointProfile = null;
            try {
                historyPointProfile = new HistoryPointProfile(
                        context, 
                        this.historyPointProfileIdToRequest,
                        accessDatabaseInstance.getJSONFavoritePointListOfProfile(this.historyPointProfileIdToRequest));
            } catch (JSONException e) {
                historyPointProfile = null;
            } finally {
                if (historyPointProfile == null) {
                    this.returnCode = Constants.RC.POI_PROFILE_PARSING_ERROR;
                    return null;
                }
            }

            // get current location
            PositionManager positionManagerInstance = PositionManager.getInstance(context);
            PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
            if (currentLocation == null) {
                this.returnCode = Constants.RC.NO_LOCATION_FOUND;
                return null;
            }

            // get current direction
            DirectionManager directionManagerInstance = DirectionManager.getInstance(context);
            Direction currentDirection = directionManagerInstance.getCurrentDirection();
            if (currentDirection == null) {
                this.returnCode = Constants.RC.NO_DIRECTION_FOUND;
                return null;
            }

            // update center and direction of profile and return
            historyPointProfile.setCenterDirectionAndPointList(
                    currentLocation, currentDirection, historyPointProfile.getPointProfileObjectList());
            return historyPointProfile;
        }

        @Override protected void onPostExecute(HistoryPointProfile historyPointProfile) {
            for (HistoryPointProfileListener historyPointProfileListener : this.historyPointProfileListenerList) {
                historyPointProfileListener.historyPointProfileRequestFinished(
                        context, returnCode, historyPointProfile, resetListPosition);
            }
        }

        @Override protected void onCancelled(HistoryPointProfile historyPointProfile) {
            for (HistoryPointProfileListener historyPointProfileListener : this.historyPointProfileListenerList) {
                historyPointProfileListener.historyPointProfileRequestFinished(
                        context, Constants.RC.CANCELLED, null, false);
            }
        }

        public int getHistoryPointProfileId() {
            return this.historyPointProfileIdToRequest;
        }

        public void addListener(HistoryPointProfileListener newHistoryPointProfileListener) {
            if (newHistoryPointProfileListener != null
                    && ! this.historyPointProfileListenerList.contains(newHistoryPointProfileListener)) {
                this.historyPointProfileListenerList.add(newHistoryPointProfileListener);
            }
        }

        public void removeListener(HistoryPointProfileListener newHistoryPointProfileListener) {
            if (newHistoryPointProfileListener != null
                    && this.historyPointProfileListenerList.contains(newHistoryPointProfileListener)) {
                this.historyPointProfileListenerList.remove(newHistoryPointProfileListener);
            }
        }

        public void cancel() {
            this.cancel(true);
        }
    }


    /**
     * next intersections
     */
    public interface NextIntersectionsListener {
	    public void nextIntersectionsRequestFinished(Context context, int returnCode, NextIntersectionsProfile nextIntersectionsProfile, boolean resetListPosition);
    }

    private RequestNextIntersections requestNextIntersections = null;
    private NextIntersectionsProfile cachedNextIntersectionsProfile = null;

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


    private class RequestNextIntersections extends AsyncTask<Void, Void, NextIntersectionsProfile> {

        private ArrayList<NextIntersectionsListener> nextIntersectionsListenerList;
        private long nodeId, wayId, nextNodeId;
        private int returnCode;
        private boolean resetListPosition;
        private HttpsURLConnection connection;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;

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
                    this.returnCode = Constants.RC.POI_PROFILE_PARSING_ERROR;
                    return null;
                }
            }

            // get current location
            PositionManager positionManagerInstance = PositionManager.getInstance(context);
            PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
            if (currentLocation == null) {
                this.returnCode = Constants.RC.NO_LOCATION_FOUND;
                return null;
            }

            // get current direction
            DirectionManager directionManagerInstance = DirectionManager.getInstance(context);
            Direction currentDirection = directionManagerInstance.getCurrentDirection();
            if (currentDirection == null) {
                this.returnCode = Constants.RC.NO_DIRECTION_FOUND;
                return null;
            }

            ArrayList<PointProfileObject> nextIntersectionsPointList = null;
            if (cachedNextIntersectionsProfile != null
                    && cachedNextIntersectionsProfile.getNodeId() == this.nodeId
                    && cachedNextIntersectionsProfile.getWayId() == this.wayId
                    && cachedNextIntersectionsProfile.getNextNodeId() == this.nextNodeId) {
                // load data from cache
                nextIntersectionsPointList = cachedNextIntersectionsProfile.getPointProfileObjectList();

            } else {
                // request next intersections from server
                this.resetListPosition = true;

                // server instance
                ServerInstance serverInstance = null;
                try {
                    serverInstance = ServerUtility.getServerInstance(
                            context, serverSettings.getServerURL());
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
                    jsonServerParams = ServerUtility.createServerParamList(context);
                    jsonServerParams.put("node_id", this.nodeId);
                    jsonServerParams.put("way_id", this.wayId);
                    jsonServerParams.put("next_node_id", this.nextNodeId);
                } catch (JSONException e) {
                    jsonServerParams = new JSONObject();
                }

                // start request
                JSONArray jsonPointList = null;
                try {
                    connection = ServerUtility.getHttpsURLConnectionObject(
                            context,
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
                        jsonPointList = jsonServerResponse.getJSONArray("next_intersections");
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
            }

            // update location and direction
            nextIntersectionsProfile.setCenterDirectionAndPointList(currentLocation, currentDirection, nextIntersectionsPointList);
            return nextIntersectionsProfile;
        }

        @Override protected void onPostExecute(NextIntersectionsProfile nextIntersectionsProfile) {
            if (this.returnCode == Constants.RC.OK) {
                cachedNextIntersectionsProfile = nextIntersectionsProfile;
            }
            for (NextIntersectionsListener nextIntersectionsListener : this.nextIntersectionsListenerList) {
                nextIntersectionsListener.nextIntersectionsRequestFinished(
                        context, returnCode, nextIntersectionsProfile, resetListPosition);
            }
        }

        @Override protected void onCancelled(NextIntersectionsProfile nextIntersectionsProfile) {
            for (NextIntersectionsListener nextIntersectionsListener : this.nextIntersectionsListenerList) {
                nextIntersectionsListener.nextIntersectionsRequestFinished(
                        context, Constants.RC.CANCELLED, null, false);
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

}
