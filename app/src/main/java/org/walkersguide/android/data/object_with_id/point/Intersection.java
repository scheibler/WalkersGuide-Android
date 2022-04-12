package org.walkersguide.android.data.object_with_id.point;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.Point;
import android.text.TextUtils;


public class Intersection extends Point implements Serializable {
    private static final long serialVersionUID = 1l;

    private ArrayList<IntersectionSegment> segmentList;
    private ArrayList<PedestrianCrossing> pedestrianCrossingList;
    private Integer numberOfStreets, numberOfStreetsWithName;

    public Intersection(JSONObject inputData) throws JSONException {
        super(inputData);

        // way segment list
        this.segmentList = new ArrayList<IntersectionSegment>();
        JSONArray jsonIntersectionSegmentList = inputData.getJSONArray(KEY_WAY_LIST);
        for (int j=0; j<jsonIntersectionSegmentList.length(); j++) {
            JSONObject jsonIntersectionSegment = jsonIntersectionSegmentList.getJSONObject(j);
            // include intersection node id, if not already present
            if (jsonIntersectionSegment.isNull(IntersectionSegment.KEY_INTERSECTION_NODE_ID)) {
                jsonIntersectionSegment.put(IntersectionSegment.KEY_INTERSECTION_NODE_ID, super.getId());
            }
            // add to list
            this.segmentList.add(new IntersectionSegment(jsonIntersectionSegment));
        }
        if (this.segmentList.isEmpty()) {
            throw new JSONException("Intersection segment list is empty");
        }

        // pedestrian crossing list
        this.pedestrianCrossingList = new ArrayList<PedestrianCrossing>();
        if (! inputData.isNull(KEY_PEDESTRIAN_CROSSING_LIST)) {
            JSONArray jsonPedestrianCrossingList = inputData.getJSONArray(KEY_PEDESTRIAN_CROSSING_LIST);
            for (int j=0; j<jsonPedestrianCrossingList.length(); j++) {
                try {
                    pedestrianCrossingList.add(
                            new PedestrianCrossing(
                                jsonPedestrianCrossingList.getJSONObject(j)));
                } catch (JSONException e) {}
            }
        }

        // number of streets
        this.numberOfStreets = Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_NUMBER_OF_STREETS);
        this.numberOfStreetsWithName = Helper.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_NUMBER_OF_STREETS_WITH_NAME);
    }

    public ArrayList<IntersectionSegment> getSegmentList() {
        return this.segmentList;
    }

    public ArrayList<PedestrianCrossing> getPedestrianCrossingList() {
        return this.pedestrianCrossingList;
    }

    public boolean hasPedestrianCrossing() {
        return this.pedestrianCrossingList != null && ! this.pedestrianCrossingList.isEmpty();
    }

    public boolean isImportant() {
        return this.numberOfStreetsWithName > 1;
    }

    public String formatNumberOfStreets() {
        if (this.numberOfStreets != null) {
            return String.format(
                    GlobalInstance.getStringResource(R.string.intersectionNumberOfStreets),
                    GlobalInstance.getPluralResource(R.plurals.street, this.numberOfStreets));
        }
        return "";
    }

    public String formatNumberOfCrossingsNearby() {
        if (hasPedestrianCrossing()) {
            return String.format(
                    GlobalInstance.getStringResource(R.string.intersectionNumberOfCrossingsNearby),
                    GlobalInstance.getPluralResource(R.plurals.crossing, this.pedestrianCrossingList.size()));
        }
        return "";
    }

    @Override public String toString() {
        String description = super.toString();
        // second line: number of streets
        String numberOfStreetsFormatted = formatNumberOfStreets();
        if (! TextUtils.isEmpty(numberOfStreetsFormatted)) {
            description += String.format("\n%1$s", numberOfStreetsFormatted);
        }
        // third line: crossings nearby
        String numberOfCrossingsNearbyFormatted = formatNumberOfCrossingsNearby();
        if (! TextUtils.isEmpty(numberOfCrossingsNearbyFormatted)) {
            description += String.format("\n%1$s", numberOfCrossingsNearbyFormatted);
        }
        return description;
    }


    /**
     * to json
     */

    public static final String KEY_WAY_LIST = "way_list";
    public static final String KEY_PEDESTRIAN_CROSSING_LIST = "pedestrian_crossing_list";
    public static final String KEY_NUMBER_OF_STREETS = "number_of_streets";
    public static final String KEY_NUMBER_OF_STREETS_WITH_NAME = "number_of_streets_with_name";

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();

        // ways
        JSONArray jsonIntersectionSegmentList = new JSONArray();
        for (IntersectionSegment intersectionSegment : this.segmentList) {
            jsonIntersectionSegmentList.put(intersectionSegment.toJson());
        }
        jsonObject.put(KEY_WAY_LIST, jsonIntersectionSegmentList);

        // pedestrian crossings
        JSONArray jsonPedestrianCrossingList = new JSONArray();
        for (PedestrianCrossing pedestrianCrossing : this.pedestrianCrossingList) {
            jsonPedestrianCrossingList.put(pedestrianCrossing.toJson());
        }
        jsonObject.put(KEY_PEDESTRIAN_CROSSING_LIST, jsonPedestrianCrossingList);

        // other variables
        if (numberOfStreets != null) {
            jsonObject.put(KEY_NUMBER_OF_STREETS, this.numberOfStreets);
        }
        if (numberOfStreetsWithName != null) {
            jsonObject.put(KEY_NUMBER_OF_STREETS_WITH_NAME, this.numberOfStreetsWithName);
        }

        return jsonObject;
    }

}
