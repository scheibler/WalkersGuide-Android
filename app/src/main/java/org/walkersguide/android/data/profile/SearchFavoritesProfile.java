package org.walkersguide.android.data.profile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.util.Constants;

import android.content.Context;

import com.google.common.primitives.Ints;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import java.util.ArrayList;


public class SearchFavoritesProfile extends PointProfile {

    // id of search profile
    public static final int ID_SEARCH = -1;

    private int sortCriteria;
    private String searchTerm;
    private ArrayList<Integer> favoritesProfileIdList;

    public SearchFavoritesProfile(Context context, String searchTerm,
            ArrayList<Integer> favoritesProfileIdList, int sortCriteria) throws JSONException {
        super(
                context,
                ID_SEARCH,
                context.getResources().getString(R.string.fpNameSearch),
                PositionManager.getDummyLocation(context).toJson(),
                Constants.DUMMY.DIRECTION,
                new JSONArray());
        // search term and selected favorites profile list
        this.searchTerm = searchTerm;
        this.favoritesProfileIdList = favoritesProfileIdList;
        // sort criteria
        this.sortCriteria = Constants.SORT_CRITERIA.DISTANCE_ASC;
        if (Ints.contains(Constants.SearchFavoritesProfileSortCriteriaValueArray, sortCriteria)) {
            this.sortCriteria = sortCriteria;
        }
    }

    public String getSearchTerm() {
        return this.searchTerm;
    }

    public ArrayList<Integer> getFavoritesProfileIdList() {
        return this.favoritesProfileIdList;
    }

    public int getSortCriteria() {
        return this.sortCriteria;
    }

}
