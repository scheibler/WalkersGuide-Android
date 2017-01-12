package org.walkersguide.android.basic.point;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class Intersection extends Point {

    public Intersection(Context context, JSONObject inputData) throws JSONException {
        // point super constructor
        super(context, inputData);
    }

    @Override public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        if (jsonObject == null) {
            return null;
        }
        return jsonObject;
    }

    @Override public String toString() {
        return super.getName() + "   " + this.getType() + ": " + super.getSubType();
    }

}
