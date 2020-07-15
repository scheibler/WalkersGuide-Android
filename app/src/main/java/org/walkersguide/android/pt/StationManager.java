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

import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.util.Constants;

import timber.log.Timber;
import android.text.TextUtils;


public class StationManager {

    public interface StationListener{
        public void stationRequestTaskSuccessful(ArrayList<Location> stationList);
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

    public void requestStationList(StationListener listener, AbstractNetworkProvider provider, Point position) {
        if (stationRequestTaskInProgress()) {
            if (listener == null) {
                return;
            } else if (stationRequestTask.isSameProvider(provider)
                    && stationRequestTask.isSamePosition(position)) {
                this.stationRequestTask.addListener(listener);
                return;
            } else {
                cancelStationRequestTask();
            }
        }
        // new request
        this.stationRequestTask = new StationRequestTask(listener, provider, position);
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

        public StationRequestTask(StationListener listener, AbstractNetworkProvider provider, Point position) {
            this.returnCode = Constants.RC.OK;
            this.stationListenerList = new ArrayList<StationListener>();
            if (listener != null) {
                this.stationListenerList.add(listener);
            }
            this.provider = provider;
            if (this.provider != null) {
                this.provider.setUserAgent(PTHelper.USER_AGENT);
            }
            this.position = position;
        }

        @Override protected ArrayList<Location> doInBackground(Void... params) {
            ArrayList<Location> stationList = new ArrayList<Location>();

            if (this.provider == null) {
                this.returnCode = Constants.RC.NO_PT_PROVIDER;
            } else if (this.position == null) {
                this.returnCode = Constants.RC.MISSING_OR_INVALID_PT_REQUEST_DATA;
            } else if (! ServerUtility.isInternetAvailable(context)) {
                this.returnCode = Constants.RC.NO_INTERNET_CONNECTION;
            } else {

                NearbyLocationsResult nearbyResult = null;
                try {
                    nearbyResult = this.provider.queryNearbyLocations(
                            EnumSet.of(LocationType.STATION),
                            new Location(LocationType.COORD, null, this.position),
                            200, 10);
                } catch (IOException e) {
                    Timber.e("e: %1$s", e.getMessage());
                    nearbyResult = null;
                } finally {

                    if (nearbyResult == null
                            || nearbyResult.status == NearbyLocationsResult.Status.SERVICE_DOWN) {
                        this.returnCode = Constants.RC.PT_SERVICE_DOWN;
                    } else if (nearbyResult.status != NearbyLocationsResult.Status.OK) {
                        this.returnCode = Constants.RC.PT_SERVICE_FAILED;
                    } else {
                        for (Location location : nearbyResult.locations) {
                            if (! stationList.contains(location)) {
                                stationList.add(location);
                            }
                        }
                    }
                }
            }

            return stationList;
        }

        @Override protected void onPostExecute(ArrayList<Location> stationList) {
            Timber.d("Done; rc=%1$d", this.returnCode);
            for (StationListener listener : this.stationListenerList) {
                if (returnCode == Constants.RC.OK) {
                    listener.stationRequestTaskSuccessful(stationList);
                } else {
                    listener.stationRequestTaskFailed(this.returnCode);
                }
            }
        }

        @Override protected void onCancelled(ArrayList<Location> stationList) {
            for (StationListener listener : this.stationListenerList) {
                listener.stationRequestTaskFailed(Constants.RC.CANCELLED);
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

        public boolean isSameProvider(AbstractNetworkProvider newProvider) {
            if (this.provider == null) {
                return newProvider == null;
            }
            return this.provider.equals(newProvider);
        }

        public boolean isSamePosition(Point newPosition) {
            if (this.position == null) {
                return newPosition == null;
            }
            return this.position.equals(newPosition);
        }
    }

}
