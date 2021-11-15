package org.walkersguide.android.database.util;

import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.profiles.DatabaseSegmentProfile;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
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
import org.walkersguide.android.helper.StringUtility;
import org.json.JSONArray;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.data.basic.segment.Segment;
import org.walkersguide.android.util.GlobalInstance;
import java.io.File;


public class SQLiteHelper extends SQLiteOpenHelper {
    private static final String INTERNAL_DATABASE_NAME = BuildConfig.DATABASE_NAME;
    private static final int DATABASE_VERSION = BuildConfig.DATABASE_VERSION;


    public static File getDatabaseFile() {
        return GlobalInstance.getContext().getDatabasePath(INTERNAL_DATABASE_NAME);
    }

    public static String buildDropTableQuery(String tableName) {
        return String.format("DROP TABLE IF EXISTS %1$s;", tableName);
    }


    // points table
    public static final String TABLE_POINTS = SQLiteHelper.V10_TABLE_POINTS;
    public static final String POINTS_ID = SQLiteHelper.V10_POINTS_ID;
    public static final String POINTS_DATA = SQLiteHelper.V10_POINTS_DATA;
    public static final String POINTS_CUSTOM_NAME = SQLiteHelper.V10_POINTS_CUSTOM_NAME;
    public static final String[] TABLE_POINTS_ALL_COLUMNS = {
        POINTS_ID, POINTS_DATA, POINTS_CUSTOM_NAME
    };

    // segments table
    public static final String TABLE_SEGMENTS = SQLiteHelper.V10_TABLE_SEGMENTS;
    public static final String SEGMENTS_ID = SQLiteHelper.V10_SEGMENTS_ID;
    public static final String SEGMENTS_DATA = SQLiteHelper.V10_SEGMENTS_DATA;
    public static final String SEGMENTS_CUSTOM_NAME = SQLiteHelper.V10_SEGMENTS_CUSTOM_NAME;
    public static final String[] TABLE_SEGMENTS_ALL_COLUMNS = {
        SEGMENTS_ID, SEGMENTS_DATA, SEGMENTS_CUSTOM_NAME
    };

    // routes table
    public static final String TABLE_ROUTES = SQLiteHelper.V10_TABLE_ROUTES;
    public static final String ROUTES_ID = SQLiteHelper.V10_ROUTES_ID;
    public static final String ROUTES_DATA = SQLiteHelper.V10_ROUTES_DATA;
    public static final String ROUTES_CURRENT_POSITION = SQLiteHelper.V10_ROUTES_CURRENT_POSITION;
    public static final String[] TABLE_ROUTES_ALL_COLUMNS = {
        ROUTES_ID, ROUTES_DATA, ROUTES_CURRENT_POSITION
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
        database.execSQL(V10_CREATE_TABLE_POINTS);
        database.execSQL(V10_CREATE_TABLE_SEGMENTS);
        database.execSQL(V10_CREATE_TABLE_ROUTES);
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

    // points table
    private static final String V10_TABLE_POINTS = "points";
    private static final String V10_POINTS_ID = "_id";
    private static final String V10_POINTS_DATA = "data";
    private static final String V10_POINTS_CUSTOM_NAME = "custom_name";
    private static final String V10_CREATE_TABLE_POINTS =
          "CREATE TABLE IF NOT EXISTS " + V10_TABLE_POINTS + "( "
        + V10_POINTS_ID + " INTEGER PRIMARY KEY, "
        + V10_POINTS_DATA + " TEXT NOT NULL, "
        + V10_POINTS_CUSTOM_NAME + " TEXT DEFAULT '');";

    // segments table
    private static final String V10_TABLE_SEGMENTS = "segments";
    private static final String V10_SEGMENTS_ID = "_id";
    private static final String V10_SEGMENTS_DATA = "data";
    private static final String V10_SEGMENTS_CUSTOM_NAME = "custom_name";
    private static final String V10_CREATE_TABLE_SEGMENTS =
          "CREATE TABLE IF NOT EXISTS " + V10_TABLE_SEGMENTS + "( "
        + V10_SEGMENTS_ID + " INTEGER PRIMARY KEY, "
        + V10_SEGMENTS_DATA + " TEXT NOT NULL, "
        + V10_SEGMENTS_CUSTOM_NAME + " TEXT DEFAULT '');";

    // routes table
    private static final String V10_TABLE_ROUTES = "routes";
    private static final String V10_ROUTES_ID = "_id";
    private static final String V10_ROUTES_DATA = "data";
    private static final String V10_ROUTES_CURRENT_POSITION = "current_position";
    private static final String V10_CREATE_TABLE_ROUTES =
          "CREATE TABLE IF NOT EXISTS " + V10_TABLE_ROUTES + "( "
        + V10_ROUTES_ID + " INTEGER PRIMARY KEY, "
        + V10_ROUTES_DATA + " TEXT NOT NULL, "
        + V10_ROUTES_CURRENT_POSITION + " INTEGER DEFAULT 0);";

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
        // all points profile
        database.execSQL(
                "UPDATE fp_points SET profile_id = 1999999 WHERE profile_id = -100;");
        // address points profile
        database.execSQL(
                "UPDATE fp_points SET profile_id = 1501000 WHERE profile_id = -110;");
        // route points profile
        database.execSQL(
                "UPDATE fp_points SET profile_id = 1505000 WHERE profile_id = -120;");
        // simulated points profile
        database.execSQL(
                "UPDATE fp_points SET profile_id = 1506000 WHERE profile_id = -130;");
        // user created points profile
        database.execSQL(
                "UPDATE fp_points SET profile_id = 1502000 WHERE profile_id = -140;");

        // create new points table
        //
        // create
        database.execSQL(V10_CREATE_TABLE_POINTS);
        // copy
        HashMap<Long,Long> oldNewPointIdMap = new HashMap<Long,Long>();
        Cursor cursor = database.query(
                "point", new String[]{"_id", "data"}, null, null, null, null, "_id ASC");
        while (cursor.moveToNext()) {
            try {
                // point data
                JSONObject jsonPointData = Point.addNodeIdToJsonObject(
                        new JSONObject(cursor.getString(cursor.getColumnIndex("data"))));
                // point ids
                long oldPointId = cursor.getLong(cursor.getColumnIndex("_id"));
                long newPointId = jsonPointData.getLong("node_id");
                // create values list
                ContentValues pointsValues = new ContentValues();
                pointsValues.put(V10_POINTS_ID, newPointId);
                pointsValues.put(V10_POINTS_DATA, jsonPointData.toString());
                database.insertWithOnConflict(
                        V10_TABLE_POINTS, null, pointsValues, SQLiteDatabase.CONFLICT_REPLACE);
                // add to id map
                oldNewPointIdMap.put(oldPointId, newPointId);
            } catch (JSONException e) {
                Timber.e("fill points table error: %1$s", e.getMessage());
            }
        }
        cursor.close();
        // delete old point table
        database.execSQL(buildDropTableQuery("point"));

        // add points from old fp_points table to ContentValues list
        cursor = database.query(
                "fp_points", new String[]{"profile_id", "point_id", "ordering"}, null, null, null, null, "profile_id ASC");
        while (cursor.moveToNext()) {
            long profileId = cursor.getLong(cursor.getColumnIndex("profile_id"));
            long oldPointId = cursor.getLong(cursor.getColumnIndex("point_id"));
            long ordering = cursor.getLong(cursor.getColumnIndex("ordering"));
            if (oldNewPointIdMap.containsKey(oldPointId)) {
                long newPointId = oldNewPointIdMap.get(oldPointId);
                // new point id found in points table -> copy
                mappingTableValues = new ContentValues();
                mappingTableValues.put(V10_MAPPING_PROFILE_ID, profileId);
                mappingTableValues.put(V10_MAPPING_OBJECT_ID, newPointId);
                mappingTableValues.put(V10_MAPPING_ACCESSED, ordering);
                mappingTableValues.put(V10_MAPPING_CREATED, ordering);
                mappingTableValuesList.add(mappingTableValues);
                // also copy points from profile "user created" to new favorites profile
                if (profileId == 1502000) {
                    mappingTableValues = new ContentValues();
                    mappingTableValues.put(V10_MAPPING_PROFILE_ID, 1000000);   // favorites
                    mappingTableValues.put(V10_MAPPING_OBJECT_ID, newPointId);
                    mappingTableValues.put(V10_MAPPING_ACCESSED, ordering);
                    mappingTableValues.put(V10_MAPPING_CREATED, ordering);
                    mappingTableValuesList.add(mappingTableValues);
                }
            } else {
                Timber.d("orph");
            }
        }
        cursor.close();
        // delete old fp_points table
        database.execSQL(buildDropTableQuery("fp_points"));

        // new segments table
        //
        // create
        database.execSQL(V10_CREATE_TABLE_SEGMENTS);
        // copy
        HashMap<Long,Long> segmentIdCreatedMap = new HashMap<Long,Long>();
        cursor = database.query(
                "excluded_ways", new String[]{"_id", "data", "timestamp"}, null, null, null, null, "_id ASC");
        while (cursor.moveToNext()) {
            try {
                // segment data
                JSONObject jsonSegmentData = Segment.addWayIdToJsonObject(
                        new JSONObject(cursor.getString(cursor.getColumnIndex("data"))));
                // new segment id
                long newSegmentId = jsonSegmentData.getLong("way_id");
                // add
                ContentValues segmentsValues = new ContentValues();
                segmentsValues.put(V10_SEGMENTS_ID, newSegmentId);
                segmentsValues.put(V10_SEGMENTS_DATA, jsonSegmentData.toString());
                database.insertWithOnConflict(
                        V10_TABLE_SEGMENTS, null, segmentsValues, SQLiteDatabase.CONFLICT_REPLACE);
                // add to id map
                segmentIdCreatedMap.put(
                        newSegmentId, cursor.getLong(cursor.getColumnIndex("timestamp")));
            } catch (JSONException e) {
                Timber.e("fill segments table error: %1$s", e.getMessage());
            }
        }
        cursor.close();
        // remove old excluded_ways table
        database.execSQL(buildDropTableQuery("excluded_ways"));

        // add segments to ContentValues list
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

        // new routes table
        //
        // create
        database.execSQL(V10_CREATE_TABLE_ROUTES);
        // copy
        HashMap<Long,Long> routeIdCreatedMap = new HashMap<Long,Long>();
        String[] oldRouteColumns = new String[] { "_id", "start", "destination",
            "via_point_list", "description", "created", "object_list" };
        cursor = database.query(
                "route", oldRouteColumns, null, null, null, null, "_id ASC");
        while (cursor.moveToNext()) {
            try {
                JSONObject jsonV10Route = Route.convertRouteFromWebserverApiV4ToV5(
                        new JSONObject(
                            cursor.getString(cursor.getColumnIndex("start"))),
                        new JSONObject(
                            cursor.getString(cursor.getColumnIndex("destination"))),
                        new JSONArray(
                            cursor.getString(cursor.getColumnIndex("via_point_list"))),
                        cursor.getString(cursor.getColumnIndex("description")),
                        new JSONArray(
                            cursor.getString(cursor.getColumnIndex("object_list"))));
                long newRouteId = jsonV10Route.getLong("route_id");
                // create values list
                ContentValues routesValues = new ContentValues();
                routesValues.put(V10_ROUTES_ID, newRouteId);
                routesValues.put(V10_ROUTES_DATA, jsonV10Route.toString());
                database.insertWithOnConflict(
                        V10_TABLE_ROUTES, null, routesValues, SQLiteDatabase.CONFLICT_REPLACE);
                // add to id map
                routeIdCreatedMap.put(
                        newRouteId, cursor.getLong(cursor.getColumnIndex("created")));
            } catch (JSONException e) {
                Timber.e("route conversion error: %1$s", e.getMessage());
            }
        }
        cursor.close();
        // remove old route table
        database.execSQL(buildDropTableQuery("route"));

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
            // all segments profile
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
            ContentValues poiProfileValues = new ContentValues();
            poiProfileValues.put(V10_POI_PROFILE_NAME, cursor.getString(cursor.getColumnIndex("name")));
            poiProfileValues.put(V10_POI_PROFILE_CATEGORY_ID_LIST, cursor.getString(cursor.getColumnIndex("category_id_list")));
            poiProfileValues.put(V10_POI_PROFILE_INCLUDE_FAVORITES, 0);
            database.insertWithOnConflict(
                    V10_TABLE_POI_PROFILE, null, poiProfileValues, SQLiteDatabase.CONFLICT_REPLACE);
        }
        cursor.close();
        // remove old poi_profile table
        database.execSQL(buildDropTableQuery("poi_profile"));
    }

}
