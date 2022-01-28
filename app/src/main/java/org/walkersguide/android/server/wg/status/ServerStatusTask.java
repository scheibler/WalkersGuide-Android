package org.walkersguide.android.server.wg.status;

import org.walkersguide.android.server.ServerException;
import org.walkersguide.android.server.wg.WgUtility;
import org.walkersguide.android.server.ServerTask;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.util.SettingsManager;


public class ServerStatusTask extends ServerTask {

    private String serverUrl;

    public ServerStatusTask() {
        this.serverUrl = SettingsManager.getInstance().getServerURL();
    }

    public ServerStatusTask(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Override public void execute() throws ServerException {
        ServerInstance serverInstance = WgUtility.getServerInstance(this.serverUrl);
        if (! isCancelled()) {
            ServerTaskExecutor.sendServerStatusTaskSuccessfulBroadcast(getId(), serverInstance);
        }
    }

}
