package org.walkersguide.android.listener;

import org.walkersguide.android.data.profile.NextIntersectionsProfile;

public interface NextIntersectionsListener {
	public void nextIntersectionsRequestFinished(int returnCode, String returnMessage, NextIntersectionsProfile nextIntersectionsProfile, boolean resetListPosition);
}
