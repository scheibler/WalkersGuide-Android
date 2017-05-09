package org.walkersguide.android.server;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.data.basic.wrapper.Station;
import org.walkersguide.android.data.station.Departure;
import org.walkersguide.android.helper.DownloadUtility;
import org.walkersguide.android.listener.DepartureResultListener;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

public class DepartureManager {

    private Context context;
    private static DepartureManager departureManagerInstance;
    private RequestDepartures requestDepartures;
    private Station lastStation;
    private long lastRefreshed;
    private ArrayList<Departure> departureList;

    public static DepartureManager getInstance(Context context) {
        if(departureManagerInstance == null){
            departureManagerInstance = new DepartureManager(context.getApplicationContext());
        }
        return departureManagerInstance;
    }

    private DepartureManager(Context context) {
        this.context = context;
        this.requestDepartures = null;
        this.lastStation = null;
        this.lastRefreshed = 0l;
        this.departureList = new ArrayList<Departure>();
    }

    public void requestDepartureList(DepartureResultListener departureResultListener, Station station) {
        if (! station.equals(lastStation)
                || System.currentTimeMillis()-lastRefreshed > 30*60*1000) {
            this.cancelDepartureRequest();
            this.requestDepartures = new RequestDepartures(departureResultListener, station);
            this.requestDepartures.execute();
        } else {
            for (Iterator<Departure> iterator = departureList.iterator(); iterator.hasNext();) {
                Departure departure = iterator.next();
                if (departure.getTime()-System.currentTimeMillis() < 0) {
                    iterator.remove();
                }
            }
            if (departureResultListener != null) {
                departureResultListener.departureQuerySuccessful(departureList);
            }
        }
    }

    public void cancelDepartureRequest() {
        if (this.requestDepartures != null
                && this.requestDepartures.getStatus() != AsyncTask.Status.FINISHED) {
            this.requestDepartures.cancel();
        }
    }


    private class RequestDepartures extends AsyncTask<Void, Void, Integer> {

        private HttpsURLConnection connection;
        private DepartureResultListener departureResultListener;
        private Station station;
        private ArrayList<Departure> departureListResult;
        private String additionalMessage;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;

        public RequestDepartures(DepartureResultListener departureResultListener, Station station) {
            this.connection = null;
            this.departureResultListener = departureResultListener;
            this.station = station;
            this.departureListResult = new ArrayList<Departure>();
            this.additionalMessage = "";
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected Integer doInBackground(Void... params) {
            // internet
            if (! DownloadUtility.isInternetAvailable(context)) {
                return 1003;
            }
            // map
            ServerSettings serverSettings = SettingsManager.getInstance(context).getServerSettings();
            if (serverSettings.getSelectedMap() == null) {
                return Constants.ID.NO_MAP_SELECTED;
            }

            int returnCode = Constants.ID.OK;
            JSONArray jsonDepartureList = null;
            try {
                // create parameter list
                JSONObject requestJson = new JSONObject();
                requestJson.put("lat", station.getLatitude());
                requestJson.put("lon", station.getLongitude());
                // vehicles
                JSONArray jsonVehicleList = new JSONArray();
                for (String vehicle : station.getVehicleList()) {
                    jsonVehicleList.put(vehicle);
                } 
                requestJson.put("vehicles", jsonVehicleList);
                // provider
                if (serverSettings.getSelectedPublicTransportProvider() != null) {
                    requestJson.put("public_transport_provider", serverSettings.getSelectedPublicTransportProvider().getIdentifier());
                }
                // lang and uid
                requestJson.put("language", Locale.getDefault().getLanguage());
                requestJson.put("session_id", ((GlobalInstance) context.getApplicationContext()).getSessionId());
                System.out.println("xxx departure request: " + requestJson.toString());
                // create connection
                connection = DownloadUtility.getHttpsURLConnectionObject(
                        context,
                        String.format("%1$s/get_departures", serverSettings.getSelectedMap().getURL()),
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
                        // get departure list
                        jsonDepartureList = jsonServerResponse.getJSONArray("departures");
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

            if (returnCode == Constants.ID.OK) {
                for (int i=0; i<jsonDepartureList.length(); i++) {
                    try {
                        JSONObject jsonDeparture = jsonDepartureList.getJSONObject(i);
                        this.departureListResult.add(
                                new Departure(
                                    jsonDeparture.getString("nr"),
                                    jsonDeparture.getString("to"),
                                    jsonDeparture.getLong("time"))
                                );
                    } catch (JSONException e) {}
                }
            }

            return returnCode;
        }

        @Override protected void onPostExecute(Integer returnCode) {
            if (returnCode == Constants.ID.OK) {
                lastStation = this.station;
                lastRefreshed = System.currentTimeMillis();
                departureList = this.departureListResult;
                if (this.departureResultListener != null) {
                    this.departureResultListener.departureQuerySuccessful(departureList);
                }
            } else {
                if (this.departureResultListener != null) {
                    this.departureResultListener.departureQueryFailed(
                            DownloadUtility.getErrorMessageForReturnCode(
                                context, returnCode, this.additionalMessage));
                }
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
                    return;
                }
                cancelConnectionHandler.postDelayed(this, 100);
            }
        }
    }

}
