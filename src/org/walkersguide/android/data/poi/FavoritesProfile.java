package org.walkersguide.android.data.poi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.data.basic.point.PointWrapper;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.util.Constants;

import android.content.Context;

import com.google.common.primitives.Ints;

public class FavoritesProfile extends PointProfile {

    // ids of default profiles
    public static final int ID_ALL_POINTS = 0;
    public static final int ID_ADDRESSES = 1;
    public static final int ID_SIMULATED_POINTS = 2;
    public static final int ID_FIRST_USER_CREATED_PROFILE = 100;

    private AccessDatabase accessDatabaseInstance;
    private int sortCriteria;

    public FavoritesProfile(Context context, int id, String name, int sortCriteria,
            JSONObject jsonCenter, int direction, JSONArray jsonPointList) throws JSONException {
        super(context, id, name, jsonCenter, direction, jsonPointList);
        this.accessDatabaseInstance = AccessDatabase.getInstance(context);
        // sort criteria
        this.sortCriteria = Constants.SORT_CRITERIA.ORDER_DESC;
        if (Ints.contains(Constants.FavoritesProfileSortCriteriaValueArray, sortCriteria)) {
            this.sortCriteria = sortCriteria;
        }
    }

    public int getSortCriteria() {
        return this.sortCriteria;
    }

    @Override public void setCenterAndDirection(PointWrapper newCenter, int newDirection) {
        super.setCenterAndDirection(newCenter, newDirection);
        accessDatabaseInstance.updateCenterAndDirectionOfFavoritesProfile(
                super.getId(), newCenter, newDirection);
    }

}
