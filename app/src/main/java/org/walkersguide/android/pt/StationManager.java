package org.walkersguide.android.pt;

import java.util.EnumSet;

import de.schildbach.pte.AbstractNetworkProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.SuggestedLocation;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;


import android.os.AsyncTask;

import java.io.IOException;

import java.util.ArrayList;



import timber.log.Timber;
import android.text.TextUtils;
import org.walkersguide.android.server.util.ServerUtility;
import de.schildbach.pte.NetworkId;


public class StationManager {

    public interface StationListener{
        public void stationRequestTaskSuccessful(Point position, ArrayList<Location> stationList);
        public void stationRequestTaskFailed(int returnCode);
    }


    private static StationManager managerInstance;
    private StationRequestTask stationRequestTask;

    public static StationManager getInstance() {
        if (managerInstance == null){
            managerInstance = getInstanceSynchronized();
        }
        return managerInstance;
    }

    private static synchronized StationManager getInstanceSynchronized() {
        if (managerInstance == null){
            managerInstance = new StationManager();
        }
        return managerInstance;
    }

    private StationManager() {
        this.stationRequestTask = null;
    }


    public void requestStationList(StationListener listener,
            NetworkId networkId, Point position, String searchTerm) {
        if (stationRequestTaskInProgress()) {
            if (listener == null) {
                return;
            } else if (this.stationRequestTask.isSameRequest(networkId, position, searchTerm)) {
                this.stationRequestTask.addListener(listener);
                return;
            } else {
                cancelStationRequestTask();
            }
        }
        // new request
        this.stationRequestTask = new StationRequestTask(
                listener, networkId, position, searchTerm);
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


    private static class StationRequestTask extends AsyncTask<Void, Void, ArrayList<Location>> {

        private int returnCode;
        private ArrayList<StationListener> stationListenerList;

        private NetworkId networkId;
        private Point position;
        private String searchTerm;

        public StationRequestTask(StationListener listener,
                NetworkId networkId, Point position, String searchTerm) {
            this.returnCode = PTHelper.RC_OK;
            this.stationListenerList = new ArrayList<StationListener>();
            if (listener != null) {
                this.stationListenerList.add(listener);
            }
            this.networkId = networkId;
            this.position = position;
            this.searchTerm = searchTerm;
        }

        @Override protected ArrayList<Location> doInBackground(Void... params) {
            ArrayList<Location> stationList = new ArrayList<Location>();

            AbstractNetworkProvider provider = PTHelper.findNetworkProvider(this.networkId);
            if (provider == null) {
                this.returnCode = PTHelper.RC_NO_NETWORK_PROVIDER;
            } else if (this.position == null) {
                this.returnCode = PTHelper.RC_NO_COORDINATES;
            } else if (! ServerUtility.isInternetAvailable()) {
                this.returnCode = PTHelper.RC_NO_INTERNET_CONNECTION;

            } else if (! TextUtils.isEmpty(this.searchTerm)) {
                SuggestLocationsResult searchResult = null;
                try {
                    searchResult = provider.suggestLocations(
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
                    nearbyResult = provider.queryNearbyLocations(
                            EnumSet.of(LocationType.STATION),
                            new Location(LocationType.COORD, null, this.position),
                            1000, 0);
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

        public boolean isSameRequest(NetworkId newNetworkId, Point newPosition, String newSearchTerm) {
            if (this.networkId == newNetworkId
                    && this.isSameSearchTerm(newSearchTerm)
                    && PTHelper.distanceBetweenTwoPoints(this.position, newPosition) < 100) {
                return true;
            }
            return false;
        }

        private boolean isSameSearchTerm(String newSearchTerm) {
            if (this.searchTerm == null) {
                return newSearchTerm == null;
            }
            return this.searchTerm.equals(newSearchTerm);
        }
    }

}
