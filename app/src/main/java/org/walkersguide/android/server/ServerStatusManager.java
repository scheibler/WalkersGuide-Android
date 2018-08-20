package org.walkersguide.android.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.data.server.OSMMap;
import org.walkersguide.android.data.route.WayClass;
import org.walkersguide.android.data.server.PublicTransportProvider;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.DownloadUtility;
import org.walkersguide.android.listener.ServerStatusListener;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.RouteSettings;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.exception.ServerCommunicationException;
import org.walkersguide.android.data.server.ServerInstance;
import android.text.TextUtils;
import java.util.Collections;
import java.util.Locale;
import org.walkersguide.android.util.GlobalInstance;


public class ServerStatusManager {

    private Context context;
    private static ServerStatusManager serverStatusManagerInstance;
    private SettingsManager settingsManagerInstance;
    private RequestServerStatus requestServerStatus;
    private ServerInstance activeServerInstance;

    public static ServerStatusManager getInstance(Context context) {
        if(serverStatusManagerInstance == null){
            serverStatusManagerInstance = new ServerStatusManager(context.getApplicationContext());
        }
        return serverStatusManagerInstance;
    }

    private ServerStatusManager(Context context) {
        this.context = context;
        this.settingsManagerInstance = SettingsManager.getInstance(context);
        this.requestServerStatus = null;
        this.activeServerInstance = null;
    }

    public void requestServerStatus(ServerStatusListener profileListener, String serverURL) {
        if (serverStatusRequestInProgress()) {
            if (profileListener == null) {
                return;
            } else if (this.requestServerStatus.getServerURL().equals(serverURL)) {
                this.requestServerStatus.addListener(profileListener);
                return;
            } else {
                cancelServerStatusRequest();
            }
        }
        this.requestServerStatus = new RequestServerStatus(profileListener, serverURL);
        this.requestServerStatus.execute();
    }

    public void invalidateServerStatusRequest(ServerStatusListener profileListener) {
        if (serverStatusRequestInProgress()) {
            this.requestServerStatus.removeListener(profileListener);
        }
    }

    public boolean serverStatusRequestInProgress() {
        if (this.requestServerStatus != null
                && this.requestServerStatus.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void cancelServerStatusRequest() {
        if (serverStatusRequestInProgress()) {
            this.requestServerStatus.cancel();
        }
        this.requestServerStatus = null;
    }

    public ServerInstance getServerInstance() {
        return this.activeServerInstance;
    }

    public ServerInstance getServerInstanceForURL(String serverURL) throws ServerCommunicationException {
        if (this.activeServerInstance == null
                || ! this.activeServerInstance.getServerURL().equals(serverURL)) {
            this.activeServerInstance = ServerStatusManager.queryServerInstance(context, serverURL);
        }
        return this.activeServerInstance;
    }


    public class RequestServerStatus extends AsyncTask<Void, Void, ServerInstance> {

        private ArrayList<ServerStatusListener> serverStatusListenerList;
        private String serverURL;
        private int returnCode;

        public RequestServerStatus(ServerStatusListener serverStatusListener, String serverURL) {
            this.serverStatusListenerList = new ArrayList<ServerStatusListener>();
            if (serverStatusListener != null) {
                this.serverStatusListenerList.add(serverStatusListener);
            }
            this.serverURL = serverURL;
            this.returnCode = Constants.RC.OK;
        }

        @Override protected ServerInstance doInBackground(Void... params) {
            if (TextUtils.isEmpty(this.serverURL)) {
                this.returnCode = Constants.RC.NO_SERVER_URL;
                return null;
            }
            activeServerInstance = null;
            ServerInstance serverInstance = null;
            try {
                serverInstance = getServerInstanceForURL(this.serverURL);
            } catch (ServerCommunicationException e) {
                this.returnCode = e.getReturnCode();
                return null;
            }
            return serverInstance;
        }

        @Override protected void onPostExecute(ServerInstance serverInstance) {
            System.out.println("xxx ServerStatus: " + this.returnCode);
            for (ServerStatusListener serverStatusListener : this.serverStatusListenerList) {
                serverStatusListener.serverStatusRequestFinished(
                        this.returnCode,
                        DownloadUtility.getErrorMessageForReturnCode(context, this.returnCode, ""),
                        serverInstance);
            }
        }

        @Override protected void onCancelled(ServerInstance serverInstance) {
            System.out.println("xxx cancel - serverstatus: " + this.returnCode);
            for (ServerStatusListener serverStatusListener : this.serverStatusListenerList) {
                serverStatusListener.serverStatusRequestFinished(
                        Constants.RC.CANCELLED,
                        DownloadUtility.getErrorMessageForReturnCode(context, Constants.RC.CANCELLED, ""),
                        serverInstance);
            }
        }

        public void cancel() {
            this.cancel(true);
        }

        public String getServerURL() {
            return this.serverURL;
        }

        public void addListener(ServerStatusListener newServerStatusListener) {
            if (newServerStatusListener != null
                    && ! this.serverStatusListenerList.contains(newServerStatusListener)) {
                this.serverStatusListenerList.add(newServerStatusListener);
            }
        }

        public void removeListener(ServerStatusListener newServerStatusListener) {
            if (newServerStatusListener != null
                    && this.serverStatusListenerList.contains(newServerStatusListener)) {
                this.serverStatusListenerList.remove(newServerStatusListener);
            }
        }
    }


    public static ServerInstance queryServerInstance(Context context, String serverURL) throws ServerCommunicationException {
        JSONObject jsonServerResponse = null;
        int returnCode = Constants.RC.OK;

        // check for internet connection
        if (! DownloadUtility.isInternetAvailable(context)) {
            throw new ServerCommunicationException(context, Constants.RC.NO_INTERNET_CONNECTION);
        }

        HttpsURLConnection connection = null;
        try {
            // create parameter list
            JSONObject requestJson = new JSONObject();
            requestJson.put("language", Locale.getDefault().getLanguage());
            requestJson.put("logging_allowed", SettingsManager.getInstance(context).getServerSettings().getLogQueriesOnServer());
            requestJson.put("session_id", ((GlobalInstance) context.getApplicationContext()).getSessionId());
            connection = DownloadUtility.getHttpsURLConnectionObject(
                    context,
                    DownloadUtility.generateServerCommand(
                        serverURL, Constants.SERVER_COMMAND.GET_STATUS),
                    requestJson);
            connection.setConnectTimeout(3000);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode != Constants.RC.OK) {
                returnCode = Constants.RC.SERVER_CONNECTION_ERROR;
            } else {
                jsonServerResponse = DownloadUtility.processServerResponseAsJSONObject(connection);
            }
        } catch (IOException e) {
            returnCode = Constants.RC.SERVER_CONNECTION_ERROR;
        } catch (JSONException e) {
            returnCode = Constants.RC.SERVER_RESPONSE_ERROR;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (returnCode != Constants.RC.OK) {
                throw new ServerCommunicationException(context, returnCode);
            }
        }

        ArrayList<OSMMap> availableMapList = new ArrayList<OSMMap>();
        JSONObject jsonMapDict = null;
        try {
            jsonMapDict = jsonServerResponse.getJSONObject("maps");
        } catch (JSONException e) {
            jsonMapDict = null;
        } finally {
            if (jsonMapDict != null) {
                Iterator<String> iter = jsonMapDict.keys();
                while (iter.hasNext()) {
                    String mapId = iter.next();
                    try {
                        JSONObject jsonMap = jsonMapDict.getJSONObject(mapId);
                        availableMapList.add(
                                new OSMMap(
                                    mapId,
                                    jsonMap.getString("name"),
                                    jsonMap.getString("description"),
                                    jsonMap.getLong("created"),
                                    jsonMap.getBoolean("development")));
                    } catch (JSONException e) {}
                }
            }
        }
        if (availableMapList.isEmpty()) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_RESPONSE_ERROR);
        }

        ArrayList<PublicTransportProvider> supportedPublicTransportProviderList = new ArrayList<PublicTransportProvider>();
        JSONArray jsonPublicTransportProviderList = null;
        try {
            jsonPublicTransportProviderList = jsonServerResponse.getJSONArray("supported_public_transport_provider_list");
        } catch (JSONException e) {
            jsonPublicTransportProviderList = null;
        } finally {
            if (jsonPublicTransportProviderList != null) {
                for (int i=0; i<jsonPublicTransportProviderList.length(); i++) {
                    try {
                        supportedPublicTransportProviderList.add(
                                new PublicTransportProvider(context, jsonPublicTransportProviderList.getString(i)));
                    } catch (JSONException e) {}
                }
            }
        }
        if (supportedPublicTransportProviderList.isEmpty()) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_RESPONSE_ERROR);
        }

        ArrayList<POICategory> supportedPOICategoryList = new ArrayList<POICategory>();
        JSONArray jsonPOICategoryList = null;
        try {
            jsonPOICategoryList = jsonServerResponse.getJSONArray("supported_poi_category_list");
        } catch (JSONException e) {
            jsonPOICategoryList = null;
        } finally {
            if (jsonPOICategoryList != null) {
                for (int i=0; i<jsonPOICategoryList.length(); i++) {
                    try {
                        supportedPOICategoryList.add(
                                new POICategory(context, jsonPOICategoryList.getString(i)));
                    } catch (JSONException e) {}
                }
            }
        }
        if (supportedPOICategoryList.isEmpty()) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_RESPONSE_ERROR);
        }

        ArrayList<WayClass> supportedWayClassList = new ArrayList<WayClass>();
        JSONArray jsonWayClassList = null;
        try {
            jsonWayClassList = jsonServerResponse.getJSONArray("supported_way_class_list");
        } catch (JSONException e) {
            jsonWayClassList = null;
        } finally {
            if (jsonWayClassList != null) {
                for (int i=0; i<jsonWayClassList.length(); i++) {
                    try {
                        supportedWayClassList.add(
                                new WayClass(context, jsonWayClassList.getString(i)));
                    } catch (JSONException e) {}
                }
            }
        }
        if (supportedWayClassList.isEmpty()) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_RESPONSE_ERROR);
        }

        ArrayList<Double> supportedIndirectionFactorList = new ArrayList<Double>();
        JSONArray jsonIndirectionFactorList = null;
        try {
            jsonIndirectionFactorList = jsonServerResponse.getJSONArray("supported_indirection_factor_list");
        } catch (JSONException e) {
            jsonIndirectionFactorList = null;
        } finally {
            if (jsonIndirectionFactorList != null) {
                for (int i=0; i<jsonIndirectionFactorList.length(); i++) {
                    try {
                        supportedIndirectionFactorList.add(
                                jsonIndirectionFactorList.getDouble(i));
                    } catch (JSONException e) {}
                }
            }
        }
        if (supportedIndirectionFactorList.isEmpty()) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_RESPONSE_ERROR);
        }

        ArrayList<Integer> supportedAPIVersionList = new ArrayList<Integer>();
        JSONArray jsonAPIVersionList = null;
        try {
            jsonAPIVersionList = jsonServerResponse.getJSONArray("supported_api_version_list");
        } catch (JSONException e) {
            jsonAPIVersionList = null;
        } finally {
            if (jsonAPIVersionList != null) {
                for (int i=0; i<jsonAPIVersionList.length(); i++) {
                    try {
                        supportedAPIVersionList.add(
                                jsonAPIVersionList.getInt(i));
                    } catch (JSONException e) {}
                }
            }
        }
        if (supportedAPIVersionList.isEmpty()) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_RESPONSE_ERROR);
        }

        // check for server api version
        Collections.sort(supportedAPIVersionList);
        if (BuildConfig.SUPPORTED_API_VERSION < supportedAPIVersionList.get(0)) {
            throw new ServerCommunicationException(context, Constants.RC.API_CLIENT_OUTDATED);
        } else if (BuildConfig.SUPPORTED_API_VERSION > supportedAPIVersionList.get(supportedAPIVersionList.size()-1)) {
            throw new ServerCommunicationException(context, Constants.RC.API_SERVER_OUTDATED);
        }

        // create server instance object
        ServerInstance serverInstance = null;
        try {
            serverInstance = new ServerInstance(
                    jsonServerResponse.getString("server_name"),
                    serverURL,
                    jsonServerResponse.getString("server_version"),
                    availableMapList,
                    supportedPublicTransportProviderList,
                    supportedPOICategoryList,
                    supportedWayClassList,
                    supportedIndirectionFactorList,
                    supportedAPIVersionList);
        } catch (JSONException e) {
            serverInstance = null;
        } finally {
            if (serverInstance == null) {
                throw new ServerCommunicationException(context, Constants.RC.SERVER_RESPONSE_ERROR);
            }
        }

        // update server settings
        ServerSettings serverSettings = SettingsManager.getInstance(context).getServerSettings();
        if (! serverInstance.getServerURL().equals(serverSettings.getServerURL())) {
            serverSettings.setServerURL(serverInstance.getServerURL());
        }
        if (serverSettings.getSelectedMap() != null) {
            OSMMap selectedMapUpdated = null;
            for (OSMMap mapFromServer : serverInstance.getAvailableMapList()) {
                if (mapFromServer.equals(serverSettings.getSelectedMap())) {
                    selectedMapUpdated = mapFromServer;
                    break;
                }
            }
            if (selectedMapUpdated != null) {
                // update currently selected map
                serverSettings.setSelectedMap(selectedMapUpdated);
            } else {
                // previously selected map is no longer available
                serverSettings.setSelectedMap(null);
            }
        }
        if (serverSettings.getSelectedPublicTransportProvider() == null
                || ! serverInstance.getSupportedPublicTransportProviderList().contains(serverSettings.getSelectedPublicTransportProvider())) {
            serverSettings.setSelectedPublicTransportProvider(
                    serverInstance.getSupportedPublicTransportProviderList().get(0));
        }

        // update route settings
        RouteSettings routeSettings = SettingsManager.getInstance(context).getRouteSettings();
        // indirection factor
        if (! supportedIndirectionFactorList.contains(routeSettings.getIndirectionFactor())) {
            routeSettings.setIndirectionFactor(RouteSettings.DEFAULT_INDIRECTION_FACTOR);
        }
        // way classes
        ArrayList<WayClass> updatedWayClassList = new ArrayList();
        for (WayClass wayClassFromSettings : routeSettings.getWayClassList()) {
            if (supportedWayClassList.contains(wayClassFromSettings)) {
                updatedWayClassList.add(wayClassFromSettings);
            }
        }
        if (updatedWayClassList.isEmpty()) {
            routeSettings.setWayClassList(supportedWayClassList);
        } else {
            routeSettings.setWayClassList(updatedWayClassList);
        }

        return serverInstance;
    }


    /** cancel running request
     */

    private CancelRequest cancelRequest;;

    public void cancelRunningRequestOnServer() {
        if (this.cancelRequest != null
                && this.cancelRequest.getStatus() != AsyncTask.Status.FINISHED) {
            this.cancelRequest.cancel();
        }
        this.cancelRequest = new CancelRequest();
        this.cancelRequest.execute();
    }


    public class CancelRequest extends AsyncTask<Void, Void, Void> {

        private HttpsURLConnection connection;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;

        public CancelRequest() {
            this.connection = null;
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected Void doInBackground(Void... params) {
            try {
                // server url
                ServerInstance serverInstance = getServerInstanceForURL(
                        SettingsManager.getInstance(context).getServerSettings().getServerURL());
                // create parameter list
                JSONObject requestJson = new JSONObject();
                requestJson.put("language", Locale.getDefault().getLanguage());
                requestJson.put("logging_allowed", SettingsManager.getInstance(context).getServerSettings().getLogQueriesOnServer());
                requestJson.put("session_id", ((GlobalInstance) context.getApplicationContext()).getSessionId());
                // create connection
                connection = DownloadUtility.getHttpsURLConnectionObject(
                        context,
                        DownloadUtility.generateServerCommand(
                            serverInstance.getServerURL(), Constants.SERVER_COMMAND.CANCEL_REQUEST),
                        requestJson);
                cancelConnectionHandler.postDelayed(cancelConnection, 100);
                connection.connect();
                int responseCode = connection.getResponseCode();
                cancelConnectionHandler.removeCallbacks(cancelConnection);
            } catch (IOException e) {
            } catch (JSONException e) {
            } catch (ServerCommunicationException e) {
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
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
