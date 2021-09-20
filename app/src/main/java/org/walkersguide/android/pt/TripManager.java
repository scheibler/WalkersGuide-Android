package org.walkersguide.android.pt;

import org.walkersguide.android.exception.ServerCommunicationException;
import de.schildbach.pte.NetworkProvider.Optimize;
import java.util.EnumSet;

import de.schildbach.pte.AbstractNetworkProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Stop;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.TripOptions;


import android.os.AsyncTask;

import java.io.IOException;

import java.util.ArrayList;



import timber.log.Timber;
import android.text.TextUtils;
import java.util.Date;
import org.walkersguide.android.helper.ServerUtility;


public class TripManager {

    public interface TripListener{
        public void tripRequestTaskSuccessful(ArrayList<Stop> stopList);
        public void tripRequestTaskFailed(int returnCode);
    }


    private static TripManager managerInstance;
    private TripRequestTask tripRequestTask;

    public static TripManager getInstance() {
        if (managerInstance == null){
            managerInstance = getInstanceSynchronized();
        }
        return managerInstance;
    }

    private static synchronized TripManager getInstanceSynchronized() {
        if (managerInstance == null){
            managerInstance = new TripManager();
        }
        return managerInstance;
    }

    private TripManager() {
        this.tripRequestTask = null;
    }


    public void requestTrip(TripListener listener,
            AbstractNetworkProvider provider, Location station, Departure departure) {
        if (tripRequestTaskInProgress()) {
            if (listener == null) {
                return;
            } else if (this.tripRequestTask.isSameRequest(provider, station, departure)) {
                this.tripRequestTask.addListener(listener);
                return;
            } else {
                cancelTripRequestTask();
            }
        }
        // new request
        this.tripRequestTask = new TripRequestTask(listener, provider, station, departure);
        this.tripRequestTask.execute();
    }

    public void invalidateTripRequestTask(TripListener listener) {
        if (tripRequestTaskInProgress()) {
            this.tripRequestTask.removeListener(listener);
        }
    }

    public void cancelTripRequestTask() {
        if (tripRequestTaskInProgress()) {
            this.tripRequestTask.cancel();
        }
    }

    public boolean tripRequestTaskInProgress() {
        if (this.tripRequestTask != null
                && this.tripRequestTask.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }


    private static class TripRequestTask extends AsyncTask<Void, Void, ArrayList<Stop>> {

        private int returnCode;
        private ArrayList<TripListener> tripListenerList;

        private AbstractNetworkProvider provider;
        private Location station;
        private Departure departure;

        public TripRequestTask(TripListener listener, AbstractNetworkProvider provider, Location station, Departure departure) {
            this.returnCode = PTHelper.RC_OK;
            this.tripListenerList = new ArrayList<TripListener>();
            if (listener != null) {
                this.tripListenerList.add(listener);
            }
            this.provider = provider;
            if (this.provider != null) {
                this.provider.setUserAgent(PTHelper.USER_AGENT);
            }
            this.station = station;
            this.departure = departure;
        }

        @Override protected ArrayList<Stop> doInBackground(Void... params) {
            ArrayList<Stop> stopList = null;

            if (this.provider == null) {
                this.returnCode = PTHelper.RC_NO_NETWORK_PROVIDER;
            } else if (this.station == null) {
                this.returnCode = PTHelper.RC_NO_STATION;
            } else if (this.departure == null) {
                this.returnCode = PTHelper.RC_NO_DEPARTURE_DATE;
            } else if (! ServerUtility.isInternetAvailable()) {
                this.returnCode = PTHelper.RC_NO_INTERNET_CONNECTION;
            } else {

                Timber.d("Request: from %1$s to %2$s at %3$s", this.station.toString(), this.departure.toString(), this.departure.plannedTime.toString());
                String lineLabel = this.departure.line.label;
                Date departureTime = PTHelper.getDepartureTime(this.departure);
                TripOptions options = new TripOptions(null, Optimize.LEAST_CHANGES, null, null, null);
                if (this.departure.line != null && this.departure.line.product != null) {
                    options = new TripOptions(
                            EnumSet.of(this.departure.line.product), Optimize.LEAST_CHANGES, null, null, null);
                }

                QueryTripsResult tripsResult = null;
                try {
                    tripsResult = this.provider.queryTrips(
                            this.station, null, this.departure.destination, departureTime, true, options);
                    stopList = parseTripsResult(lineLabel, departureTime, tripsResult);
                } catch (IOException e) {
                    this.returnCode = PTHelper.RC_REQUEST_FAILED;
                } catch (ServerCommunicationException e) {
                    this.returnCode = e.getReturnCode();
                }

                // ambiguous destination
                if (this.returnCode == PTHelper.RC_AMBIGUOUS_DESTINATION
                        && tripsResult.ambiguousTo != null) {
                    // filter out stations
                    int maxAmbiguousLocationRequests = 3;
                    ArrayList<Location> ambiguousDestinationStationList = new ArrayList<Location>();
                    for (Location ambiguousDestinationLocation : tripsResult.ambiguousTo) {
                        if (ambiguousDestinationLocation.type == LocationType.STATION) {
                            ambiguousDestinationStationList.add(ambiguousDestinationLocation);
                            if (ambiguousDestinationStationList.size() == maxAmbiguousLocationRequests) {
                                break;
                            }
                        }
                    }

                    for (Location ambiguousDestinationStation : ambiguousDestinationStationList) {
                        // reset return code
                        this.returnCode = PTHelper.RC_OK;
                        Timber.d("ambiguous destination: %1$s", ambiguousDestinationStation.toString());
                        // start request
                        try {
                            stopList = parseTripsResult(
                                    lineLabel,
                                    departureTime,
                                    this.provider.queryTrips(
                                        this.station, null, ambiguousDestinationStation, departureTime, true, options));
                        } catch (IOException e) {
                            this.returnCode = PTHelper.RC_REQUEST_FAILED;
                        } catch (ServerCommunicationException e) {
                            this.returnCode = e.getReturnCode();
                        } finally {
                            if (stopList != null
                                    || this.returnCode != PTHelper.RC_NO_TRIPS) {
                                break;
                            }
                        }
                    }
                    // nothing found
                    if (this.returnCode == PTHelper.RC_NO_TRIPS) {
                        this.returnCode = PTHelper.RC_AMBIGUOUS_DESTINATION;
                    }
                }
            }

            return stopList;
        }

        @Override protected void onPostExecute(ArrayList<Stop> stopList) {
            for (TripListener listener : this.tripListenerList) {
                if (this.returnCode == PTHelper.RC_OK) {
                    listener.tripRequestTaskSuccessful(stopList);
                } else {
                    listener.tripRequestTaskFailed(this.returnCode);
                }
            }
        }

        @Override protected void onCancelled(ArrayList<Stop> stopList) {
            for (TripListener listener : this.tripListenerList) {
                listener.tripRequestTaskFailed(PTHelper.RC_CANCELLED);
            }
        }

        public void cancel() {
            this.cancel(true);
        }

        public void addListener(TripListener newListener) {
            if (newListener != null
                    && ! this.tripListenerList.contains(newListener)) {
                this.tripListenerList.add(newListener);
            }
        }

        public void removeListener(TripListener listenerToRemove) {
            if (listenerToRemove != null
                    && this.tripListenerList.contains(listenerToRemove)) {
                this.tripListenerList.remove(listenerToRemove);
            }
        }

        public boolean isSameRequest(AbstractNetworkProvider newProvider, Location newStation, Departure newDeparture) {
            if (this.isSameProvider(newProvider)
                    && this.isSameStation(newStation)
                    && this.isSameDeparture(newDeparture)) {
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
            return this.station.equals(newStation);
        }

        private boolean isSameDeparture(Departure newDeparture) {
            if (this.departure == null) {
                return newDeparture == null;
            }
            return this.departure.equals(newDeparture);
        }

        private ArrayList<Stop> parseTripsResult(String lineLabel, Date departureTime,
                QueryTripsResult tripsResult) throws ServerCommunicationException {
            if (tripsResult != null
                    && tripsResult.status == QueryTripsResult.Status.OK) {
                for (Trip trip : tripsResult.trips) {
                    if (trip.getFirstPublicLeg() != null
                            && trip.isTravelable()
                            && trip.getNumChanges() == 0) {
                        String tripLineLabel = trip.getFirstPublicLeg().line.label;
                        Date tripDepartureTime = PTHelper.getDepartureTime(trip.getFirstPublicLeg().departureStop);
                        Timber.d("    line: %1$s, %2$s / %3$s", lineLabel.equals(tripLineLabel), lineLabel.toString(), tripLineLabel.toString());
                        Timber.d("    time: %1$s, %2$s / %3$s", departureTime.equals(tripDepartureTime), departureTime.toString(), tripDepartureTime.toString());
                        if (lineLabel.equals(tripLineLabel)
                                && departureTime.equals(tripDepartureTime)) {
                            // trip found
                            ArrayList<Stop> stopList = new ArrayList<Stop>();
                            stopList.add(trip.getFirstPublicLeg().departureStop);
                            for (Stop stop : trip.getFirstPublicLeg().intermediateStops) {
                                stopList.add(stop);
                            }
                            stopList.add(trip.getFirstPublicLeg().arrivalStop);
                            return stopList;
                        }
                    }
                }
                throw new ServerCommunicationException(PTHelper.RC_NO_TRIPS);

            // errors
            } else if (tripsResult == null) {
                throw new ServerCommunicationException(PTHelper.RC_REQUEST_FAILED);
            } else if (tripsResult.status == QueryTripsResult.Status.SERVICE_DOWN) {
                throw new ServerCommunicationException(PTHelper.RC_SERVICE_DOWN);
            } else if (tripsResult.status == QueryTripsResult.Status.UNKNOWN_FROM
                    || tripsResult.status == QueryTripsResult.Status.UNKNOWN_TO) {
                throw new ServerCommunicationException(PTHelper.RC_INVALID_STATION);
            } else if (tripsResult.status == QueryTripsResult.Status.NO_TRIPS) {
                throw new ServerCommunicationException(PTHelper.RC_NO_TRIPS);
            } else if (tripsResult.status == QueryTripsResult.Status.AMBIGUOUS) {
                throw new ServerCommunicationException(PTHelper.RC_AMBIGUOUS_DESTINATION);
            } else {
                Timber.d("Status: %1$s", tripsResult.status);
                throw new ServerCommunicationException(PTHelper.RC_UNKNOWN_SERVER_RESPONSE);
            }
        }
    }

}
