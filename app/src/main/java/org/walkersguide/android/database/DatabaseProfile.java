package org.walkersguide.android.database;

import java.io.Serializable;
import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.profile.Profile;
import org.walkersguide.android.database.profile.FavoritesProfile;
import java.util.Locale;
import java.util.ArrayList;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.util.AccessDatabase;


public class DatabaseProfile extends Profile implements Serializable {
    private static final long serialVersionUID = 1l;


    public static DatabaseProfile create(long id) {
        if (id == ID_ADDRESS_POINTS) {
            return addressPoints();
        } else if (id == ID_INTERSECTION_POINTS) {
            return intersectionPoints();
        } else if (id == ID_STATION_POINTS) {
            return stationPoints();
        } else if (id == ID_SIMULATED_POINTS) {
            return simulatedPoints();
        } else if (id == ID_ALL_POINTS) {
            return allPoints();

        } else if (id == ID_EXCLUDED_ROUTING_SEGMENTS) {
            return excludedRoutingSegments();

        } else if (id == ID_PLANNED_ROUTES) {
            return plannedRoutes();
        } else if (id == ID_STREET_COURSES) {
            return streetCourses();
        } else if (id == ID_ALL_ROUTES) {
            return allRoutes();

        } else {
            return FavoritesProfile.create(id);
        }
    }

    public static ArrayList<DatabaseProfile> pointHistoryProfileList() {
        ArrayList<DatabaseProfile> profileList = new ArrayList<DatabaseProfile>();
        profileList.add(allPoints());
        profileList.add(addressPoints());
        profileList.add(intersectionPoints());
        profileList.add(stationPoints());
        profileList.add(simulatedPoints());
        return profileList;
    }

    public static ArrayList<DatabaseProfile> routeHistoryProfileList() {
        ArrayList<DatabaseProfile> profileList = new ArrayList<DatabaseProfile>();
        profileList.add(allRoutes());
        profileList.add(plannedRoutes());
        profileList.add(streetCourses());
        return profileList;
    }

    // for points
    private static final long ID_ADDRESS_POINTS = 1501000;
    private static final long ID_INTERSECTION_POINTS = 1502000;
    private static final long ID_STATION_POINTS = 1503000;
    private static final long ID_SIMULATED_POINTS = 1504000;
    private static final long ID_ALL_POINTS = 1999999;

    public static DatabaseProfile addressPoints() {
        return new DatabaseProfile(
                ID_ADDRESS_POINTS,
                GlobalInstance.getStringResource(R.string.databaseProfileAddressPoints),
                ForObject.POINTS);
    }

    public static DatabaseProfile intersectionPoints() {
        return new DatabaseProfile(
                ID_INTERSECTION_POINTS,
                GlobalInstance.getStringResource(R.string.databaseProfileIntersectionPoints),
                ForObject.POINTS);
    }

    public static DatabaseProfile stationPoints() {
        return new DatabaseProfile(
                ID_STATION_POINTS,
                GlobalInstance.getStringResource(R.string.databaseProfileStationPoints),
                ForObject.POINTS);
    }

    public static DatabaseProfile simulatedPoints() {
        return new DatabaseProfile(
                ID_SIMULATED_POINTS,
                GlobalInstance.getStringResource(R.string.databaseProfileSimulatedPoints),
                ForObject.POINTS);
    }

    public static DatabaseProfile allPoints() {
        return new DatabaseProfile(
                ID_ALL_POINTS,
                GlobalInstance.getStringResource(R.string.databaseProfileAllPoints),
                ForObject.POINTS);
    }

    // for segments
    private static final long ID_EXCLUDED_ROUTING_SEGMENTS = 3501000;

    public static DatabaseProfile excludedRoutingSegments() {
        return new DatabaseProfile(
                ID_EXCLUDED_ROUTING_SEGMENTS,
                GlobalInstance.getStringResource(R.string.databaseProfileExcludedFromRouting),
                ForObject.SEGMENTS);
    }

    // for routes
    private static final long ID_PLANNED_ROUTES = 5501000;
    private static final long ID_STREET_COURSES = 5502000;
    private static final long ID_ALL_ROUTES = 5999999;

    public static DatabaseProfile plannedRoutes() {
        return new DatabaseProfile(
                ID_PLANNED_ROUTES,
                GlobalInstance.getStringResource(R.string.databaseProfilePlannedRoutes),
                ForObject.ROUTES);
    }

    public static DatabaseProfile streetCourses() {
        return new DatabaseProfile(
                ID_STREET_COURSES,
                GlobalInstance.getStringResource(R.string.databaseProfileStreetCourses),
                ForObject.ROUTES);
    }

    public static DatabaseProfile allRoutes() {
        return new DatabaseProfile(
                ID_ALL_ROUTES,
                GlobalInstance.getStringResource(R.string.databaseProfileAllRoutes),
                ForObject.ROUTES);
    }


    /**
     * constructor
     */

    protected enum ForObject {
        POINTS, ROUTES, SEGMENTS
    }

    private ForObject forObject;

    protected DatabaseProfile(long id, String name, ForObject forObject) {
        super(id, name);
        this.forObject = forObject;
    }

    public boolean isForPoints() {
        return this.forObject == ForObject.POINTS;
    }

    public boolean isForSegments() {
        return this.forObject == ForObject.SEGMENTS;
    }

    public boolean isForRoutes() {
        return this.forObject == ForObject.ROUTES;
    }

    public boolean contains(ObjectWithId object) {
        return AccessDatabase
            .getInstance()
            .getDatabaseProfileListFor(object)
            .contains(this);
    }

    public boolean add(ObjectWithId object) {
        return AccessDatabase
            .getInstance()
            .addObjectToDatabaseProfile(object, this);
    }

    public boolean remove(ObjectWithId object) {
        return AccessDatabase
            .getInstance()
            .removeObjectFromDatabaseProfile(object, this);
    }

}
