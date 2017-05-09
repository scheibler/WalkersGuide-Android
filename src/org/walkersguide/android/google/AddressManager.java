package org.walkersguide.android.google;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.PointWrapper;
import org.walkersguide.android.data.poi.FavoritesProfile;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.DownloadUtility;
import org.walkersguide.android.listener.AddressListener;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

public class AddressManager extends AsyncTask<Void, Void, Integer> {

    // actions
    public static final int ACTION_REQUEST_ADDRESS = 3810;
    public static final int ACTION_REQUEST_COORDINATES = 3811;

    private Context context;
    private HttpsURLConnection connection;
    private AddressListener addressListener;
    private int addressRequestAction, pointPutInto;
    private String address, additionalErrorMessage;
    private Handler cancelConnectionHandler;
    private CancelConnection cancelConnection;

    public AddressManager(Context context, AddressListener addressListener, int pointPutInto, String address) {
        this.context = context;
        this.addressListener = addressListener;
        this.addressRequestAction = ACTION_REQUEST_COORDINATES;
        this.pointPutInto = pointPutInto;
        this.address = address;
        this.connection = null;
        this.additionalErrorMessage = null;
        this.cancelConnectionHandler = new Handler();
        this.cancelConnection = new CancelConnection();
    }

    @Override protected Integer doInBackground(Void... params) {
        PointWrapper addressObject = null;
        int returnCode = Constants.ID.OK;

        // internet
        if (! DownloadUtility.isInternetAvailable(context)) {
            return 1003;
        }

        if (addressRequestAction == ACTION_REQUEST_COORDINATES) {
            JSONArray jsonAddressList = null;
            try {
                // create connection
                connection = DownloadUtility.getHttpsURLConnectionObject(
                        context,
                        String.format(
                            "https://maps.googleapis.com/maps/api/geocode/json?address=%1$s&sensor=false&language=%2$s",
                            URLEncoder.encode(address, "UTF-8"),
                            Locale.getDefault().getLanguage()),
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
                    if (! jsonServerResponse.has("status")) {
                        returnCode = 1010;          // server connection error
                    } else if (! jsonServerResponse.getString("status").equals("OK")) {
                        this.additionalErrorMessage = jsonServerResponse.getString("status");
                        returnCode = 1012;          // error from server
                    } else {
                        // get address list
                        jsonAddressList = jsonServerResponse.getJSONArray("results");
                    }
                }
            } catch (CertificateException | IOException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException e) {
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
                if (jsonAddressList.length() == 0) {
                    returnCode = 1013;
                } else {
                    try {
                        // get formatted address
                        String formattedAddress = jsonAddressList.getJSONObject(0).getString("formatted_address");
                        // get lat and lon
                        JSONObject jsonAddressLocation = jsonAddressList.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                        double lat = jsonAddressLocation.getDouble("lat");
                        double lon = jsonAddressLocation.getDouble("lng");
                        // create adress object
                        JSONObject jsonAddress = new JSONObject();
                        jsonAddress.put("name", formattedAddress);
                        jsonAddress.put("type", Constants.POINT.STREET_ADDRESS);
                        jsonAddress.put("sub_type", context.getResources().getString(R.string.streetAddressPointName));
                        jsonAddress.put("lat", lat);
                        jsonAddress.put("lon", lon);
                        addressObject = new PointWrapper(this.context, jsonAddress);
                    } catch (JSONException e) {
                        addressObject = null;
                    }
                }
            }
        }

        if (addressObject != null) {
            // add to addresses profile
            AccessDatabase.getInstance(this.context).addPointToFavoritesProfile(
                    addressObject, FavoritesProfile.ID_ADDRESSES);
            // put into
            switch (pointPutInto) {
                case Constants.POINT_PUT_INTO.START:
                    break;
                case Constants.POINT_PUT_INTO.DESTINATION:
                    break;
                case Constants.POINT_PUT_INTO.SIMULATION:
                    PositionManager.getInstance(context).setSimulatedLocation(addressObject);
                    break;
                default:
                    break;
            }
        }

        return returnCode;
    }

    @Override protected void onPostExecute(Integer returnCode) {
        if (addressListener != null) {
            addressListener.addressRequestFinished(
                    returnCode,
                    DownloadUtility.getErrorMessageForReturnCode(
                        context, returnCode, this.additionalErrorMessage));
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
