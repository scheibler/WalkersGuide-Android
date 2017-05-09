package org.walkersguide.android.data.basic.point;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class StreetAddress extends Point {

    public StreetAddress(Context context, JSONObject inputData) throws JSONException {
        // point super constructor
        super(context, inputData);
    }

    @Override public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override public String toString() {
        return super.toString();
    }

}
