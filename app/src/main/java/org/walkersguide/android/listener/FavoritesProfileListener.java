package org.walkersguide.android.listener;

import org.walkersguide.android.data.poi.FavoritesProfile;

public interface FavoritesProfileListener {
	public void favoritesProfileRequestFinished(int returnCode, String returnMessage, FavoritesProfile favoritesProfile);
}
