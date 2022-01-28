package org.walkersguide.android.data.object_with_id.point.point_with_address_data;

import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.R;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.point.PointWithAddressData;


public class StreetAddress extends PointWithAddressData {


    public static class Builder extends PointWithAddressData.Builder {
        public Builder(String name, double latitude, double longitude) {
            super(
                    Point.Type.STREET_ADDRESS,
                    name,
                    GlobalInstance.getStringResource(R.string.streetAddressPointSubtype),
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
