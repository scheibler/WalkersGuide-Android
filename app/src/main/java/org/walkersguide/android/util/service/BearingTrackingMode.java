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
import timber.log.Timber;
import java.util.Arrays;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.Bearing.Orientation;


public class BearingTrackingMode {
    private static final int MIN_RANGE = 355;
    private static final int MAX_RANGE = 4;

    private SettingsManager settingsManagerInstance;
    private TTSWrapper ttsWrapperInstance;

    private boolean running;

    public BearingTrackingMode() {
        this.settingsManagerInstance = SettingsManager.getInstance();
        this.ttsWrapperInstance = TTSWrapper.getInstance();
        this.running = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void cancel() {
        if (isRunning()) {
            this.running = false;
        }
    }

    public synchronized void lookForObjectsWithinViewingDirection(TrackedObjectCache cache, Bearing currentBearing) {
        ArrayList<ObjectWithId> filteredObjectList = new ArrayList<ObjectWithId>();
        this.running = true;

        final AnnouncementRadius announcementRadius = settingsManagerInstance.getTrackingModeAnnouncementRadius();
        for (ObjectWithId objectWithId :
                Helper.filterObjectWithIdListByViewingDirection(cache.profileList, MIN_RANGE, MAX_RANGE)) {
            if (objectWithId.distanceFromCurrentLocation() < announcementRadius.meter) {
                filteredObjectList.add(objectWithId);
            }
            if (! isRunning()) {
                return;
            }
        }

        for (ObjectWithId objectWithId :
                Helper.filterObjectWithIdListByViewingDirection(cache.objectsList, MIN_RANGE, MAX_RANGE)) {
            if (! filteredObjectList.contains(objectWithId)) {
                filteredObjectList.add(objectWithId);
            }
            if (! isRunning()) {
                return;
            }
        }

        Collections.sort(
                filteredObjectList,
                new ObjectWithId.SortByDistanceRelativeToCurrentLocation(true));
        if (! isRunning()) {
            return;
        }

        ArrayList<String> messageList = new ArrayList<String>();
        for (Orientation orientation : Orientation.values()) {
            if (currentBearing.differenceTo(new Bearing(orientation.quadrant.mean)) < 5) {
                messageList.add(orientation.toString());
                break;
            }
        }
        for (ObjectWithId objectWithId : filteredObjectList) {
            messageList.add(
                    String.format(
                        "%1$s %2$s",
                        objectWithId.formatNameAndSubType(),
                        GlobalInstance.getPluralResource(
                            R.plurals.inMeters, objectWithId.distanceFromCurrentLocation()))
                    );
        }

        if (messageList.isEmpty()) {
            ttsWrapperInstance.stop();

        } else {
            if (! filteredObjectList.isEmpty()) {
                long[] vibrationPattern = new long[filteredObjectList.size() * 2];
                for (int i=0; i<vibrationPattern.length; i+=2) {
                    vibrationPattern[i] = i == 0 ? 0l : 200l;
                    vibrationPattern[i+1] = 50l;
                }
                Helper.vibratePattern(vibrationPattern);
            }

            ttsWrapperInstance.announce(
                    TextUtils.join(", ", messageList));
        }

        this.running = false;
    }

}
