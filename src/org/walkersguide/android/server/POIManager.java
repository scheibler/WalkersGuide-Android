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
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.data.poi.POIProfile;
import org.walkersguide.android.data.poi.PointProfileObject;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.DownloadUtility;
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

    public void requestPOIProfile(
            POIProfileListener profileListener, int profileId, int requestAction) {
        if (requestInProgress()) {
            if (this.requestPOI.getPOIProfileId() == profileId
                    && this.requestPOI.getRequestAction() == requestAction) {
                this.requestPOI.updateListener(profileListener);
                return;
            } else {
                cancelRequest();
            }
        }
        this.requestPOI = new RequestPOI(profileListener, profileId, requestAction);
        this.requestPOI.execute();
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

    public void invalidateRequest() {
        if (requestInProgress()) {
            this.requestPOI.updateListener(null);
        }
    }

    private BroadcastReceiver newLocationReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                    && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD2.ID
                    && ! intent.getBooleanExtra(Constants.ACTION_NEW_LOCATION_ATTR.BOOL_AT_HIGH_SPEED, false)
                    && ! requestInProgress()) {
                requestPOIProfile(
                        null, settingsManagerInstance.getPOIFragmentSettings().getSelectedPOIProfileId(), ACTION_UPDATE);
            }
        }
    };


    private class RequestPOI extends AsyncTask<Void, Void, POIProfile> {

        private POIProfileListener poiProfileListener;
        private int poiProfileIdToRequest, requestAction, returnCode;
        private String additionalMessage;
        private HttpsURLConnection connection;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;
        private long t1;

        public RequestPOI(POIProfileListener poiProfileListener, int poiProfileId, int requestAction) {
            this.poiProfileListener = poiProfileListener;
            this.poiProfileIdToRequest = poiProfileId;
            this.requestAction = requestAction;
            this.returnCode = Constants.ID.OK;
            this.additionalMessage = "";
            this.connection = null;
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected POIProfile doInBackground(Void... params) {
            // check for map port
            ServerSettings serverSettings = SettingsManager.getInstance(context).getServerSettings();
            if (serverSettings.getSelectedMap() == null) {
                this.returnCode = Constants.ID.NO_MAP_SELECTED;
                return null;
            }

            // load poi profile
            AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(context);
            POIProfile poiProfile = accessDatabaseInstance.getPOIProfile(this.poiProfileIdToRequest);
            if (poiProfile == null) {
                this.returnCode = 1001;
                return null;
            } else if (poiProfile.getPOICategoryList().isEmpty()) {
                this.returnCode = 1002;
                return null;
            }

            // get current location
            PositionManager positionManagerInstance = PositionManager.getInstance(context);
            PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
            if (currentLocation.equals(PositionManager.getDummyLocation(context))) {
                this.returnCode = 1004;
                return null;
            }

            // get current direction
            DirectionManager directionManagerInstance = DirectionManager.getInstance(context);
            int currentDirection = directionManagerInstance.getCurrentDirection();
            if (currentDirection == Constants.DUMMY.DIRECTION) {
                this.returnCode = 1005;
                return null;
            }

            ArrayList<PointProfileObject> pointList = poiProfile.getPointProfileObjectList();
            if (pointList == null
                    || poiProfile.getCenter() == null
                    || this.requestAction == ACTION_MORE_RESULTS
                    || poiProfile.getCenter().distanceTo(currentLocation) > poiProfile.getLookupRadius()/2) {

                // get radius and number of results
                int radius = POIProfile.INITIAL_RADIUS;
                int numberOfResults = POIProfile.INITIAL_NUMBER_OF_RESULTS;
                switch (this.requestAction) {
                    case ACTION_MORE_RESULTS:
                        radius = poiProfile.getRadius();
                        numberOfResults = poiProfile.getNumberOfResults();
                        if (pointList != null) {
                            // radius
                            if (pointList.isEmpty()) {
                                radius += POIProfile.INITIAL_RADIUS;
                            } else {
                                int distanceToFarestPoint = pointList.get(pointList.size()-1).distanceFromCenter();
                                if (poiProfile.getRadius()-distanceToFarestPoint < POIProfile.INITIAL_RADIUS) {
                                    radius += POIProfile.INITIAL_RADIUS;
                                }
                            }
                            // number of results
                            int numberOfParsedPoints = pointList.size();
                            if (poiProfile.getNumberOfResults()-numberOfParsedPoints < POIProfile.INITIAL_NUMBER_OF_RESULTS) {
                                numberOfResults += POIProfile.INITIAL_NUMBER_OF_RESULTS;
                            }
                        }
                        break;
                    default:
                        break;
                }

                // poi category tags
                JSONArray jsonPOICategoryTagList = new JSONArray();
                for (POICategory poiCategory : poiProfile.getPOICategoryList()) {
                    jsonPOICategoryTagList.put(poiCategory.getTag());
                } 

                // request poi from server
                JSONArray jsonPointList = null;
                if (! DownloadUtility.isInternetAvailable(context)) {
                    this.returnCode = 1003;
                } else {
                    try {
                        // create parameter list
                        JSONObject requestJson = new JSONObject();
                        requestJson.put("lat", currentLocation.getPoint().getLatitude());
                        requestJson.put("lon", currentLocation.getPoint().getLongitude());
                        requestJson.put("radius", radius);
                        requestJson.put("number_of_results", numberOfResults);
                        requestJson.put("tags", jsonPOICategoryTagList);
                        requestJson.put("language", Locale.getDefault().getLanguage());
                        requestJson.put("session_id", ((GlobalInstance) context.getApplicationContext()).getSessionId());
                        System.out.println("xxx poi request: " + requestJson.toString());
                        // create connection
                        connection = DownloadUtility.getHttpsURLConnectionObject(
                                context,
                                String.format("%1$s/get_poi", serverSettings.getSelectedMap().getURL()),
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
                                jsonPointList = jsonServerResponse.getJSONArray("poi");
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

                // parse points
                if (returnCode == Constants.ID.OK) {
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
            }

            // update location and direction
            poiProfile.setCenterAndDirection(currentLocation, currentDirection);

            return poiProfile;
        }

        @Override protected void onPostExecute(POIProfile poiProfile) {
            System.out.println("xxx poiManager: " + this.returnCode);
            if (this.poiProfileListener != null) {
                this.poiProfileListener.poiProfileRequestFinished(
                        returnCode,
                        DownloadUtility.getErrorMessageForReturnCode(context, this.returnCode, ""),
                        poiProfile);
            }
        }

        @Override protected void onCancelled(POIProfile poiProfile) {
            System.out.println("xxx poiManager: cancelled");
            if (this.poiProfileListener != null) {
                this.poiProfileListener.poiProfileRequestFinished(
                        Constants.ID.CANCELLED,
                        DownloadUtility.getErrorMessageForReturnCode(context, Constants.ID.CANCELLED, ""),
                        poiProfile);
            }
        }

        public int getPOIProfileId() {
            return this.poiProfileIdToRequest;
        }

        public int getRequestAction() {
            return this.requestAction;
        }

        public void updateListener(POIProfileListener newProfileListener) {
            if (newProfileListener != null) {
                this.poiProfileListener = newProfileListener;
            }
        }

        public void cancel() {
            this.cancel(true);
        }

        private class CancelConnection implements Runnable {
            public void run() {
                if (isCancelled()) {
                    System.out.println("xxx cancel connection in runnable");
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

}
