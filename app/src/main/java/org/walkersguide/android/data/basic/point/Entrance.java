package org.walkersguide.android.data.basic.point;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;


public class Entrance extends PointWithAddressData {

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

    @Override public String getName() {
        return super.createPrintableAddress();
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
        if (super.getSubType().equals("")
                || this.createPrintableAddress().equals(super.getSubType())) {
            return this.createPrintableAddress();
        }
        return String.format("%1$s (%2$s)", super.createPrintableAddress(), super.getSubType());
    }

}
