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

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.DirectionSettings;


public class DirectionManager implements SensorEventListener {

    // direction thresholds
    public interface THRESHOLD0 {
        public static final int ID = 0;
        public static final int BEARING = 0;
    }
    public interface THRESHOLD1 {
        public static final int ID = 1;
        public static final int BEARING = 11;
    }
    public interface THRESHOLD2 {
        public static final int ID = 2;
        public static final int BEARING = 22;
    }

    // time between compass values
    public static final int MIN_COMPASS_VALUE_DELAY = 250;          // 250 ms

    // shake detection
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 3;

    private Context context;
    private static DirectionManager directionManagerInstance;
    private SettingsManager settingsManagerInstance;

    // direction variables
    private HashMap<Integer,Integer> lastDirectionMap;

    // sensor variables
    private SensorManager sensorManager;
    private float[] valuesAccelerometer, valuesMagneticField;
    private boolean hasAccelerometerData, hasMagneticFieldData;
    private int[] compassDirectionArray;
    private long timeOfLastCompassDirection;

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

        // last direction map
        this.lastDirectionMap = new HashMap<Integer,Integer>();

        // compass specific variables
        valuesAccelerometer = new float[]{-1.0f, -1.0f, -1.0f};
        valuesMagneticField = new float[]{-1.0f, -1.0f, -1.0f};
        hasAccelerometerData = false;
        hasMagneticFieldData = false;
        compassDirectionArray = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        timeOfLastCompassDirection = System.currentTimeMillis();

        // listen for new gps position broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_GPS_LOCATION);
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
    }


    /**
     * direction management
     */

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
            // reset direction source if simulation
            DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
            if (directionSettings.getSelectedDirectionSource() == Constants.DIRECTION_SOURCE.SIMULATION) {
                setDirectionSource(
                        directionSettings.getPreviousDirectionSource());
            }
        }
    }

    public int getDirectionSource() {
        return settingsManagerInstance.getDirectionSettings().getSelectedDirectionSource();
    }

    public int getPreviousDirectionSource() {
        return settingsManagerInstance.getDirectionSettings().getPreviousDirectionSource();
    }

    public void setDirectionSource(int newDirectionSource) {
        DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
        if (Ints.contains(Constants.DirectionSourceValueArray, newDirectionSource)
                && directionSettings.getSelectedDirectionSource() != newDirectionSource) {
            // new direction broadcast
            if (directionSettings.getSelectedDirectionSource() != Constants.DIRECTION_SOURCE.SIMULATION) {
                directionSettings.setPreviousDirectionSource(
                        directionSettings.getSelectedDirectionSource());
            }
            directionSettings.setSelectedDirectionSource(newDirectionSource);
            broadcastCurrentDirection();
        }
    }


    /**
     * current direction
     */

    public int getCurrentDirection() {
        DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
        switch (directionSettings.getSelectedDirectionSource()) {
            case Constants.DIRECTION_SOURCE.COMPASS:
                return directionSettings.getCompassDirection();
            case Constants.DIRECTION_SOURCE.GPS:
                return directionSettings.getGPSDirection();
            case Constants.DIRECTION_SOURCE.SIMULATION:
                return directionSettings.getSimulatedDirection();
            default:
                return -1;
        }
    }

    public void requestCurrentDirection() {
        broadcastCurrentDirection();
    }

    private void broadcastCurrentDirection() {
        int currentDirection = getCurrentDirection();
        if (currentDirection != -1) {
            Intent intent = new Intent(Constants.ACTION_NEW_DIRECTION);
            // direction value
            intent.putExtra(
                    Constants.ACTION_NEW_DIRECTION_ATTR.INT_DIRECTION_IN_DEGREE,
                    currentDirection);
            // source
            intent.putExtra(
                    Constants.ACTION_NEW_DIRECTION_ATTR.INT_SOURCE,
                    settingsManagerInstance.getDirectionSettings().getSelectedDirectionSource());
            // bearing threshold
            intent.putExtra(
                    Constants.ACTION_NEW_DIRECTION_ATTR.INT_THRESHOLD_ID,
                    THRESHOLD0.ID);
            if (lastDirectionMap.get(THRESHOLD1.ID) == null
                    || Math.abs(lastDirectionMap.get(THRESHOLD1.ID)-currentDirection) > THRESHOLD1.BEARING) {
                lastDirectionMap.put(THRESHOLD1.ID, currentDirection);
                intent.putExtra(
                        Constants.ACTION_NEW_DIRECTION_ATTR.INT_THRESHOLD_ID,
                        THRESHOLD1.ID);
            }
            if (lastDirectionMap.get(THRESHOLD2.ID) == null
                    || Math.abs(lastDirectionMap.get(THRESHOLD2.ID)-currentDirection) > THRESHOLD2.BEARING) {
                lastDirectionMap.put(THRESHOLD2.ID, currentDirection);
                intent.putExtra(
                        Constants.ACTION_NEW_DIRECTION_ATTR.INT_THRESHOLD_ID,
                        THRESHOLD2.ID);
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }


    /**
     * direction from compass
     * implements SensorEventListener
     */

    public void requestCompassDirection() {
        broadcastCompassDirection();
    }

    private void broadcastCompassDirection() {
        DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
        if (directionSettings.getCompassDirection() != -1) {
            Intent intent = new Intent(Constants.ACTION_NEW_COMPASS_DIRECTION);
            intent.putExtra(
                    Constants.ACTION_NEW_DIRECTION_ATTR.INT_DIRECTION_IN_DEGREE,
                    directionSettings.getCompassDirection());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
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
                DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();

                // calculate new compass value
                SensorManager.getOrientation(matrixR, orientationValues);
                System.arraycopy(compassDirectionArray, 0, compassDirectionArray, 1, compassDirectionArray.length-1);
                compassDirectionArray[0] = (
                        (int) Math.round(
                            Math.toDegrees(orientationValues[0])
                            + directionSettings.getDifferenceToTrueNorth())
                        + 360) % 360;

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
                if (directionSettings.getCompassDirection() != average
                        && (System.currentTimeMillis()-this.timeOfLastCompassDirection) > MIN_COMPASS_VALUE_DELAY) {
                    directionSettings.setCompassDirection(average);
                    this.timeOfLastCompassDirection = System.currentTimeMillis();
                    // broadcast new compass direction action
                    broadcastCompassDirection();
                    // new direction broadcast
                    if (directionSettings.getSelectedDirectionSource() == Constants.DIRECTION_SOURCE.COMPASS) {
                        broadcastCurrentDirection();
                    }
                }
            }
        }
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}


    /**
     * direction from gps
     */

    public void requestGPSDirection() {
        broadcastGPSDirection();
    }

    private void broadcastGPSDirection() {
        DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
        if (directionSettings.getGPSDirection() != -1) {
            Intent intent = new Intent(Constants.ACTION_NEW_GPS_DIRECTION);
            intent.putExtra(
                    Constants.ACTION_NEW_DIRECTION_ATTR.INT_DIRECTION_IN_DEGREE,
                    directionSettings.getGPSDirection());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_GPS_LOCATION)) {
                GPS gpsLocation = null;
                try {
                    gpsLocation = new GPS(
                            context,
                            new JSONObject(
                                intent.getStringExtra(Constants.ACTION_NEW_GPS_LOCATION_ATTR.STRING_POINT_SERIALIZED))
                            );
                } catch (JSONException e) {
                    gpsLocation = null;
                } finally {
                    if (gpsLocation != null
                            && gpsLocation.getBearing() >= 0.0f) {
                        DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
                        directionSettings.setGPSDirection(Math.round(gpsLocation.getBearing()));
                        // broadcast new gps direction action
                        broadcastGPSDirection();
                        // new direction broadcast
                        if (directionSettings.getSelectedDirectionSource() == Constants.DIRECTION_SOURCE.GPS) {
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
        DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
        if (directionSettings.getSimulatedDirection() != -1) {
            Intent intent = new Intent(Constants.ACTION_NEW_SIMULATED_DIRECTION);
            intent.putExtra(
                    Constants.ACTION_NEW_SIMULATED_DIRECTION_ATTR.INT_DIRECTION_IN_DEGREE,
                    directionSettings.getSimulatedDirection());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    public void setSimulatedDirection(int newDirection) {
        DirectionSettings directionSettings = settingsManagerInstance.getDirectionSettings();
        if (newDirection >= 0
                && newDirection <= 359
                && newDirection != directionSettings.getSimulatedDirection()) {
            directionSettings.setSimulatedDirection(newDirection);
            // broadcast new simulated direction action
            broadcastSimulatedDirection();
            // broadcast new direction action
            if (directionSettings.getSelectedDirectionSource() == Constants.DIRECTION_SOURCE.SIMULATION) {
                broadcastCurrentDirection();
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
