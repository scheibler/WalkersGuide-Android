package org.walkersguide.android.server;

import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;


public abstract class ServerException extends Exception {

    public static final int RC_REQUEST_FAILED = 1000;
    public static final int RC_NO_INTERNET_CONNECTION = 1001;
    public static final int RC_BAD_REQUEST = 1002;
    public static final int RC_BAD_RESPONSE = 1003;
    public static final int RC_REQUEST_CANCELLED = 1004;


    public static String getMessageForReturnCode(int returnCode) {
        switch (returnCode) {
            case RC_REQUEST_FAILED:
                return GlobalInstance.getStringResource(R.string.errorReqRequestFailed);
            case RC_NO_INTERNET_CONNECTION:
                return GlobalInstance.getStringResource(R.string.errorReqNoInternetConnection);
            case RC_BAD_REQUEST:
                return GlobalInstance.getStringResource(R.string.errorReqBadRequest);
            case RC_BAD_RESPONSE:
                return GlobalInstance.getStringResource(R.string.errorReqBadResponse);
            case RC_REQUEST_CANCELLED:
                return GlobalInstance.getStringResource(R.string.errorReqRequestCancelled);
            default:
                return String.format(
                        GlobalInstance.getStringResource(R.string.errorReqUnknownReturnCode),
                        returnCode);
        }
    }


    private int returnCode;

    public ServerException(String message, int returnCode) {
        super(message);
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return this.returnCode;
    }

}
