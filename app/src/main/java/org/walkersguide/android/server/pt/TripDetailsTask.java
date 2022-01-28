package org.walkersguide.android.server.pt;

import java.util.EnumSet;
import de.schildbach.pte.NetworkProvider.Optimize;
import de.schildbach.pte.dto.Stop;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.TripOptions;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.NetworkId;
import de.schildbach.pte.AbstractNetworkProvider;


import org.walkersguide.android.server.ServerTask;
import org.walkersguide.android.server.ServerTaskExecutor;
import timber.log.Timber;
import java.io.IOException;
import java.util.Date;
import java.util.ArrayList;


public class TripDetailsTask extends ServerTask {

    private NetworkId networkId;
    private Location station;
    private Departure departure;

    public TripDetailsTask(NetworkId networkId, Location station, Departure departure) {
        this.networkId = networkId;
        this.station = station;
        this.departure = departure;
    }

    @Override public void execute() throws PtException {
        ArrayList<Stop> stopList = null;

        AbstractNetworkProvider provider = PtUtility.findNetworkProvider(this.networkId);
        if (provider == null) {
            throw new PtException(PtException.RC_NO_NETWORK_PROVIDER);
        } else if (this.station == null) {
            throw new PtException(PtException.RC_NO_STATION);
        } else if (this.departure == null) {
            throw new PtException(PtException.RC_NO_DEPARTURE_DATE);
        }

        Timber.d("Request: from %1$s to %2$s at %3$s", this.station.toString(), this.departure.toString(), departure.plannedTime.toString());
        String lineLabel = this.departure.line.label;
        Date departureTime = PtUtility.getDepartureTime(this.departure);
        TripOptions options = new TripOptions(null, Optimize.LEAST_CHANGES, null, null, null);
        if (this.departure.line != null && this.departure.line.product != null) {
            options = new TripOptions(
                    EnumSet.of(this.departure.line.product), Optimize.LEAST_CHANGES, null, null, null);
        }

        QueryTripsResult tripsResult = null;
        try {
            tripsResult = provider.queryTrips(
                    this.station, null, this.departure.destination, departureTime, true, options);
        } catch (IOException e) {
            tripsResult = null;
        } finally {
            if (tripsResult == null) {
                throw new PtException(PtException.RC_REQUEST_FAILED);
            }
        }
        try {
            sendBroadcastAndFinish(
                    parseTripsResult(lineLabel, departureTime, tripsResult));
        } catch (PtException e) {
            if (e.destinationIsAmbiguous()
                    && tripsResult.ambiguousTo != null) {
                ;// skip to next block
            } else {
                throw e;
            }
        }

        // ambiguous destination
        //
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
            Timber.d("ambiguous destination: %1$s", ambiguousDestinationStation.toString());
            QueryTripsResult ambDestinationTripsResult = null;
            try {
                ambDestinationTripsResult = provider.queryTrips(
                        this.station, null, ambiguousDestinationStation, departureTime, true, options);
            } catch (IOException e) {
                ambDestinationTripsResult = null;
            } finally {
                if (ambDestinationTripsResult == null) {
                    throw new PtException(PtException.RC_REQUEST_FAILED);
                }
            }

            // post-processing
            try {
                sendBroadcastAndFinish(
                        parseTripsResult(lineLabel, departureTime, ambDestinationTripsResult));
            } catch (PtException e) {
                if (e.hasNoTrips()) {
                    ;   // skip to next block
                } else {
                    throw e;
                }
            }
        }

        // nothing found
        throw new PtException(PtException.RC_AMBIGUOUS_DESTINATION);
    }

    private static ArrayList<Stop> parseTripsResult(String lineLabel, Date departureTime,
            QueryTripsResult tripsResult) throws PtException {
        if (tripsResult != null
                && tripsResult.status == QueryTripsResult.Status.OK) {

            for (Trip trip : tripsResult.trips) {

                if (trip.getFirstPublicLeg() != null
                        && trip.isTravelable()
                        && trip.getNumChanges() == 0) {
                    String tripLineLabel = trip.getFirstPublicLeg().line.label;
                    Date tripDepartureTime = PtUtility.getDepartureTime(trip.getFirstPublicLeg().departureStop);
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

            // nothing found
            throw new PtException(PtException.RC_NO_TRIPS);

        // else: errors
        } else if (tripsResult == null) {
            throw new PtException(PtException.RC_REQUEST_FAILED);
        } else if (tripsResult.status == QueryTripsResult.Status.SERVICE_DOWN) {
            throw new PtException(PtException.RC_SERVICE_DOWN);
        } else if (tripsResult.status == QueryTripsResult.Status.UNKNOWN_FROM
                || tripsResult.status == QueryTripsResult.Status.UNKNOWN_TO) {
            throw new PtException(PtException.RC_INVALID_STATION);
        } else if (tripsResult.status == QueryTripsResult.Status.NO_TRIPS) {
            throw new PtException(PtException.RC_NO_TRIPS);
        } else if (tripsResult.status == QueryTripsResult.Status.AMBIGUOUS) {
            throw new PtException(PtException.RC_AMBIGUOUS_DESTINATION);
        } else {
            Timber.e("Status: %1$s", tripsResult.status);
            throw new PtException(PtException.RC_UNKNOWN_SERVER_RESPONSE);
        }
    }

    private void sendBroadcastAndFinish(ArrayList<Stop> stopList) {
        if (! isCancelled()) {
            ServerTaskExecutor.sendTripDetailsTaskSuccessfulBroadcast(getId(), stopList);
        }
    }

}
