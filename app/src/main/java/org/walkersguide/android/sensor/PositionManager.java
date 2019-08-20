package org.walkersguide.android.sensor;

import android.annotation.TargetApi;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.hardware.GeomagneticField;

import android.location.Location;
import android.location.LocationManager;

import android.Manifest;

import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;

import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.lang.Math;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;

import org.json.JSONException;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.HistoryPointProfile;
import org.walkersguide.android.data.sensor.attribute.NewLocationAttributes;
import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.data.sensor.threshold.DistanceThreshold;
import org.walkersguide.android.data.sensor.threshold.SpeedThreshold;
import org.walkersguide.android.helper.FileUtility;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.LocationSettings;


public class PositionManager implements android.location.LocationListener {
    private DateFormat timeFormatter = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
    private DateFormat dateAndTimeFormatter = SimpleDateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);

    private Context context;
    private static PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;
    private Vibrator vibrator;

    public static PositionManager getInstance(Context context) {
        if(positionManagerInstance == null){
            positionManagerInstance = new PositionManager(context.getApplicationContext());
        }
        return positionManagerInstance;
    }

    private PositionManager(Context context) {
        this.context = context;
        this.settingsManagerInstance = SettingsManager.getInstance(context);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }


    /**
     * gps management
     */
    private LocationManager locationManager = null;
    private boolean gpsFixFound = false;
    private boolean simulationEnabled = false;

    public void stopGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            locationManager = null;
        }
    }

    public void startGPS() {
        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (! locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager = null;
                Intent locationProviderDisabledIntent = new Intent(Constants.ACTION_LOCATION_PROVIDER_DISABLED);
                LocalBroadcastManager.getInstance(context).sendBroadcast(locationProviderDisabledIntent);
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                locationManager = null;
                Intent locationPermissionDeniedIntent = new Intent(Constants.ACTION_LOCATION_PERMISSION_DENIED);
                LocalBroadcastManager.getInstance(context).sendBroadcast(locationPermissionDeniedIntent);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, this);
                gpsFixFound = false;
            }
            if (BuildConfig.DEBUG) {
                FileUtility.appendToLog(
                        context,
                        "newLocationAttributes",
                        dateAndTimeFormatter.format(new Date(System.currentTimeMillis())));
            }
        }
    }

    public boolean getSimulationEnabled() {
        return this.simulationEnabled;
    }

    public void setSimulationEnabled(boolean enabled) {
        this.simulationEnabled = enabled;
        broadcastCurrentLocation();
    }


    /**
     * current location
     */
    private HashMap<DistanceThreshold,PointWrapper> lastAggregatingLocationMap = null;
    private PointWrapper lastImmediateLocation = null;
    private float[] travelingSpeedArray = null;

    public PointWrapper getCurrentLocation() {
        LocationSettings locationSettings = settingsManagerInstance.getLocationSettings();
        if (this.simulationEnabled) {
            return locationSettings.getSimulatedLocation();
        } else {
            return locationSettings.getGPSLocation();
        }
    }

    public void requestCurrentLocation() {
        broadcastCurrentLocation();
    }

    private void broadcastCurrentLocation() {
        PointWrapper currentLocation = getCurrentLocation();
        NewLocationAttributes.Builder newLocationAttributesBuilder = new NewLocationAttributes.Builder(context, currentLocation);

        // add optional attributes
        if (currentLocation != null) {

            // aggregating threshold
            if (lastAggregatingLocationMap == null) {
                // initialize
                lastAggregatingLocationMap = new HashMap<DistanceThreshold,PointWrapper>();;
                for (DistanceThreshold distanceThreshold : DistanceThreshold.values()) {
                    lastAggregatingLocationMap.put(distanceThreshold, currentLocation);
                }
                newLocationAttributesBuilder.setAggregatingDistanceThreshold(DistanceThreshold.ZERO_METERS);
            } else {
                for (DistanceThreshold distanceThreshold : DistanceThreshold.values()) {
                    if (lastAggregatingLocationMap.get(distanceThreshold).distanceTo(currentLocation) > distanceThreshold.getDistanceThresholdInMeters()) {
                        lastAggregatingLocationMap.put(distanceThreshold, currentLocation);
                        newLocationAttributesBuilder.setAggregatingDistanceThreshold(distanceThreshold);
                    }
                }
            }

            // immediate threshold
            if (lastImmediateLocation == null) {
                // initialize
                lastImmediateLocation = currentLocation;
                newLocationAttributesBuilder.setImmediateDistanceThreshold(DistanceThreshold.ZERO_METERS);
            } else {
                for (DistanceThreshold distanceThreshold : DistanceThreshold.values()) {
                    if (lastImmediateLocation.distanceTo(currentLocation) > distanceThreshold.getDistanceThresholdInMeters()) {
                        newLocationAttributesBuilder.setImmediateDistanceThreshold(distanceThreshold);
                    }
                }
                lastImmediateLocation = currentLocation;
            }

            // speed threshold
            if (travelingSpeedArray == null) {
                // initialize
                travelingSpeedArray = new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
                newLocationAttributesBuilder.setSpeedThreshold(SpeedThreshold.ZERO_KMH);
            } else {
                // get average speed
                float speedSum = 0.0f;
                for (int i=0; i<travelingSpeedArray.length; i++) {
                    speedSum += travelingSpeedArray[i];
                }
                float averageSpeedinKMH = speedSum / travelingSpeedArray.length;
                if (! this.simulationEnabled) {
                    for (SpeedThreshold speedThreshold : SpeedThreshold.values()) {
                        if (averageSpeedinKMH > speedThreshold.getSpeedThresholdInKMH()) {
                            newLocationAttributesBuilder.setSpeedThreshold(speedThreshold);
                        }
                    }
                }
            }
        }

        // send intent
        Intent intent = new Intent(Constants.ACTION_NEW_LOCATION);
        intent.putExtra(
                Constants.ACTION_NEW_LOCATION_ATTRIBUTES, newLocationAttributesBuilder.toJson().toString());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        // debug: log to file
        if (BuildConfig.DEBUG) {
            NewLocationAttributes nla = newLocationAttributesBuilder.build();
            FileUtility.appendToLog(
                    context,
                    "newLocationAttributes",
                    String.format(
                        "%1$s\tAgg:%2$3d\tImm:%3$3d\tspeed:%4$3d",
                        timeFormatter.format(new Date(System.currentTimeMillis())),
                        nla.getAggregatingDistanceThreshold().getDistanceThresholdInMeters(),
                        nla.getImmediateDistanceThreshold().getDistanceThresholdInMeters(),
                        nla.getSpeedThreshold().getSpeedThresholdInKMH())
                    );
        }
    }


    /**
     * location from gps
     */

    public void requestGPSLocation() {
        broadcastGPSLocation();
    }

    private void broadcastGPSLocation() {
        PointWrapper gpsLocation = settingsManagerInstance.getLocationSettings().getGPSLocation();
        Intent intent = new Intent(Constants.ACTION_NEW_GPS_LOCATION);
        try {
            intent.putExtra(
                    Constants.ACTION_NEW_GPS_LOCATION_OBJECT, gpsLocation.toJson().toString());
        } catch (JSONException | NullPointerException e) {}
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override public void onLocationChanged(Location newLocationObject) {
        // try to create gps point from location object
        GPS.Builder gpsBuilder = new GPS.Builder(
                context, newLocationObject.getLatitude(), newLocationObject.getLongitude(), newLocationObject.getTime());

        // optional args
        if (newLocationObject.getProvider() != null) {
            gpsBuilder.setProvider(newLocationObject.getProvider());
        }
        if (newLocationObject.hasAccuracy()) {
            gpsBuilder.setAccuracy(newLocationObject.getAccuracy());
        }
        if (newLocationObject.hasAltitude()) {
            gpsBuilder.setAltitude(newLocationObject.getAltitude());
        }
        if (newLocationObject.getExtras() != null) {
            Bundle locationExtras = newLocationObject.getExtras();
            if (locationExtras.containsKey("satellites")
                    && locationExtras.getInt("satellites") >= 0) {
                gpsBuilder.setNumberOfSatellites(locationExtras.getInt("satellites"));
            }
        }
        if (newLocationObject.hasSpeed()) {
            // convert from m/s into km/h
            float speedInKMH = newLocationObject.getSpeed() * 3.6f;
            // add to builder
            gpsBuilder.setSpeed(speedInKMH);
            // traveling speed array update
            if (travelingSpeedArray != null) {
                System.arraycopy(travelingSpeedArray, 0, travelingSpeedArray, 1, travelingSpeedArray.length-1);
                travelingSpeedArray[0] = speedInKMH;
            }
        }

        // direction sub object
        if (newLocationObject.hasBearing()) {
            Direction.Builder directionBuilder = new Direction.Builder(
                    context, Math.round(newLocationObject.getBearing()))
                .setTime(newLocationObject.getTime());
            // accuracy rating
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                directionBuilder.setAccuracyRating(
                        getGPSBearingAccuracyRating(newLocationObject));
            }
            // add direction object
            Direction gpsDirection = directionBuilder.build();
            if (gpsDirection != null) {
                gpsBuilder.setDirection(gpsDirection);
                // debug: log to file
                if (BuildConfig.DEBUG) {
                    FileUtility.appendToLog(
                            context,
                            "gpsBearing",
                            String.format(
                                "new: bearing=%1$d, accuracy=%2$d\n",
                                gpsDirection.getBearing(),
                                gpsDirection.getAccuracyRating())
                            );
                }
            }
        }

        GPS currentLocation = null;
        PointWrapper pointWrapper = settingsManagerInstance.getLocationSettings().getGPSLocation();
        if (pointWrapper  != null
                && pointWrapper.getPoint() instanceof GPS) {
            currentLocation = (GPS) pointWrapper.getPoint();
        }
        GPS newLocation = gpsBuilder.build();
        if(isBetterLocation(currentLocation, newLocation)) {

            // save current location
            LocationSettings locationSettings = settingsManagerInstance.getLocationSettings();
            try {
                locationSettings.setGPSLocation(
                        new PointWrapper(this.context, gpsBuilder.toJson()));
            } catch (JSONException e) {
                return;
            }
            // broadcast new gps position action
            broadcastGPSLocation();
            // broadcast new location action
            if (! this.simulationEnabled) {
                broadcastCurrentLocation();
            }

            // first gps fix
            if (! gpsFixFound) {
                gpsFixFound = true;
                // notify user about first gps fix after app start
                vibrator.vibrate(new long[]{250, 50, 250, 50}, -1);
                // obtain the diff to true north
                if (newLocationObject.hasAltitude()) {
                    GeomagneticField geoField = new GeomagneticField(
                            Double.valueOf(newLocationObject.getLatitude()).floatValue(),
                            Double.valueOf(newLocationObject.getLongitude()).floatValue(),
                            Double.valueOf(newLocationObject.getAltitude()).floatValue(),
                            System.currentTimeMillis());
                    DirectionManager.getInstance(context).setDifferenceToTrueNorth(geoField.getDeclination());
                }
            }
        }
    }

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}

    private static boolean isBetterLocation(GPS currentLocation, GPS newLocation) {
        if (newLocation == null) {
            return false;
        } else if (currentLocation == null) {
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = newLocation.getTime() - currentLocation.getTime();
        boolean isNewer = timeDelta > 0;
        boolean isABitNewer = timeDelta > 10000;
        boolean isSignificantlyNewer = timeDelta > 20000;
        boolean isMuchMuchNewer = timeDelta > 180000;

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (newLocation.getAccuracy() - currentLocation.getAccuracy());
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
        boolean isFromSameProvider = newLocation.getProvider().equals(currentLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isNewer && isMoreAccurateThanThresholdValue) {
            return true;
        } else if (isNewer && isMoreAccurate && isFromSameProvider) {
            return true;
        } else if (isABitNewer && isABitLessAccurate && isFromSameProvider) {
            return true;
        } else if (isSignificantlyNewer && isSignificantlyLessAccurate) {
            return true;
        } else if (isMuchMuchNewer) {
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private int getGPSBearingAccuracyRating(Location location) {
        if (location.hasBearingAccuracy()) {
            int bearingAccuracyDegrees = Math.round(location.getBearingAccuracyDegrees());
            // debug: log to file
            if (BuildConfig.DEBUG) {
                FileUtility.appendToLog(
                        context,
                        "gpsBearing",
                        String.format("bearingAccuracyDegrees: %1$d", bearingAccuracyDegrees));
            }
            // return accuracy rating value
            if (bearingAccuracyDegrees < Direction.GPS_DIRECTION_HIGH_ACCURACY_THRESHOLD) {
                return Constants.DIRECTION_ACCURACY_RATING.HIGH;
            } else if (bearingAccuracyDegrees > Direction.GPS_DIRECTION_LOW_ACCURACY_THRESHOLD) {
                return Constants.DIRECTION_ACCURACY_RATING.LOW;
            } else {
                return Constants.DIRECTION_ACCURACY_RATING.MEDIUM;
            }
        } else {
            return Constants.DIRECTION_ACCURACY_RATING.UNKNOWN;
        }
    }


    /**
     * location from simulation
     */

    public void requestSimulatedLocation() {
        broadcastSimulatedLocation();
    }

    private void broadcastSimulatedLocation() {
        PointWrapper simulatedLocation = settingsManagerInstance.getLocationSettings().getSimulatedLocation();
        Intent intent = new Intent(Constants.ACTION_NEW_SIMULATED_LOCATION);
        try {
            intent.putExtra(
                    Constants.ACTION_NEW_SIMULATED_LOCATION_OBJECT, simulatedLocation.toJson().toString());
        } catch (JSONException | NullPointerException e) {}
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void setSimulatedLocation(PointWrapper newLocation) {
        if (newLocation != null) {
            LocationSettings locationSettings = this.settingsManagerInstance.getLocationSettings();
            locationSettings.setSimulatedLocation(newLocation);
            // add to simulated points profile
            AccessDatabase.getInstance(this.context).addFavoritePointToProfile(
                    newLocation, HistoryPointProfile.ID_SIMULATED_POINTS);
            // broadcast new simulated location action
            broadcastSimulatedLocation();
            // broadcast new location action
            if (this.simulationEnabled) {
                broadcastCurrentLocation();
            }
        }
    }

}
