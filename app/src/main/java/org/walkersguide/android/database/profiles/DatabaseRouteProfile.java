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


public enum DatabaseRouteProfile implements DatabaseProfile {

    FAVORITES(
            5000000, GlobalInstance.getStringResource(R.string.databaseRouteProfileFavorites)),

    PLANNED_ROUTES(
            5501000, GlobalInstance.getStringResource(R.string.databaseRouteProfilePlannedRoutes)),
    STREET_COURSES(
            5502000, GlobalInstance.getStringResource(R.string.databaseRouteProfileStreetCourses)),
    RECORDED_ROUTES(
            5503000, GlobalInstance.getStringResource(R.string.databaseRouteProfileRecordedRoutes)),

    ALL_ROUTES(
            5999999, GlobalInstance.getStringResource(R.string.databaseRouteProfileAllRoutes));


    private static final Map<Long,DatabaseRouteProfile> valuesById;
    static {
        valuesById = new HashMap<Long,DatabaseRouteProfile>();
        for(DatabaseRouteProfile profile : DatabaseRouteProfile.values()) {
            valuesById.put(profile.getId(), profile);
        }
    }

    public static DatabaseRouteProfile lookUpById(long id) {
        return valuesById.get(id);
    }


    private long id;
    private String name;

    private DatabaseRouteProfile(long id, String name) {
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
