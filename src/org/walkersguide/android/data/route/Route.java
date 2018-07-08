package org.walkersguide.android.data.route;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;

import android.content.Context;

public class Route {

    private Context context;
    private int id, currentIndex;
    private PointWrapper startPoint, destinationPoint;
    private String description;
    private ArrayList<RouteObject> routeObjectList;

    public Route(Context context, int id, JSONObject jsonStartPoint, JSONObject jsonDestinationPoint,
            String description, int currentIndex, JSONArray jsonRouteObjectList) throws JSONException {
        this.context = context;
        this.id = id;
        this.startPoint = new PointWrapper(context, jsonStartPoint);
        this.destinationPoint = new PointWrapper(context, jsonDestinationPoint);
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

    public String getDescription() {
        return this.description;
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
                && this.description.equals(other.getDescription()));
    }

}
