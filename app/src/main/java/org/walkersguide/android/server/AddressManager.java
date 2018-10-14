package org.walkersguide.android.server;

import android.content.Context;

import android.location.Location;

import android.os.AsyncTask;
import android.os.Handler;

import android.text.TextUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.HistoryPointProfile;
import org.walkersguide.android.exception.ServerCommunicationException;
import org.walkersguide.android.helper.DownloadUtility;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.AddressListener;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;


public class AddressManager extends AsyncTask<Void, Void, PointWrapper> {

    // address providers
    // google maps
    private static final String GOOGLE_MAPS_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    // osm: https://wiki.openstreetmap.org/wiki/Nominatim
    private static final String OSM_URL = "https://nominatim.openstreetmap.org";

    private Context context;
    private AddressListener addressListener;
    private SettingsManager settingsManagerInstance;
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
        this.settingsManagerInstance = SettingsManager.getInstance(context);
        this.address = address;
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.connection = null;
        this.returnCode = Constants.RC.OK;
        this.additionalErrorMessage = null;
        this.cancelConnectionHandler = new Handler();
        this.cancelConnection = new CancelConnection();
    }

    public AddressManager(Context context, AddressListener addressListener, double latitude, double longitude) {
        this.context = context;
        this.addressListener = addressListener;
        this.settingsManagerInstance = SettingsManager.getInstance(context);
        this.address = "";
        this.latitude = latitude;
        this.longitude = longitude;
        this.connection = null;
        this.returnCode = Constants.RC.OK;
        this.additionalErrorMessage = null;
        this.cancelConnectionHandler = new Handler();
        this.cancelConnection = new CancelConnection();
    }

    @Override protected PointWrapper doInBackground(Void... params) {
        AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(context);

        // first look into local database
        HistoryPointProfile historyPointProfile = null;
        try {
            historyPointProfile = new HistoryPointProfile(
                    context,
                    HistoryPointProfile.ID_ADDRESS_POINTS,
                    accessDatabaseInstance.getJSONFavoritePointListOfProfile(HistoryPointProfile.ID_ADDRESS_POINTS));
        } catch (JSONException e) {
            historyPointProfile = null;
        } finally {
            if (historyPointProfile != null) {
                TreeMap<Float,PointWrapper> distancesToFavoritesMap = new TreeMap<Float,PointWrapper>();
                for (PointWrapper address : historyPointProfile.getPointProfileObjectList()) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            latitude, longitude,
                            address.getPoint().getLatitude(), address.getPoint().getLongitude(),
                            results);
                    distancesToFavoritesMap.put(results[0], address);
                }
                Map.Entry<Float,PointWrapper> closestAddress = distancesToFavoritesMap.firstEntry();
                if (closestAddress != null
                        && closestAddress.getKey() < 25.0) {       // PositionManager.THRESHOLD3.DISTANCE) {
                    return closestAddress.getValue();
                }
            }
        }

        // otherwise query address provider
        JSONObject jsonStreetAddress = null;
        // check internet connection
        if (! DownloadUtility.isInternetAvailable(context)) {
            returnCode = Constants.RC.NO_INTERNET_CONNECTION;
            return null;
        }

        // got coordinates, require address
        if (latitude != 0.0 && longitude != 0.0) {
            try {
                if (settingsManagerInstance.getServerSettings().getSelectedAddressProvider().getId().equals(Constants.ADDRESS_PROVIDER.OSM)) {
                    jsonStreetAddress = requestAddressFromOpenStreetMap(
                            context,
                            String.format(
                                Locale.ROOT,
                                "%1$s/reverse?format=jsonv2&lat=%2$f&lon=%3$f&accept-language=%4$s&addressdetails=1&zoom=18",
                                OSM_URL,
                                latitude,
                                longitude,
                                Locale.getDefault().getLanguage())
                            );
                } else if (settingsManagerInstance.getServerSettings().getSelectedAddressProvider().getId().equals(Constants.ADDRESS_PROVIDER.GOOGLE)) {
                    jsonStreetAddress = requestAddressFromGoogle(
                            context,
                            String.format(
                                Locale.ROOT,
                                "%1$s?latlng=%2$f,%3$f&language=%4$s",
                                GOOGLE_MAPS_URL,
                                latitude,
                                longitude,
                                Locale.getDefault().getLanguage())
                            );
                } else {
                    this.returnCode = Constants.RC.ADDRESS_PROVIDER_NOT_SUPPORTED;
                    return null;
                }
            } catch (ServerCommunicationException e) {
                this.returnCode = e.getReturnCode();
                return null;
            }
            try {
                // check for accuracy of address
                float[] results = new float[1];
                Location.distanceBetween(
                        latitude, longitude, jsonStreetAddress.getDouble("lat"), jsonStreetAddress.getDouble("lon"), results);
                if (results[0] > PositionManager.THRESHOLD3.DISTANCE) {
                    // if the address differs for more than 100 meters, don't take it
                    this.returnCode = Constants.RC.NO_ADDRESS_FOR_COORDINATES;
                    return null;
                }
            } catch (JSONException e) {
                this.returnCode = Constants.RC.SERVER_RESPONSE_ERROR;
                return null;
            }

        // got address, require coordinates
        } else if (! address.equals("")) {
            String encodedAddress = null;
            try {
                encodedAddress = URLEncoder.encode(address, "UTF-8");
            } catch (UnsupportedEncodingException e ) {
                this.returnCode = Constants.RC.USER_INPUT_ERROR;
                return null;
            }
            try {
                if (settingsManagerInstance.getServerSettings().getSelectedAddressProvider().getId().equals(Constants.ADDRESS_PROVIDER.OSM)) {
                    jsonStreetAddress = requestCoordinatesFromOpenStreetMap(
                            context,
                            String.format(
                                Locale.ROOT,
                                "%1$s/search?format=jsonv2&q=%2$s&accept-language=%3$s&addressdetails=1&limit=1",
                                OSM_URL,
                                encodedAddress,
                                Locale.getDefault().getLanguage())
                            );
                } else if (settingsManagerInstance.getServerSettings().getSelectedAddressProvider().getId().equals(Constants.ADDRESS_PROVIDER.GOOGLE)) {
                    jsonStreetAddress = requestCoordinatesFromGoogle(
                            context,
                            String.format(
                                Locale.ROOT,
                                "%1$s?address=%2$s&language=%3$s",
                                GOOGLE_MAPS_URL,
                                encodedAddress,
                                Locale.getDefault().getLanguage())
                            );
                } else {
                    this.returnCode = Constants.RC.ADDRESS_PROVIDER_NOT_SUPPORTED;
                }
            } catch (ServerCommunicationException e) {
                this.returnCode = e.getReturnCode();
                return null;
            }

        // got neither coordinates nor address
        } else {
            this.returnCode = Constants.RC.NEITHER_COORDINATES_NOR_ADDRESS;
        }

        PointWrapper addressPoint = null;
            try {
                addressPoint = new PointWrapper(context, jsonStreetAddress);
            } catch (JSONException e) {
                addressPoint = null;
                returnCode = Constants.RC.SERVER_RESPONSE_ERROR;
            } finally {
                if (addressPoint != null) {
                    // add to database
                    accessDatabaseInstance.addFavoritePointToProfile(addressPoint, HistoryPointProfile.ID_ADDRESS_POINTS);
                }
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
                    Constants.RC.CANCELLED,
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


    /**
     * coordinates -> address
     */

    /** Google **/

    private JSONObject requestAddressFromGoogle(Context context, String query) throws ServerCommunicationException {
        JSONObject jsonStreetAddress = null;
        try {
            // create connection
            connection = DownloadUtility.getHttpsURLConnectionObject(context, query, null);
            cancelConnectionHandler.postDelayed(cancelConnection, 100);
            connection.connect();
            int responseCode = connection.getResponseCode();
            cancelConnectionHandler.removeCallbacks(cancelConnection);
            if (isCancelled()) {
                throw new ServerCommunicationException(context, Constants.RC.CANCELLED);
            } else if (responseCode != Constants.RC.OK) {
                throw new ServerCommunicationException(context, Constants.RC.SERVER_CONNECTION_ERROR);
            } else {
                JSONObject jsonServerResponse = DownloadUtility.processServerResponseAsJSONObject(connection);
                if (! jsonServerResponse.has("status")) {
                    throw new ServerCommunicationException(context, Constants.RC.SERVER_CONNECTION_ERROR);
                } else if (! jsonServerResponse.getString("status").equals("OK")) {
                    if (jsonServerResponse.getString("status").equals("OVER_QUERY_LIMIT")) {
                        throw new ServerCommunicationException(context, Constants.RC.GOOGLE_MAPS_QUOTA_EXCEEDED);
                    } else {
                        throw new ServerCommunicationException(context, Constants.RC.SERVER_CONNECTION_ERROR);
                    }
                } else if (jsonServerResponse.getJSONArray("results").length() == 0) {
                    // no address found
                    throw new ServerCommunicationException(context, Constants.RC.NO_ADDRESS_FOR_COORDINATES);
                } else {
                    // get the first address object returned from google
                    jsonStreetAddress = createJsonStreetAddressFromGoogle(
                            context, jsonServerResponse.getJSONArray("results").getJSONObject(0));
                }
            }
        } catch (IOException e) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_CONNECTION_ERROR);
        } catch (JSONException e) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_RESPONSE_ERROR);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return jsonStreetAddress;
    }

    private JSONObject requestCoordinatesFromGoogle(Context context, String query) throws ServerCommunicationException {
        JSONObject jsonStreetAddress = null;
        try {
            // create connection
            connection = DownloadUtility.getHttpsURLConnectionObject(context, query, null);
            cancelConnectionHandler.postDelayed(cancelConnection, 100);
            connection.connect();
            int responseCode = connection.getResponseCode();
            cancelConnectionHandler.removeCallbacks(cancelConnection);
            if (isCancelled()) {
                throw new ServerCommunicationException(context, Constants.RC.CANCELLED);
            } else if (responseCode != Constants.RC.OK) {
                throw new ServerCommunicationException(context, Constants.RC.SERVER_CONNECTION_ERROR);
            } else {
                JSONObject jsonServerResponse = DownloadUtility.processServerResponseAsJSONObject(connection);
                if (! jsonServerResponse.has("status")) {
                    throw new ServerCommunicationException(context, Constants.RC.SERVER_CONNECTION_ERROR);
                } else if (! jsonServerResponse.getString("status").equals("OK")) {
                    if (jsonServerResponse.getString("status").equals("OVER_QUERY_LIMIT")) {
                        throw new ServerCommunicationException(context, Constants.RC.GOOGLE_MAPS_QUOTA_EXCEEDED);
                    } else {
                        throw new ServerCommunicationException(context, Constants.RC.SERVER_CONNECTION_ERROR);
                    }
                } else if (jsonServerResponse.getJSONArray("results").length() == 0) {
                    // no coordinates found
                    throw new ServerCommunicationException(context, Constants.RC.NO_COORDINATES_FOR_ADDRESS);
                } else {
                    jsonStreetAddress = createJsonStreetAddressFromGoogle(
                            context, jsonServerResponse.getJSONArray("results").getJSONObject(0));
                }
            }
        } catch (IOException e) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_CONNECTION_ERROR);
        } catch (JSONException e) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_RESPONSE_ERROR);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return jsonStreetAddress;
    }

    private JSONObject createJsonStreetAddressFromGoogle(
            Context context, JSONObject jsonAddressData) throws JSONException {
        // create street address in json format
        JSONObject jsonStreetAddress = new JSONObject();
        // display name
        jsonStreetAddress.put("name", jsonAddressData.getString("formatted_address"));
        jsonStreetAddress.put("display_name", jsonAddressData.getString("formatted_address"));
        // type and subtype
        jsonStreetAddress.put("type", Constants.POINT.STREET_ADDRESS);
        jsonStreetAddress.put("sub_type", StringUtility.formatPointType(context, Constants.POINT.STREET_ADDRESS));
        // cordinates
        JSONObject jsonCoordinates = jsonAddressData.getJSONObject("geometry").getJSONObject("location");
        jsonStreetAddress.put("lat", jsonCoordinates.getDouble("lat"));
        jsonStreetAddress.put("lon", jsonCoordinates.getDouble("lng"));
        // address components
        JSONArray jsonAddressComponentList = jsonAddressData.getJSONArray("address_components");
        for (int i=0; i<jsonAddressComponentList.length(); i++) {
            GoogleAddressComponent googleAddressComponent = new GoogleAddressComponent(jsonAddressComponentList.getJSONObject(i));
            // house number
            if (googleAddressComponent.getTypeList().contains("street_number")) {
                jsonStreetAddress.put("house_number", googleAddressComponent.getLongName());
            } else if (googleAddressComponent.getTypeList().contains("route")) {
                jsonStreetAddress.put("road", googleAddressComponent.getLongName());
            } else if (googleAddressComponent.getTypeList().contains("sublocality_level_3")) {
                jsonStreetAddress.put("residential", googleAddressComponent.getLongName());
            } else if (googleAddressComponent.getTypeList().contains("sublocality_level_2")) {
                jsonStreetAddress.put("suburb", googleAddressComponent.getLongName());
            } else if (googleAddressComponent.getTypeList().contains("sublocality_level_1")) {
                jsonStreetAddress.put("city_district", googleAddressComponent.getLongName());
            } else if (googleAddressComponent.getTypeList().contains("postal_code")) {
                jsonStreetAddress.put("postcode", googleAddressComponent.getLongName());
            } else if (googleAddressComponent.getTypeList().contains("locality")) {
                jsonStreetAddress.put("city", googleAddressComponent.getLongName());
            } else if (googleAddressComponent.getTypeList().contains("administrative_area_level_1")) {
                jsonStreetAddress.put("state", googleAddressComponent.getLongName());
            } else if (googleAddressComponent.getTypeList().contains("country")) {
                jsonStreetAddress.put("country", googleAddressComponent.getLongName());
            } else if (googleAddressComponent.getTypeList().contains("country")) {
                jsonStreetAddress.put("country_code", googleAddressComponent.getShortName().toLowerCase());
            }
        }
        return jsonStreetAddress;
    }

    private class GoogleAddressComponent {
        private String longName, shortName;
        private ArrayList<String> typeList;
        // constructor
        public GoogleAddressComponent(JSONObject jsonAddressComponent) throws JSONException {
            // names
            this.longName = jsonAddressComponent.getString("long_name");
            this.shortName = jsonAddressComponent.getString("short_name");
            // type list
            this.typeList = new ArrayList<String>();
            JSONArray jsonAddressTypeList = jsonAddressComponent.getJSONArray("types");
            for (int i=0; i<jsonAddressTypeList.length(); i++) {
                this.typeList.add(jsonAddressTypeList.getString(i));
            }
        }
        // getters
        public String getLongName() {
            return this.longName;
        }
        public String getShortName() {
            return this.shortName;
        }
        public ArrayList<String> getTypeList() {
            return this.typeList;
        }
    }


    /** OSM **/

    private JSONObject requestAddressFromOpenStreetMap(Context context, String queryURL) throws ServerCommunicationException {
        JSONObject jsonStreetAddress = null;
        try {
            // create connection
            connection = DownloadUtility.getHttpsURLConnectionObject(
                    context, queryURL, null);
            cancelConnectionHandler.postDelayed(cancelConnection, 100);
            connection.connect();
            int responseCode = connection.getResponseCode();
            cancelConnectionHandler.removeCallbacks(cancelConnection);
            if (isCancelled()) {
                throw new ServerCommunicationException(context, Constants.RC.CANCELLED);
            } else if (responseCode != Constants.RC.OK) {
                throw new ServerCommunicationException(context, Constants.RC.SERVER_CONNECTION_ERROR);
            } else {
                JSONObject jsonServerResponse = DownloadUtility.processServerResponseAsJSONObject(connection);
                // create street address in json format
                jsonStreetAddress = createJsonStreetAddressFromOSM(context, jsonServerResponse);
            }
        } catch (IOException e) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_CONNECTION_ERROR);
        } catch (JSONException e) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_RESPONSE_ERROR);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return jsonStreetAddress;
    }


    private JSONObject requestCoordinatesFromOpenStreetMap(Context context, String queryURL) throws ServerCommunicationException {
        JSONObject jsonStreetAddress = null;
        try {
            // create connection
            connection = DownloadUtility.getHttpsURLConnectionObject(
                    context, queryURL, null);
            cancelConnectionHandler.postDelayed(cancelConnection, 100);
            connection.connect();
            int responseCode = connection.getResponseCode();
            cancelConnectionHandler.removeCallbacks(cancelConnection);
            if (isCancelled()) {
                throw new ServerCommunicationException(context, Constants.RC.CANCELLED);
            } else if (responseCode != Constants.RC.OK) {
                throw new ServerCommunicationException(context, Constants.RC.SERVER_CONNECTION_ERROR);
            } else {
                JSONArray jsonServerResponse = DownloadUtility.processServerResponseAsJSONArray(connection);
                if (jsonServerResponse.length() == 0) {
                    // no coordinates
                    throw new ServerCommunicationException(context, Constants.RC.NO_COORDINATES_FOR_ADDRESS);
                } else {
                    // create street address in json format
                    jsonStreetAddress = createJsonStreetAddressFromOSM(context, jsonServerResponse.getJSONObject(0));
                }
            }
        } catch (IOException e) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_CONNECTION_ERROR);
        } catch (JSONException e) {
            throw new ServerCommunicationException(context, Constants.RC.SERVER_RESPONSE_ERROR);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return jsonStreetAddress;
    }

    private JSONObject createJsonStreetAddressFromOSM(
            Context context, JSONObject jsonAddressData) throws JSONException {
        // create street address in json format
        JSONObject jsonStreetAddress = new JSONObject();
        // name
        jsonStreetAddress.put("name", jsonAddressData.getString("display_name"));
        jsonStreetAddress.put("display_name", jsonAddressData.getString("display_name"));
        // type and subtype
        jsonStreetAddress.put("type", Constants.POINT.STREET_ADDRESS);
        jsonStreetAddress.put("sub_type", StringUtility.formatPointType(context, Constants.POINT.STREET_ADDRESS));
        // coordinates
        jsonStreetAddress.put("lat", jsonAddressData.getDouble("lat"));
        jsonStreetAddress.put("lon", jsonAddressData.getDouble("lon"));
        // address sub object
        JSONObject jsonAddressComponentList = jsonAddressData.getJSONObject("address");
        Iterator<String> keysIterator = jsonAddressComponentList.keys();
        while (keysIterator.hasNext()) {
            String type = (String)keysIterator.next();
            if (type.equals("house_number")) {
                jsonStreetAddress.put("house_number", jsonAddressComponentList.getString(type));
            } else if (type.equals("road")) {
                jsonStreetAddress.put("road", jsonAddressComponentList.getString(type));
            } else if (type.equals("residential")) {
                jsonStreetAddress.put("residential", jsonAddressComponentList.getString(type));
            } else if (type.equals("suburb")) {
                jsonStreetAddress.put("suburb", jsonAddressComponentList.getString(type));
            } else if (type.equals("city_district")) {
                jsonStreetAddress.put("city_district", jsonAddressComponentList.getString(type));
            } else if (type.equals("postcode")) {
                jsonStreetAddress.put("postcode", jsonAddressComponentList.getString(type));
            } else if (type.equals("city")) {
                jsonStreetAddress.put("city", jsonAddressComponentList.getString(type));
            } else if (type.equals("state")) {
                jsonStreetAddress.put("state", jsonAddressComponentList.getString(type));
            } else if (type.equals("country")) {
                jsonStreetAddress.put("country", jsonAddressComponentList.getString(type));
            } else if (type.equals("country_code")) {
                jsonStreetAddress.put("country_code", jsonAddressComponentList.getString(type));
            }
        }
        // extra name, not always there
        if (jsonAddressData.has("name")) {
            String extraName = jsonAddressData.getString("name");
            if (! TextUtils.isEmpty(extraName)
                    && ! extraName.equals("null")) {
                jsonStreetAddress.put("extra_name", extraName);
                    }
        }
        return jsonStreetAddress;
    }

}
