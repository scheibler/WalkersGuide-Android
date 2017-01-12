package org.walkersguide.android.poi;

import org.walkersguide.android.R;

import android.content.Context;

public class POICategory {

    private int id;
    private String name, tag;

    public POICategory(Context context, int id, String tag) {
        this.id = id;
        this.tag = tag;
        // determine name from tag
        if (tag.equals("transport_bus_tram")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryBusTram);
        } else if (tag.equals("transport_train_lightrail_subway")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryTrain);
        } else if (tag.equals("transport_airport_ferry_aerialway")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryAirportFerry);
        } else if (tag.equals("transport_taxi")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryTaxi);
        } else if (tag.equals("food")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryFood);
        } else if (tag.equals("tourism")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryTourism);
        } else if (tag.equals("nature")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryNature);
        } else if (tag.equals("shop")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryShop);
        } else if (tag.equals("education")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryEducation);
        } else if (tag.equals("health")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryHealth);
        } else if (tag.equals("entertainment")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryEntertainment);
        } else if (tag.equals("finance")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryFinance);
        } else if (tag.equals("public_service")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryPublicService);
        } else if (tag.equals("all_buildings_with_name")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryBuildingsWithName);
        } else if (tag.equals("surveillance")) {
            this.name = context.getResources().getString(R.string.labelPOICategorySurveillance);
        } else if (tag.equals("bridge")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryBridge);
        } else if (tag.equals("bench")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryBench);
        } else if (tag.equals("trash")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryTrash);
        } else if (tag.equals("named_intersection")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryNamedIntersection);
        } else if (tag.equals("other_intersection")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryOtherIntersection);
        } else if (tag.equals("pedestrian_crossings")) {
            this.name = context.getResources().getString(R.string.labelPOICategoryPedestrianCrossings);
        } else {
            this.name = tag;
        }
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getTag() {
        return this.tag;
    }

    @Override public String toString() {
        return this.tag;
    }

	@Override public int hashCode() {
        return this.id;
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
        return this.id == other.getId();
    }

}
