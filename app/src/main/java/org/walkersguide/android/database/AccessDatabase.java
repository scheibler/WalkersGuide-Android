package org.walkersguide.android.database;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.FavoritesProfile;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.data.profile.POIProfile;
import org.walkersguide.android.data.profile.SearchFavoritesProfile;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.data.route.RouteObject;
import org.walkersguide.android.data.server.OSMMap;
import org.walkersguide.android.data.server.PublicTransportProvider;
import org.walkersguide.android.util.Constants;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.data.basic.segment.Footway;
import org.walkersguide.android.server.FavoritesManager;
import android.database.SQLException;

public class AccessDatabase {

    private Context context;
    private static AccessDatabase accessDatabaseInstance;
    private SQLiteDatabase database;
    private SQLiteHelper dbHelper;

    public static AccessDatabase getInstance(Context context) {
        if(accessDatabaseInstance == null){
            accessDatabaseInstance = new AccessDatabase(context.getApplicationContext());
        }
        return accessDatabaseInstance;
    }

    private AccessDatabase(Context context) {
        this.context = context;
        dbHelper = new SQLiteHelper(context);
        this.database = dbHelper.getWritableDatabase();
    }

    public void reOpen() throws SQLException {
        dbHelper.close();
        dbHelper = new SQLiteHelper(context);
        database = dbHelper.getWritableDatabase();
    }

    public void setSomeDefaults() {
        // insert some default favorites profiles if table is empty
        if (getFavoritesProfileMap().isEmpty()) {
            String sqlInsertFavoritesProfileQuery = String.format(
                    "INSERT INTO %1$s (%2$s, %3$s, %4$s, %5$s, %6$s) VALUES (?, ?, ?, ?, ?)",
                    SQLiteHelper.TABLE_FAVORITES_PROFILE, SQLiteHelper.FAVORITES_PROFILE_ID,
                    SQLiteHelper.FAVORITES_PROFILE_NAME, SQLiteHelper.FAVORITES_PROFILE_SORT_CRITERIA,
                    SQLiteHelper.FAVORITES_PROFILE_CENTER, SQLiteHelper.FAVORITES_PROFILE_DIRECTION);
            // all history points
            database.execSQL(
                    sqlInsertFavoritesProfileQuery,
                    new String[] {
                        String.valueOf(FavoritesProfile.ID_ALL_POINTS),
                        context.getResources().getString(R.string.fpNameAllPoints),
                        String.valueOf(Constants.SORT_CRITERIA.ORDER_DESC),
                        Constants.DUMMY.LOCATION,
                        String.valueOf(Constants.DUMMY.DIRECTION)});
            // addresses
            database.execSQL(
                    sqlInsertFavoritesProfileQuery,
                    new String[] {
                        String.valueOf(FavoritesProfile.ID_ADDRESS_POINTS),
                        context.getResources().getString(R.string.fpNameAddressPoints),
                        String.valueOf(Constants.SORT_CRITERIA.ORDER_DESC),
                        Constants.DUMMY.LOCATION,
                        String.valueOf(Constants.DUMMY.DIRECTION)});
            // route points
            database.execSQL(
                    sqlInsertFavoritesProfileQuery,
                    new String[] {
                        String.valueOf(FavoritesProfile.ID_ROUTE_POINTS),
                        context.getResources().getString(R.string.fpNameRoutePoints),
                        String.valueOf(Constants.SORT_CRITERIA.ORDER_DESC),
                        Constants.DUMMY.LOCATION,
                        String.valueOf(Constants.DUMMY.DIRECTION)});
            // simulated points
            database.execSQL(
                    sqlInsertFavoritesProfileQuery,
                    new String[] {
                        String.valueOf(FavoritesProfile.ID_SIMULATED_POINTS),
                        context.getResources().getString(R.string.fpNameSimulatedPoints),
                        String.valueOf(Constants.SORT_CRITERIA.ORDER_DESC),
                        Constants.DUMMY.LOCATION,
                        String.valueOf(Constants.DUMMY.DIRECTION)});
            // user created
            database.execSQL(
                    sqlInsertFavoritesProfileQuery,
                    new String[] {
                        String.valueOf(FavoritesProfile.ID_USER_CREATED_POINTS),
                        context.getResources().getString(R.string.fpNameUserCreatedPoints),
                        String.valueOf(Constants.SORT_CRITERIA.ORDER_DESC),
                        Constants.DUMMY.LOCATION,
                        String.valueOf(Constants.DUMMY.DIRECTION)});
            // create a custom profile and delete immediately
            database.execSQL(
                    sqlInsertFavoritesProfileQuery,
                    new String[] {
                        String.valueOf(FavoritesProfile.ID_FIRST_USER_CREATED_PROFILE),
                        "placeholder",
                        String.valueOf(Constants.SORT_CRITERIA.ORDER_DESC),
                        Constants.DUMMY.LOCATION,
                        String.valueOf(Constants.DUMMY.DIRECTION)});
            database.execSQL(
                    String.format(
                        "DELETE FROM %1$s WHERE %2$s = ?",
                        SQLiteHelper.TABLE_FAVORITES_PROFILE,
                        SQLiteHelper.FAVORITES_PROFILE_ID),
                    new String[] {
                        String.valueOf(FavoritesProfile.ID_FIRST_USER_CREATED_PROFILE)});
        }

        // add search poi profile
        POIProfile searchPOIProfile = getPOIProfile(SearchFavoritesProfile.ID_SEARCH);
        if (searchPOIProfile == null) {
            String sqlInsertPOIProfileQuery = String.format(
                    "INSERT INTO %1$s (%2$s, %3$s, %4$s, %5$s, %6$s, %7$s, %8$s, %9$s, %10$s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    SQLiteHelper.TABLE_POI_PROFILE, SQLiteHelper.POI_PROFILE_ID,
                    SQLiteHelper.POI_PROFILE_NAME, SQLiteHelper.POI_PROFILE_RADIUS,
                    SQLiteHelper.POI_PROFILE_NUMBER_OF_RESULTS, SQLiteHelper.POI_PROFILE_CATEGORY_ID_LIST,
                    SQLiteHelper.POI_PROFILE_SEARCH_TERM, SQLiteHelper.POI_PROFILE_CENTER,
                    SQLiteHelper.POI_PROFILE_DIRECTION, SQLiteHelper.POI_PROFILE_POINT_LIST);
            JSONArray jsonEmptyArray = new JSONArray();
            String emptySearchString = "";
            // add search poi profile
            database.execSQL(
                    sqlInsertPOIProfileQuery,
                    new String[] {
                        String.valueOf(SearchFavoritesProfile.ID_SEARCH),
                        context.getResources().getString(R.string.ppNameSearch),
                        String.valueOf(POIProfile.INITIAL_SEARCH_RADIUS),
                        String.valueOf(POIProfile.INITIAL_NUMBER_OF_RESULTS),
                        jsonEmptyArray.toString(),
                        emptySearchString,
                        Constants.DUMMY.LOCATION,
                        String.valueOf(Constants.DUMMY.DIRECTION),
                        jsonEmptyArray.toString() });
        }
    }


    /**
     * FavoritesProfile
     */

    public TreeMap<Integer,String> getFavoritesProfileMap() {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_FAVORITES_PROFILE, SQLiteHelper.TABLE_FAVORITES_PROFILE_ALL_COLUMNS,
                null, null, null, null, SQLiteHelper.FAVORITES_PROFILE_ID + " ASC");
        TreeMap<Integer,String> favoritesProfileMap = new TreeMap<Integer,String>();
        while (cursor.moveToNext()) {
            favoritesProfileMap.put(
                    cursor.getInt(cursor.getColumnIndex(SQLiteHelper.FAVORITES_PROFILE_ID)),
                    cursor.getString(cursor.getColumnIndex(SQLiteHelper.FAVORITES_PROFILE_NAME)));
        }
        cursor.close();
        return favoritesProfileMap;
    }

    public FavoritesProfile getFavoritesProfile(int id) {
        // get profile parameters
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_FAVORITES_PROFILE, SQLiteHelper.TABLE_FAVORITES_PROFILE_ALL_COLUMNS,
                String.format("%1$s = %2$d", SQLiteHelper.FAVORITES_PROFILE_ID, id),
                null, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
        } else {
            cursor.close();
            return null;
        }
        String favoritesProfileName = cursor.getString(
                cursor.getColumnIndex(SQLiteHelper.FAVORITES_PROFILE_NAME));
        int favoritesProfileSortCriteria = cursor.getInt(
                cursor.getColumnIndex(SQLiteHelper.FAVORITES_PROFILE_SORT_CRITERIA));
        String favoritesProfileJSONCenterSerialized = cursor.getString(
                cursor.getColumnIndex(SQLiteHelper.FAVORITES_PROFILE_CENTER));
        int favoritesProfileDirection = cursor.getInt(
                cursor.getColumnIndex(SQLiteHelper.FAVORITES_PROFILE_DIRECTION));
        cursor.close();

        // get points for selected profile
        JSONArray jsonPointList = new JSONArray();
        cursor = database.rawQuery(
                String.format(
                    "SELECT %1$s.%2$s AS %3$s, %4$s.%5$s AS %6$s FROM %7$s LEFT JOIN %8$s WHERE %9$s.%10$s = %11$s.%12$s AND %13$s.%14$s = ?",
                    SQLiteHelper.TABLE_POINT, SQLiteHelper.POINT_DATA, SQLiteHelper.POINT_DATA,
                    SQLiteHelper.TABLE_FP_POINTS, SQLiteHelper.FP_POINTS_ORDER, SQLiteHelper.FP_POINTS_ORDER,
                    SQLiteHelper.TABLE_FP_POINTS, SQLiteHelper.TABLE_POINT,
                    SQLiteHelper.TABLE_FP_POINTS, SQLiteHelper.FP_POINTS_POINT_ID,
                    SQLiteHelper.TABLE_POINT, SQLiteHelper.POINT_ID,
                    SQLiteHelper.TABLE_FP_POINTS, SQLiteHelper.FP_POINTS_PROFILE_ID),
                new String[]{String.valueOf(id)});
        while (cursor.moveToNext()) {
            JSONObject jsonPoint = null;
            try {
                jsonPoint = new JSONObject(
                        cursor.getString(cursor.getColumnIndex(SQLiteHelper.POINT_DATA)));
                jsonPoint.put("order", cursor.getLong(cursor.getColumnIndex(SQLiteHelper.FP_POINTS_ORDER)));
            } catch (JSONException e) {
                jsonPoint = null;
            } finally {
                if (jsonPoint != null) {
                    jsonPointList.put(jsonPoint);
                }
            }
        }
        cursor.close();

        // create favorites profile object
        try {
            return new FavoritesProfile(
                    this.context,
                    id,
                    favoritesProfileName,
                    favoritesProfileSortCriteria,
                    new JSONObject(favoritesProfileJSONCenterSerialized),
                    favoritesProfileDirection,
                    jsonPointList);
        } catch (JSONException e) {
            return null;
        }
    }

    public String getNameOfFavoritesProfile(int id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_FAVORITES_PROFILE, SQLiteHelper.TABLE_FAVORITES_PROFILE_ALL_COLUMNS,
                String.format("%1$s = %2$d", SQLiteHelper.FAVORITES_PROFILE_ID, id),
                null, null, null, null);
        String name = "";
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            name = cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.FAVORITES_PROFILE_NAME));
        }
        cursor.close();
        return name;
    }

    public int getSortCriteriaOfFavoritesProfile(int id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_FAVORITES_PROFILE, SQLiteHelper.TABLE_FAVORITES_PROFILE_ALL_COLUMNS,
                String.format("%1$s = %2$d", SQLiteHelper.FAVORITES_PROFILE_ID, id),
                null, null, null, null);
        int sortCriteria = Constants.SORT_CRITERIA.ORDER_DESC;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            sortCriteria = cursor.getInt(
                    cursor.getColumnIndex(SQLiteHelper.FAVORITES_PROFILE_SORT_CRITERIA));
        }
        cursor.close();
        return sortCriteria;
    }

    public int addFavoritesProfile(String newName, int newSortCriteria) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.FAVORITES_PROFILE_NAME, newName);
        values.put(SQLiteHelper.FAVORITES_PROFILE_SORT_CRITERIA, newSortCriteria);
        values.put(SQLiteHelper.FAVORITES_PROFILE_CENTER, Constants.DUMMY.LOCATION);
        values.put(SQLiteHelper.FAVORITES_PROFILE_DIRECTION, Constants.DUMMY.DIRECTION);
        return (int) database.insertWithOnConflict(
                SQLiteHelper.TABLE_FAVORITES_PROFILE,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public boolean updateNameAndSortCriteriaOfFavoritesProfile(
            int id, String newName, int newSortCriteria) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.FAVORITES_PROFILE_NAME, newName);
        values.put(SQLiteHelper.FAVORITES_PROFILE_SORT_CRITERIA, newSortCriteria);
        int numberOfRowsAffected = database.updateWithOnConflict(
                SQLiteHelper.TABLE_FAVORITES_PROFILE,
                values,
                SQLiteHelper.FAVORITES_PROFILE_ID + " = ?", new String[]{String.valueOf(id)},
                SQLiteDatabase.CONFLICT_IGNORE);
        return numberOfRowsAffected == 1 ? true : false;
    }

    public boolean updateCenterAndDirectionOfFavoritesProfile(
            int id, PointWrapper newCenter, int newDirection) {
        ContentValues values = new ContentValues();
        try {
            values.put(SQLiteHelper.FAVORITES_PROFILE_CENTER, newCenter.toJson().toString());
        } catch (JSONException | NullPointerException e) {
            values.put(SQLiteHelper.FAVORITES_PROFILE_CENTER, "");
        }
        values.put(SQLiteHelper.FAVORITES_PROFILE_DIRECTION, newDirection);
        int numberOfRowsAffected = database.updateWithOnConflict(
                SQLiteHelper.TABLE_FAVORITES_PROFILE,
                values,
                SQLiteHelper.FAVORITES_PROFILE_ID + " = ?", new String[]{String.valueOf(id)},
                SQLiteDatabase.CONFLICT_IGNORE);
        return numberOfRowsAffected == 1 ? true : false;
    }

    public void clearFavoritesProfile(int id) {
        // delete points from FPPoints table
        database.delete(
                SQLiteHelper.TABLE_FP_POINTS,
                String.format("%1$s = ?", SQLiteHelper.FP_POINTS_PROFILE_ID),
                new String[] {String.valueOf(id)});
        // delete orphan points
        deleteOrphanPointsOfFavoriteProfile(id);
        // kill previous favorites search instance
        FavoritesManager.getInstance(this.context).destroySearchInstance();
    }

    public void removeFavoritesProfile(int id) {
        // delete profile row from favorites_profile table
        database.delete(
                SQLiteHelper.TABLE_FAVORITES_PROFILE,
                String.format("%1$s = ?", SQLiteHelper.FAVORITES_PROFILE_ID),
                new String[] {String.valueOf(id)});
        // delete orphan points
        deleteOrphanPointsOfFavoriteProfile(id);
        // kill previous favorites search instance
        FavoritesManager.getInstance(this.context).destroySearchInstance();
    }

    public TreeSet<Integer> getCheckedFavoritesProfileIdsForPoint(PointWrapper pointWrapper) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_FP_POINTS, SQLiteHelper.TABLE_FP_POINTS_ALL_COLUMNS,
                String.format(
                    "%1$s = %2$d", 
                    SQLiteHelper.FP_POINTS_POINT_ID,
                    pointWrapper.hashCode()),
                null, null, null, SQLiteHelper.FP_POINTS_PROFILE_ID + " ASC");
        TreeSet<Integer> profileIdSet = new TreeSet<Integer>();
        while (cursor.moveToNext()) {
            profileIdSet.add(
                    cursor.getInt(cursor.getColumnIndex(SQLiteHelper.FP_POINTS_PROFILE_ID)));
        }
        cursor.close();
        return profileIdSet;
    }

    public void addPointToFavoritesProfile(PointWrapper pointToAdd, int profileId) {
        if (pointToAdd == null) {
            return;
        } else if (profileId != FavoritesProfile.ID_ALL_POINTS) {
            this.addPointToFavoritesProfile(pointToAdd, FavoritesProfile.ID_ALL_POINTS);
        } else {
            // add point to point table
            ContentValues values = new ContentValues();
            values.put(SQLiteHelper.POINT_ID, pointToAdd.hashCode());
            try {
                values.put(SQLiteHelper.POINT_DATA, pointToAdd.toJson().toString());
            } catch (JSONException e) {
                values.put(SQLiteHelper.POINT_DATA, "");
            }
            database.insertWithOnConflict(
                    SQLiteHelper.TABLE_POINT,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE);
        }
        // add point to point FPPoints table or update order column
        ContentValues fpValues = new ContentValues();
        fpValues.put(SQLiteHelper.FP_POINTS_PROFILE_ID, profileId);
        fpValues.put(SQLiteHelper.FP_POINTS_POINT_ID, pointToAdd.hashCode());
        fpValues.put(SQLiteHelper.FP_POINTS_ORDER, System.currentTimeMillis());
        database.insertWithOnConflict(
                SQLiteHelper.TABLE_FP_POINTS,
                null,
                fpValues,
                SQLiteDatabase.CONFLICT_REPLACE);
        // kill previous favorites search instance
        FavoritesManager.getInstance(this.context).destroySearchInstance();
    }

    public void removePointFromFavoritesProfile(PointWrapper pointToRemove, int profileId) {
        if (pointToRemove == null) {
            return;
        }
        // delete point from FPPoints table
        database.delete(
                SQLiteHelper.TABLE_FP_POINTS,
                String.format(
                    "%1$s = ? AND %2$s = ?", SQLiteHelper.FP_POINTS_PROFILE_ID, SQLiteHelper.FP_POINTS_POINT_ID),
                new String[] {
                    String.valueOf(profileId), String.valueOf(pointToRemove.hashCode())});
        // delete point from points table if not longer part of any favorites profile
        deleteOrphanPointsOfFavoriteProfile(profileId);
        // kill previous favorites search instance
        FavoritesManager.getInstance(this.context).destroySearchInstance();
    }

    private void deleteOrphanPointsOfFavoriteProfile(int id) {
        database.execSQL(
                String.format(
                    "DELETE FROM %1$s where %2$s IN (SELECT %3$s.%4$s FROM %5$s LEFT JOIN %6$s ON %7$s.%8$s = %9$s.%10$s WHERE %11$s.%12$s IS NULL);",
                    SQLiteHelper.TABLE_POINT, SQLiteHelper.POINT_ID,
                    SQLiteHelper.TABLE_POINT, SQLiteHelper.POINT_ID,
                    SQLiteHelper.TABLE_POINT, SQLiteHelper.TABLE_FP_POINTS,
                    SQLiteHelper.TABLE_POINT, SQLiteHelper.POINT_ID,
                    SQLiteHelper.TABLE_FP_POINTS, SQLiteHelper.FP_POINTS_POINT_ID,
                    SQLiteHelper.TABLE_FP_POINTS, SQLiteHelper.FP_POINTS_POINT_ID)
                );
    }


    /**
     * POIProfile
     */

    public TreeMap<Integer,String> getPOIProfileMap() {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_POI_PROFILE, SQLiteHelper.TABLE_POI_PROFILE_ALL_COLUMNS,
                String.format("%1$s != %2$d", SQLiteHelper.POI_PROFILE_ID, SearchFavoritesProfile.ID_SEARCH),
                null, null, null, SQLiteHelper.POI_PROFILE_ID + " ASC");
        TreeMap<Integer,String> poiProfileMap = new TreeMap<Integer,String>();
        while (cursor.moveToNext()) {
            poiProfileMap.put(
                    cursor.getInt(cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_ID)),
                    cursor.getString(cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_NAME)));
        }
        cursor.close();
        return poiProfileMap;
    }

    public POIProfile getPOIProfile(int id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_POI_PROFILE, SQLiteHelper.TABLE_POI_PROFILE_ALL_COLUMNS,
                SQLiteHelper.POI_PROFILE_ID + " = " + id,
                null, null, null, null);
        POIProfile poiProfile = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            try {
                poiProfile = new POIProfile(
                        this.context,
                        cursor.getInt(
                            cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_ID)),
                        cursor.getString(
                            cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_NAME)),
                        cursor.getInt(
                            cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_RADIUS)),
                        cursor.getInt(
                            cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_NUMBER_OF_RESULTS)),
                        new JSONArray(
                            cursor.getString(
                                cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_CATEGORY_ID_LIST))),
                        cursor.getString(
                            cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_SEARCH_TERM)),
                        new JSONObject(
                            cursor.getString(
                                cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_CENTER))),
                        cursor.getInt(
                            cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_DIRECTION)),
                        new JSONArray(
                            cursor.getString(
                                cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_POINT_LIST))));
            } catch (JSONException e) {
                poiProfile = null;
            }
        }
        cursor.close();
        return poiProfile;
    }

    public String getNameOfPOIProfile(int id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_POI_PROFILE, SQLiteHelper.TABLE_POI_PROFILE_ALL_COLUMNS,
                String.format("%1$s = %2$d", SQLiteHelper.POI_PROFILE_ID, id),
                null, null, null, null);
        String name = "";
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            name = cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_NAME));
        }
        cursor.close();
        return name;
    }

    public ArrayList<POICategory> getCategoryListOfPOIProfile(int id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_POI_PROFILE, SQLiteHelper.TABLE_POI_PROFILE_ALL_COLUMNS,
                String.format("%1$s = %2$d", SQLiteHelper.POI_PROFILE_ID, id),
                null, null, null, null);
        ArrayList<POICategory> poiCategoryList = new ArrayList<POICategory>();
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            JSONArray jsonPOICategoryIdList = new JSONArray();
            try {
                jsonPOICategoryIdList = new JSONArray(
                        cursor.getString(
                            cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_CATEGORY_ID_LIST)));
            } catch (JSONException     e) {}
            for (int i=0; i<jsonPOICategoryIdList.length(); i++) {
                try {
                    poiCategoryList.add(
                            new POICategory(
                                context, jsonPOICategoryIdList.getString(i)));
                } catch (JSONException     e) {}
            }
        }
        cursor.close();
        return poiCategoryList;
    }

    public String getSearchTermOfPOIProfile(int id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_POI_PROFILE, SQLiteHelper.TABLE_POI_PROFILE_ALL_COLUMNS,
                String.format("%1$s = %2$d", SQLiteHelper.POI_PROFILE_ID, id),
                null, null, null, null);
        String searchTerm = "";
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            searchTerm = cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_SEARCH_TERM));
        }
        cursor.close();
        return searchTerm;
    }

    public int addPOIProfile(String name, ArrayList<POICategory> poiCategoryList) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.POI_PROFILE_NAME, name);
        values.put(SQLiteHelper.POI_PROFILE_RADIUS, POIProfile.INITIAL_RADIUS);
        values.put(SQLiteHelper.POI_PROFILE_NUMBER_OF_RESULTS, POIProfile.INITIAL_NUMBER_OF_RESULTS);
        JSONArray jsonPOICategoryIdList = new JSONArray();
        for (POICategory category : poiCategoryList) {
            jsonPOICategoryIdList.put(category.getId());
        }
        values.put(SQLiteHelper.POI_PROFILE_CATEGORY_ID_LIST, jsonPOICategoryIdList.toString());
        values.put(SQLiteHelper.POI_PROFILE_SEARCH_TERM, "");
        values.put(SQLiteHelper.POI_PROFILE_CENTER, Constants.DUMMY.LOCATION);
        values.put(SQLiteHelper.POI_PROFILE_DIRECTION, Constants.DUMMY.DIRECTION);
        values.put(SQLiteHelper.POI_PROFILE_POINT_LIST, (new JSONArray()).toString());
        return (int) database.insertWithOnConflict(
                SQLiteHelper.TABLE_POI_PROFILE,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public boolean updateNameAndCategoryListOfPOIProfile(
            int id, String name, ArrayList<POICategory> poiCategoryList) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.POI_PROFILE_NAME, name);
        values.put(SQLiteHelper.POI_PROFILE_RADIUS, id == SearchFavoritesProfile.ID_SEARCH ? POIProfile.INITIAL_SEARCH_RADIUS : POIProfile.INITIAL_RADIUS);
        values.put(SQLiteHelper.POI_PROFILE_NUMBER_OF_RESULTS, POIProfile.INITIAL_NUMBER_OF_RESULTS);
        JSONArray jsonPOICategoryIdList = new JSONArray();
        for (POICategory category : poiCategoryList) {
            jsonPOICategoryIdList.put(category.getId());
        }
        values.put(SQLiteHelper.POI_PROFILE_CATEGORY_ID_LIST, jsonPOICategoryIdList.toString());
        values.put(SQLiteHelper.POI_PROFILE_CENTER, Constants.DUMMY.LOCATION);
        values.put(SQLiteHelper.POI_PROFILE_DIRECTION, Constants.DUMMY.DIRECTION);
        values.put(SQLiteHelper.POI_PROFILE_POINT_LIST, (new JSONArray()).toString());
        int numberOfRowsAffected = database.updateWithOnConflict(
                SQLiteHelper.TABLE_POI_PROFILE,
                values,
                SQLiteHelper.POI_PROFILE_ID + " = ?", new String[]{String.valueOf(id)},
                SQLiteDatabase.CONFLICT_IGNORE);
        return numberOfRowsAffected == 1 ? true : false;
    }

    public boolean updateRadiusAndNumberOfResultsOfPOIProfile(
            int id, int newRadius, int newNumberOfResults) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.POI_PROFILE_RADIUS, newRadius);
        values.put(SQLiteHelper.POI_PROFILE_NUMBER_OF_RESULTS, newNumberOfResults);
        int numberOfRowsAffected = database.updateWithOnConflict(
                SQLiteHelper.TABLE_POI_PROFILE,
                values,
                SQLiteHelper.POI_PROFILE_ID + " = ?", new String[]{String.valueOf(id)},
                SQLiteDatabase.CONFLICT_IGNORE);
        return numberOfRowsAffected == 1 ? true : false;
    }

    public boolean updateSearchTermOfPOIProfile(int id, String newSearchTerm) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.POI_PROFILE_RADIUS, id == SearchFavoritesProfile.ID_SEARCH ? POIProfile.INITIAL_SEARCH_RADIUS : POIProfile.INITIAL_RADIUS);
        values.put(SQLiteHelper.POI_PROFILE_NUMBER_OF_RESULTS, POIProfile.INITIAL_NUMBER_OF_RESULTS);
        values.put(SQLiteHelper.POI_PROFILE_SEARCH_TERM, newSearchTerm);
        values.put(SQLiteHelper.POI_PROFILE_CENTER, Constants.DUMMY.LOCATION);
        values.put(SQLiteHelper.POI_PROFILE_DIRECTION, Constants.DUMMY.DIRECTION);
        values.put(SQLiteHelper.POI_PROFILE_POINT_LIST, (new JSONArray()).toString());
        int numberOfRowsAffected = database.updateWithOnConflict(
                SQLiteHelper.TABLE_POI_PROFILE,
                values,
                SQLiteHelper.POI_PROFILE_ID + " = ?", new String[]{String.valueOf(id)},
                SQLiteDatabase.CONFLICT_IGNORE);
        return numberOfRowsAffected == 1 ? true : false;
    }

    public boolean updateCenterDirectionANPointListOfPOIProfile(
            int id, PointWrapper newCenter, int newDirection, ArrayList<PointProfileObject> newPointList) {
        ContentValues values = new ContentValues();
        try {
            values.put(SQLiteHelper.POI_PROFILE_CENTER, newCenter.toJson().toString());
        } catch (JSONException | NullPointerException e) {
            values.put(SQLiteHelper.POI_PROFILE_CENTER, Constants.DUMMY.LOCATION);
        }
        values.put(SQLiteHelper.POI_PROFILE_DIRECTION, newDirection);
        if (newPointList == null) {
            values.put(SQLiteHelper.POI_PROFILE_POINT_LIST, (new JSONArray()).toString());
        } else {
            JSONArray jsonPointList = new JSONArray();
            for (PointProfileObject pointProfileObject : newPointList) {
                try {
                    jsonPointList.put(pointProfileObject.getPoint().toJson());
                } catch (JSONException e) {}
            }
            values.put(SQLiteHelper.POI_PROFILE_POINT_LIST, jsonPointList.toString());
        }
        int numberOfRowsAffected = database.updateWithOnConflict(
                SQLiteHelper.TABLE_POI_PROFILE,
                values,
                SQLiteHelper.POI_PROFILE_ID + " = ?", new String[]{String.valueOf(id)},
                SQLiteDatabase.CONFLICT_IGNORE);
        return numberOfRowsAffected == 1 ? true : false;
    }

    public void removePOIProfile(int id) {
        database.delete(
                SQLiteHelper.TABLE_POI_PROFILE,
                SQLiteHelper.POI_PROFILE_ID + " = ?",
                new String[] {String.valueOf(id)});
    }


    /**
     * routes
     */

    public TreeMap<Integer,String> getRouteMap() {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_ROUTE, SQLiteHelper.TABLE_ROUTE_ALL_COLUMNS,
                null, null, null, null, SQLiteHelper.ROUTE_CREATED + " ASC");
        TreeMap<Integer,String> routeMap = new TreeMap<Integer,String>();
        while (cursor.moveToNext()) {
            try {
                routeMap.put(
                        cursor.getInt(cursor.getColumnIndex(SQLiteHelper.ROUTE_ID)),
                        String.format(
                            "%1$s: %2$s\n%3$s: %4$s",
                            context.getResources().getString(R.string.buttonStartPoint),
                            (new JSONObject(
                                cursor.getString(cursor.getColumnIndex(SQLiteHelper.ROUTE_START)))).getString("name"),
                            context.getResources().getString(R.string.buttonDestinationPoint),
                            (new JSONObject(
                                cursor.getString(cursor.getColumnIndex(SQLiteHelper.ROUTE_DESTINATION)))).getString("name"))
                        );
            } catch (JSONException e) {}
        }
        cursor.close();
        return routeMap;
    }

    public Route getRoute(int id) throws JSONException {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_ROUTE, SQLiteHelper.TABLE_ROUTE_ALL_COLUMNS,
                SQLiteHelper.ROUTE_ID + " = " + id,
                null, null, null, null);
        Route route= null;
        if (cursor.moveToFirst()) {
            route = new Route(
                    this.context,
                    cursor.getInt(
                        cursor.getColumnIndex(SQLiteHelper.ROUTE_ID)),
                    new JSONObject(
                        cursor.getString(
                            cursor.getColumnIndex(SQLiteHelper.ROUTE_START))),
                    new JSONObject(
                        cursor.getString(
                            cursor.getColumnIndex(SQLiteHelper.ROUTE_DESTINATION))),
                    new JSONArray(
                        cursor.getString(
                            cursor.getColumnIndex(SQLiteHelper.ROUTE_VIA_POINT_LIST))),
                    cursor.getString(
                        cursor.getColumnIndex(SQLiteHelper.ROUTE_DESCRIPTION)),
                    cursor.getInt(
                        cursor.getColumnIndex(SQLiteHelper.ROUTE_CURRENT_OBJECT_INDEX)),
                    new JSONArray(
                        cursor.getString(
                            cursor.getColumnIndex(SQLiteHelper.ROUTE_OBJECT_LIST))));
        }
        cursor.close();
        return route;
    }

    public int getCurrentObjectIndexOfRoute(int id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_ROUTE, SQLiteHelper.TABLE_ROUTE_ALL_COLUMNS,
                SQLiteHelper.ROUTE_ID + " = " + id,
                null, null, null, null);
        int currentObjectIndex = -1;
        if (cursor.moveToFirst()) {
            currentObjectIndex = cursor.getInt(
                    cursor.getColumnIndex(SQLiteHelper.ROUTE_CURRENT_OBJECT_INDEX));
        }
        cursor.close();
        return currentObjectIndex;
    }

    public boolean setCurrentObjectIndexOfRoute(int id, int newObjectIndex) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_ROUTE, SQLiteHelper.TABLE_ROUTE_ALL_COLUMNS,
                SQLiteHelper.ROUTE_ID + " = " + id,
                null, null, null, null);
        if (cursor.moveToFirst()) {
            int currentObjectIndex = cursor.getInt(
                    cursor.getColumnIndex(SQLiteHelper.ROUTE_CURRENT_OBJECT_INDEX));
            JSONArray jsonRouteObjectList = null;
            try {
                jsonRouteObjectList = new JSONArray(
                        cursor.getString(
                            cursor.getColumnIndex(SQLiteHelper.ROUTE_OBJECT_LIST)));
            } catch (JSONException e) {
                jsonRouteObjectList = new JSONArray();
            }
            if (newObjectIndex != currentObjectIndex
                    && newObjectIndex >= 0
                    && newObjectIndex < jsonRouteObjectList.length()) {
                ContentValues values = null;
                try {
                    values = new ContentValues();
                    values.put(SQLiteHelper.ROUTE_CURRENT_OBJECT_INDEX, newObjectIndex);
                    values.put(SQLiteHelper.ROUTE_CURRENT_OBJECT_DATA, jsonRouteObjectList.getJSONObject(newObjectIndex).toString());
                } catch (JSONException e) {
                    values = null;
                }
                if (values != null) {
                    int numberOfRowsAffected = database.updateWithOnConflict(
                            SQLiteHelper.TABLE_ROUTE,
                            values,
                            SQLiteHelper.ROUTE_ID + " = ?",
                            new String[]{String.valueOf(id)},
                            SQLiteDatabase.CONFLICT_REPLACE);
                    return numberOfRowsAffected == 1 ? true : false;
                }
            }
        }
        return false;
    }

    public PointWrapper getStartPointOfRoute(int id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_ROUTE, SQLiteHelper.TABLE_ROUTE_ALL_COLUMNS,
                SQLiteHelper.ROUTE_ID + " = " + id,
                null, null, null, null);
        PointWrapper startPoint = null;
        if (cursor.moveToFirst()) {
            try {
                startPoint = new PointWrapper(
                        this.context,
                        new JSONObject(
                            cursor.getString(
                                cursor.getColumnIndex(SQLiteHelper.ROUTE_START))));
            } catch (JSONException e) {}
        }
        cursor.close();
        return startPoint;
    }

    public PointWrapper getDestinationPointOfRoute(int id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_ROUTE, SQLiteHelper.TABLE_ROUTE_ALL_COLUMNS,
                SQLiteHelper.ROUTE_ID + " = " + id,
                null, null, null, null);
        PointWrapper destinationPoint = null;
        if (cursor.moveToFirst()) {
            try {
                destinationPoint = new PointWrapper(
                        this.context,
                        new JSONObject(
                            cursor.getString(
                                cursor.getColumnIndex(SQLiteHelper.ROUTE_DESTINATION))));
            } catch (JSONException e) {}
        }
        cursor.close();
        return destinationPoint;
    }

    public ArrayList<PointWrapper> getViaPointListOfRoute(int id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_ROUTE, SQLiteHelper.TABLE_ROUTE_ALL_COLUMNS,
                SQLiteHelper.ROUTE_ID + " = " + id,
                null, null, null, null);
        ArrayList<PointWrapper> viaPointList = new ArrayList<PointWrapper>();
        if (cursor.moveToFirst()) {
            JSONArray jsonViaPointList = null;
            try {
                jsonViaPointList = new JSONArray(
                        cursor.getString(
                            cursor.getColumnIndex(SQLiteHelper.ROUTE_VIA_POINT_LIST)));
            } catch (JSONException e) {
                jsonViaPointList = null;
            } finally {
                if (jsonViaPointList != null) {
                    for (int i=0; i<jsonViaPointList.length(); i++) {
                        try {
                            viaPointList.add(
                                    new PointWrapper(
                                        this.context, jsonViaPointList.getJSONObject(i)));
                        } catch (JSONException e) {}
                    }
                }
            }
        }
        cursor.close();
        return viaPointList;
    }

    public RouteObject getCurrentObjectDataOfRoute(int id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_ROUTE, SQLiteHelper.TABLE_ROUTE_ALL_COLUMNS,
                SQLiteHelper.ROUTE_ID + " = " + id,
                null, null, null, null);
        RouteObject routeObject = null;
        if (cursor.moveToFirst()) {
            try {
                routeObject = new RouteObject(
                        this.context,
                        cursor.getInt(
                            cursor.getColumnIndex(SQLiteHelper.ROUTE_CURRENT_OBJECT_INDEX)),
                        new JSONObject(
                            cursor.getString(
                                cursor.getColumnIndex(SQLiteHelper.ROUTE_CURRENT_OBJECT_DATA))));
            } catch (JSONException e) {}
        }
        cursor.close();
        return routeObject;
    }

    public int addRoute(PointWrapper startPoint, PointWrapper destinationPoint,
            ArrayList<PointWrapper> viaPointList, String description,
            ArrayList<RouteObject> routeObjectList) throws JSONException {
        // prepare table row to insert
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.ROUTE_START, startPoint.toJson().toString());
        values.put(SQLiteHelper.ROUTE_DESTINATION, destinationPoint.toJson().toString());
        JSONArray jsonViaPointList = new JSONArray();
        for (PointWrapper viaPoint : viaPointList) {
            jsonViaPointList.put(viaPoint.toJson());
        }
        values.put(SQLiteHelper.ROUTE_VIA_POINT_LIST, jsonViaPointList.toString());
        values.put(SQLiteHelper.ROUTE_DESCRIPTION, description);
        values.put(SQLiteHelper.ROUTE_CREATED, System.currentTimeMillis());
        values.put(SQLiteHelper.ROUTE_CURRENT_OBJECT_INDEX, 0);
        values.put(SQLiteHelper.ROUTE_CURRENT_OBJECT_DATA, routeObjectList.get(0).toJson().toString());
        JSONArray jsonRouteObjectList = new JSONArray();
        for (RouteObject routeObject : routeObjectList) {
            jsonRouteObjectList.put(routeObject.toJson());
        }
        values.put(SQLiteHelper.ROUTE_OBJECT_LIST, jsonRouteObjectList.toString());
        // try to insert
        int id = (int) database.insertWithOnConflict(
                SQLiteHelper.TABLE_ROUTE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        System.out.println("xxx insert route: " + id);
        // if insertion fails, update the existing row
        if (id == -1) {
            Cursor cursor = database.query(
                    SQLiteHelper.TABLE_ROUTE, SQLiteHelper.TABLE_ROUTE_ALL_COLUMNS,
                    String.format(
                        "%1$s = %2$s AND %3$s = %4$s AND %5$s = %6$s",
                        SQLiteHelper.ROUTE_START,
                        DatabaseUtils.sqlEscapeString(values.getAsString(SQLiteHelper.ROUTE_START)),
                        SQLiteHelper.ROUTE_DESTINATION,
                        DatabaseUtils.sqlEscapeString(values.getAsString(SQLiteHelper.ROUTE_DESTINATION)),
                        SQLiteHelper.ROUTE_DESCRIPTION,
                        DatabaseUtils.sqlEscapeString(values.getAsString(SQLiteHelper.ROUTE_DESCRIPTION))),
                    null, null, null, null);
            if (cursor.moveToFirst()) {
                id = cursor.getInt(cursor.getColumnIndex(SQLiteHelper.ROUTE_ID));
                database.update(
                        SQLiteHelper.TABLE_ROUTE,
                        values,
                        String.format("%1$s = ?", SQLiteHelper.ROUTE_ID),
                        new String[]{String.valueOf(id)});
                System.out.println("xxx updated route: " + id);
            }
            cursor.close();
        }
        return id;
    }


    /**
     * excluded ways
     */

	public ArrayList<SegmentWrapper> getExcludedWaysList() {
		Cursor cursor = database.query(
                SQLiteHelper.TABLE_EXCLUDED_WAYS, SQLiteHelper.TABLE_EXCLUDED_WAYS_ALL_COLUMNS,
                null, null, null, null, SQLiteHelper.EXCLUDED_WAYS_TIMESTAMP + " DESC");
		ArrayList<SegmentWrapper> excludedWaysList = new ArrayList<SegmentWrapper>(cursor.getCount());
        while (cursor.moveToNext()) {
            SegmentWrapper segmentWrapper = null;
            try {
                segmentWrapper = new SegmentWrapper(
                        this.context,
                        new JSONObject(
                            cursor.getString(
                                cursor.getColumnIndex(SQLiteHelper.EXCLUDED_WAYS_DATA))));
            } catch (JSONException e) {
                segmentWrapper = null;
            } finally {
                if (segmentWrapper != null) {
        			excludedWaysList.add(segmentWrapper);
                }
            }
		}
		cursor.close();
		return excludedWaysList;
	}

    public void addExcludedWaySegment(SegmentWrapper segmentToAdd) {
        if (segmentToAdd == null
                || ! (segmentToAdd.getSegment() instanceof Footway)
                || ((Footway) segmentToAdd.getSegment()).getWayId() == -1) {
            return;
        }
        // add segment to excluded ways table
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.EXCLUDED_WAYS_ID, segmentToAdd.hashCode());
        values.put(SQLiteHelper.EXCLUDED_WAYS_TIMESTAMP, System.currentTimeMillis());
        try {
            values.put(SQLiteHelper.EXCLUDED_WAYS_DATA, segmentToAdd.toJson().toString());
        } catch (JSONException e) {
            return;
        }
        database.insertWithOnConflict(
                SQLiteHelper.TABLE_EXCLUDED_WAYS,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void removeExcludedWaySegment(SegmentWrapper segmentToRemove) {
        if (segmentToRemove == null) {
            return;
        }
        // delete segment from excluded ways table
        database.delete(
                SQLiteHelper.TABLE_EXCLUDED_WAYS,
                String.format(
                    "%1$s = ?", SQLiteHelper.EXCLUDED_WAYS_ID),
                new String[] {
                    String.valueOf(segmentToRemove.hashCode())});
    }

}
