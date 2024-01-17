package org.walkersguide.android.util.service;

import java.util.ArrayList;
import java.util.Collections;

import android.text.TextUtils;

import org.walkersguide.android.R;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.data.profile.AnnouncementRadius;
import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.WalkersGuideService.TrackedObjectCache;
import java.util.HashMap;
import androidx.core.util.Pair;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.angle.Bearing;
import java.util.Iterator;
import java.util.Map;


public class DistanceTrackingMode {
    private HashMap<Long,Pair<Point,Long>> announcedObjectBlacklist;
    private SettingsManager settingsManagerInstance;

    public DistanceTrackingMode() {
        this.announcedObjectBlacklist = new HashMap<Long,Pair<Point,Long>>();
        this.settingsManagerInstance = SettingsManager.getInstance();
    }

    public synchronized void lookForNearbyObjects(TrackedObjectCache cache, Point currentLocation, Bearing currentBearing) {
        final AnnouncementRadius announcementRadius = settingsManagerInstance.getTrackingModeAnnouncementRadius();
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

}
