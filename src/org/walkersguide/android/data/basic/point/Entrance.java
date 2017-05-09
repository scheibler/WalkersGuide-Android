package org.walkersguide.android.data.basic.point;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class Entrance extends Point {

    private String label;

    public Entrance(Context context, JSONObject inputData) throws JSONException {
        // point super constructor
        super(context, inputData);
        try {
            this.label = inputData.getString("label");
        } catch (JSONException e) {
            this.label = "";
        }
    }

    public String getLabel() {
        return this.label;
    }

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        if (! this.label.equals("")) {
            try {
                jsonObject.put("label", this.label);
            } catch (JSONException e) {}
        }
        return jsonObject;
    }

    @Override public String toString() {
        return super.toString();
    }

}
