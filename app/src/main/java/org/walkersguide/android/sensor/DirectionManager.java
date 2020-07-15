package org.walkersguide.android.sensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.support.v4.content.LocalBroadcastManager;

import com.google.common.primitives.Ints;

import java.lang.NullPointerException;

import java.util.HashMap;

import org.json.JSONException;

import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.sensor.attribute.NewDirectionAttributes;
import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.data.sensor.threshold.BearingThreshold;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.DirectionSettings;

import timber.log.Timber;


public class DirectionManager implements SensorEventListener {

    private Context context;
    private static DirectionManager directionManagerInstance;
    private SettingsManager settingsManagerInstance;

    public static DirectionManager getInstance(Context context) {
        if(directionManagerInstance == null){
            directionManagerInstance = new DirectionManager(context.getApplicationContext());
        }
        return directionManagerInstance;
    }

    private DirectionManager(Context context) {
        this.context = context;
        this.settingsManagerInstance = SettingsManager.getInstance(context);
        // listen for new gps position broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_GPS_LOCATION);
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
    }


    /**
     * direction management
     */
    private SensorManager sensorManager = null;
    private boolean simulationEnabled = false;

    public void stopSensors() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            sensorManager = null;
        }
    }

    public void startSensors() {
        DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
        if (sensorManager == null) {
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            // accelerometer sensor (shake detection and fallback compass)
            sensorManager.registerListener(
                    this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
            // register rotation vector sensor if the device has a gyroscope, otherwise fall back to magnetic field sensor
            if (sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) {
                // rotation vector
                sensorManager.registerListener(
                        this,
                        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                        SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                // magnetic field sensor
                sensorManager.registerListener(
                        this,
                        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    public int getDirectionSource() {
        return settingsManagerInstance.getDirectionSettings().getSelectedDirectionSource();
    }

    public void setDirectionSource(int newDirectionSource) {
        DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
        if (Ints.contains(Constants.DirectionSourceValueArray, newDirectionSource)
                && directionSettings.getSelectedDirectionSource() != newDirectionSource) {
            directionSettings.setSelectedDirectionSource(newDirectionSource);
            broadcastCurrentDirection();
        }
    }

    public boolean getSimulationEnabled() {
        return this.simulationEnabled;
    }

    public void setSimulationEnabled(boolean enabled) {
        this.simulationEnabled = enabled;
        broadcastCurrentDirection();
    }


    /**
     * current direction
     */
    private HashMap<BearingThreshold,Direction> lastAggregatingDirectionMap = null;
    private Direction lastImmediateDirection = null;

    public Direction getCurrentDirection() {
        DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
        if (this.simulationEnabled) {
            return directionSettings.getSimulatedDirection();
        } else {
            switch (getDirectionSource()) {
                case Constants.DIRECTION_SOURCE.COMPASS:
                    return directionSettings.getCompassDirection();
                case Constants.DIRECTION_SOURCE.GPS:
                    return directionSettings.getGPSDirection();
                default:
                    return null;
            }
        }
    }

    public void requestCurrentDirection() {
        broadcastCurrentDirection();
    }

    private void broadcastCurrentDirection() {
        Direction currentDirection = getCurrentDirection();
        NewDirectionAttributes.Builder newDirectionAttributesBuilder = new NewDirectionAttributes.Builder(context, currentDirection);

        // add optional attributes
        if (currentDirection != null) {

            // aggregating threshold
            if (lastAggregatingDirectionMap == null) {
                // initialize
                lastAggregatingDirectionMap = new HashMap<BearingThreshold,Direction>();;
                for (BearingThreshold bearingThreshold : BearingThreshold.values()) {
                    lastAggregatingDirectionMap.put(bearingThreshold, currentDirection);
                }
                newDirectionAttributesBuilder.setAggregatingBearingThreshold(BearingThreshold.ZERO_DEGREES);
            } else {
                for (BearingThreshold bearingThreshold : BearingThreshold.values()) {
                    if (Math.abs( lastAggregatingDirectionMap.get(bearingThreshold).getBearing() - currentDirection.getBearing() ) > bearingThreshold.getBearingThresholdInDegrees()) {
                        lastAggregatingDirectionMap.put(bearingThreshold, currentDirection);
                        newDirectionAttributesBuilder.setAggregatingBearingThreshold(bearingThreshold);
                    }
                }
            }

            // immediate threshold
            if (lastImmediateDirection == null) {
                // initialize
                lastImmediateDirection = currentDirection;
                newDirectionAttributesBuilder.setImmediateBearingThreshold(BearingThreshold.ZERO_DEGREES);
            } else {
                for (BearingThreshold bearingThreshold : BearingThreshold.values()) {
                    if (Math.abs( lastImmediateDirection.getBearing() - currentDirection.getBearing() ) > bearingThreshold.getBearingThresholdInDegrees()) {
                        newDirectionAttributesBuilder.setImmediateBearingThreshold(bearingThreshold);
                    }
                }
                lastImmediateDirection = currentDirection;
            }
        }

        // send intent
        Intent intent = new Intent(Constants.ACTION_NEW_DIRECTION);
        intent.putExtra(
                Constants.ACTION_NEW_DIRECTION_ATTRIBUTES, newDirectionAttributesBuilder.toJson().toString());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


    /**
     * direction from compass
     * implements SensorEventListener
     */
    // time between compass values
    private static final int MIN_COMPASS_VALUE_DELAY = 250;          // 250 ms

    // sensors
    private int sensorAccuracyRating = Constants.DIRECTION_ACCURACY_RATING.UNKNOWN;
    // accelerometer
    private float[] valuesAccelerometer = new float[3];
    // compass
    private int[] compassDirectionArray = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private float differenceToTrueNorth = 0.0f;

    public void setDifferenceToTrueNorth(float newDifference) {
        this.differenceToTrueNorth = newDifference;
    }

    public void requestCompassDirection() {
        broadcastCompassDirection();
    }

    private void broadcastCompassDirection() {
        Direction compassDirection = settingsManagerInstance.getDirectionSettings().getCompassDirection();
        Intent intent = new Intent(Constants.ACTION_NEW_COMPASS_DIRECTION);
        try {
            intent.putExtra(
                    Constants.ACTION_NEW_COMPASS_DIRECTION_OBJECT, compassDirection.toJson().toString());
        } catch (JSONException | NullPointerException e) {}
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {

            case Sensor.TYPE_ACCELEROMETER:
                // try to detect device shaking
                calculateShakeIntensity(event.values);
                // accelerometer value array is required for compass without gyroscope
                System.arraycopy(event.values, 0, valuesAccelerometer, 0, 3);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_ROTATION_VECTOR:
                // get new compass value
                float[] orientationValues = new float[3];

                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    // compass without gyroscope for old devices
                    float[] matrixR = new float[9];
                    float[] matrixI = new float[9];
                    SensorManager.getRotationMatrix(
                            matrixR, matrixI, valuesAccelerometer, event.values);
                    SensorManager.getOrientation(matrixR, orientationValues);
                    // swap x and z axis if the smartphone stands upright
                    if (isPhoneUpright(orientationValues)) {
                        SensorManager.remapCoordinateSystem(
                                matrixR, SensorManager.AXIS_X, SensorManager.AXIS_Z, matrixR);
                        SensorManager.getOrientation(matrixR, orientationValues);
                    }

                } else {
                    // gyroscope included -> better compass accuracy
                    float[] matrixRotation = new float[16];
                    SensorManager.getRotationMatrixFromVector(
                            matrixRotation, event.values);
                    SensorManager.getOrientation(matrixRotation, orientationValues);
                    // swap x and z axis if the smartphone stands upright
                    if (isPhoneUpright(orientationValues)) {
                        //Timber.d("vertical");
                        SensorManager.remapCoordinateSystem(
                                matrixRotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, matrixRotation);
                        SensorManager.getOrientation(matrixRotation, orientationValues);
                    }
                }

                System.arraycopy(compassDirectionArray, 0, compassDirectionArray, 1, compassDirectionArray.length-1);
                compassDirectionArray[0] = radianToDegree(orientationValues[0]);

                // calculate average compass value
                // Mitsuta method: http://abelian.org/vlf/bearings.html
                int sum = compassDirectionArray[0];
                int D = compassDirectionArray[0];
                int delta = 0;
                for (int i=1; i<compassDirectionArray.length; i++) {
                    delta = compassDirectionArray[i] - D;
                    if (delta < -180) {
                        D = D + delta + 360;
                    } else if (delta < 180) {
                        D = D + delta;
                    } else {
                        D = D + delta - 360;
                    }
                    sum += D;
                }
                int average = (((int) sum / compassDirectionArray.length) + 360) % 360;

                // decide, if we accept the compass value as new device wide bearing value
                DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
                Direction currentCompassDirection = directionSettings.getCompassDirection();
                Direction newCompassDirection = new Direction.Builder(
                        context, average)
                    .setTime(System.currentTimeMillis())
                    .setAccuracyRating(sensorAccuracyRating)
                    .build();
                if (newCompassDirection != null
                        && (
                            currentCompassDirection == null
                            || (
                                currentCompassDirection.getBearing() != newCompassDirection.getBearing()
                                && (System.currentTimeMillis()-currentCompassDirection.getTime()) > MIN_COMPASS_VALUE_DELAY)
                           )) {
                    directionSettings.setCompassDirection(newCompassDirection);
                    // broadcast new compass direction action
                    broadcastCompassDirection();
                    // new direction broadcast
                    if (! this.simulationEnabled
                            && getDirectionSource() == Constants.DIRECTION_SOURCE.COMPASS) {
                        broadcastCurrentDirection();
                            }
                           }
                break;
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            Timber.d("onAccuracyChanged: %1$s=%2$d", sensor.getStringType(), accuracy);
        }
        switch (sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_MAGNETIC_FIELD:
                switch (accuracy) {
                    case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                        sensorAccuracyRating = Constants.DIRECTION_ACCURACY_RATING.HIGH;
                        break;
                    case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                        sensorAccuracyRating = Constants.DIRECTION_ACCURACY_RATING.MEDIUM;
                        break;
                    default:
                        sensorAccuracyRating = Constants.DIRECTION_ACCURACY_RATING.LOW;
                        break;
                }
                break;
        }
    }

    private boolean isPhoneUpright(float[] orientationValues) {
        int pitch = radianToDegree(orientationValues[1]);
        int roll = radianToDegree(orientationValues[2]);
        //Timber.d("%1$d", roll);
        //Timber.d("%1$d; %2$d; %3$d", radianToDegree(orientationValues[0]), pitch, roll);
        return (pitch >= 65 && pitch <= 115)
            || (pitch >= 245 && pitch <= 295);
    }

    private int radianToDegree(float radian) {
        return (
                (int) Math.round(
                      Math.toDegrees(radian)
                    + differenceToTrueNorth)
                + 360)
               % 360;
    }


    /**
     * direction from gps
     */

    public void requestGPSDirection() {
        broadcastGPSDirection();
    }

    private void broadcastGPSDirection() {
        Direction gpsDirection = settingsManagerInstance.getDirectionSettings().getGPSDirection();
        Intent intent = new Intent(Constants.ACTION_NEW_GPS_DIRECTION);
        try {
            intent.putExtra(
                    Constants.ACTION_NEW_GPS_DIRECTION_OBJECT, gpsDirection.toJson().toString());
        } catch (JSONException | NullPointerException e) {}
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_GPS_LOCATION)) {
                PointWrapper pointWrapper = PointWrapper.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_GPS_LOCATION_OBJECT));
                if (pointWrapper  != null
                        && pointWrapper.getPoint() instanceof GPS) {
                    GPS gps = (GPS) pointWrapper.getPoint();
                    if (gps.getDirection() != null) {
                        DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
                        directionSettings.setGPSDirection(gps.getDirection());
                        // broadcast new gps direction action
                        broadcastGPSDirection();
                        // new direction broadcast
                        if (! simulationEnabled
                                && getDirectionSource() == Constants.DIRECTION_SOURCE.GPS) {
                            broadcastCurrentDirection();
                        }
                    }
                }
            }
        }
    };


    /**
     * simulated direction
     */

    public void requestSimulatedDirection() {
        broadcastSimulatedDirection();
    }

    private void broadcastSimulatedDirection() {
        Direction simulatedDirection = settingsManagerInstance.getDirectionSettings().getSimulatedDirection();
        Intent intent = new Intent(Constants.ACTION_NEW_SIMULATED_DIRECTION);
        try {
            intent.putExtra(
                    Constants.ACTION_NEW_SIMULATED_DIRECTION_OBJECT, simulatedDirection.toJson().toString());
        } catch (JSONException | NullPointerException e) {}
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void setSimulatedDirection(Direction newDirection) {
        if (newDirection != null) {
            DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
            directionSettings.setSimulatedDirection(newDirection);
            // broadcast new simulated direction action
            broadcastSimulatedDirection();
            // broadcast new direction action
            if (this.simulationEnabled) {
                broadcastCurrentDirection();
            }
        }
    }


    /**
     * shake detection
     */
    // shake constants
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 3;
    // shake detection
    private int mShakeCount;
    private long mLastTime, mLastShake, mLastForce;

    private void calculateShakeIntensity(float[] newAccelerometerValues) {
        long now = System.currentTimeMillis();
        if ((now - mLastForce) > SHAKE_TIMEOUT) {
            mShakeCount = 0;
        }
        if ((now - mLastTime) > TIME_THRESHOLD) {
            long diff = now - mLastTime;
            float speed = Math.abs(
                    newAccelerometerValues[0] + newAccelerometerValues[1]
                    + newAccelerometerValues[2] - valuesAccelerometer[0]
                    - valuesAccelerometer[1] - valuesAccelerometer[2]) / diff * 10000;
            if (speed > settingsManagerInstance.getGeneralSettings().getShakeIntensity()) {
                if ((++mShakeCount >= SHAKE_COUNT)
                        && (now - mLastShake > SHAKE_DURATION)) {
                    mLastShake = now;
                    mShakeCount = 0;
                    // broadcast shake detected
                    Intent intent = new Intent(Constants.ACTION_SHAKE_DETECTED);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
                mLastForce = now;
            }
            mLastTime = now;
        }
    }

}
