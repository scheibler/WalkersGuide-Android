package org.walkersguide.android.data.basic.point;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;
import org.walkersguide.android.util.StringUtility;


public class Line implements Serializable {
    private static final long serialVersionUID = 1l;

    private String nr, to;

    public Line(JSONObject inputData) throws JSONException {
        this.nr = inputData.getString(KEY_NR);
        this.to = StringUtility.getNullableStringFromJsonObject(inputData, KEY_TO);
    }

    public String getNr() {
        return this.nr;
    }

    public String getTo() {
        return this.to;
    }

    public String getDescription() {
        if (this.to != null) {
            return String.format("%1$s, %2$s", this.nr, this.to);
        }
        return this.nr;
    }

    @Override public String toString() {
        return this.nr;
    }


    /**
     * to json
     */

    public static final String KEY_NR = "nr";
    public static final String KEY_TO = "to";

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_NR, this.nr);
        if (this.to != null) {
            jsonObject.put(KEY_TO, this.to);
        }
        return jsonObject;
    }

}
