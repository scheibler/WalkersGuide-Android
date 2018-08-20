package org.walkersguide.android.data.poi;

import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;

import android.content.Context;


public class POICategory {

    private String id, name;

    public POICategory(Context context, String id) {
        this.id = id;
        // determine name from id
        if (id.equals(Constants.POI_CATEGORY.TRANSPORT_BUS_TRAM)) {
            this.name = context.getResources().getString(R.string.poiCategoryBusTram);
        } else if (id.equals(Constants.POI_CATEGORY.TRANSPORT_TRAIN_LIGHTRAIL_SUBWAY)) {
            this.name = context.getResources().getString(R.string.poiCategoryTrain);
        } else if (id.equals(Constants.POI_CATEGORY.TRANSPORT_AIRPORT_FERRY_AERIALWAY)) {
            this.name = context.getResources().getString(R.string.poiCategoryAirportFerry);
        } else if (id.equals(Constants.POI_CATEGORY.TRANSPORT_TAXI)) {
            this.name = context.getResources().getString(R.string.poiCategoryTaxi);
        } else if (id.equals(Constants.POI_CATEGORY.FOOD)) {
            this.name = context.getResources().getString(R.string.poiCategoryFood);
        } else if (id.equals(Constants.POI_CATEGORY.TOURISM)) {
            this.name = context.getResources().getString(R.string.poiCategoryTourism);
        } else if (id.equals(Constants.POI_CATEGORY.NATURE)) {
            this.name = context.getResources().getString(R.string.poiCategoryNature);
        } else if (id.equals(Constants.POI_CATEGORY.SHOP)) {
            this.name = context.getResources().getString(R.string.poiCategoryShop);
        } else if (id.equals(Constants.POI_CATEGORY.EDUCATION)) {
            this.name = context.getResources().getString(R.string.poiCategoryEducation);
        } else if (id.equals(Constants.POI_CATEGORY.HEALTH)) {
            this.name = context.getResources().getString(R.string.poiCategoryHealth);
        } else if (id.equals(Constants.POI_CATEGORY.ENTERTAINMENT)) {
            this.name = context.getResources().getString(R.string.poiCategoryEntertainment);
        } else if (id.equals(Constants.POI_CATEGORY.FINANCE)) {
            this.name = context.getResources().getString(R.string.poiCategoryFinance);
        } else if (id.equals(Constants.POI_CATEGORY.PUBLIC_SERVICE)) {
            this.name = context.getResources().getString(R.string.poiCategoryPublicService);
        } else if (id.equals(Constants.POI_CATEGORY.ALL_BUILDINGS_WITH_NAME)) {
            this.name = context.getResources().getString(R.string.poiCategoryBuildingsWithName);
        } else if (id.equals(Constants.POI_CATEGORY.ENTRANCE)) {
            this.name = context.getResources().getString(R.string.poiCategoryEntrance);
        } else if (id.equals(Constants.POI_CATEGORY.SURVEILLANCE)) {
            this.name = context.getResources().getString(R.string.poiCategorySurveillance);
        } else if (id.equals(Constants.POI_CATEGORY.BRIDGE)) {
            this.name = context.getResources().getString(R.string.poiCategoryBridge);
        } else if (id.equals(Constants.POI_CATEGORY.BENCH)) {
            this.name = context.getResources().getString(R.string.poiCategoryBench);
        } else if (id.equals(Constants.POI_CATEGORY.TRASH)) {
            this.name = context.getResources().getString(R.string.poiCategoryTrash);
        } else if (id.equals(Constants.POI_CATEGORY.NAMED_INTERSECTION)) {
            this.name = context.getResources().getString(R.string.poiCategoryNamedIntersection);
        } else if (id.equals(Constants.POI_CATEGORY.OTHER_INTERSECTION)) {
            this.name = context.getResources().getString(R.string.poiCategoryOtherIntersection);
        } else if (id.equals(Constants.POI_CATEGORY.PEDESTRIAN_CROSSINGS)) {
            this.name = context.getResources().getString(R.string.poiCategoryPedestrianCrossings);
        } else {
            this.name = id;
        }
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    @Override public String toString() {
        return this.name;
    }

	@Override public int hashCode() {
        return this.id.hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof POICategory)) {
			return false;
        }
		POICategory other = (POICategory) obj;
        return this.id.equals(other.getId());
    }

}
