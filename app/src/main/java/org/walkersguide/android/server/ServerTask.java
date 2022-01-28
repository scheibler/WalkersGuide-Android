package org.walkersguide.android.server;

import timber.log.Timber;


public abstract class ServerTask {

    private final long id = System.currentTimeMillis();

    public ServerTask() {
    }

    public long getId() {
        return this.id;
    }

    public boolean isCancelled() {
        return ServerTaskExecutor.getInstance().taskIsCancelled(this.id);
    }

    public abstract <T extends ServerException> void execute() throws T;

}
