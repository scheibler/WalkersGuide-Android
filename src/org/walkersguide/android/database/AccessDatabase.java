package org.walkersguide.android.database;

import java.util.ArrayList;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.PointWrapper;
import org.walkersguide.android.data.poi.FavoritesProfile;
import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.data.poi.POIProfile;
import org.walkersguide.android.data.poi.PointProfileObject;
import org.walkersguide.android.data.server.Map;
import org.walkersguide.android.data.server.PublicTransportProvider;
import org.walkersguide.android.util.Constants;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

public class AccessDatabase {

    private Context context;
    private static AccessDatabase accessDatabaseInstance;
    private SQLiteDatabase database;

    public static AccessDatabase getInstance(Context context) {
        if(accessDatabaseInstance == null){
            accessDatabaseInstance = new AccessDatabase(context.getApplicationContext());
        }
        return accessDatabaseInstance;
    }

    private AccessDatabase(Context context) {
        this.context = context;
        SQLiteHelper dbHelper = new SQLiteHelper(context);
        this.database = dbHelper.getWritableDatabase();

        // insert some default favorites profiles if table is empty
        //database.delete(SQLiteHelper.TABLE_POINT, null, null);
        //database.delete(SQLiteHelper.TABLE_FP_POINTS, null, null);
        //database.delete(SQLiteHelper.TABLE_FAVORITES_PROFILE, null, null);
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
                        String.valueOf(FavoritesProfile.ID_ADDRESSES),
                        context.getResources().getString(R.string.fpNameAddresses),
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
            // user defined entrances
            database.execSQL(
                    sqlInsertFavoritesProfileQuery,
                    new String[] {
                        String.valueOf(FavoritesProfile.ID_FIRST_USER_CREATED_PROFILE),
                        context.getResources().getString(R.string.fpNameMyEntrances),
                        String.valueOf(Constants.SORT_CRITERIA.ORDER_DESC),
                        Constants.DUMMY.LOCATION,
                        String.valueOf(Constants.DUMMY.DIRECTION)});
        }
    }


    /**
     * maps
     */

	public ArrayList<Map> getMapList() {
		Cursor cursor = database.query(
                SQLiteHelper.TABLE_MAP, SQLiteHelper.TABLE_MAP_ALL_COLUMNS,
                null, null, null, null, SQLiteHelper.MAP_NAME + " ASC");
		ArrayList<Map> mapList = new ArrayList<Map>(cursor.getCount());
        while (cursor.moveToNext()) {
			mapList.add(
                    cursorToMap(cursor));
		}
		cursor.close();
		return mapList;
	}

    public Map getMap(String name) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_MAP, SQLiteHelper.TABLE_MAP_ALL_COLUMNS,
                SQLiteHelper.MAP_NAME + " = " + DatabaseUtils.sqlEscapeString(name),
                null, null, null, null);
        Map map = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            map = cursorToMap(cursor);
        }
        cursor.close();
        return map;
    }

    public int addMap(String name, String url) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.MAP_NAME, name);
        values.put(SQLiteHelper.MAP_URL, url);
        return (int) database.insertWithOnConflict(
                SQLiteHelper.TABLE_MAP,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void removeMap(String name) {
        database.delete(
                SQLiteHelper.TABLE_MAP,
                SQLiteHelper.MAP_NAME + " = ?",
                new String[] {name});
    }

    private Map cursorToMap(Cursor cursor) {
        return new Map(
                cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.MAP_NAME)),
                cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.MAP_URL)));
    }


    /**
     * public transport provider
     */

	public ArrayList<PublicTransportProvider> getPublicTransportProviderList() {
		Cursor cursor = database.query(
                SQLiteHelper.TABLE_PUBLIC_TRANSPORT_PROVIDER, SQLiteHelper.TABLE_PUBLIC_TRANSPORT_PROVIDER_ALL_COLUMNS,
                null, null, null, null, SQLiteHelper.PUBLIC_TRANSPORT_PROVIDER_NAME + " ASC");
		ArrayList<PublicTransportProvider> publicTransportProviderList = new ArrayList<PublicTransportProvider>(cursor.getCount());
        while (cursor.moveToNext()) {
			publicTransportProviderList.add(
                    cursorToPublicTransportProvider(cursor));
		}
		cursor.close();
		return publicTransportProviderList;
	}

    public PublicTransportProvider getPublicTransportProvider(String identifier) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_PUBLIC_TRANSPORT_PROVIDER, SQLiteHelper.TABLE_PUBLIC_TRANSPORT_PROVIDER_ALL_COLUMNS,
                SQLiteHelper.PUBLIC_TRANSPORT_PROVIDER_IDENTIFIER+ " = " + DatabaseUtils.sqlEscapeString(identifier),
                null, null, null, null);
        PublicTransportProvider publicTransportProvider = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            publicTransportProvider = cursorToPublicTransportProvider(cursor);
        }
        cursor.close();
        return publicTransportProvider;
    }

    public int addPublicTransportProvider(String identifier, String name) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.PUBLIC_TRANSPORT_PROVIDER_IDENTIFIER, identifier);
        values.put(SQLiteHelper.PUBLIC_TRANSPORT_PROVIDER_NAME, name);
        return (int) database.insertWithOnConflict(
                SQLiteHelper.TABLE_PUBLIC_TRANSPORT_PROVIDER,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void removePublicTransportProvider(String identifier) {
        database.delete(
                SQLiteHelper.TABLE_PUBLIC_TRANSPORT_PROVIDER,
                SQLiteHelper.PUBLIC_TRANSPORT_PROVIDER_IDENTIFIER + " = ?",
                new String[] {identifier});
    }

    private PublicTransportProvider cursorToPublicTransportProvider(Cursor cursor) {
        return new PublicTransportProvider(
                cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.PUBLIC_TRANSPORT_PROVIDER_IDENTIFIER)),
                cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.PUBLIC_TRANSPORT_PROVIDER_NAME)));
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
        System.out.println("xxx point size: " + cursor.getCount());
        while (cursor.moveToNext()) {
            JSONObject jsonPoint = null;
            try {
                jsonPoint = new JSONObject(
                        cursor.getString(cursor.getColumnIndex(SQLiteHelper.POINT_DATA)));
                jsonPoint.put("order", cursor.getInt(cursor.getColumnIndex(SQLiteHelper.FP_POINTS_ORDER)));
            } catch (JSONException e) {
                jsonPoint = null;
            } finally {
                if (jsonPoint != null) {
                    jsonPointList.put(jsonPoint);
                }
            }
        }
        cursor.close();

        System.out.println("xxx curr: " + favoritesProfileJSONCenterSerialized);
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
            System.out.println("xxx error: " + e.getMessage().replace("\n", "--"));
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
    }

    public void removeFavoritesProfile(int id) {
        // delete profile row from favorites_profile table
        database.delete(
                SQLiteHelper.TABLE_FAVORITES_PROFILE,
                String.format("%1$s = ?", SQLiteHelper.FAVORITES_PROFILE_ID),
                new String[] {String.valueOf(id)});
        // delete orphan points
        deleteOrphanPointsOfFavoriteProfile(id);
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
                    SQLiteDatabase.CONFLICT_IGNORE);
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
    }

    private void deleteOrphanPointsOfFavoriteProfile(int id) {
        database.delete(
                SQLiteHelper.TABLE_POINT,
                String.format(
                    "NOT EXISTS (SELECT NULL FROM %1$s WHERE %2$s.%3$s = %4$s.%5$s AND %6$s.%7$s = ?)",
                    SQLiteHelper.TABLE_FP_POINTS,
                    SQLiteHelper.TABLE_FP_POINTS, SQLiteHelper.FP_POINTS_POINT_ID,
                    SQLiteHelper.TABLE_POINT, SQLiteHelper.POINT_ID,
                    SQLiteHelper.TABLE_FP_POINTS, SQLiteHelper.FP_POINTS_PROFILE_ID),
                new String[] {String.valueOf(id)});
    }


    /**
     * POIProfile
     */

    public TreeMap<Integer,String> getPOIProfileMap() {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_POI_PROFILE, SQLiteHelper.TABLE_POI_PROFILE_ALL_COLUMNS,
                null, null, null, null, SQLiteHelper.POI_PROFILE_ID + " ASC");
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
                POICategory category = null;
                try {
                    category = getPOICategory(
                            jsonPOICategoryIdList.getInt(i));
                } catch (JSONException     e) {
                    category = null;
                } finally {
                    if (category != null) {
                        poiCategoryList.add(category);
                    }
                }
            }
        }
        cursor.close();
        return poiCategoryList;
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
        JSONArray jsonPOICategoryIdList = new JSONArray();
        for (POICategory category : poiCategoryList) {
            jsonPOICategoryIdList.put(category.getId());
        }
        values.put(SQLiteHelper.POI_PROFILE_CATEGORY_ID_LIST, jsonPOICategoryIdList.toString());
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
     * POICategory
     */

	public ArrayList<POICategory> getPOICategoryList() {
		Cursor cursor = database.query(
                SQLiteHelper.TABLE_POI_CATEGORY, SQLiteHelper.TABLE_POI_CATEGORY_ALL_COLUMNS,
                null, null, null, null, SQLiteHelper.POI_CATEGORY_ID + " ASC");
		ArrayList<POICategory> poiCategoryList = new ArrayList<POICategory>(cursor.getCount());
        while (cursor.moveToNext()) {
			poiCategoryList.add(
                    cursorToPOICategory(cursor));
		}
		cursor.close();
		return poiCategoryList;
	}

    public POICategory getPOICategory(int id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_POI_CATEGORY, SQLiteHelper.TABLE_POI_CATEGORY_ALL_COLUMNS,
                SQLiteHelper.POI_CATEGORY_ID + " = " + id,
                null, null, null, null);
        POICategory category = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            category = cursorToPOICategory(cursor);
        }
        cursor.close();
        return category;
    }

    public int addPOICategory(String tag) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.POI_CATEGORY_TAG, tag);
        return (int) database.insertWithOnConflict(
                SQLiteHelper.TABLE_POI_CATEGORY,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void removePOICategory(String tag) {
        database.delete(
                SQLiteHelper.TABLE_POI_CATEGORY,
                SQLiteHelper.POI_CATEGORY_TAG + " = ?",
                new String[] {tag});
    }

    private POICategory cursorToPOICategory(Cursor cursor) {
        return new POICategory(
                this.context,
                cursor.getInt(
                    cursor.getColumnIndex(SQLiteHelper.POI_CATEGORY_ID)),
                cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.POI_CATEGORY_TAG)));
    }

}
