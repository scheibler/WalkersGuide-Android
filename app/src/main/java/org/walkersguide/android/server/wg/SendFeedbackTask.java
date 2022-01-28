package org.walkersguide.android.server.wg;

import org.walkersguide.android.server.ServerUtility;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.server.ServerTask;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.ui.dialog.SendFeedbackDialog.FeedbackToken;
import org.json.JSONObject;
import org.json.JSONException;
import android.text.TextUtils;
import timber.log.Timber;
import java.util.Locale;


public class SendFeedbackTask extends ServerTask {

    private FeedbackToken token;
    private String sender, message;

    public SendFeedbackTask(FeedbackToken token, String sender, String message) {
        this.token = token;
        this.sender = sender;
        this.message = message;
    }

    @Override public void execute() throws WgException {
        Timber.d("SendFeedbackTask started");
        JSONObject jsonServerParams = null;
        try {
            jsonServerParams = WgUtility.createServerParamList();
            // mandatory
            jsonServerParams.put("token", token.name().toLowerCase(Locale.ROOT));
            jsonServerParams.put("message", message);
            // optional
            if (! TextUtils.isEmpty(sender)) {
                jsonServerParams.put("sender", sender);
            }
        } catch (JSONException e) {
            throw new WgException(WgException.RC_BAD_REQUEST);
        }

        ServerInstance serverInstance = WgUtility.getServerInstanceForServerUrlFromSettings();
        ServerUtility.performRequestAndReturnString(
                String.format(
                    "%1$s/send_feedback", serverInstance.getServerURL()),
                jsonServerParams,
                WgException.class);

        Timber.d("task ready, isCancelled: %1$s", isCancelled());
        if (! isCancelled()) {
            ServerTaskExecutor.sendSendFeedbackSuccessfulBroadcast(getId());
        }
    }

}
