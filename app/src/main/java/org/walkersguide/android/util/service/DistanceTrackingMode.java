package org.walkersguide.android.util.service;

import org.walkersguide.android.util.WalkersGuideService.TrackedObjectCache;
import org.walkersguide.android.R;

import java.lang.Class;

import java.text.Normalizer;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.location.LocationManager;

import android.media.AudioAttributes;

import android.net.Uri;

import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.Vibrator;

import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.sensor.DeviceSensorManager.DeviceSensorUpdate;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.sensor.PositionManager.LocationUpdate;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.point.GPS;
import android.widget.Toast;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.database.profile.StaticProfile;
import org.walkersguide.android.data.angle.RelativeBearing;
import org.walkersguide.android.data.object_with_id.route.RouteObject;
import org.walkersguide.android.data.angle.Turn;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.server.wg.poi.PoiProfileRequest;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.server.ServerTaskExecutor;
import java.util.concurrent.Executors;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.server.wg.poi.PoiProfileTask;
import org.walkersguide.android.server.wg.poi.PoiProfileResult;
import org.walkersguide.android.server.wg.WgException;
import java.util.Collections;
import org.walkersguide.android.tts.TTSWrapper;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import org.walkersguide.android.util.SettingsManager;
import com.google.common.primitives.Ints;
import androidx.core.util.Pair;


public class DistanceTrackingMode {
    private HashMap<Long,Pair<Point,Long>> announcedObjectBlacklist;
    private SettingsManager settingsManagerInstance;

    public DistanceTrackingMode() {
        this.announcedObjectBlacklist = new HashMap<Long,Pair<Point,Long>>();
        this.settingsManagerInstance = SettingsManager.getInstance();
    }

    public synchronized void lookForNearbyObjects(TrackedObjectCache cache, Point currentLocation, Bearing currentBearing) {
        final AnnouncementRadius announcementRadius = settingsManagerInstance.getDistanceTrackingModeAnnouncementRadius();
        cleanup(currentLocation);

        // selected point profile or collection to be tracked
        for (ObjectWithId objectWithId : cache.profileList) {
            if (isWithinDistanceOf(objectWithId, currentLocation, announcementRadius.meter)
                    && isWithinBearingOf(objectWithId, currentLocation, currentBearing, 300, 60)
                    && announce(objectWithId, currentLocation)) {
                return;
            }
        }

        // special tracked objects profile
        for (ObjectWithId objectWithId : cache.objectsList) {
            if (isWithinBearingOf(objectWithId, currentLocation, currentBearing, 270, 90)
                    && announce(objectWithId, currentLocation)) {
                return;
            }
        }
    }

    private boolean announce(ObjectWithId object, Point currentLocation) {
        if (! this.announcedObjectBlacklist.containsKey(object.getId())) {
            TTSWrapper.getInstance().announce(
                    String.format(
                        "%1$s %2$s",
                        object.formatNameAndSubType(),
                        object.formatDistanceAndRelativeBearingFromCurrentLocation(
                            R.plurals.inMeters))
                    );
            announcedObjectBlacklist.put(
                    object.getId(), Pair.create(currentLocation, System.currentTimeMillis()));
            return true;
        }
        return false;
    }

    private void cleanup(Point currentLocation) {
        final int announcementDistanceInterval = settingsManagerInstance.getTtsSettings().getDistanceAnnouncementInterval();
        final long announcementTimeInterval = 30;
        Iterator<Map.Entry<Long,Pair<Point,Long>>> it = announcedObjectBlacklist.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long,Pair<Point,Long>> entry = (Map.Entry<Long,Pair<Point,Long>>)it.next();
            if (! isWithinDistanceOf(entry.getValue().first, currentLocation, announcementDistanceInterval)
                    && ! isWithinTimeOf(entry.getValue().second, announcementTimeInterval)) {
                it.remove();
            }
        }
    }

    private boolean isWithinBearingOf(ObjectWithId object, Point currentLocation, Bearing currentBearing, int minAngle, int maxAngle) {
        return currentLocation.bearingTo(object)
            .relativeToCurrentBearing()
            .withinRange(minAngle, maxAngle);
    }

    private boolean isWithinDistanceOf(ObjectWithId object, Point currentLocation, int thresholdInMeters) {
        return currentLocation.distanceTo(object) < thresholdInMeters;
    }

    private boolean isWithinTimeOf(long timestamp, long thresholdInSeconds) {
        return System.currentTimeMillis() - timestamp < thresholdInSeconds * 1000;
    }


    public static class AnnouncementRadius {
        private static final int[] values = new int[]{ 25, 50, 100, 250, 500, 1000 };

        public static ArrayList<AnnouncementRadius> values() {
            ArrayList<AnnouncementRadius> announcementRadiusList = new ArrayList<AnnouncementRadius>();
            for (int meter : values) {
                announcementRadiusList.add(AnnouncementRadius.create(meter));
            }
            return announcementRadiusList;
        }

        public static AnnouncementRadius create(int meter) {
            if (Ints.contains(values, meter)) {
                return new AnnouncementRadius(meter);
            }
            return defaultRadius();
        }

        public static AnnouncementRadius defaultRadius() {
            return new AnnouncementRadius(values[1]);
        }

        public int meter;

        private AnnouncementRadius(int meter) throws IllegalArgumentException {
            this.meter = meter;
        }

        @Override public String toString() {
            return GlobalInstance.getPluralResource(R.plurals.meter, this.meter);
        }

        @Override public int hashCode() {
            return this.meter;
        }

        @Override public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (! (obj instanceof AnnouncementRadius)) {
                return false;
            }
            AnnouncementRadius other = (AnnouncementRadius) obj;
            return this.meter == other.meter;
        }
    }

}
