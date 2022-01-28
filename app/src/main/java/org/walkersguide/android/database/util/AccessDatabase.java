package org.walkersguide.android.database.util;

import org.walkersguide.android.server.wg.poi.PoiCategory;
import org.walkersguide.android.server.wg.poi.PoiProfile;

import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.database.DatabaseProfileRequest;
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

import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.data.object_with_id.route.RouteObject;
import org.walkersguide.android.data.object_with_id.Point;
import java.util.ListIterator;
import java.util.Collections;
import timber.log.Timber;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.walkersguide.android.data.object_with_id.Segment;
import java.util.Locale;
import org.walkersguide.android.data.object_with_id.HikingTrail;


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
     * objects
     */
    private static final int TYPE_POINT = 1;
    private static final int TYPE_SEGMENT = 2;
    private static final int TYPE_ROUTE = 3;
    private static final int TYPE_HIKING_TRAIL = 4;

    public ObjectWithId getObjectWithId(long id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_OBJECTS, SQLiteHelper.TABLE_OBJECTS_ALL_COLUMNS,
                String.format(
                    Locale.ROOT, "%1$s = %2$d", SQLiteHelper.OBJECTS_ID, id),
                null, null, null, null);
        ObjectWithId objectWithId = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            try {
                objectWithId = createObject(
                        cursor.getInt(
                            cursor.getColumnIndexOrThrow(SQLiteHelper.OBJECTS_TYPE)),
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(SQLiteHelper.OBJECTS_DATA)));
            } catch (IllegalArgumentException | JSONException e) {}
        }
        cursor.close();
        return objectWithId;
    }

    public String getObjectWithIdCustomName(ObjectWithId objectWithId) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_OBJECTS, SQLiteHelper.TABLE_OBJECTS_ALL_COLUMNS,
                String.format(
                    Locale.ROOT, "%1$s = %2$d", SQLiteHelper.OBJECTS_ID, objectWithId.getId()),
                null, null, null, null);
        String customName = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            try {
                String customNameFromDatabase = cursor.getString(
                        cursor.getColumnIndexOrThrow(SQLiteHelper.OBJECTS_CUSTOM_NAME));
                if (! TextUtils.isEmpty(customNameFromDatabase)) {
                    customName = customNameFromDatabase;
                }
            } catch (IllegalArgumentException e) {}
        }
        cursor.close();
        return customName;
    }

    public boolean addObjectWithId(ObjectWithId objectWithId) {
        return addObjectWithId(
                objectWithId, objectWithId.getCustomName());
    }

    public boolean addObjectWithId(ObjectWithId objectWithId, String customName) {
        // prepare
        int type;
        if (objectWithId instanceof Point) {
            type = TYPE_POINT;
        } else if (objectWithId instanceof Route) {
            type = TYPE_ROUTE;
        } else if (objectWithId instanceof Segment) {
            type = TYPE_SEGMENT;
        } else if (objectWithId instanceof HikingTrail) {
            type = TYPE_HIKING_TRAIL;
        } else {
            return false;
        }
        String objectWithIdSerialized = null;
        try {
            objectWithIdSerialized = objectWithId.toJson().toString();
        } catch (JSONException e) {
            return false;
        }

        // add to or replace in objectWithIds table
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.OBJECTS_ID, objectWithId.getId());
        values.put(SQLiteHelper.OBJECTS_TYPE, type);
        values.put(SQLiteHelper.OBJECTS_DATA, objectWithIdSerialized);
        values.put(SQLiteHelper.OBJECTS_CUSTOM_NAME, TextUtils.isEmpty(customName) ? "" : customName);
        long rowIdTableObjectWithIds = database.insertWithOnConflict(
                SQLiteHelper.TABLE_OBJECTS,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE);
        Timber.d("addObjectWithId: rowId=%1$d --  insert into %2$s table", rowIdTableObjectWithIds, SQLiteHelper.TABLE_OBJECTS);
        return rowIdTableObjectWithIds == -1 ? false : true;
    }

    private ObjectWithId createObject(int type, String stringJsonObjectWithId)
            throws IllegalArgumentException, JSONException {
        JSONObject jsonObjectWithId = new JSONObject(stringJsonObjectWithId);
        if (type == TYPE_POINT) {
            return Point.create(jsonObjectWithId);
        } else if (type == TYPE_ROUTE) {
            return Route.create(jsonObjectWithId);
        } else if (type == TYPE_SEGMENT) {
            return Segment.create(jsonObjectWithId);
        } else if (type == TYPE_HIKING_TRAIL) {
            return HikingTrail.create(jsonObjectWithId);
        } else {
            throw new IllegalArgumentException();
        }
    }


    /**
     * object mapping
     */

    public ArrayList<ObjectWithId> getObjectListFor(DatabaseProfileRequest request) {
        DatabaseProfile profile = request.getProfile();
        String searchTerm = request.getSearchTerm();
        SortMethod sortMethod = request.getSortMethod();
        ArrayList<ObjectWithId> objectList = new ArrayList<ObjectWithId>();

        // build sql query
        String objectTableName = SQLiteHelper.TABLE_OBJECTS;
        String objectTableColumnId = SQLiteHelper.OBJECTS_ID;
        String objectTableColumnType = SQLiteHelper.OBJECTS_TYPE;
        String objectTableColumnData = SQLiteHelper.OBJECTS_DATA;
        ArrayList<String> queryList = new ArrayList<String>();

        // select
        queryList.add("SELECT");
        queryList.add(
                String.format(
                    Locale.ROOT, "%1$s.%2$s AS %2$s", objectTableName, objectTableColumnData));
        queryList.add(", ");
        queryList.add(
                String.format(
                    Locale.ROOT, "%1$s.%2$s AS %2$s", objectTableName, objectTableColumnType));

        // from
        queryList.add("FROM");
        queryList.add(
                String.format(
                    Locale.ROOT, "%1$s LEFT JOIN %2$s", objectTableName, SQLiteHelper.TABLE_MAPPING));

        // where
        queryList.add("WHERE");
        queryList.add(
                String.format(
                    Locale.ROOT, "%1$s.%2$s = %3$s.%4$s",
                    objectTableName, objectTableColumnId,
                    SQLiteHelper.TABLE_MAPPING, SQLiteHelper.MAPPING_OBJECT_ID));
        queryList.add("AND");
        queryList.add(
                String.format(
                    Locale.ROOT, "%1$s.%2$s = ?",
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
                        Locale.ROOT,
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
            try {
                objectList.add(
                        createObject(
                            cursor.getInt(
                                    cursor.getColumnIndexOrThrow(objectTableColumnType)),
                            cursor.getString(
                                    cursor.getColumnIndexOrThrow(objectTableColumnData)))
                        );
            } catch (IllegalArgumentException | JSONException e) {}
        }
        cursor.close();

        // filter by search term
        if (! TextUtils.isEmpty(searchTerm)) {
            ListIterator<ObjectWithId> objectListIterator = objectList.listIterator();
            while(objectListIterator.hasNext()){
                ObjectWithId objectWithId = objectListIterator.next();
                boolean match = true;
                for (String word : searchTerm.split("\\s")) {
                    if (! objectWithId.toString().toLowerCase(Locale.getDefault())
                            .contains(word.toLowerCase(Locale.getDefault()))) {
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
                        objectList, new ObjectWithId.SortByBearingRelativeToCurrentBearing(0, sortMethod.isAscending()));
            case DISTANCE_ASC:
            case DISTANCE_DESC:
                Collections.sort(
                        objectList, new ObjectWithId.SortByDistanceRelativeToCurrentLocation(sortMethod.isAscending()));
                break;
        }

        return objectList;
    }

    public ArrayList<DatabaseProfile> getDatabaseProfileListFor(ObjectWithId objectWithId) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_MAPPING, SQLiteHelper.TABLE_MAPPING_ALL_COLUMNS,
                String.format(
                    Locale.ROOT,
                    "%1$s = %2$d", 
                    SQLiteHelper.MAPPING_OBJECT_ID,
                    objectWithId.getId()),
                null, null, null, SQLiteHelper.MAPPING_PROFILE_ID + " ASC");
        ArrayList<DatabaseProfile> profileList = new ArrayList<DatabaseProfile>();
        while (cursor.moveToNext()) {
            // create profile
            DatabaseProfile  profile = null;
            try {
                profile = DatabaseProfile.create(
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(SQLiteHelper.MAPPING_PROFILE_ID)));
            } catch (IllegalArgumentException e) {}
            // add
            if (profile != null && ! profileList.contains(profile)) {
                profileList.add(profile);
            }
        }
        cursor.close();
        return profileList;
    }

    public boolean addObjectToDatabaseProfile(ObjectWithId object, DatabaseProfile profile) {
        // add object first
        DatabaseProfile allObjectsProfile = null;
        if (profile.isForPoints()
               && ! profile.equals(DatabaseProfile.allPoints())) {
            allObjectsProfile = DatabaseProfile.allPoints();
        } else if (profile.isForRoutes()
               && ! profile.equals(DatabaseProfile.allRoutes())) {
            allObjectsProfile = DatabaseProfile.allRoutes();
        }
        if (allObjectsProfile != null) {
            if (! this.addObjectToDatabaseProfile(object, allObjectsProfile)) {
                return false;
            }
        } else {
            if (! addObjectWithId(object)) {
                return false;
            }
        }

        // try to get access value of possibly existing mapping table row
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_MAPPING, SQLiteHelper.TABLE_MAPPING_ALL_COLUMNS,
                String.format(
                    Locale.ROOT,
                    "%1$s = %2$d AND %3$s = %4$d",
                    SQLiteHelper.MAPPING_PROFILE_ID, profile.getId(),
                    SQLiteHelper.MAPPING_OBJECT_ID, object.getId()),
                null, null, null, null);
        long created = System.currentTimeMillis();
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            try {
                created = cursor.getLong(
                        cursor.getColumnIndexOrThrow(SQLiteHelper.MAPPING_CREATED));
            } catch (IllegalArgumentException e) {}
        }
        cursor.close();

        // add object to mapping table or update accessed column
        ContentValues mappingValues = new ContentValues();
        mappingValues.put(SQLiteHelper.MAPPING_PROFILE_ID, profile.getId());
        mappingValues.put(SQLiteHelper.MAPPING_OBJECT_ID, object.getId());
        mappingValues.put(SQLiteHelper.MAPPING_ACCESSED, System.currentTimeMillis());
        mappingValues.put(SQLiteHelper.MAPPING_CREATED, created);
        long rowIdTableMapping = database.insertWithOnConflict(
                SQLiteHelper.TABLE_MAPPING,
                null,
                mappingValues,
                SQLiteDatabase.CONFLICT_REPLACE);
        return rowIdTableMapping == -1 ? false : true;
    }

    public boolean removeObjectFromDatabaseProfile(ObjectWithId object, DatabaseProfile profile) {
        int numberOfRemovedRows = database.delete(
                SQLiteHelper.TABLE_MAPPING,
                String.format(
                    Locale.ROOT,
                    "%1$s = ? AND %2$s = ?", SQLiteHelper.MAPPING_PROFILE_ID, SQLiteHelper.MAPPING_OBJECT_ID),
                new String[] {
                    String.valueOf(profile.getId()), String.valueOf(object.getId())});
        return numberOfRemovedRows > 0 ? true : false;
    }

    public void clearDatabaseProfile(DatabaseProfile profile) {
        database.delete(
                SQLiteHelper.TABLE_MAPPING,
                String.format(
                    Locale.ROOT, "%1$s = ?", SQLiteHelper.MAPPING_PROFILE_ID),
                new String[] {String.valueOf(profile.getId())});
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
                            cursor.getColumnIndexOrThrow(SQLiteHelper.POI_PROFILE_CATEGORY_ID_LIST)));
                boolean includeFavorites = cursor.getInt(
                        cursor.getColumnIndexOrThrow(SQLiteHelper.POI_PROFILE_INCLUDE_FAVORITES)) == 1 ? true : false;
                profile = new PoiProfile(
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(SQLiteHelper.POI_PROFILE_ID)),
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(SQLiteHelper.POI_PROFILE_NAME)),
                        PoiCategory.listFromJson(jsonPOICategoryIdList),
                        includeFavorites);
            } catch (IllegalArgumentException | JSONException     e) {
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
        return numberOfRowsAffected == 1 ? true : false;
    }

    public boolean removePoiProfile(PoiProfile profile) {
        int numberOfRowsAffected = database.delete(
                SQLiteHelper.TABLE_POI_PROFILE,
                SQLiteHelper.POI_PROFILE_ID + " = ?",
                new String[]{String.valueOf(profile.getId())});
        return numberOfRowsAffected == 1 ? true : false;
    }

}
