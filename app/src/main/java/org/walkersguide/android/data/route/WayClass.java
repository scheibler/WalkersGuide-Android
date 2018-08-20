package org.walkersguide.android.data.route;

import org.walkersguide.android.R;

import android.content.Context;
import org.walkersguide.android.util.Constants;


public class WayClass {

    private String id, name;

    public WayClass(Context context, String id) {
        this.id = id;
        // determine name from id
        if (id.equals(Constants.ROUTING_WAY_CLASS.BIG_STREETS)) {
            this.name = context.getResources().getString(R.string.routingWayClassBigStreets);
        } else if (id.equals(Constants.ROUTING_WAY_CLASS.SMALL_STREETS)) {
            this.name = context.getResources().getString(R.string.routingWayClassSmallStreets);
        } else if (id.equals(Constants.ROUTING_WAY_CLASS.PAVED_WAYS)) {
            this.name = context.getResources().getString(R.string.routingWayClassPavedWays);
        } else if (id.equals(Constants.ROUTING_WAY_CLASS.UNPAVED_WAYS)) {
            this.name = context.getResources().getString(R.string.routingWayClassUnpavedWays);
        } else if (id.equals(Constants.ROUTING_WAY_CLASS.STEPS)) {
            this.name = context.getResources().getString(R.string.routingWayClassSteps);
        } else if (id.equals(Constants.ROUTING_WAY_CLASS.UNCLASSIFIED_WAYS)) {
            this.name = context.getResources().getString(R.string.routingWayClassUnclassifiedWays);
        } else {
            this.name = id;
        }
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    @Override public String toString() {
        return this.name;
    }

	@Override public int hashCode() {
        return this.id.hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof WayClass)) {
			return false;
        }
		WayClass other = (WayClass) obj;
        return this.id.equals(other.getId());
    }

}
