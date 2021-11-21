package org.walkersguide.android.data.basic.point;

import org.walkersguide.android.R;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.util.StringUtility;
import org.walkersguide.android.util.GlobalInstance;


public class StreetAddress extends PointWithAddressData {


    public static class Builder extends PointWithAddressData.Builder {
        public Builder(String name, double latitude, double longitude) {
            super(
                    Point.Type.STREET_ADDRESS,
                    name,
                    GlobalInstance.getStringResource(R.string.pointTypeStreetAddress),
                    latitude,
                    longitude);
        }

        // build
        public StreetAddress build() throws JSONException {
            return new StreetAddress(super.inputData);
        }
    }


    public StreetAddress(JSONObject inputData) throws JSONException {
        super(inputData);
    }

    @Override public String getOriginalName() {
        return super.formatAddressMediumLength();
    }

}
