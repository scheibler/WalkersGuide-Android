package org.walkersguide.android.helper;

import android.content.Context;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.os.Build;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.server.poi.PoiCategory;
import org.walkersguide.android.data.server.OSMMap;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.exception.ServerCommunicationException;
import org.walkersguide.android.R;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;


public class ServerUtility {
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 180000;


    /**
     * get server instance
     */

    public static ServerInstance getServerInstance(Context context, String serverURL) throws ServerCommunicationException {
        // check server url
        if (TextUtils.isEmpty(serverURL)) {
            throw new ServerCommunicationException(context, Constants.RC.NO_SERVER_URL);
        }

        // server instance from cache
        ServerInstance cachedServerInstance = ServerStatusManager.getInstance(context).getCachedServerInstance();
        if (cachedServerInstance != null
                && cachedServerInstance.getServerURL().equals(serverURL)) {
            return cachedServerInstance;
        }

        // create server param list
        JSONObject jsonServerParams = null;
        try {
            jsonServerParams = createServerParamList(context);
        } catch (JSONException e) {
            jsonServerParams = new JSONObject();
        }

        HttpsURLConnection connection = null;
        JSONObject jsonServerResponse = null;
        int returnCode = Constants.RC.OK;
        try {
            connection = ServerUtility.getHttpsURLConnectionObject(
                    context,
                    String.format(
                        "%1$s/%2$s", serverURL, Constants.SERVER_COMMAND.GET_STATUS),
                    jsonServerParams);
            connection.connect();
            returnCode = connection.getResponseCode();
            if (returnCode == Constants.RC.OK) {
                jsonServerResponse = processServerResponseAsJSONObject(connection);
            }
        } catch (IOException e) {
            returnCode = Constants.RC.CONNECTION_FAILED;
        } catch (JSONException e) {
            returnCode = Constants.RC.BAD_RESPONSE;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (returnCode != Constants.RC.OK) {
                throw new ServerCommunicationException(context, returnCode);
            }
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
            throw new ServerCommunicationException(context, Constants.RC.BAD_RESPONSE);
        } else {
            // check for server api version
            Collections.sort(supportedAPIVersionList);
            // check if app is outdated
            int minServerApiVersion = supportedAPIVersionList.get(0);
            if (BuildConfig.SUPPORTED_API_VERSION_LIST[BuildConfig.SUPPORTED_API_VERSION_LIST.length-1] < minServerApiVersion) {
                throw new ServerCommunicationException(context, Constants.RC.API_CLIENT_OUTDATED);
            }
            // check if server is outdated
            int maxServerApiVersion = supportedAPIVersionList.get(supportedAPIVersionList.size()-1);
            if (BuildConfig.SUPPORTED_API_VERSION_LIST[0] > maxServerApiVersion) {
                throw new ServerCommunicationException(context, Constants.RC.API_SERVER_OUTDATED);
            }
        }

        // server name and version
        String serverName;
        try {
            serverName = jsonServerResponse.getString("server_name");
        } catch (JSONException e) {
            throw new ServerCommunicationException(context, Constants.RC.BAD_RESPONSE);
        }
        String serverVersion;
        try {
            serverVersion = jsonServerResponse.getString("server_version");
        } catch (JSONException e) {
            throw new ServerCommunicationException(context, Constants.RC.BAD_RESPONSE);
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
                                    jsonMap.getLong("created")));
                    } catch (JSONException e) {}
                }
            }
        }
        if (availableMapList.isEmpty()) {
            throw new ServerCommunicationException(context, Constants.RC.NO_MAP_LIST);
        }

        ArrayList<PoiCategory> supportedPoiCategoryList = new ArrayList<PoiCategory>();
        JSONArray jsonPoiCategoryList = null;
        try {
            jsonPoiCategoryList = jsonServerResponse.getJSONArray("supported_poi_category_list");
        } catch (JSONException e) {
            jsonPoiCategoryList = null;
        } finally {
            if (jsonPoiCategoryList != null) {
                supportedPoiCategoryList = PoiCategory.listFromJson(jsonPoiCategoryList);
            }
        }
        if (supportedPoiCategoryList.isEmpty()) {
            throw new ServerCommunicationException(context, Constants.RC.BAD_RESPONSE);
        }

        // create server instance object
        ServerInstance serverInstance = new ServerInstance(
                serverName, serverURL, serverVersion, availableMapList,
                supportedPoiCategoryList, supportedAPIVersionList);

        // update server settings
        ServerSettings serverSettings = SettingsManager.getInstance().getServerSettings();
        if (! serverInstance.getServerURL().equals(serverSettings.getServerURL())) {
            serverSettings.setServerURL(serverInstance.getServerURL());
        }
        if (serverSettings.getSelectedMap() != null) {
            int indexOfSelectedMap = serverInstance.getAvailableMapList().indexOf(serverSettings.getSelectedMap());
            if (indexOfSelectedMap == -1) {
                // reset
                serverSettings.setSelectedMap(null);
            } else {
                // update
                serverSettings.setSelectedMap(serverInstance.getAvailableMapList().get(indexOfSelectedMap));
            }
        }

        // cache new server instance object and return
        ServerStatusManager.getInstance(context).setCachedServerInstance(serverInstance);
        return serverInstance;
    }


    /** helper functions
     */

    public static HttpsURLConnection getHttpsURLConnectionObject(Context context, String queryURL,
            JSONObject postParameters) throws IOException, ServerCommunicationException {
        // check for internet connection
        if (! isInternetAvailable()) {
            throw new ServerCommunicationException(context, Constants.RC.NO_INTERNET_CONNECTION);
        }

        // prepare connection object
        URL url = new URL(queryURL);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty(
                "User-agent",
                String.format(
                    "%1$s/%2$s (Android:%3$s; Contact:%4$s)",
                    context.getResources().getString(R.string.app_name),
                    BuildConfig.VERSION_NAME,
                    android.os.Build.VERSION.RELEASE,
                    BuildConfig.CONTACT_EMAIL)
                );

        // load additional parameters via post method, if given
        if (postParameters != null) {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            os.write(postParameters.toString().getBytes("UTF-8"));
            os.close();
        }
        return connection;
    }

    public static boolean isInternetAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager) GlobalInstance.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        return false;
    }

    public static JSONObject createServerParamList(Context context) throws JSONException {
        ServerSettings serverSettings = SettingsManager.getInstance().getServerSettings();
        JSONObject requestJson = new JSONObject();
        // session id and language
        requestJson.put("session_id", ((GlobalInstance) context.getApplicationContext()).getSessionId());
        requestJson.put("language", Locale.getDefault().getLanguage());
        // selected map id
        if (serverSettings.getSelectedMap() != null) {
            requestJson.put("map_id", serverSettings.getSelectedMap().getId());
        }
        return requestJson;
    }

    public static JSONArray processServerResponseAsJSONArray(
            HttpsURLConnection connection) throws IOException, JSONException {
        return new JSONArray(processServerResponseAsString(connection));
    }

    public static JSONObject processServerResponseAsJSONObject(
            HttpsURLConnection connection) throws IOException, JSONException {
        return new JSONObject(processServerResponseAsString(connection));
    }

    private static String processServerResponseAsString(
            HttpsURLConnection connection) throws IOException, JSONException {
        BufferedReader reader;
        StringBuilder sb = new StringBuilder();
        InputStream in = connection.getInputStream();
        if (
                   (   // new variant: cherrypy build-in gzip
                       connection.getContentEncoding() != null
                    && connection.getContentEncoding().equalsIgnoreCase("gzip"))
                || (   // legacy
                       connection.getContentType() != null
                    && connection.getContentType().equalsIgnoreCase("application/gzip"))
                ) {
            reader = new BufferedReader(new InputStreamReader(
                        new GZIPInputStream(in), "utf-8"), 8);
        } else {
            reader = new BufferedReader(new InputStreamReader(in, "utf-8"), 8);
        }
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        in.close();
        // convert to json
        return sb.toString();
    }


    /**
     * return codes
     */

    public static String getErrorMessageForReturnCode(Context context, int returnCode) {
        return getErrorMessageForReturnCode(returnCode);
    }

    public static String getErrorMessageForReturnCode(int returnCode) {
        switch (returnCode) {
            // walkersguide server errors
            //
            // caused by client
            case Constants.RC.OK:
                return "";
            case Constants.RC.BAD_REQUEST:
                return GlobalInstance.getStringResource(R.string.errorBadRequest);
            case Constants.RC.REQUEST_IN_PROGRESS:
                return GlobalInstance.getStringResource(R.string.errorRequestInProgress);
            // caused by server
            case Constants.RC.INTERNAL_SERVER_ERROR:
                return GlobalInstance.getStringResource(R.string.errorInternalServerError);
            case Constants.RC.BAD_GATEWAY:
                return GlobalInstance.getStringResource(R.string.errorBadGateway);
            case Constants.RC.SERVICE_UNAVAILABLE:
                return GlobalInstance.getStringResource(R.string.errorServiceUnavailableOrBusy);
            // walkersguide custom errors
            case Constants.RC.CANCELLED_BY_CLIENT:
                return GlobalInstance.getStringResource(R.string.errorCancelled);
            case Constants.RC.NO_POI_TAGS_SELECTED:
                return GlobalInstance.getStringResource(R.string.errorNoPOITagsSelected);
            case Constants.RC.MAP_LOADING_FAILED:
                return GlobalInstance.getStringResource(R.string.errorMapLoadingFailed);
            case Constants.RC.WRONG_MAP_SELECTED:
                return GlobalInstance.getStringResource(R.string.errorWrongMapSelected);
            case Constants.RC.MAP_OUTDATED:
                return GlobalInstance.getStringResource(R.string.errorMapOutdated);
            // route calculation
            case Constants.RC.START_OR_DESTINATION_MISSING:
                return GlobalInstance.getStringResource(R.string.errorStartOrDestinationMissing);
            case Constants.RC.START_AND_DESTINATION_TOO_FAR_AWAY:
                return GlobalInstance.getStringResource(R.string.errorStartAndDestinationTooFarAway);
            case Constants.RC.TOO_MANY_WAY_CLASSES_IGNORED:
                return GlobalInstance.getStringResource(R.string.errorTooManyWayClassesIgnored);
            case Constants.RC.NO_ROUTE_BETWEEN_START_AND_DESTINATION:
                return GlobalInstance.getStringResource(R.string.errorNoRouteBetweenStartAndDestination);

            // android app
            case Constants.RC.CANCELLED:
                return GlobalInstance.getStringResource(R.string.errorCancelled);
            case Constants.RC.NO_LOCATION_FOUND:
                return GlobalInstance.getStringResource(R.string.errorNoLocationFound);
            case Constants.RC.NO_DIRECTION_FOUND:
                return GlobalInstance.getStringResource(R.string.errorNoDirectionFound);
            // server
            case Constants.RC.CONNECTION_FAILED:
                return GlobalInstance.getStringResource(R.string.errorConnectionFailed);
            case Constants.RC.BAD_RESPONSE:
                return GlobalInstance.getStringResource(R.string.errorBadResponse);
            case Constants.RC.NO_SERVER_URL:
                return GlobalInstance.getStringResource(R.string.errorNoServerURL);
            case Constants.RC.NO_INTERNET_CONNECTION:
                return GlobalInstance.getStringResource(R.string.errorNoInternetConnection);
            case Constants.RC.API_CLIENT_OUTDATED:
                return GlobalInstance.getStringResource(R.string.errorAPIClientOutdated);
            case Constants.RC.API_SERVER_OUTDATED:
                return GlobalInstance.getStringResource(R.string.errorAPIServerOutdated);
            case Constants.RC.NO_MAP_LIST:
                return GlobalInstance.getStringResource(R.string.errorNoMapList);
            // addresses
            case Constants.RC.NO_COORDINATES_FOR_ADDRESS:
                return GlobalInstance.getStringResource(R.string.errorNoCoordinatesForAddress);
            case Constants.RC.NO_ADDRESS_FOR_COORDINATES:
                return GlobalInstance.getStringResource(R.string.errorNoAddressForCoordinates);
            case Constants.RC.NEITHER_COORDINATES_NOR_ADDRESS:
                return GlobalInstance.getStringResource(R.string.errorNeitherCoordinatesNorAddress);
            // poi
            case Constants.RC.NO_POI_PROFILE_CREATED:
                return GlobalInstance.getStringResource(R.string.errorNoPOIProfileCreated);
            case Constants.RC.NO_POI_PROFILE_SELECTED:
                return GlobalInstance.getStringResource(R.string.errorNoPOIProfileSelected);
            case Constants.RC.POI_PROFILE_PARSING_ERROR:
                return GlobalInstance.getStringResource(R.string.errorPOIProfileParsing);
            case Constants.RC.UNSUPPORTED_POI_REQUEST_ACTION:
                return GlobalInstance.getStringResource(R.string.errorUnsupportedPOIRequestAction);
            // route
            case Constants.RC.NO_ROUTE_CREATED:
                return GlobalInstance.getStringResource(R.string.errorNoRouteCreated);
            case Constants.RC.NO_ROUTE_SELECTED:
                return GlobalInstance.getStringResource(R.string.errorNoRouteSelected);
            case Constants.RC.ROUTE_PARSING_ERROR:
                return GlobalInstance.getStringResource(R.string.errorRouteParsing);
            default:
                return String.format(
                        GlobalInstance.getStringResource(R.string.messageUnknownError), returnCode);
        }
    }

}
