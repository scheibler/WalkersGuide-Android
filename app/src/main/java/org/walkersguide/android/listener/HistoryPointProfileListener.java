package org.walkersguide.android.listener;

import org.walkersguide.android.data.profile.HistoryPointProfile;


public interface HistoryPointProfileListener {
	public void historyPointProfileRequestFinished(int returnCode, String returnMessage, HistoryPointProfile historyPointProfile, boolean resetListPosition);
}
