package org.walkersguide.android.ui;

import org.walkersguide.android.MainActivity;
import org.walkersguide.android.R;
import org.walkersguide.android.interfaces.FragmentCommunicator;
import org.walkersguide.android.utils.GlobalInstance;
import org.walkersguide.android.utils.SettingsManager;
import org.walkersguide.android.utils.TTSWrapper;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

public class POIFragment extends Fragment implements FragmentCommunicator {

	// Store instance variables
	private GlobalInstance globalInstance;
	private SettingsManager settingsManagerInstance;
    private TTSWrapper ttsWrapperInstance;

	// ui components
	private Button buttonSelectPOIProfile;
	private ListView listViewPOI;

	// newInstance constructor for creating fragment with arguments
	public static POIFragment newInstance() {
		POIFragment poiFragmentInstance = new POIFragment();
		return poiFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
			((MainActivity) activity).poiFragmentCommunicator = this;
		}
        // global instance
		globalInstance = ((GlobalInstance) context.getApplicationContext());
        // settings manager
		settingsManagerInstance = SettingsManager.getInstance(context);
        // tts
        ttsWrapperInstance = TTSWrapper.getInstance(context);
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_poi, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        // top layout
        buttonSelectPOIProfile = (Button) view.findViewById(R.id.buttonSelectPOIProfile);
        buttonSelectPOIProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            }
        });

        // content layout
        listViewPOI = (ListView) view.findViewById(R.id.listViewPOI);
        listViewPOI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                //Werk werk = (Werk) parent.getItemAtPosition(position);
            }
        });
        listViewPOI.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                return true;
            }
        });

        // bottom layout
        Button buttonRefresh = (Button) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            }
        });

        Button buttonSearch = (Button) view.findViewById(R.id.buttonSearch);
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            }
        });
    }

    @Override public void onFragmentEnabled() {
    }

	@Override public void onFragmentDisabled() {
    }

}
