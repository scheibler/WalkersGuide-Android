package org.walkersguide.android.server.pt;

import java.util.EnumSet;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.SuggestedLocation;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.SuggestLocationsResult;
import de.schildbach.pte.dto.NearbyLocationsResult;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.NetworkId;
import de.schildbach.pte.AbstractNetworkProvider;


import org.walkersguide.android.server.ServerTask;
import org.walkersguide.android.server.ServerTaskExecutor;
import android.text.TextUtils;
import timber.log.Timber;
import java.io.IOException;
import java.util.ArrayList;


public class NearbyStationsTask extends ServerTask {

    private NetworkId networkId;
    private Point position;
        private String searchTerm;

    public NearbyStationsTask(NetworkId networkId, Point position, String searchTerm) {
        this.networkId = networkId;
        this.position = position;
        this.searchTerm = searchTerm;
    }

    @Override public void execute() throws PtException {
        ArrayList<Location> stationList = new ArrayList<Location>();

        AbstractNetworkProvider provider = PtUtility.findNetworkProvider(this.networkId);
        if (provider == null) {
            throw new PtException(PtException.RC_NO_NETWORK_PROVIDER);
        } else if (this.position == null) {
            throw new PtException(PtException.RC_NO_COORDINATES);
        }

        if (! TextUtils.isEmpty(this.searchTerm)) {
            SuggestLocationsResult searchResult = null;
            try {
                searchResult = provider.suggestLocations(
                        this.searchTerm, EnumSet.of(LocationType.STATION), 0);
            } catch (IOException e) {
                searchResult = null;
            } finally {
                if (searchResult == null) {
                    throw new PtException(PtException.RC_REQUEST_FAILED);
                } else if (searchResult.status == SuggestLocationsResult.Status.OK) {
                    for (SuggestedLocation suggestedLocation : searchResult.suggestedLocations) {
                        if (! stationList.contains(suggestedLocation.location)) {
                            stationList.add(suggestedLocation.location);
                        }
                    }
                } else if (searchResult.status == SuggestLocationsResult.Status.SERVICE_DOWN) {
                    throw new PtException(PtException.RC_SERVICE_DOWN);
                } else {
                    throw new PtException(PtException.RC_UNKNOWN_SERVER_RESPONSE);
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
                    throw new PtException(PtException.RC_REQUEST_FAILED);
                } else if (nearbyResult.status == NearbyLocationsResult.Status.OK) {
                    stationList = new ArrayList<Location>();
                    for (Location location : nearbyResult.locations) {
                        if (! stationList.contains(location)) {
                            stationList.add(location);
                        }
                    }
                } else if (nearbyResult.status == NearbyLocationsResult.Status.SERVICE_DOWN) {
                    throw new PtException(PtException.RC_SERVICE_DOWN);
                } else {
                    throw new PtException(PtException.RC_UNKNOWN_SERVER_RESPONSE);
                }
            }
        }

        if (! isCancelled()) {
            ServerTaskExecutor.sendNearbyStationsTaskSuccessfulBroadcast(getId(), stationList);
        }
    }

}
