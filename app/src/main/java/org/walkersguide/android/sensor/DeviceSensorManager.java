package org.walkersguide.android.sensor;

import org.walkersguide.android.R;
import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.util.Helper;
import timber.log.Timber;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.bearing.BearingSensorValue;
import android.hardware.GeomagneticField;
import org.walkersguide.android.sensor.bearing.BearingSensor;
import org.walkersguide.android.sensor.bearing.BearingSensorAccuracyRating;
import org.walkersguide.android.util.GlobalInstance;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;





import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.util.SettingsManager;
import android.view.Display;
import android.view.WindowManager;
import android.view.Surface;
import android.os.Handler;
import android.os.Looper;


public class DeviceSensorManager implements SensorEventListener {

    public interface DeviceSensorUpdate {
        public void newBearing(Bearing bearing, boolean isImportant);
        public void shakeDetected();
    }

    private DeviceSensorUpdate deviceSensorUpdateListener;

    public void setDeviceSensorUpdateListener(DeviceSensorUpdate listener) {
        this.deviceSensorUpdateListener = listener;
    }


    /**
     * singleton
     */

    private static DeviceSensorManager deviceSensorManagerInstance;
    private SettingsManager settingsManagerInstance;
    private Display display;

    public static DeviceSensorManager getInstance() {
        if (deviceSensorManagerInstance == null){
            deviceSensorManagerInstance = getInstanceSynchronized();
        }
        return deviceSensorManagerInstance;
    }

    private static synchronized DeviceSensorManager getInstanceSynchronized() {
        if (deviceSensorManagerInstance == null){
            deviceSensorManagerInstance = new DeviceSensorManager();
        }
        return deviceSensorManagerInstance;
    }

    private DeviceSensorManager() {
        this.settingsManagerInstance = SettingsManager.getInstance();
        this.display = ((WindowManager) GlobalInstance.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }


    /**
     * start and stop sensor updates
     */
    public static final String ACTION_BEARING_SENSOR_CHANGED = "bearingSensorChanged";

    private SensorManager sensorManager = null;
    private AutoSwitchBearingSourceHysteresis autoSwitchBearingSourceHysteresis;
    private boolean deviceUpright;

    public void startSensors() {
        if (sensorManager == null) {
            sensorManager = (SensorManager) GlobalInstance.getContext().getSystemService(Context.SENSOR_SERVICE);
            autoSwitchBearingSourceHysteresis = new AutoSwitchBearingSourceHysteresis();
            deviceUpright = false;

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

            // gps location updates
            IntentFilter filter = new IntentFilter();
            filter.addAction(PositionManager.ACTION_NEW_GPS_LOCATION);
            LocalBroadcastManager.getInstance(GlobalInstance.getContext()).registerReceiver(mMessageReceiver, filter);

            // set defaults
            if (getBearingValueFromCompass() == null) {
                settingsManagerInstance.setBearingSensorValue(
                        BearingSensor.COMPASS,
                        new BearingSensorValue(
                            0, System.currentTimeMillis(), BearingSensorAccuracyRating.LOW));
            }
            if (getBearingValueFromSatellite() == null
                    && getSelectedBearingSensor() == BearingSensor.SATELLITE) {
                settingsManagerInstance.setSelectedBearingSensor(BearingSensor.COMPASS);
            }
        }
    }

    public void stopSensors() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            sensorManager = null;
            autoSwitchBearingSourceHysteresis.unregisterHandlers();
            autoSwitchBearingSourceHysteresis = null;
            deviceUpright = false;

            LocalBroadcastManager.getInstance(GlobalInstance.getContext()).unregisterReceiver(mMessageReceiver);
        }
    }

    public BearingSensor getSelectedBearingSensor() {
        return settingsManagerInstance.getSelectedBearingSensor();
    }

    public void setSelectedBearingSensor(BearingSensor newBearingSensor) {
        if (newBearingSensor != null) {
            settingsManagerInstance.setSelectedBearingSensor(newBearingSensor);
            LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(new Intent(ACTION_BEARING_SENSOR_CHANGED));
            broadcastCurrentBearing(true);
        }
    }

    public boolean isDeviceUpright() {
        return this.deviceUpright;
    }

    private void updateDeviceUpright(float[] orientationValues) {
        // compass: degree (z-axis)
        //Timber.d("degree: %1$.1f", orientationValues[0]);
        // vertically upright: pitch (y-axis)
        //Timber.d("pitch: %1$.1f", orientationValues[1]);
        boolean verticallyUpright = orientationValues[1] < -1.11 || orientationValues[1] > 1.11;
        // horizontally upright: roll (x-axis)
        //Timber.d("roll: %1$.1f", orientationValues[2]);
        boolean horizontallyUpright = (orientationValues[2] > -1.95 && orientationValues[2] < -1.35)
                                            || (orientationValues[2] > 1.35 &&  orientationValues[2] < 1.95);

        boolean newDeviceUpright = verticallyUpright || horizontallyUpright;
        if (newDeviceUpright != this.deviceUpright) {
            this.deviceUpright = newDeviceUpright;
            if (settingsManagerInstance.getAutoSwitchBearingSourceEnabled()) {
                this.autoSwitchBearingSourceHysteresis.deviceOrientationChanged();
            }
        }
    }


    private class AutoSwitchBearingSourceHysteresis {
        private static final long THRESHOLD_IN_MS = 5000l;

        private Handler handler;
        private Runnable switchToCompass, switchToSatellite;

        public AutoSwitchBearingSourceHysteresis() {
            this.handler = new Handler(Looper.getMainLooper());
            this.switchToCompass = new Runnable() {
                @Override public void run() {
                    setSelectedBearingSensor(BearingSensor.COMPASS);
                    announceBearingSensorChange();
                }
            };
            this.switchToSatellite = new Runnable() {
                @Override public void run() {
                    setSelectedBearingSensor(BearingSensor.SATELLITE);
                    announceBearingSensorChange();
                }
            };
        }

        private void announceBearingSensorChange() {
            TTSWrapper.getInstance().announce(
                    String.format(
                        "%1$s: %2$s",
                        GlobalInstance.getStringResource(R.string.bearingSensor),
                        getSelectedBearingSensor())
                    );
        }

        public void deviceOrientationChanged() {
            switch (getSelectedBearingSensor()) {
                case COMPASS:
                    if (isDeviceUpright()) {
                        this.handler.postDelayed(this.switchToSatellite, THRESHOLD_IN_MS);
                    } else {
                        this.handler.removeCallbacks(this.switchToSatellite);
                    }
                    break;
                case SATELLITE:
                    if (! isDeviceUpright()) {
                        this.handler.postDelayed(this.switchToCompass, THRESHOLD_IN_MS);
                    } else {
                        this.handler.removeCallbacks(this.switchToCompass);
                    }
                    break;
            }
        }

        public void unregisterHandlers() {
            this.handler.removeCallbacks(this.switchToSatellite);
            this.handler.removeCallbacks(this.switchToCompass);
        }
    }


    /**
     * current bearing
     */
    public static final String ACTION_NEW_BEARING = "new_bearing";
    public static final String EXTRA_BEARING = "bearing";
    public static final String EXTRA_IS_IMPORTANT = "isImportant";

    public Bearing getCurrentBearing() {
        if (this.simulationEnabled) {
            return getSimulatedBearing();
        } else {
            switch (getSelectedBearingSensor()) {
                case COMPASS:
                    return getBearingValueFromCompass();
                case SATELLITE:
                    return getBearingValueFromSatellite();
                default:
                    return null;
            }
        }
    }

    public boolean hasCurrentBearing() {
        return getCurrentBearing() != null;
    }

    public void requestCurrentBearing() {
        broadcastCurrentBearing(true);
    }

    private void broadcastCurrentBearing(boolean isImportant) {
        Bearing currentBearing = getCurrentBearing();
        if (deviceSensorUpdateListener != null) {
            deviceSensorUpdateListener.newBearing(currentBearing, isImportant);
        }

        Intent intent = new Intent(ACTION_NEW_BEARING);
        intent.putExtra(EXTRA_BEARING, currentBearing);
        intent.putExtra(EXTRA_IS_IMPORTANT, isImportant);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }


    /**
     * compass bearing
     * implements SensorEventListener
     */
    public static final String ACTION_NEW_BEARING_VALUE_FROM_COMPASS = "new_bearing_value_from_compass";

    // min time difference between compass values
    private static final int MIN_COMPASS_VALUE_DELAY = 250;          // 250 ms

    private BearingSensorAccuracyRating bearingSensorAccuracyRating = null;
    // accelerometer
    private float[] valuesAccelerometer = new float[3];
    // compass
    private int[] bearingValueFromCompassArray = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private float differenceToTrueNorth = 0.0f;

    public void requestBearingValueFromCompass() {
        broadcastBearingValueFromCompass();
    }

    private void broadcastBearingValueFromCompass() {
        Intent intent = new Intent(ACTION_NEW_BEARING_VALUE_FROM_COMPASS);
        intent.putExtra(EXTRA_BEARING, getBearingValueFromCompass());
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    public BearingSensorValue getBearingValueFromCompass() {
        return settingsManagerInstance.getBearingSensorValue(BearingSensor.COMPASS);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {

            case Sensor.TYPE_ACCELEROMETER:
                // try to detect device shaking
                calculateShakeIntensity(event.values);
                // accelerometer value array is required for compass without gyroscope
                System.arraycopy(event.values, 0, valuesAccelerometer, 0, 3);
                // upside down
                float z = event.values[2];
                if (z < -9.0) {
                    // device is upside down
                }
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
                    updateDeviceUpright(orientationValues);

                    // swap x and z axis if the smartphone stands upright
                    if (isDeviceUpright()) {
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
                    updateDeviceUpright(orientationValues);

                    // swap x and z axis if the smartphone stands upright
                    if (isDeviceUpright()) {
                        SensorManager.remapCoordinateSystem(
                                matrixRotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, matrixRotation);
                        SensorManager.getOrientation(matrixRotation, orientationValues);
                    }
                }

                System.arraycopy(
                        bearingValueFromCompassArray, 0,
                        bearingValueFromCompassArray, 1,
                        bearingValueFromCompassArray.length - 1);
                bearingValueFromCompassArray[0] = radianToDegree(orientationValues[0]);

                // calculate average compass value
                // Mitsuta method: http://abelian.org/vlf/bearings.html
                int sum = bearingValueFromCompassArray[0];
                int D = bearingValueFromCompassArray[0];
                int delta = 0;
                for (int i=1; i<bearingValueFromCompassArray.length; i++) {
                    delta = bearingValueFromCompassArray[i] - D;
                    if (delta < -180) {
                        D = D + delta + 360;
                    } else if (delta < 180) {
                        D = D + delta;
                    } else {
                        D = D + delta - 360;
                    }
                    sum += D;
                }

                int newBearingValueFromCompassInDegree = (int) sum / bearingValueFromCompassArray.length;
                if (display.getRotation() == Surface.ROTATION_90) {
                    // device turned left
                    newBearingValueFromCompassInDegree += 90;
                } else if (display.getRotation() == Surface.ROTATION_270) {
                    // device turned right
                    newBearingValueFromCompassInDegree -= 90;
                }
                newBearingValueFromCompassInDegree = (newBearingValueFromCompassInDegree + 360) % 360;

                // decide, if we accept the compass value as new device wide bearing value
                BearingSensorValue currentBearingValueFromCompass = getBearingValueFromCompass();
                BearingSensorValue newBearingValueFromCompass = new BearingSensorValue(
                        newBearingValueFromCompassInDegree,
                        System.currentTimeMillis(),
                        bearingSensorAccuracyRating);
                if (! newBearingValueFromCompass.equals(currentBearingValueFromCompass)
                        && (
                                currentBearingValueFromCompass == null
                            || (System.currentTimeMillis() - currentBearingValueFromCompass.getTimestamp()) > MIN_COMPASS_VALUE_DELAY)) {
                    settingsManagerInstance.setBearingSensorValue(
                            BearingSensor.COMPASS, newBearingValueFromCompass);

                    // broadcast new compass value action
                    broadcastBearingValueFromCompass();
                    // new bearing broadcast
                    if (! this.simulationEnabled
                            && getSelectedBearingSensor() == BearingSensor.COMPASS) {
                        broadcastCurrentBearing(false);
                    }
                }
                break;
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        switch (sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_MAGNETIC_FIELD:
                switch (accuracy) {
                    case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                        bearingSensorAccuracyRating = BearingSensorAccuracyRating.HIGH;
                        break;
                    case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                        bearingSensorAccuracyRating = BearingSensorAccuracyRating.MEDIUM;
                        break;
                    case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                    case SensorManager.SENSOR_STATUS_UNRELIABLE:
                        bearingSensorAccuracyRating = BearingSensorAccuracyRating.LOW;
                        break;
                    default:
                        bearingSensorAccuracyRating = null;
                        break;
                }
                break;
        }
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
     * gps bearing
     */
    public static final String ACTION_NEW_BEARING_VALUE_FROM_SATELLITE = "new_bearing_value_from_satellite";

    public void requestBearingValueFromSatellite() {
        broadcastBearingValueFromSatellite();
    }

    private void broadcastBearingValueFromSatellite() {
        Intent intent = new Intent(ACTION_NEW_BEARING_VALUE_FROM_SATELLITE);
        intent.putExtra(EXTRA_BEARING, getBearingValueFromSatellite());
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    public BearingSensorValue getBearingValueFromSatellite() {
        return settingsManagerInstance.getBearingSensorValue(BearingSensor.SATELLITE);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PositionManager.ACTION_NEW_GPS_LOCATION)) {
                GPS gps = (GPS) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION);
                if (gps != null
                        && gps.getBearing() != null
                        && gps.getSpeed() != null
                        && gps.getSpeed() > 0.66) {     // in km/h
                    settingsManagerInstance.setBearingSensorValue(
                            BearingSensor.SATELLITE, gps.getBearing());

                    // broadcasts
                    broadcastBearingValueFromSatellite();
                    if (! simulationEnabled
                            && getSelectedBearingSensor() == BearingSensor.SATELLITE) {
                        broadcastCurrentBearing(false);
                    }

                    // obtain the diff to true north
                    if (differenceToTrueNorth == 0.0f
                            && gps.getAltitude() != null) {
                        GeomagneticField geoField = new GeomagneticField(
                                Double.valueOf(
                                        gps.getCoordinates().getLatitude())
                                    .floatValue(),
                                Double.valueOf(
                                        gps.getCoordinates().getLongitude())
                                    .floatValue(),
                                Double.valueOf(
                                        gps.getAltitude())
                                    .floatValue(),
                                System.currentTimeMillis());
                        differenceToTrueNorth = geoField.getDeclination();
                    }
                }
            }
        }
    };


    /**
     * simulated bearing
     */

    private boolean simulationEnabled = false;

    public boolean getSimulationEnabled() {
        return this.simulationEnabled;
    }

    public void setSimulationEnabled(boolean enabled) {
        this.simulationEnabled = enabled;
        broadcastCurrentBearing(true);
    }

    // change simulated bearing
    public static final String ACTION_NEW_SIMULATED_BEARING = "new_simulated_bearing";

    public void requestSimulatedBearing() {
        broadcastSimulatedBearing();
    }

    private void broadcastSimulatedBearing() {
        Intent intent = new Intent(ACTION_NEW_SIMULATED_BEARING);
        intent.putExtra(EXTRA_BEARING, getSimulatedBearing());
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    public Bearing getSimulatedBearing() {
        return settingsManagerInstance.getSimulatedBearing();
    }

    public void setSimulatedBearing(Bearing newBearing) {
        if (newBearing != null) {
            settingsManagerInstance.setSimulatedBearing(newBearing);
            // broadcast new simulated bearing action
            broadcastSimulatedBearing();
            // broadcast new bearing action
            if (this.simulationEnabled) {
                broadcastCurrentBearing(true);
            }
        }
    }


    /**
     * shake detection
     */
    public static final String ACTION_SHAKE_DETECTED = "shake_detected";

    // shake constants
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 3;

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

            if (speed > settingsManagerInstance.getSelectedShakeIntensity().threshold) {
                if ((++mShakeCount >= SHAKE_COUNT)
                        && (now - mLastShake > SHAKE_DURATION)) {
                    mLastShake = now;
                    mShakeCount = 0;

                    if (deviceSensorUpdateListener != null) {
                        deviceSensorUpdateListener.shakeDetected();
                    }

                    // broadcast shake detected
                    Intent intent = new Intent(ACTION_SHAKE_DETECTED);
                    LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
                }
                mLastForce = now;
            }

            mLastTime = now;
        }
    }

}
