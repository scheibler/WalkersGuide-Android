package org.walkersguide.android.data.basic.point;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;


public class StreetAddress extends PointWithAddressData {

    public StreetAddress(Context context, JSONObject inputData) throws JSONException {
        // point super constructor
        super(context, inputData);
    }

    @Override public String getName() {
        return super.formatAddressMediumLength();
    }

    @Override public JSONObject toJson() throws JSONException {
        return super.toJson();
    }

    @Override public String toString() {
        return String.format("%1$s (%2$s)", this.getName(), super.getSubType());
    }

}
