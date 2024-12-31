package org.walkersguide.android.database.util;

import org.walkersguide.android.data.object_with_id.Route.PointListItem;
import org.walkersguide.android.server.wg.poi.PoiCategory;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.server.wg.poi.PoiProfile.PoiProfileParams;

import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.data.ObjectWithId.ObjectWithIdParams;
import org.walkersguide.android.database.profile.Collection;
import org.walkersguide.android.database.profile.Collection.CollectionParams;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.ObjectTypeFilter;
import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.util.GlobalInstance;
import android.content.ContentValues;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import android.text.TextUtils;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ListIterator;
import java.util.Collections;
import timber.log.Timber;
import java.util.Locale;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.database.profile.static_profile.HistoryProfile;
import org.walkersguide.android.database.profile.StaticProfile;
import java.util.Map;
import java.util.HashMap;
import org.walkersguide.android.data.object_with_id.common.Coordinates;


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
     */

    public ArrayList<ObjectWithId> getObjectListFor(DatabaseProfileRequest request) {
        DatabaseProfile profile = request.getProfile();
        String searchTerm = request.getSearchTerm();
        SortMethod sortMethod = request.getSortMethod();
        ArrayList<ObjectWithId> objectList = new ArrayList<ObjectWithId>();

        // build sql query
        String objectTableName = SQLiteHelper.TABLE_OBJECTS;
        String objectTableColumnId = SQLiteHelper.OBJECTS_ID;
        String objectTableColumnData = SQLiteHelper.OBJECTS_DATA;
        String objectTableColumnCustomName = SQLiteHelper.OBJECTS_CUSTOM_NAME;
        String objectTableColumnUserAnnotation = SQLiteHelper.OBJECTS_USER_ANNOTATION;
        ArrayList<String> queryList = new ArrayList<String>();

        // select
        queryList.add("SELECT");
        queryList.add(
                String.format(
                    Locale.ROOT, "%1$s.%2$s AS %2$s", objectTableName, objectTableColumnData));
        queryList.add(", ");
        queryList.add(
                String.format(
                    Locale.ROOT, "%1$s.%2$s AS %2$s", objectTableName, objectTableColumnCustomName));
        queryList.add(", ");
        queryList.add(
                String.format(
                    Locale.ROOT, "%1$s.%2$s AS %2$s", objectTableName, objectTableColumnUserAnnotation));

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
        Cursor cursor = null;
        try {
            cursor = database.rawQuery(
                    TextUtils.join(" ", queryList), new String[]{String.valueOf(profile.getId())});
        } catch (Exception e) {
            return objectList;
        }
        while (cursor.moveToNext()) {
            try {

                ObjectWithId objectFromDb = createObjectWithIdFrom(cursor);
                if (request.hasObjectTypeFilter()) {
                    switch (request.getObjectTypeFilter()) {
                        case ALL:
                            objectList.add(objectFromDb);
                            break;
                        case POINTS:
                            if (objectFromDb instanceof Point) {
                                objectList.add(objectFromDb);
                            }
                            break;
                        case ROUTES:
                            if (objectFromDb instanceof Route) {
                                objectList.add(objectFromDb);
                            }
                            break;
                        case SEGMENTS:
                            if (objectFromDb instanceof Segment) {
                                objectList.add(objectFromDb);
                            }
                            break;
                    }
                } else {
                    objectList.add(objectFromDb);
                }

            } catch (IllegalArgumentException | JSONException e) {
                Timber.e("error: %1$s", e.toString());
            }
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
            case DISTANCE_ASC:
            case DISTANCE_DESC:
                Collections.sort(
                        objectList, new ObjectWithId.SortByDistanceRelativeToCurrentLocation(sortMethod.isAscending()));
                break;
        }

        return objectList;
    }

    public ArrayList<DatabaseProfile> getDatabaseProfileListFor(ObjectWithId objectWithId) {
        ArrayList<DatabaseProfile> profileList = new ArrayList<DatabaseProfile>();

        Cursor cursor = null;
        try {
            cursor = database.query(
                    SQLiteHelper.TABLE_MAPPING, SQLiteHelper.TABLE_MAPPING_ALL_COLUMNS,
                    String.format(
                        Locale.ROOT,
                        "%1$s = %2$d", 
                        SQLiteHelper.MAPPING_OBJECT_ID,
                        objectWithId.getId()),
                    null, null, null, SQLiteHelper.MAPPING_PROFILE_ID + " ASC");
        } catch (Exception e) {
            return profileList;
        }

        while (cursor.moveToNext()) {
            // create profile
            DatabaseProfile  profile = null;
            try {
                profile = DatabaseProfile.load(
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
        // add dependencies
        if (! profile.equals(HistoryProfile.allPoints())
                && profile instanceof HistoryProfile && object instanceof Point) {
            Timber.d("addObjectToDatabaseProfile: found point and history profile but not all_points, add to latter first");
            if (! addObjectToDatabaseProfile(object, HistoryProfile.allPoints())) {
                return false;
            }
        } else if (! profile.equals(HistoryProfile.allRoutes())
                && profile instanceof HistoryProfile && object instanceof Route) {
            Timber.d("addObjectToDatabaseProfile: found route and history profile but not all_routes, add to latter first");
            if (! addObjectToDatabaseProfile(object, HistoryProfile.allRoutes())) {
                return false;
            }
        } else if (profile.equals(StaticProfile.pinnedObjectsWithId())) {
            Timber.d("addObjectToDatabaseProfile: found profile pinnedObjectsWithId, add to history first");
            if (! addObjectToDatabaseProfile(object, HistoryProfile.pinnedObjectsWithId())) {
                return false;
            }
        } else if (profile.equals(StaticProfile.trackedObjectsWithId())) {
            Timber.d("addObjectToDatabaseProfile: found profile trackedObjectsWithId, add to history first");
            if (! addObjectToDatabaseProfile(object, HistoryProfile.trackedObjectsWithId())) {
                return false;
            }
        } else if (object == null || ! object.saveToDatabase()) {
            Timber.e("addObjectToDatabaseProfile: Could not save object to database");
            return false;
        }

        // try to get access value of possibly existing mapping table row
        long created = System.currentTimeMillis();
        Cursor cursor = null;
        try {
            cursor = database.query(
                    SQLiteHelper.TABLE_MAPPING, SQLiteHelper.TABLE_MAPPING_ALL_COLUMNS,
                    String.format(
                        Locale.ROOT,
                        "%1$s = %2$d AND %3$s = %4$d",
                        SQLiteHelper.MAPPING_PROFILE_ID, profile.getId(),
                        SQLiteHelper.MAPPING_OBJECT_ID, object.getId()),
                    null, null, null, null);
        } catch (Exception e) {
            cursor = null;
        } finally {
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    try {
                        created = cursor.getLong(
                                cursor.getColumnIndexOrThrow(SQLiteHelper.MAPPING_CREATED));
                    } catch (IllegalArgumentException e) {}
                }
                cursor.close();
            }
        }

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
        int numberOfRemovedRows = 0;
        if (object != null) {
            if (profile != null) {
                numberOfRemovedRows = database.delete(
                        SQLiteHelper.TABLE_MAPPING,
                        String.format(
                            Locale.ROOT,
                            "%1$s = ? AND %2$s = ?", SQLiteHelper.MAPPING_PROFILE_ID, SQLiteHelper.MAPPING_OBJECT_ID),
                        new String[] {
                            String.valueOf(profile.getId()), String.valueOf(object.getId())});
            } else {
                numberOfRemovedRows = database.delete(
                        SQLiteHelper.TABLE_MAPPING,
                        String.format(
                            Locale.ROOT, "%1$s = ?", SQLiteHelper.MAPPING_OBJECT_ID),
                        new String[] { String.valueOf(object.getId()) });
            }
        }
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
     * objects
     */

    public ArrayList<PointListItem> lookUpPointsInDatabaseByCoornates(ArrayList<PointListItem> inputListItems) {
        Timber.d("inputList: \n  %1$s", TextUtils.join("\n  ", inputListItems));
        Cursor cursor = null;
        try {
            cursor = database.query(
                    SQLiteHelper.TABLE_OBJECTS, SQLiteHelper.TABLE_OBJECTS_ALL_COLUMNS,
                    null, null, null, null, null);
        } catch (Exception e) {
            return inputListItems;
        }

        ArrayList<PointListItem> outputListItems = new ArrayList<PointListItem>();
        while (cursor.moveToNext() && ! inputListItems.isEmpty()) {
            Point pointFromDatabase = null;
            try {
                ObjectWithId objectWithId = createObjectWithIdFrom(cursor);
                if (objectWithId instanceof Point) {
                    pointFromDatabase = (Point) objectWithId;
                }
            } catch (IllegalArgumentException | JSONException e) {}
            if (pointFromDatabase == null) continue;

            ListIterator<PointListItem> inputListItemsIterator = inputListItems.listIterator();
            while(inputListItemsIterator.hasNext()){
                PointListItem inputListItem = inputListItemsIterator.next();
                Point pointFromInputListItem = inputListItem.point;
                if (pointFromDatabase.getCoordinates().equals(pointFromInputListItem.getCoordinates())) {
                    Timber.d("coordinate match found: replace %1$s with %2$s from db", pointFromInputListItem.toString(), pointFromDatabase.toString());
                    // update point name with that from the gpx file
                    if (! pointFromDatabase.getName().equals(pointFromInputListItem.getName())
                            && ! TextUtils.isEmpty(pointFromInputListItem.getName())
                            && inputListItem.isImportant) {
                        pointFromDatabase.setCustomNameInDatabase(pointFromInputListItem.getName());
                        Timber.d("set name to %1$s", pointFromDatabase.getName());
                    }
                    outputListItems.add(
                            new PointListItem(
                                pointFromDatabase, inputListItem.isImportant));
                    inputListItemsIterator.remove();
                    break;
                }
            }
        }

        cursor.close();
        outputListItems.addAll(inputListItems);
        Timber.d("outputList: \n  %1$s", TextUtils.join("\n  ", outputListItems));
        return outputListItems;
    }

    public ObjectWithId getObjectWithId(long id) {
        ObjectWithId objectWithId = null;
        Cursor cursor = null;
        try {
            cursor = database.query(
                    SQLiteHelper.TABLE_OBJECTS, SQLiteHelper.TABLE_OBJECTS_ALL_COLUMNS,
                    String.format(
                        Locale.ROOT, "%1$s = %2$d", SQLiteHelper.OBJECTS_ID, id),
                    null, null, null, null);
        } catch (Exception e) {
            return objectWithId;
        }
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            try {
                objectWithId = createObjectWithIdFrom(cursor);
            } catch (IllegalArgumentException | JSONException e) {}
        }
        cursor.close();
        return objectWithId;
    }

    public ObjectWithIdParams getObjectWithIdParams(long id) {
        ObjectWithIdParams params = null;
        Cursor cursor = null;
        try {
            cursor = database.query(
                    SQLiteHelper.TABLE_OBJECTS, SQLiteHelper.TABLE_OBJECTS_ALL_COLUMNS,
                    String.format(
                        Locale.ROOT, "%1$s = %2$d", SQLiteHelper.OBJECTS_ID, id),
                    null, null, null, null);
        } catch (Exception e) {
            return params;
        }
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            params = new ObjectWithIdParams();
            try {
                params.id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(SQLiteHelper.OBJECTS_ID));
                params.data = cursor.getString(
                        cursor.getColumnIndexOrThrow(SQLiteHelper.OBJECTS_DATA));
                params.customName = cursor.getString(
                        cursor.getColumnIndexOrThrow(SQLiteHelper.OBJECTS_CUSTOM_NAME));
                params.userAnnotation = cursor.getString(
                        cursor.getColumnIndexOrThrow(SQLiteHelper.OBJECTS_USER_ANNOTATION));
            } catch (IllegalArgumentException e) {
                params = null;
            }
        }
        cursor.close();
        return params;
    }

    public boolean addOrUpdateObjectWithId(ObjectWithIdParams params) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.OBJECTS_ID, params.id);
        values.put(SQLiteHelper.OBJECTS_DATA, params.data);
        values.put(SQLiteHelper.OBJECTS_CUSTOM_NAME, params.customName);
        values.put(SQLiteHelper.OBJECTS_USER_ANNOTATION, params.userAnnotation);
        long rowIdTableObjectWithIds = database.insertWithOnConflict(
                SQLiteHelper.TABLE_OBJECTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        Timber.d("addOrUpdateObjectWithId: rowId=%1$d --  insert into %2$s table", rowIdTableObjectWithIds, SQLiteHelper.TABLE_OBJECTS);
        return rowIdTableObjectWithIds == -1 ? false : true;
    }

    public boolean removeObjectWithId(ObjectWithId object) {
        int numberOfRemovedRows = 0;
        if (object != null) {
            // remove from all database profiles
            removeObjectFromDatabaseProfile(object, null);
            // remove from objects table
            numberOfRemovedRows = database.delete(
                    SQLiteHelper.TABLE_OBJECTS,
                    String.format(
                        Locale.ROOT, "%1$s = ?", SQLiteHelper.OBJECTS_ID),
                    new String[] { String.valueOf(object.getId()) });
        }
        return numberOfRemovedRows > 0 ? true : false;
    }

    private void showPointsWithSameCoordinates() {
        Cursor cursor = null;
        try {
            cursor = database.query(
                    SQLiteHelper.TABLE_OBJECTS, SQLiteHelper.TABLE_OBJECTS_ALL_COLUMNS,
                    null, null, null, null, null);
        } catch (Exception e) {
            return;
        }

        HashMap<Integer,ArrayList<Point>> map = new HashMap<Integer,ArrayList<Point>>();
        while (cursor.moveToNext()) {
            Point pointFromDatabase = null;
            try {
                ObjectWithId objectWithId = createObjectWithIdFrom(cursor);
                if (objectWithId instanceof Point) {
                    pointFromDatabase = (Point) objectWithId;
                }
            } catch (IllegalArgumentException | JSONException e) {}
            if (pointFromDatabase == null) continue;

            ArrayList<Point> pointListForCoordinates = map.get(pointFromDatabase.getCoordinates().hashCode());
            if (pointListForCoordinates == null) pointListForCoordinates = new ArrayList<Point>();
            pointListForCoordinates.add(pointFromDatabase);
            map.put(pointFromDatabase.getCoordinates().hashCode(), pointListForCoordinates);
        }
        cursor.close();

        int collisions = 0;
        for (Map.Entry<Integer,ArrayList<Point>> entry : map.entrySet()) {
            if (entry.getValue().size() > 1) {
                Timber.d("Coordinates %1$s:\n  - %2$s", entry.getKey(), TextUtils.join("\n  - ", entry.getValue()));
                collisions++;
            }
        }
        Timber.d("%1$d collisions", collisions);
    }


    /**
     * Collection
     */

    public ArrayList<Collection> getCollectionList() {
        ArrayList<Collection> profileList = new ArrayList<Collection>();
        for (Long profileId : getIdList(
                    SQLiteHelper.TABLE_COLLECTION,
                    SQLiteHelper.COLLECTION_ID,
                    String.format(
                        Locale.ROOT,
                        "%1$s >= %2$d AND %1$s <= %3$d",
                        SQLiteHelper.COLLECTION_ID,
                        SQLiteHelper.TABLE_COLLECTION_FIRST_ID,
                        SQLiteHelper.TABLE_COLLECTION_LAST_ID))) {
            Collection profile = Collection.load(profileId);
            if (profile != null) {
                profileList.add(profile);
            }
        }
        Collections.sort(profileList);
        return profileList;
    }

    public CollectionParams getCollectionParams(long id) {
        CollectionParams params = null;
        Cursor cursor = null;
        try {
            cursor = database.query(
                    SQLiteHelper.TABLE_COLLECTION, SQLiteHelper.TABLE_COLLECTION_ALL_COLUMNS,
                    String.format(
                        Locale.ROOT, "%1$s = %2$d", SQLiteHelper.COLLECTION_ID, id),
                    null, null, null, null);
        } catch (Exception e) {
            return params;
        }
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            params = new CollectionParams();
            try {
                params.name = cursor.getString(
                        cursor.getColumnIndexOrThrow(SQLiteHelper.COLLECTION_NAME));
                params.isPinned =
                    cursor.getInt(
                            cursor.getColumnIndexOrThrow(SQLiteHelper.COLLECTION_IS_PINNED)) == 1
                    ? true : false;
            } catch (IllegalArgumentException e) {
                params = null;
            }
        }
        cursor.close();
        return params;
    }

    public Collection addCollection(CollectionParams params) {
        long newCollectionId = database.insertWithOnConflict(
                SQLiteHelper.TABLE_COLLECTION,
                null,
                createContentValuesForCollectionTable(params),
                SQLiteDatabase.CONFLICT_REPLACE);
        return Collection.load(newCollectionId);
    }

    public boolean updateCollection(long id, CollectionParams params) {
        int numberOfRowsAffected = database.updateWithOnConflict(
                SQLiteHelper.TABLE_COLLECTION,
                createContentValuesForCollectionTable(params),
                SQLiteHelper.COLLECTION_ID + " = ?",
                new String[]{String.valueOf(id)},
                SQLiteDatabase.CONFLICT_REPLACE);
        return numberOfRowsAffected == 1 ? true : false;
    }

    public boolean removeCollection(long id) {
        int numberOfRowsAffected = database.delete(
                SQLiteHelper.TABLE_COLLECTION,
                SQLiteHelper.COLLECTION_ID + " = ?",
                new String[]{String.valueOf(id)});
        return numberOfRowsAffected == 1 ? true : false;
    }

    private ContentValues createContentValuesForCollectionTable(CollectionParams params) {
        ContentValues values = new ContentValues();
        values.put(
                SQLiteHelper.COLLECTION_NAME,
                params.name);
        values.put(
                SQLiteHelper.COLLECTION_IS_PINNED,
                params.isPinned ? 1 : 0);
        return values;
    }


    /**
     * POIProfile
     */

    public ArrayList<PoiProfile> getPoiProfileList() {
        ArrayList<PoiProfile> profileList = new ArrayList<PoiProfile>();
        for (Long profileId : getIdList(
                    SQLiteHelper.TABLE_POI_PROFILE,
                    SQLiteHelper.POI_PROFILE_ID,
                    String.format(
                        Locale.ROOT,
                        "%1$s >= %2$d AND %1$s <= %3$d",
                        SQLiteHelper.POI_PROFILE_ID,
                        SQLiteHelper.TABLE_POI_PROFILE_FIRST_ID,
                        SQLiteHelper.TABLE_POI_PROFILE_LAST_ID))) {
            PoiProfile profile = PoiProfile.load(profileId);
            if (profile != null) {
                profileList.add(profile);
            }
        }
        Collections.sort(profileList);
        return profileList;
    }

    public PoiProfileParams getPoiProfileParams(long id) {
        PoiProfileParams poiProfileParams = null;
        Cursor cursor = null;
        try {
            cursor = database.query(
                    SQLiteHelper.TABLE_POI_PROFILE, SQLiteHelper.TABLE_POI_PROFILE_ALL_COLUMNS,
                    String.format(
                        Locale.ROOT, "%1$s = %2$d", SQLiteHelper.POI_PROFILE_ID, id),
                    null, null, null, null);
        } catch (Exception e) {
            return poiProfileParams;
        }
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            poiProfileParams = new PoiProfileParams();
            try {
                poiProfileParams.name = cursor.getString(
                        cursor.getColumnIndexOrThrow(SQLiteHelper.POI_PROFILE_NAME));
                poiProfileParams.isPinned =
                    cursor.getInt(
                            cursor.getColumnIndexOrThrow(SQLiteHelper.POI_PROFILE_IS_PINNED)) == 1
                    ? true : false;
                poiProfileParams.poiCategoryList = PoiCategory.listFromJson(
                        new JSONArray(
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(SQLiteHelper.POI_PROFILE_POI_CATEGORY_ID_LIST))));
                poiProfileParams.collectionList = Collection.listFromJson(
                        new JSONArray(
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(SQLiteHelper.POI_PROFILE_COLLECTION_ID_LIST))));
            } catch (IllegalArgumentException | JSONException e) {
                poiProfileParams = null;
            }
        }
        cursor.close();
        return poiProfileParams;
    }

    public PoiProfile addPoiProfile(PoiProfileParams params) {
        long newPoiProfileId = database.insertWithOnConflict(
                SQLiteHelper.TABLE_POI_PROFILE,
                null,
                createContentValuesForPoiProfileTable(params),
                SQLiteDatabase.CONFLICT_REPLACE);
        return PoiProfile.load(newPoiProfileId);
    }

    public boolean updatePoiProfile(long id, PoiProfileParams params) {
        int numberOfRowsAffected = database.updateWithOnConflict(
                SQLiteHelper.TABLE_POI_PROFILE,
                createContentValuesForPoiProfileTable(params),
                SQLiteHelper.POI_PROFILE_ID + " = ?",
                new String[]{String.valueOf(id)},
                SQLiteDatabase.CONFLICT_REPLACE);
        return numberOfRowsAffected == 1 ? true : false;
    }

    public boolean removePoiProfile(long id) {
        int numberOfRowsAffected = database.delete(
                SQLiteHelper.TABLE_POI_PROFILE,
                SQLiteHelper.POI_PROFILE_ID + " = ?",
                new String[]{String.valueOf(id)});
        return numberOfRowsAffected == 1 ? true : false;
    }

    private ContentValues createContentValuesForPoiProfileTable(PoiProfileParams params) {
        ContentValues values = new ContentValues();
        values.put(
                SQLiteHelper.POI_PROFILE_NAME,
                params.name);
        values.put(
                SQLiteHelper.POI_PROFILE_IS_PINNED,
                params.isPinned ? 1 : 0);
        values.put(
                SQLiteHelper.POI_PROFILE_POI_CATEGORY_ID_LIST,
                PoiCategory.listToJson(params.poiCategoryList).toString());
        values.put(
                SQLiteHelper.POI_PROFILE_COLLECTION_ID_LIST,
                Collection.listToJson(params.collectionList).toString());
        return values;
    }


    /**
     * MutableProfile
     */

    public ArrayList<Profile> getPinnedProfileList() {
        ArrayList<Profile> profileList = new ArrayList<Profile>();
        for (Collection collection : getCollectionList()) {
            if (collection.isPinned()) {
                profileList.add(collection);
            }
        }
        for (PoiProfile poiProfile : getPoiProfileList()) {
            if (poiProfile.isPinned()) {
                profileList.add(poiProfile);
            }
        }
        Collections.sort(profileList);
        return profileList;
    }

    public void clearPinnedProfileList() {
        for (Collection collection : getCollectionList()) {
            collection.setPinned(false);
        }
        for (PoiProfile poiProfile : getPoiProfileList()) {
            poiProfile.setPinned(false);
        }
    }


    /**
     * helper
     */

    private ObjectWithId createObjectWithIdFrom(Cursor cursor) throws IllegalArgumentException, JSONException {
        return ObjectWithId.fromJson(
                new JSONObject(
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(SQLiteHelper.OBJECTS_DATA))));
    }

    public ArrayList<Long> getIdList(String tableName, String tableColumnId, String whereClause) {
        ArrayList<Long> idList = new ArrayList<Long>();

        Cursor cursor = null;
        try {
            cursor = database.query(
                    tableName, new String[]{tableColumnId}, whereClause, null, null, null, null);
        } catch (Exception e) {
            return idList;
        }

        while (cursor.moveToNext()) {
            Long id = null;
            try {
                id = Long.valueOf(
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(tableColumnId)));
            } catch (IllegalArgumentException e) {
            } finally {
                if (id != null) {
                    idList.add(id);
                }
            }
        }
        cursor.close();

        return idList;
    }

}
