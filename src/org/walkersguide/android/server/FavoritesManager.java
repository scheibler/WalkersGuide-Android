package org.walkersguide.android.server;

import org.walkersguide.android.data.basic.point.PointWrapper;
import org.walkersguide.android.data.poi.FavoritesProfile;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.DownloadUtility;
import org.walkersguide.android.listener.FavoritesProfileListener;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

public class FavoritesManager {

    private Context context;
    private static FavoritesManager favoritesManagerInstance;
    private SettingsManager settingsManagerInstance;
    private RequestFavoritesProfile requestFavoritesProfile;

    public static FavoritesManager getInstance(Context context) {
        if(favoritesManagerInstance == null){
            favoritesManagerInstance = new FavoritesManager(context.getApplicationContext());
        }
        return favoritesManagerInstance;
    }

    private FavoritesManager(Context context) {
        this.context = context;
        this.settingsManagerInstance = SettingsManager.getInstance(context);
        this.requestFavoritesProfile = null;
        // listen for new locations
        LocalBroadcastManager.getInstance(context).registerReceiver(
                newLocationReceiver,
                new IntentFilter(Constants.ACTION_NEW_LOCATION));
    }

    public void requestFavoritesProfile(FavoritesProfileListener profileListener, int profileId) {
        if (requestInProgress()) {
            if (this.requestFavoritesProfile.getFavoritesProfileId() == profileId) {
                this.requestFavoritesProfile.updateListener(profileListener);
                return;
            } else {
                cancelRequest();
            }
        }
        this.requestFavoritesProfile = new RequestFavoritesProfile(profileListener, profileId);
        this.requestFavoritesProfile.execute();
    }

    public boolean requestInProgress() {
        if (this.requestFavoritesProfile != null
                && this.requestFavoritesProfile.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void cancelRequest() {
        if (requestInProgress()) {
            this.requestFavoritesProfile.cancel();
        }
    }

    public void invalidateRequest() {
        if (requestInProgress()) {
            this.requestFavoritesProfile.updateListener(null);
        }
    }

    private BroadcastReceiver newLocationReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                    && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD2.ID
                    && ! intent.getBooleanExtra(Constants.ACTION_NEW_LOCATION_ATTR.BOOL_AT_HIGH_SPEED, false)
                    && ! requestInProgress()) {
                System.out.println("xx favoritesManager: new location");
            }
        }
    };


    private class RequestFavoritesProfile extends AsyncTask<Void, Void, FavoritesProfile> {

        private FavoritesProfileListener favoritesProfileListener;
        private int favoritesProfileIdToRequest, returnCode;

        public RequestFavoritesProfile(FavoritesProfileListener profileListener, int favoritesProfileIdToRequest) {
            this.favoritesProfileListener = profileListener;
            this.favoritesProfileIdToRequest = favoritesProfileIdToRequest;
            this.returnCode = Constants.ID.OK;
        }

        @Override protected FavoritesProfile doInBackground(Void... params) {
            // load favorites profile
            AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(context);
            FavoritesProfile favoritesProfile = accessDatabaseInstance.getFavoritesProfile(this.favoritesProfileIdToRequest);
            System.out.println("xxx get fav profile " + favoritesProfileIdToRequest + ": " + favoritesProfile);
            if (favoritesProfile == null) {
                this.returnCode = 1001;
                return null;
            }

            // get current location
            PositionManager positionManagerInstance = PositionManager.getInstance(context);
            PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
            if (currentLocation == null) {
                this.returnCode = 1004;
                return null;
            }

            // get current direction
            DirectionManager directionManagerInstance = DirectionManager.getInstance(context);
            int currentDirection = directionManagerInstance.getCurrentDirection();
            if (currentDirection == -1) {
                this.returnCode = 1005;
                return null;
            }

            // update center and direction of profile and return
            favoritesProfile.setCenterAndDirection(currentLocation, currentDirection);
            return favoritesProfile;
        }

        @Override protected void onPostExecute(FavoritesProfile favoritesProfile) {
            System.out.println("xxx favManager: " + this.returnCode);
            if (this.favoritesProfileListener != null) {
                this.favoritesProfileListener.favoritesProfileRequestFinished(
                        returnCode,
                        DownloadUtility.getErrorMessageForReturnCode(context, this.returnCode, ""),
                        favoritesProfile);
            }
        }

        @Override protected void onCancelled(FavoritesProfile favoritesProfile) {
            System.out.println("xxx favManager: cancelled");
            if (this.favoritesProfileListener != null) {
                this.favoritesProfileListener.favoritesProfileRequestFinished(
                        Constants.ID.CANCELLED,
                        DownloadUtility.getErrorMessageForReturnCode(context, Constants.ID.CANCELLED, ""),
                        favoritesProfile);
            }
        }

        public int getFavoritesProfileId() {
            return this.favoritesProfileIdToRequest;
        }

        public void updateListener(FavoritesProfileListener profileListener) {
            if (profileListener != null) {
                this.favoritesProfileListener = profileListener;
            }
        }

        public void cancel() {
            this.cancel(true);
        }
    }

}
