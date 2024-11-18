package org.walkersguide.android.tts;

import java.io.Serializable;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.database.util.AccessDatabase;


public class TtsSettings implements Serializable {
    private static final long serialVersionUID = 1l;

    public static TtsSettings getDefault() {
        return new TtsSettings(true, 30, 1.0f);
    }


    private boolean announcementsEnabled;
    private int distanceAnnouncementInterval;
    private float speechRate;

    public TtsSettings(boolean announcementsEnabled, int distanceAnnouncementInterval, float speechRate) {
        this.announcementsEnabled = announcementsEnabled;
        this.distanceAnnouncementInterval = distanceAnnouncementInterval;
        this.speechRate = speechRate;
    }

    public boolean getAnnouncementsEnabled() {
        return this.announcementsEnabled;
    }

    public void setAnnouncementsEnabled(boolean newState) {
        this.announcementsEnabled = newState;
    }

    public int getDistanceAnnouncementInterval() {
        return this.distanceAnnouncementInterval;
    }

    public boolean setDistanceAnnouncementInterval(int newInterval) {
        if (newInterval > 0) {
            this.distanceAnnouncementInterval = newInterval;
            return true;
        }
        return false;
    }

    public float getSpeechRate() {
        return this.speechRate;
    }

    public boolean setSpeechRate(float newSpeechRate) {
        if (newSpeechRate > 0) {
            this.speechRate = newSpeechRate;
            return true;
        }
        return false;
    }

}
