package org.walkersguide.android.data.route;

import android.content.Context;

import com.google.common.primitives.Doubles;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;


public class WayClass {

    private String id;
    private double weight;

    public WayClass(String id, double weight) {
        this.id = id;
        if (Doubles.contains(Constants.RoutingWayClassWeightValueArray, weight)) {
            this.weight = weight;
        } else {
            this.weight = defaultWeightForWayClass(id);
        }
    }

    public WayClass( JSONObject inputData) throws JSONException {
        this.id = inputData.getString("id");
        this.weight = inputData.getDouble("weight");
    }

    public String getId() {
        return this.id;
    }

    public double getWeight() {
        return this.weight;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", this.id);
        jsonObject.put("weight", this.weight);
        return jsonObject;
    }

    @Override public String toString() {
        return this.id;
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


    public static String idToString(Context context, String id) {
        if (id.equals(Constants.ROUTING_WAY_CLASS_ID.BIG_STREETS)) {
            return context.getResources().getString(R.string.routingWayClassBigStreets);
        } else if (id.equals(Constants.ROUTING_WAY_CLASS_ID.SMALL_STREETS)) {
            return context.getResources().getString(R.string.routingWayClassSmallStreets);
        } else if (id.equals(Constants.ROUTING_WAY_CLASS_ID.PAVED_WAYS)) {
            return context.getResources().getString(R.string.routingWayClassPavedWays);
        } else if (id.equals(Constants.ROUTING_WAY_CLASS_ID.UNPAVED_WAYS)) {
            return context.getResources().getString(R.string.routingWayClassUnpavedWays);
        } else if (id.equals(Constants.ROUTING_WAY_CLASS_ID.STEPS)) {
            return context.getResources().getString(R.string.routingWayClassSteps);
        } else if (id.equals(Constants.ROUTING_WAY_CLASS_ID.UNCLASSIFIED_WAYS)) {
            return context.getResources().getString(R.string.routingWayClassUnclassifiedWays);
        } else {
            return id;
        }
    }

    public static String weightToString(Context context, double weight) {
        if (weight == Constants.ROUTING_WAY_CLASS_WEIGHT.VERY_PREFERABLE) {
            return context.getResources().getString(R.string.wayClassWeightVeryPreferable);
        } else if (weight == Constants.ROUTING_WAY_CLASS_WEIGHT.PREFERABLE) {
            return context.getResources().getString(R.string.wayClassWeightPreferable);
        } else if (weight == Constants.ROUTING_WAY_CLASS_WEIGHT.SLIGHTLY_PREFERABLE) {
            return context.getResources().getString(R.string.wayClassWeightSlightlyPreferable);
        } else if (weight == Constants.ROUTING_WAY_CLASS_WEIGHT.NEUTRAL) {
            return context.getResources().getString(R.string.wayClassWeightNeutral);
        } else if (weight == Constants.ROUTING_WAY_CLASS_WEIGHT.SLIGHTLY_NEGLIGIBLE) {
            return context.getResources().getString(R.string.wayClassWeightSlightlyNegligible);
        } else if (weight == Constants.ROUTING_WAY_CLASS_WEIGHT.NEGLIGIBLE) {
            return context.getResources().getString(R.string.wayClassWeightNegligible);
        } else if (weight == Constants.ROUTING_WAY_CLASS_WEIGHT.VERY_NEGLIGIBLE) {
            return context.getResources().getString(R.string.wayClassWeightVeryNegligible);
        } else if (weight == Constants.ROUTING_WAY_CLASS_WEIGHT.IGNORE) {
            return context.getResources().getString(R.string.wayClassWeightIgnore);
        } else {
            return String.valueOf(weight);
        }
    }

    public static double defaultWeightForWayClass(String id) {
        if (id.equals(Constants.ROUTING_WAY_CLASS_ID.BIG_STREETS)) {
            return Constants.ROUTING_WAY_CLASS_WEIGHT.SLIGHTLY_PREFERABLE;
        } else if (id.equals(Constants.ROUTING_WAY_CLASS_ID.SMALL_STREETS)) {
            return Constants.ROUTING_WAY_CLASS_WEIGHT.PREFERABLE;
        } else if (id.equals(Constants.ROUTING_WAY_CLASS_ID.PAVED_WAYS)) {
            return Constants.ROUTING_WAY_CLASS_WEIGHT.NEUTRAL;
        } else if (id.equals(Constants.ROUTING_WAY_CLASS_ID.UNPAVED_WAYS)) {
            return Constants.ROUTING_WAY_CLASS_WEIGHT.NEGLIGIBLE;
        } else if (id.equals(Constants.ROUTING_WAY_CLASS_ID.STEPS)) {
            return Constants.ROUTING_WAY_CLASS_WEIGHT.NEGLIGIBLE;
        } else if (id.equals(Constants.ROUTING_WAY_CLASS_ID.UNCLASSIFIED_WAYS)) {
            return Constants.ROUTING_WAY_CLASS_WEIGHT.SLIGHTLY_NEGLIGIBLE;
        } else {
            return Constants.ROUTING_WAY_CLASS_WEIGHT.IGNORE;
        }
    }

}
