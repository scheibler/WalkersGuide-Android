package org.walkersguide.android.database.profiles;

import android.content.Context;

import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONException;

import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.GlobalInstance;
import java.util.Map;
import java.util.HashMap;


public enum DatabasePointProfile implements DatabaseProfile {

    FAVORITES(
            1000000, GlobalInstance.getStringResource(R.string.databasePointProfileFavorites)),

    ADDRESS_POINTS(
            1501000, GlobalInstance.getStringResource(R.string.databasePointProfileAddressPoints)),
    GPS_POINTS(
            1502000, GlobalInstance.getStringResource(R.string.databasePointProfileGpsPoints)),
    INTERSECTION_POINTS(
            1503000, GlobalInstance.getStringResource(R.string.databasePointProfileIntersectionPoints)),
    STATION_POINTS(
            1504000, GlobalInstance.getStringResource(R.string.databasePointProfileStationPoints)),

    ROUTE_POINTS(
            1505000, GlobalInstance.getStringResource(R.string.databasePointProfileRoutePoints)),
    SIMULATED_POINTS(
            1506000, GlobalInstance.getStringResource(R.string.databasePointProfileSimulatedPoints)),

    ALL_POINTS(
            1999999, GlobalInstance.getStringResource(R.string.databasePointProfileAllPoints));


    private static final Map<Long,DatabasePointProfile> valuesById;
    static {
        valuesById = new HashMap<Long,DatabasePointProfile>();
        for(DatabasePointProfile profile : DatabasePointProfile.values()) {
            valuesById.put(profile.id, profile);
        }
    }

    public static DatabasePointProfile lookUpById(long id) {
        return valuesById.get(id);
    }


    private long id;
    private String name;

    private DatabasePointProfile(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return this.id;
    }

    public  String getName() {
        return this.name;
    }

    @Override public String toString() {
        return this.name;
    }

}
