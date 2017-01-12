package org.walkersguide.android.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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
            Context context, String serverURL, JSONObject parameters)
        throws CertificateException, IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException {

        // prepare connection object
        URL url = new URL(serverURL);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json, application/gzip");

        // load self signed certificates of the following two domains
        if (serverURL.startsWith("https://wasserbett.ath.cx")
                || serverURL.startsWith("https://walkersguide.org")) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput;
            // Create a KeyStore containing our trusted CAs
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            // load certificate of wasserbett.ath.cx
            caInput = context.getResources().openRawResource(R.raw.wasserbett);
            try {
                keyStore.setCertificateEntry("ca1", cf.generateCertificate(caInput));
            } finally {
                caInput.close();
            }
            // load certificate of walkersguide.org
            caInput = context.getResources().openRawResource(R.raw.walkersguide);
            try {
                keyStore.setCertificateEntry("ca2", cf.generateCertificate(caInput));
            } finally {
                caInput.close();
            }
            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);
            // Create an SSLContext that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
        }

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

    public static JSONObject processServerResponse(
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
        return new JSONObject(sb.toString());
    }

    public static String getErrorMessageForReturnCode(Context context, int returnCode, String additionalMessage) {
        switch (returnCode) {
            case 200:
                return "";
            case 1000:      // canceled
                return context.getResources().getString(R.string.messageError1000);
            case 1001:      // poi profile not found
                return context.getResources().getString(R.string.messageError1001);
            case 1002:      // no poi categories selected
                return context.getResources().getString(R.string.messageError1002);
            case 1003:      // no internet connection
                return context.getResources().getString(R.string.messageError1003);
            case 1004:      // no location found
                return context.getResources().getString(R.string.messageError1004);
            case 1005:      // no direction found
                return context.getResources().getString(R.string.messageError1005);
            case 1010:      // no connection to server
                return context.getResources().getString(R.string.messageError1010);
            case 1011:      // input output error
                return context.getResources().getString(R.string.messageError1011);
            case 1012:      // error message from server
                return String.format(
                        context.getResources().getString(R.string.messageError1012), additionalMessage);
            default:
                return String.format(
                        context.getResources().getString(R.string.messageUnknownError), returnCode);
        }
    }

}
