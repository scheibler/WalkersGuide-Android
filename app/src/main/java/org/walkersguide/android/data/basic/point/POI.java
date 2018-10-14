package org.walkersguide.android.data.basic.point;

import android.content.Context;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.R;


public class POI extends PointWithAddressData {

    private ArrayList<PointWrapper> entranceList;
    private PointWrapper isInside;
    private String email, phone, website, openingHours;

    public POI(Context context, JSONObject inputData) throws JSONException {
        // point super constructor
        super(context, inputData);

        // entrance list
        this.entranceList = new ArrayList<PointWrapper>();
        JSONArray jsonEntranceList;
        try {
            jsonEntranceList = inputData.getJSONArray("entrance_list");
        } catch (JSONException e) {
            jsonEntranceList = new JSONArray();
        }
        for (int j=0; j<jsonEntranceList.length(); j++) {
            PointWrapper entrance = null;
            try {
                entrance = new PointWrapper(context, jsonEntranceList.getJSONObject(j));
            } catch (JSONException e) {
                entrance = null;
            } finally {
                if (entrance != null) {
                    this.entranceList.add(entrance);
                }
            }
        }

        // is inside a building
        try {
            this.isInside = new PointWrapper(context, inputData.getJSONObject("is_inside"));
        } catch (JSONException e) {
            isInside = null;
        }

        // other optional attributes
        try {
            this.email = inputData.getString("email");
        } catch (JSONException e) {
            this.email = "";
        }
        try {
            this.phone = inputData.getString("phone");
        } catch (JSONException e) {
            this.phone = "";
        }
        try {
            this.website = inputData.getString("website");
        } catch (JSONException e) {
            this.website = "";
        }
        try {
            this.openingHours = inputData.getString("opening_hours");
        } catch (JSONException e) {
            this.openingHours = "";
        }
    }

    public ArrayList<PointWrapper> getEntranceList() {
        return this.entranceList;
    }

    public PointWrapper getOuterBuilding() {
        return this.isInside;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getWebsite() {
        return this.website;
    }

    public String getOpeningHours() {
        return this.openingHours;
    }

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();

        // entrances
        JSONArray jsonEntranceList = new JSONArray();
        for (PointWrapper entrance : this.entranceList) {
            try {
                jsonEntranceList.put(entrance.toJson());
            } catch (JSONException e) {}
        }
        if (jsonEntranceList.length() > 0) {
            try {
                jsonObject.put("entrance_list", jsonEntranceList);
            } catch (JSONException e) {}
        }

        // outer building
        if (this.isInside != null) {
            try {
                jsonObject.put("is_inside", this.isInside.toJson());
            } catch (JSONException e) {}
        }

        // optional parameters
        if (! this.email.equals("")) {
            try {
                jsonObject.put("email", this.email);
            } catch (JSONException e) {}
        }
        if (! this.phone.equals("")) {
            try {
                jsonObject.put("phone", this.phone);
            } catch (JSONException e) {}
        }
        if (! this.website.equals("")) {
            try {
                jsonObject.put("website", this.website);
            } catch (JSONException e) {}
        }
        if (! this.openingHours.equals("")) {
            try {
                jsonObject.put("opening_hours", this.openingHours);
            } catch (JSONException e) {}
        }

        return jsonObject;
    }

    @Override public String toString() {
        String description = super.toString();
        if (! this.entranceList.isEmpty()) {
            description += String.format(
                    ", %1$s",
                    super.getContext().getResources().getQuantityString(
                        R.plurals.entrance, this.entranceList.size(), this.entranceList.size()));
        }
        return description;
    }

}
