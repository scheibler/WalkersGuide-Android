package org.walkersguide.android.database.profile.static_profile;

import org.walkersguide.android.data.Profile.Icon;
import java.io.Serializable;
import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.database.profile.StaticProfile;
import java.util.ArrayList;
import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.ObjectWithId;


public class HistoryProfile extends StaticProfile implements Serializable {
    private static final long serialVersionUID = 1l;


    public static ArrayList<HistoryProfile> getHistoryProfileList() {
        ArrayList<HistoryProfile> historyProfileList = new ArrayList<HistoryProfile>();
        // point profiles
        historyProfileList.add(allPoints());
        historyProfileList.add(addressPoints());
        historyProfileList.add(intersectionPoints());
        historyProfileList.add(stationPoints());
        historyProfileList.add(simulatedPoints());
        historyProfileList.add(pinnedPoints());
        historyProfileList.add(trackedPoints());
        // route profiles
        historyProfileList.add(allRoutes());
        historyProfileList.add(plannedRoutes());
        historyProfileList.add(pinnedRoutes());
        historyProfileList.add(routesFromGpxFile());
        historyProfileList.add(recordedRoutes());
        return historyProfileList;
    }


    // meta profiles

    private static final long ID_HISTORY_ALL_POINTS = 52;
    private static final long ID_HISTORY_ALL_ROUTES = 57;

    public static HistoryProfile allPoints() {
        return new HistoryProfile(
                ID_HISTORY_ALL_POINTS,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileAllPoints),
                ForObjects.POINTS);
    }

    public static HistoryProfile allRoutes() {
        return new HistoryProfile(
                ID_HISTORY_ALL_ROUTES,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileAllRoutes),
                ForObjects.ROUTES);
    }


    // point history
    private static final long ID_HISTORY_ADDRESS_POINTS = 60;
    private static final long ID_HISTORY_INTERSECTION_POINTS = 63;
    private static final long ID_HISTORY_STATION_POINTS = 66;
    private static final long ID_HISTORY_SIMULATED_POINTS = 69;
    private static final long ID_HISTORY_PINNED_POINTS = 72;
    private static final long ID_HISTORY_TRACKED_POINTS = 75;

    public static HistoryProfile addressPoints() {
        return new HistoryProfile(
                ID_HISTORY_ADDRESS_POINTS,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileAddressPoints),
                ForObjects.POINTS);
    }

    public static HistoryProfile intersectionPoints() {
        return new HistoryProfile(
                ID_HISTORY_INTERSECTION_POINTS,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileIntersectionPoints),
                ForObjects.POINTS);
    }

    public static HistoryProfile stationPoints() {
        return new HistoryProfile(
                ID_HISTORY_STATION_POINTS,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileStationPoints),
                ForObjects.POINTS);
    }

    public static HistoryProfile simulatedPoints() {
        return new HistoryProfile(
                ID_HISTORY_SIMULATED_POINTS,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileSimulatedPoints),
                ForObjects.POINTS);
    }

    public static HistoryProfile pinnedPoints() {
        return new HistoryProfile(
                ID_HISTORY_PINNED_POINTS,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfilePinnedPoints),
                ForObjects.POINTS);
    }

    public static HistoryProfile trackedPoints() {
        return new HistoryProfile(
                ID_HISTORY_TRACKED_POINTS,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileTrackedPoints),
                ForObjects.POINTS);
    }


    // route history
    private static final long ID_HISTORY_PLANNED_ROUTES = 80;
    private static final long ID_HISTORY_PINNED_ROUTES = 83;
    private static final long ID_HISTORY_ROUTES_FROM_GPX_FILE = 86;
    private static final long ID_HISTORY_RECORDED_ROUTES = 89;

    public static HistoryProfile plannedRoutes() {
        return new HistoryProfile(
                ID_HISTORY_PLANNED_ROUTES,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfilePlannedRoutes),
                ForObjects.ROUTES);
    }

    public static HistoryProfile pinnedRoutes() {
        return new HistoryProfile(
                ID_HISTORY_PINNED_ROUTES,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfilePinnedRoutes),
                ForObjects.ROUTES);
    }

    public static HistoryProfile routesFromGpxFile() {
        return new HistoryProfile(
                ID_HISTORY_ROUTES_FROM_GPX_FILE,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileRoutesFromGpxFile),
                ForObjects.ROUTES);
    }

    public static HistoryProfile recordedRoutes() {
        return new HistoryProfile(
                ID_HISTORY_RECORDED_ROUTES,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileRecordedRoutes),
                ForObjects.ROUTES);
    }


    // constructor

    protected HistoryProfile(long id, String name, ForObjects forObjects) {
        super(id, name, forObjects, SortMethod.ACCESSED_DESC);
    }


    // profile class

    @Override public Icon getIcon() {
        return Icon.HISTORY;
    }


    // add / remove object from / to profile

    @Override public boolean add(ObjectWithId object) {
        HistoryProfile allObjectsProfile = null;
        if (object instanceof Point
                && this.getId() != ID_HISTORY_ALL_POINTS) {
            allObjectsProfile = allPoints();
        } else if (object instanceof Route
                && this.getId() != ID_HISTORY_ALL_ROUTES) {
            allObjectsProfile = allRoutes();
        }

        if (allObjectsProfile != null) {
            if (! AccessDatabase
                    .getInstance()
                    .addObjectToDatabaseProfile(object, allObjectsProfile)) {
                return false;
            }
        }
        return super.add(object);
    }

}
