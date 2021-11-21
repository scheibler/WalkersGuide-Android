package org.walkersguide.android.server.address;

import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.DatabaseProfileRequest;
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

import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.data.basic.point.StreetAddress;
import org.walkersguide.android.data.sensor.threshold.DistanceThreshold;
import org.walkersguide.android.server.util.ServerCommunicationException;
import org.walkersguide.android.server.util.ServerUtility;
import org.walkersguide.android.util.StringUtility;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.GlobalInstance;


public class AddressManager extends AsyncTask<Void, Void, Integer> {
    // address providers
    //  osm: https://wiki.openstreetmap.org/wiki/Nominatim
    private static final String OSM_URL = "https://nominatim.openstreetmap.org";

    public interface AddressRequestListener {
        public void requireAddressRequestSuccessful(StreetAddress addressPoint);
        public void requireCoordinatesRequestSuccessful(ArrayList<StreetAddress> addressPointList);
        public void addressOrCoordinatesRequestFailed(int returnCode);
    }


    private abstract class Request {}

    private class RequireAddressRequest extends Request {
        public double latitude, longitude;
        public StreetAddress result;
        public RequireAddressRequest(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.result = null;
        }
    }

    private class RequireCoordinatesRequest extends Request {
        public String address;
        public boolean nearbyCurrentLocation;
        public ArrayList<StreetAddress> resultList;
        public RequireCoordinatesRequest(String address, boolean nearbyCurrentLocation) {
            this.address = address;
            this.nearbyCurrentLocation = nearbyCurrentLocation;
            this.resultList = null;
        }
    }


    private AddressRequestListener listener;
    private Request request;
    private HttpsURLConnection connection;
    private Handler cancelConnectionHandler;
    private CancelConnection cancelConnection;

    public AddressManager(AddressRequestListener listener, double latitude, double longitude) {
        this.listener = listener;
        this.request = new RequireAddressRequest(latitude, longitude);
        this.connection = null;
        this.cancelConnectionHandler = new Handler();
        this.cancelConnection = new CancelConnection();
    }

    public AddressManager(AddressRequestListener listener, String address, boolean nearbyCurrentLocation) {
        this.listener = listener;
        this.request = new RequireCoordinatesRequest(address, nearbyCurrentLocation);
        this.connection = null;
        this.cancelConnectionHandler = new Handler();
        this.cancelConnection = new CancelConnection();
    }

    @Override protected Integer doInBackground(Void... params) {
        int returnCode = Constants.RC.OK;

        // got coordinates, require address
        if (this.request instanceof RequireAddressRequest) {
            RequireAddressRequest requireAddressRequest = (RequireAddressRequest) this.request;

            // first look into local database
            TreeMap<Float,StreetAddress> distancesToFavoritesMap = new TreeMap<Float,StreetAddress>();
            DatabaseProfileRequest databaseProfileRequest = new DatabaseProfileRequest(
                    DatabasePointProfile.ADDRESS_POINTS, null, SortMethod.DISTANCE_ASC);
            for (ObjectWithId objectWithId : AccessDatabase.getInstance().getObjectWithIdListFor(databaseProfileRequest)) {
                if (objectWithId instanceof StreetAddress) {
                    StreetAddress address = (StreetAddress) objectWithId;
                    // calculate distance
                    float[] results = new float[1];
                    Location.distanceBetween(
                            requireAddressRequest.latitude, requireAddressRequest.longitude,
                            address.getLatitude(), address.getLongitude(),
                            results);
                    // add
                    distancesToFavoritesMap.put(results[0], address);
                }
            }

            Map.Entry<Float,StreetAddress> closestAddress = distancesToFavoritesMap.firstEntry();
            if (closestAddress != null
                    && closestAddress.getKey() < DistanceThreshold.TWENTY_METERS.getDistanceThresholdInMeters()) {
                requireAddressRequest.result = closestAddress.getValue();

            } else {
                // otherwise query address provider
                StreetAddress foundAddress = null;
                try {
                    foundAddress = requestAddressFromOpenStreetMap(
                            String.format(
                                Locale.ROOT,
                                "%1$s/reverse?format=jsonv2&lat=%2$f&lon=%3$f&accept-language=%4$s&addressdetails=1&zoom=18",
                                OSM_URL,
                                requireAddressRequest.latitude,
                                requireAddressRequest.longitude,
                                Locale.getDefault().getLanguage())
                            );
                } catch (ServerCommunicationException e) {
                    returnCode = e.getReturnCode();
                } finally {
                    if (foundAddress != null) {
                        // check for accuracy of address
                        float[] results = new float[1];
                        Location.distanceBetween(
                                requireAddressRequest.latitude, requireAddressRequest.longitude,
                                foundAddress.getLatitude(), foundAddress.getLongitude(),
                                results);
                        if (results[0] < DistanceThreshold.ONE_HUNDRED_METERS.getDistanceThresholdInMeters()) {
                            requireAddressRequest.result = foundAddress;
                        } else {
                            // if the address differs for more than 100 meters, don't take it
                            returnCode = Constants.RC.NO_ADDRESS_FOR_COORDINATES;
                        }
                    }
                }
            }

        // got address, require coordinates
        } else if (this.request instanceof RequireCoordinatesRequest) {
            RequireCoordinatesRequest requireCoordinatesRequest = (RequireCoordinatesRequest) this.request;

            String queryCoordinatesURL = null;
            try {
                queryCoordinatesURL = String.format(
                        Locale.ROOT,
                        "%1$s/search?format=jsonv2&q=%2$s&accept-language=%3$s&addressdetails=1&limit=10",
                        OSM_URL,
                        URLEncoder.encode(requireCoordinatesRequest.address, "UTF-8"),
                        Locale.getDefault().getLanguage());
                // get current location
                PositionManager positionManagerInstance = PositionManager.getInstance();
                Point currentLocation = positionManagerInstance.getCurrentLocation();
                if (currentLocation != null && requireCoordinatesRequest.nearbyCurrentLocation) {
                    queryCoordinatesURL += String.format(
                            Locale.ROOT,
                            "&viewboxlbrt=%1$f,%2$f,%3$f,%4$f&bounded=1",
                            currentLocation.getLongitude() - 0.1,
                            currentLocation.getLatitude() - 0.1,
                            currentLocation.getLongitude() + 0.1,
                            currentLocation.getLatitude() + 0.1);
                }
                requireCoordinatesRequest.resultList = requestCoordinatesFromOpenStreetMap(queryCoordinatesURL);
            } catch (ServerCommunicationException e) {
                returnCode = e.getReturnCode();
            } catch (UnsupportedEncodingException e) {
                returnCode = Constants.RC.BAD_REQUEST;
            }

        // got neither coordinates nor address
        } else {
            returnCode = Constants.RC.NEITHER_COORDINATES_NOR_ADDRESS;
        }

        return returnCode;
    }

    @Override protected void onPostExecute(Integer returnCode) {
        if (listener != null) {
            if (returnCode == Constants.RC.OK) {
                if (this.request instanceof RequireAddressRequest) {
                    listener.requireAddressRequestSuccessful(
                            ((RequireAddressRequest) this.request).result);
                } else {
                    listener.requireCoordinatesRequestSuccessful(
                            ((RequireCoordinatesRequest) this.request).resultList);
                }
            } else {
                listener.addressOrCoordinatesRequestFailed(returnCode);
            }
        }
    }

    @Override protected void onCancelled(Integer returnCode) {
        if (listener != null) {
            listener.addressOrCoordinatesRequestFailed(Constants.RC.CANCELLED);
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


    /** OSM **/

    private StreetAddress requestAddressFromOpenStreetMap(String queryURL) throws ServerCommunicationException {
        StreetAddress streetAddress = null;
        int returnCode = Constants.RC.OK;
        try {
            // create connection
            connection = ServerUtility.getHttpsURLConnectionObject(queryURL, null);
            cancelConnectionHandler.postDelayed(cancelConnection, 100);
            connection.connect();
            returnCode = connection.getResponseCode();
            cancelConnectionHandler.removeCallbacks(cancelConnection);
            if (isCancelled()) {
                returnCode = Constants.RC.CANCELLED;
            } else if (returnCode == Constants.RC.OK) {
                JSONObject jsonServerResponse = ServerUtility.processServerResponseAsJSONObject(connection);
                // create street address in json format
                streetAddress = createStreetAddressFromOSM(jsonServerResponse);
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
                throw new ServerCommunicationException(returnCode);
            }
        }
        return streetAddress;
    }


    private ArrayList<StreetAddress> requestCoordinatesFromOpenStreetMap(String queryURL) throws ServerCommunicationException {
        ArrayList<StreetAddress> streetAddressList = new ArrayList<StreetAddress>();
        int returnCode = Constants.RC.OK;
        try {
            // create connection
            connection = ServerUtility.getHttpsURLConnectionObject(queryURL, null);
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
                    streetAddressList.add(
                            createStreetAddressFromOSM(
                                jsonServerResponse.getJSONObject(i)));
                }
                if (streetAddressList.isEmpty()) {
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
                throw new ServerCommunicationException(returnCode);
            }
        }
        return streetAddressList;
    }


    private StreetAddress createStreetAddressFromOSM(JSONObject jsonAddressData) throws JSONException {
        StreetAddress.Builder addressBuilder = new StreetAddress.Builder(
                jsonAddressData.getString("display_name"),
                jsonAddressData.getDouble("lat"),
                jsonAddressData.getDouble("lon"));
        addressBuilder.setDisplayName(jsonAddressData.getString("display_name"));

        // address sub object
        JSONObject jsonAddressComponentList = jsonAddressData.getJSONObject("address");
        Iterator<String> keysIterator = jsonAddressComponentList.keys();
        while (keysIterator.hasNext()) {
            String type = (String)keysIterator.next();
            if (type.equals("house_number")) {
                addressBuilder.setHouseNumber(jsonAddressComponentList.getString(type));
            } else if (type.equals("pedestrian")) {
                addressBuilder.setRoad(jsonAddressComponentList.getString(type));
            } else if (type.equals("road")) {
                addressBuilder.setRoad(jsonAddressComponentList.getString(type));
            } else if (type.equals("residential")) {
                addressBuilder.setResidential(jsonAddressComponentList.getString(type));
            } else if (type.equals("suburb")) {
                addressBuilder.setSuburb(jsonAddressComponentList.getString(type));
            } else if (type.equals("city_district")) {
                addressBuilder.setCityDistrict(jsonAddressComponentList.getString(type));
            } else if (type.equals("postcode")) {
                addressBuilder.setZipcode(jsonAddressComponentList.getString(type));
            } else if (type.equals("village")) {
                addressBuilder.setCity(jsonAddressComponentList.getString(type));
            } else if (type.equals("city")) {
                addressBuilder.setCity(jsonAddressComponentList.getString(type));
            } else if (type.equals("state")) {
                addressBuilder.setState(jsonAddressComponentList.getString(type));
            } else if (type.equals("country")) {
                addressBuilder.setCountry(jsonAddressComponentList.getString(type));
            } else if (type.equals("country_code")) {
                addressBuilder.setCountryCode(jsonAddressComponentList.getString(type));
            } else if (type.equals(jsonAddressData.getString("type"))) {
                addressBuilder.setExtraName(jsonAddressComponentList.getString(type));
            }
        }

        return addressBuilder.build();
    }

}
