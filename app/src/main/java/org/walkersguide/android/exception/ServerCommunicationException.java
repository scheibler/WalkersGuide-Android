package org.walkersguide.android.exception;

import java.io.IOException;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;
import android.content.Context;
import org.walkersguide.android.helper.DownloadUtility;


public class ServerCommunicationException extends Exception {

    private int returnCode;

    public ServerCommunicationException(Context context, int returnCode) {
        super(DownloadUtility.getErrorMessageForReturnCode(context, returnCode, ""));
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return this.returnCode;
    }

}
