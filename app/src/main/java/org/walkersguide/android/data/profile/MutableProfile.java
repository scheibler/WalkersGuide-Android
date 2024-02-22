package org.walkersguide.android.data.profile;

import org.walkersguide.android.util.WalkersGuideService.TrackingMode;
import org.walkersguide.android.data.Profile;
import java.io.Serializable;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.database.profile.Collection;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.WalkersGuideService;


public interface MutableProfile extends Serializable {

    public static class MutableProfileParams {
        public String name;
        public boolean isPinned;
    }


    public long getId();
    public  MutableProfileParams getProfileParamsFromDatabase();

    public default String getName() {
        MutableProfileParams params = getProfileParamsFromDatabase();
        return params != null ? params.name : "";
    }

    public boolean rename(String newName);

    public default boolean isPinned() {
        MutableProfileParams params = getProfileParamsFromDatabase();
        return params != null ? params.isPinned : false;
    }

    public boolean setPinned(boolean pinned);

    public default boolean isTracked() {
        Profile trackedProfile = SettingsManager.getInstance().getTrackedProfile();
        return trackedProfile != null && trackedProfile.getId() == getId();
    }

    public default boolean setTracked(boolean tracked) {
        SettingsManager.getInstance().setTrackedProfileId(tracked ? getId() : null);
        WalkersGuideService.invalidateTrackedObjectList();
        WalkersGuideService.setTrackingMode(TrackingMode.DISTANCE, true);
        return tracked == isTracked();
    }

    public default boolean profileWasRemoved() {
        return getProfileParamsFromDatabase() == null;
    }


    public boolean remove();

}
