package org.walkersguide.android.data.profile;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.util.Constants;

import android.content.Context;

public class POIProfile extends PointProfile {

    // radius and number of results
    // initial values
    public static final int INITIAL_RADIUS = 1000;
    public static final int INITIAL_SEARCH_RADIUS = 5000;
    public static final int INITIAL_NUMBER_OF_RESULTS = 100;
    // max values
    public static final int MAXIMAL_RADIUS = 20000;
    public static final int MAXIMAL_SEARCH_RADIUS = 100000;
    public static final int MAXIMAL_NUMBER_OF_RESULTS = 1000;

    private AccessDatabase accessDatabaseInstance;
    private ArrayList<POICategory> poiCategoryList;
    private int radius, numberOfResults;
    private String searchTerm;

    public POIProfile(Context context, int id, String name, int radius, int numberOfResults, JSONArray jsonPOICategoryIdList,
            String searchTerm, JSONObject jsonCenter, int direction, JSONArray jsonPointList) throws JSONException {
        super(context, id, name, jsonCenter, direction, jsonPointList);
        this.accessDatabaseInstance = AccessDatabase.getInstance(context);
        this.radius = radius;
        this.numberOfResults = numberOfResults;
        this.searchTerm = searchTerm;
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
        if (super.getId() == SearchFavoritesProfile.ID_SEARCH) {
            return POIProfile.INITIAL_SEARCH_RADIUS;
        }
        return POIProfile.INITIAL_RADIUS;
    }

    public int getMaximalRadius() {
        if (super.getId() == SearchFavoritesProfile.ID_SEARCH) {
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

    public void setRadiusAndNumberOfResults(int newRadius, int newNumberOfResults) {
        // set radius and number of results
        this.radius = newRadius;
        this.numberOfResults = newNumberOfResults;
        accessDatabaseInstance.updateRadiusAndNumberOfResultsOfPOIProfile(
                super.getId(), this.radius, this.numberOfResults);
    }

    public String getSearchTerm() {
        return this.searchTerm;
    }

    public void setSearchTerm(String newSearchTerm) {
        if (newSearchTerm != null) {
            this.searchTerm = newSearchTerm;
            accessDatabaseInstance.updateSearchTermOfPOIProfile(super.getId(), this.searchTerm);
        }
    }

    @Override public void setCenterAndDirection(PointWrapper newCenter, int newDirection) {
        super.setCenterAndDirection(newCenter, newDirection);
        accessDatabaseInstance.updateCenterDirectionANPointListOfPOIProfile(
                super.getId(), newCenter, newDirection, super.getPointProfileObjectList());
    }

}
