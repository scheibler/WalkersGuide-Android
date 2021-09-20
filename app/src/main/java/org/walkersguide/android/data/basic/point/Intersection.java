package org.walkersguide.android.data.basic.point;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.segment.IntersectionSegment;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.util.GlobalInstance;


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
        this.numberOfStreets = StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_NUMBER_OF_STREETS);
        this.numberOfStreetsWithName = StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_NUMBER_OF_STREETS_WITH_NAME);
    }

    public ArrayList<IntersectionSegment> getSegmentList() {
        return this.segmentList;
    }

    public ArrayList<PedestrianCrossing> getPedestrianCrossingList() {
        return this.pedestrianCrossingList;
    }

    public Integer getNumberOfStreets() {
        return this.numberOfStreets;
    }

    public Integer getNumberOfStreetsWithName() {
        return this.numberOfStreetsWithName;
    }

    @Override public String toString() {
        String description = super.toString();
        if (numberOfStreets != null) {
            description += "\n";
            description += String.format(
                    GlobalInstance.getStringResource(R.string.intersectionNumberOfStreets),
                    GlobalInstance.getPluralResource(R.plurals.street, this.numberOfStreets));
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
