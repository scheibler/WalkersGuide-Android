package org.walkersguide.android.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.BuildConfig;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import org.json.JSONArray;
import android.os.Build;
import java.util.ArrayList;
import android.text.TextUtils;


public class DownloadUtility {

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 120000;

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        return false;
    }

    public static HttpsURLConnection getHttpsURLConnectionObject(
        Context context, String serverURL, JSONObject parameters) throws IOException {
        // prepare connection object
        URL url = new URL(serverURL);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json, application/gzip");
        connection.setRequestProperty("User-agent", UserAgent(context));
        // load additional parameters via post method, if given
        if (parameters != null) {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            os.write(parameters.toString().getBytes("UTF-8"));
            os.close();
        }
        return connection;
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
        if (connection.getContentType().equals("application/gzip")) {
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

    public static String getErrorMessageForReturnCode(Context context, int returnCode, String additionalMessage) {
        switch (returnCode) {
            // general
            case Constants.RC.OK:
                return "";
            case Constants.RC.CANCELLED:
                return context.getResources().getString(R.string.errorCancelled);
            case Constants.RC.NO_INTERNET_CONNECTION:
                return context.getResources().getString(R.string.errorNoInternetConnection);
            case Constants.RC.NO_SERVER_URL:
                return context.getResources().getString(R.string.errorNoServerURL);
            case Constants.RC.API_CLIENT_OUTDATED:
                return context.getResources().getString(R.string.errorAPIClientOutdated);
            case Constants.RC.API_SERVER_OUTDATED:
                return context.getResources().getString(R.string.errorAPIServerOutdated);
            case Constants.RC.NO_LOCATION_FOUND:
                return context.getResources().getString(R.string.errorNoLocationFound);
            case Constants.RC.NO_DIRECTION_FOUND:
                return context.getResources().getString(R.string.errorNoDirectionFound);
            case Constants.RC.NO_MAP_SELECTED:
                return context.getResources().getString(R.string.errorNoMapSelected);
            case Constants.RC.USER_INPUT_ERROR:
                return context.getResources().getString(R.string.errorUserInput);
            // server
            case Constants.RC.SERVER_CONNECTION_ERROR:
                return context.getResources().getString(R.string.errorServerConnection);
            case Constants.RC.SERVER_RESPONSE_ERROR:
                return context.getResources().getString(R.string.errorServerResponse);
            case Constants.RC.SERVER_RESPONSE_ERROR_WITH_EXTRA_DATA:
                return String.format(
                        context.getResources().getString(R.string.errorServerResponseWithExtraData), additionalMessage);
            // addresses
            case Constants.RC.NO_COORDINATES_FOR_ADDRESS:
                return context.getResources().getString(R.string.errorNoCoorDinatesForAddress);
            case Constants.RC.NO_ADDRESS_FOR_COORDINATES:
                return context.getResources().getString(R.string.errorNoAddressForCoordinates);
            case Constants.RC.NEITHER_COORDINATES_NOR_ADDRESS:
                return context.getResources().getString(R.string.errorNeitherCoordinatesNorAddress);
            case Constants.RC.GOOGLE_MAPS_QUOTA_EXCEEDED:
                return context.getResources().getString(R.string.errorGoogleMapsQuotaExceeded);
            case Constants.RC.ADDRESS_PROVIDER_NOT_SUPPORTED:
                return context.getResources().getString(R.string.errorAddressProviderNotSupported);
            // poi and favorites
            case Constants.RC.NO_FAVORITES_PROFILE_SELECTED:
                return context.getResources().getString(R.string.errorNoFavoritesProfileSelected);
            case Constants.RC.NO_POI_PROFILE_SELECTED:
                return context.getResources().getString(R.string.errorNoPOIProfileSelected);
            case Constants.RC.NO_POI_CATEGORY_SELECTED:
                return context.getResources().getString(R.string.errorNoPOICategorySelected);
            case Constants.RC.UNSUPPORTED_POI_REQUEST_ACTION:
                return context.getResources().getString(R.string.errorUnsupportedPOIRequestAction);
            case Constants.RC.NO_SEARCH_TERM:
                return context.getResources().getString(R.string.errorNoSearchTerm);
            // route
            case Constants.RC.NO_ROUTE_START_POINT:
                return context.getResources().getString(R.string.errorNoRouteStartPoint);
            case Constants.RC.NO_ROUTE_DESTINATION_POINT:
                return context.getResources().getString(R.string.errorNoRouteDestinationPoint);
            case Constants.RC.NO_ROUTE_SELECTED:
                return context.getResources().getString(R.string.errorNoRouteSelected);
            case Constants.RC.ROUTE_PARSING_ERROR:
                return context.getResources().getString(R.string.errorRouteParsing);
            // settings
            case Constants.RC.DATABASE_IMPORT_FAILED:      // database import failed
                return context.getResources().getString(R.string.errorDatabaseImportFailed);
            default:
                return String.format(
                        context.getResources().getString(R.string.messageUnknownError), returnCode);
        }
    }

    public static String generateServerCommand(String serverURL, String command) {
        ArrayList<String> serverCommandPartList = new ArrayList<String>();
        // server url
        serverCommandPartList.add(serverURL);
        if (! serverURL.endsWith("/")) {
            serverCommandPartList.add("/");
        }
        // command
        serverCommandPartList.add(command);
        if (! command.endsWith("/")) {
            serverCommandPartList.add("/");
        }
        return TextUtils.join("", serverCommandPartList);
    }

    public static String UserAgent(Context context) {
        return String.format(
                "%1$s/%2$s (Android:%3$s; Contact:%4$s)",
                context.getResources().getString(R.string.app_name),
                BuildConfig.VERSION_NAME,
                android.os.Build.VERSION.RELEASE,
                BuildConfig.CONTACT_EMAIL);
    }

}
