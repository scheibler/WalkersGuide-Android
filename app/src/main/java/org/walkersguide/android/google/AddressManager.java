package org.walkersguide.android.google;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.DownloadUtility;
import org.walkersguide.android.listener.AddressListener;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;

public class AddressManager extends AsyncTask<Void, Void, PointWrapper> {

    // google maps url
        private static final String GOOGLE_MAPS_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    private Context context;
    private AddressListener addressListener;
    private String address;
    private double latitude, longitude;
    private HttpsURLConnection connection;
    private int returnCode, pointPutInto;
    private String additionalErrorMessage;
    private Handler cancelConnectionHandler;
    private CancelConnection cancelConnection;

    public AddressManager(Context context, AddressListener addressListener, String address) {
        this.context = context;
        this.addressListener = addressListener;
        this.address = address;
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.connection = null;
        this.returnCode = Constants.ID.OK;
        this.additionalErrorMessage = null;
        this.cancelConnectionHandler = new Handler();
        this.cancelConnection = new CancelConnection();
    }

    public AddressManager(Context context, AddressListener addressListener, double latitude, double longitude) {
        this.context = context;
        this.addressListener = addressListener;
        this.address = "";
        this.latitude = latitude;
        this.longitude = longitude;
        this.connection = null;
        this.returnCode = Constants.ID.OK;
        this.additionalErrorMessage = null;
        this.cancelConnectionHandler = new Handler();
        this.cancelConnection = new CancelConnection();
    }

    @Override protected PointWrapper doInBackground(Void... params) {
        PointWrapper addressPoint = null;
        AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(context);

        if (latitude != 0.0 && longitude != 0.0) {
            // request address for coordinates
            PointWrapper addressPointFromDatabase = accessDatabaseInstance.getAddress(latitude, longitude);
            System.out.println("xxx address from database: " + addressPointFromDatabase);
            if (addressPointFromDatabase != null
                    && ! addressPointFromDatabase.equals(PositionManager.getDummyLocation(context))) {
                addressPoint = addressPointFromDatabase;
            } else {
                // internet
                if (! DownloadUtility.isInternetAvailable(context)) {
                    returnCode = 1003;
                } else {
                    String addressFromGoogle = "";
                    double latitudeFromGoogle = 0.0;
                    double longitudeFromGoogle = 0.0;
                    try {
                        // create connection
                        connection = DownloadUtility.getHttpsURLConnectionObject(
                                context,
                                String.format(
                                    Locale.ROOT,
                                    "%1$s?latlng=%2$f,%3$f&sensor=false&language=%4$s",
                                    GOOGLE_MAPS_URL,
                                    latitude,
                                    longitude,
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
                                if (jsonServerResponse.getString("status").equals("OVER_QUERY_LIMIT")) {
                                    returnCode = 1015;
                                } else {
                                    this.additionalErrorMessage = jsonServerResponse.getString("status");
                                    returnCode = 1012;          // error from server
                                }
                            } else {
                                // get the first address object returned from google
                                JSONObject jsonAddressData = jsonServerResponse.getJSONArray("results").getJSONObject(0);
                                addressFromGoogle = jsonAddressData.getString("formatted_address");
                                // corresponding location
                                JSONObject jsonCoordinates = jsonAddressData.getJSONObject("geometry").getJSONObject("location");
                                latitudeFromGoogle = jsonCoordinates.getDouble("lat");
                                longitudeFromGoogle = jsonCoordinates.getDouble("lng");
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
                    if (returnCode == Constants.ID.OK
                            && ! addressFromGoogle.equals("")
                            && latitudeFromGoogle != 0.0
                            && longitudeFromGoogle != 0.0) {
                        float[] results = new float[1];
                        Location.distanceBetween(
                                latitude, longitude, latitudeFromGoogle, longitudeFromGoogle, results);
                        if (results[0] > PositionManager.THRESHOLD3.DISTANCE) {
                            // if the address from google differs for more than 50 meters, don't take it
                            returnCode = 1014;
                            // add dummy location to database
                            accessDatabaseInstance.addAddress(
                                    latitude, longitude, PositionManager.getDummyLocation(context));
                        } else {
                            // create adress object
                            try {
                                JSONObject jsonAddress = new JSONObject();
                                jsonAddress.put("name", addressFromGoogle);
                                jsonAddress.put("type", Constants.POINT.STREET_ADDRESS);
                                jsonAddress.put("sub_type", context.getResources().getString(R.string.streetAddressPointName));
                                jsonAddress.put("lat", latitude);
                                jsonAddress.put("lon", longitude);
                                addressPoint = new PointWrapper(this.context, jsonAddress);
                            } catch (JSONException e) {
                                addressPoint = null;
                            }
                        }
                    }
                }
            }

        } else if (! address.equals("")) {
            // request coordinates for address
            // internet
            if (! DownloadUtility.isInternetAvailable(context)) {
                returnCode = 1003;
            } else {
                JSONArray jsonAddressList = null;
                try {
                    // create connection
                    connection = DownloadUtility.getHttpsURLConnectionObject(
                            context,
                            String.format(
                                "%1$s?address=%2$s&sensor=false&language=%3$s",
                                GOOGLE_MAPS_URL,
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
                            if (jsonServerResponse.getString("status").equals("OVER_QUERY_LIMIT")) {
                                returnCode = 1015;
                            } else {
                                this.additionalErrorMessage = jsonServerResponse.getString("status");
                                returnCode = 1012;          // error from server
                            }
                        } else {
                            // get address list
                            jsonAddressList = jsonServerResponse.getJSONArray("results");
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
                            addressPoint = new PointWrapper(this.context, jsonAddress);
                        } catch (JSONException e) {
                            addressPoint = null;
                        }
                    }
                }
            }
        }

        if (addressPoint != null) {
            // add to database
            accessDatabaseInstance.addAddress(
                    addressPoint.getPoint().getLatitude(),
                    addressPoint.getPoint().getLongitude(),
                    addressPoint);
        }
        return addressPoint;
    }

    @Override protected void onPostExecute(PointWrapper addressPoint) {
        if (addressListener != null) {
            addressListener.addressRequestFinished(
                    returnCode,
                    DownloadUtility.getErrorMessageForReturnCode(
                        context, returnCode, this.additionalErrorMessage),
                    addressPoint);
        }
    }

    @Override protected void onCancelled(PointWrapper addressPoint) {
        if (addressListener != null) {
            addressListener.addressRequestFinished(
                    Constants.ID.CANCELLED,
                    DownloadUtility.getErrorMessageForReturnCode(
                        context, returnCode, this.additionalErrorMessage),
                    addressPoint);
        }
    }

    public int getPointPutIntoVariable() {
        return this.pointPutInto;
    }

    public void setPointPutIntoVariable(int newPointPutInto) {
        this.pointPutInto = newPointPutInto;
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
