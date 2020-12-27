package org.walkersguide.android.pt;

import java.util.EnumSet;

import de.schildbach.pte.AbstractNetworkProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.SuggestedLocation;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;

import android.content.Context;

import android.os.AsyncTask;

import java.io.IOException;

import java.util.ArrayList;



import timber.log.Timber;
import android.text.TextUtils;
import org.walkersguide.android.helper.ServerUtility;


public class StationManager {

    public interface StationListener{
        public void stationRequestTaskSuccessful(Point position, ArrayList<Location> stationList);
        public void stationRequestTaskFailed(int returnCode);
    }


    private Context context;
    private static StationManager stationManagerInstance;
    private StationRequestTask stationRequestTask;

    public static StationManager getInstance(Context context) {
        if(stationManagerInstance == null){
            stationManagerInstance = new StationManager(context.getApplicationContext());
        }
        return stationManagerInstance;
    }

    private StationManager(Context context) {
        this.context = context;
        this.stationRequestTask = null;
    }

    public void requestStationList(StationListener listener,
            AbstractNetworkProvider provider, Point position, String searchTerm) {
        if (stationRequestTaskInProgress()) {
            if (listener == null) {
                return;
            } else if (this.stationRequestTask.isSameRequest(provider, position, searchTerm)) {
                this.stationRequestTask.addListener(listener);
                return;
            } else {
                cancelStationRequestTask();
            }
        }
        // new request
        this.stationRequestTask = new StationRequestTask(
                listener, provider, position, searchTerm);
        this.stationRequestTask.execute();
    }

    public void invalidateStationRequestTask(StationListener listener) {
        if (stationRequestTaskInProgress()) {
            this.stationRequestTask.removeListener(listener);
        }
    }

    public void cancelStationRequestTask() {
        if (stationRequestTaskInProgress()) {
            this.stationRequestTask.cancel();
        }
    }

    public boolean stationRequestTaskInProgress() {
        if (this.stationRequestTask != null
                && this.stationRequestTask.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }


    private class StationRequestTask extends AsyncTask<Void, Void, ArrayList<Location>> {

        private int returnCode;
        private ArrayList<StationListener> stationListenerList;

        private AbstractNetworkProvider provider;
        private Point position;
        private String searchTerm;

        public StationRequestTask(StationListener listener,
                AbstractNetworkProvider provider, Point position, String searchTerm) {
            this.returnCode = PTHelper.RC_OK;
            this.stationListenerList = new ArrayList<StationListener>();
            if (listener != null) {
                this.stationListenerList.add(listener);
            }
            this.provider = provider;
            if (this.provider != null) {
                this.provider.setUserAgent(PTHelper.USER_AGENT);
            }
            this.position = position;
            this.searchTerm = searchTerm;
        }

        @Override protected ArrayList<Location> doInBackground(Void... params) {
            ArrayList<Location> stationList = new ArrayList<Location>();

            if (this.provider == null) {
                this.returnCode = PTHelper.RC_NO_NETWORK_PROVIDER;
            } else if (this.position == null) {
                this.returnCode = PTHelper.RC_NO_COORDINATES;
            } else if (! ServerUtility.isInternetAvailable(context)) {
                this.returnCode = PTHelper.RC_NO_INTERNET_CONNECTION;

            } else if (! TextUtils.isEmpty(this.searchTerm)) {
                SuggestLocationsResult searchResult = null;
                try {
                    searchResult = this.provider.suggestLocations(
                            this.searchTerm, EnumSet.of(LocationType.STATION), 0);
                } catch (IOException e) {
                    searchResult = null;
                } finally {
                    if (searchResult == null) {
                        this.returnCode = PTHelper.RC_REQUEST_FAILED;
                    } else if (searchResult.status == SuggestLocationsResult.Status.OK) {
                        for (SuggestedLocation suggestedLocation : searchResult.suggestedLocations) {
                            if (! stationList.contains(suggestedLocation.location)) {
                                stationList.add(suggestedLocation.location);
                            }
                        }
                    } else if (searchResult.status == SuggestLocationsResult.Status.SERVICE_DOWN) {
                        this.returnCode = PTHelper.RC_SERVICE_DOWN;
                    } else {
                        this.returnCode = PTHelper.RC_UNKNOWN_SERVER_RESPONSE;
                    }
                }

            } else {
                NearbyLocationsResult nearbyResult = null;
                try {
                    nearbyResult = this.provider.queryNearbyLocations(
                            EnumSet.of(LocationType.STATION),
                            new Location(LocationType.COORD, null, this.position),
                            0, 0);
                } catch (IOException e) {
                    Timber.e("e: %1$s", e.getMessage());
                    nearbyResult = null;
                } finally {
                    if (nearbyResult == null) {
                        this.returnCode = PTHelper.RC_REQUEST_FAILED;
                    } else if (nearbyResult.status == NearbyLocationsResult.Status.OK) {
                        stationList = new ArrayList<Location>();
                        for (Location location : nearbyResult.locations) {
                            if (! stationList.contains(location)) {
                                stationList.add(location);
                            }
                        }
                    } else if (nearbyResult.status == NearbyLocationsResult.Status.SERVICE_DOWN) {
                        this.returnCode = PTHelper.RC_SERVICE_DOWN;
                    } else {
                        this.returnCode = PTHelper.RC_UNKNOWN_SERVER_RESPONSE;
                    }
                }
            }
            Timber.d("rc=%1$d", this.returnCode);

            return stationList;
        }

        @Override protected void onPostExecute(ArrayList<Location> stationList) {
            for (StationListener listener : this.stationListenerList) {
                if (returnCode == PTHelper.RC_OK) {
                    listener.stationRequestTaskSuccessful(this.position, stationList);
                } else {
                    listener.stationRequestTaskFailed(this.returnCode);
                }
            }
        }

        @Override protected void onCancelled(ArrayList<Location> stationList) {
            for (StationListener listener : this.stationListenerList) {
                listener.stationRequestTaskFailed(PTHelper.RC_CANCELLED);
            }
        }

        public void cancel() {
            this.cancel(true);
        }

        public void addListener(StationListener newListener) {
            if (newListener != null
                    && ! this.stationListenerList.contains(newListener)) {
                this.stationListenerList.add(newListener);
            }
        }

        public void removeListener(StationListener listenerToRemove) {
            if (listenerToRemove != null
                    && this.stationListenerList.contains(listenerToRemove)) {
                this.stationListenerList.remove(listenerToRemove);
            }
        }

        public boolean isSameRequest(AbstractNetworkProvider newProvider, Point newPosition, String newSearchTerm) {
            if (this.isSameProvider(newProvider) && this.isSameSearchTerm(newSearchTerm)
                    && PTHelper.distanceBetweenTwoPoints(this.position, newPosition) < 100) {
                return true;
            }
            return false;
        }

        private boolean isSameProvider(AbstractNetworkProvider newProvider) {
            if (this.provider == null) {
                return newProvider == null;
            }
            return this.provider.equals(newProvider);
        }

        private boolean isSameSearchTerm(String newSearchTerm) {
            if (this.searchTerm == null) {
                return newSearchTerm == null;
            }
            return this.searchTerm.equals(newSearchTerm);
        }
    }

}
