package org.walkersguide.android.ui.fragment;

import org.walkersguide.android.R;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.poi.POIManager;
import org.walkersguide.android.poi.POIProfile;
import org.walkersguide.android.poi.PointListObject;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.POISettings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class POIFragment extends Fragment implements FragmentCommunicator {

	// Store instance variables
	private SettingsManager settingsManagerInstance;

    // query in progress
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;

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
        // settings manager
		settingsManagerInstance = SettingsManager.getInstance(context);
        // tts
        this.progressHandler = new Handler();
        this.progressUpdater = new ProgressUpdater();
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
        // request poi profile
        listViewPOI.setAdapter(null);
        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_POI_PROFILE_UPDATED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // enable progress updater
        progressHandler.postDelayed(progressUpdater, 2000);
        // start request
        POIManager.getInstance(getActivity()).requestPOIProfile(
                settingsManagerInstance.getPOISettings().getSelectedPOIProfileId());
        Toast.makeText(
                getActivity(),
                getResources().getString(R.string.messagePleaseWait),
                Toast.LENGTH_LONG).show();
    }

	@Override public void onFragmentDisabled() {
        // unregister broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        // disable progress updater
        progressHandler.removeCallbacks(progressUpdater);
        // save current list position
        POISettings poiSettings = settingsManagerInstance.getPOISettings();
        poiSettings.setSelectedPositionInPOIList(
                listViewPOI.getFirstVisiblePosition());
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_POI_PROFILE_UPDATED)) {
                // unregister broadcast receiver
                LocalBroadcastManager.getInstance(context).unregisterReceiver(mMessageReceiver);
                // disable progress updater
                progressHandler.removeCallbacks(progressUpdater);
                // load selected poi profile
                AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(context);
                POIProfile poiProfile = accessDatabaseInstance.getPOIProfile(
                        intent.getIntExtra(Constants.ACTION_POI_PROFILE_UPDATED_ATTR.INT_POI_PROFILE_ID, -1));
                if (poiProfile != null
                        && poiProfile.getPointList() != null) {
                    listViewPOI.setAdapter(
                            new ArrayAdapter<PointListObject>(
                                context,
                                android.R.layout.simple_list_item_1,
                                poiProfile.getPointList())
                            );
                    // list position
                    POISettings poiSettings = SettingsManager.getInstance(context).getPOISettings();
                    if (poiSettings.getSelectedPositionInPOIList() > 0) {
                        listViewPOI.setSelection(
                                poiSettings.getSelectedPositionInPOIList());
                    }
                }
                String returnMessage = intent.getStringExtra(
                        Constants.ACTION_POI_PROFILE_UPDATED_ATTR.STRING_RETURN_MESSAGE);
                if (! returnMessage.equals("")) {
                    Toast.makeText(
                            context, returnMessage, Toast.LENGTH_LONG).show();
                }
            }
        }
    };


    private class ProgressUpdater implements Runnable {
        public void run() {
            //vibrator.vibrate(50);
            progressHandler.postDelayed(this, 2000);
        }
    }

}
