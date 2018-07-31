package org.walkersguide.android.data.basic.point;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class StreetAddress extends PointWithAddressData {

    public StreetAddress(Context context, JSONObject inputData) throws JSONException {
        // point super constructor
        super(context, inputData);
    }

    @Override public String getName() {
        return super.createPrintableAddress();
    }

    @Override public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override public String toString() {
        if (super.getSubType().equals("")
                || this.createPrintableAddress().equals(super.getSubType())) {
            return this.createPrintableAddress();
        }
        return String.format("%1$s (%2$s)", super.createPrintableAddress(), super.getSubType());
    }

}
