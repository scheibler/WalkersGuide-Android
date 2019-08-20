package org.walkersguide.android.util;

import android.annotation.TargetApi;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.Context;
import android.content.Intent;

import android.os.Build;

import android.support.v4.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.activity.MainActivity;

import timber.log.Timber;


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
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        this.sessionId = UUID.randomUUID().toString();
        this.wasInBackground = true;
        // notification channel (android 8)
        createNotificationChannel();
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


    /**
     * notifications
     */

    public static final String WG_NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID;
    public static final int WG_NOTIFICATION_ID = 20183;

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // high priority notification channel
            NotificationChannel highPriorityNotificationChannel = new NotificationChannel(
                    WG_NOTIFICATION_CHANNEL_ID,
                    getResources().getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH);
            highPriorityNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            highPriorityNotificationChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(highPriorityNotificationChannel);
        }
    }

    private void showTestNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // create notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                    ? Notification.PRIORITY_LOW
                    : Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSmallIcon(
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP
                    ? R.drawable.ic_launcher_invers
                    : R.drawable.ic_launcher)
            .setContentText("Testnachricht")
            .setTicker("Testnachricht");
        // navigation channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(WG_NOTIFICATION_CHANNEL_ID);
        }
        // show
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(
                WG_NOTIFICATION_ID, notificationBuilder.build());
    }

}
