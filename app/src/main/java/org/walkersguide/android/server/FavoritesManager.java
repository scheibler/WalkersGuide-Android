package org.walkersguide.android.server;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.FavoritesProfile;
import org.walkersguide.android.data.profile.SearchFavoritesProfile;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.DownloadUtility;
import org.walkersguide.android.listener.FavoritesProfileListener;
import org.walkersguide.android.listener.SearchFavoritesProfileListener;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import java.util.ArrayList;
import android.text.TextUtils;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.json.JSONException;

public class FavoritesManager {

    private Context context;
    private static FavoritesManager favoritesManagerInstance;

    public static FavoritesManager getInstance(Context context) {
        if(favoritesManagerInstance == null){
            favoritesManagerInstance = new FavoritesManager(context.getApplicationContext());
        }
        return favoritesManagerInstance;
    }

    private FavoritesManager(Context context) {
        this.context = context;
        this.requestFavoritesProfile = null;
        this.requestFavoritesSearch = null;
    }


    /**
     * favorites  profiles
     */
    private RequestFavoritesProfile requestFavoritesProfile;

    public void requestFavoritesProfile(FavoritesProfileListener profileListener, int profileId) {
        if (favoritesProfileRequestInProgress()) {
            if (profileListener == null) {
                return;
            } else if (this.requestFavoritesProfile.getFavoritesProfileId() == profileId) {
                this.requestFavoritesProfile.addListener(profileListener);
                return;
            } else {
                cancelFavoritesProfileRequest();
            }
        }
        this.requestFavoritesProfile = new RequestFavoritesProfile(profileListener, profileId);
        this.requestFavoritesProfile.execute();
    }

    public void invalidateFavoritesProfileRequest(FavoritesProfileListener profileListener) {
        if (favoritesProfileRequestInProgress()) {
            this.requestFavoritesProfile.removeListener(profileListener);
        }
    }

    public boolean favoritesProfileRequestInProgress() {
        if (this.requestFavoritesProfile != null
                && this.requestFavoritesProfile.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void cancelFavoritesProfileRequest() {
        if (favoritesProfileRequestInProgress()) {
            this.requestFavoritesProfile.cancel();
        }
    }


    private class RequestFavoritesProfile extends AsyncTask<Void, Void, FavoritesProfile> {

        private ArrayList<FavoritesProfileListener> favoritesProfileListenerList;
        private int favoritesProfileIdToRequest, returnCode;
        private boolean resetListPosition;

        public RequestFavoritesProfile(FavoritesProfileListener profileListener, int favoritesProfileIdToRequest) {
            this.favoritesProfileListenerList = new ArrayList<FavoritesProfileListener>();
            if (profileListener != null) {
                this.favoritesProfileListenerList.add(profileListener);
            }
            this.favoritesProfileIdToRequest = favoritesProfileIdToRequest;
            this.returnCode = Constants.ID.OK;
            this.resetListPosition = false;
        }

        @Override protected FavoritesProfile doInBackground(Void... params) {
            // load favorites profile
            AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(context);
            FavoritesProfile favoritesProfile = accessDatabaseInstance.getFavoritesProfile(this.favoritesProfileIdToRequest);
            if (favoritesProfile == null) {
                this.returnCode = 1032;
                return null;
            }

            // get current location
            PositionManager positionManagerInstance = PositionManager.getInstance(context);
            PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
            if (currentLocation.equals(PositionManager.getDummyLocation(context))) {
                this.returnCode = 1004;
                return null;
            }

            // get current direction
            DirectionManager directionManagerInstance = DirectionManager.getInstance(context);
            int currentDirection = directionManagerInstance.getCurrentDirection();
            if (currentDirection == Constants.DUMMY.DIRECTION) {
                this.returnCode = 1005;
                return null;
            }

            if (currentLocation.distanceTo(favoritesProfile.getCenter()) > PositionManager.THRESHOLD3.DISTANCE) {
                this.resetListPosition = true;
            }

            // update center and direction of profile and return
            favoritesProfile.setCenterAndDirection(currentLocation, currentDirection);
            return favoritesProfile;
        }

        @Override protected void onPostExecute(FavoritesProfile favoritesProfile) {
            System.out.println("xxx favManager: " + this.returnCode + "     resetListPosition: " + resetListPosition + "    id: "  + this.favoritesProfileIdToRequest);
            for (FavoritesProfileListener favoritesProfileListener : this.favoritesProfileListenerList) {
                favoritesProfileListener.favoritesProfileRequestFinished(
                        this.returnCode,
                        DownloadUtility.getErrorMessageForReturnCode(context, this.returnCode, ""),
                        favoritesProfile,
                        this.resetListPosition);
            }
        }

        @Override protected void onCancelled(FavoritesProfile favoritesProfile) {
            for (FavoritesProfileListener favoritesProfileListener : this.favoritesProfileListenerList) {
                favoritesProfileListener.favoritesProfileRequestFinished(
                        Constants.ID.CANCELLED,
                        DownloadUtility.getErrorMessageForReturnCode(context, Constants.ID.CANCELLED, ""),
                        favoritesProfile,
                        false);
            }
        }

        public int getFavoritesProfileId() {
            return this.favoritesProfileIdToRequest;
        }

        public void addListener(FavoritesProfileListener newFavoritesProfileListener) {
            if (newFavoritesProfileListener != null
                    && ! this.favoritesProfileListenerList.contains(newFavoritesProfileListener)) {
                this.favoritesProfileListenerList.add(newFavoritesProfileListener);
            }
        }

        public void removeListener(FavoritesProfileListener newFavoritesProfileListener) {
            if (newFavoritesProfileListener != null
                    && this.favoritesProfileListenerList.contains(newFavoritesProfileListener)) {
                this.favoritesProfileListenerList.remove(newFavoritesProfileListener);
            }
        }

        public void cancel() {
            this.cancel(true);
        }
    }


    /**
     * search in favorites
     */
    private RequestFavoritesSearch requestFavoritesSearch;
    private SearchFavoritesProfile lastSearchFavoritesProfile;

    public void requestFavoritesSearch(SearchFavoritesProfileListener profileListener,
            String searchTerm, ArrayList<Integer> favoritesProfileIdList, int sortCriteria) {
        if (searchRequestInProgress()) {
            if (profileListener == null) {
                return;
            } else if (this.requestFavoritesSearch.getSearchTerm().equals(searchTerm)
                    && this.requestFavoritesSearch.getFavoritesProfileIdList().size() == favoritesProfileIdList.size()
                    && this.requestFavoritesSearch.getFavoritesProfileIdList().containsAll(favoritesProfileIdList)
                    && favoritesProfileIdList.containsAll(this.requestFavoritesSearch.getFavoritesProfileIdList())
                    && this.requestFavoritesSearch.getSortCriteria() == sortCriteria) {
                this.requestFavoritesSearch.addListener(profileListener);
                return;
            } else {
                cancelSearchRequest();
            }
        }
        this.requestFavoritesSearch = new RequestFavoritesSearch(
                profileListener, searchTerm, favoritesProfileIdList, sortCriteria);
        this.requestFavoritesSearch.execute();
    }

    public void invalidateFavoritesSearchRequest(SearchFavoritesProfileListener profileListener) {
        if (searchRequestInProgress()) {
            this.requestFavoritesSearch.removeListener(profileListener);
        }
    }

    public boolean searchRequestInProgress() {
        if (this.requestFavoritesSearch != null
                && this.requestFavoritesSearch.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void cancelSearchRequest() {
        if (searchRequestInProgress()) {
            this.requestFavoritesSearch.cancel();
        }
        this.requestFavoritesSearch = null;
    }


    private class RequestFavoritesSearch extends AsyncTask<Void, Void, SearchFavoritesProfile> {

        private ArrayList<SearchFavoritesProfileListener> searchFavoritesProfileListenerList;
        private int returnCode;
        private String searchTerm;
        private ArrayList<Integer> favoritesProfileIdList;
        private int sortCriteria;
        private boolean resetListPosition;

        public RequestFavoritesSearch(SearchFavoritesProfileListener profileListener, String searchTerm,
                ArrayList<Integer> favoritesProfileIdList, int sortCriteria) {
            this.searchFavoritesProfileListenerList = new ArrayList<SearchFavoritesProfileListener>();
            if (profileListener != null) {
                this.searchFavoritesProfileListenerList.add(profileListener);
            }
            this.returnCode = Constants.ID.OK;
            this.searchTerm = searchTerm;
            this.favoritesProfileIdList = favoritesProfileIdList;
            this.sortCriteria = sortCriteria;
            this.resetListPosition = false;
        }

        @Override protected SearchFavoritesProfile doInBackground(Void... params) {
            SearchFavoritesProfile searchFavoritesProfile = null;
            try {
                searchFavoritesProfile = new SearchFavoritesProfile(
                        context, this.searchTerm, this.favoritesProfileIdList, this.sortCriteria);
            } catch (JSONException e) {
                searchFavoritesProfile = null;
            } finally {
                if (searchFavoritesProfile == null) {
                    this.returnCode = 1032;
                    return null;
                }
            }

            // get current location
            PositionManager positionManagerInstance = PositionManager.getInstance(context);
            PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
            if (currentLocation.equals(PositionManager.getDummyLocation(context))) {
                this.returnCode = 1004;
                return null;
            }

            // get current direction
            DirectionManager directionManagerInstance = DirectionManager.getInstance(context);
            int currentDirection = directionManagerInstance.getCurrentDirection();
            if (currentDirection == Constants.DUMMY.DIRECTION) {
                this.returnCode = 1005;
                return null;
            }

            // no search term
            if (TextUtils.isEmpty(this.searchTerm)) {
                this.returnCode = 1030;
                return null;
            }

            // no favorites profiles to search in
            if (this.favoritesProfileIdList == null
                    || favoritesProfileIdList.isEmpty()) {
                this.returnCode = 1031;
                return null;
            }

            ArrayList<PointProfileObject> foundFavoritesList;
            if (lastSearchFavoritesProfile != null
                    && lastSearchFavoritesProfile.getSearchTerm().equals(this.searchTerm)
                    && lastSearchFavoritesProfile.getFavoritesProfileIdList().size() == this.favoritesProfileIdList.size()
                    && lastSearchFavoritesProfile.getFavoritesProfileIdList().containsAll(this.favoritesProfileIdList)
                    && favoritesProfileIdList.containsAll(lastSearchFavoritesProfile.getFavoritesProfileIdList())) {
                // load data from cache
                foundFavoritesList = lastSearchFavoritesProfile.getPointProfileObjectList();
                if (currentLocation.distanceTo(lastSearchFavoritesProfile.getCenter()) > PositionManager.THRESHOLD3.DISTANCE) {
                    this.resetListPosition = true;
                }

            } else {
                this.resetListPosition = true;
                // load all favorites from database
                foundFavoritesList = new ArrayList<PointProfileObject>();
                for (Integer favoritesProfileId : this.favoritesProfileIdList) {
                    FavoritesProfile favoritesProfile = AccessDatabase.getInstance(context).getFavoritesProfile(favoritesProfileId);
                    for (PointProfileObject favorite : favoritesProfile.getPointProfileObjectList()) {
                        if (! foundFavoritesList.contains(favorite)
                                && favorite.toString().toLowerCase().contains(this.searchTerm.toLowerCase())) {
                            foundFavoritesList.add(favorite);
                        }
                    }
                }
            }

            ArrayList<PointProfileObject> updatedFoundFavoritesList = new ArrayList<PointProfileObject>();
            for (PointProfileObject favorite : foundFavoritesList) {
                try {
                    updatedFoundFavoritesList.add(
                            new PointProfileObject(context, searchFavoritesProfile, favorite.toJson()));
                } catch (JSONException e) {}
            }
            searchFavoritesProfile.setPointProfileObjectList(updatedFoundFavoritesList);

            // update center and direction of profile and return
            searchFavoritesProfile.setCenterAndDirection(currentLocation, currentDirection);
            return searchFavoritesProfile;
        }

        @Override protected void onPostExecute(SearchFavoritesProfile searchFavoritesProfile) {
            System.out.println("xxx favManager search: " + this.returnCode + "   reset: " + resetListPosition);
            if (this.returnCode == Constants.ID.OK) {
                lastSearchFavoritesProfile = searchFavoritesProfile;
            }
            for (SearchFavoritesProfileListener searchFavoritesProfileListener : this.searchFavoritesProfileListenerList) {
                searchFavoritesProfileListener.searchFavoritesProfileRequestFinished(
                        this.returnCode,
                        DownloadUtility.getErrorMessageForReturnCode(context, this.returnCode, ""),
                        searchFavoritesProfile,
                        this.resetListPosition);
            }
        }

        @Override protected void onCancelled(SearchFavoritesProfile searchFavoritesProfile) {
            System.out.println("xxx favManager search: cancelled");
            for (SearchFavoritesProfileListener searchFavoritesProfileListener : this.searchFavoritesProfileListenerList) {
                searchFavoritesProfileListener.searchFavoritesProfileRequestFinished(
                        Constants.ID.CANCELLED,
                        DownloadUtility.getErrorMessageForReturnCode(context, Constants.ID.CANCELLED, ""),
                        searchFavoritesProfile,
                        false);
            }
        }


        public String getSearchTerm() {
            return this.searchTerm;
        }

        public ArrayList<Integer> getFavoritesProfileIdList() {
            return this.favoritesProfileIdList;
        }

        public int getSortCriteria() {
            return this.sortCriteria;
        }

        public void addListener(SearchFavoritesProfileListener newSearchFavoritesProfileListener) {
            if (newSearchFavoritesProfileListener != null
                    && ! this.searchFavoritesProfileListenerList.contains(newSearchFavoritesProfileListener)) {
                this.searchFavoritesProfileListenerList.add(newSearchFavoritesProfileListener);
            }
        }

        public void removeListener(SearchFavoritesProfileListener newSearchFavoritesProfileListener) {
            if (newSearchFavoritesProfileListener != null
                    && this.searchFavoritesProfileListenerList.contains(newSearchFavoritesProfileListener)) {
                this.searchFavoritesProfileListenerList.remove(newSearchFavoritesProfileListener);
            }
        }

        public void cancel() {
            this.cancel(true);
        }
    }

}
