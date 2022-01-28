    package org.walkersguide.android.server.address;

import org.walkersguide.android.R;
import org.walkersguide.android.server.ServerException;
import org.walkersguide.android.util.GlobalInstance;


public class AddressException extends ServerException {

    public static final int RC_NO_COORDINATES_FOR_ADDRESS = 2000;
    public static final int RC_NO_ADDRESS_FOR_COORDINATES = 2001;

    public static String getMessageForReturnCode(int returnCode) {
        switch (returnCode) {
            case RC_NO_COORDINATES_FOR_ADDRESS:
                return GlobalInstance.getStringResource(R.string.errorAddrReqNoCoordinatesForAddress);
            case RC_NO_ADDRESS_FOR_COORDINATES:
                return GlobalInstance.getStringResource(R.string.errorAddrReqNoAddressForCoordinates);
            default:
                return ServerException.getMessageForReturnCode(returnCode);
        }
    }


    public AddressException(int returnCode) {
        super(getMessageForReturnCode(returnCode), returnCode);
    }

}
