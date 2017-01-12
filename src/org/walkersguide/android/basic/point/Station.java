package org.walkersguide.android.basic.point;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class Station extends POI {

    public Station(Context context, JSONObject inputData) throws JSONException {
        // poi super constructor
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
        return super.getName() + "   " + super.getType() + ": " + super.getSubType();
    }

}
