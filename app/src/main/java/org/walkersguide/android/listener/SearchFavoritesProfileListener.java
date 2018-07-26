package org.walkersguide.android.listener;

import org.walkersguide.android.data.profile.SearchFavoritesProfile;

public interface SearchFavoritesProfileListener {
	public void searchFavoritesProfileRequestFinished(int returnCode, String returnMessage, SearchFavoritesProfile searchFavoritesProfile, boolean resetListPosition);
}
