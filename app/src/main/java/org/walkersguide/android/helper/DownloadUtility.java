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
import org.walkersguide.android.BuildConfig;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import org.json.JSONArray;
import android.os.Build;


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
            case 200:
                return "";
            case 1000:      // canceled
                return context.getResources().getString(R.string.messageError1000);
            case 1003:      // no internet connection
                return context.getResources().getString(R.string.messageError1003);
            case 1004:      // no location found
                return context.getResources().getString(R.string.messageError1004);
            case 1005:      // no direction found
                return context.getResources().getString(R.string.messageError1005);
            case 1006:      // no map selected
                return context.getResources().getString(R.string.messageError1006);
            case 1007:      // app outdated
                return context.getResources().getString(R.string.messageError1007);
            // server
            case 1010:      // no connection to server
                return context.getResources().getString(R.string.messageError1010);
            case 1011:      // input output error
                return context.getResources().getString(R.string.messageError1011);
            case 1012:      // error message from server
                return String.format(
                        context.getResources().getString(R.string.messageError1012), additionalMessage);
            case 1013:      // no coordinates for address
                return context.getResources().getString(R.string.messageError1013);
            case 1014:      // no address for coordinates
                return context.getResources().getString(R.string.messageError1014);
            case 1015:      // OVER_QUERY_LIMIT
                return context.getResources().getString(R.string.messageError1015);
            case 1016:      // unsupported address provider
                return context.getResources().getString(R.string.messageError1016);
            case 1017:      // neither address nor coordinates to resolve
                return context.getResources().getString(R.string.messageError1017);
            case 1018:      // server input error
                return context.getResources().getString(R.string.messageError1018);
            // route
            case 1020:      // no start point
                return context.getResources().getString(R.string.messageError1020);
            case 1021:      // no destination point
                return context.getResources().getString(R.string.messageError1021);
            case 1022:      // no route
                return context.getResources().getString(R.string.messageError1022);
            case 1023:      // route parsing error
                return context.getResources().getString(R.string.messageError1023);
            // favorites and poi
            case 1030:      // no favorites or poi search term
                return context.getResources().getString(R.string.messageError1030);
            case 1031:      // no favorites profile selected
                return context.getResources().getString(R.string.messageError1031);
            case 1032:      // no poi profile selected
                return context.getResources().getString(R.string.messageError1032);
            case 1033:      // no poi category selected
                return context.getResources().getString(R.string.messageError1033);
            case 1034:      // unsupported poi request action
                return context.getResources().getString(R.string.messageError1034);
            case 1040:      // database import failed
                return context.getResources().getString(R.string.messageError1040);
            default:
                return String.format(
                        context.getResources().getString(R.string.messageUnknownError), returnCode);
        }
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
