package org.walkersguide.android.util;

import java.util.Timer;
import java.util.TimerTask;

import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;

import android.app.Application;

public class GlobalInstance extends Application {

    private static final long MAX_ACTIVITY_TRANSITION_TIME_MS = 2000;

    // background detection
    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    private boolean wasInBackground;

    @Override public void onCreate() {
        super.onCreate();
        this.wasInBackground = true;
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
                // activate sensors
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
