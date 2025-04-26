package org.walkersguide.android.server.pt;

import org.walkersguide.android.R;
import org.walkersguide.android.server.ServerException;
import org.walkersguide.android.util.GlobalInstance;


public class PtException extends ServerException {

    // requests
    public static final int RC_NO_NETWORK_PROVIDER = 3000;
    public static final int RC_NO_COORDINATES = 3001;
    public static final int RC_NO_STATION = 3002;
    public static final int RC_NO_DEPARTURE_DATE = 3003;

    // responses
    public static final int RC_REQUEST_FAILED = 3010;
    public static final int RC_SERVICE_DOWN = 3011;
    public static final int RC_INVALID_PROVIDER = 3012;
    public static final int RC_INVALID_STATION = 3013;
    public static final int RC_NO_DEPARTURES = 3014;
    public static final int RC_NO_TRIPS = 3015;
    public static final int RC_AMBIGUOUS_DESTINATION = 3016;
    public static final int RC_UNKNOWN_SERVER_RESPONSE = 3017;


    public static String getMessageForReturnCode(int returnCode) {
        switch (returnCode) {

            case RC_NO_NETWORK_PROVIDER:
                return GlobalInstance.getStringResource(R.string.errorPtReqNoNetworkProvider);
            case RC_NO_STATION:
                return GlobalInstance.getStringResource(R.string.errorPtReqNoStation);
            case RC_NO_DEPARTURE_DATE:
                return GlobalInstance.getStringResource(R.string.errorPtReqNoDepartureDate);
            case RC_NO_COORDINATES:
                return GlobalInstance.getStringResource(R.string.errorPtReqNoCoordinates);

            case RC_REQUEST_FAILED:
                return GlobalInstance.getStringResource(R.string.errorPtReqServerRequestFailed);
            case RC_SERVICE_DOWN:
                return GlobalInstance.getStringResource(R.string.errorPtReqPTServiceDown);
            case RC_INVALID_PROVIDER:
                return GlobalInstance.getStringResource(R.string.errorPtReqInvalidProvider);
            case RC_INVALID_STATION:
                return GlobalInstance.getStringResource(R.string.errorPtReqInvalidStation);
            case RC_NO_DEPARTURES:
                return GlobalInstance.getStringResource(R.string.errorPtReqNoDepartures);
            case RC_NO_TRIPS:
                return GlobalInstance.getStringResource(R.string.errorPtReqNoTrips);
            case RC_AMBIGUOUS_DESTINATION:
                return GlobalInstance.getStringResource(R.string.errorPtReqAmbiguousDestination);
            case RC_UNKNOWN_SERVER_RESPONSE:
                return GlobalInstance.getStringResource(R.string.errorPtReqUnknownServerResponse);

            default:
                return ServerException.getMessageForReturnCode(returnCode);
        }
    }


    public PtException(int returnCode) {
        super(getMessageForReturnCode(returnCode), returnCode);
    }

    public boolean destinationIsAmbiguous() {
        return getReturnCode() == RC_AMBIGUOUS_DESTINATION;
    }

    public boolean hasNoTrips() {
        return getReturnCode() == RC_NO_TRIPS;
    }

    public boolean showPtProviderDialog() {
        return getReturnCode() == RC_NO_NETWORK_PROVIDER;
    }

}
