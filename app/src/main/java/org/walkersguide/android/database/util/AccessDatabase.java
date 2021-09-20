package org.walkersguide.android.database.util;

import org.walkersguide.android.server.poi.PoiCategory;
import org.walkersguide.android.server.poi.PoiProfile;

import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
import org.walkersguide.android.database.profiles.DatabaseRouteProfile;
import org.walkersguide.android.database.profiles.DatabaseSegmentProfile;
import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.util.GlobalInstance;
import android.content.ContentValues;
import android.content.Context;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import android.text.TextUtils;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.data.route.RouteObject;
import org.walkersguide.android.data.basic.point.Point;
import java.util.ListIterator;
import java.util.Collections;
import timber.log.Timber;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.walkersguide.android.data.basic.segment.Segment;


public class AccessDatabase {

    private static AccessDatabase managerInstance;
    private SQLiteDatabase database;
    private SQLiteHelper dbHelper;

    public static AccessDatabase getInstance() {
        if (managerInstance == null){
            managerInstance = getInstanceSynchronized();
        }
        return managerInstance;
    }

    private static synchronized AccessDatabase getInstanceSynchronized() {
        if (managerInstance == null){
            managerInstance = new AccessDatabase();
        }
        return managerInstance;
    }

    private AccessDatabase() {
        open();
    }

    public void open() throws SQLException {
        this.dbHelper = new SQLiteHelper(GlobalInstance.getContext());
        this.database = dbHelper.getWritableDatabase();
    }

    public void close() throws SQLException {
        this.dbHelper.close();
    }


    /**
     * object mapping
     * base class for point, segment and route, which follow below
     */

    public ArrayList<ObjectWithId> getObjectWithIdListFor(DatabaseProfileRequest request) {
        DatabaseProfile profile = request.getProfile();
        String searchTerm = request.getSearchTerm();
        SortMethod sortMethod = request.getSortMethod();
        ArrayList<ObjectWithId> objectList = new ArrayList<ObjectWithId>();

        // build sql query
        String objectTableName = null, objectTableColumnId = null, objectTableColumnData = null;
        if (profile instanceof DatabasePointProfile) {
            objectTableName = SQLiteHelper.TABLE_POINTS;
            objectTableColumnId = SQLiteHelper.POINTS_ID;
            objectTableColumnData = SQLiteHelper.POINTS_DATA;
        } else if (profile instanceof DatabaseRouteProfile) {
            objectTableName = SQLiteHelper.TABLE_ROUTES;
            objectTableColumnId = SQLiteHelper.ROUTES_ID;
            objectTableColumnData = SQLiteHelper.ROUTES_DATA;
        } else if (profile instanceof DatabaseSegmentProfile) {
            objectTableName = SQLiteHelper.TABLE_SEGMENTS;
            objectTableColumnId = SQLiteHelper.SEGMENTS_ID;
            objectTableColumnData = SQLiteHelper.SEGMENTS_DATA;
        }

        if (objectTableName != null && objectTableColumnId != null && objectTableColumnData != null) {
            ArrayList<String> queryList = new ArrayList<String>();

            // select
            queryList.add("SELECT");
            queryList.add(
                    String.format(
                        "%1$s.%2$s AS %2$s", objectTableName, objectTableColumnData));

            // from
            queryList.add("FROM");
            queryList.add(
                    String.format(
                        "%1$s LEFT JOIN %2$s", objectTableName, SQLiteHelper.TABLE_MAPPING));

            // where
            queryList.add("WHERE");
            queryList.add(
                    String.format(
                        "%1$s.%2$s = %3$s.%4$s",
                        objectTableName, objectTableColumnId,
                        SQLiteHelper.TABLE_MAPPING, SQLiteHelper.MAPPING_OBJECT_ID));
            queryList.add("AND");
            queryList.add(
                    String.format(
                        "%1$s.%2$s = ?",
                        SQLiteHelper.TABLE_MAPPING, SQLiteHelper.MAPPING_PROFILE_ID));

            // order by
            String orderByColumn = null;
            switch (sortMethod) {
                case ACCESSED_ASC:
                case ACCESSED_DESC:
                    orderByColumn = SQLiteHelper.MAPPING_ACCESSED;
                    break;
                case CREATED_ASC:
                case CREATED_DESC:
                    orderByColumn = SQLiteHelper.MAPPING_CREATED;
                    break;
            }
            if (orderByColumn != null) {
                queryList.add(
                        String.format(
                            "ORDER BY %1$s.%2$s %3$s",
                            SQLiteHelper.TABLE_MAPPING,
                            orderByColumn,
                            sortMethod.isAscending() ? "ASC" : "DESC")
                        );
            }

            // execute sql query
            Cursor cursor = database.rawQuery(
                    TextUtils.join(" ", queryList), new String[]{String.valueOf(profile.getId())});
            while (cursor.moveToNext()) {
                String stringJsonObject = cursor.getString(
                        cursor.getColumnIndex(objectTableColumnData));
                try {
                    if (profile instanceof DatabasePointProfile) {
                        objectList.add(
                                Point.create(new JSONObject(stringJsonObject)));
                    } else if (profile instanceof DatabaseRouteProfile) {
                        objectList.add(
                                Route.create(new JSONObject(stringJsonObject)));
                    } else if (profile instanceof DatabaseSegmentProfile) {
                        objectList.add(
                                Segment.create(new JSONObject(stringJsonObject)));
                    }
                } catch (JSONException e) {}
            }
            cursor.close();
        }

        // filter by search term
        if (! TextUtils.isEmpty(searchTerm)) {
            ListIterator<ObjectWithId> objectListIterator = objectList.listIterator();
            while(objectListIterator.hasNext()){
                ObjectWithId objectWithId = objectListIterator.next();
                boolean match = true;
                for (String word : searchTerm.split("\\s")) {
                    if (! objectWithId.toString().toLowerCase().contains(word.toLowerCase())) {
                        match = false;
                        break;
                    }
                }
                if (! match) {
                    objectListIterator.remove();
                }
            }
        }

        switch (sortMethod) {
            case NAME_ASC:
            case NAME_DESC:
                Collections.sort(
                        objectList, new ObjectWithId.SortByName(sortMethod.isAscending()));
                break;
            case BEARING_ASC:
            case BEARING_DESC:
                Collections.sort(
                        objectList, new ObjectWithId.SortByBearingFromCurrentDirection(0, sortMethod.isAscending()));
            case DISTANCE_ASC:
            case DISTANCE_DESC:
                Collections.sort(
                        objectList, new ObjectWithId.SortByDistanceFromCurrentDirection(sortMethod.isAscending()));
                break;
        }

        return objectList;
    }

    public ArrayList<DatabaseProfile> getDatabaseProfileListFor(ObjectWithId objectWithId) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_MAPPING, SQLiteHelper.TABLE_MAPPING_ALL_COLUMNS,
                String.format(
                    "%1$s = %2$d", 
                    SQLiteHelper.MAPPING_OBJECT_ID,
                    objectWithId.getId()),
                null, null, null, SQLiteHelper.MAPPING_PROFILE_ID + " ASC");
        ArrayList<DatabaseProfile> profileList = new ArrayList<DatabaseProfile>();
        while (cursor.moveToNext()) {
            long profileId = cursor.getLong(
                    cursor.getColumnIndex(SQLiteHelper.MAPPING_PROFILE_ID));
            // create profile
            DatabaseProfile profile = null;
            if (objectWithId instanceof Point) {
                profile = DatabasePointProfile.lookUpById(profileId);
            } else if (objectWithId instanceof Route) {
                profile = DatabaseRouteProfile.lookUpById(profileId);
            } else if (objectWithId instanceof Segment) {
                profile = DatabaseSegmentProfile.lookUpById(profileId);
            }
            // add
            if (profile != null && ! profileList.contains(profile)) {
                profileList.add(profile);
            }
        }
        cursor.close();
        return profileList;
    }

    public boolean addObjectToDatabaseProfile(ObjectWithId objectWithId, DatabaseProfile profile) {
        // add object first
        if (objectWithId instanceof Point
                && profile instanceof DatabasePointProfile) {
            Point point = (Point) objectWithId;
            DatabasePointProfile pointProfile = (DatabasePointProfile) profile;
            if (! addPointToDatabasePointProfile(point, pointProfile)) {
                return false;
            }
        } else if (objectWithId instanceof Route
                && profile instanceof DatabaseRouteProfile) {
            Route route = (Route) objectWithId;
            DatabaseRouteProfile routeProfile = (DatabaseRouteProfile) profile;
            if (! addRouteToDatabaseRouteProfile(route, routeProfile)) {
                return false;
            }
        } else if (objectWithId instanceof Segment
                && profile instanceof DatabaseSegmentProfile) {
            Segment segment = (Segment) objectWithId;
            DatabaseSegmentProfile segmentProfile = (DatabaseSegmentProfile) profile;
            if (! addSegmentToDatabaseSegmentProfile(segment, segmentProfile)) {
                return false;
            }
        } else {
            return false;
        }

        // try to get access value of possibly existing mapping table row
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_MAPPING, SQLiteHelper.TABLE_MAPPING_ALL_COLUMNS,
                String.format(
                    "%1$s = %2$d AND %3$s = %4$d",
                    SQLiteHelper.MAPPING_PROFILE_ID, profile.getId(),
                    SQLiteHelper.MAPPING_OBJECT_ID, objectWithId.getId()),
                null, null, null, null);
        long created = System.currentTimeMillis();
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            created = cursor.getLong(
                    cursor.getColumnIndex(SQLiteHelper.MAPPING_CREATED));
        }
        cursor.close();

        // add object to mapping table or update accessed column
        ContentValues mappingValues = new ContentValues();
        mappingValues.put(SQLiteHelper.MAPPING_PROFILE_ID, profile.getId());
        mappingValues.put(SQLiteHelper.MAPPING_OBJECT_ID, objectWithId.getId());
        mappingValues.put(SQLiteHelper.MAPPING_ACCESSED, System.currentTimeMillis());
        mappingValues.put(SQLiteHelper.MAPPING_CREATED, created);
        long rowIdTableMapping = database.insertWithOnConflict(
                SQLiteHelper.TABLE_MAPPING,
                null,
                mappingValues,
                SQLiteDatabase.CONFLICT_REPLACE);
        return rowIdTableMapping == -1 ? false : true;
    }

    private boolean addPointToDatabasePointProfile(Point point, DatabasePointProfile profile) {
        if (profile != DatabasePointProfile.ALL_POINTS) {
            if (! this.addPointToDatabasePointProfile(point, DatabasePointProfile.ALL_POINTS)) {
                return false;
            }
        } else {
            if (! addPoint(point, getPointCustomName(point.getId()))) {
                return false;
            }
        }
        return true;
    }

    private boolean addRouteToDatabaseRouteProfile(Route route, DatabaseRouteProfile profile) {
        if (profile != DatabaseRouteProfile.ALL_ROUTES) {
            if (! addRouteToDatabaseRouteProfile(route, DatabaseRouteProfile.ALL_ROUTES)) {
                return false;
            }
        } else {
            if (! addRoute(route, getRouteCurrentPosition(route.getId()))) {
                return false;
            }
        }
        return true;
    }

    private boolean addSegmentToDatabaseSegmentProfile(Segment segment, DatabaseSegmentProfile profile) {
        if (profile != DatabaseSegmentProfile.ALL_SEGMENTS) {
            if (! this.addSegmentToDatabaseSegmentProfile(segment, DatabaseSegmentProfile.ALL_SEGMENTS)) {
                return false;
            }
        } else {
            if (! addSegment(segment, getSegmentCustomName(segment.getId()))) {
                return false;
            }
        }
        return true;
    }

    public void removeObjectFromDatabaseProfile(ObjectWithId objectWithId, DatabaseProfile profile) {
        database.delete(
                SQLiteHelper.TABLE_MAPPING,
                String.format(
                    "%1$s = ? AND %2$s = ?", SQLiteHelper.MAPPING_PROFILE_ID, SQLiteHelper.MAPPING_OBJECT_ID),
                new String[] {
                    String.valueOf(profile.getId()), String.valueOf(objectWithId.getId())});
    }

    public void clearDatabaseProfile(DatabaseProfile profile) {
        database.delete(
                SQLiteHelper.TABLE_MAPPING,
                String.format("%1$s = ?", SQLiteHelper.MAPPING_PROFILE_ID),
                new String[] {String.valueOf(profile.getId())});
    }


    /**
     * points
     */

    public Point getPoint(long id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_POINTS, SQLiteHelper.TABLE_POINTS_ALL_COLUMNS,
                String.format("%1$s = %2$d", SQLiteHelper.POINTS_ID, id),
                null, null, null, null);
        Point point = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            String stringJsonPoint = cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.POINTS_DATA));
            try {
                point = Point.create(new JSONObject(stringJsonPoint));
            } catch (JSONException e) {}
        }
        cursor.close();
        return point;
    }

    public String getPointCustomName(long id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_POINTS, SQLiteHelper.TABLE_POINTS_ALL_COLUMNS,
                String.format("%1$s = %2$d", SQLiteHelper.POINTS_ID, id),
                null, null, null, null);
        String customName = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            String customNameFromDatabase = cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.POINTS_CUSTOM_NAME));
            if (! TextUtils.isEmpty(customNameFromDatabase)) {
                customName = customNameFromDatabase;
            }
        }
        cursor.close();
        return customName;
    }

    public boolean addPoint(Point point, String customName) {
        // prepare
        String pointSerialized = null;
        try {
            pointSerialized = point.toJson().toString();
        } catch (JSONException e) {
            pointSerialized = null;
        } finally {
            if (pointSerialized == null) {
                return false;
            }
        }

        // add to or replace in points table
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.POINTS_ID, point.getId());
        values.put(SQLiteHelper.POINTS_DATA, pointSerialized);
        values.put(SQLiteHelper.POINTS_CUSTOM_NAME, TextUtils.isEmpty(customName) ? "" : customName);
        long rowIdTablePoints = database.insertWithOnConflict(
                SQLiteHelper.TABLE_POINTS,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE);
        Timber.d("addPoint: rowId=%1$d --  to insert %2$s into %3$s table", rowIdTablePoints, pointSerialized, SQLiteHelper.TABLE_POINTS);
        return rowIdTablePoints == -1 ? false : true;
    }


    /**
     * routes
     */

    public Route getRoute(long id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_ROUTES, SQLiteHelper.TABLE_ROUTES_ALL_COLUMNS,
                String.format("%1$s = %2$d", SQLiteHelper.ROUTES_ID, id),
                null, null, null, null);
        Route route = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            String stringJsonRoute = cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.ROUTES_DATA));
            try {
                route = Route.create(new JSONObject(stringJsonRoute));
            } catch (JSONException e) {}
        }
        cursor.close();
        return route;
    }

    public int getRouteCurrentPosition(long id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_ROUTES, SQLiteHelper.TABLE_ROUTES_ALL_COLUMNS,
                String.format("%1$s = %2$d", SQLiteHelper.ROUTES_ID, id),
                null, null, null, null);
        int currentPosition = 0;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            currentPosition = cursor.getInt(
                    cursor.getColumnIndex(SQLiteHelper.ROUTES_CURRENT_POSITION));
        }
        cursor.close();
        return currentPosition;
    }

    public boolean addRoute(Route route, int currentPosition) {
        // prepare
        String routeSerialized = null;
        try {
            routeSerialized = route.toJson().toString();
        } catch (JSONException e) {
            routeSerialized = null;
        } finally {
            if (routeSerialized == null) {
                return false;
            }
        }

        // add to or replace in routes table
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.ROUTES_ID, route.getId());
        values.put(SQLiteHelper.ROUTES_DATA, routeSerialized);
        values.put(SQLiteHelper.ROUTES_CURRENT_POSITION, currentPosition < 0 ? 0 : currentPosition);
        long rowIdTableRoutes = database.insertWithOnConflict(
                SQLiteHelper.TABLE_ROUTES,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE);
        return rowIdTableRoutes == -1 ? false : true;
    }


    /**
     * segments
     */

    public Segment getSegment(long id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_SEGMENTS, SQLiteHelper.TABLE_SEGMENTS_ALL_COLUMNS,
                String.format("%1$s = %2$d", SQLiteHelper.SEGMENTS_ID, id),
                null, null, null, null);
        Segment segment = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            String stringJsonSegment = cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.SEGMENTS_DATA));
            try {
                segment = Segment.create(new JSONObject(stringJsonSegment));
            } catch (JSONException e) {}
        }
        cursor.close();
        return segment;
    }

    public String getSegmentCustomName(long id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_SEGMENTS, SQLiteHelper.TABLE_SEGMENTS_ALL_COLUMNS,
                String.format("%1$s = %2$d", SQLiteHelper.SEGMENTS_ID, id),
                null, null, null, null);
        String customName = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            String customNameFromDatabase = cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.SEGMENTS_CUSTOM_NAME));
            if (! TextUtils.isEmpty(customNameFromDatabase)) {
                customName = customNameFromDatabase;
            }
        }
        cursor.close();
        return customName;
    }

    public boolean addSegment(Segment segment, String customName) {
        // prepare
        String segmentSerialized = null;
        try {
            segmentSerialized = segment.toJson().toString();
        } catch (JSONException e) {
            segmentSerialized = null;
        } finally {
            if (segmentSerialized == null) {
                return false;
            }
        }

        // add to or replace in segments table
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.SEGMENTS_ID, segment.getId());
        values.put(SQLiteHelper.SEGMENTS_DATA, segmentSerialized);
        values.put(SQLiteHelper.SEGMENTS_CUSTOM_NAME, TextUtils.isEmpty(customName) ? "" : customName);
        long rowIdTableSegments = database.insertWithOnConflict(
                SQLiteHelper.TABLE_SEGMENTS,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE);
        return rowIdTableSegments == -1 ? false : true;
    }


    /**
     * POIProfile
     */

    public ArrayList<PoiProfile> getPoiProfileList() {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_POI_PROFILE, SQLiteHelper.TABLE_POI_PROFILE_ALL_COLUMNS,
                null, null, null, null, SQLiteHelper.POI_PROFILE_ID + " ASC");

        ArrayList<PoiProfile> profileList = new ArrayList<PoiProfile>();
        while (cursor.moveToNext()) {
            PoiProfile profile = null;
            try {
                JSONArray jsonPOICategoryIdList = new JSONArray(
                        cursor.getString(
                            cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_CATEGORY_ID_LIST)));
                boolean includeFavorites = cursor.getInt(
                        cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_INCLUDE_FAVORITES)) == 1 ? true : false;
                profile = new PoiProfile(
                        cursor.getLong(
                            cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_ID)),
                        cursor.getString(
                            cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_NAME)),
                        PoiCategory.listFromJson(jsonPOICategoryIdList),
                        includeFavorites);
            } catch (JSONException     e) {
            } finally {
                if (profile != null && ! profileList.contains(profile)) {
                    profileList.add(profile);
                }
            }
        }
        cursor.close();

        return profileList;
    }

    public PoiProfile getPoiProfile(long id) {
        for (PoiProfile profile : getPoiProfileList()) {
            if (profile.getId() == id) {
                return profile;
            }
        }
        return null;
    }

    public PoiProfile addPoiProfile(String name, ArrayList<PoiCategory> poiCategoryList, boolean includeFavorites) {
        // prepare
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.POI_PROFILE_NAME, name);
        values.put(SQLiteHelper.POI_PROFILE_CATEGORY_ID_LIST, PoiCategory.listToJson(poiCategoryList).toString());
        values.put(SQLiteHelper.POI_PROFILE_INCLUDE_FAVORITES, includeFavorites ? 1 : 0);
        // create
        long newPoiProfileId = database.insertWithOnConflict(
                SQLiteHelper.TABLE_POI_PROFILE,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE);
        return getPoiProfile(newPoiProfileId);
    }

    public boolean updatePoiProfile(PoiProfile profile) {
        // prepare
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.POI_PROFILE_NAME, profile.getName());
        values.put(SQLiteHelper.POI_PROFILE_CATEGORY_ID_LIST, PoiCategory.listToJson(profile.getPoiCategoryList()).toString());
        values.put(SQLiteHelper.POI_PROFILE_INCLUDE_FAVORITES, profile.getIncludeFavorites() ? 1 : 0);
        // update
        int numberOfRowsAffected = database.updateWithOnConflict(
                SQLiteHelper.TABLE_POI_PROFILE,
                values,
                SQLiteHelper.POI_PROFILE_ID + " = ?",
                new String[]{String.valueOf(profile.getId())},
                SQLiteDatabase.CONFLICT_REPLACE);
        if (numberOfRowsAffected == 1) {
            sendPoiProfileModifiedBroadcast(profile);
            return true;
        }
        return false;
    }

    public boolean removePoiProfile(PoiProfile profile) {
        int numberOfRowsAffected = database.delete(
                SQLiteHelper.TABLE_POI_PROFILE,
                SQLiteHelper.POI_PROFILE_ID + " = ?",
                new String[]{String.valueOf(profile.getId())});
        if (numberOfRowsAffected == 1) {
            return true;
        }
        return false;
    }

    // profile modified broadcast
    public static final String ACTION_POI_PROFILE_MODIFIED = String.format(
            "%1$s.action.poi_profile_modified", BuildConfig.APPLICATION_ID);
    public static final String EXTRA_POI_PROFILE = "poiProfile";

    private static void sendPoiProfileModifiedBroadcast(PoiProfile profile) {
        Intent intent = new Intent(ACTION_POI_PROFILE_MODIFIED);
        intent.putExtra(EXTRA_POI_PROFILE, profile);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

}
