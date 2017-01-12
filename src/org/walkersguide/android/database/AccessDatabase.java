package org.walkersguide.android.database;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.basic.point.Point;
import org.walkersguide.android.poi.POICategory;
import org.walkersguide.android.poi.POIProfile;
import org.walkersguide.android.poi.PointListObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
    }


    /**
     * POIProfile
     */

    public ArrayList<POIProfile> getPOIProfileList() {
		Cursor cursor = database.query(
                SQLiteHelper.TABLE_POI_PROFILE, SQLiteHelper.TABLE_POI_PROFILE_ALL_COLUMNS,
                null, null, null, null, SQLiteHelper.POI_PROFILE_ID + " ASC");
		ArrayList<POIProfile> poiProfileList = new ArrayList<POIProfile>(cursor.getCount());
        while (cursor.moveToNext()) {
			poiProfileList.add(
                    cursorToPOIProfile(cursor));
		}
		cursor.close();
		return poiProfileList;
	}

    public POIProfile getPOIProfile(int id) {
        Cursor cursor = database.query(
                SQLiteHelper.TABLE_POI_PROFILE, SQLiteHelper.TABLE_POI_PROFILE_ALL_COLUMNS,
                SQLiteHelper.POI_PROFILE_ID + " = " + id,
                null, null, null, null);
        POIProfile poiProfile = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            poiProfile = cursorToPOIProfile(cursor);
            // load poi point list
            JSONArray jsonPointList = null;
            try {
                jsonPointList = new JSONArray(
                        cursor.getString(
                            cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_POINT_LIST)));
            } catch (JSONException e) {
                jsonPointList = null;
            } finally {
                if (jsonPointList != null) {
                    poiProfile.setPointList(jsonPointList);
                }
            }
        }
        cursor.close();
        return poiProfile;
    }

    public int addPOIProfile(String name) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.POI_PROFILE_NAME, name);
        values.put(SQLiteHelper.POI_PROFILE_RADIUS, POIProfile.INITIAL_RADIUS);
        values.put(SQLiteHelper.POI_PROFILE_NUMBER_OF_RESULTS, POIProfile.INITIAL_NUMBER_OF_RESULTS);
        values.put(SQLiteHelper.POI_PROFILE_CATEGORY_ID_LIST, (new JSONArray()).toString());
        values.put(SQLiteHelper.POI_PROFILE_CENTER, "");
        values.put(SQLiteHelper.POI_PROFILE_DIRECTION, 0);
        values.put(SQLiteHelper.POI_PROFILE_POINT_LIST, "");
        return (int) database.insertWithOnConflict(
                SQLiteHelper.TABLE_POI_PROFILE,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void updatePOICategoryIdsOfPOIProfile(
            int id, ArrayList<POICategory> poiCategoryList) {
        JSONArray jsonPOICategoryIdList = new JSONArray();
        for (POICategory category : poiCategoryList) {
            jsonPOICategoryIdList.put(category.getId());
        }
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.POI_PROFILE_CATEGORY_ID_LIST, jsonPOICategoryIdList.toString());
        database.update(
                SQLiteHelper.TABLE_POI_PROFILE,
                values,
                SQLiteHelper.POI_PROFILE_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void updateCenterAndDirectionOfPOIProfile(
            int id, Point newCenter, int newDirection) {
        ContentValues values = new ContentValues();
        if (newCenter == null) {
            values.put(SQLiteHelper.POI_PROFILE_CENTER, "");
        } else {
            values.put(SQLiteHelper.POI_PROFILE_CENTER, newCenter.toJson().toString());
        }
        values.put(SQLiteHelper.POI_PROFILE_DIRECTION, newDirection);
        database.update(
                SQLiteHelper.TABLE_POI_PROFILE,
                values,
                SQLiteHelper.POI_PROFILE_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void updatePointListOfPOIProfile(
            int id, ArrayList<PointListObject> newPointList) {
        ContentValues values = new ContentValues();
        if (newPointList == null) {
            values.put(SQLiteHelper.POI_PROFILE_POINT_LIST, "");
        } else {
            JSONArray jsonPointList = new JSONArray();
            for (PointListObject pointListObject : newPointList) {
                JSONObject jsonPoint = pointListObject.getPoint().toJson();
                if (jsonPoint != null) {
                    jsonPointList.put(jsonPoint);
                }
            }
            values.put(SQLiteHelper.POI_PROFILE_POINT_LIST, jsonPointList.toString());
        }
        database.update(
                SQLiteHelper.TABLE_POI_PROFILE,
                values,
                SQLiteHelper.POI_PROFILE_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void removePOIProfile(int id) {
        database.delete(
                SQLiteHelper.TABLE_POI_PROFILE,
                SQLiteHelper.POI_PROFILE_ID + " = ?",
                new String[] {String.valueOf(id)});
    }

    private POIProfile cursorToPOIProfile(Cursor cursor) {
        return new POIProfile(
                this.context,
                cursor.getInt(
                    cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_ID)),
                cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_NAME)),
                cursor.getInt(
                    cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_RADIUS)),
                cursor.getInt(
                    cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_NUMBER_OF_RESULTS)),
                cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_CATEGORY_ID_LIST)),
                cursor.getString(
                    cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_CENTER)),
                cursor.getInt(
                    cursor.getColumnIndex(SQLiteHelper.POI_PROFILE_DIRECTION)));
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
