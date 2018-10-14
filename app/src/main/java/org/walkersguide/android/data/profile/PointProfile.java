package org.walkersguide.android.data.profile;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject.SortByDistanceFromCenterASC;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject.SortByDistanceFromCenterDESC;
import org.walkersguide.android.data.basic.wrapper.PointWrapper.SortByNameASC;
import org.walkersguide.android.data.basic.wrapper.PointWrapper.SortByNameDESC;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject.SortByOrderASC;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject.SortByOrderDESC;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.util.Constants;


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

    public ArrayList<PointProfileObject> getPointProfileObjectList() {
        return this.pointProfileObjectList;
    }

    public void setCenterDirectionAndPointList(PointWrapper newCenter, int newDirection,
            ArrayList<PointProfileObject> newPointProfileObjectList) {
        this.center = newCenter;
        this.direction = newDirection;
        this.pointProfileObjectList = newPointProfileObjectList;
        // sort point list
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
