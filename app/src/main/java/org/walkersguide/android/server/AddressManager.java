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
import org.walkersguide.android.data.sensor.threshold.DistanceThreshold;
import org.walkersguide.android.exception.ServerCommunicationException;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;


public class AddressManager extends AsyncTask<Void, Void, PointWrapper> {

    public interface AddressListener {
        public void addressRequestFinished(Context context, int returnCode, PointWrapper addressPoint);
    }

    // address providers
    // google maps
    private static final String GOOGLE_MAPS_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    // osm: https://wiki.openstreetmap.org/wiki/Nominatim
    private static final String OSM_URL = "https://nominatim.openstreetmap.org";

    private Context context;
    private AddressListener addressListener;
    private SettingsManager settingsManagerInstance;
    private String address;
    private boolean nearbyCurrentLocation;
    private double latitude, longitude;
    private HttpsURLConnection connection;
    private int returnCode, pointPutInto;
    private Handler cancelConnectionHandler;
    private CancelConnection cancelConnection;

    public AddressManager(Context context, AddressListener addressListener, String address, boolean nearbyCurrentLocation) {
        this.context = context;
        this.addressListener = addressListener;
        this.settingsManagerInstance = SettingsManager.getInstance(context);
        this.address = address;
        this.nearbyCurrentLocation = nearbyCurrentLocation;
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.connection = null;
        this.returnCode = Constants.RC.OK;
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
                        && closestAddress.getKey() < DistanceThreshold.TWENTY_METERS.getDistanceThresholdInMeters()) {
                    return closestAddress.getValue();
                }
            }
        }

        // otherwise query address provider
        JSONObject jsonStreetAddress = null;

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
                }
            } catch (ServerCommunicationException e) {
                this.returnCode = e.getReturnCode();
            } finally {
                if (returnCode != Constants.RC.OK) {
                    return null;
                }
            }
            try {
                // check for accuracy of address
                float[] results = new float[1];
                Location.distanceBetween(
                        latitude, longitude, jsonStreetAddress.getDouble("lat"), jsonStreetAddress.getDouble("lon"), results);
                if (results[0] > DistanceThreshold.ONE_HUNDRED_METERS.getDistanceThresholdInMeters()) {
                    // if the address differs for more than 100 meters, don't take it
                    this.returnCode = Constants.RC.NO_ADDRESS_FOR_COORDINATES;
                }
            } catch (JSONException e) {
                this.returnCode = Constants.RC.BAD_RESPONSE;
            } finally {
                if (returnCode != Constants.RC.OK) {
                    return null;
                }
            }

        // got address, require coordinates
        } else if (! TextUtils.isEmpty(address)) {
            String queryCoordinatesURL = null;
            try {
                if (settingsManagerInstance.getServerSettings().getSelectedAddressProvider().getId().equals(Constants.ADDRESS_PROVIDER.OSM)) {
                    queryCoordinatesURL = String.format(
                            Locale.ROOT,
                            "%1$s/search?format=jsonv2&q=%2$s&accept-language=%3$s&addressdetails=1&limit=1",
                            OSM_URL,
                            URLEncoder.encode(address, "UTF-8"),
                            Locale.getDefault().getLanguage());
                    // get current location
                    if (nearbyCurrentLocation) {
                        PositionManager positionManagerInstance = PositionManager.getInstance(context);
                        PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
                        if (currentLocation == null) {
                            this.returnCode = Constants.RC.NO_LOCATION_FOUND;
                            return null;
                        }
                        queryCoordinatesURL += String.format(
                                Locale.ROOT,
                                "&viewboxlbrt=%1$f,%2$f,%3$f,%4$f&bounded=1",
                                currentLocation.getPoint().getLongitude() - 0.1,
                                currentLocation.getPoint().getLatitude() - 0.1,
                                currentLocation.getPoint().getLongitude() + 0.1,
                                currentLocation.getPoint().getLatitude() + 0.1);
                    }
                    jsonStreetAddress = requestCoordinatesFromOpenStreetMap(context, queryCoordinatesURL);
                } else if (settingsManagerInstance.getServerSettings().getSelectedAddressProvider().getId().equals(Constants.ADDRESS_PROVIDER.GOOGLE)) {
                    queryCoordinatesURL = String.format(
                            Locale.ROOT,
                            "%1$s?address=%2$s&language=%3$s",
                            GOOGLE_MAPS_URL,
                            URLEncoder.encode(address, "UTF-8"),
                            Locale.getDefault().getLanguage());
                    jsonStreetAddress = requestCoordinatesFromGoogle(context, queryCoordinatesURL);
                } else {
                    this.returnCode = Constants.RC.ADDRESS_PROVIDER_NOT_SUPPORTED;
                }
            } catch (ServerCommunicationException e) {
                this.returnCode = e.getReturnCode();
            } catch (UnsupportedEncodingException e) {
                this.returnCode = Constants.RC.BAD_REQUEST;
            } finally {
                if (returnCode != Constants.RC.OK) {
                    return null;
                }
            }

        // got neither coordinates nor address
        } else {
            this.returnCode = Constants.RC.NEITHER_COORDINATES_NOR_ADDRESS;
            return null;
        }

        PointWrapper addressPoint = null;
        try {
            addressPoint = new PointWrapper(context, jsonStreetAddress);
        } catch (JSONException | NullPointerException e) {
            addressPoint = null;
            returnCode = Constants.RC.BAD_RESPONSE;
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
                    context, returnCode, addressPoint);
        }
    }

    @Override protected void onCancelled(PointWrapper addressPoint) {
        if (addressListener != null) {
            addressListener.addressRequestFinished(
                    context, Constants.RC.CANCELLED, addressPoint);
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
        int returnCode = Constants.RC.OK;
        try {
            // create connection
            connection = ServerUtility.getHttpsURLConnectionObject(context, query, null);
            cancelConnectionHandler.postDelayed(cancelConnection, 100);
            connection.connect();
            returnCode = connection.getResponseCode();
            cancelConnectionHandler.removeCallbacks(cancelConnection);
            if (isCancelled()) {
                returnCode = Constants.RC.CANCELLED;
            } else if (returnCode == Constants.RC.OK) {
                JSONObject jsonServerResponse = ServerUtility.processServerResponseAsJSONObject(connection);
                if (! jsonServerResponse.has("status")) {
                    returnCode = Constants.RC.BAD_RESPONSE;
                } else if (! jsonServerResponse.getString("status").equals("OK")) {
                    if (jsonServerResponse.getString("status").equals("OVER_QUERY_LIMIT")) {
                        returnCode = Constants.RC.GOOGLE_MAPS_QUOTA_EXCEEDED;
                    } else {
                        returnCode = Constants.RC.BAD_RESPONSE;
                    }
                } else if (jsonServerResponse.getJSONArray("results").length() == 0) {
                    // no address found
                    returnCode = Constants.RC.NO_ADDRESS_FOR_COORDINATES;
                } else {
                    // get the first address object returned from google
                    jsonStreetAddress = createJsonStreetAddressFromGoogle(
                            context, jsonServerResponse.getJSONArray("results").getJSONObject(0));
                }
            }
        } catch (IOException e) {
            returnCode = Constants.RC.CONNECTION_FAILED;
        } catch (JSONException e) {
            returnCode = Constants.RC.BAD_RESPONSE;
        } catch (ServerCommunicationException e) {
            returnCode = e.getReturnCode();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (returnCode != Constants.RC.OK) {
                throw new ServerCommunicationException(context, returnCode);
            }
        }
        return jsonStreetAddress;
    }

    private JSONObject requestCoordinatesFromGoogle(Context context, String query) throws ServerCommunicationException {
        JSONObject jsonStreetAddress = null;
        int returnCode = Constants.RC.OK;
        try {
            // create connection
            connection = ServerUtility.getHttpsURLConnectionObject(context, query, null);
            cancelConnectionHandler.postDelayed(cancelConnection, 100);
            connection.connect();
            returnCode = connection.getResponseCode();
            cancelConnectionHandler.removeCallbacks(cancelConnection);
            if (isCancelled()) {
                returnCode = Constants.RC.CANCELLED;
            } else if (returnCode == Constants.RC.OK) {
                JSONObject jsonServerResponse = ServerUtility.processServerResponseAsJSONObject(connection);
                if (! jsonServerResponse.has("status")) {
                    returnCode = Constants.RC.BAD_RESPONSE;
                } else if (! jsonServerResponse.getString("status").equals("OK")) {
                    if (jsonServerResponse.getString("status").equals("OVER_QUERY_LIMIT")) {
                        returnCode = Constants.RC.GOOGLE_MAPS_QUOTA_EXCEEDED;
                    } else {
                        returnCode = Constants.RC.BAD_RESPONSE;
                    }
                } else if (jsonServerResponse.getJSONArray("results").length() == 0) {
                    // no coordinates found
                    returnCode = Constants.RC.NO_COORDINATES_FOR_ADDRESS;
                } else {
                    jsonStreetAddress = createJsonStreetAddressFromGoogle(
                            context, jsonServerResponse.getJSONArray("results").getJSONObject(0));
                }
            }
        } catch (IOException e) {
            returnCode = Constants.RC.CONNECTION_FAILED;
        } catch (JSONException e) {
            returnCode = Constants.RC.BAD_RESPONSE;
        } catch (ServerCommunicationException e) {
            returnCode = e.getReturnCode();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (returnCode != Constants.RC.OK) {
                throw new ServerCommunicationException(context, returnCode);
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
        int returnCode = Constants.RC.OK;
        try {
            // create connection
            connection = ServerUtility.getHttpsURLConnectionObject(
                    context, queryURL, null);
            cancelConnectionHandler.postDelayed(cancelConnection, 100);
            connection.connect();
            returnCode = connection.getResponseCode();
            cancelConnectionHandler.removeCallbacks(cancelConnection);
            if (isCancelled()) {
                returnCode = Constants.RC.CANCELLED;
            } else if (returnCode == Constants.RC.OK) {
                JSONObject jsonServerResponse = ServerUtility.processServerResponseAsJSONObject(connection);
                // create street address in json format
                jsonStreetAddress = createJsonStreetAddressFromOSM(context, jsonServerResponse);
            }
        } catch (IOException e) {
            returnCode = Constants.RC.CONNECTION_FAILED;
        } catch (JSONException e) {
            returnCode = Constants.RC.BAD_RESPONSE;
        } catch (ServerCommunicationException e) {
            returnCode = e.getReturnCode();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (returnCode != Constants.RC.OK) {
                throw new ServerCommunicationException(context, returnCode);
            }
        }
        return jsonStreetAddress;
    }


    private JSONObject requestCoordinatesFromOpenStreetMap(Context context, String queryURL) throws ServerCommunicationException {
        JSONObject jsonStreetAddress = null;
        int returnCode = Constants.RC.OK;
        try {
            // create connection
            connection = ServerUtility.getHttpsURLConnectionObject(
                    context, queryURL, null);
            cancelConnectionHandler.postDelayed(cancelConnection, 100);
            connection.connect();
            returnCode = connection.getResponseCode();
            cancelConnectionHandler.removeCallbacks(cancelConnection);
            if (isCancelled()) {
                returnCode = Constants.RC.CANCELLED;
            } else if (returnCode == Constants.RC.OK) {
                JSONArray jsonServerResponse = ServerUtility.processServerResponseAsJSONArray(connection);
                if (jsonServerResponse.length() == 0) {
                    // no coordinates
                    returnCode = Constants.RC.NO_COORDINATES_FOR_ADDRESS;
                } else {
                    // create street address in json format
                    jsonStreetAddress = createJsonStreetAddressFromOSM(context, jsonServerResponse.getJSONObject(0));
                }
            }
        } catch (IOException e) {
            returnCode = Constants.RC.CONNECTION_FAILED;
        } catch (JSONException e) {
            returnCode = Constants.RC.BAD_RESPONSE;
        } catch (ServerCommunicationException e) {
            returnCode = e.getReturnCode();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (returnCode != Constants.RC.OK) {
                throw new ServerCommunicationException(context, returnCode);
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

/*

        // got address, require coordinates
        } else if (! TextUtils.isEmpty(address)) {
            String queryCoordinatesURL = null;
            int boundaryBoxExpansionFactor = 0;
            final int MAX_BOUNDARY_BOX_EXPANSION_FACTOR = 3;
            while (this.returnCode == Constants.RC.OK) {
                boundaryBoxExpansionFactor++;
                try {
                    if (settingsManagerInstance.getServerSettings().getSelectedAddressProvider().getId().equals(Constants.ADDRESS_PROVIDER.OSM)) {
                        queryCoordinatesURL = String.format(
                                Locale.ROOT,
                                "%1$s/search?format=jsonv2&q=%2$s&accept-language=%3$s&addressdetails=1&limit=1",
                                OSM_URL,
                                URLEncoder.encode(address, "UTF-8"),
                                Locale.getDefault().getLanguage());
                        // get current location
                        if (nearbyCurrentLocation) {
                            PositionManager positionManagerInstance = PositionManager.getInstance(context);
                            PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
                            if (currentLocation == null) {
                                this.returnCode = Constants.RC.NO_LOCATION_FOUND;
                                return null;
                            }
                            queryCoordinatesURL += String.format(
                                    Locale.ROOT,
                                    "&viewboxlbrt=%1$f,%2$f,%3$f,%4$f&bounded=1",
                                    currentLocation.getPoint().getLongitude() - 0.05*boundaryBoxExpansionFactor,
                                    currentLocation.getPoint().getLatitude() - 0.05*boundaryBoxExpansionFactor,
                                    currentLocation.getPoint().getLongitude() + 0.05*boundaryBoxExpansionFactor,
                                    currentLocation.getPoint().getLatitude() + 0.05*boundaryBoxExpansionFactor);
                        }
                        jsonStreetAddress = requestCoordinatesFromOpenStreetMap(context, queryCoordinatesURL);
                        break;
                    } else {
                        this.returnCode = Constants.RC.ADDRESS_PROVIDER_NOT_SUPPORTED;
                    }
                } catch (ServerCommunicationException e) {
                    if (nearbyCurrentLocation
                            && boundaryBoxExpansionFactor < MAX_BOUNDARY_BOX_EXPANSION_FACTOR
                            && e.getReturnCode() == Constants.RC.NO_COORDINATES_FOR_ADDRESS) {
                        continue;
                    }
                    this.returnCode = e.getReturnCode();
                } catch (UnsupportedEncodingException e) {
                    this.returnCode = Constants.RC.NO_COORDINATES_FOR_ADDRESS;
                }
            }

    private JSONArray requestCoordinatesFromOpenStreetMap(Context context, String queryURL) throws ServerCommunicationException {
        JSONArray jsonStreetAddressList = new JSONArray();
        int returnCode = Constants.RC.OK;
        try {
            // create connection
            connection = ServerUtility.getHttpsURLConnectionObject(
                    context, queryURL, null);
            cancelConnectionHandler.postDelayed(cancelConnection, 100);
            connection.connect();
            returnCode = connection.getResponseCode();
            cancelConnectionHandler.removeCallbacks(cancelConnection);
            if (isCancelled()) {
                returnCode = Constants.RC.CANCELLED;
            } else if (returnCode == Constants.RC.OK) {
                JSONArray jsonServerResponse = ServerUtility.processServerResponseAsJSONArray(connection);
                for (int i=0; i<jsonServerResponse.length(); i++) {
                    // create street address in json format
                    jsonStreetAddressList.put(
                            createJsonStreetAddressFromOSM(
                                context, jsonServerResponse.getJSONObject(i)));
                }
                if (jsonStreetAddressList.length() == 0) {
                    returnCode = Constants.RC.NO_COORDINATES_FOR_ADDRESS;
                }
            }
        } catch (IOException e) {
            returnCode = Constants.RC.CONNECTION_FAILED;
        } catch (JSONException e) {
            returnCode = Constants.RC.BAD_RESPONSE;
        } catch (ServerCommunicationException e) {
            returnCode = e.getReturnCode();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (returnCode != Constants.RC.OK) {
                throw new ServerCommunicationException(context, returnCode);
            }
        }
        return jsonStreetAddressList;
    }

*/
