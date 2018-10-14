package org.walkersguide.android.data.profile;

import android.content.Context;

import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;

import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;


public class HistoryPointProfile extends PointProfile {

    // ids and map
    public static final int ID_ALL_POINTS = -100;
    public static final int ID_ADDRESS_POINTS = -110;
    public static final int ID_ROUTE_POINTS = -120;
    public static final int ID_SIMULATED_POINTS = -130;
    public static final int ID_USER_CREATED_POINTS = -140;

    public static TreeMap<Integer,String> getProfileMap(Context context) {
        TreeMap<Integer,String> profileMap = new TreeMap<Integer,String>();
        profileMap.put(
                ID_ALL_POINTS, context.getResources().getString(R.string.fpNameAllPoints));
        profileMap.put(
                ID_ADDRESS_POINTS, context.getResources().getString(R.string.fpNameAddressPoints));
        profileMap.put(
                ID_ROUTE_POINTS, context.getResources().getString(R.string.fpNameRoutePoints));
        profileMap.put(
                ID_SIMULATED_POINTS, context.getResources().getString(R.string.fpNameSimulatedPoints));
        profileMap.put(
                ID_USER_CREATED_POINTS, context.getResources().getString(R.string.fpNameUserCreatedPoints));
        return profileMap;
    }


    public HistoryPointProfile(Context context, int id, JSONArray jsonPointList) throws JSONException {
        super(
                context,
                id,
                getProfileMap(context).get(id),
                PositionManager.getDummyLocation(context).toJson(),
                Constants.DUMMY.DIRECTION,
                jsonPointList);
    }

    public int getSortCriteria() {
        return Constants.SORT_CRITERIA.ORDER_DESC;
    }

}
