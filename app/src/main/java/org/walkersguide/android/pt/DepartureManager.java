package org.walkersguide.android.pt;

import de.schildbach.pte.dto.Location;
import java.util.EnumSet;

import de.schildbach.pte.AbstractNetworkProvider;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.QueryDeparturesResult;


import android.os.AsyncTask;

import java.io.IOException;

import java.util.ArrayList;


import timber.log.Timber;
import android.text.TextUtils;
import java.util.Date;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.util.GlobalInstance;


public class DepartureManager {

    public interface DepartureListener{
        public void departureRequestTaskSuccessful(ArrayList<Departure> departureList);
        public void departureRequestTaskFailed(int returnCode);
    }


    private static DepartureManager managerInstance;
    private DepartureRequestTask departureRequestTask;

    public static DepartureManager getInstance() {
        if (managerInstance == null){
            managerInstance = getInstanceSynchronized();
        }
        return managerInstance;
    }

    private static synchronized DepartureManager getInstanceSynchronized() {
        if (managerInstance == null){
            managerInstance = new DepartureManager();
        }
        return managerInstance;
    }

    private DepartureManager() {
        this.departureRequestTask = null;
    }


    public void requestDeparture(DepartureListener listener, AbstractNetworkProvider provider, Location station, Date departureDate) {
        if (departureRequestTaskInProgress()) {
            if (listener == null) {
                return;
            } else if (this.departureRequestTask.isSameRequest(provider, station)) {
                this.departureRequestTask.addListener(listener);
                return;
            } else {
                cancelDepartureRequestTask();
            }
        }
        // new request
        this.departureRequestTask = new DepartureRequestTask(listener, provider, station, departureDate);
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


    private static class DepartureRequestTask extends AsyncTask<Void, Void, ArrayList<Departure>> {

        private int returnCode;
        private ArrayList<DepartureListener> departureListenerList;

        private AbstractNetworkProvider provider;
        private Location station;
        private Date initialDepartureDate;

        public DepartureRequestTask(DepartureListener listener, AbstractNetworkProvider provider, Location station, Date initialDepartureDate) {
            this.returnCode = PTHelper.RC_OK;
            this.departureListenerList = new ArrayList<DepartureListener>();
            if (listener != null) {
                this.departureListenerList.add(listener);
            }
            this.provider = provider;
            if (this.provider != null) {
                this.provider.setUserAgent(PTHelper.USER_AGENT);
            }
            this.station = station;
            this.initialDepartureDate = initialDepartureDate;
        }

        @Override protected ArrayList<Departure> doInBackground(Void... params) {
            ArrayList<Departure> departureList = new ArrayList<Departure>();

            if (this.provider == null) {
                this.returnCode = PTHelper.RC_NO_NETWORK_PROVIDER;
            } else if (this.station == null) {
                this.returnCode = PTHelper.RC_NO_STATION;
            } else if (! ServerUtility.isInternetAvailable()) {
                this.returnCode = PTHelper.RC_NO_INTERNET_CONNECTION;
            } else {

                int numberOfRequests = 0, maxNumberOfRequests = 5, maxNumberOfDepartures = 50;
                Date nextDepartureDate = this.initialDepartureDate;
                Date maxDepartureDate = new Date(
                        this.initialDepartureDate.getTime() + 3*60*60*1000);
                QueryDeparturesResult departuresResult = null;
                while (this.returnCode == PTHelper.RC_OK
                        && numberOfRequests < maxNumberOfRequests
                        && departureList.size() < maxNumberOfDepartures
                        && nextDepartureDate.before(maxDepartureDate)) {

                    // query departures
                    try {
                        Timber.d("request: stationId=%1$s, date=%2$s", this.station.id, nextDepartureDate.toString());
                        departuresResult = this.provider.queryDepartures(
                                this.station.id, nextDepartureDate, 100, false);
                    } catch (IOException e) {
                        Timber.e("DepartureManager query error: %1$s", e.getMessage());
                        departuresResult = null;
                    } finally {
                        if (departuresResult == null) {
                            break;
                        }
                        Timber.d("result: %1$s, numberOfRequests: %2$d", departuresResult.status, numberOfRequests);
                    }

                    // parse departures
                    for (StationDepartures stationDepartures  : departuresResult.stationDepartures) {
                        for (Departure departure : stationDepartures.departures) {
                            if (! departureList.contains(departure)) {
                                departureList.add(departure);
                            }
                        }
                    }

                    // update next departure date
                    if (! departureList.isEmpty()) {
                        nextDepartureDate = PTHelper.getDepartureTime(
                                departureList.get(departureList.size()-1));
                    }
                    // increment request counter
                    numberOfRequests += 1;
                }

                // post-processing
                if (departureList.isEmpty()) {
                    if (departuresResult == null) {
                        this.returnCode = PTHelper.RC_REQUEST_FAILED;
                    } else if (departuresResult.status == QueryDeparturesResult.Status.SERVICE_DOWN) {
                        this.returnCode = PTHelper.RC_SERVICE_DOWN;
                    } else if (departuresResult.status == QueryDeparturesResult.Status.INVALID_STATION) {
                        this.returnCode = PTHelper.RC_INVALID_STATION;
                    } else if (departuresResult.status == QueryDeparturesResult.Status.OK) {
                        this.returnCode = PTHelper.RC_NO_DEPARTURES;
                    } else {
                        this.returnCode = PTHelper.RC_UNKNOWN_SERVER_RESPONSE;
                    }
                }
            }

            return departureList;
        }

        @Override protected void onPostExecute(ArrayList<Departure> departureList) {
            for (DepartureListener listener : this.departureListenerList) {
                if (returnCode == PTHelper.RC_OK) {
                    listener.departureRequestTaskSuccessful(departureList);
                } else {
                    listener.departureRequestTaskFailed(returnCode);
                }
            }
        }

        @Override protected void onCancelled(ArrayList<Departure> departureList) {
            for (DepartureListener listener : this.departureListenerList) {
                listener.departureRequestTaskFailed(PTHelper.RC_CANCELLED);
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

        public boolean isSameRequest(AbstractNetworkProvider newProvider, Location newStation) {
            if (this.isSameProvider(newProvider) && this.isSameStation(newStation)) {
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

        private boolean isSameStation(Location newStation) {
            if (this.station == null) {
                return newStation == null;
            }
            return this.station.id.equals(newStation.id);
        }
    }

}
