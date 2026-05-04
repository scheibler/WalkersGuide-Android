package org.walkersguide.android.util.service;

import java.util.ArrayList;
import java.util.Collections;

import android.text.TextUtils;

import org.walkersguide.android.R;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.data.profile.AnnouncementRadius;
import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.tts.TTSWrapper.MessageType;
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
import org.walkersguide.android.sensor.PositionManager;
import timber.log.Timber;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.Angle;


public class DistanceTrackingMode {

    private final static int MIN_THRESHOLD_IN_METERS = 10;
    private final static int MIN_THRESHOLD_IN_SECONDS = 10;
    private final static int MAX_DISTANCE_APPEND_INTERSECTION_SCHEME = 25;

    private HashMap<ObjectWithId,Pair<Point,Long>> announcedObjectBlacklist;
    private SettingsManager settingsManagerInstance;

    public DistanceTrackingMode() {
        this.announcedObjectBlacklist = new HashMap<ObjectWithId,Pair<Point,Long>>();
        this.settingsManagerInstance = SettingsManager.getInstance();
    }

    public synchronized void lookForNearbyObjects(TrackedObjectCache cache, Point currentLocation, Bearing currentBearing) {
        if (currentLocation == null || currentBearing == null) return;

        final AnnouncementRadius announcementRadius = settingsManagerInstance.getTrackingModeAnnouncementRadius();
        cleanup(currentLocation);

        // selected point profile or collection to be tracked
        for (ObjectWithId objectWithId : cache.profileList) {
            Integer distanceToCurrentLocation = currentLocation.distanceTo(objectWithId);
            if (isWithinDistance(distanceToCurrentLocation, announcementRadius.meter)
                    && isWithinBearing(currentLocation.bearingTo(objectWithId), distanceToCurrentLocation)) {
                if (announce(objectWithId, currentLocation, currentBearing)) {
                    return;
                }
            }
        }

        // special tracked objects profile
        for (ObjectWithId objectWithId : cache.objectsList) {
            if (announce(objectWithId, currentLocation, currentBearing)) {
                return;
            }
        }
    }

    private boolean announce(ObjectWithId object, Point currentLocation, Bearing currentBearing) {
        if (! this.announcedObjectBlacklist.containsKey(object)) {
            String text = String.format(
                    "%1$s %2$s",
                    object.formatNameAndSubType(),
                    object.formatDistanceAndRelativeBearingFromCurrentLocation(R.plurals.inMeters));

            // only add intersection scheme if we are very clos to an intersection
            if (object instanceof Intersection
                    && settingsManagerInstance.getSpeakIntersectionStructure()
                    && currentLocation.distanceTo(object) < MAX_DISTANCE_APPEND_INTERSECTION_SCHEME) {
                Intersection intersection = (Intersection) object;
                Intersection.SchemeData intersectionSchemeData = intersection
                    .getSchemeDataForViewingDirection(
                            currentBearing,
                            Angle.Quadrant.Q3.max,  // bearing offset = 157 -> sort the ways, which are strongly to the left of the user, to the top of the list
                            false);     // don't announce the way segment behind the user because one's coming from there
                if (intersectionSchemeData != null) {
                    text += String.format(
                            ".\n%1$s: %2$s",
                            GlobalInstance.getStringResource(R.string.labelIntersectionStructureHeading),
                            intersectionSchemeData.formatLlegendAsOneLiner());
                }
            }

            TTSWrapper.getInstance().announce(
                    text, MessageType.TRACKED_OBJECT_MODE_DISTANCE);
            announcedObjectBlacklist.put(
                    object, Pair.create(currentLocation, System.currentTimeMillis()));
            return true;
        }

        return false;
    }

    private void cleanup(Point currentLocation) {
        Iterator<Map.Entry<ObjectWithId,Pair<Point,Long>>> it = announcedObjectBlacklist.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ObjectWithId,Pair<Point,Long>> entry = (Map.Entry<ObjectWithId,Pair<Point,Long>>)it.next();

            float factor;
            Integer distanceToCurrentLocation = currentLocation.distanceTo(entry.getKey());
            if (distanceToCurrentLocation > 10000) {
                factor = 10.0f;
            } else if (distanceToCurrentLocation > 5000) {
                factor = 6.0f;
            } else if (distanceToCurrentLocation > 2500) {
                factor = 5.0f;
            } else if (distanceToCurrentLocation > 1000) {
                factor = 4.0f;
            } else if (distanceToCurrentLocation > 500) {
                factor = 3.0f;
            } else if (distanceToCurrentLocation > 250) {
                factor = 2.0f;
            } else if (distanceToCurrentLocation > 50) {
                factor = 1.0f;
            } else if (distanceToCurrentLocation > 25) {
                factor = 0.75f;
            } else {
                factor = 0.5f;
            }

            final int announcementDistanceInterval = Math.round(
                    factor * settingsManagerInstance.getTtsSettings().getDistanceAnnouncementInterval());
            final int announcementTimeInterval = Math.round(factor * 30);
            if (! isWithinDistance(currentLocation.distanceTo(entry.getValue().first), announcementDistanceInterval)
                    && ! isWithinTimeOf(entry.getValue().second, announcementTimeInterval)) {
                it.remove();
            }
        }
    }

    private boolean isWithinBearing(Bearing bearingToObject, int distanceToObjectInMeters) {
        final int minAngle = distanceToObjectInMeters < MIN_THRESHOLD_IN_METERS ? 240 : 300;
        final int maxAngle = distanceToObjectInMeters < MIN_THRESHOLD_IN_METERS ? 120 :  60;
        return bearingToObject
            .relativeToCurrentBearing()
            .withinRange(minAngle, maxAngle);
    }

    private boolean isWithinDistance(int distanceToObjectInMeters, int thresholdInMeters) {
        int threshold = thresholdInMeters > MIN_THRESHOLD_IN_METERS
            ? thresholdInMeters : MIN_THRESHOLD_IN_METERS;
        return distanceToObjectInMeters < threshold;
    }

    private boolean isWithinTimeOf(long lastAnnouncedInMilliseconds, int thresholdInSeconds) {
        int threshold = thresholdInSeconds > MIN_THRESHOLD_IN_SECONDS
            ? thresholdInSeconds : MIN_THRESHOLD_IN_SECONDS;
        return System.currentTimeMillis() - lastAnnouncedInMilliseconds < threshold * 1000;
    }

}
