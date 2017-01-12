package org.walkersguide.android.sensor;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.basic.point.GPS;
import org.walkersguide.android.basic.point.Point;
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
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.primitives.Ints;

public class PositionManager implements android.location.LocationListener {

    public static final float UPDATE_LOCATION_THRESHOLD_1 = 50.0f;            // 50 meters

    private Context context;
    private static PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;

    // gps variables
    private LocationManager locationManager;
    private GPS currentGPSPosition;
    private Point simulatedLocation;
    private Point lastLocation;
    private int locationSource;

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

        // load locations from application settings
        LocationSettings locationSettings = settingsManagerInstance.getLocationSettings();
        this.locationSource = locationSettings.getSelectedLocationSource();
        this.currentGPSPosition = locationSettings.getCurrentGPSPosition();
        this.simulatedLocation = locationSettings.getSimulatedLocation();
        this.lastLocation = null;
    }


    /**
     * point getters
     */

    public Point getCurrentLocation() {
        switch (this.locationSource) {
            case Constants.LOCATION_SOURCE.GPS:
                return this.currentGPSPosition;
            case Constants.LOCATION_SOURCE.SIMULATION:
                return this.simulatedLocation;
            default:
                return null;
        }
    }

    public GPS getCurrentGPSPosition() {
        return this.currentGPSPosition;
    }

    public Point getSimulatedLocation() {
        return this.simulatedLocation;
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
        }
    }

    public void setLocationSource(int newLocationSource) {
        if (Ints.contains(Constants.LocationSourceValueArray, newLocationSource)
                && this.locationSource != newLocationSource) {
            this.locationSource = newLocationSource;
            // save
            LocationSettings locationSettings = settingsManagerInstance.getLocationSettings();
            locationSettings.setSelectedLocationSource(newLocationSource);
            // new direction broadcast
            sendNewLocationBroadcast();
        }
    }

    private void sendNewLocationBroadcast() {
        Intent intent;
        switch (this.locationSource) {
            case Constants.LOCATION_SOURCE.GPS:
                intent = new Intent(Constants.ACTION_NEW_LOCATION);
                if (this.lastLocation == null
                        || this.lastLocation.distanceTo(this.currentGPSPosition) > UPDATE_LOCATION_THRESHOLD_1) {
                    this.lastLocation = this.currentGPSPosition;
                    intent.putExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_UPDATE_THRESHOLD, 1);
                } else {
                    intent.putExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_UPDATE_THRESHOLD, 0);
                }
                if (this.currentGPSPosition.getSpeed() > Constants.SPEED.HIGH) {
                    intent.putExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_AT_HIGH_SPEED, 1);
                } else {
                    intent.putExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_AT_HIGH_SPEED, 0);
                }
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                break;
            case Constants.LOCATION_SOURCE.SIMULATION:
                intent = new Intent(Constants.ACTION_NEW_LOCATION);
                if (this.lastLocation == null
                        || this.lastLocation.distanceTo(this.simulatedLocation) > UPDATE_LOCATION_THRESHOLD_1) {
                    this.lastLocation = this.simulatedLocation;
                    intent.putExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_UPDATE_THRESHOLD, 1);
                } else {
                    intent.putExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_UPDATE_THRESHOLD, 0);
                }
                intent.putExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_AT_HIGH_SPEED, 0);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                break;
        }
    }


    /**
     * simulation
     */

    public void setSimulatedLocation(Point newLocation) {
        if (newLocation != null) {
            LocationSettings locationSettings = this.settingsManagerInstance.getLocationSettings();
            this.simulatedLocation = newLocation;
            locationSettings.setSimulatedLocation(newLocation);
            // broadcast new location action
            if (this.locationSource == Constants.LOCATION_SOURCE.SIMULATION) {
                sendNewLocationBroadcast();
            }
        }
    }


    /**
     * location from gps
     */

    @Override public void onLocationChanged(Location newLocationObject) {
        if(isBetterLocation(newLocationObject)) {
            System.out.println("xxx new location: " + newLocationObject.getProvider());
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

            // try to create gps point from location object
            GPS newLocation = null;;
            try {
                JSONObject jsonNewLocation = new JSONObject();
                jsonNewLocation.put("name", context.getResources().getString(R.string.currentLocationName));
                jsonNewLocation.put("type", Constants.TYPE.GPS);
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
                if (newLocationObject.hasSpeed()) {
                    jsonNewLocation.put("speed", newLocationObject.getSpeed());
                }
                newLocation = new GPS(this.context, jsonNewLocation);
            } catch (JSONException e) {
                newLocation = null;
            }

            // take as new gps location
            if (newLocation != null) {
                System.out.println("xxx new location: " + newLocation);
                // save current location
                LocationSettings locationSettings = this.settingsManagerInstance.getLocationSettings();
                this.currentGPSPosition = newLocation;
                locationSettings.setCurrentGPSPosition(newLocation);
                // broadcast new gps position action
                Intent intent = new Intent(Constants.ACTION_NEW_GPS_POSITION);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                // broadcast new location action
                if (this.locationSource == Constants.LOCATION_SOURCE.GPS) {
                    sendNewLocationBroadcast();
                }
            }
        }
    }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}

    private boolean isBetterLocation(Location newLocationObject) {
        // some trivial tests
        if (newLocationObject == null) {
            return false;
        } else if (this.currentGPSPosition == null) {
            return true;
        }

        // define log variables
        boolean locationAccepted = false;

        // Check whether the new location fix is newer or older
        long timeDelta = newLocationObject.getTime() - this.currentGPSPosition.getTime();
        boolean isNewer = timeDelta > 0;
        boolean isABitNewer = timeDelta > 10000;
        boolean isSignificantlyNewer = timeDelta > 20000;
        boolean isMuchMuchNewer = timeDelta > 180000;

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (newLocationObject.getAccuracy() - this.currentGPSPosition.getAccuracy());
        int accuracyThresholdValue = 15;
        boolean isMoreAccurateThanThresholdValue = newLocationObject.getAccuracy() <= accuracyThresholdValue;
        boolean isMoreAccurate = false;
        boolean isABitLessAccurate = false;
        boolean isSignificantlyLessAccurate = false;
        if (newLocationObject.getAccuracy() < (2*accuracyThresholdValue)) {
            isMoreAccurate = accuracyDelta <= 0;
            isABitLessAccurate = accuracyDelta <= 10;
            isSignificantlyLessAccurate = accuracyDelta <= 30;
        } else {
            isMoreAccurate = accuracyDelta < 0;
            isABitLessAccurate = accuracyDelta < 10;
            isSignificantlyLessAccurate = accuracyDelta < 30;
        }

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(
                newLocationObject.getProvider(), this.currentGPSPosition.getProvider());

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

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

}
