package org.walkersguide.android.data.object_with_id.point;

import org.walkersguide.android.data.object_with_id.common.Coordinates;
import org.walkersguide.android.util.Helper;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.Point;
import android.text.TextUtils;
import androidx.core.util.Pair;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.RelativeBearing;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.data.object_with_id.Segment.SortByBearingRelativeTo;
import java.lang.CharSequence;
import android.text.SpannableString;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import org.walkersguide.android.ui.UiHelper;
import org.walkersguide.android.data.Angle;
import java.util.stream.Collectors;


public class Intersection extends Point implements Serializable {
    private static final long serialVersionUID = 1l;

    private ArrayList<IntersectionSegment> segmentList;
    private ArrayList<PedestrianCrossing> pedestrianCrossingList;
    private Integer numberOfStreets, numberOfStreetsWithName;

    public Intersection(JSONObject inputData) throws JSONException {
        super(inputData);

        // way segment list
        this.segmentList = new ArrayList<IntersectionSegment>();
        JSONArray jsonIntersectionSegmentList = getJsonIntersectionSegmentListWithStartAndEndCoordinates(
                inputData.getJSONArray(KEY_WAY_LIST),
                super.getCoordinates());
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

    public boolean isImportant() {
        return this.numberOfStreetsWithName != null
            ? this.numberOfStreetsWithName > 1
            : false;
    }

    public String formatNumberOfStreets() {
        return String.format(
                GlobalInstance.getStringResource(R.string.intersectionNumberOfStreets),
                GlobalInstance.getPluralResource(
                    R.plurals.street,
                    this.numberOfStreets != null ? this.numberOfStreets : this.segmentList.size()));
    }

    @Override public String toString() {
        String description = super.toString();
        // second line: number of streets
        description += System.lineSeparator();
        description += formatNumberOfStreets();
        // third line: crossings nearby
        if (hasPedestrianCrossings()) {
            description += System.lineSeparator();
            description += formatNumberOfCrossingsNearby();
        }
        return description;
    }

    // intersection segments

    public ArrayList<IntersectionSegment> getSegmentList() {
        return this.segmentList;
    }

    public IntersectionSegment findIntersectionSegmentWhichIsPartOfPreviousRouteSegment() {
        for (IntersectionSegment intersectionSegment : this.segmentList) {
            if (intersectionSegment.isPartOfPreviousRouteSegment()) {
                return intersectionSegment;
            }
        }
        return null;
    }

    public IntersectionSegment findIntersectionSegmentWhichIsPartOfNextRouteSegment() {
        for (IntersectionSegment intersectionSegment : this.segmentList) {
            if (intersectionSegment.isPartOfNextRouteSegment()) {
                return intersectionSegment;
            }
        }
        return null;
    }

    public IntersectionSegment findMatchingIntersectionSegmentFor(long nextNodeId) {
        for (IntersectionSegment intersectionSegment : this.segmentList) {
            if (nextNodeId == intersectionSegment.getNextNodeId()) {
                return intersectionSegment;
            }
        }
        return null;
    }

    public IntersectionSegment findClosestIntersectionSegmentTo(Bearing bearing, int threshold) {
        Pair<IntersectionSegment,Integer> closest = null;
        for (IntersectionSegment intersectionSegment : this.segmentList) {
            int difference = intersectionSegment.getBearing().differenceTo(bearing);
            if (closest == null || closest.second > difference) {
                closest = Pair.create(intersectionSegment, difference);
            }
        }
        return closest.second <= threshold ? closest.first : null;
    }

    public static class SchemeData {
        public final String intersectionName;
        public final LinkedHashMap<RelativeBearing,IntersectionSegment> segmentMap;
        public final CharSequence legend;

        public SchemeData(String intersectionName, LinkedHashMap<RelativeBearing, IntersectionSegment> segmentMap, CharSequence legend) {
            this.intersectionName = intersectionName;
            this.segmentMap = segmentMap;
            this.legend = legend;
        }

        public String formatLlegendAsOneLiner() {
            return legend.toString().replace("\n", " ");    // still comma separated (see getDescriptionForSortedSegmentMap())
        }
    }

    public SchemeData getSchemeDataForFixedViewingDirectionFromPreviousRouteSegment(boolean includeWhatsBehind) {
        IntersectionSegment partOfPreviousRouteSegment = findIntersectionSegmentWhichIsPartOfPreviousRouteSegment();
        if (partOfPreviousRouteSegment == null) return null;
        return getSchemeDataForViewingDirection(
                partOfPreviousRouteSegment.getBearing().inverse(),
                Angle.Quadrant.Q3.max,  // bearing offset = 157 -> sort the ways, which are strongly to the left of the user, to the top of the list
                includeWhatsBehind);
    }

    public SchemeData getSchemeDataForViewingDirection(
            Bearing viewingDirection, int offsetAngle, boolean includeWhatsBehind) {
        LinkedHashMap<RelativeBearing,IntersectionSegment> segmentMap = getSegmentMapSortedBy(
                viewingDirection, offsetAngle, includeWhatsBehind);
        CharSequence legend = getDescriptionForSortedSegmentMap(segmentMap);
        return new SchemeData(getName(), segmentMap, legend);
    }

    private LinkedHashMap<RelativeBearing,IntersectionSegment> getSegmentMapSortedBy(
            Bearing viewingDirection, int offsetAngle, boolean includeWhatsBehind) {
        LinkedHashMap<RelativeBearing,IntersectionSegment> segmentMap = new LinkedHashMap<>();
        SortByBearingRelativeTo comparator =
            new Segment.SortByBearingRelativeTo(viewingDirection, offsetAngle, true);

        for (IntersectionSegment intersectionSegment : this.segmentList
                .stream().sorted(comparator).collect(Collectors.toList())) {
            RelativeBearing relativeBearingIntersectionSegment = intersectionSegment
                .getBearing()
                .relativeTo(viewingDirection);

            if (includeWhatsBehind
                    || relativeBearingIntersectionSegment.getDirection() != RelativeBearing.Direction.BEHIND) {
                segmentMap.put(relativeBearingIntersectionSegment, intersectionSegment);
            }
        }
        return segmentMap;
    }

    private CharSequence getDescriptionForSortedSegmentMap(
            LinkedHashMap<RelativeBearing,IntersectionSegment> segmentMap) {
        CharSequence formattedIntersectionStructure = new SpannableString("");
        Iterator<Map.Entry<RelativeBearing, IntersectionSegment>> iterator = segmentMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<RelativeBearing, IntersectionSegment> entry = iterator.next();
            RelativeBearing relativeBearingIntersectionSegment = entry.getKey();
            IntersectionSegment intersectionSegment = entry.getValue();

            // instruction structure label text (preserves text formatting)
            formattedIntersectionStructure = TextUtils.concat(
                    formattedIntersectionStructure,
                    UiHelper.bold(
                        relativeBearingIntersectionSegment.getDirection().toString()),
                    ":\n",
                    intersectionSegment.isPartOfNextRouteSegment()
                    ? UiHelper.red(intersectionSegment.getName())
                    : intersectionSegment.getName());

            if (iterator.hasNext()) {
                formattedIntersectionStructure = TextUtils.concat(
                        formattedIntersectionStructure, ",\n");
            }
        }

        return formattedIntersectionStructure;
    }

    // crossings nearby

    public ArrayList<PedestrianCrossing> getPedestrianCrossingList() {
        return this.pedestrianCrossingList;
    }

    public boolean hasPedestrianCrossings() {
        return this.pedestrianCrossingList != null && ! this.pedestrianCrossingList.isEmpty();
    }

    public String formatNumberOfCrossingsNearby() {
        return GlobalInstance.getPluralResource(
                R.plurals.intersectionNumberOfCrossingsNearby,
                hasPedestrianCrossings() ? this.pedestrianCrossingList.size() : 0);
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


    private static JSONArray getJsonIntersectionSegmentListWithStartAndEndCoordinates(
            JSONArray jsonIntersectionSegmentList, Coordinates intersectionCoordinates) throws JSONException {
        JSONArray newJsonIntersectionSegmentList = new JSONArray();

        for (int j=0; j<jsonIntersectionSegmentList.length(); j++) {
            JSONObject jsonIntersectionSegment = jsonIntersectionSegmentList.getJSONObject(j);

            Coordinates startCoordinates = null, endCoordinates = null;
            try {
                startCoordinates = new Coordinates(
                        jsonIntersectionSegment.getJSONObject(Segment.KEY_START));
                endCoordinates = new Coordinates(
                        jsonIntersectionSegment.getJSONObject(Segment.KEY_END));
            } catch (JSONException e) {}

            if (startCoordinates == null || endCoordinates == null) {
                startCoordinates = intersectionCoordinates;
                if (startCoordinates == null) {
                    throw new JSONException("intersectionCoordinates is null");
                }
                jsonIntersectionSegment.put(
                        Segment.KEY_START, startCoordinates.toJson());

                endCoordinates = Helper.calculateEndCoordinatesForStartCoordinatesAndAngle(
                        startCoordinates,
                        new Bearing(jsonIntersectionSegment.getInt(Segment.KEY_BEARING)));
                if (endCoordinates == null) {
                    throw new JSONException("Could not calculate end coordinates from bearing");
                }
                jsonIntersectionSegment.put(
                        Segment.KEY_END, endCoordinates.toJson());
            }

            newJsonIntersectionSegmentList.put(jsonIntersectionSegment);
        }

        return newJsonIntersectionSegmentList;
    }

}
