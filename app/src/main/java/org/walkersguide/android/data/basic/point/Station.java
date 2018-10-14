package org.walkersguide.android.data.basic.point;

import android.content.Context;

import android.text.TextUtils;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.station.Line;
import org.walkersguide.android.R;


public class Station extends POI {

    private ArrayList<Line> lineList;
    private ArrayList<String> vehicleList;
    private long stationId;
    private int exactPosition;

    public Station(Context context, JSONObject inputData) throws JSONException {
        // poi super constructor
        super(context, inputData);
        // station id from deutsche bahn
        try {
            this.stationId = inputData.getLong("station_id");
        } catch (JSONException e) {
            this.stationId = -1l;
        }
        // exact position: only for route objects
        try {
            boolean exactPositionFromJson = inputData.getBoolean("exact_position");
            if (exactPositionFromJson) {
                this.exactPosition = 1;
            } else {
                this.exactPosition = 0;
            }
        } catch (JSONException e) {
            this.exactPosition = -1;
        }

        // lines
        this.lineList = new ArrayList<Line>();
        JSONArray jsonLineList = null;
        try {
            jsonLineList = inputData.getJSONArray("lines");
        } catch (JSONException e) {
            jsonLineList = null;
        } finally {
            if (jsonLineList != null) {
                for (int i=0; i<jsonLineList.length(); i++) {
                    Line line = null;
                    try {
                        line = new Line(jsonLineList.getJSONObject(i));
                    } catch (JSONException e) {
                        line = null;
                    } finally {
                        if (line != null) {
                            this.lineList.add(line);
                        }
                    }
                }
            }
        }

        // vehicles
        this.vehicleList = new ArrayList<String>();
        JSONArray jsonVehicleList = null;
        try {
            jsonVehicleList = inputData.getJSONArray("vehicles");
        } catch (JSONException e) {
            jsonVehicleList = null;
        } finally {
            if (jsonVehicleList != null) {
                for (int i=0; i<jsonVehicleList.length(); i++) {
                    String vehicle = null;
                    try {
                        vehicle = jsonVehicleList.getString(i);
                    } catch (JSONException e) {
                        vehicle = null;
                    } finally {
                        if (vehicle != null) {
                            this.vehicleList.add(vehicle);
                        }
                    }
                }
            }
        }
    }

    public ArrayList<Line> getLineList() {
        return this.lineList;
    }

    public ArrayList<String> getVehicleList() {
        return this.vehicleList;
    }

    public long getStationId() {
        return this.stationId;
    }

    public int getExactPosition() {
        return this.exactPosition;
    }

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        if (this.stationId > -1) {
            try {
                jsonObject.put("stationId", this.stationId);
            } catch (JSONException e) {}
        }
        if (this.exactPosition > -1) {
            try {
                jsonObject.put("exact_position", this.exactPosition == 1 ? true : false);
            } catch (JSONException e) {}
        }
        // lines
        JSONArray jsonLineList = new JSONArray();
        for (Line line : this.lineList) {
            try {
                jsonLineList.put(line.toJson());
            } catch (JSONException e) {}
        }
        if (jsonLineList.length() > 0) {
            try {
                jsonObject.put("lines", jsonLineList);
            } catch (JSONException e) {}
        }
        // vehicles
        JSONArray jsonVehicleList = new JSONArray();
        for (String vehicle : this.vehicleList) {
            jsonVehicleList.put( vehicle);
        }
        if (jsonVehicleList.length() > 0) {
            try {
                jsonObject.put("vehicles", jsonVehicleList);
            } catch (JSONException e) {}
        }
        return jsonObject;
    }

    @Override public String toString() {
        String description = super.toString();
        if (! this.lineList.isEmpty()) {
            description += "\n" + String.format(
                    super.getContext().getResources().getString(R.string.stationLines),
                    TextUtils.join(", ", this.lineList));
        } else if (! this.vehicleList.isEmpty()) {
            description += "\n" + String.format(
                    super.getContext().getResources().getString(R.string.stationLines),
                    TextUtils.join(", ", this.vehicleList));
        }
        return description;
    }

}
