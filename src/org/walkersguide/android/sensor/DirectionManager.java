package org.walkersguide.android.sensor;

import org.walkersguide.android.basic.point.GPS;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.DirectionSettings;

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

public class DirectionManager implements SensorEventListener {

    // shake detection
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 3;
    // update thresholds
    public static final int UPDATE_DIRECTION_THRESHOLD_1 = 22;            // 22 degree

    private Context context;
    private static DirectionManager directionManagerInstance;
    private SettingsManager settingsManagerInstance;

    // direction variables
    private int compassDirection, gpsDirection, manualDirection, lastDirection;
    private int directionSource;

    // sensor variables
    private SensorManager sensorManager;
    private float[] valuesAccelerometer, valuesMagneticField;
    private boolean hasAccelerometerData, hasMagneticFieldData;
    private int[] bearingOfMagneticField;

    // shake detection
    private int mShakeCount;
    private long mLastTime, mLastShake, mLastForce;

    public static DirectionManager getInstance(Context context) {
        if(directionManagerInstance == null){
            directionManagerInstance = new DirectionManager(context.getApplicationContext());
        }
        return directionManagerInstance;
    }

    private DirectionManager(Context context) {
        this.context = context;
        this.sensorManager = null;
        this.settingsManagerInstance = SettingsManager.getInstance(context);

        // load direction settings
        DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
        this.directionSource = directionSettings.getSelectedDirectionSource();
        this.compassDirection = directionSettings.getCompassDirection();
        this.gpsDirection = directionSettings.getGPSDirection();
        this.manualDirection = directionSettings.getManualDirection();
        this.lastDirection = 0;

        // compass specific variables
        valuesAccelerometer = new float[]{-1.0f,-1.0f,-1.0f};
        valuesMagneticField = new float[]{-1.0f,-1.0f,-1.0f};
        hasAccelerometerData = false;
        hasMagneticFieldData = false;
        bearingOfMagneticField = new int[5];
        for (int i : bearingOfMagneticField) {
            i = 0;
        }

        // listen for new gps position broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_GPS_POSITION);
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
    }


    /**
     * direction management
     */

    public int getCurrentDirection() {
        switch (this.directionSource) {
            case Constants.DIRECTION_SOURCE.COMPASS:
                return this.compassDirection;
            case Constants.DIRECTION_SOURCE.GPS:
                return this.gpsDirection;
            case Constants.DIRECTION_SOURCE.MANUAL:
                return this.manualDirection;
            default:
                return -1;
        }
    }

    public void stopSensors() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            sensorManager = null;
        }
    }

    public void startSensors() {
        if (sensorManager == null) {
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            // accelerometer sensor
            sensorManager.registerListener(
                    this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
            // magnetic field sensor
            sensorManager.registerListener(
                    this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }


    /**
     * direction source
     */

    public int getDirectionSource() {
        return this.directionSource;
    }

    public void setDirectionSource(int newDirectionSource) {
        if (Ints.contains(Constants.DirectionSourceValueArray, newDirectionSource)
                && this.directionSource != newDirectionSource) {
            this.directionSource = newDirectionSource;
            // save
            DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
            directionSettings.setSelectedDirectionSource(newDirectionSource);
            // new direction broadcast
            sendNewDirectionBroadcast();
        }
    }

    private void sendNewDirectionBroadcast() {
        Intent intent;
        switch (this.directionSource) {
            case Constants.DIRECTION_SOURCE.COMPASS:
                intent = new Intent(Constants.ACTION_NEW_DIRECTION);
                if (Math.abs(this.lastDirection-this.compassDirection) > UPDATE_DIRECTION_THRESHOLD_1) {
                    this.lastDirection = this.compassDirection;
                    intent.putExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_UPDATE_THRESHOLD, 1);
                } else {
                    intent.putExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_UPDATE_THRESHOLD, 0);
                }
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                break;
            case Constants.DIRECTION_SOURCE.GPS:
                intent = new Intent(Constants.ACTION_NEW_DIRECTION);
                if (Math.abs(this.lastDirection-this.gpsDirection) > UPDATE_DIRECTION_THRESHOLD_1) {
                    this.lastDirection = this.gpsDirection;
                    intent.putExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_UPDATE_THRESHOLD, 1);
                } else {
                    intent.putExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_UPDATE_THRESHOLD, 0);
                }
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                break;
            case Constants.DIRECTION_SOURCE.MANUAL:
                intent = new Intent(Constants.ACTION_NEW_DIRECTION);
                if (Math.abs(this.lastDirection-this.manualDirection) > UPDATE_DIRECTION_THRESHOLD_1) {
                    this.lastDirection = this.manualDirection;
                    intent.putExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_UPDATE_THRESHOLD, 1);
                } else {
                    intent.putExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_UPDATE_THRESHOLD, 0);
                }
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                break;
        }
    }


    /**
     * direction from compass
     * implements SensorEventListener
     */

    public int getCompassDirection() {
        return this.compassDirection;
    }

    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // first try to detect shaking
                calculateShakeIntensity(event.values);
                // then proceed with data collection for compass
                System.arraycopy(event.values, 0, valuesAccelerometer, 0, 3);
                hasAccelerometerData = true;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, valuesMagneticField, 0, 3);
                hasMagneticFieldData = true;
                break;
        }

        // calculate compass value
        if (hasAccelerometerData && hasMagneticFieldData) {
            float[] matrixR = new float[9];
            float[] matrixI = new float[9];
            float[] orientationValues = new float[3];
            boolean success = SensorManager.getRotationMatrix(
                    matrixR, matrixI, valuesAccelerometer, valuesMagneticField);
            if(success) {
                SensorManager.getOrientation(matrixR, orientationValues);
                System.arraycopy(bearingOfMagneticField, 0, bearingOfMagneticField, 1, bearingOfMagneticField.length-1);
                bearingOfMagneticField[0] = (
                        (int) Math.round(
                            Math.toDegrees(orientationValues[0])
                            + settingsManagerInstance.getDirectionSettings().getDifferenceToTrueNorth())
                        + 360) % 360;

                // calculate average compass value
                // Mitsuta method: http://abelian.org/vlf/bearings.html
                int sum = bearingOfMagneticField[0];
                int D = bearingOfMagneticField[0];
                int delta = 0;
                for (int i=1; i<bearingOfMagneticField.length; i++) {
                    delta = bearingOfMagneticField[i] - D;
                    if (delta < -180) {
                        D = D + delta + 360;
                    } else if (delta < 180) {
                        D = D + delta;
                    } else {
                        D = D + delta - 360;
                    }
                    sum += D;
                }
                int average = (((int) sum / bearingOfMagneticField.length) + 360) % 360;

                // decide, if we accept the compass value as new device wide bearing value
                if (this.compassDirection != average) {
                    this.compassDirection = average;
                    // save
                    DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
                    directionSettings.setCompassDirection(average);
                    // broadcast new compass direction action
                    Intent intent = new Intent(Constants.ACTION_NEW_COMPASS_DIRECTION);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    // new direction broadcast
                    if (this.directionSource == Constants.DIRECTION_SOURCE.COMPASS) {
                        sendNewDirectionBroadcast();
                    }
                }
            }
        }
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}


    /**
     * direction from gps
     */

    public int getGPSDirection() {
        return this.gpsDirection;
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_GPS_POSITION)) {
                GPS currentGPSPosition = PositionManager.getInstance(context).getCurrentGPSPosition();
                if (currentGPSPosition != null
                        && currentGPSPosition.getBearing() >= 0.0f) {
                    gpsDirection = Math.round(currentGPSPosition.getBearing());
                    // save
                    DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
                    directionSettings.setGPSDirection(gpsDirection);
                    // new direction broadcast
                    if (directionSource == Constants.DIRECTION_SOURCE.GPS) {
                        sendNewDirectionBroadcast();
                    }
                }
            }
        }
    };


    /**
     * manual
     */

    public int getManualDirection() {
        return this.manualDirection;
    }

    public void setManualDirection(int newDirection) {
        if (newDirection>= 0 && newDirection < 360) {
            this.manualDirection = newDirection;
            // save
            DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
            directionSettings.setManualDirection(newDirection);
            // new direction broadcast
            if (this.directionSource == Constants.DIRECTION_SOURCE.MANUAL) {
                sendNewDirectionBroadcast();
            }
        }
    }


    /**
     * shake detection
     */

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
