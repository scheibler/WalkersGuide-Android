package org.walkersguide.android.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;

import android.app.Application;
import org.walkersguide.android.database.AccessDatabase;

public class GlobalInstance extends Application {

    private static final long MAX_ACTIVITY_TRANSITION_TIME_MS = 2000;

    // session id
    private String sessionId;

    // background detection
    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    private boolean wasInBackground;

    @Override public void onCreate() {
        super.onCreate();
        this.sessionId = UUID.randomUUID().toString();
        this.wasInBackground = true;
        // open database and check for some defaults
        AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(this);
        accessDatabaseInstance.setSomeDefaults();
    }

    public String getSessionId() {
        return this.sessionId;
    }


    /**
     * Background detection
     */

    public boolean applicationWasInBackground() {
        return this.wasInBackground;
    }

    public void setApplicationInBackground(boolean b) {
        this.wasInBackground = b;
    }

    public void startActivityTransitionTimer() {
        this.mActivityTransitionTimer = new Timer();
        this.mActivityTransitionTimerTask = new TimerTask() {
            public void run() {
                // is run, when application was sent to background or the screen was turned off
                ((GlobalInstance) getApplicationContext()).setApplicationInBackground(true);
                // deactivate sensors
                PositionManager.getInstance(getApplicationContext()).stopGPS();
                DirectionManager.getInstance(getApplicationContext()).stopSensors();
            }
        };
        this.mActivityTransitionTimer.schedule(mActivityTransitionTimerTask,
                MAX_ACTIVITY_TRANSITION_TIME_MS);
    }

    public void stopActivityTransitionTimer() {
        if (this.mActivityTransitionTimerTask != null) {
            this.mActivityTransitionTimerTask.cancel();
        }
        if (this.mActivityTransitionTimer != null) {
            this.mActivityTransitionTimer.cancel();
        }
    }

}
