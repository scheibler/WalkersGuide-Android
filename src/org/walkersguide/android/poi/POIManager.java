package org.walkersguide.android.poi;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.basic.point.Point;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.DownloadUtility;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

public class POIManager {

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
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_LOCATION);
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
    }

    public void requestPOIProfile(int poiProfileId) {
        if (this.requestPOI != null
                && this.requestPOI.getStatus() != AsyncTask.Status.FINISHED) {
            if (this.requestPOI.getPOIProfileId() == poiProfileId) {
                return;
            } else {
                this.requestPOI.cancel();
            }
        }
        this.requestPOI = new RequestPOI(this.context, poiProfileId);
        this.requestPOI.execute();
    }

    public void cancelPOIProfileRequest() {
        if (this.requestPOI != null
                && this.requestPOI.getStatus() != AsyncTask.Status.FINISHED) {
            this.requestPOI.cancel();
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                    && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_UPDATE_THRESHOLD, -1) >= 1
                    && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_AT_HIGH_SPEED, 0) == 0) {
                requestPOIProfile(
                        settingsManagerInstance.getPOISettings().getSelectedPOIProfileId());
            }
        }
    };


    private class RequestPOI extends AsyncTask<Void, Void, Integer> {

        private Context context;
        private HttpsURLConnection connection;
        private int poiProfileId;
        private String additionalMessage;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;
        private long t1;

        public RequestPOI(Context context, int poiProfileId) {
            this.context = context;
            this.connection = null;
            this.poiProfileId = poiProfileId;
            this.additionalMessage = "";
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected void onPreExecute() {
            t1 = System.currentTimeMillis();
        }

        @Override protected Integer doInBackground(Void... params) {
            // load poi profile
            AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(this.context);
            POIProfile poiProfile = accessDatabaseInstance.getPOIProfile(this.poiProfileId);
            if (poiProfile == null) {
                return 1001;
            } else if (poiProfile.getPOICategoryList().isEmpty()) {
                return 1002;
            }

            // get current location
            PositionManager positionManagerInstance = PositionManager.getInstance(this.context);
            Point currentLocation = positionManagerInstance.getCurrentLocation();
            if (currentLocation == null) {
                return 1004;
            }

            // get current direction
            DirectionManager directionManagerInstance = DirectionManager.getInstance(this.context);
            int currentDirection = directionManagerInstance.getCurrentDirection();
            if (currentDirection == -1) {
                return 1005;
            }

            int returnCode = Constants.ID.OK;
            if (poiProfile.getPointList() == null
                    || poiProfile.getPointList().isEmpty()
                    || poiProfile.getCenter() == null
                    || poiProfile.getCenter().distanceTo(currentLocation) > 100) {

                // request poi from server
                JSONArray jsonPointList = null;
                if (! DownloadUtility.isInternetAvailable(this.context)) {
                    returnCode = 1003;
                } else {
                    try {
                        // create parameter list
                        JSONObject requestJson = new JSONObject();
                        requestJson.put("lat", currentLocation.getLatitude());
                        requestJson.put("lon", currentLocation.getLongitude());
                        requestJson.put("radius", poiProfile.getRadius());
                        requestJson.put("tags", TextUtils.join("+", poiProfile.getPOICategoryList()));
                        requestJson.put("language", Locale.getDefault().getLanguage());
                        requestJson.put("session_id", settingsManagerInstance.getSessionId());
                        // create connection
                        connection = DownloadUtility.getHttpsURLConnectionObject(
                                this.context,
                                String.format("%1$s/get_poi", SettingsManager.SERVER_URL),
                                requestJson);
                        cancelConnectionHandler.postDelayed(cancelConnection, 100);
                        connection.connect();
                        cancelConnectionHandler.removeCallbacks(cancelConnection);
                        if (isCancelled()) {
                            returnCode = 1000;          // cancelled
                        } else if (connection.getResponseCode() != Constants.ID.OK) {
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
                    poiProfile.setPointList(jsonPointList);
                }
            }

            // update location and direction
            poiProfile.setCenterAndDirection(
                    currentLocation, currentDirection);

            return returnCode;
        }

        @Override protected void onPostExecute(Integer returnCode) {
            String returnMessage = DownloadUtility.getErrorMessageForReturnCode(
                    this.context, returnCode, this.additionalMessage);
            System.out.println("xxx poi updated: " + returnCode + ";   message: " + returnMessage + ";   time: " + (System.currentTimeMillis()-t1));
            // send poi profile update broadcast
            Intent intent = new Intent(Constants.ACTION_POI_PROFILE_UPDATED);
            intent.putExtra(
                    Constants.ACTION_POI_PROFILE_UPDATED_ATTR.INT_POI_PROFILE_ID,
                    this.poiProfileId);
            intent.putExtra(
                    Constants.ACTION_POI_PROFILE_UPDATED_ATTR.INT_RETURN_CODE,
                    returnCode);
            intent.putExtra(
                    Constants.ACTION_POI_PROFILE_UPDATED_ATTR.STRING_RETURN_MESSAGE,
                    returnMessage);
            LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
        }

        public int getPOIProfileId() {
            return this.poiProfileId;
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
                    return;
                }
                cancelConnectionHandler.postDelayed(this, 100);
            }
        }
    }

}
