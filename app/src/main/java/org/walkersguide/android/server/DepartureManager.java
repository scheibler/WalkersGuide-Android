package org.walkersguide.android.server;

import android.content.Context;

import android.os.AsyncTask;
import android.os.Handler;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.point.Station;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.data.station.Departure;
import org.walkersguide.android.exception.ServerCommunicationException;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

import timber.log.Timber;


public class DepartureManager {

    public interface DepartureResultListener {
        public void departureRequestFinished(Context context, int returnCode, ArrayList<Departure> departureList, boolean resetListPosition);
    }


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
        cleanDepartureCache();
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

    public void cleanDepartureCache() {
        this.lastDepartureList = new ArrayList<Departure>();
        this.lastStation = null;
        this.lastRefreshed = 0l;
    }


    private class RequestDepartures extends AsyncTask<Void, Void, ArrayList<Departure>> {

        private ArrayList<DepartureResultListener> departureResultListenerList;
        private Station station;
        private int returnCode;
        private HttpsURLConnection connection;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;
        private boolean resetListPosition;

        public RequestDepartures(DepartureResultListener departureResultListener, Station station) {
            this.departureResultListenerList = new ArrayList<DepartureResultListener>();
            if (departureResultListener != null) {
                this.departureResultListenerList.add(departureResultListener);
            }
            this.station = station;
            this.returnCode = Constants.RC.OK;
            this.connection = null;
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
            this.resetListPosition = false;
        }

        @Override protected ArrayList<Departure> doInBackground(Void... params) {
            ServerSettings serverSettings = SettingsManager.getInstance(context).getServerSettings();

            ArrayList<Departure> departureList = null;
            if (! lastDepartureList.isEmpty()
                    && this.station.equals(lastStation)
                    && System.currentTimeMillis()-lastRefreshed < 30*60*1000) {
                Timber.d("cached departures");
                // cleanup already passed departures
                for (Iterator<Departure> iterator = lastDepartureList.iterator(); iterator.hasNext();) {
                    Departure departure = iterator.next();
                    if (departure.getTime()-System.currentTimeMillis() < -60000) {
                        iterator.remove();
                        this.resetListPosition = true;
                    }
                }
                departureList = lastDepartureList;

            } else {
                this.resetListPosition = true;
                Timber.d("querydepartures");

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
                    } else if (serverInstance.getSupportedPublicTransportProviderList().isEmpty()) {
                        this.returnCode = Constants.RC.NO_PUBLIC_TRANSPORT_PROVIDER_LIST;
                        return null;
                    }
                }

                // create server param list
                JSONObject jsonServerParams = null;
                try {
                    jsonServerParams = ServerUtility.createServerParamList(context);
                    jsonServerParams.put("lat", station.getLatitude());
                    jsonServerParams.put("lon", station.getLongitude());
                    // vehicles
                    JSONArray jsonVehicleList = new JSONArray();
                    for (String vehicle : station.getVehicleList()) {
                        jsonVehicleList.put(vehicle);
                    } 
                    jsonServerParams.put("vehicles", jsonVehicleList);
                    // provider
                    if (serverSettings.getSelectedPublicTransportProvider() != null) {
                        jsonServerParams.put("public_transport_provider", serverSettings.getSelectedPublicTransportProvider().getId());
                    }
                } catch (JSONException e) {
                    jsonServerParams = new JSONObject();
                }

                // start request
                JSONArray jsonDepartureList = null;
                try {
                    connection = ServerUtility.getHttpsURLConnectionObject(
                            context,
                            String.format(
                                "%1$s/%2$s", serverInstance.getServerURL(), Constants.SERVER_COMMAND.GET_DEPARTURES),
                            jsonServerParams);
                    cancelConnectionHandler.postDelayed(cancelConnection, 100);
                    connection.connect();
                    returnCode = connection.getResponseCode();
                    cancelConnectionHandler.removeCallbacks(cancelConnection);
                    if (isCancelled()) {
                        this.returnCode = Constants.RC.CANCELLED;
                    } else if (returnCode == Constants.RC.OK) {
                        JSONObject jsonServerResponse = ServerUtility.processServerResponseAsJSONObject(connection);
                        // get departure list
                        jsonDepartureList = jsonServerResponse.getJSONArray("departures");
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
                lastDepartureList = departureList;
                lastStation = this.station;
                lastRefreshed = System.currentTimeMillis();
            }

            return departureList;
        }

        @Override protected void onPostExecute(ArrayList<Departure> departureList) {
            for (DepartureResultListener departureResultListener : this.departureResultListenerList) {
                departureResultListener.departureRequestFinished(
                        context, returnCode, departureList, resetListPosition);
            }
        }

        @Override protected void onCancelled(ArrayList<Departure> departureList) {
            for (DepartureResultListener departureResultListener : this.departureResultListenerList) {
                departureResultListener.departureRequestFinished(
                        context, Constants.RC.CANCELLED, null, false);
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
