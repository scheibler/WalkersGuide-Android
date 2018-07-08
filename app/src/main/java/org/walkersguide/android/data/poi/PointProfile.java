package org.walkersguide.android.data.poi;

import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.poi.PointProfileObject.SortByDistanceFromCenterASC;
import org.walkersguide.android.data.poi.PointProfileObject.SortByDistanceFromCenterDESC;
import org.walkersguide.android.data.poi.PointProfileObject.SortByNameASC;
import org.walkersguide.android.data.poi.PointProfileObject.SortByNameDESC;
import org.walkersguide.android.data.poi.PointProfileObject.SortByOrderASC;
import org.walkersguide.android.data.poi.PointProfileObject.SortByOrderDESC;
import org.walkersguide.android.util.Constants;

import android.content.Context;

public abstract class PointProfile {

    private Context context;
    private int id;
    private String name;
    private PointWrapper center;
    private int direction;
    private ArrayList<PointProfileObject> pointProfileObjectList;

    public PointProfile(Context context, int id, String name, JSONObject jsonCenter,
            int direction, JSONArray jsonPointList) throws JSONException {
        this.context = context;
        this.id = id;
        this.name = name;

        // center and direction
        this.center = new PointWrapper(context, jsonCenter);
        this.direction = direction;

        // point list
        this.pointProfileObjectList = new ArrayList<PointProfileObject>();
        for (int i=0; i<jsonPointList.length(); i++) {
            try {
                this.pointProfileObjectList.add(
                        new PointProfileObject(
                            context, this, jsonPointList.getJSONObject(i))
                        );
            } catch (JSONException e) {}
        }
    }

    public abstract int getSortCriteria();

    public Context getContext() {
        return this.context;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public PointWrapper getCenter() {
        return this.center;
    }

    public int getDirection() {
        return this.direction;
    }

    public void setCenterAndDirection(PointWrapper newCenter, int newDirection) {
        this.center = newCenter;
        this.direction = newDirection;
        if (this.pointProfileObjectList != null) {
            switch (getSortCriteria()) {
                case Constants.SORT_CRITERIA.NAME_ASC:
                    Collections.sort(
                            this.pointProfileObjectList, new SortByNameASC());
                    break;
                case Constants.SORT_CRITERIA.NAME_DESC:
                    Collections.sort(
                            this.pointProfileObjectList, new SortByNameDESC());
                    break;
                case Constants.SORT_CRITERIA.DISTANCE_ASC:
                    Collections.sort(
                            this.pointProfileObjectList, new SortByDistanceFromCenterASC());
                    break;
                case Constants.SORT_CRITERIA.DISTANCE_DESC:
                    Collections.sort(
                            this.pointProfileObjectList, new SortByDistanceFromCenterDESC());
                    break;
                case Constants.SORT_CRITERIA.ORDER_ASC:
                    Collections.sort(
                            this.pointProfileObjectList, new SortByOrderASC());
                    break;
                case Constants.SORT_CRITERIA.ORDER_DESC:
                    Collections.sort(
                            this.pointProfileObjectList, new SortByOrderDESC());
                    break;
                default:
                    break;
            }
        }
    }

    public ArrayList<PointProfileObject> getPointProfileObjectList() {
        return this.pointProfileObjectList;
    }

    public void setPointProfileObjectList(ArrayList<PointProfileObject> newPointList) {
        this.pointProfileObjectList = newPointList;
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
        } else if (! (obj instanceof PointProfile)) {
			return false;
        }
		PointProfile other = (PointProfile) obj;
        return this.id == other.getId();
    }

}
