package org.walkersguide.android.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.exception.ServerCommunicationException;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.data.basic.point.Station;
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
    private ArrayList<Departure> lastDepartureList;
    private Station lastStation;
    private long lastRefreshed;

    public static DepartureManager getInstance(Context context) {
        if(departureManagerInstance == null){
            departureManagerInstance = new DepartureManager(context.getApplicationContext());
        }
        return departureManagerInstance;
    }

    private DepartureManager(Context context) {
        this.context = context;
        this.requestDepartures = null;
        this.lastDepartureList = new ArrayList<Departure>();
        this.lastStation = null;
        this.lastRefreshed = 0l;
    }

    public void requestDepartureList(DepartureResultListener departureResultListener, Station station) {
        if (departureRequestInProgress()) {
            if (departureResultListener == null) {
                return;
            } else if (this.requestDepartures.getStation().equals(station)) {
                this.requestDepartures.addListener(departureResultListener);
                return;
            } else {
                cancelDepartureRequest();
            }
        }
        this.requestDepartures = new RequestDepartures(departureResultListener, station);
        this.requestDepartures.execute();
    }

    public void invalidateDepartureRequest(DepartureResultListener departureResultListener) {
        if (departureRequestInProgress()) {
            this.requestDepartures.removeListener(departureResultListener);
        }
    }

    public boolean departureRequestInProgress() {
        if (this.requestDepartures != null
                && this.requestDepartures.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void cancelDepartureRequest() {
        if (departureRequestInProgress()) {
            this.requestDepartures.cancel();
        }
    }


    private class RequestDepartures extends AsyncTask<Void, Void, ArrayList<Departure>> {

        private ArrayList<DepartureResultListener> departureResultListenerList;
        private Station station;
        private int returnCode;
        private String additionalMessage;
        private HttpsURLConnection connection;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;

        public RequestDepartures(DepartureResultListener departureResultListener, Station station) {
            this.departureResultListenerList = new ArrayList<DepartureResultListener>();
            if (departureResultListener != null) {
                this.departureResultListenerList.add(departureResultListener);
            }
            this.station = station;
            this.returnCode = Constants.RC.OK;
            this.additionalMessage = "";
            this.connection = null;
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected ArrayList<Departure> doInBackground(Void... params) {
            ServerSettings serverSettings = SettingsManager.getInstance(context).getServerSettings();

            ArrayList<Departure> departureList = null;
            if (this.station.equals(lastStation)
                    && System.currentTimeMillis()-lastRefreshed < 30*60*1000) {
                // cleanup already passed departures
                for (Iterator<Departure> iterator = lastDepartureList.iterator(); iterator.hasNext();) {
                    Departure departure = iterator.next();
                    if (departure.getTime()-System.currentTimeMillis() < 0) {
                        iterator.remove();
                    }
                }
                departureList = lastDepartureList;

            } else {
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

                // start request
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
                        requestJson.put("public_transport_provider", serverSettings.getSelectedPublicTransportProvider().getId());
                    }
                    // lang, map and uid
                    requestJson.put("language", Locale.getDefault().getLanguage());
                    requestJson.put("logging_allowed", serverSettings.getLogQueriesOnServer());
                    requestJson.put("map_id", serverSettings.getSelectedMap().getId());
                    requestJson.put("session_id", ((GlobalInstance) context.getApplicationContext()).getSessionId());
                    System.out.println("xxx departure request: " + requestJson.toString());
                    // create connection
                    connection = DownloadUtility.getHttpsURLConnectionObject(
                            context,
                            DownloadUtility.generateServerCommand(
                                serverInstance.getServerURL(), Constants.SERVER_COMMAND.GET_DEPARTURES),
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
                            // get departure list
                            jsonDepartureList = jsonServerResponse.getJSONArray("departures");
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
                    lastStation = this.station;
                    lastRefreshed = System.currentTimeMillis();
                }

                departureList = new ArrayList<Departure>();
                for (int i=0; i<jsonDepartureList.length(); i++) {
                    try {
                        JSONObject jsonDeparture = jsonDepartureList.getJSONObject(i);
                        departureList.add(
                                new Departure(
                                    jsonDeparture.getString("nr"),
                                    jsonDeparture.getString("to"),
                                    jsonDeparture.getLong("time"))
                                );
                    } catch (JSONException e) {}
                }
            }

            return departureList;
        }

        @Override protected void onPostExecute(ArrayList<Departure> departureList) {
            for (DepartureResultListener departureResultListener : this.departureResultListenerList) {
                departureResultListener.departureRequestFinished(
                        this.returnCode,
                        DownloadUtility.getErrorMessageForReturnCode(context, this.returnCode, this.additionalMessage),
                        departureList);
            }
        }

        @Override protected void onCancelled(ArrayList<Departure> departureList) {
            for (DepartureResultListener departureResultListener : this.departureResultListenerList) {
                departureResultListener.departureRequestFinished(
                        Constants.RC.CANCELLED,
                        DownloadUtility.getErrorMessageForReturnCode(context, Constants.RC.CANCELLED, ""),
                        departureList);
            }
        }

        public void cancel() {
            this.cancel(true);
        }

        public Station getStation() {
            return this.station;
        }

        public void addListener(DepartureResultListener newDepartureResultListener) {
            if (newDepartureResultListener != null
                    && ! this.departureResultListenerList.contains(newDepartureResultListener)) {
                this.departureResultListenerList.add(newDepartureResultListener);
            }
        }

        public void removeListener(DepartureResultListener newDepartureResultListener) {
            if (newDepartureResultListener != null
                    && this.departureResultListenerList.contains(newDepartureResultListener)) {
                this.departureResultListenerList.remove(newDepartureResultListener);
            }
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
