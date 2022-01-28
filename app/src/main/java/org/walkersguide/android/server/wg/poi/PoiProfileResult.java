package org.walkersguide.android.server.wg.poi;

import java.util.ArrayList;

import java.io.Serializable;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.database.SortMethod;


public class PoiProfileResult implements Serializable{
    private static final long serialVersionUID = 1l;

    public static int calculateLookupRadius(ArrayList<Point> pointList, Point center, int radius, int numberOfResults) {
        if (pointList.size() >= numberOfResults) {
            return pointList.get(
                    pointList.size() - 1)
                .distanceTo(center);
        }
        return radius;
    }

    public static int calculateLookupNumberOfResults(ArrayList<Point> pointList) {
        return pointList.size();
    }


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


    private int radius, numberOfResults;
    private Point center;
    private ArrayList<Point> allPoiList, onlyPoiList;
    private boolean resetListPosition;

    public PoiProfileResult(int radius, int numberOfResults, Point center,
            ArrayList<Point> allPoiList, ArrayList<Point> onlyPoiList, boolean resetListPosition) {
        this.radius = radius;
        this.numberOfResults = numberOfResults;
        this.center = center;
        this.allPoiList = allPoiList;
        this.onlyPoiList = onlyPoiList;
        this.resetListPosition = resetListPosition;
    }

    public int getRadius() {
        return this.radius;
    }

    public int getLookupRadius() {
        return PoiProfileResult.calculateLookupRadius(
                this.allPoiList, this.center, this.radius, this.numberOfResults);
    }

    public int getNumberOfResults() {
        return this.numberOfResults;
    }

    public int getLookupNumberOfResults() {
        return PoiProfileResult.calculateLookupNumberOfResults(this.allPoiList);
    }

    public Point getCenter() {
        return this.center;
    }

    public ArrayList<Point> getAllPointList() {
        return this.allPoiList;
    }

    public ArrayList<Point> getOnlyPoiList() {
        return this.onlyPoiList;
    }

    public boolean getResetListPosition() {
        return this.resetListPosition;
    }

}
