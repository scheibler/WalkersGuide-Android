package org.walkersguide.android.data.station;

import org.json.JSONException;
import org.json.JSONObject;

public class Line {

    private String nr, to;

    public Line(JSONObject inputData) throws JSONException {
        this.nr = inputData.getString("nr");
        try {
            this.to = inputData.getString("to");
        } catch (JSONException e) {
            this.to = "";
        }
    }

    public String getNr() {
        return this.nr;
    }

    public String getTo() {
        return this.to;
    }

    public String getDescription() {
        if (! this.to.equals("")) {
            return String.format("%1$s, %2$s", this.nr, this.to);
        }
        return this.nr;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nr", this.nr);
        if (! this.to.equals("")) {
            try {
                jsonObject.put("to", this.to);
            } catch (JSONException e) {}
        }
        return jsonObject;
    }

    @Override public String toString() {
        return this.nr;
    }

}
