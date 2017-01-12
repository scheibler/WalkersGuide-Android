package org.walkersguide.android.poi;

import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.basic.point.Point;
import org.walkersguide.android.database.AccessDatabase;

import android.content.Context;

public class POIProfile {

    public static final int INITIAL_RADIUS = 1000;
    public static final int INITIAL_NUMBER_OF_RESULTS = 50;

    private Context context;
    private String name;
    private int id, radius, numberOfResults;
    private ArrayList<POICategory> poiCategoryList;
    private Point center;
    private int direction;
    private ArrayList<PointListObject> pointList;

    public POIProfile(Context context, int id, String name, int radius, int numberOfResults,
            String jsonPOICategoryIdListSerialized, String jsonCenterSerialized, int direction) {
        this.context = context;
        this.id = id;
        this.name = name;
        this.radius = radius;
        this.numberOfResults = numberOfResults;
        this.pointList = null;

        // poi categories
        this.poiCategoryList = new ArrayList<POICategory>();
        try {
            JSONArray jsonPOICategoryIdList = new JSONArray(jsonPOICategoryIdListSerialized);
            for (int i=0; i<jsonPOICategoryIdList.length(); i++) {
                POICategory category = AccessDatabase.getInstance(context).getPOICategory(
                        jsonPOICategoryIdList.getInt(i));
                if (category != null) {
                    this.poiCategoryList.add(category);
                }
            }
        } catch (JSONException e) {}

        // center point
        this.center = null;
        try {
            this.center = new Point(
                    context, new JSONObject(jsonCenterSerialized));
        } catch (JSONException e) {}
        this.direction = direction;
    }

    public Context getContext() {
        return this.context;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getRadius() {
        return this.radius;
    }

    public int getNumberOfResults() {
        return this.numberOfResults;
    }

    public ArrayList<POICategory> getPOICategoryList() {
        return this.poiCategoryList;
    }

    public void setPOICategoryList(ArrayList<POICategory> newCategoryList) {
        AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(this.context);
        // set new poi category list
        this.poiCategoryList = newCategoryList;
        accessDatabaseInstance.updatePOICategoryIdsOfPOIProfile(
                this.id, this.poiCategoryList);
        // update poi list
        this.pointList = null;
        accessDatabaseInstance.updatePointListOfPOIProfile(this.id, this.pointList);
    }

    public Point getCenter() {
        return this.center;
    }

    public int getDirection() {
        return this.direction;
    }

    public void setCenterAndDirection(Point newCenter, int newDirection) {
        AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(this.context);
        // set center and direction
        this.center = newCenter;
        this.direction = newDirection;
        accessDatabaseInstance.updateCenterAndDirectionOfPOIProfile(
                this.id, this.center, this.direction);
        // update poi list
        if (this.pointList != null) {
            Collections.sort(this.pointList);
            accessDatabaseInstance.updatePointListOfPOIProfile(this.id, this.pointList);
        }
    }

    public ArrayList<PointListObject> getPointList() {
        return this.pointList;
    }

    public void setPointList(JSONArray jsonPointList) {
        this.pointList = new ArrayList<PointListObject>();
        for (int i=0; i<jsonPointList.length(); i++) {
            try {
                this.pointList.add(
                        new PointListObject(this, jsonPointList.getJSONObject(i)));
            } catch (JSONException e) {}
        }
    }

    @Override public String toString() {
        return this.name;
    }

	@Override public int hashCode() {
        return this.id;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof POIProfile)) {
			return false;
        }
		POIProfile other = (POIProfile) obj;
        return this.id == other.getId();
    }

}
