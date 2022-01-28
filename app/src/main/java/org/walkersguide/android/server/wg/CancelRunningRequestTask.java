package org.walkersguide.android.server.wg;

import org.walkersguide.android.server.ServerUtility;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.server.ServerTask;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.json.JSONObject;
import org.json.JSONException;
import timber.log.Timber;


public class CancelRunningRequestTask extends ServerTask {

    public CancelRunningRequestTask() {
    }

    @Override public void execute() throws WgException {
        Timber.d("CancelRunningRequestTask started");
        JSONObject jsonServerParams = null;
        try {
            jsonServerParams = WgUtility.createServerParamList();
        } catch (JSONException e) {
            throw new WgException(WgException.RC_BAD_REQUEST);
        }

        ServerInstance serverInstance = WgUtility.getServerInstanceForServerUrlFromSettings();
        ServerUtility.performRequestAndReturnString(
                String.format(
                    "%1$s/cancel_request", serverInstance.getServerURL()),
                jsonServerParams,
                WgException.class);

        if (! isCancelled()) {
            ServerTaskExecutor.sendCancelRunningRequestSuccessfulBroadcast(getId());
        }
    }

}
