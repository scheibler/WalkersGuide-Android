package org.walkersguide.android.listener;

import org.walkersguide.android.data.profile.POIProfile;

public interface POIProfileListener {
	public void poiProfileRequestFinished(int returnCode, String returnMessage, POIProfile poiProfile, boolean resetListPosition);
}
