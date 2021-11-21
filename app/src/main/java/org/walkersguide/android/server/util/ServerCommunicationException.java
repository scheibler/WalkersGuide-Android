package org.walkersguide.android.server.util;

import android.content.Context;
import android.content.Context;

import org.walkersguide.android.util.GlobalInstance;


public class ServerCommunicationException extends Exception {

    private int returnCode;

    public ServerCommunicationException(int returnCode) {
        super(ServerUtility.getErrorMessageForReturnCode(GlobalInstance.getContext(), returnCode));
        this.returnCode = returnCode;
    }

    public ServerCommunicationException(Context context, int returnCode) {
        super(ServerUtility.getErrorMessageForReturnCode(context, returnCode));
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return this.returnCode;
    }

}
