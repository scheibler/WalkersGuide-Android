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


public enum DatabaseSegmentProfile implements DatabaseProfile {

    FAVORITES(
            3000000, GlobalInstance.getStringResource(R.string.databaseSegmentProfileFavorites)),

    EXCLUDED_FROM_ROUTING(
            3501000, GlobalInstance.getStringResource(R.string.databaseSegmentProfileExcludedFromRouting)),

    ALL_SEGMENTS(
            3999999, GlobalInstance.getStringResource(R.string.databaseSegmentProfileAllSegments));


    private static final Map<Long,DatabaseSegmentProfile> valuesById;
    static {
        valuesById = new HashMap<Long,DatabaseSegmentProfile>();
        for(DatabaseSegmentProfile profile : DatabaseSegmentProfile.values()) {
            valuesById.put(profile.getId(), profile);
        }
    }

    public static DatabaseSegmentProfile lookUpById(long id) {
        return valuesById.get(id);
    }


    private long id;
    private String name;

    private DatabaseSegmentProfile(long id, String name) {
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
