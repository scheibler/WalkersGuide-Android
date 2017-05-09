package org.walkersguide.android.listener;

public interface ServerStatusListener {
	public void statusRequestFinished(int updateAction, int returnCode, String returnMessage);
}
