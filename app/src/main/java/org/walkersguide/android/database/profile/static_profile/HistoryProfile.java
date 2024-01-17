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


    public static ArrayList<HistoryProfile> getPointsHistoryProfileList() {
        ArrayList<HistoryProfile> historyProfileList = new ArrayList<HistoryProfile>();
        historyProfileList.add(allPoints());
        historyProfileList.add(addressPoints());
        historyProfileList.add(intersectionPoints());
        historyProfileList.add(stationPoints());
        historyProfileList.add(simulatedPoints());
        return historyProfileList;
    }

    public static ArrayList<HistoryProfile> getRoutesHistoryProfileList() {
        ArrayList<HistoryProfile> historyProfileList = new ArrayList<HistoryProfile>();
        historyProfileList.add(allRoutes());
        historyProfileList.add(plannedRoutes());
        historyProfileList.add(recordedRoutes());
        historyProfileList.add(routesFromGpxFile());
        return historyProfileList;
    }

    public static ArrayList<HistoryProfile> getOverviewHistoryProfileList() {
        ArrayList<HistoryProfile> historyProfileList = new ArrayList<HistoryProfile>();
        historyProfileList.add(pinnedObjectsWithId());
        historyProfileList.add(trackedObjectsWithId());
        return historyProfileList;
    }


    // meta profiles
    private static final long ID_HISTORY_PINNED_OBJECTS_WITH_ID = 53;
    private static final long ID_HISTORY_TRACKED_OBJECTS_WITH_ID = 56;

    public static HistoryProfile pinnedObjectsWithId() {
        return new HistoryProfile(
                ID_HISTORY_PINNED_OBJECTS_WITH_ID,
                GlobalInstance.getStringResource(R.string.databaseProfilePinnedObjectsWithId),
                R.plurals.pointAndRoute);
    }

    public static HistoryProfile trackedObjectsWithId() {
        return new HistoryProfile(
                ID_HISTORY_TRACKED_OBJECTS_WITH_ID,
                GlobalInstance.getStringResource(R.string.databaseProfileTrackedObjectsWithId),
                R.plurals.pointAndRoute);
    }


    // point history
    private static final long ID_HISTORY_ADDRESS_POINTS = 60;
    private static final long ID_HISTORY_INTERSECTION_POINTS = 63;
    private static final long ID_HISTORY_STATION_POINTS = 66;
    private static final long ID_HISTORY_SIMULATED_POINTS = 69;
    private static final long ID_HISTORY_ALL_POINTS = 79;

    public static HistoryProfile addressPoints() {
        return new HistoryProfile(
                ID_HISTORY_ADDRESS_POINTS,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileAddressPoints),
                R.plurals.point);
    }

    public static HistoryProfile intersectionPoints() {
        return new HistoryProfile(
                ID_HISTORY_INTERSECTION_POINTS,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileIntersectionPoints),
                R.plurals.point);
    }

    public static HistoryProfile stationPoints() {
        return new HistoryProfile(
                ID_HISTORY_STATION_POINTS,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileStationPoints),
                R.plurals.point);
    }

    public static HistoryProfile simulatedPoints() {
        return new HistoryProfile(
                ID_HISTORY_SIMULATED_POINTS,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileSimulatedPoints),
                R.plurals.point);
    }

    public static HistoryProfile allPoints() {
        return new HistoryProfile(
                ID_HISTORY_ALL_POINTS,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileAllPoints),
                R.plurals.point);
    }


    // route history
    private static final long ID_HISTORY_PLANNED_ROUTES = 80;
    private static final long ID_HISTORY_RECORDED_ROUTES = 83;
    private static final long ID_HISTORY_ROUTES_FROM_GPX_FILE = 86;
    private static final long ID_HISTORY_ALL_ROUTES = 89;

    public static HistoryProfile plannedRoutes() {
        return new HistoryProfile(
                ID_HISTORY_PLANNED_ROUTES,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfilePlannedRoutes),
                R.plurals.route);
    }

    public static HistoryProfile recordedRoutes() {
        return new HistoryProfile(
                ID_HISTORY_RECORDED_ROUTES,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileRecordedRoutes),
                R.plurals.route);
    }

    public static HistoryProfile routesFromGpxFile() {
        return new HistoryProfile(
                ID_HISTORY_ROUTES_FROM_GPX_FILE,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileRoutesFromGpxFile),
                R.plurals.route);
    }

    public static HistoryProfile allRoutes() {
        return new HistoryProfile(
                ID_HISTORY_ALL_ROUTES,
                GlobalInstance.getStringResource(R.string.historyDatabaseProfileAllRoutes),
                R.plurals.route);
    }


    // constructor

    protected HistoryProfile(long id, String name, int pluralResId) {
        super(id, name, pluralResId, SortMethod.ACCESSED_DESC);
    }


    // profile class

    @Override public Icon getIcon() {
        return Icon.HISTORY;
    }

}
