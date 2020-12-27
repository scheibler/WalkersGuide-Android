package org.walkersguide.android.pt;

import de.schildbach.pte.NetworkProvider.Optimize;
import java.util.EnumSet;

import de.schildbach.pte.AbstractNetworkProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Stop;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.TripOptions;

import android.content.Context;

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


    private Context context;
    private static TripManager TripManagerInstance;
    private TripRequestTask tripRequestTask;

    public static TripManager getInstance(Context context) {
        if(TripManagerInstance == null){
            TripManagerInstance = new TripManager(context.getApplicationContext());
        }
        return TripManagerInstance;
    }

    private TripManager(Context context) {
        this.context = context;
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


    private class TripRequestTask extends AsyncTask<Void, Void, ArrayList<Stop>> {

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
            ArrayList<Stop> stopList = new ArrayList<Stop>();

            if (this.provider == null) {
                this.returnCode = PTHelper.RC_NO_NETWORK_PROVIDER;
            } else if (this.station == null) {
                this.returnCode = PTHelper.RC_NO_STATION;
            } else if (this.departure == null) {
                this.returnCode = PTHelper.RC_NO_DEPARTURE_DATE;
            } else if (! ServerUtility.isInternetAvailable(context)) {
                this.returnCode = PTHelper.RC_NO_INTERNET_CONNECTION;
            } else {

                Timber.d("Request: from %1$s to %2$s at %3$s", this.station.toString(), this.departure.toString(), this.departure.plannedTime.toString());
                String lineLabel = this.departure.line.label;
                Date departureTime = PTHelper.getDepartureTime(this.departure);
                Location destination = this.departure.destination;
                TripOptions options = new TripOptions(null, Optimize.LEAST_CHANGES, null, null, null);
                if (this.departure.line != null && this.departure.line.product != null) {
                    options = new TripOptions(
                            EnumSet.of(this.departure.line.product), Optimize.LEAST_CHANGES, null, null, null);
                }
                QueryTripsResult tripsResult = null;

                try {
                    tripsResult = this.provider.queryTrips(
                            this.station, null, destination, departureTime, true, options);
                    if (tripsResult != null
                            && tripsResult.status == QueryTripsResult.Status.AMBIGUOUS
                            && tripsResult.ambiguousTo != null
                            && tripsResult.ambiguousTo.size() > 0) {
                        destination = tripsResult.ambiguousTo.get(0);
                        Timber.d("ambiguous: %1$s", destination.toString());
                        tripsResult = this.provider.queryTrips(
                                this.station, null, destination, departureTime, true, options);
                    }

                } catch (IOException e) {
                    Timber.e("TripManager query error: %1$s", e.getMessage());
                    tripsResult = null;

                } finally {
                    if (tripsResult == null) {
                        this.returnCode = PTHelper.RC_REQUEST_FAILED;
                    } else if (tripsResult.status == QueryTripsResult.Status.OK) {
                        for (Trip trip : tripsResult.trips) {
                            if (trip.getFirstPublicLeg() != null
                                    && trip.isTravelable()
                                    && trip.getNumChanges() == 0) {
                                String tripLineLabel = trip.getFirstPublicLeg().line.label;
                                Date tripDepartureTime = PTHelper.getDepartureTime(trip.getFirstPublicLeg().departureStop);
                                Timber.d("line: %1$s, %2$s / %3$s", lineLabel.equals(tripLineLabel), lineLabel.toString(), tripLineLabel.toString());
                                Timber.d("time: %1$s, %2$s / %3$s", departureTime.equals(tripDepartureTime), departureTime.toString(), tripDepartureTime.toString());
                                if (lineLabel.equals(tripLineLabel)
                                        && departureTime.equals(tripDepartureTime)) {
                                    stopList.add(trip.getFirstPublicLeg().departureStop);
                                    for (Stop stop : trip.getFirstPublicLeg().intermediateStops) {
                                        stopList.add(stop);
                                    }
                                    stopList.add(trip.getFirstPublicLeg().arrivalStop);
                                    Timber.d("Arrival stop: %1$s", trip.getFirstPublicLeg().arrivalStop.toString());
                                    break;
                                }
                            }
                        }
                        if (stopList.isEmpty()) {
                            this.returnCode = PTHelper.RC_NO_TRIPS;
                        }
                    } else if (tripsResult.status == QueryTripsResult.Status.SERVICE_DOWN) {
                        this.returnCode = PTHelper.RC_SERVICE_DOWN;
                    } else if (tripsResult.status == QueryTripsResult.Status.UNKNOWN_FROM
                            || tripsResult.status == QueryTripsResult.Status.UNKNOWN_TO) {
                        this.returnCode = PTHelper.RC_INVALID_STATION;
                    } else if (tripsResult.status == QueryTripsResult.Status.NO_TRIPS) {
                        this.returnCode = PTHelper.RC_NO_TRIPS;
                    } else {
                        this.returnCode = PTHelper.RC_UNKNOWN_SERVER_RESPONSE;
                        Timber.d("Status: %1$s", tripsResult.status);
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

    }

}
