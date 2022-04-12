package org.walkersguide.android.tts;

import java.io.Serializable;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.database.util.AccessDatabase;


public class TtsSettings implements Serializable {
    private static final long serialVersionUID = 1l;

    public static TtsSettings getDefault() {
        return new TtsSettings(true, 30);
    }


    private boolean announcementsEnabled;
    private int distanceAnnouncementInterval;

    public TtsSettings(boolean announcementsEnabled, int distanceAnnouncementInterval) {
        this.announcementsEnabled = announcementsEnabled;
        this.distanceAnnouncementInterval = distanceAnnouncementInterval;
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

    public void setDistanceAnnouncementInterval(int newInterval) {
        if (newInterval > 0) {
            this.distanceAnnouncementInterval = newInterval;
        }
    }

}
