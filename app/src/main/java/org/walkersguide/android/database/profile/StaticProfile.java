package org.walkersguide.android.database.profile;

import timber.log.Timber;
import org.walkersguide.android.database.profile.static_profile.HistoryProfile;
import java.io.Serializable;
import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.database.SortMethod;
import java.util.Locale;
import java.util.ArrayList;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.util.AccessDatabase;
import java.util.Arrays;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.util.WalkersGuideService;
import org.walkersguide.android.util.WalkersGuideService.TrackingMode;


public class StaticProfile extends DatabaseProfile implements Serializable {
    private static final long serialVersionUID = 1l;


    public static StaticProfile load(long id) {
        if (id == ID_EXCLUDED_ROUTING_SEGMENTS) {
            return excludedRoutingSegments();
        } else if (id == ID_RECORDED_ROUTES) {
            return recordedRoutes();
        } else if (id == ID_PINNED_OBJECTS_WITH_ID) {
            return pinnedObjectsWithId();
        } else if (id == ID_TRACKED_OBJECTS_WITH_ID) {
            return trackedObjectsWithId();
        }

        // history profiles
        ArrayList<HistoryProfile> historyProfileList = new ArrayList<HistoryProfile>();
        historyProfileList.addAll(HistoryProfile.getPointsHistoryProfileList());
        historyProfileList.addAll(HistoryProfile.getRoutesHistoryProfileList());
        historyProfileList.addAll(HistoryProfile.getOverviewHistoryProfileList());
        for (HistoryProfile profile : historyProfileList) {
            if (id == profile.getId()) {
                return profile;
            }
        }

        return null;
    }


    // static profiles
    private static final long ID_EXCLUDED_ROUTING_SEGMENTS = 10;
    private static final long ID_PINNED_OBJECTS_WITH_ID = 12;
    private static final long ID_RECORDED_ROUTES = 14;
    private static final long ID_TRACKED_OBJECTS_WITH_ID = 16;

    public static StaticProfile excludedRoutingSegments() {
        return new StaticProfile(
                ID_EXCLUDED_ROUTING_SEGMENTS,
                GlobalInstance.getStringResource(R.string.databaseProfileExcludedFromRouting),
                R.plurals.way,
                SortMethod.DISTANCE_ASC);
    }

    public static StaticProfile recordedRoutes() {
        return new StaticProfile(
                ID_RECORDED_ROUTES,
                GlobalInstance.getStringResource(R.string.databaseProfileRecordedRoutes),
                R.plurals.recordedRoute,
                SortMethod.CREATED_DESC);
    }

    public static StaticProfile pinnedObjectsWithId() {
        return new StaticProfile(
                ID_PINNED_OBJECTS_WITH_ID,
                GlobalInstance.getStringResource(R.string.databaseProfilePinnedObjectsWithId),
                R.plurals.pointAndRoute,
                SortMethod.DISTANCE_ASC);
    }

    public static StaticProfile trackedObjectsWithId() {
        return new StaticProfile(
                ID_TRACKED_OBJECTS_WITH_ID,
                GlobalInstance.getStringResource(R.string.databaseProfileTrackedObjectsWithId),
                R.plurals.point,
                SortMethod.DISTANCE_ASC);
    }


    /**
     * constructor
     */

    private String name;

    protected StaticProfile(long id, String name, int pluralResId, SortMethod defaultSortMethod) {
        super(id, pluralResId, defaultSortMethod);
        this.name = name;
    }


    // profile class

    @Override public String getName() {
        return this.name;
    }

    @Override public boolean addObject(ObjectWithId object) {
        if (this.getId() == ID_TRACKED_OBJECTS_WITH_ID) {
            WalkersGuideService.invalidateTrackedObjectList();
            WalkersGuideService.setTrackingMode(TrackingMode.DISTANCE, true);
        }
        return super.addObject(object);
    }

    @Override public boolean removeObject(ObjectWithId object) {
        if (this.getId() == ID_TRACKED_OBJECTS_WITH_ID) {
            WalkersGuideService.invalidateTrackedObjectList();
        }
        return super.removeObject(object);
    }

}
