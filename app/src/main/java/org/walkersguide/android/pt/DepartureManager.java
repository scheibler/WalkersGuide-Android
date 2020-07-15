package org.walkersguide.android.pt;

import de.schildbach.pte.dto.Location;
import java.util.EnumSet;

import de.schildbach.pte.AbstractNetworkProvider;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.QueryDeparturesResult;

import android.content.Context;

import android.os.AsyncTask;

import java.io.IOException;

import java.util.ArrayList;


import timber.log.Timber;
import android.text.TextUtils;
import java.util.Date;

import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.util.Constants;


public class DepartureManager {

    public interface DepartureListener{
        public void departureRequestTaskSuccessful(ArrayList<Departure> departureList);
        public void departureRequestTaskFailed(int returnCode);
    }


    private Context context;
    private static DepartureManager DepartureManagerInstance;
    private DepartureRequestTask departureRequestTask;

    public static DepartureManager getInstance(Context context) {
        if(DepartureManagerInstance == null){
            DepartureManagerInstance = new DepartureManager(context.getApplicationContext());
        }
        return DepartureManagerInstance;
    }

    private DepartureManager(Context context) {
        this.context = context;
        this.departureRequestTask = null;
    }

    public void requestDeparture(DepartureListener listener, AbstractNetworkProvider provider, Location station, Date departureTime) {
        if (departureRequestTaskInProgress()) {
            if (listener == null) {
                return;
            } else if (departureRequestTask.isSameProvider(provider)
                    && departureRequestTask.isSameStation(station)
                    && departureRequestTask.isSameDepartureTime(departureTime)) {
                this.departureRequestTask.addListener(listener);
                return;
            } else {
                cancelDepartureRequestTask();
            }
        }
        // new request
        this.departureRequestTask = new DepartureRequestTask(listener, provider, station, departureTime);
        this.departureRequestTask.execute();
    }

    public void invalidateDepartureRequestTask(DepartureListener listener) {
        if (departureRequestTaskInProgress()) {
            this.departureRequestTask.removeListener(listener);
        }
    }

    public void cancelDepartureRequestTask() {
        if (departureRequestTaskInProgress()) {
            this.departureRequestTask.cancel();
        }
    }

    public boolean departureRequestTaskInProgress() {
        if (this.departureRequestTask != null
                && this.departureRequestTask.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }


    private class DepartureRequestTask extends AsyncTask<Void, Void, ArrayList<Departure>> {

        private int returnCode;
        private ArrayList<DepartureListener> departureListenerList;

        private AbstractNetworkProvider provider;
        private Location station;
        private Date departureTime;

        public DepartureRequestTask(DepartureListener listener, AbstractNetworkProvider provider, Location station, Date departureTime) {
            this.returnCode = Constants.RC.OK;
            this.departureListenerList = new ArrayList<DepartureListener>();
            if (listener != null) {
                this.departureListenerList.add(listener);
            }
            this.provider = provider;
            if (this.provider != null) {
                this.provider.setUserAgent(PTHelper.USER_AGENT);
            }
            this.station = station;
            this.departureTime = departureTime;
        }

        @Override protected ArrayList<Departure> doInBackground(Void... params) {
            ArrayList<Departure> departureList = new ArrayList<Departure>();

            if (this.provider == null) {
                this.returnCode = Constants.RC.NO_PT_PROVIDER;
            } else if (this.station == null || this.departureTime == null) {
                this.returnCode = Constants.RC.MISSING_OR_INVALID_PT_REQUEST_DATA;
            } else if (! ServerUtility.isInternetAvailable(context)) {
                this.returnCode = Constants.RC.NO_INTERNET_CONNECTION;
            } else {

                QueryDeparturesResult departuresResult = null;
                try {
                    departuresResult = this.provider.queryDepartures(
                            this.station.id, this.departureTime, 100, false);
                } catch (IOException e) {
                    Timber.e("DepartureManager query error: %1$s", e.getMessage());
                    departuresResult = null;
                } finally {

                    if (departuresResult == null
                            || departuresResult.status == QueryDeparturesResult.Status.SERVICE_DOWN) {
                        this.returnCode = Constants.RC.PT_SERVICE_DOWN;
                    } else if (departuresResult.status != QueryDeparturesResult.Status.OK) {
                        Timber.d("Failed: %1$s", departuresResult.status);
                        this.returnCode = Constants.RC.PT_SERVICE_FAILED;
                    } else {
                        for (StationDepartures stationDepartures  : departuresResult.stationDepartures) {
                            for (Departure departure : stationDepartures.departures) {
                                if (! departureList.contains(departure)) {
                                    departureList.add(departure);
                                }
                            }
                        }
                        if (departureList.isEmpty()) {
                            this.returnCode = Constants.RC.NO_PT_DEPARTURES;
                        }
                    }
                }
            }

            return departureList;
        }

        @Override protected void onPostExecute(ArrayList<Departure> departureList) {
            Timber.d("Done; rc=%1$d", this.returnCode);
            for (DepartureListener listener : this.departureListenerList) {
                if (returnCode == Constants.RC.OK) {
                    listener.departureRequestTaskSuccessful(departureList);
                } else {
                    listener.departureRequestTaskFailed(returnCode);
                }
            }
        }

        @Override protected void onCancelled(ArrayList<Departure> departureList) {
            for (DepartureListener listener : this.departureListenerList) {
                listener.departureRequestTaskFailed(Constants.RC.CANCELLED);
            }
        }

        public void cancel() {
            this.cancel(true);
        }

        public void addListener(DepartureListener newListener) {
            if (newListener != null
                    && ! this.departureListenerList.contains(newListener)) {
                this.departureListenerList.add(newListener);
            }
        }

        public void removeListener(DepartureListener listenerToRemove) {
            if (listenerToRemove != null
                    && this.departureListenerList.contains(listenerToRemove)) {
                this.departureListenerList.remove(listenerToRemove);
            }
        }

        public boolean isSameProvider(AbstractNetworkProvider newProvider) {
            if (this.provider == null) {
                return newProvider == null;
            }
            return this.provider.equals(newProvider);
        }

        public boolean isSameStation(Location newStation) {
            if (this.station == null) {
                return newStation == null;
            }
            return this.station.equals(newStation);
        }

        public boolean isSameDepartureTime(Date newDate) {
            if (this.departureTime == null) {
                return newDate == null;
            }
            return this.departureTime.equals(newDate);
        }
    }

}
