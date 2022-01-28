package org.walkersguide.android.server.wg.poi;

import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import java.io.Serializable;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;


public class PoiCategory implements Serializable {
    private static final long serialVersionUID = 1l;

    // supported ids
    public static final String TRANSPORT_BUS_TRAM = "transport_bus_tram";
    public static final String TRANSPORT_TRAIN_LIGHTRAIL_SUBWAY = "transport_train_lightrail_subway";
    public static final String TRANSPORT_AIRPORT_FERRY_AERIALWAY = "transport_airport_ferry_aerialway";
    public static final String TRANSPORT_TAXI = "transport_taxi";
    public static final String FOOD = "food";
    public static final String TOURISM = "tourism";
    public static final String NATURE = "nature";
    public static final String SHOP = "shop";
    public static final String EDUCATION = "education";
    public static final String HEALTH = "health";
    public static final String ENTERTAINMENT = "entertainment";
    public static final String FINANCE = "finance";
    public static final String PUBLIC_SERVICE = "public_service";
    public static final String ALL_BUILDINGS_WITH_NAME = "all_buildings_with_name";
    public static final String ENTRANCE = "entrance";
    public static final String SURVEILLANCE = "surveillance";
    public static final String BRIDGE = "bridge";
    public static final String BENCH = "bench";
    public static final String TRASH = "trash";
    public static final String NAMED_INTERSECTION = "named_intersection";
    public static final String OTHER_INTERSECTION = "other_intersection";
    public static final String PEDESTRIAN_CROSSINGS = "pedestrian_crossings";


    // json

    public static ArrayList<PoiCategory> listFromJson(JSONArray jsonPoiCategoryIdList) {
        ArrayList<PoiCategory> poiCategoryList = new ArrayList<PoiCategory>();
        for (int i=0; i<jsonPoiCategoryIdList.length(); i++) {
            try {
                poiCategoryList.add(
                        new PoiCategory(
                            jsonPoiCategoryIdList.getString(i)));
            } catch (JSONException e) {}
        }
        return poiCategoryList;
    }

    public static JSONArray listToJson(ArrayList<PoiCategory> poiCategoryList) {
        JSONArray jsonPoiCategoryIdList = new JSONArray();
        for (PoiCategory category : poiCategoryList) {
            jsonPoiCategoryIdList.put(category.getId());
        }
        return jsonPoiCategoryIdList;
    }


    // constructor

    private String id;

    public PoiCategory(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        if (id.equals(TRANSPORT_BUS_TRAM)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryBusTram);
        } else if (id.equals(TRANSPORT_TRAIN_LIGHTRAIL_SUBWAY)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryTrain);
        } else if (id.equals(TRANSPORT_AIRPORT_FERRY_AERIALWAY)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryAirportFerry);
        } else if (id.equals(TRANSPORT_TAXI)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryTaxi);
        } else if (id.equals(FOOD)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryFood);
        } else if (id.equals(TOURISM)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryTourism);
        } else if (id.equals(NATURE)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryNature);
        } else if (id.equals(SHOP)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryShop);
        } else if (id.equals(EDUCATION)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryEducation);
        } else if (id.equals(HEALTH)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryHealth);
        } else if (id.equals(ENTERTAINMENT)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryEntertainment);
        } else if (id.equals(FINANCE)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryFinance);
        } else if (id.equals(PUBLIC_SERVICE)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryPublicService);
        } else if (id.equals(ALL_BUILDINGS_WITH_NAME)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryBuildingsWithName);
        } else if (id.equals(ENTRANCE)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryEntrance);
        } else if (id.equals(SURVEILLANCE)) {
            return GlobalInstance.getStringResource(R.string.poiCategorySurveillance);
        } else if (id.equals(BRIDGE)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryBridge);
        } else if (id.equals(BENCH)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryBench);
        } else if (id.equals(TRASH)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryTrash);
        } else if (id.equals(NAMED_INTERSECTION)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryNamedIntersection);
        } else if (id.equals(OTHER_INTERSECTION)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryOtherIntersection);
        } else if (id.equals(PEDESTRIAN_CROSSINGS)) {
            return GlobalInstance.getStringResource(R.string.poiCategoryPedestrianCrossings);
        } else {
            return id;
        }
    }

    @Override public String toString() {
        return this.getName();
    }

	@Override public int hashCode() {
        return this.id.hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof PoiCategory)) {
			return false;
        }
		PoiCategory other = (PoiCategory) obj;
        return this.id.equals(other.getId());
    }

}
