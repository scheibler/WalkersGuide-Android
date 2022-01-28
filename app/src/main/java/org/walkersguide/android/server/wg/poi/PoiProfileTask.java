package org.walkersguide.android.server.wg.poi;

import org.walkersguide.android.database.profile.FavoritesProfile;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.server.wg.WgException;
import org.walkersguide.android.server.ServerUtility;
import org.walkersguide.android.server.wg.WgUtility;
import org.walkersguide.android.server.ServerTask;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.json.JSONObject;
import org.json.JSONException;
import timber.log.Timber;
import java.util.ArrayList;
import org.walkersguide.android.data.object_with_id.Point;
import org.json.JSONArray;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.util.GlobalInstance;
import java.util.Collections;


public class PoiProfileTask extends ServerTask {

    public enum RequestAction {
        UPDATE, MORE_RESULTS
    }

    private PoiProfileRequest request;
    private RequestAction action;
    private Point newCenter;

    public PoiProfileTask(PoiProfileRequest request, RequestAction action, Point newCenter) {
        this.request = request;
        this.action = action;
        this.newCenter = newCenter;
    }

    @Override public void execute() throws WgException {
        AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance();

        // does any poi profile exist?
        if (accessDatabaseInstance.getPoiProfileList().isEmpty()) {
            throw new WgException(WgException.RC_NO_POI_PROFILE_CREATED);
        }
        // does the selected poi profile exist?
        if (this.request.getProfile() == null) {
            throw new WgException(WgException.RC_NO_POI_PROFILE_SELECTED);
        }
        // check new center point
        if (this.newCenter == null) {
            throw new WgException(WgException.RC_BAD_REQUEST);
        }

        // initialize
        PoiProfileResult cachedResult = GlobalInstance
            .getInstance()
            .getCachedPoiProfileResult(this.request);
        boolean distanceToLastPOIProfileCenterPointTooLarge = 
            cachedResult == null
            || cachedResult.getCenter().distanceTo(newCenter) > (cachedResult.getLookupRadius() / 2);
        int initialRadius;
        if (this.request.getProfile().getPoiCategoryList().isEmpty()) {
            initialRadius = PoiProfileResult.INITIAL_LOCAL_FAVORITES_RADIUS;
        } else if (this.request.hasSearchTerm()) {
            initialRadius = PoiProfileResult.INITIAL_SEARCH_RADIUS;
        } else {
            initialRadius = PoiProfileResult.INITIAL_RADIUS;
        }

        // new values for radius and number of results
        int newRadius, newNumberOfResults;
        if (distanceToLastPOIProfileCenterPointTooLarge) {
            // new location
            // (re)start with initial values
            newRadius = initialRadius;
            newNumberOfResults = PoiProfileResult.INITIAL_NUMBER_OF_RESULTS;
        } else {
            // more or less the same position again
            // note: "cachedResult" can't be null in this "else" condition
            //       see creation of distanceToLastPOIProfileCenterPointTooLarge for details
            newRadius = cachedResult.getRadius();
            newNumberOfResults = cachedResult.getNumberOfResults();
            // increase radius, numberOfResults or both
            if (this.action == RequestAction.MORE_RESULTS) {
                //          num_low num_hgh
                //  rad_low r++,--- ---,n++
                //  rad_hgh r++,--- r++,n++
                if (((float) cachedResult.getLookupNumberOfResults() / cachedResult.getNumberOfResults()) > 0.66) {
                    // num_hgh: increase number of results
                    newNumberOfResults += PoiProfileResult.INITIAL_NUMBER_OF_RESULTS;
                    if (((float) cachedResult.getLookupRadius() / cachedResult.getRadius()) > 0.33) {
                        // rad_hgh: increase radius
                        newRadius += initialRadius;
                    }
                } else {
                    // num_low: increase radius
                    newRadius += initialRadius;
                }
            }
        }

        ArrayList<Point> newOnlyPoiList = new ArrayList<Point>();
        if (cachedResult != null
                && ! distanceToLastPOIProfileCenterPointTooLarge
                && this.action == RequestAction.UPDATE) {
            // server poi from cache
            newOnlyPoiList = cachedResult.getOnlyPoiList();

        } else if (! this.request.getProfile().getPoiCategoryList().isEmpty()) {

            // create server param list
            JSONObject jsonServerParams = null;
            try {
                jsonServerParams = WgUtility.createServerParamList();
                jsonServerParams.put("lat", newCenter.getLatitude());
                jsonServerParams.put("lon", newCenter.getLongitude());
                jsonServerParams.put("radius", newRadius);
                jsonServerParams.put("number_of_results", newNumberOfResults);
                jsonServerParams.put("tags", PoiCategory.listToJson(this.request.getProfile().getPoiCategoryList()));
                // optional params
                if (request.hasSearchTerm()) {
                    jsonServerParams.put("search", request.getSearchTerm());
                }
            } catch (JSONException e) {
                throw new WgException(WgException.RC_BAD_REQUEST);
            }

            ServerInstance serverInstance = WgUtility.getServerInstanceForServerUrlFromSettings();
            JSONObject jsonServerResponse = ServerUtility.performRequestAndReturnJsonObject(
                    String.format(
                        "%1$s/get_poi", serverInstance.getServerURL()),
                    jsonServerParams,
                    WgException.class);

            // parse points
            JSONArray jsonPointList = null;
            try {
                jsonPointList = jsonServerResponse.getJSONArray("poi");
            } catch (JSONException e) {
                throw new WgException(WgException.RC_BAD_RESPONSE);
            }
            for (int i=0; i<jsonPointList.length(); i++) {
                try {
                    newOnlyPoiList.add(
                            Point.create(jsonPointList.getJSONObject(i)));
                } catch (JSONException e) {
                    Timber.e("server point profile request: point parsing error: %1$s", e.getMessage());
                }
            }
        }

        // include points from favorite ids
        ArrayList<Point> newAllPointList = new ArrayList<Point>(newOnlyPoiList);
        if (this.request.getProfile().getIncludeFavorites()) {
            int newAllPointListLookupRadius = PoiProfileResult.calculateLookupRadius(
                    newAllPointList, newCenter, newRadius, newNumberOfResults);
            DatabaseProfileRequest databaseProfileRequest = new DatabaseProfileRequest(
                    FavoritesProfile.favoritePoints(), request.getSearchTerm(), SortMethod.DISTANCE_ASC);
            for (ObjectWithId objectWithId : accessDatabaseInstance.getObjectListFor(databaseProfileRequest)) {
                if (objectWithId instanceof Point) {
                    Point favorite = (Point) objectWithId;
                    if (newCenter.distanceTo(favorite) > newAllPointListLookupRadius) {
                        // favorites sorted by distance (ascending), therefore "break" instead of "continue"
                        break;
                    }
                    // add if not already present
                    if (! newAllPointList.contains(favorite)) {
                        newAllPointList.add(favorite);
                    }
                }
            }
        }

        // reset list position
        boolean newResetListPosition = this.action == RequestAction.UPDATE
            && (
                       cachedResult == null
                    || cachedResult.getCenter().distanceTo(newCenter) > 100);

        Collections.sort(
                newAllPointList,
                new ObjectWithId.SortByDistanceRelativeToCurrentLocation(true));
        PoiProfileResult newResult = new PoiProfileResult(newRadius, newNumberOfResults,
                newCenter, newAllPointList, newOnlyPoiList, newResetListPosition);

        if (! isCancelled()) {
            // add to cache
            GlobalInstance
                .getInstance()
                .cachePoiProfileResult(this.request, newResult);

            ServerTaskExecutor.sendPoiProfileTaskSuccessfulBroadcast(getId(), newResult);
        }
    }

}
