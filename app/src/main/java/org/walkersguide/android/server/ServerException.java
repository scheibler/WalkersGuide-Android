package org.walkersguide.android.server;

import javax.net.ssl.HttpsURLConnection;
import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import java.util.Locale;


public abstract class ServerException extends Exception {

    public static final int RC_NO_INTERNET_CONNECTION = 1000;
    public static final int RC_UNKNOWN_HOST = 1001;
    public static final int RC_SERVER_CONNECTION_FAILED = 1002;
    public static final int RC_SERVER_CONNECTION_TIMEOUT = 1003;
    public static final int RC_SERVER_CONNECTION_UNKNOWN_IO_EXCEPTION = 1004;
    public static final int RC_BAD_RESPONSE = 1005;
    public static final int RC_REQUEST_CANCELLED = 1006;


    public static String getMessageForReturnCode(int returnCode) {
        switch (returnCode) {
            case RC_NO_INTERNET_CONNECTION:
                return GlobalInstance.getStringResource(R.string.errorReqNoInternetConnection);
            case RC_UNKNOWN_HOST:
                return GlobalInstance.getStringResource(R.string.errorReqUnknownHost);
            case HttpsURLConnection.HTTP_NOT_FOUND:
            case HttpsURLConnection.HTTP_BAD_GATEWAY:
            case RC_SERVER_CONNECTION_FAILED:
                String serverConnectionFailedMessage = GlobalInstance.getStringResource(R.string.errorReqServerConnectionFailed);
                if (returnCode != RC_SERVER_CONNECTION_FAILED) {
                    serverConnectionFailedMessage += String.format(
                            Locale.getDefault(), " (RC=%1$d)", returnCode);
                }
                return serverConnectionFailedMessage;
            case RC_SERVER_CONNECTION_TIMEOUT:
                return GlobalInstance.getStringResource(R.string.errorReqServerConnectionTimeout);
            case RC_SERVER_CONNECTION_UNKNOWN_IO_EXCEPTION:
                return GlobalInstance.getStringResource(R.string.errorReqUnknownIOException);
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
