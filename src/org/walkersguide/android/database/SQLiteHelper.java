package org.walkersguide.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper {

    public static final String INTERNAL_DATABASE_NAME = "walkersguide.db";
    public static final int DATABASE_VERSION = 1;

    // poi profile table
    public static final String TABLE_POI_PROFILE = "poi_profile";
    public static final String POI_PROFILE_ID = "_id";
    public static final String POI_PROFILE_NAME = "name";
    public static final String POI_PROFILE_RADIUS = "radius";
    public static final String POI_PROFILE_NUMBER_OF_RESULTS = "number_of_results";
    public static final String POI_PROFILE_CATEGORY_ID_LIST = "category_id_list";
    public static final String POI_PROFILE_CENTER = "center";
    public static final String POI_PROFILE_DIRECTION = "direction";
    public static final String POI_PROFILE_POINT_LIST = "point_list";
    public static final String[] TABLE_POI_PROFILE_ALL_COLUMNS = {
        POI_PROFILE_ID, POI_PROFILE_NAME, POI_PROFILE_RADIUS,
        POI_PROFILE_NUMBER_OF_RESULTS, POI_PROFILE_CATEGORY_ID_LIST, POI_PROFILE_CENTER,
        POI_PROFILE_DIRECTION, POI_PROFILE_POINT_LIST
    };
    public static final String CREATE_POI_PROFILE_TABLE = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_POI_PROFILE + "("
        + POI_PROFILE_ID + " integer primary key autoincrement, "
        + POI_PROFILE_NAME + " text unique not null, "
        + POI_PROFILE_RADIUS + " integer, "
        + POI_PROFILE_NUMBER_OF_RESULTS + " integer, "
        + POI_PROFILE_CATEGORY_ID_LIST + " text not null, "
        + POI_PROFILE_CENTER + " text, "
        + POI_PROFILE_DIRECTION + " integer, "
        + POI_PROFILE_POINT_LIST + " text);";
    public static final String DROP_POI_PROFILE_TABLE =
        "DROP TABLE IF EXISTS " + TABLE_POI_PROFILE + ";";

    // poi category table
    public static final String TABLE_POI_CATEGORY = "poi_category";
    public static final String POI_CATEGORY_ID = "_id";
    public static final String POI_CATEGORY_TAG = "tag";
    public static final String[] TABLE_POI_CATEGORY_ALL_COLUMNS = {
        POI_CATEGORY_ID, POI_CATEGORY_TAG
    };
    public static final String CREATE_POI_CATEGORY_TABLE = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_POI_CATEGORY + "("
        + POI_CATEGORY_ID + " integer primary key autoincrement, "
        + POI_CATEGORY_TAG + " text unique not null);";
    public static final String DROP_POI_CATEGORY_TABLE =
        "DROP TABLE IF EXISTS " + TABLE_POI_CATEGORY + ";";

    public SQLiteHelper(Context context) {
        super(context, INTERNAL_DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase database) {
        // create tables
        database.execSQL(CREATE_POI_PROFILE_TABLE);
        database.execSQL(CREATE_POI_CATEGORY_TABLE);
    }

    @Override public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        System.out.println("xxx onUpdate: " + oldVersion + " / " + newVersion);
    }

}
