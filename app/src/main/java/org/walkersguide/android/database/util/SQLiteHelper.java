package org.walkersguide.android.database.util;

import org.walkersguide.android.util.SettingsManager;
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
import android.text.TextUtils;


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
    public static final String OBJECTS_DATA = SQLiteHelper.V10_OBJECTS_DATA;
    public static final String OBJECTS_CUSTOM_NAME = SQLiteHelper.V10_OBJECTS_CUSTOM_NAME;
    public static final String OBJECTS_USER_ANNOTATION = SQLiteHelper.V12_OBJECTS_USER_ANNOTATION;
    public static final String[] TABLE_OBJECTS_ALL_COLUMNS = {
        OBJECTS_ID, OBJECTS_DATA, OBJECTS_CUSTOM_NAME, OBJECTS_USER_ANNOTATION
    };

    // collection table
    public static final String TABLE_COLLECTION = SQLiteHelper.V12_TABLE_COLLECTION;
    public static final String COLLECTION_ID = SQLiteHelper.V12_COLLECTION_ID;
    public static final String COLLECTION_NAME = SQLiteHelper.V12_COLLECTION_NAME;
    public static final String COLLECTION_IS_PINNED = SQLiteHelper.V12_COLLECTION_IS_PINNED;
    public static final String[] TABLE_COLLECTION_ALL_COLUMNS = {
        COLLECTION_ID, COLLECTION_NAME, COLLECTION_IS_PINNED
    };
    public static final int TABLE_COLLECTION_FIRST_ID = SQLiteHelper.V12_TABLE_COLLECTION_FIRST_ID;
    public static final int TABLE_COLLECTION_LAST_ID = SQLiteHelper.V12_TABLE_COLLECTION_LAST_ID;

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
    public static final String TABLE_POI_PROFILE = SQLiteHelper.V12_TABLE_POI_PROFILE;
    public static final String POI_PROFILE_ID = SQLiteHelper.V12_POI_PROFILE_ID;
    public static final String POI_PROFILE_NAME = SQLiteHelper.V12_POI_PROFILE_NAME;
    public static final String POI_PROFILE_IS_PINNED = SQLiteHelper.V12_POI_PROFILE_IS_PINNED;
    public static final String POI_PROFILE_POI_CATEGORY_ID_LIST = SQLiteHelper.V12_POI_PROFILE_POI_CATEGORY_ID_LIST;
    public static final String POI_PROFILE_COLLECTION_ID_LIST = SQLiteHelper.V12_POI_PROFILE_COLLECTION_ID_LIST;
    public static final String[] TABLE_POI_PROFILE_ALL_COLUMNS = {
        POI_PROFILE_ID, POI_PROFILE_NAME, POI_PROFILE_IS_PINNED,
        POI_PROFILE_POI_CATEGORY_ID_LIST, POI_PROFILE_COLLECTION_ID_LIST
    };
    public static final int TABLE_POI_PROFILE_FIRST_ID = SQLiteHelper.V12_TABLE_POI_PROFILE_FIRST_ID;
    public static final int TABLE_POI_PROFILE_LAST_ID = SQLiteHelper.V12_TABLE_POI_PROFILE_LAST_ID;


    public SQLiteHelper(Context context) {
        super(context, INTERNAL_DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase database) {
        // this should only be executed once - when the app is started for the first time
        //
        // don't show the ChangelogDialog, that's just for app updates
        SettingsManager.getInstance().setChangelogDialogVersionCode();

        // create tables
        database.execSQL(V12_CREATE_TABLE_OBJECTS);
        database.execSQL(V10_CREATE_TABLE_MAPPING);
        createV12CollectionsTable(database);
        createV12PoiProfileTable(database);
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

        if (oldVersion < 11) {
            // remove street courses (id 5502000) from mapping table
            database.execSQL(
                    "DELETE FROM mapping WHERE profile_id = 5502000;");
        }

        if (oldVersion < 12) {
            configureDbVersion12(database);
        }

        if (oldVersion < 13) {
            // update id of profile HISTORY_PINNED_OBJECTS_WITH_ID from  72 to 53
            database.execSQL(
                    "UPDATE mapping SET profile_id = 53 WHERE profile_id = 72;");
            // update id of profile HISTORY_TRACKED_OBJECTS_WITH_ID from  75 to 56
            database.execSQL(
                    "UPDATE mapping SET profile_id = 56 WHERE profile_id = 75;");
            // update id of profile HISTORY_ALL_POINTS from  52 to 79
            database.execSQL(
                    "UPDATE mapping SET profile_id = 79 WHERE profile_id = 52;");
            // delete id of HISTORY_PINNED_ROUTES database profile 83
            database.execSQL(
                    "DELETE FROM mapping WHERE profile_id = 83;");
            // update id of HISTORY_RECORDED_ROUTES database profile from  89 to 83
            database.execSQL(
                    "UPDATE mapping SET profile_id = 83 WHERE profile_id = 89;");
            // update id of HISTORY_ALL_ROUTES database profile from  57 to 89
            database.execSQL(
                    "UPDATE mapping SET profile_id = 89 WHERE profile_id = 57;");
        }
    }


    /*
     * db version >= 10
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

    public static long createDatabaseV10RouteId(JSONArray jsonRouteObjectList) throws JSONException {
        final int prime = 31;
        int result = jsonRouteObjectList.length();
        for (int i=1; i<jsonRouteObjectList.length(); i++) {
            // start at index '1' is intentional, route object at '0' has no segment
            int distance = jsonRouteObjectList
                .getJSONObject(i)
                .getJSONObject("segment")
                .getInt("distance");
            result = ((prime * result) + distance) % (Integer.MAX_VALUE - 1);
        }
        return ObjectWithId.FIRST_LOCAL_ID + result;
    }

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
                JSONObject jsonPointData = addNodeIdToJsonObject(
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
                Timber.e("fill objects table with points error: %1$s", e.getMessage());
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
                Timber.e("cant find point from old fp_points table: %1$s", e.getMessage());
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
                Timber.w("orph");
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
                JSONObject jsonSegmentData = addWayIdToJsonObject(
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
                Timber.e("fill objects table with segments error: %1$s", e.getMessage());
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
        }

        // copy from routes table
        HashMap<Long,Long> routeIdCreatedMap = new HashMap<Long,Long>();
        String[] oldRouteColumns = new String[] { "_id", "start", "destination",
            "via_point_list", "description", "created", "object_list" };
        cursor = database.query(
                "route", oldRouteColumns, null, null, null, null, "_id ASC");
        while (cursor.moveToNext()) {
            try {
                JSONObject jsonV10Route = convertRouteFromWebserverApiV4ToV5(
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
                Timber.e("fill objects table with routes error: %1$s", e.getMessage());
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
            long rowId = database.insertWithOnConflict(
                    V10_TABLE_MAPPING, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            if (rowId == -1) {
                Timber.e("cant insert %1$s into mapping table", values.toString());
            }
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
            } catch (IllegalArgumentException e) {
                Timber.e("Error during poi profile transfer: %1$s", e.getMessage());
            }
        }
        cursor.close();

        // cleanup
        database.execSQL(buildDropTableQuery("fp_points"));
        database.execSQL(buildDropTableQuery("excluded_ways"));
        database.execSQL(buildDropTableQuery("route"));
        database.execSQL(buildDropTableQuery("poi_profile"));
    }

    private static JSONObject convertRouteFromWebserverApiV4ToV5(
            JSONObject jsonStartPoint, JSONObject jsonDestinationPoint, JSONArray jsonViaPointList,
            String description, JSONArray jsonOldRouteObjectList) throws JSONException {
        JSONObject jsonRoute = new JSONObject();

        jsonRoute.put(
                "start_point", addNodeIdToJsonObject(jsonStartPoint));
        jsonRoute.put(
                "destination_point", addNodeIdToJsonObject(jsonDestinationPoint));
        if (jsonViaPointList != null) {
            if (jsonViaPointList.length() > 0) {
                jsonRoute.put(
                        "via_point_1", addNodeIdToJsonObject(jsonViaPointList.getJSONObject(0)));
            }
            if (jsonViaPointList.length() > 1) {
                jsonRoute.put(
                        "via_point_2", addNodeIdToJsonObject(jsonViaPointList.getJSONObject(1)));
            }
            if (jsonViaPointList.length() > 2) {
                jsonRoute.put(
                        "via_point_3", addNodeIdToJsonObject(jsonViaPointList.getJSONObject(2)));
            }
        }

        JSONArray jsonRouteObjectList = new JSONArray();
        for (int i=0; i<jsonOldRouteObjectList.length(); i++) {
            boolean isFirstRouteObject = i == 0 ? true : false;
            boolean isLastRouteObject = i == (jsonOldRouteObjectList.length() - 1) ? true : false;

            JSONObject jsonRouteObject = new JSONObject();
            jsonRouteObject.put("is_first_route_object", isFirstRouteObject);
            jsonRouteObject.put("is_last_route_object", isLastRouteObject);

            // segment
            if (! isFirstRouteObject) {
                JSONObject jsonSegment = addWayIdToJsonObject(
                        jsonOldRouteObjectList.getJSONObject(i).getJSONObject("segment"));
                jsonRouteObject.put("segment", jsonSegment);
            }

            // point
            JSONObject jsonPoint = addNodeIdToJsonObject(
                    jsonOldRouteObjectList.getJSONObject(i).getJSONObject("point"));
            // extract turn value
            if (! isFirstRouteObject && ! isLastRouteObject) {
                jsonRouteObject.put("turn", jsonPoint.getInt("turn"));
            }
            // cleanup point
            jsonPoint.remove("turn");
            // add
            jsonRouteObject.put("point", jsonPoint);

            jsonRouteObjectList.put(jsonRouteObject);
        }
        jsonRoute.put("instructions", jsonRouteObjectList);

        jsonRoute.put("route_id", createDatabaseV10RouteId(jsonRouteObjectList));
        jsonRoute.put("description", description);
        return jsonRoute;
    }

    private static JSONObject addNodeIdToJsonObject(JSONObject jsonPoint) throws JSONException {
        if (jsonPoint.isNull("node_id")) {
            jsonPoint.put("node_id", ObjectWithId.generateId());
        }
        return jsonPoint;
    }

    private static JSONObject addWayIdToJsonObject(JSONObject jsonSegment) throws JSONException {
        if (jsonSegment.isNull("way_id")) {
            jsonSegment.put("way_id", ObjectWithId.generateId());
        }
        return jsonSegment;
    }


    /*
     * db version >= 12
     */

    // objects table
    private static final String V12_OBJECTS_USER_ANNOTATION = "userAnnotation";
    private static final String V12_CREATE_TABLE_OBJECTS =
          "CREATE TABLE IF NOT EXISTS " + V10_TABLE_OBJECTS + "( "
        + V10_OBJECTS_ID + " INTEGER PRIMARY KEY, "
        + V10_OBJECTS_DATA + " TEXT NOT NULL, "
        + V10_OBJECTS_CUSTOM_NAME + " TEXT DEFAULT '', "
        + V12_OBJECTS_USER_ANNOTATION + " TEXT DEFAULT '');";

    // collections table
    private static final String V12_TABLE_COLLECTION = "collection";
    private static final String V12_COLLECTION_ID = "_id";
    private static final String V12_COLLECTION_NAME = "name";
    private static final String V12_COLLECTION_IS_PINNED = "is_pinned";
    private static final String V12_CREATE_TABLE_COLLECTION = 
        "CREATE TABLE IF NOT EXISTS " + V12_TABLE_COLLECTION + "("
        + V12_COLLECTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + V12_COLLECTION_NAME + " TEXT NOT NULL, "
        + V12_COLLECTION_IS_PINNED + " INTEGER DEFAULT 0);";
    private static final int V12_TABLE_COLLECTION_FIRST_ID = 1000000;
    private static final int V12_TABLE_COLLECTION_LAST_ID  = 9999999;

    // poi profile table
    private static final String V12_TABLE_POI_PROFILE = "poi_profile";
    private static final String V12_POI_PROFILE_ID = "_id";
    private static final String V12_POI_PROFILE_NAME = "name";
    private static final String V12_POI_PROFILE_IS_PINNED = "is_pinned";
    private static final String V12_POI_PROFILE_POI_CATEGORY_ID_LIST = "poi_category_id_list";
    private static final String V12_POI_PROFILE_COLLECTION_ID_LIST = "collection_id_list";
    private static final String V12_CREATE_TABLE_POI_PROFILE = 
        "CREATE TABLE IF NOT EXISTS " + V12_TABLE_POI_PROFILE + "("
        + V12_POI_PROFILE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + V12_POI_PROFILE_NAME + " TEXT NOT NULL, "
        + V12_POI_PROFILE_IS_PINNED + " INTEGER DEFAULT 0, "
        + V12_POI_PROFILE_POI_CATEGORY_ID_LIST + " TEXT DEFAULT '[]', "
        + V12_POI_PROFILE_COLLECTION_ID_LIST + " TEXT DEFAULT '[]');";
    private static final int V12_TABLE_POI_PROFILE_FIRST_ID = 100;
    private static final int V12_TABLE_POI_PROFILE_LAST_ID  = 999999;

    private void configureDbVersion12(SQLiteDatabase database) {
        // delete already previously deleted database tables again, just to be on the save side
        database.execSQL(buildDropTableQuery("point"));
        database.execSQL(buildDropTableQuery("poi_profile"));

        // change ids of static database profiles in the mapping table

        // update id of excluded_ways database profile from  3501000 to 10
        database.execSQL(
                "UPDATE mapping SET profile_id = 10 WHERE profile_id = 3501000;");

        // history profiles
        //
        // update id of HISTORY_ALL_POINTS database profile from  1999999 to 52
        database.execSQL(
                "UPDATE mapping SET profile_id = 52 WHERE profile_id = 1999999;");
        // update id of HISTORY_ALL_ROUTE database profile from  5999999 to 57
        database.execSQL(
                "UPDATE mapping SET profile_id = 57 WHERE profile_id = 5999999;");

        // point history
        //
        // update id of profile HISTORY_ADDRESS_POINTS from  1501000 to 60
        database.execSQL(
                "UPDATE mapping SET profile_id = 60 WHERE profile_id = 1501000;");
        // update id of HISTORY_INTERSECTION_POINTS database profile from  1502000 to 63
        database.execSQL(
                "UPDATE mapping SET profile_id = 63 WHERE profile_id = 1502000;");
        // update id of HISTORY_STATION_POINTS database profile from  1503000 to 66
        database.execSQL(
                "UPDATE mapping SET profile_id = 66 WHERE profile_id = 1503000;");
        // update id of HISTORY_SIMULATED_POINTS database profile from  1504000 to 69
        database.execSQL(
                "UPDATE mapping SET profile_id = 69 WHERE profile_id = 1504000;");
        // update id of HISTORY_PINNED_POINTS database profile from  1505000 to 72
        database.execSQL(
                "UPDATE mapping SET profile_id = 72 WHERE profile_id = 1505000;");
        // update id of HISTORY_TRACKED_POINTS database profile from  1506000 to 75
        database.execSQL(
                "UPDATE mapping SET profile_id = 75 WHERE profile_id = 1506000;");

        // route history
        //
        // update id of HISTORY_PLANNED_ROUTES database profile from  5501000 to 80
        database.execSQL(
                "UPDATE mapping SET profile_id = 80 WHERE profile_id = 5501000;");
        // update id of HISTORY_PINNED_ROUTES database profile from  5501750 to 83
        database.execSQL(
                "UPDATE mapping SET profile_id = 83 WHERE profile_id = 5501750;");
        // update id of HISTORY_ROUTES_FROM_GPX_FILE database profile from  5501500 to 86
        database.execSQL(
                "UPDATE mapping SET profile_id = 86 WHERE profile_id = 5501500;");
        // update id of HISTORY_RECORDED_ROUTES database profile from  5502500 to 89
        database.execSQL(
                "UPDATE mapping SET profile_id = 89 WHERE profile_id = 5502500;");

        // collections table

        // create collections table
        createV12CollectionsTable(database);

        // create favorites collection
        ContentValues favoritesCollectionValues = new ContentValues();
        favoritesCollectionValues.put(
                V12_COLLECTION_NAME, GlobalInstance.getStringResource(R.string.collectionNameFavorites));
        favoritesCollectionValues.put(V12_COLLECTION_IS_PINNED, 1);
        long favoritesCollectionId = database.insertWithOnConflict(
                V12_TABLE_COLLECTION, null, favoritesCollectionValues, SQLiteDatabase.CONFLICT_REPLACE);
        Timber.d("favoritesCollectionId: %1$d", favoritesCollectionId);
        if (favoritesCollectionId > -1) {
            // favorite points
            database.execSQL(
                    "UPDATE mapping SET profile_id = " + favoritesCollectionId + " WHERE profile_id = 1000000;");
            // favorite routes
            database.execSQL(
                    "UPDATE mapping SET profile_id = " + favoritesCollectionId + " WHERE profile_id = 5000000;");
        }

        // poi profile table

        // create new poi profile table
        createV12PoiProfileTable(database);

        // restore poi profiles
        Cursor cursor = database.query(
                "poi_profiles", new String[]{"name", "category_id_list"}, null, null, null, null, "_id ASC");
        while (cursor.moveToNext()) {
            try {
                ContentValues poiProfileValues = new ContentValues();
                poiProfileValues.put(
                        V12_POI_PROFILE_NAME,
                        cursor.getString(cursor.getColumnIndexOrThrow("name")));
                poiProfileValues.put(
                        V12_POI_PROFILE_POI_CATEGORY_ID_LIST,
                        cursor.getString(cursor.getColumnIndexOrThrow("category_id_list")));
                database.insertWithOnConflict(
                        V12_TABLE_POI_PROFILE, null, poiProfileValues, SQLiteDatabase.CONFLICT_REPLACE);
            } catch (IllegalArgumentException e) {
                Timber.e("Error during poi profile transfer: %1$s", e.getMessage());
            }
        }
        cursor.close();

        // delete old poi profile table
        database.execSQL(buildDropTableQuery("poi_profiles"));

        // objects table

        // recreate objects table without type but with userAnnotation column
        // rename old
        database.execSQL("ALTER TABLE objects RENAME TO objects_old");
        // create new
        database.execSQL(V12_CREATE_TABLE_OBJECTS);

        // copy
        int total=0, invalidType=0, ncName=0, error=0, pmType=0, smType=0, rmType=0, pmName=0, smName=0, rmName=0;
        Cursor objectsCursor = database.query(
                "objects_old",
                new String[]{V10_OBJECTS_ID, V10_OBJECTS_TYPE, V10_OBJECTS_DATA, V10_OBJECTS_CUSTOM_NAME},
                null, null, null, null, V10_OBJECTS_ID + " ASC");
        while (objectsCursor.moveToNext()) {
            try {

                ContentValues objectValues = new ContentValues();
                objectValues.put(
                        V10_OBJECTS_ID,
                        objectsCursor.getLong(
                            objectsCursor.getColumnIndexOrThrow(V10_OBJECTS_ID)));

                int objectType = objectsCursor.getInt(
                        objectsCursor.getColumnIndexOrThrow(V10_OBJECTS_TYPE));
                if (objectType < 1 || objectType > 3) {
                    invalidType++;
                    continue;
                }

                JSONObject objectData = new JSONObject(
                        objectsCursor.getString(
                            objectsCursor.getColumnIndexOrThrow(V10_OBJECTS_DATA)));
                // missing key "type" in json data
                if (objectData.isNull("type")) {
                    switch (objectType) {
                        case 1:     // point
                            objectData.put("type", "point");
                            pmType++;
                            break;
                        case 2:     // segment
                            objectData.put("type", "segment");
                            smType++;
                            break;
                        case 3:     // route
                            objectData.put("type", "p2p_route");
                            rmType++;
                            break;
                    }
                }
                // missing key "name" in json data
                if (objectData.isNull("name")) {
                    switch (objectType) {
                        case 1:     // point
                            objectData.put(
                                    "name",
                                    String.format(
                                        Locale.getDefault(),
                                        "%1$.3f, %2$.3f",
                                        objectData.getDouble("lat"),
                                        objectData.getDouble("lon"))
                                    );
                            pmName++;
                            break;
                        case 2:     // segment
                            objectData.put("name", "Segment");
                            smName++;
                            break;
                        case 3:     // route
                            objectData.put(
                                    "name",
                                    String.format(
                                        "%1$s: %2$s\n%3$s: %4$s",
                                        GlobalInstance.getStringResource(R.string.labelPrefixStart),
                                        objectData.getJSONObject("start_point").getString("name"),
                                        GlobalInstance.getStringResource(R.string.labelPrefixDestination),
                                        objectData.getJSONObject("destination_point").getString("name"))
                                    );
                            rmName++;
                            break;
                    }
                }
                objectValues.put(V10_OBJECTS_DATA, objectData.toString());

                // custom name
                String objectCustomName = objectsCursor.getString(
                        objectsCursor.getColumnIndexOrThrow(V10_OBJECTS_CUSTOM_NAME));
                if (! TextUtils.isEmpty(objectCustomName)) {
                    ncName++;
                    objectValues.put(V10_OBJECTS_CUSTOM_NAME, objectCustomName);
                }

                database.insertWithOnConflict(
                        V10_TABLE_OBJECTS, null, objectValues, SQLiteDatabase.CONFLICT_REPLACE);
                total++;
            } catch (IllegalArgumentException | JSONException e) {
                Timber.e("V12, fill objects table without type but with userAnnotation column: %1$s", e.getMessage());
                error++;
            }
        }
        objectsCursor.close();
        Timber.d("total=%1$d, invalidType=%2$d, restoredCustomNames=%3$d, error=%4$d", total, invalidType, ncName, error);
        Timber.d("missing type: point=%1$d, segment=%2$d, route=%3$d", pmType, smType, rmType);
        Timber.d("missing name: point=%1$d, segment=%2$d, route=%3$d", pmName, smName, rmName);

        // delete old objects table
        database.execSQL(buildDropTableQuery("objects_old"));
    }

    private void createV12CollectionsTable(SQLiteDatabase database) {
        database.execSQL(V12_CREATE_TABLE_COLLECTION);
        // insert and delete a dummy row to push the start of the auto incremented id to V12_TABLE_COLLECTION_FIRST_ID
        database.execSQL(
                  "INSERT INTO " + V12_TABLE_COLLECTION + " "
                + "VALUES(" + V12_TABLE_COLLECTION_FIRST_ID + ", 'dummy', 0);");
        database.execSQL(
                    "DELETE FROM " + V12_TABLE_COLLECTION + " "
                  + "WHERE " + V12_COLLECTION_ID + " = " + V12_TABLE_COLLECTION_FIRST_ID + ";");
    }

    private void createV12PoiProfileTable(SQLiteDatabase database) {
        database.execSQL(V12_CREATE_TABLE_POI_PROFILE);
        // insert and delete a dummy row to push the start of the auto incremented id to V12_TABLE_POI_PROFILE_FIRST_ID
        // to make room for static poi profiles in the lower id range, which may be required later
        database.execSQL(
                  "INSERT INTO " + V12_TABLE_POI_PROFILE + " "
                + "VALUES(" + V12_TABLE_POI_PROFILE_FIRST_ID + ", 'dummy', 0, '[]', '[]');");
        database.execSQL(
                    "DELETE FROM " + V12_TABLE_POI_PROFILE + " "
                  + "WHERE " + V12_POI_PROFILE_ID + " = " + V12_TABLE_POI_PROFILE_FIRST_ID + ";");
    }

}
