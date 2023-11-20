package org.walkersguide.android.server.wg.poi;

import java.util.ArrayList;

import java.io.Serializable;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.database.SortMethod;
import timber.log.Timber;
import org.walkersguide.android.data.ObjectWithId;


public class PoiProfileResult implements Serializable{
    private static final long serialVersionUID = 1l;

    public static int calculateLookupRadius(ArrayList<ObjectWithId> objectList, Point center, int radius, int numberOfResults) {
        if (objectList.size() >= numberOfResults) {
            return objectList.get(
                    objectList.size() - 1)
                .distanceTo(center);
        }
        return radius;
    }

    public static int calculateLookupNumberOfResults(ArrayList<ObjectWithId> objectList) {
        return objectList.size();
    }

    public static boolean hasSameFirstPoi(
            ArrayList<ObjectWithId> oldObjectList, ArrayList<ObjectWithId> newObjectList) {
        if (       oldObjectList != null && ! oldObjectList.isEmpty()
                && newObjectList != null && ! newObjectList.isEmpty()) {
            return oldObjectList.get(0).equals(newObjectList.get(0));
        }
        return false;
    }


    // radius and number of results
    // initial values
    public static final int INITIAL_RADIUS = 1000;
    public static final int INITIAL_SEARCH_RADIUS = 5000;
    public static final int INITIAL_LOCAL_COLLECTION_RADIUS = 20000;
    public static final int INITIAL_NUMBER_OF_RESULTS = 100;


    private int radius, numberOfResults;
    private Point center;
    private ArrayList<ObjectWithId> allObjectList;
    private ArrayList<Point> onlyPoiList;
    private boolean resetListPosition;

    public PoiProfileResult(int radius, int numberOfResults, Point center,
            ArrayList<ObjectWithId> allObjectList, ArrayList<Point> onlyPoiList, boolean resetListPosition) {
        this.radius = radius;
        this.numberOfResults = numberOfResults;
        this.center = center;
        this.allObjectList = allObjectList;
        this.onlyPoiList = onlyPoiList;
        this.resetListPosition = resetListPosition;
    }

    public int getRadius() {
        return this.radius;
    }

    public int getLookupRadius() {
        return PoiProfileResult.calculateLookupRadius(
                this.allObjectList, this.center, this.radius, this.numberOfResults);
    }

    public int getNumberOfResults() {
        return this.numberOfResults;
    }

    public int getLookupNumberOfResults() {
        return PoiProfileResult.calculateLookupNumberOfResults(this.allObjectList);
    }

    public Point getCenter() {
        return this.center;
    }

    public ArrayList<ObjectWithId> getAllObjectList() {
        return this.allObjectList;
    }

    public ArrayList<Point> getOnlyPoiList() {
        return this.onlyPoiList;
    }

    public boolean getResetListPosition() {
        return this.resetListPosition;
    }

}
