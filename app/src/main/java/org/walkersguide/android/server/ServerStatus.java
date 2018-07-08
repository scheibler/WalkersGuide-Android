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
import org.walkersguide.android.data.server.PublicTransportProvider;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.DownloadUtility;
import org.walkersguide.android.listener.ServerStatusListener;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

public class ServerStatus extends AsyncTask<Void, Void, Integer> {

    // actions
    public static final int ACTION_UPDATE_MANAGEMENT = 385;
    public static final int ACTION_UPDATE_MAP = 386;
    public static final int ACTION_UPDATE_BOTH = 387;

    private Context context;
    private ServerStatusListener serverStatusListener;
    private int updateAction;
    private String serverManagementURL;
    private OSMMap selectedMap;
    private HttpsURLConnection connection;
    private Handler cancelConnectionHandler;
    private CancelConnection cancelConnection;

    public ServerStatus(Context context,
            int updateAction, String serverManagementURL, OSMMap selectedMap) {
        this.context = context;
        if (context instanceof ServerStatusListener) {
            this.serverStatusListener = (ServerStatusListener) context;
        }
        this.updateAction = updateAction;
        this.serverManagementURL = serverManagementURL;
        this.selectedMap = selectedMap;
        this.connection = null;
        this.cancelConnectionHandler = new Handler();
        this.cancelConnection = new CancelConnection();
    }

    public ServerStatus(Context context, ServerStatusListener serverStatusListener,
            int updateAction, String serverManagementURL, OSMMap selectedMap) {
        this.context = context;
        this.serverStatusListener = serverStatusListener;
        this.updateAction = updateAction;
        this.serverManagementURL = serverManagementURL;
        this.selectedMap = selectedMap;
        this.connection = null;
        this.cancelConnectionHandler = new Handler();
        this.cancelConnection = new CancelConnection();
    }

    @Override protected Integer doInBackground(Void... params) {
        if (! DownloadUtility.isInternetAvailable(this.context)) {
            return 1003;
        }

        AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(this.context);
        ServerSettings serverSettings = SettingsManager.getInstance(this.context).getServerSettings();
        int returnCode = Constants.ID.OK;

        if (updateAction == ACTION_UPDATE_MANAGEMENT
                || updateAction == ACTION_UPDATE_BOTH) {
            JSONObject jsonMapDict = null;
            try {
                connection = DownloadUtility.getHttpsURLConnectionObject(
                        this.context,
                        String.format("%1$s/get_available_maps", this.serverManagementURL),
                        null);
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
                    if (jsonServerResponse.has("available_maps")) {
                        jsonMapDict = jsonServerResponse.getJSONObject("available_maps");
                    }
                }
            } catch (IOException e) {
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
            System.out.println("xxx serverStatus management rc: " + returnCode);

            if (returnCode == Constants.ID.OK) {
                // set available maps
                ArrayList<OSMMap> oldMapList = accessDatabaseInstance.getMapList();
                // load new map list
                Iterator<String> iter = jsonMapDict.keys();
                while (iter.hasNext()) {
                    String mapName = iter.next();
                    OSMMap map = null;
                    try {
                        map = new OSMMap(mapName, jsonMapDict.getString(mapName));
                    } catch (JSONException e) {
                        map = null;
                    } finally {
                        if (map != null) {
                            accessDatabaseInstance.addMap(map.getName(), map.getURL());
                            if (oldMapList.contains(map)) {
                                oldMapList.remove(map);
                            }
                        }
                    }
                }
                // remove not longer supported maps
                for (OSMMap map : oldMapList) {
                    accessDatabaseInstance.removeMap(map.getName());
                }
                // new server url
                if (! this.serverManagementURL.equals(serverSettings.getServerURL())) {
                    serverSettings.setServerURL(this.serverManagementURL);
                }
            }
        }

        if (updateAction == ACTION_UPDATE_MAP
                || updateAction == ACTION_UPDATE_BOTH) {
            if (this.selectedMap == null) {
                return 1006;            // no map selected
            }
            OSMMap map = null;
            JSONArray jsonSupportedPOITags = null;
            JSONObject jsonPublicTransportProviderDict = null;
            try {
                connection = DownloadUtility.getHttpsURLConnectionObject(
                        this.context,
                        String.format("%1$s/get_status", this.selectedMap.getURL()),
                        null);
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
                    // map
                    map = new OSMMap(
                            jsonServerResponse.getString("map_name"),
                            this.selectedMap.getURL(),
                            jsonServerResponse.getInt("map_version"),
                            jsonServerResponse.getLong("map_created"));
                    // poi tags and public transport provider
                    jsonSupportedPOITags = jsonServerResponse.getJSONArray("supported_poi_tags");
                    jsonPublicTransportProviderDict = jsonServerResponse.getJSONObject("supported_public_transport_provider");
                }
            } catch (IOException e) {
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
                // poi categories
                ArrayList<String> oldPOICategoryTagList = new ArrayList<String>();
                for (POICategory poiCategory : accessDatabaseInstance.getPOICategoryList()) {
                    oldPOICategoryTagList.add(poiCategory.getTag());
                }
                for (int i=0; i<jsonSupportedPOITags.length(); i++) {
                    String tag = null;
                    try {
                        tag = jsonSupportedPOITags.getString(i);
                    } catch (JSONException e) {
                        tag = null;
                    } finally {
                        if (tag != null) {
                            accessDatabaseInstance.addPOICategory(tag);
                            // try to find in old tag list
                            if (oldPOICategoryTagList.contains(tag)) {
                                oldPOICategoryTagList.remove(tag);
                            }
                        }
                    }
                }
                for (String tag : oldPOICategoryTagList) {
                    accessDatabaseInstance.removePOICategory(tag);
                    System.out.println("xxx remove poi tag: " + tag);
                }
                // public transport provider
                ArrayList<PublicTransportProvider> oldPublicTransportProviderList = accessDatabaseInstance.getPublicTransportProviderList();
                // load new map list
                Iterator<String> iter = jsonPublicTransportProviderDict.keys();
                while (iter.hasNext()) {
                    String identifier = iter.next();
                    PublicTransportProvider publicTransportProvider = null;
                    try {
                        publicTransportProvider = new PublicTransportProvider(
                                identifier, jsonPublicTransportProviderDict.getString(identifier));
                    } catch (JSONException e) {
                        publicTransportProvider = null;
                    } finally {
                        if (publicTransportProvider != null) {
                            accessDatabaseInstance.addPublicTransportProvider(
                                    publicTransportProvider.getIdentifier(), publicTransportProvider.getName());
                            if (oldPublicTransportProviderList.contains(publicTransportProvider)) {
                                oldPublicTransportProviderList.remove(publicTransportProvider);
                            }
                        }
                    }
                }
                // remove not longer supported provider
                for (PublicTransportProvider publicTransportProvider : oldPublicTransportProviderList) {
                    accessDatabaseInstance.removePublicTransportProvider(publicTransportProvider.getIdentifier());
                }
                // set new map and check if app is outdated
                if (map.getVersion() > Constants.DEFAULT.SUPPORTED_MAP_VERSION) {
                    returnCode = 1007;          // app outdated
                } else {
                    serverSettings.setSelectedMap(map);
                }
            }
        }

        return returnCode;
    }

    @Override protected void onPostExecute(Integer returnCode) {
        System.out.println("xxx server status rc: " + returnCode);
        String returnMessage = DownloadUtility.getErrorMessageForReturnCode(this.context, returnCode, "");
        if (serverStatusListener != null) {
        	serverStatusListener.statusRequestFinished(this.updateAction, returnCode, returnMessage);
        } else {
            Intent intent = new Intent(Constants.ACTION_SERVER_STATUS_UPDATED);
            intent.putExtra(
                    Constants.ACTION_SERVER_STATUS_UPDATED_ATTR.INT_RETURN_CODE, returnCode);
            intent.putExtra(
                    Constants.ACTION_SERVER_STATUS_UPDATED_ATTR.STRING_RETURN_MESSAGE, returnMessage);
            LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
        }
    }

    @Override protected void onCancelled(Integer empty) {
        int returnCode = Constants.ID.CANCELLED;
        String returnMessage = DownloadUtility.getErrorMessageForReturnCode(this.context, returnCode, "");
        if (serverStatusListener != null) {
        	serverStatusListener.statusRequestFinished(this.updateAction, returnCode, returnMessage);
        } else {
            Intent intent = new Intent(Constants.ACTION_SERVER_STATUS_UPDATED);
            intent.putExtra(
                    Constants.ACTION_SERVER_STATUS_UPDATED_ATTR.INT_RETURN_CODE, returnCode);
            intent.putExtra(
                    Constants.ACTION_SERVER_STATUS_UPDATED_ATTR.STRING_RETURN_MESSAGE, returnMessage);
            LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
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
