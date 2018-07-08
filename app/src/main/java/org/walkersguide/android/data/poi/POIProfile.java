package org.walkersguide.android.data.poi;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.util.Constants;

import android.content.Context;

public class POIProfile extends PointProfile {

    // radius and number of results
    // initial values
    public static final int INITIAL_RADIUS = 1000;
    public static final int INITIAL_NUMBER_OF_RESULTS = 100;
    // max values
    public static final int MAXIMAL_RADIUS = 10000;
    public static final int MAXIMAL_NUMBER_OF_RESULTS = 1000;

    private AccessDatabase accessDatabaseInstance;
    private ArrayList<POICategory> poiCategoryList;
    private int radius, numberOfResults;

    public POIProfile(Context context, int id, String name, int radius, int numberOfResults, JSONArray jsonPOICategoryIdList,
            JSONObject jsonCenter, int direction, JSONArray jsonPointList) throws JSONException {
        super(context, id, name, jsonCenter, direction, jsonPointList);
        this.accessDatabaseInstance = AccessDatabase.getInstance(context);
        this.radius = radius;
        this.numberOfResults = numberOfResults;
        // poi categories
        this.poiCategoryList = new ArrayList<POICategory>();
        for (int i=0; i<jsonPOICategoryIdList.length(); i++) {
            POICategory category = AccessDatabase.getInstance(context).getPOICategory(
                    jsonPOICategoryIdList.getInt(i));
            if (category != null) {
                this.poiCategoryList.add(category);
            }
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

    public int getNumberOfResults() {
        return this.numberOfResults;
    }

    public void setRadiusAndNumberOfResults(int newRadius, int newNumberOfResults) {
        // set radius and number of results
        this.radius = newRadius;
        this.numberOfResults = newNumberOfResults;
        accessDatabaseInstance.updateRadiusAndNumberOfResultsOfPOIProfile(
                super.getId(), this.radius, this.numberOfResults);
    }

    @Override public void setCenterAndDirection(PointWrapper newCenter, int newDirection) {
        super.setCenterAndDirection(newCenter, newDirection);
        accessDatabaseInstance.updateCenterDirectionANPointListOfPOIProfile(
                super.getId(), newCenter, newDirection, super.getPointProfileObjectList());
    }

}
