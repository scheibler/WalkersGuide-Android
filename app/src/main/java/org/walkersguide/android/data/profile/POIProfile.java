package org.walkersguide.android.data.profile;

import android.content.Context;

import android.text.TextUtils;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.util.Constants;


public class POIProfile extends PointProfile {

    // radius and number of results
    // initial values
    public static final int INITIAL_RADIUS = 1000;
    public static final int INITIAL_SEARCH_RADIUS = 5000;
    public static final int INITIAL_LOCAL_FAVORITES_RADIUS = 10000;
    public static final int INITIAL_NUMBER_OF_RESULTS = 100;
    // max values
    public static final int MAXIMAL_RADIUS = 20000;
    public static final int MAXIMAL_SEARCH_RADIUS = 100000;
    public static final int MAXIMAL_LOCAL_FAVORITES_RADIUS = 1000000;
    public static final int MAXIMAL_NUMBER_OF_RESULTS = 1000;

    private ArrayList<Integer> favoriteIdList;
    private ArrayList<POICategory> poiCategoryList;
    private int radius, numberOfResults;
    private String searchTerm;

    public POIProfile(Context context, int id, String name, int radius, int numberOfResults,
            JSONArray jsonFavoriteIdList, JSONArray jsonPOICategoryIdList,
            String searchTerm, JSONObject jsonCenter, JSONObject jsonDirection, JSONArray jsonPointList) throws JSONException {
        super(context, id, name, jsonCenter, jsonDirection, jsonPointList);
        this.radius = radius;
        this.numberOfResults = numberOfResults;
        this.searchTerm = searchTerm;
        // favorite profile ids
        this.favoriteIdList = new ArrayList<Integer>();
        for (int i=0; i<jsonFavoriteIdList.length(); i++) {
            try {
                this.favoriteIdList.add(jsonFavoriteIdList.getInt(i));
            } catch (JSONException e) {}
        }
        // poi categories
        this.poiCategoryList = new ArrayList<POICategory>();
        for (int i=0; i<jsonPOICategoryIdList.length(); i++) {
            try {
                this.poiCategoryList.add(
                        new POICategory(
                            context, jsonPOICategoryIdList.getString(i)));
            } catch (JSONException e) {}
        }
    }

    public int getSortCriteria() {
        return Constants.SORT_CRITERIA.DISTANCE_ASC;
    }

    public ArrayList<Integer> getFavoriteIdList() {
        return this.favoriteIdList;
    }

    public ArrayList<POICategory> getPOICategoryList() {
        return this.poiCategoryList;
    }

    public int getRadius() {
        return this.radius;
    }

    public int getLookupRadius() {
        ArrayList<PointProfileObject> pointProfileObjectList = super.getPointProfileObjectList();
        if (pointProfileObjectList != null) {
            if (pointProfileObjectList.size() == this.numberOfResults) {
                return pointProfileObjectList.get(pointProfileObjectList.size()-1).distanceFromCenter();
            }
            return this.radius;
        }
        return 0;
    }

    public int getInitialRadius() {
        if (this.poiCategoryList.isEmpty()) {
            return POIProfile.INITIAL_LOCAL_FAVORITES_RADIUS;
        } else if (! TextUtils.isEmpty(this.searchTerm)) {
            return POIProfile.INITIAL_SEARCH_RADIUS;
        }
        return POIProfile.INITIAL_RADIUS;
    }

    public int getMaximalRadius() {
        if (this.poiCategoryList.isEmpty()) {
            return POIProfile.MAXIMAL_LOCAL_FAVORITES_RADIUS;
        } else if (! TextUtils.isEmpty(this.searchTerm)) {
            return POIProfile.MAXIMAL_SEARCH_RADIUS;
        }
        return POIProfile.MAXIMAL_RADIUS;
    }

    public int getNumberOfResults() {
        return this.numberOfResults;
    }

    public int getLookupNumberOfResults() {
        ArrayList<PointProfileObject> pointProfileObjectList = super.getPointProfileObjectList();
        if (pointProfileObjectList != null) {
            return pointProfileObjectList.size();
        }
        return 0;
    }

    public int getInitialNumberOfResults() {
        return POIProfile.INITIAL_NUMBER_OF_RESULTS;
    }

    public int getMaximalNumberOfResults() {
        return POIProfile.MAXIMAL_NUMBER_OF_RESULTS;
    }

    public String getSearchTerm() {
        return this.searchTerm;
    }

    public void setRadiusNumberOfResultsCenterDirectionAndPointListAndUpdateInDatabase(
            int newRadius, int newNumberOfResults, PointWrapper newCenter, Direction newDirection,
            ArrayList<PointProfileObject> newPointProfileObjectList) {
        this.radius = newRadius;
        this.numberOfResults = newNumberOfResults;
        super.setCenterDirectionAndPointList(
                newCenter, newDirection, newPointProfileObjectList);
        // update database
        AccessDatabase.getInstance(super.getContext())
            .updateRadiusNumberOfResultsCenterDirectionAndPointListOfPOIProfile(
                    super.getId(), this.getRadius(), this.getNumberOfResults(),
                    super.getCenter(), super.getDirection(), super.getPointProfileObjectList());
    }

}
