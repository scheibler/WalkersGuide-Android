package org.walkersguide.android.sensor;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.point.PointWrapper;
import org.walkersguide.android.data.poi.FavoritesProfile;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.DirectionSettings;
import org.walkersguide.android.util.SettingsManager.LocationSettings;

import android.content.Context;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.primitives.Ints;

public class PositionManager implements android.location.LocationListener {

    // location thresholds
    public interface THRESHOLD0 {
        public static final int ID = 0;
        public static final float DISTANCE = 0.0f;           // 0 meters -> every change
    }
    public interface THRESHOLD1 {
        public static final int ID = 1;
        public static final float DISTANCE = 5.0f;           // 5 meters
    }
    public interface THRESHOLD2 {
        public static final int ID = 2;
        public static final float DISTANCE = 50.0f;          // 50 meters
    }

    // high speed
    public static final float THRESHOLD_HIGH_SPEED = 5.0f;                          // 5 km/h

    // dummy location point
    public static PointWrapper getDummyLocation(Context context) {
        PointWrapper dummyLocation = null;
        try {
            dummyLocation = new PointWrapper(context, new JSONObject(Constants.DUMMY.LOCATION));
        } catch (JSONException e) {}
        return dummyLocation;
    }

    private Context context;
    private static PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;
    private Vibrator vibrator;

    // gps variables
    private LocationManager locationManager;
    private HashMap<Integer,PointWrapper> lastLocationMap;
    private boolean gpsFixFound;
    private float[] travelingSpeedArray;

    public static PositionManager getInstance(Context context) {
        if(positionManagerInstance == null){
            positionManagerInstance = new PositionManager(context.getApplicationContext());
        }
        return positionManagerInstance;
    }

    private PositionManager(Context context) {
        this.context = context;
        this.locationManager = null;
        this.settingsManagerInstance = SettingsManager.getInstance(context);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        // last location map
        this.lastLocationMap = new HashMap<Integer,PointWrapper>();
        this.gpsFixFound = false;
        // traveling speed array
        this.travelingSpeedArray = new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    }


    /**
     * gps management
     */

    public void stopGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            locationManager = null;
        }
    }

    public void startGPS() {
        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, this);
            gpsFixFound = false;
        }
    }

    public int getLocationSource() {
        return settingsManagerInstance.getLocationSettings().getSelectedLocationSource();
    }

    public void setLocationSource(int newLocationSource) {
        LocationSettings locationSettings = settingsManagerInstance.getLocationSettings();
        if (Ints.contains(Constants.LocationSourceValueArray, newLocationSource)
                && locationSettings.getSelectedLocationSource() != newLocationSource) {
            // new direction broadcast
            locationSettings.setSelectedLocationSource(newLocationSource);
            broadcastCurrentLocation();
        }
    }


    /**
     * current location
     */

    public PointWrapper getCurrentLocation() {
        LocationSettings locationSettings = settingsManagerInstance.getLocationSettings();
        switch (locationSettings.getSelectedLocationSource()) {
            case Constants.LOCATION_SOURCE.GPS:
                return locationSettings.getGPSLocation();
            case Constants.LOCATION_SOURCE.SIMULATION:
                return locationSettings.getSimulatedLocation();
            default:
                return null;
        }
    }

    public void requestCurrentLocation() {
        broadcastCurrentLocation();
    }

    private void broadcastCurrentLocation() {
        PointWrapper currentLocation = getCurrentLocation();
        if (currentLocation != null) {
            Intent intent = new Intent(Constants.ACTION_NEW_LOCATION);
            // point
            try {
                intent.putExtra(
                        Constants.ACTION_NEW_LOCATION_ATTR.STRING_POINT_SERIALIZED,
                        currentLocation.toJson().toString());
            } catch (JSONException e) {
                intent.putExtra(
                        Constants.ACTION_NEW_LOCATION_ATTR.STRING_POINT_SERIALIZED,
                        "");
            }
            // source
            intent.putExtra(
                    Constants.ACTION_NEW_LOCATION_ATTR.INT_SOURCE,
                    settingsManagerInstance.getLocationSettings().getSelectedLocationSource());

            // distance threshold
            intent.putExtra(
                    Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID,
                    THRESHOLD0.ID);
            if (lastLocationMap.get(THRESHOLD1.ID) == null
                    || lastLocationMap.get(THRESHOLD1.ID).distanceTo(currentLocation) > THRESHOLD1.DISTANCE) {
                lastLocationMap.put(THRESHOLD1.ID, currentLocation);
                intent.putExtra(
                        Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID,
                        THRESHOLD1.ID);
            }
            if (lastLocationMap.get(THRESHOLD2.ID) == null
                    || lastLocationMap.get(THRESHOLD2.ID).distanceTo(currentLocation) > THRESHOLD2.DISTANCE) {
                lastLocationMap.put(THRESHOLD2.ID, currentLocation);
                intent.putExtra(
                        Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID,
                        THRESHOLD2.ID);
            }

            // speed threshold
            float speedSum = 0.0f;
            for (int i=1; i<travelingSpeedArray.length; i++) {
                speedSum += travelingSpeedArray[i];
            }
            if (speedSum/travelingSpeedArray.length > THRESHOLD_HIGH_SPEED) {
                intent.putExtra(Constants.ACTION_NEW_LOCATION_ATTR.BOOL_AT_HIGH_SPEED, true);
            } else {
                intent.putExtra(Constants.ACTION_NEW_LOCATION_ATTR.BOOL_AT_HIGH_SPEED, false);
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }


    /**
     * location from gps
     */

    public PointWrapper getGPSLocation() {
        return settingsManagerInstance.getLocationSettings().getGPSLocation();
    }

    public void requestGPSLocation() {
        broadcastGPSLocation();
    }

    private void broadcastGPSLocation() {
        LocationSettings locationSettings = settingsManagerInstance.getLocationSettings();
        if (locationSettings.getGPSLocation() != null) {
            Intent intent = new Intent(Constants.ACTION_NEW_GPS_LOCATION);
            try {
                intent.putExtra(
                        Constants.ACTION_NEW_LOCATION_ATTR.STRING_POINT_SERIALIZED,
                        locationSettings.getGPSLocation().toJson().toString());
            } catch (JSONException e) {
                intent.putExtra(
                        Constants.ACTION_NEW_LOCATION_ATTR.STRING_POINT_SERIALIZED,
                        "");
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    @Override public void onLocationChanged(Location newLocationObject) {
        // try to create gps point from location object
        PointWrapper newLocation = null;;
        try {
            JSONObject jsonNewLocation = new JSONObject();
            jsonNewLocation.put("name", context.getResources().getString(R.string.currentLocationName));
            jsonNewLocation.put("type", Constants.POINT.GPS);
            jsonNewLocation.put("sub_type", context.getResources().getString(R.string.currentLocationName));
            jsonNewLocation.put("lat", newLocationObject.getLatitude());
            jsonNewLocation.put("lon", newLocationObject.getLongitude());
            jsonNewLocation.put("provider", newLocationObject.getProvider());
            jsonNewLocation.put("time", newLocationObject.getTime());
            if (newLocationObject.hasAccuracy()) {
                jsonNewLocation.put("accuracy", newLocationObject.getAccuracy());
            }
            if (newLocationObject.hasAltitude()) {
                jsonNewLocation.put("altitude", newLocationObject.getAltitude());
            }
            if (newLocationObject.hasBearing()) {
                jsonNewLocation.put("bearing", newLocationObject.getBearing());
            }
            if (newLocationObject.getExtras() != null) {
                Bundle locationExtras = newLocationObject.getExtras();
                if (locationExtras.containsKey("satellites")
                        && locationExtras.getInt("satellites") >= 0) {
                    jsonNewLocation.put("number_of_satellites", locationExtras.getInt("satellites"));
                        }
            }
            if (newLocationObject.hasSpeed()) {
                jsonNewLocation.put("speed", newLocationObject.getSpeed());
            }
            newLocation = new PointWrapper(this.context, jsonNewLocation);
        } catch (JSONException e) {
            return;
        }

        // traveling speed array update
        LocationSettings locationSettings = this.settingsManagerInstance.getLocationSettings();
        GPS gpsLocation = (GPS) locationSettings.getGPSLocation().getPoint();
        if (newLocationObject.hasSpeed()) {
            if (gpsLocation == null
                    || newLocationObject.getTime() - gpsLocation.getTime() > 180000) {
                // last fix at least three minutes ago -> reset speed array
                travelingSpeedArray = new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
            } else {
                System.arraycopy(travelingSpeedArray, 0, travelingSpeedArray, 1, travelingSpeedArray.length-1);
                travelingSpeedArray[0] = newLocationObject.getSpeed();
            }
        }

        if(isBetterLocation(gpsLocation, (GPS) newLocation.getPoint())) {
            // save current location
            locationSettings.setGPSLocation(newLocation);
            // broadcast new gps position action
            broadcastGPSLocation();
            // broadcast new location action
            if (locationSettings.getSelectedLocationSource() == Constants.LOCATION_SOURCE.GPS) {
                broadcastCurrentLocation();
            }

            // first gps fix
            if (! gpsFixFound) {
                gpsFixFound = true;
                // inform user about first gps fix after start
                vibrator.vibrate(new long[]{250, 50, 250, 50}, -1);
                // obtain the diff to true north
                if (newLocationObject.hasAltitude()) {
                    GeomagneticField geoField = new GeomagneticField(
                            Double.valueOf(newLocationObject.getLatitude()).floatValue(),
                            Double.valueOf(newLocationObject.getLongitude()).floatValue(),
                            Double.valueOf(newLocationObject.getAltitude()).floatValue(),
                            System.currentTimeMillis());
                    DirectionSettings directionSettings = this.settingsManagerInstance.getDirectionSettings();
                    directionSettings.setDifferenceToTrueNorth(geoField.getDeclination());
                }
            }
        }
    }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}

    private static boolean isBetterLocation(GPS gpsLocation, GPS newLocation) {
        boolean locationAccepted = false;

        // some trivial tests
        if (newLocation == null) {
            return false;
        } else if (gpsLocation == null) {
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = newLocation.getTime() - gpsLocation.getTime();
        boolean isNewer = timeDelta > 0;
        boolean isABitNewer = timeDelta > 10000;
        boolean isSignificantlyNewer = timeDelta > 20000;
        boolean isMuchMuchNewer = timeDelta > 180000;

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (newLocation.getAccuracy() - gpsLocation.getAccuracy());
        int accuracyThresholdValue = 15;
        boolean isMoreAccurateThanThresholdValue = newLocation.getAccuracy() <= accuracyThresholdValue;
        boolean isMoreAccurate = false;
        boolean isABitLessAccurate = false;
        boolean isSignificantlyLessAccurate = false;
        if (newLocation.getAccuracy() < (2*accuracyThresholdValue)) {
            isMoreAccurate = accuracyDelta <= 0;
            isABitLessAccurate = accuracyDelta <= 10;
            isSignificantlyLessAccurate = accuracyDelta <= 30;
        } else {
            isMoreAccurate = accuracyDelta < 0;
            isABitLessAccurate = accuracyDelta < 10;
            isSignificantlyLessAccurate = accuracyDelta < 30;
        }

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = newLocation.getProvider().equals(gpsLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isNewer && isMoreAccurateThanThresholdValue) {
            locationAccepted = true;
        } else if (isNewer && isMoreAccurate && isFromSameProvider) {
            locationAccepted = true;
        } else if (isABitNewer && isABitLessAccurate && isFromSameProvider) {
            locationAccepted = true;
        } else if (isSignificantlyNewer && isSignificantlyLessAccurate) {
            locationAccepted = true;
        } else if (isMuchMuchNewer) {
            locationAccepted = true;
        }

        return locationAccepted;
    }


    /**
     * location from simulation
     */

    public PointWrapper getSimulatedLocation() {
        return settingsManagerInstance.getLocationSettings().getSimulatedLocation();
    }

    public void requestSimulatedLocation() {
        broadcastSimulatedLocation();
    }

    private void broadcastSimulatedLocation() {
        LocationSettings locationSettings = settingsManagerInstance.getLocationSettings();
        if (locationSettings.getSimulatedLocation() != null) {
            Intent intent = new Intent(Constants.ACTION_NEW_SIMULATED_LOCATION);
            try {
                intent.putExtra(
                        Constants.ACTION_NEW_LOCATION_ATTR.STRING_POINT_SERIALIZED,
                        locationSettings.getSimulatedLocation().toJson().toString());
            } catch (JSONException e) {
                intent.putExtra(
                        Constants.ACTION_NEW_LOCATION_ATTR.STRING_POINT_SERIALIZED,
                        "");
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    public void setSimulatedLocation(PointWrapper newLocation) {
        if (newLocation != null) {
            LocationSettings locationSettings = this.settingsManagerInstance.getLocationSettings();
            locationSettings.setSimulatedLocation(newLocation);
            // add to simulated points profile
            AccessDatabase.getInstance(this.context).addPointToFavoritesProfile(
                    newLocation, FavoritesProfile.ID_SIMULATED_POINTS);
            // broadcast new simulated location action
            broadcastSimulatedLocation();
            // broadcast new location action
            if (locationSettings.getSelectedLocationSource() == Constants.LOCATION_SOURCE.SIMULATION) {
                broadcastCurrentLocation();
            }
        }
    }

}
