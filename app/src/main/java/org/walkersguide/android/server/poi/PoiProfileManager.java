package org.walkersguide.android.server.poi;

import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.server.poi.PoiCategory;
import org.walkersguide.android.server.poi.PoiProfileRequest;
import org.walkersguide.android.server.poi.PoiProfileResult;

import android.os.Handler;


import java.io.IOException;

import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.sensor.threshold.DistanceThreshold;
import org.walkersguide.android.server.util.ServerInstance;
import org.walkersguide.android.server.util.ServerCommunicationException;
import org.walkersguide.android.server.util.ServerUtility;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.data.basic.point.Point;
import java.util.LinkedHashMap;
import java.util.Collections;
import timber.log.Timber;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import android.os.Looper;
import java.util.concurrent.Executors;
import java.io.InputStream;


public class PoiProfileManager {

    public enum RequestAction {
        UPDATE, MORE_RESULTS
    }


    /**
     * singleton
     */

    private static PoiProfileManager managerInstance;

    public static PoiProfileManager getInstance() {
        if (managerInstance == null){
            managerInstance = getInstanceSynchronized();
        }
        return managerInstance;
    }

    private static synchronized PoiProfileManager getInstanceSynchronized() {
        if (managerInstance == null){
            managerInstance = new PoiProfileManager();
        }
        return managerInstance;
    }

    private PoiProfileManager() {
    }


    /**
     * get poi profiles
     */

    public interface PoiProfileRequestListener {
        public void poiProfileRequestSuccessful(PoiProfileResult result);
        public void poiProfileRequestFailed(int returnCode);
    }

    private PoiProfileTask serverTask = null;

    public void startPoiProfileRequest(
            PoiProfileRequestListener listener, PoiProfileRequest request, RequestAction action) {
        if (poiProfileRequestInProgress()) {
            if (serverTask.getPoiProfileRequest().equals(request)
                    && serverTask.getRequestAction() == action) {
                serverTask.addListener(listener);
            } else if (! serverTask.isCancelled()) {
                cancelPoiProfileRequest();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override public void run() {
                        while (poiProfileRequestInProgress()) {
                            try {
                                Thread.sleep(250);
                            } catch (InterruptedException e) {}
                        }
                        Timber.d("startPoiProfileRequest again");
                        startPoiProfileRequest(listener, request, action);
                    }
                }, 500);
            }
            return;
        }
        serverTask = new PoiProfileTask(request, action);
        serverTask.addListener(listener);
        serverTask.execute();
    }

    public void invalidatePoiProfileRequest(PoiProfileRequestListener listener) {
        if (poiProfileRequestInProgress()) {
            serverTask.removeListener(listener);
        }
    }

    public boolean poiProfileRequestInProgress() {
        if (serverTask != null
                && serverTask.isRunning()) {
            return true;
        }
        return false;
    }

    public void cancelPoiProfileRequest() {
        if (poiProfileRequestInProgress()) {
            serverTask.cancel();
        }
    }


    // cache
    private LinkedHashMap<PoiProfileRequest,PoiProfileResult> poiProfileResultByRequestMap = new LinkedHashMap<PoiProfileRequest,PoiProfileResult>();

    public PoiProfileResult getCachedPoiProfileResult(PoiProfileRequest request) {
        return this.poiProfileResultByRequestMap.get(request);
    }

    public void cachePoiProfileResult(PoiProfileRequest request, PoiProfileResult result) {
        this.poiProfileResultByRequestMap.put(request, result);
    }

    public void clearPoiProfileResultCache() {
        this.poiProfileResultByRequestMap.clear();
    }


    private static class PoiProfileTask {
        private ExecutorService executorService = Executors.newSingleThreadExecutor();
        private Handler handler = new Handler(Looper.getMainLooper());
        private Future executor = null;
        private boolean cancelled = false;

        private ArrayList<PoiProfileRequestListener> listenerList = new ArrayList<PoiProfileRequestListener>();
        private HttpsURLConnection connection = null;

        private PoiProfileRequest request = null;
        private RequestAction action = null;
        private boolean resetListPosition = false;

        public PoiProfileTask(PoiProfileRequest request, RequestAction action) {
            this.request = request;
            this.action = action;
        }

        public void execute() {
            executor = this.executorService.submit(() -> {
                try {
                    final PoiProfileResult result = calculate();

                    // add to cache
                    PoiProfileManager
                        .getInstance()
                        .cachePoiProfileResult(this.request, result);

                    handler.post(() -> {
                        for (PoiProfileRequestListener listener : this.listenerList) {
                            listener.poiProfileRequestSuccessful(result);
                        }
                    });

                } catch (ServerCommunicationException e) {
                    final ServerCommunicationException scException = e;
                    handler.post(() -> {
                        for (PoiProfileRequestListener listener : this.listenerList) {
                            listener.poiProfileRequestFailed(scException.getReturnCode());
                        }
                    });
                }
            });
        }

        private PoiProfileResult calculate() throws ServerCommunicationException {
            AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance();

            // does any poi profile exist?
            if (accessDatabaseInstance.getPoiProfileList().isEmpty()) {
                throw new ServerCommunicationException(Constants.RC.NO_POI_PROFILE_CREATED);
            }
            // does the selected poi profile exist?
            if (request.getProfile() == null) {
                throw new ServerCommunicationException(Constants.RC.NO_POI_PROFILE_SELECTED);
            }

            // get new center point (current location)
            PositionManager positionManagerInstance = PositionManager.getInstance();
            Point newCenter = positionManagerInstance.getCurrentLocation();
            if (newCenter == null) {
                throw new ServerCommunicationException(Constants.RC.NO_LOCATION_FOUND);
            }

            // initialize
            PoiProfileResult cachedResult = PoiProfileManager
                .getInstance()
                .getCachedPoiProfileResult(this.request);
            boolean distanceToLastPOIProfileCenterPointTooLarge = 
                cachedResult == null
                || cachedResult.getCenter().distanceTo(newCenter) > (cachedResult.getLookupRadius() / 2);
            int initialRadius;
            if (this.request.getProfile().getPoiCategoryList().isEmpty()) {
                initialRadius = PoiProfileResult.INITIAL_LOCAL_FAVORITES_RADIUS;
            } else if (this.request.hasSearchTerm()) {
                initialRadius = PoiProfileResult.INITIAL_SEARCH_RADIUS;
            } else {
                initialRadius = PoiProfileResult.INITIAL_RADIUS;
            }

            // new values for radius and number of results
            int newRadius, newNumberOfResults;
            if (distanceToLastPOIProfileCenterPointTooLarge) {
                // new location
                // (re)start with initial values
                newRadius = initialRadius;
                newNumberOfResults = PoiProfileResult.INITIAL_NUMBER_OF_RESULTS;
            } else {
                // more or less the same position again
                // note: "cachedResult" can't be null in this "else" condition
                //       see creation of distanceToLastPOIProfileCenterPointTooLarge for details
                newRadius = cachedResult.getRadius();
                newNumberOfResults = cachedResult.getNumberOfResults();
                // increase radius, numberOfResults or both
                if (this.action == RequestAction.MORE_RESULTS) {
                    //          num_low num_hgh
                    //  rad_low r++,--- ---,n++
                    //  rad_hgh r++,--- r++,n++
                    if (((float) cachedResult.getLookupNumberOfResults() / cachedResult.getNumberOfResults()) > 0.66) {
                        // num_hgh: increase number of results
                        newNumberOfResults += PoiProfileResult.INITIAL_NUMBER_OF_RESULTS;
                        if (((float) cachedResult.getLookupRadius() / cachedResult.getRadius()) > 0.33) {
                            // rad_hgh: increase radius
                            newRadius += initialRadius;
                        }
                    } else {
                        // num_low: increase radius
                        newRadius += initialRadius;
                    }
                }
            }

            ArrayList<Point> newOnlyPoiList = new ArrayList<Point>();
            if (cachedResult != null
                    && ! distanceToLastPOIProfileCenterPointTooLarge
                    && this.action == RequestAction.UPDATE) {
                // server poi from cache
                newOnlyPoiList = cachedResult.getOnlyPoiList();

            } else if (! this.request.getProfile().getPoiCategoryList().isEmpty()) {

                // create server param list
                JSONObject jsonServerParams = null;
                try {
                    jsonServerParams = ServerUtility.createServerParamList();
                    jsonServerParams.put("lat", newCenter.getLatitude());
                    jsonServerParams.put("lon", newCenter.getLongitude());
                    jsonServerParams.put("radius", newRadius);
                    jsonServerParams.put("number_of_results", newNumberOfResults);
                    jsonServerParams.put("tags", PoiCategory.listToJson(this.request.getProfile().getPoiCategoryList()));
                    // optional params
                    if (request.hasSearchTerm()) {
                        jsonServerParams.put("search", request.getSearchTerm());
                    }
                } catch (JSONException e) {
                    throw new ServerCommunicationException(Constants.RC.BAD_REQUEST);
                }

                InputStream in = null;
                ServerCommunicationException scException = null;
                JSONArray jsonPointList = null;
                try {
                    ServerInstance serverInstance = ServerUtility.getServerInstance(
                                SettingsManager.getInstance().getServerURL());
                    connection = ServerUtility.connectToServer(
                            String.format(
                                "%1$s/%2$s", serverInstance.getServerURL(), Constants.SERVER_COMMAND.GET_POI),
                            jsonServerParams);
                    if (this.cancelled) {
                        throw new ServerCommunicationException(Constants.RC.CANCELLED);
                    }
                    in = ServerUtility.getInputStream(connection);
                    JSONObject jsonServerResponse = new JSONObject(
                            ServerUtility.getServerResponse(in));
                    // get poi list
                    jsonPointList = jsonServerResponse.getJSONArray("poi");
                } catch (ServerCommunicationException e) {
                    scException = e;
                } catch (IOException e) {
                    Timber.e("ioError: %1$s", e.getMessage());
                    scException = new ServerCommunicationException(Constants.RC.CONNECTION_FAILED);
                } catch (JSONException e) {
                    Timber.e("JSONException: %1$s", e.getMessage());
                    scException = new ServerCommunicationException(Constants.RC.BAD_RESPONSE);
                } finally {
                    ServerUtility.cleanUp(connection, in);
                    if (scException != null) {
                        throw scException;
                    }
                }

                // parse points
                for (int i=0; i<jsonPointList.length(); i++) {
                    try {
                        newOnlyPoiList.add(
                                Point.create(jsonPointList.getJSONObject(i)));
                    } catch (JSONException e) {
                        Timber.e("server point profile request: point parsing error: %1$s", e.getMessage());
                    }
                }
            }

            // include points from favorite ids
            ArrayList<Point> newAllPointList = new ArrayList<Point>(newOnlyPoiList);
            if (request.getProfile().getIncludeFavorites()) {
                int newAllPointListLookupRadius = PoiProfileResult.calculateLookupRadius(
                        newAllPointList, newCenter, newRadius, newNumberOfResults);
                DatabaseProfileRequest databaseProfileRequest = new DatabaseProfileRequest(
                        DatabasePointProfile.FAVORITES, request.getSearchTerm(), SortMethod.DISTANCE_ASC);
                for (ObjectWithId objectWithId : accessDatabaseInstance.getObjectWithIdListFor(databaseProfileRequest)) {
                    if (objectWithId instanceof Point) {
                        Point favorite = (Point) objectWithId;
                        if (newCenter.distanceTo(favorite) > newAllPointListLookupRadius) {
                            // favorites sorted by distance (ascending), therefore "break" instead of "continue"
                            break;
                        }
                        // add if not already present
                        if (! newAllPointList.contains(favorite)) {
                            newAllPointList.add(favorite);
                        }
                    }
                }
            }

            // reset list position
            boolean newResetListPosition =
                this.action == RequestAction.UPDATE
                && (
                        cachedResult == null
                        || cachedResult.getCenter().distanceTo(newCenter) > DistanceThreshold.ONE_HUNDRED_METERS.getDistanceThresholdInMeters());

            Collections.sort(newAllPointList, new ObjectWithId.SortByDistanceFromCurrentDirection(true));
            return new PoiProfileResult(newRadius, newNumberOfResults,
                    newCenter, newAllPointList, newOnlyPoiList, newResetListPosition);
        }

        public boolean isRunning() {
            return this.executor != null && ! this.executor.isDone();
        }

        public boolean isCancelled() {
            return this.cancelled;
        }

        public void cancel() {
            this.cancelled = true;
            ServerUtility.cleanUp(connection, null);
            try {
                ServerUtility.cancelRunningRequest();
            } catch (ServerCommunicationException e) {}
        }

        public PoiProfileRequest getPoiProfileRequest() {
            return this.request;
        }

        public RequestAction getRequestAction() {
            return this.action;
        }

        public void addListener(PoiProfileRequestListener newListener) {
            if (newListener != null
                    && ! this.listenerList.contains(newListener)) {
                this.listenerList.add(newListener);
            }
        }

        public void removeListener(PoiProfileRequestListener newListener) {
            if (newListener != null
                    && this.listenerList.contains(newListener)) {
                this.listenerList.remove(newListener);
            }
        }
    }

}
