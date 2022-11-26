package org.walkersguide.android.data.object_with_id.point.point_with_address_data.poi;

import android.text.TextUtils;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.object_with_id.point.point_with_address_data.poi.station.Line;
import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.POI;
import java.util.Set;
import java.util.TreeSet;
import java.util.Comparator;
import timber.log.Timber;
import java.lang.NumberFormatException;


public class Station extends POI implements Serializable {
    private static final long serialVersionUID = 1l;

    private ArrayList<Line> lineList;
    private ArrayList<String> vehicleList;

    public Station(JSONObject inputData) throws JSONException {
        super(inputData);

        // lines
        this.lineList = new ArrayList<Line>();
        if (! inputData.isNull(KEY_LINE_LIST)) {
            JSONArray jsonLineList = inputData.getJSONArray(KEY_LINE_LIST);
            for (int j=0; j<jsonLineList.length(); j++) {
                try {
                    lineList.add(
                            new Line(
                                jsonLineList.getJSONObject(j)));
                } catch (JSONException e) {}
            }
        }

        // vehicles
        this.vehicleList = new ArrayList<String>();
        if (! inputData.isNull(KEY_VEHICLE_LIST)) {
            JSONArray jsonVehicleList = inputData.getJSONArray(KEY_VEHICLE_LIST);
            for (int j=0; j<jsonVehicleList.length(); j++) {
                try {
                    vehicleList.add(jsonVehicleList.getString(j));
                } catch (JSONException e) {}
            }
        }
    }

    public ArrayList<Line> getLineList() {
        return this.lineList;
    }

    public ArrayList<String> getVehicleList() {
        return this.vehicleList;
    }

    @Override public String toString() {
        String description = super.toString();
        if (! this.lineList.isEmpty()) {
            // unique line numbers
            Set<String> lineNrSet = new TreeSet<String>(new LineNrComparator());
            for (Line line : this.lineList) {
                lineNrSet.add(line.getNr());
            }
            description += "\n" + String.format(
                    GlobalInstance.getStringResource(R.string.stationLines),
                    TextUtils.join(", ", lineNrSet));
        }
        return description;
    }

    private class LineNrComparator implements Comparator<String>{
        @Override public int compare(String nr1, String nr2) {
            String nr1LeadingString = extractLeadingString(nr1);
            String nr2LeadingString = extractLeadingString(nr2);
            if (nr1LeadingString.equalsIgnoreCase(nr2LeadingString)) {
                return Integer.compare(
                        extractInt(nr1), extractInt(nr2));
            } else {
                return nr1LeadingString.compareTo(nr2LeadingString);
            }
        }

        private String extractLeadingString(String s) {
            String leadingString = "";
            try {
                leadingString = s.split("[0-9]+")[0];
            } catch(ArrayIndexOutOfBoundsException e) {}
            return leadingString;
        }

        private int extractInt(String s) {
            int number = 0;
            try {
                number =Integer.parseInt(s.replaceAll("\\D", ""));
            } catch (NumberFormatException e) {}
            return number;
        }
    }


    /**
     * to json
     */

    public static final String KEY_LINE_LIST = "lines";
    public static final String KEY_VEHICLE_LIST = "vehicles";

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();

        // lines
        JSONArray jsonLineList = new JSONArray();
        for (Line line : this.lineList) {
            jsonLineList.put(line.toJson());
        }
        jsonObject.put(KEY_LINE_LIST, jsonLineList);

        JSONArray jsonVehicleList = new JSONArray();
        for (String vehicle : this.vehicleList) {
            jsonVehicleList.put(vehicle);
        }
        jsonObject.put(KEY_VEHICLE_LIST, jsonVehicleList);

        return jsonObject;
    }

}
