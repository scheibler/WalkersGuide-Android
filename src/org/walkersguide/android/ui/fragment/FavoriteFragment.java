package org.walkersguide.android.ui.fragment;

import org.walkersguide.android.R;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.util.SettingsManager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FavoriteFragment extends Fragment implements FragmentCommunicator {

	// Store instance variables
	private SettingsManager settingsManagerInstance;

	// ui components

	// newInstance constructor for creating fragment with arguments
	public static FavoriteFragment newInstance() {
		FavoriteFragment favoriteFragmentInstance = new FavoriteFragment();
		return favoriteFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
			((MainActivity) activity).favoriteFragmentCommunicator = this;
		}
        // settings manager
		settingsManagerInstance = SettingsManager.getInstance(context);
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_favorite, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
    }

    @Override public void onFragmentEnabled() {
    }

	@Override public void onFragmentDisabled() {
    }

}
