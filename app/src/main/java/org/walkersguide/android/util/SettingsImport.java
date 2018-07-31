package org.walkersguide.android.util;

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
import org.walkersguide.android.listener.SettingsImportListener;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import java.io.FileInputStream;
import java.io.File;
import java.io.InputStream;
import org.walkersguide.android.database.SQLiteHelper;
import java.io.OutputStream;
import android.database.SQLException;
import org.walkersguide.android.R;
import java.io.FileOutputStream;


public class SettingsImport extends AsyncTask<Void, Void, Integer> {

    private Context context;
    private SettingsImportListener settingsImportListener;
    private File databaseFileToImport;

    public SettingsImport(Context context, SettingsImportListener settingsImportListener, File databaseFileToImport) {
        this.context = context;
        this.settingsImportListener = settingsImportListener;
        this.databaseFileToImport = databaseFileToImport;
    }

    @Override protected Integer doInBackground(Void... params) {
        // open database file
        InputStream in = null;
        try {
            in = new FileInputStream(this.databaseFileToImport);
        } catch(IOException e) {
            return 1040;
        }

        // create temp database file
        File oldDatabaseFile = context.getDatabasePath(SQLiteHelper.INTERNAL_DATABASE_NAME);
        File tempDatabaseFile = context.getDatabasePath(SQLiteHelper.INTERNAL_TEMP_DATABASE_NAME);
        if (tempDatabaseFile.exists()) {
            tempDatabaseFile.delete();
        } else {
            tempDatabaseFile.getParentFile().mkdirs();
        }

        // copy
        int returnCode = Constants.ID.OK;
        OutputStream out = null;
        byte[] buffer = new byte[1024];
        int length;
        try {
            out = new FileOutputStream(tempDatabaseFile);
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
                if (isCancelled()) {
                    returnCode = 1000;
                    break;
                }
            }
        } catch(IOException e) {
            returnCode = 1040;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {}
            }
        }

        if (returnCode == Constants.ID.OK) {
            // replace database
            if (oldDatabaseFile.exists()) {
                oldDatabaseFile.delete();
            }
            tempDatabaseFile.renameTo(
                    context.getDatabasePath(SQLiteHelper.INTERNAL_DATABASE_NAME));
            // re-open database
            try {
                AccessDatabase.getInstance(this.context).reOpen();
            } catch (SQLException e) {
                returnCode = 1040;
            }
        } else {
            // remove temp file
            if (tempDatabaseFile.exists()) {
                tempDatabaseFile.delete();
            }
        }

        return returnCode;
    }

    @Override protected void onPostExecute(Integer returnCode) {
        System.out.println("xxx settings imported: " + returnCode);
        String returnMessage = context.getResources().getString(R.string.labelImportDatabaseSuccessful);
        if (returnCode != Constants.ID.OK) {
            returnMessage = DownloadUtility.getErrorMessageForReturnCode(this.context, returnCode, "");
        }
        if (settingsImportListener != null) {
        	settingsImportListener.settingsImportFinished(returnCode, returnMessage);
        }
    }

    @Override protected void onCancelled(Integer empty) {
        int returnCode = Constants.ID.CANCELLED;
        String returnMessage = DownloadUtility.getErrorMessageForReturnCode(this.context, returnCode, "");
        if (settingsImportListener != null) {
        	settingsImportListener.settingsImportFinished(returnCode, returnMessage);
        }
    }

    public void cancel() {
        this.cancel(true);
    }

}
