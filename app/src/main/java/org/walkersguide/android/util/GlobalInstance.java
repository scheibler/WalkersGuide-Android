package org.walkersguide.android.util;

        import org.walkersguide.android.shortcut.PinnedShortcutUtility;
import org.walkersguide.android.shortcut.StaticShortcutAction;
import android.annotation.TargetApi;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.Context;
import android.content.Intent;

import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.activity.MainActivity;

import timber.log.Timber;
import java.io.File;
import android.os.Environment;
import org.walkersguide.android.database.util.SQLiteHelper;
import android.os.Handler;
import java.util.LinkedHashMap;
import org.walkersguide.android.server.wg.poi.PoiProfileRequest;
import org.walkersguide.android.server.wg.poi.PoiProfileResult;
import android.annotation.SuppressLint;
import org.walkersguide.android.data.object_with_id.Route;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.Map;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.media.AudioManager;
import android.os.Looper;


public class GlobalInstance extends Application {

    @Override public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        // app context
        this.globalInstance = this;
        // session
        this.sessionId = UUID.randomUUID().toString();
        this.wasInBackground = true;
        // notification channel (android 8)
        createNotificationChannel();
    }


    /**
     * application context
     */
    private static GlobalInstance globalInstance;

    public static GlobalInstance getInstance() {
        return (GlobalInstance) globalInstance;
    }

    public static Context getContext() {
        return globalInstance;
    }

    public static String getStringResource(int resourceId) {
        return getContext().getResources().getString(resourceId);
    }

    public static String getPluralResource(int resourceId, int number) {
        return getContext().getResources().getQuantityString(resourceId, number, number);
    }


    /**
     * session id
     */
    private String sessionId;

    public String getSessionId() {
        return this.sessionId;
    }


    /**
     * Background detection
     */
    private static final long MAX_ACTIVITY_TRANSITION_TIME_MS = 2000;

    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    private boolean wasInBackground;

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
                PositionManager.getInstance().stopGPS();
                DeviceSensorManager.getInstance().stopSensors();
                // disable obsolete pinned shortcuts in background thread
                Executors.newSingleThreadExecutor().execute(() -> {
                    PinnedShortcutUtility.disableObsoletePinnedShortcuts();
                });
                // stop playback of silence wav file
                stopPlaybackOfSilenceWavFile();
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
        PendingIntent pendingIntent = createPendingIntent(
                new Intent(this, MainActivity.class));
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

    @SuppressLint("Deprecation, UnspecifiedImmutableFlag")
    private PendingIntent createPendingIntent(Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            return PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }


    /**
     * sound pool
     */
	private Handler soundPoolHandler;
    private Runnable soundPoolRunnable;
    private SoundPool soundPool;
    private int soundSilence;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("deprecation")
    public void startPlaybackOfSilenceWavFile() {
	    soundPoolHandler = new Handler(Looper.getMainLooper());
        soundPoolRunnable = new Runnable() {
            @Override public void run() {
                if (soundPool != null) {
                    soundPool.play(soundSilence, 1, 1, 1, 0, 1f);
                }
                if (soundPoolHandler != null) {
                    soundPoolHandler.postDelayed(this, 5000);
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build();
            soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
        } else {
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        }

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override public void onLoadComplete (SoundPool soundPool, int sampleId, int status) {
                if (sampleId == soundSilence && status == 0) {  // success
                    soundPoolHandler.post(soundPoolRunnable);
                }
            }
        });
        soundSilence = soundPool.load(getApplicationContext(), R.raw.silence, 1);
    }

    public void stopPlaybackOfSilenceWavFile() {
        if (soundPoolHandler != null && soundPoolRunnable != null) {
            soundPoolHandler.removeCallbacks(soundPoolRunnable);
            soundPoolHandler = null;
            soundPoolRunnable = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            soundSilence = 0;
        }
    }


    /**
     * route position
     */
    private LinkedHashMap<Route,Integer> routeCurrentPositionMap = new LinkedHashMap<Route,Integer>();

    public int getRouteCurrentPosition(Route route) {
        if (this.routeCurrentPositionMap.containsKey(route)) {
            return this.routeCurrentPositionMap.get(route);
        }
        return 0;
    }

    public void setRouteCurrentPosition(Route route, int newPosition) {
        this.routeCurrentPositionMap.put(route, newPosition);
    }


    /**
     * static shortcuts
     */
    private LinkedHashMap<StaticShortcutAction,Boolean> staticShortcutActionEnabledMap = new LinkedHashMap<StaticShortcutAction,Boolean>();

    public ArrayList<StaticShortcutAction> getEnabledStaticShortcutActions() {
        ArrayList<StaticShortcutAction> enabledStaticShortcutActionList = new ArrayList<StaticShortcutAction>();
        for (Map.Entry<StaticShortcutAction,Boolean> entry : this.staticShortcutActionEnabledMap.entrySet()) {
            if (entry.getValue()) {
                enabledStaticShortcutActionList.add(entry.getKey());
            }
        }
        return enabledStaticShortcutActionList;
    }

    public void enableStaticShortcutAction(StaticShortcutAction action) {
        this.staticShortcutActionEnabledMap.put(action, true);
    }

    public void resetEnabledStaticShortcutActions() {
        this.staticShortcutActionEnabledMap.clear();
    }


    /**
     * caches
     */

    // server instance cache
    private ServerInstance cachedServerInstance = null;

    public ServerInstance getCachedServerInstance() {
        return this.cachedServerInstance;
    }

    public void setCachedServerInstance(ServerInstance newServerInstance) {
        this.cachedServerInstance = newServerInstance;
    }

    // poi profiles
    private LinkedHashMap<PoiProfileRequest,PoiProfileResult> poiProfileResultByRequestMap = new LinkedHashMap<PoiProfileRequest,PoiProfileResult>();

    public PoiProfileResult getCachedPoiProfileResult(PoiProfileRequest request) {
        return this.poiProfileResultByRequestMap.get(request);
    }

    public void cachePoiProfileResult(PoiProfileRequest request, PoiProfileResult result) {
        this.poiProfileResultByRequestMap.put(request, result);
    }

    // clear caches

    public void clearCaches() {
        this.cachedServerInstance = null;
        this.poiProfileResultByRequestMap.clear();
    }

}
