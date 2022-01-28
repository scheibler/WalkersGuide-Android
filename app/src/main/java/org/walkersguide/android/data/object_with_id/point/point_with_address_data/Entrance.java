package org.walkersguide.android.data.object_with_id.point.point_with_address_data;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.data.object_with_id.point.PointWithAddressData;


public class Entrance extends PointWithAddressData implements Serializable {
    private static final long serialVersionUID = 1l;

    private String label;

    public Entrance(JSONObject inputData) throws JSONException {
        super(inputData);
        this.label = Helper.getNullableStringFromJsonObject(inputData, KEY_LABEL);
    }

    public String getLabel() {
        return this.label;
    }

    @Override public String getOriginalName() {
        if (this.label != null) {
            return String.format(
                    "%1$s, %2$s", super.formatAddressMediumLength(), this.label);
        }
        return super.formatAddressMediumLength();
    }


    /**
     * to json
     */

    public static final String KEY_LABEL = "label";

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        if (this.label != null) {
            jsonObject.put(KEY_LABEL, this.label);
        }
        return jsonObject;
    }

}
