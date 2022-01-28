package org.walkersguide.android.database.util;

import org.walkersguide.android.R;
import org.walkersguide.android.data.ObjectWithId;
import android.content.Context;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Map;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.BuildConfig;

import timber.log.Timber;
import java.util.ArrayList;
import android.content.ContentValues;
import java.util.HashMap;
import org.walkersguide.android.util.Helper;
import org.json.JSONArray;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.util.GlobalInstance;
import java.io.File;
import java.util.Locale;


public class SQLiteHelper extends SQLiteOpenHelper {
    private static final String INTERNAL_DATABASE_NAME = BuildConfig.DATABASE_NAME;
    private static final int DATABASE_VERSION = BuildConfig.DATABASE_VERSION;


    public static File getDatabaseFile() {
        return GlobalInstance.getContext().getDatabasePath(INTERNAL_DATABASE_NAME);
    }

    public static String buildDropTableQuery(String tableName) {
        return String.format("DROP TABLE IF EXISTS %1$s;", tableName);
    }


    // objects table
    public static final String TABLE_OBJECTS = SQLiteHelper.V10_TABLE_OBJECTS;
    public static final String OBJECTS_ID = SQLiteHelper.V10_OBJECTS_ID;
    public static final String OBJECTS_TYPE = SQLiteHelper.V10_OBJECTS_TYPE;
    public static final String OBJECTS_DATA = SQLiteHelper.V10_OBJECTS_DATA;
    public static final String OBJECTS_CUSTOM_NAME = SQLiteHelper.V10_OBJECTS_CUSTOM_NAME;
    public static final String[] TABLE_OBJECTS_ALL_COLUMNS = {
        OBJECTS_ID, OBJECTS_TYPE, OBJECTS_DATA, OBJECTS_CUSTOM_NAME
    };

    // object -> profile mapping table
    public static final String TABLE_MAPPING = SQLiteHelper.V10_TABLE_MAPPING;
    public static final String MAPPING_PROFILE_ID = SQLiteHelper.V10_MAPPING_PROFILE_ID;
    public static final String MAPPING_OBJECT_ID = SQLiteHelper.V10_MAPPING_OBJECT_ID;
    public static final String MAPPING_ACCESSED = SQLiteHelper.V10_MAPPING_ACCESSED;;
    public static final String MAPPING_CREATED = SQLiteHelper.V10_MAPPING_CREATED;
    public static final String[] TABLE_MAPPING_ALL_COLUMNS = {
        MAPPING_PROFILE_ID, MAPPING_OBJECT_ID, MAPPING_ACCESSED, MAPPING_CREATED
    };

    // poi profile table
    public static final String TABLE_POI_PROFILE = SQLiteHelper.V10_TABLE_POI_PROFILE;
    public static final String POI_PROFILE_ID = SQLiteHelper.V10_POI_PROFILE_ID;
    public static final String POI_PROFILE_NAME = SQLiteHelper.V10_POI_PROFILE_NAME;
    public static final String POI_PROFILE_CATEGORY_ID_LIST = SQLiteHelper.V10_POI_PROFILE_CATEGORY_ID_LIST;
    public static final String POI_PROFILE_INCLUDE_FAVORITES = SQLiteHelper.V10_POI_PROFILE_INCLUDE_FAVORITES;
    public static final String[] TABLE_POI_PROFILE_ALL_COLUMNS = {
        POI_PROFILE_ID, POI_PROFILE_NAME, POI_PROFILE_CATEGORY_ID_LIST, POI_PROFILE_INCLUDE_FAVORITES
    };


    public SQLiteHelper(Context context) {
        super(context, INTERNAL_DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase database) {
        database.execSQL(V10_CREATE_TABLE_OBJECTS);
        database.execSQL(V10_CREATE_TABLE_MAPPING);
        database.execSQL(V10_CREATE_TABLE_POI_PROFILE);
    }

    @Override public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Timber.d("onUpgrade: %1$d -> %2$d", oldVersion, newVersion);

        if (oldVersion < 2) {
            // create excluded ways table
            database.execSQL(
                      "CREATE TABLE excluded_ways("
                    + "  _id integer primary key, timestamp integer, data text not null);");
            // add via point column to route table
            database.execSQL(
                    "ALTER TABLE route ADD COLUMN via_point_list TEXT NOT NULL DEFAULT '[]';");
        }

        if (oldVersion < 3) {
            // add search term column in poi profile table
            database.execSQL(
                    "ALTER TABLE poi_profile ADD COLUMN search_term TEXT DEFAULT '';");
        }

        if (oldVersion < 4) {
            // drop obsolete address cache table
            database.execSQL("DROP TABLE IF EXISTS address;");
        }

        if (oldVersion < 6) {
            database.execSQL("DROP TABLE IF EXISTS map;");
            database.execSQL("DROP TABLE IF EXISTS poi_category;");
            database.execSQL("DROP TABLE IF EXISTS public_transport_provider;");
        }

        if (oldVersion < 7) {
            database.execSQL("DELETE FROM poi_profile WHERE _id = -1;");
        }

        if (oldVersion < 8) {
            // update history point profile ids in fp_points table
            // all points
            database.execSQL(
                    "UPDATE fp_points SET profile_id = -100 WHERE profile_id = 0;");
            // address points
            database.execSQL(
                    "UPDATE fp_points SET profile_id = -110 WHERE profile_id = 1;");
            // route points
            database.execSQL(
                    "UPDATE fp_points SET profile_id = -120 WHERE profile_id = 2;");
            // simulated points
            database.execSQL(
                    "UPDATE fp_points SET profile_id = -130 WHERE profile_id = 3;");
            // user created points
            database.execSQL(
                    "UPDATE fp_points SET profile_id = -140 WHERE profile_id = 4;");
            //
            // delete all references of user created favorites profiles
            database.execSQL(
                    "DELETE FROM fp_points WHERE profile_id >= 100;");
            database.execSQL(
                    "DROP TABLE IF EXISTS favorites_profile;");
            //
            // delete search poi profile
            database.execSQL(
                    "DELETE FROM poi_profile WHERE _id = -1000000000;");
        }

        if (oldVersion < 9) {
            // add favorite_id_list column in poi profile table
            database.execSQL(
                    "ALTER TABLE poi_profile ADD COLUMN favorite_id_list TEXT DEFAULT '';");
            // add name column in route table
            database.execSQL(
                    "ALTER TABLE route ADD COLUMN name TEXT DEFAULT '';");
        }

        if (oldVersion < 10) {
            configureDbVersion10(database);
        }
    }


    /*
     * db version 10
     */

    // objects table
    private static final String V10_TABLE_OBJECTS = "objects";
    private static final String V10_OBJECTS_ID = "_id";
    private static final String V10_OBJECTS_TYPE = "type";
    private static final String V10_OBJECTS_DATA = "data";
    private static final String V10_OBJECTS_CUSTOM_NAME = "custom_name";
    private static final String V10_CREATE_TABLE_OBJECTS =
          "CREATE TABLE IF NOT EXISTS " + V10_TABLE_OBJECTS + "( "
        + V10_OBJECTS_ID + " INTEGER PRIMARY KEY, "
        + V10_OBJECTS_TYPE + " INTEGER NOT NULL, "
        + V10_OBJECTS_DATA + " TEXT NOT NULL, "
        + V10_OBJECTS_CUSTOM_NAME + " TEXT DEFAULT '');";

    // mapping table
    private static final String V10_TABLE_MAPPING = "mapping";
    private static final String V10_MAPPING_PROFILE_ID = "profile_id";
    private static final String V10_MAPPING_OBJECT_ID = "object_id";
    private static final String V10_MAPPING_ACCESSED = "accessed";
    private static final String V10_MAPPING_CREATED = "created";
    private static final String V10_CREATE_TABLE_MAPPING =
          "CREATE TABLE IF NOT EXISTS " + V10_TABLE_MAPPING + "( "
        + V10_MAPPING_PROFILE_ID + " INTEGER NOT NULL, "
        + V10_MAPPING_OBJECT_ID + " INTEGER NOT NULL, "
        + V10_MAPPING_ACCESSED + " INTEGER NOT NULL, "
        + V10_MAPPING_CREATED + " INTEGER NOT NULL, "
        + "PRIMARY KEY( " + V10_MAPPING_PROFILE_ID + ", " + V10_MAPPING_OBJECT_ID + "));";

    // poi profile table
    private static final String V10_TABLE_POI_PROFILE = "poi_profiles";
    private static final String V10_POI_PROFILE_ID = "_id";
    private static final String V10_POI_PROFILE_NAME = "name";
    private static final String V10_POI_PROFILE_CATEGORY_ID_LIST = "category_id_list";
    private static final String V10_POI_PROFILE_INCLUDE_FAVORITES = "include_favorites";
    private static final String V10_CREATE_TABLE_POI_PROFILE = 
        "CREATE TABLE IF NOT EXISTS " + V10_TABLE_POI_PROFILE + "("
        + V10_POI_PROFILE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + V10_POI_PROFILE_NAME + " TEXT UNIQUE NOT NULL, "
        + V10_POI_PROFILE_CATEGORY_ID_LIST + " TEXT NOT NULL, "
        + V10_POI_PROFILE_INCLUDE_FAVORITES + " INTEGER NOT NULL);";

    private void configureDbVersion10(SQLiteDatabase database) {
        ArrayList<ContentValues> mappingTableValuesList = new ArrayList<ContentValues>();
        ContentValues mappingTableValues = null;

        // change profile ids in fp_points table
        // favorite points profile
        database.execSQL(
                "UPDATE fp_points SET profile_id = 1000000 WHERE profile_id = -140;");
        // address points profile
        database.execSQL(
                "UPDATE fp_points SET profile_id = 1501000 WHERE profile_id = -110;");
        // simulated points profile
        database.execSQL(
                "UPDATE fp_points SET profile_id = 1504000 WHERE profile_id = -130;");
        // all points profile
        database.execSQL(
                "UPDATE fp_points SET profile_id = 1999999 WHERE profile_id = -100;");

        // new objects table
        database.execSQL(V10_CREATE_TABLE_OBJECTS);

        // copy from point table
        HashMap<Long,Long> oldNewPointIdMap = new HashMap<Long,Long>();
        Cursor cursor = database.query(
                "point", new String[]{"_id", "data"}, null, null, null, null, "_id ASC");
        while (cursor.moveToNext()) {
            try {
                // point data
                JSONObject jsonPointData = Point.addNodeIdToJsonObject(
                        new JSONObject(cursor.getString(cursor.getColumnIndexOrThrow("data"))));
                // point ids
                long oldPointId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                long newPointId = jsonPointData.getLong("node_id");
                // create values list
                ContentValues pointsValues = new ContentValues();
                pointsValues.put(V10_OBJECTS_ID, newPointId);
                pointsValues.put(V10_OBJECTS_TYPE, 1);
                pointsValues.put(V10_OBJECTS_DATA, jsonPointData.toString());
                database.insertWithOnConflict(
                        V10_TABLE_OBJECTS, null, pointsValues, SQLiteDatabase.CONFLICT_REPLACE);
                // add to id map
                oldNewPointIdMap.put(oldPointId, newPointId);
            } catch (IllegalArgumentException | JSONException e) {
                Timber.e("fill points table error: %1$s", e.getMessage());
            }
        }
        cursor.close();

        // add points from old fp_points table to mappingTableValues list
        cursor = database.query(
                "fp_points", new String[]{"profile_id", "point_id", "ordering"}, null, null, null, null, "profile_id ASC");
        while (cursor.moveToNext()) {
            long profileId, oldPointId, ordering;
            try {
                profileId = cursor.getLong(cursor.getColumnIndexOrThrow("profile_id"));
                oldPointId = cursor.getLong(cursor.getColumnIndexOrThrow("point_id"));
                ordering = cursor.getLong(cursor.getColumnIndexOrThrow("ordering"));
            } catch (IllegalArgumentException e) {
                continue;
            }

            if (oldNewPointIdMap.containsKey(oldPointId)) {
                long newPointId = oldNewPointIdMap.get(oldPointId);
                // new point id found in points table -> copy
                mappingTableValues = new ContentValues();
                mappingTableValues.put(V10_MAPPING_PROFILE_ID, profileId);
                mappingTableValues.put(V10_MAPPING_OBJECT_ID, newPointId);
                mappingTableValues.put(V10_MAPPING_ACCESSED, ordering);
                mappingTableValues.put(V10_MAPPING_CREATED, ordering);
                mappingTableValuesList.add(mappingTableValues);
            } else {
                Timber.d("orph");
            }
        }
        cursor.close();

        // copy from excluded_ways table
        HashMap<Long,Long> segmentIdCreatedMap = new HashMap<Long,Long>();
        cursor = database.query(
                "excluded_ways", new String[]{"_id", "data", "timestamp"}, null, null, null, null, "_id ASC");
        while (cursor.moveToNext()) {
            try {
                // segment data
                JSONObject jsonSegmentData = Segment.addWayIdToJsonObject(
                        new JSONObject(cursor.getString(cursor.getColumnIndexOrThrow("data"))));
                // new segment id
                long newSegmentId = jsonSegmentData.getLong("way_id");
                // add
                ContentValues segmentsValues = new ContentValues();
                segmentsValues.put(V10_OBJECTS_ID, newSegmentId);
                segmentsValues.put(V10_OBJECTS_TYPE, 2);
                segmentsValues.put(V10_OBJECTS_DATA, jsonSegmentData.toString());
                database.insertWithOnConflict(
                        V10_TABLE_OBJECTS, null, segmentsValues, SQLiteDatabase.CONFLICT_REPLACE);
                // add to id map
                segmentIdCreatedMap.put(
                        newSegmentId, cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));
            } catch (IllegalArgumentException | JSONException e) {
                Timber.e("fill segments table error: %1$s", e.getMessage());
            }
        }
        cursor.close();

        // add excluded ways to mapping table ContentValues list
        for (Map.Entry<Long,Long> segmentIdCreated : segmentIdCreatedMap.entrySet()) {
            long segmentId = segmentIdCreated.getKey();
            long segmentCreated = segmentIdCreated.getValue();
            // excluded from routing profile
            mappingTableValues = new ContentValues();
            mappingTableValues.put(V10_MAPPING_PROFILE_ID, 3501000);
            mappingTableValues.put(V10_MAPPING_OBJECT_ID, segmentId);
            mappingTableValues.put(V10_MAPPING_ACCESSED, segmentCreated);
            mappingTableValues.put(V10_MAPPING_CREATED, segmentCreated);
            mappingTableValuesList.add(mappingTableValues);
            // all segments profile
            mappingTableValues = new ContentValues();
            mappingTableValues.put(V10_MAPPING_PROFILE_ID, 3999999);
            mappingTableValues.put(V10_MAPPING_OBJECT_ID, segmentId);
            mappingTableValues.put(V10_MAPPING_ACCESSED, segmentCreated);
            mappingTableValues.put(V10_MAPPING_CREATED, segmentCreated);
            mappingTableValuesList.add(mappingTableValues);
        }

        // copy from routes table
        HashMap<Long,Long> routeIdCreatedMap = new HashMap<Long,Long>();
        String[] oldRouteColumns = new String[] { "_id", "start", "destination",
            "via_point_list", "description", "created", "object_list" };
        cursor = database.query(
                "route", oldRouteColumns, null, null, null, null, "_id ASC");
        while (cursor.moveToNext()) {
            try {
                JSONObject jsonV10Route = Route.convertRouteFromWebserverApiV4ToV5(
                        new JSONObject(
                            cursor.getString(cursor.getColumnIndexOrThrow("start"))),
                        new JSONObject(
                            cursor.getString(cursor.getColumnIndexOrThrow("destination"))),
                        new JSONArray(
                            cursor.getString(cursor.getColumnIndexOrThrow("via_point_list"))),
                        cursor.getString(cursor.getColumnIndexOrThrow("description")),
                        new JSONArray(
                            cursor.getString(cursor.getColumnIndexOrThrow("object_list"))));
                long newRouteId = jsonV10Route.getLong("route_id");
                // create values list
                ContentValues routesValues = new ContentValues();
                routesValues.put(V10_OBJECTS_ID, newRouteId);
                routesValues.put(V10_OBJECTS_TYPE, 3);
                routesValues.put(V10_OBJECTS_DATA, jsonV10Route.toString());
                database.insertWithOnConflict(
                        V10_TABLE_OBJECTS, null, routesValues, SQLiteDatabase.CONFLICT_REPLACE);
                // add to id map
                routeIdCreatedMap.put(
                        newRouteId, cursor.getLong(cursor.getColumnIndexOrThrow("created")));
            } catch (IllegalArgumentException | JSONException e) {
                Timber.e("route conversion error: %1$s", e.getMessage());
            }
        }
        cursor.close();

        // add routes to ContentValues list
        for (Map.Entry<Long,Long> routeIdCreated : routeIdCreatedMap.entrySet()) {
            long routeId = routeIdCreated.getKey();
            long routeCreated = routeIdCreated.getValue();
            // planned routes profile
            mappingTableValues = new ContentValues();
            mappingTableValues.put(V10_MAPPING_PROFILE_ID, 5501000);
            mappingTableValues.put(V10_MAPPING_OBJECT_ID, routeId);
            mappingTableValues.put(V10_MAPPING_ACCESSED, routeCreated);
            mappingTableValues.put(V10_MAPPING_CREATED, routeCreated);
            mappingTableValuesList.add(mappingTableValues);
            // all routes profile
            mappingTableValues = new ContentValues();
            mappingTableValues.put(V10_MAPPING_PROFILE_ID, 5999999);
            mappingTableValues.put(V10_MAPPING_OBJECT_ID, routeId);
            mappingTableValues.put(V10_MAPPING_ACCESSED, routeCreated);
            mappingTableValues.put(V10_MAPPING_CREATED, routeCreated);
            mappingTableValuesList.add(mappingTableValues);
        }

        // new mapping table
        //
        // create
        database.execSQL(V10_CREATE_TABLE_MAPPING);
        // fill mapping table
        for (ContentValues values : mappingTableValuesList) {
            database.insertWithOnConflict(
                    V10_TABLE_MAPPING, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }

        // new poi profile table
        //
        // create
        database.execSQL(V10_CREATE_TABLE_POI_PROFILE);
        // copy
        cursor = database.query(
                "poi_profile", new String[]{"name", "category_id_list"}, null, null, null, null, "_id ASC");
        while (cursor.moveToNext()) {
            try {
                ContentValues poiProfileValues = new ContentValues();
                poiProfileValues.put(V10_POI_PROFILE_NAME, cursor.getString(cursor.getColumnIndexOrThrow("name")));
                poiProfileValues.put(V10_POI_PROFILE_CATEGORY_ID_LIST, cursor.getString(cursor.getColumnIndexOrThrow("category_id_list")));
                poiProfileValues.put(V10_POI_PROFILE_INCLUDE_FAVORITES, 0);
                database.insertWithOnConflict(
                        V10_TABLE_POI_PROFILE, null, poiProfileValues, SQLiteDatabase.CONFLICT_REPLACE);
            } catch (IllegalArgumentException e) {}
        }
        cursor.close();

        // cleanup
        database.execSQL(buildDropTableQuery("fp_points"));
        database.execSQL(buildDropTableQuery("excluded_ways"));
        database.execSQL(buildDropTableQuery("route"));
        database.execSQL(buildDropTableQuery("poi_profile"));
    }

}
