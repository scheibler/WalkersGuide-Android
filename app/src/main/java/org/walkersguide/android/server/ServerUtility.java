package org.walkersguide.android.server;

import android.content.Context;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.os.Build;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;

import java.net.URL;

import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import java.net.CookieManager;
import java.net.CookieHandler;
import timber.log.Timber;
import java.nio.charset.StandardCharsets;


public class ServerUtility {
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 180000;

    public static <T extends ServerException> JSONArray performRequestAndReturnJsonArray(
            String queryURL, JSONObject postParameters, Class<T> exceptionClass) throws T {
        try {
            return new JSONArray(
                    performRequestAndReturnString(
                        queryURL, postParameters, exceptionClass));
        } catch (JSONException e) {
            throw createException(exceptionClass, ServerException.RC_BAD_RESPONSE);
        }
    }

    public static <T extends ServerException> JSONObject performRequestAndReturnJsonObject(
            String queryURL, JSONObject postParameters, Class<T> exceptionClass) throws T {
        try {
            return new JSONObject(
                    performRequestAndReturnString(
                        queryURL, postParameters, exceptionClass));
        } catch (JSONException e) {
            throw createException(exceptionClass,ServerException.RC_BAD_RESPONSE);
        }
    }

    public static <T extends ServerException> String performRequestAndReturnString(
            String queryURL, JSONObject postParameters, Class<T> exceptionClass) throws T {
        HttpsURLConnection connection = null;

        // remove all cookies from cookie manager, if available
        // just a precaution
        CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
        if (cookieManager != null
                && cookieManager.getCookieStore() != null
                && cookieManager.getCookieStore().getCookies() != null
                && ! cookieManager.getCookieStore().getCookies().isEmpty()) {
            cookieManager.getCookieStore().removeAll();
        }

        // construct request
        try {
            URL url = new URL(queryURL);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty(
                    "User-agent",
                    String.format(
                        "%1$s/%2$s (Android:%3$s; Contact:%4$s)",
                        GlobalInstance.getStringResource(R.string.app_name),
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
        } catch (IOException e) {
            cleanUp(connection, null);
            throw createException(exceptionClass, ServerException.RC_BAD_REQUEST);
        }

        // connect
        if (! isInternetAvailable()) {
            throw createException(exceptionClass, ServerException.RC_NO_INTERNET_CONNECTION);
        }
        try {
            connection.connect();
            int returnCode = connection.getResponseCode();
            Timber.d("%1$d -- Request: %2$s", returnCode, queryURL);
            if (returnCode != HttpsURLConnection.HTTP_OK) {
                cleanUp(connection, null);
                throw createException(exceptionClass, returnCode);
            }
        } catch (IOException e) {
            Timber.e("Server connection failed: %1$s", e.getMessage());
            cleanUp(connection, null);
            throw createException(exceptionClass, ServerException.RC_REQUEST_FAILED);
        }

        // get response
        InputStream in = null;
        String response = null;
        try {
            in = getInputStream(connection);
            response = getServerResponse(in);
        } catch (IOException e) {
            Timber.e("Server response error: %1$s", e.getMessage());
            cleanUp(connection, in);
            throw createException(exceptionClass, ServerException.RC_BAD_RESPONSE);
        }

        cleanUp(connection, in);
        if (response == null) {
            throw createException(exceptionClass, ServerException.RC_BAD_RESPONSE);
        }
        return response;
    }


    // public helpers

    public static boolean isInternetAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager) GlobalInstance.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        return false;
    }


    // private helpers

    private static InputStream getInputStream(HttpsURLConnection connection) throws IOException {
        InputStream in = connection.getInputStream();
        if (
                   (   // new variant: cherrypy build-in gzip
                       connection.getContentEncoding() != null
                    && connection.getContentEncoding().equalsIgnoreCase("gzip"))
                || (   // default gzip mime type
                       connection.getContentType() != null
                    && connection.getContentType().equalsIgnoreCase("application/gzip"))
                ) {
            return new GZIPInputStream(in);
        } else {
            return in;
        }
    }

    private static String getServerResponse(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8), 8);
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        return sb.toString();
    }

    private static void cleanUp(HttpsURLConnection connection, InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {}
        }
        if (connection != null) {
            connection.disconnect();
        }
    }

    private static <T extends ServerException> T createException(Class<T> exceptionClass, int returnCode) {
        try {
            return (T) exceptionClass
                .getConstructor(new Class<?>[] { int.class })
                .newInstance(returnCode);
        } catch (Exception e) {}
        return null;
    }

}
