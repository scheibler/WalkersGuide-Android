package org.walkersguide.android.basic.point;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class POI extends Point {

    private ArrayList<POI> entranceList;
    private POI isInside;
    private String address, email, phone, website, openingHours;
    private int trafficSignalsSound, trafficSignalsVibration;

    public POI(Context context, JSONObject inputData) throws JSONException {
        // point super constructor
        super(context, inputData);

        // entrance list
        this.entranceList = new ArrayList<POI>();
        JSONArray jsonEntranceList;
        try {
            jsonEntranceList = inputData.getJSONArray("entrance_list");
        } catch (JSONException e) {
            jsonEntranceList = new JSONArray();
        }
        for (int j=0; j<jsonEntranceList.length(); j++) {
            POI entrance = null;
            try {
                entrance = new POI(context, jsonEntranceList.getJSONObject(j));
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
            this.isInside = new POI(context, inputData.getJSONObject("is_inside"));
        } catch (JSONException e) {
            isInside = null;
        }

        // other optional attributes
        try {
            this.address = inputData.getString("address");
        } catch (JSONException e) {
            this.address = "";
        }
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

        // traffic signal attributes
        try {
            this.trafficSignalsSound = inputData.getInt("traffic_signals_sound");
        } catch (JSONException e) {
            this.trafficSignalsSound = -1;
        }
        try {
            this.trafficSignalsVibration = inputData.getInt("traffic_signals_vibration");
        } catch (JSONException e) {
            this.trafficSignalsVibration = -1;
        }
    }

    public ArrayList<POI> getEntranceList() {
        return this.entranceList;
    }

    public POI getOuterBuilding() {
        return this.isInside;
    }

    public String getAddress() {
        return this.address;
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

    @Override public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        if (jsonObject == null) {
            return null;
        }

        // entrances
        JSONArray jsonEntranceList = new JSONArray();
        for (POI entrance : this.entranceList) {
            JSONObject jsonEntrance = entrance.toJson();
            if (jsonEntrance != null) {
                jsonEntranceList.put(jsonEntrance);
            }
        }
        if (jsonEntranceList.length() > 0) {
            try {
                jsonObject.put("entrance_list", jsonEntranceList);
            } catch (JSONException e) {}
        }

        // outer building
        if (this.isInside != null) {
            JSONObject outer = this.isInside.toJson();
            if (outer != null) {
                try {
                    jsonObject.put("is_inside", outer);
                } catch (JSONException e) {}
            }
        }

        // optional parameters
        if (! this.address.equals("")) {
            try {
                jsonObject.put("address", this.address);
            } catch (JSONException e) {}
        }
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

        // traffic signal attributes
        if (this.trafficSignalsSound > -1) {
            try {
                jsonObject.put("traffic_signals_sound", this.trafficSignalsSound);
            } catch (JSONException e) {}
        }
        if (this.trafficSignalsVibration > -1) {
            try {
                jsonObject.put("traffic_signals_vibration", this.trafficSignalsVibration);
            } catch (JSONException e) {}
        }

        return jsonObject;
    }

    @Override public String toString() {
        return super.getName() + "   " + super.getType() + ": " + super.getSubType();
    }

}
