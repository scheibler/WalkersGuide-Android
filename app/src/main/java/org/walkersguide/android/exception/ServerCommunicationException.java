package org.walkersguide.android.exception;

import android.content.Context;

import org.walkersguide.android.helper.ServerUtility;


public class ServerCommunicationException extends Exception {

    private int returnCode;

    public ServerCommunicationException(Context context, int returnCode) {
        super(ServerUtility.getErrorMessageForReturnCode(context, returnCode));
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return this.returnCode;
    }

}
