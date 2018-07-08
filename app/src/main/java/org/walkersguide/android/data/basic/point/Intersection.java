package org.walkersguide.android.data.basic.point;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.segment.IntersectionSegment;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;

import android.content.Context;

public class Intersection extends Point {

    private ArrayList<IntersectionSegment> segmentList;
    private ArrayList<PointWrapper> pedestrianCrossingList;
    private int numberOfStreets, numberOfStreetsWithName;

    public Intersection(Context context, JSONObject inputData) throws JSONException {
        // point super constructor
        super(context, inputData);

        // way segment list
        this.segmentList = new ArrayList<IntersectionSegment>();
        JSONArray jsonIntersectionSegmentList = inputData.getJSONArray("way_list");
        for (int j=0; j<jsonIntersectionSegmentList.length(); j++) {
            this.segmentList.add(
                    new IntersectionSegment(context, jsonIntersectionSegmentList.getJSONObject(j)));
        }

        // pedestrian crossing list
        this.pedestrianCrossingList = new ArrayList<PointWrapper>();
        JSONArray jsonPedestrianCrossingList;
        try {
            jsonPedestrianCrossingList = inputData.getJSONArray("pedestrian_crossing_list");
        } catch (JSONException e) {
            jsonPedestrianCrossingList = new JSONArray();
        }
        for (int j=0; j<jsonPedestrianCrossingList.length(); j++) {
            PointWrapper pedestrianCrossing = null;
            try {
                pedestrianCrossing = new PointWrapper(context, jsonPedestrianCrossingList.getJSONObject(j));
            } catch (JSONException e) {
                pedestrianCrossing = null;
            } finally {
                if (pedestrianCrossing != null) {
                    this.pedestrianCrossingList.add(pedestrianCrossing);
                }
            }
        }

        // number of streets
        try {
            this.numberOfStreets = inputData.getInt("number_of_streets");
        } catch (JSONException e) {
            this.numberOfStreets = this.segmentList.size();
        }
        try {
            this.numberOfStreetsWithName = inputData.getInt("number_of_streets_with_name");
        } catch (JSONException e) {
            this.numberOfStreetsWithName = 0;
        }
    }

    public ArrayList<IntersectionSegment> getSegmentList() {
        return this.segmentList;
    }

    public ArrayList<PointWrapper> getPedestrianCrossingList() {
        return this.pedestrianCrossingList;
    }

    public int getNumberOfStreets() {
        return this.numberOfStreets;
    }

    public int getNumberOfStreetsWithName() {
        return this.numberOfStreetsWithName;
    }

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        // ways
        JSONArray jsonIntersectionSegmentList = new JSONArray();
        for (IntersectionSegment intersectionSegment : this.segmentList) {
            jsonIntersectionSegmentList.put(intersectionSegment.toJson());
        }
        jsonObject.put("way_list", jsonIntersectionSegmentList);

        // pedestrian crossings
        JSONArray jsonPedestrianCrossingList = new JSONArray();
        for (PointWrapper pedestrianCrossing : this.pedestrianCrossingList) {
            try {
                jsonPedestrianCrossingList.put(pedestrianCrossing.toJson());
            } catch (JSONException e) {}
        }
        if (jsonPedestrianCrossingList.length() > 0) {
            try {
                jsonObject.put("pedestrian_crossing_list", jsonPedestrianCrossingList);
            } catch (JSONException e) {}
        }

        // other variables
        try {
            jsonObject.put("number_of_streets_with_name", this.numberOfStreetsWithName);
        } catch (JSONException e) {}
        try {
            jsonObject.put("number_of_streets", this.numberOfStreets);
        } catch (JSONException e) {}

        return jsonObject;
    }

    @Override public String toString() {
        return String.format(
                super.getContext().getResources().getString(R.string.intersectionDescription),
                super.getName(),
                this.numberOfStreets);
    }


}
