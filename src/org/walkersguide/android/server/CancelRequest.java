package org.walkersguide.android.server;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.helper.DownloadUtility;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

import android.content.Context;
import android.os.AsyncTask;

public class CancelRequest extends AsyncTask<Void, Void, Void> {

    private Context context;

    public CancelRequest(Context context) {
        this.context = context;
    }

    @Override protected Void doInBackground(Void... params) {
        ServerSettings serverSettings = SettingsManager.getInstance(context).getServerSettings();
        // send cancel request
        if (serverSettings.getSelectedMap() != null
                && DownloadUtility.isInternetAvailable(this.context)) {
            HttpsURLConnection connection = null;
            try {
                // create parameter list
                JSONObject requestJson = new JSONObject();
                requestJson.put("language", Locale.getDefault().getLanguage());
                requestJson.put("session_id", ((GlobalInstance) context.getApplicationContext()).getSessionId());
                // create connection
                connection = DownloadUtility.getHttpsURLConnectionObject(
                        this.context,
                        String.format("%1$s/cancel_request", serverSettings.getSelectedMap().getURL()),
                        requestJson);
                connection.connect();
                int responseCode = connection.getResponseCode();
            } catch (CertificateException | IOException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException e) {
            } catch (JSONException e) {
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        return null;
    }

}
