package org.walkersguide.android.data.route;

import android.content.Context;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.R;


public class Route {

    private Context context;
    private int id, currentIndex;
    private PointWrapper startPoint, destinationPoint;
    private ArrayList<PointWrapper> viaPointList;
    private String description;
    private ArrayList<RouteObject> routeObjectList;

    public Route(Context context, int id,
            JSONObject jsonStartPoint, JSONObject jsonDestinationPoint, JSONArray jsonViaPointList,
            String description, int currentIndex, JSONArray jsonRouteObjectList) throws JSONException {
        this.context = context;
        this.id = id;
        this.startPoint = new PointWrapper(context, jsonStartPoint);
        this.destinationPoint = new PointWrapper(context, jsonDestinationPoint);
        this.viaPointList = new ArrayList<PointWrapper>();
        for (int i=0; i<jsonViaPointList.length(); i++) {
            this.viaPointList.add(
                    new PointWrapper(
                        context, jsonViaPointList.getJSONObject(i)));
        }
        this.description = description;
        this.currentIndex = currentIndex;
        this.routeObjectList = new ArrayList<RouteObject>();
        for (int i=0; i<jsonRouteObjectList.length(); i++) {
            this.routeObjectList.add(
                    new RouteObject(
                        context, i, jsonRouteObjectList.getJSONObject(i)));
        }
    }

    public int getId() {
        return this.id;
    }

    public PointWrapper getStartPoint() {
        return this.startPoint;
    }

    public PointWrapper getDestinationPoint() {
        return this.destinationPoint;
    }

    public ArrayList<PointWrapper> getViaPointList() {
        return this.viaPointList;
    }

    public String getDescription() {
        return this.description;
    }

    public int getCurrentRouteIndex() {
        return this.currentIndex;
    }

    public RouteObject getCurrentRouteObject() {
        try {
            return this.routeObjectList.get(this.currentIndex);
        } catch (IndexOutOfBoundsException e ) {
            return null;
        }
    }

    public ArrayList<RouteObject> getRouteObjectList() {
        return this.routeObjectList;
    }

    @Override public String toString() {
        return String.format(
                "%1$s: %2$s\n%3$s: %4$s",
                context.getResources().getString(R.string.buttonStartPoint),
                this.startPoint.getPoint().getName(),
                context.getResources().getString(R.string.buttonDestinationPoint),
                this.destinationPoint.getPoint().getName());
    }

	@Override public int hashCode() {
        int hash = 17;
		hash = hash * 31 + this.startPoint.hashCode();
		hash = hash * 31 + this.destinationPoint.hashCode();
        for (PointWrapper viaPoint : this.viaPointList) {
    		hash = hash * 31 + viaPoint.hashCode();
        }
		hash = hash * 31 + this.description.hashCode();
        return hash;
    }

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof Route)) {
			return false;
        }
		Route other = (Route) obj;
        return (this.startPoint.equals(other.getStartPoint())
                && this.destinationPoint.equals(other.getDestinationPoint())
                && this.viaPointList.size() == other.getViaPointList().size()
                && other.getViaPointList().containsAll(this.viaPointList)
                && this.viaPointList.containsAll(other.getViaPointList())
                && this.description.equals(other.getDescription()));
    }

}
