package org.walkersguide.android.listener;

import org.walkersguide.android.data.server.ServerInstance;


public interface ServerStatusListener {
	public void serverStatusRequestFinished(int returnCode, String returnMessage, ServerInstance serverInstance);
}
