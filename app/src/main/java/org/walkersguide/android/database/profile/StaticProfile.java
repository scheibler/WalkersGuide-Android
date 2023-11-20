package org.walkersguide.android.database.profile;

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
import org.walkersguide.android.database.DatabaseProfile.ForObjects;
import org.walkersguide.android.util.WalkersGuideService;


public class StaticProfile extends DatabaseProfile implements Serializable {
    private static final long serialVersionUID = 1l;


    public static StaticProfile load(long id) {
        if (id == ID_EXCLUDED_ROUTING_SEGMENTS) {
            return excludedRoutingSegments();
        } else if (id == ID_PINNED_POINTS_AND_ROUTES) {
            return pinnedPointsAndRoutes();
        } else if (id == ID_RECORDED_ROUTES) {
            return recordedRoutes();
        } else if (id == ID_TRACKED_POINTS) {
            return trackedPoints();
        }

        // history profiles
        for (HistoryProfile profile : HistoryProfile.getHistoryProfileList()) {
            if (id == profile.getId()) {
                return profile;
            }
        }

        return null;
    }


    // static profiles
    private static final long ID_EXCLUDED_ROUTING_SEGMENTS = 10;
    private static final long ID_PINNED_POINTS_AND_ROUTES = 12;
    private static final long ID_RECORDED_ROUTES = 14;
    private static final long ID_TRACKED_POINTS = 16;

    public static StaticProfile excludedRoutingSegments() {
        return new StaticProfile(
                ID_EXCLUDED_ROUTING_SEGMENTS,
                GlobalInstance.getStringResource(R.string.databaseProfileExcludedFromRouting),
                ForObjects.SEGMENTS,
                SortMethod.CREATED_DESC);
    }

    public static StaticProfile pinnedPointsAndRoutes() {
        return new StaticProfile(
                ID_PINNED_POINTS_AND_ROUTES,
                GlobalInstance.getStringResource(R.string.databaseProfilePinnedPointsAndRoutes),
                ForObjects.POINTS_AND_ROUTES,
                SortMethod.DISTANCE_ASC);
    }

    public static StaticProfile recordedRoutes() {
        return new StaticProfile(
                ID_RECORDED_ROUTES,
                GlobalInstance.getStringResource(R.string.databaseProfileRecordedRoutes),
                ForObjects.ROUTES,
                SortMethod.CREATED_DESC);
    }

    public static StaticProfile trackedPoints() {
        return new StaticProfile(
                ID_TRACKED_POINTS,
                GlobalInstance.getStringResource(R.string.databaseProfileTrackedPoints),
                ForObjects.POINTS,
                SortMethod.DISTANCE_ASC);
    }


    /**
     * constructor
     */

    private String name;

    protected StaticProfile(long id, String name, ForObjects forObjects, SortMethod defaultSortMethod) {
        super(id, forObjects, defaultSortMethod);
        this.name = name;
    }


    // profile class

    @Override public String getName() {
        return this.name;
    }

    @Override public boolean addObject(ObjectWithId object) {
        if (this.getId() == ID_TRACKED_POINTS) {
            WalkersGuideService.invalidateTrackedObjectList();
        }
        return super.addObject(object);
    }

    @Override public boolean removeObject(ObjectWithId object) {
        if (this.getId() == ID_TRACKED_POINTS) {
            WalkersGuideService.invalidateTrackedObjectList();
        }
        return super.removeObject(object);
    }

}
