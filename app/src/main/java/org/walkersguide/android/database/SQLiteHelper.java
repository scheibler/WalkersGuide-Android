package org.walkersguide.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.walkersguide.android.BuildConfig;


public class SQLiteHelper extends SQLiteOpenHelper {

    public static final String INTERNAL_DATABASE_NAME = BuildConfig.DATABASE_NAME;;
    public static final int DATABASE_VERSION = BuildConfig.DATABASE_VERSION;

    // map table
    public static final String TABLE_MAP = "map";
    public static final String MAP_NAME = "name";
    public static final String MAP_URL = "url";
    public static final String MAP_VERSION = "version";
    public static final String MAP_CREATED = "created";
    public static final String[] TABLE_MAP_ALL_COLUMNS = {
        MAP_NAME, MAP_URL, MAP_VERSION, MAP_CREATED
    };
    public static final String CREATE_MAP_TABLE = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_MAP + "("
        + MAP_NAME + " text not null primary key, "
        + MAP_URL + " text not null, "
        + MAP_VERSION + " integer, "
        + MAP_CREATED + " integer);";
    public static final String DROP_MAP_TABLE =
        "DROP TABLE IF EXISTS " + TABLE_MAP + ";";

    // public transport provider table
    public static final String TABLE_PUBLIC_TRANSPORT_PROVIDER = "public_transport_provider";
    public static final String PUBLIC_TRANSPORT_PROVIDER_IDENTIFIER = "identifier";
    public static final String PUBLIC_TRANSPORT_PROVIDER_NAME = "name";
    public static final String[] TABLE_PUBLIC_TRANSPORT_PROVIDER_ALL_COLUMNS = {
        PUBLIC_TRANSPORT_PROVIDER_IDENTIFIER, PUBLIC_TRANSPORT_PROVIDER_NAME
    };
    public static final String CREATE_PUBLIC_TRANSPORT_PROVIDER_TABLE = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_PUBLIC_TRANSPORT_PROVIDER + "("
        + PUBLIC_TRANSPORT_PROVIDER_IDENTIFIER + " text not null primary key, "
        + PUBLIC_TRANSPORT_PROVIDER_NAME + " text not null);";
    public static final String DROP_PUBLIC_TRANSPORT_PROVIDER_TABLE =
        "DROP TABLE IF EXISTS " + TABLE_PUBLIC_TRANSPORT_PROVIDER + ";";

    // address table
    public static final String TABLE_ADDRESS = "address";
    public static final String ADDRESS_LATITUDE = "latitude";
    public static final String ADDRESS_LONGITUDE = "longitude";
    public static final String ADDRESS_POINT = "point";
    public static final String[] TABLE_ADDRESS_ALL_COLUMNS = {
        ADDRESS_LATITUDE, ADDRESS_LONGITUDE, ADDRESS_POINT
    };
    public static final String CREATE_ADDRESS_TABLE = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_ADDRESS + "("
        + ADDRESS_LATITUDE + " integer, "
        + ADDRESS_LONGITUDE + " integer, "
        + ADDRESS_POINT + " text not null, "
        + "UNIQUE (" + ADDRESS_LATITUDE + ", " + ADDRESS_LONGITUDE + ") ON CONFLICT IGNORE);";
    public static final String DROP_ADDRESS_TABLE =
        "DROP TABLE IF EXISTS " + TABLE_ADDRESS + ";";

    // favorites profile table
    public static final String TABLE_FAVORITES_PROFILE = "favorites_profile";
    public static final String FAVORITES_PROFILE_ID = "_id";
    public static final String FAVORITES_PROFILE_NAME = "name";
    public static final String FAVORITES_PROFILE_SORT_CRITERIA = "sort_criteria";
    public static final String FAVORITES_PROFILE_CENTER = "center";
    public static final String FAVORITES_PROFILE_DIRECTION = "direction";
    public static final String[] TABLE_FAVORITES_PROFILE_ALL_COLUMNS = {
        FAVORITES_PROFILE_ID, FAVORITES_PROFILE_NAME, FAVORITES_PROFILE_SORT_CRITERIA,
        FAVORITES_PROFILE_CENTER, FAVORITES_PROFILE_DIRECTION
    };
    public static final String CREATE_FAVORITES_PROFILE_TABLE = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITES_PROFILE + "("
        + FAVORITES_PROFILE_ID + " integer primary key autoincrement, "
        + FAVORITES_PROFILE_NAME + " text unique not null, "
        + FAVORITES_PROFILE_SORT_CRITERIA + " integer, "
        + FAVORITES_PROFILE_CENTER + " text, "
        + FAVORITES_PROFILE_DIRECTION + " integer, "
        + "UNIQUE(" + FAVORITES_PROFILE_NAME + ") ON CONFLICT IGNORE);";
    public static final String DROP_FAVORITES_PROFILE_TABLE =
        "DROP TABLE IF EXISTS " + TABLE_FAVORITES_PROFILE + ";";

    // point table
    public static final String TABLE_POINT = "point";
    public static final String POINT_ID = "_id";
    public static final String POINT_DATA = "data";
    public static final String[] TABLE_POINT_ALL_COLUMNS = {
        POINT_ID, POINT_DATA
    };
    public static final String CREATE_POINT_TABLE = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_POINT + "("
        + POINT_ID + " integer primary key, "
        + POINT_DATA + " text not null);";
    public static final String DROP_POINT_TABLE =
        "DROP TABLE IF EXISTS " + TABLE_POINT + ";";

    // favorites profile point table
    public static final String TABLE_FP_POINTS = "fp_points";
    public static final String FP_POINTS_PROFILE_ID = "profile_id";
    public static final String FP_POINTS_POINT_ID = "point_id";
    public static final String FP_POINTS_ORDER = "ordering";
    public static final String[] TABLE_FP_POINTS_ALL_COLUMNS = {
        FP_POINTS_PROFILE_ID, FP_POINTS_POINT_ID, FP_POINTS_ORDER
    };
    public static final String CREATE_FP_POINTS_TABLE = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_FP_POINTS + "("
        + FP_POINTS_PROFILE_ID + " integer, "
        + FP_POINTS_POINT_ID + " integer, "
        + FP_POINTS_ORDER + " integer, "
        + "PRIMARY KEY (" + FP_POINTS_PROFILE_ID + ", " + FP_POINTS_POINT_ID + "));";
    public static final String DROP_FP_POINTS_TABLE =
        "DROP TABLE IF EXISTS " + TABLE_FP_POINTS + ";";

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

    // route table
    public static final String TABLE_ROUTE = "route";
    public static final String ROUTE_ID = "_id";
    public static final String ROUTE_START = "start";
    public static final String ROUTE_DESTINATION = "destination";
    public static final String ROUTE_VIA_POINT_LIST = "via_point_list";
    public static final String ROUTE_DESCRIPTION = "description";
    public static final String ROUTE_CREATED = "created";
    public static final String ROUTE_CURRENT_OBJECT_INDEX = "current_object_index";
    public static final String ROUTE_CURRENT_OBJECT_DATA = "current_object_data";
    public static final String ROUTE_OBJECT_LIST = "object_list";
    public static final String[] TABLE_ROUTE_ALL_COLUMNS = {
        ROUTE_ID, ROUTE_START, ROUTE_DESTINATION, ROUTE_VIA_POINT_LIST, ROUTE_DESCRIPTION, ROUTE_CREATED,
        ROUTE_CURRENT_OBJECT_INDEX, ROUTE_CURRENT_OBJECT_DATA, ROUTE_OBJECT_LIST
    };
    public static final String CREATE_ROUTE_TABLE = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_ROUTE + "("
        + ROUTE_ID + " integer primary key autoincrement, "
        + ROUTE_START + " text not null, "
        + ROUTE_DESTINATION + " text not null, "
        + ROUTE_VIA_POINT_LIST + " text not null, "
        + ROUTE_DESCRIPTION + " text not null, "
        + ROUTE_CREATED + " integer, "
        + ROUTE_CURRENT_OBJECT_INDEX + " integer, "
        + ROUTE_CURRENT_OBJECT_DATA + " text not null, "
        + ROUTE_OBJECT_LIST + " text not null, "
        + "UNIQUE (" + ROUTE_START + ", " + ROUTE_DESTINATION + ", " + ROUTE_DESCRIPTION + ") ON CONFLICT IGNORE);";
    public static final String DROP_ROUTE_TABLE =
        "DROP TABLE IF EXISTS " + TABLE_ROUTE + ";";

    // excluded way segment table
    public static final String TABLE_EXCLUDED_WAYS = "excluded_ways";
    public static final String EXCLUDED_WAYS_ID = "_id";
    public static final String EXCLUDED_WAYS_TIMESTAMP = "timestamp";
    public static final String EXCLUDED_WAYS_DATA = "data";
    public static final String[] TABLE_EXCLUDED_WAYS_ALL_COLUMNS = {
        EXCLUDED_WAYS_ID, EXCLUDED_WAYS_TIMESTAMP, EXCLUDED_WAYS_DATA
    };
    public static final String CREATE_EXCLUDED_WAYS_TABLE = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_EXCLUDED_WAYS + "("
        + EXCLUDED_WAYS_ID + " integer primary key, "
        + EXCLUDED_WAYS_TIMESTAMP + " integer, "
        + EXCLUDED_WAYS_DATA + " text not null);";
    public static final String DROP_EXCLUDED_WAYS_TABLE =
        "DROP TABLE IF EXISTS " + TABLE_EXCLUDED_WAYS + ";";

    public SQLiteHelper(Context context) {
        super(context, INTERNAL_DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase database) {
        // create tables
        database.execSQL(CREATE_MAP_TABLE);
        database.execSQL(CREATE_PUBLIC_TRANSPORT_PROVIDER_TABLE);
        database.execSQL(CREATE_ADDRESS_TABLE);
        database.execSQL(CREATE_FAVORITES_PROFILE_TABLE);
        database.execSQL(CREATE_POINT_TABLE);
        database.execSQL(CREATE_FP_POINTS_TABLE);
        database.execSQL(CREATE_POI_PROFILE_TABLE);
        database.execSQL(CREATE_POI_CATEGORY_TABLE);
        database.execSQL(CREATE_ROUTE_TABLE);
        database.execSQL(CREATE_EXCLUDED_WAYS_TABLE);
    }

    @Override public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        System.out.println("xxx onUpdate: " + oldVersion + " / " + newVersion);
        if (oldVersion == 2) {
            // create excluded ways table
            database.execSQL(CREATE_EXCLUDED_WAYS_TABLE);
            // recreate map table
            database.execSQL(DROP_MAP_TABLE);
            database.execSQL(CREATE_MAP_TABLE);
            // add via point column to route table
            database.execSQL(
                    String.format(
                        "ALTER TABLE %1$s ADD COLUMN %2$s TEXT NOT NULL DEFAULT '[]';",
                        TABLE_ROUTE, ROUTE_VIA_POINT_LIST)
                    );
        }
    }

}
