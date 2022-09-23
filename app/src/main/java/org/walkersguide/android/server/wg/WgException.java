package org.walkersguide.android.server.wg;

import org.walkersguide.android.R;
import org.walkersguide.android.server.ServerException;
import org.walkersguide.android.util.GlobalInstance;


public class WgException extends ServerException {

    // WalkersGuide server return codes
    //
    // errors caused by client request
    public static final int RC_BAD_REQUEST = 400;
    public static final int RC_REQUEST_IN_PROGRESS = 429;
    // errors caused by the server
    public static final int RC_INTERNAL_SERVER_ERROR = 500;
    public static final int RC_SERVICE_UNAVAILABLE = 503;
    // walkersguide specific errors
    public static final int RC_CANCELLED_BY_CLIENT = 550;
    // map
    public static final int RC_MAP_LOADING_FAILED = 555;
    public static final int RC_WRONG_MAP_SELECTED = 556;
    public static final int RC_MAP_OUTDATED = 557;
    // poi
    public static final int RC_NO_POI_TAGS_SELECTED = 560;
    // route calculation
    public static final int RC_START_OR_DESTINATION_MISSING = 570;
    public static final int RC_START_AND_DESTINATION_TOO_FAR_AWAY = 571;
    public static final int RC_TOO_MANY_WAY_CLASSES_IGNORED = 572;
    public static final int RC_NO_ROUTE_BETWEEN_START_AND_DESTINATION = 573;

    // local return codes thrown by the app
    // poi
    public static final int RC_NO_POI_PROFILE_CREATED = 4000;
    public static final int RC_NO_POI_PROFILE_SELECTED = 4001;
    public static final int RC_POI_PROFILE_PARSING_ERROR = 4003;
    public static final int RC_UNSUPPORTED_POI_REQUEST_ACTION = 4004;
    // route
    public static final int RC_NO_ROUTE_CREATED = 4010;
    public static final int RC_NO_ROUTE_SELECTED = 4011;
    public static final int RC_ROUTE_PARSING_ERROR = 4012;
    // status
    public static final int RC_NO_SERVER_URL = 4020;
    public static final int RC_API_CLIENT_OUTDATED = 4021;
    public static final int RC_API_SERVER_OUTDATED = 4022;
    public static final int RC_NO_MAP_LIST = 4023;


    public static String getMessageForReturnCode(int returnCode) {
        switch (returnCode) {

            // walkersguide server errorWgReqs
            //
            // caused by client
            case RC_BAD_REQUEST:
                return GlobalInstance.getStringResource(R.string.errorWgReqBadRequest);
            case RC_REQUEST_IN_PROGRESS:
                return GlobalInstance.getStringResource(R.string.errorWgReqRequestInProgress);
            // caused by server
            case RC_INTERNAL_SERVER_ERROR:
                return GlobalInstance.getStringResource(R.string.errorWgReqInternalServerError);
            case RC_SERVICE_UNAVAILABLE:
                return GlobalInstance.getStringResource(R.string.errorWgReqServiceUnavailableOrBusy);
            // walkersguide custom errorWgReqs
            case RC_CANCELLED_BY_CLIENT:
                return GlobalInstance.getStringResource(R.string.errorReqRequestCancelled);
            case RC_NO_POI_TAGS_SELECTED:
                return GlobalInstance.getStringResource(R.string.errorWgReqNoPOITagsSelected);
            case RC_MAP_LOADING_FAILED:
                return GlobalInstance.getStringResource(R.string.errorWgReqMapLoadingFailed);
            case RC_WRONG_MAP_SELECTED:
                return GlobalInstance.getStringResource(R.string.errorWgReqWrongMapSelected);
            case RC_MAP_OUTDATED:
                return GlobalInstance.getStringResource(R.string.errorWgReqMapOutdated);
            // route calculation
            case RC_START_OR_DESTINATION_MISSING:
                return GlobalInstance.getStringResource(R.string.errorWgReqStartOrDestinationMissing);
            case RC_START_AND_DESTINATION_TOO_FAR_AWAY:
                return GlobalInstance.getStringResource(R.string.errorWgReqStartAndDestinationTooFarAway);
            case RC_TOO_MANY_WAY_CLASSES_IGNORED:
                return GlobalInstance.getStringResource(R.string.errorWgReqTooManyWayClassesIgnored);
            case RC_NO_ROUTE_BETWEEN_START_AND_DESTINATION:
                return GlobalInstance.getStringResource(R.string.errorWgReqNoRouteBetweenStartAndDestination);

            // android app
            //
            // poi
            case RC_NO_POI_PROFILE_CREATED:
                return GlobalInstance.getStringResource(R.string.errorWgReqNoPOIProfileCreated);
            case RC_NO_POI_PROFILE_SELECTED:
                return GlobalInstance.getStringResource(R.string.errorWgReqNoPOIProfileSelected);
            case RC_POI_PROFILE_PARSING_ERROR:
                return GlobalInstance.getStringResource(R.string.errorWgReqPOIProfileParsing);
            case RC_UNSUPPORTED_POI_REQUEST_ACTION:
                return GlobalInstance.getStringResource(R.string.errorWgReqUnsupportedPOIRequestAction);
            // route
            case RC_NO_ROUTE_CREATED:
                return GlobalInstance.getStringResource(R.string.errorWgReqNoRouteCreated);
            case RC_NO_ROUTE_SELECTED:
                return GlobalInstance.getStringResource(R.string.errorWgReqNoRouteSelected);
            case RC_ROUTE_PARSING_ERROR:
                return GlobalInstance.getStringResource(R.string.errorWgReqRouteParsing);
            // status
            case RC_NO_SERVER_URL:
                return GlobalInstance.getStringResource(R.string.errorWgReqNoServerURL);
            case RC_API_CLIENT_OUTDATED:
                return GlobalInstance.getStringResource(R.string.errorWgReqAPIClientOutdated);
            case RC_API_SERVER_OUTDATED:
                return GlobalInstance.getStringResource(R.string.errorWgReqAPIServerOutdated);
            case RC_NO_MAP_LIST:
                return GlobalInstance.getStringResource(R.string.errorWgReqNoMapList);

            default:
                return ServerException.getMessageForReturnCode(returnCode);
        }
    }


    public WgException(int returnCode) {
        super(getMessageForReturnCode(returnCode), returnCode);
    }

    public boolean showMapDialog() {
        return getReturnCode() == RC_MAP_LOADING_FAILED
            || getReturnCode() == RC_WRONG_MAP_SELECTED;
    }

}
