package org.walkersguide.android.ui.fragment.tabs.overview;

import android.widget.Spinner;
import android.content.Intent;
import org.walkersguide.android.util.WalkersGuideService;
import org.walkersguide.android.util.WalkersGuideService.TrackingMode;
import org.walkersguide.android.ui.adapter.ObjectWithIdAdapter;
import org.walkersguide.android.data.profile.MutableProfile;
import org.walkersguide.android.database.profile.StaticProfile;
import org.walkersguide.android.R;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import android.widget.TextView;
import android.widget.AbsListView;
import java.util.concurrent.Executors;
import org.walkersguide.android.data.ObjectWithId;
import java.util.ArrayList;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.database.DatabaseProfileRequest;
import android.os.Handler;
import android.os.Looper;
import org.walkersguide.android.ui.dialog.select.SelectProfileFromMultipleSourcesDialog;
import org.walkersguide.android.ui.dialog.select.SelectObjectWithIdFromMultipleSourcesDialog;
import androidx.annotation.NonNull;

import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.MainActivityController;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentResultListener;
import android.content.Context;
import timber.log.Timber;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import android.widget.RadioGroup;
import org.walkersguide.android.ui.view.ProfileView;
import android.widget.ListView;
import android.widget.ImageButton;
import android.content.BroadcastReceiver;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;


public class TrackingFragment extends Fragment implements FragmentResultListener {
    private final static String KEY_LIST_POSITION = "listPosition";

	public static TrackingFragment newInstance() {
		TrackingFragment fragment = new TrackingFragment();
		return fragment;
	}


    private SettingsManager settingsManagerInstance;
    private MainActivityController mainActivityController;
    private int listPosition;

    private Spinner spinnerTrackingMode;
    private TextView labelTrackingModeHint;
    private ProfileView layoutTrackedProfile;
    private TextView labelTrackedObjectsHeading;
	private ListView listViewTrackedObjects;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        settingsManagerInstance = SettingsManager.getInstance();

        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            listPosition = 0;
        }

        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectProfileFromMultipleSourcesDialog.REQUEST_SELECT_PROFILE, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectObjectWithIdFromMultipleSourcesDialog.REQUEST_SELECT_OBJECT_WITH_ID, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectProfileFromMultipleSourcesDialog.REQUEST_SELECT_PROFILE)) {
            SelectProfileFromMultipleSourcesDialog.Target profileTarget = (SelectProfileFromMultipleSourcesDialog.Target)
                bundle.getSerializable(SelectProfileFromMultipleSourcesDialog.EXTRA_TARGET);
            Profile selectedProfile = (Profile) bundle.getSerializable(SelectProfileFromMultipleSourcesDialog.EXTRA_PROFILE);
            if (profileTarget == SelectProfileFromMultipleSourcesDialog.Target.SET_AS_TRACKED_PROFILE
                    && selectedProfile instanceof MutableProfile
                    && ((MutableProfile) selectedProfile).setTracked(true)) {
                requestUiUpdate();
            }

        } else if (requestKey.equals(SelectObjectWithIdFromMultipleSourcesDialog.REQUEST_SELECT_OBJECT_WITH_ID)) {
            SelectObjectWithIdFromMultipleSourcesDialog.Target objectWithIdTarget = (SelectObjectWithIdFromMultipleSourcesDialog.Target)
                bundle.getSerializable(SelectObjectWithIdFromMultipleSourcesDialog.EXTRA_TARGET);
            ObjectWithId selectedObjectWithId = (ObjectWithId) bundle.getSerializable(SelectObjectWithIdFromMultipleSourcesDialog.EXTRA_OBJECT_WITH_ID);
            if (objectWithIdTarget == SelectObjectWithIdFromMultipleSourcesDialog.Target.ADD_TO_TRACKED_OBJECTS
                    && StaticProfile.trackedPoints().addObject(selectedObjectWithId)) {
                requestUiUpdate();
            }
        }
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            if (activity instanceof MainActivity) {
                mainActivityController = (MainActivityController) ((MainActivity) activity);
            }
        }
    }

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_tracking, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        spinnerTrackingMode = (Spinner) view.findViewById(R.id.spinnerTrackingMode);
        spinnerTrackingMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView parent) {
            }
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                WalkersGuideService.setTrackingMode(
                        (TrackingMode) parent.getItemAtPosition(position));
            }
        });
        ArrayAdapter<TrackingMode> trackingModeAdapter = new ArrayAdapter<TrackingMode>(
                TrackingFragment.this.getContext(),
                // layout for the collapsed state
                android.R.layout.simple_list_item_1,
                TrackingMode.values());
        // layout for the expanded/opened state
        trackingModeAdapter.setDropDownViewResource(R.layout.layout_single_text_view_checkbox);
        spinnerTrackingMode.setAdapter(trackingModeAdapter);

        labelTrackingModeHint = (TextView) view.findViewById(R.id.labelTrackingModeHint);
        labelTrackingModeHint.setText("");

        layoutTrackedProfile = (ProfileView) view.findViewById(R.id.layoutTrackedProfile);
        layoutTrackedProfile.setOnProfileDefaultActionListener(new ProfileView.OnProfileDefaultActionListener() {
            @Override public void onProfileDefaultActionClicked(Profile profile) {
                SelectProfileFromMultipleSourcesDialog.newInstance(
                        SelectProfileFromMultipleSourcesDialog.Target.SET_AS_TRACKED_PROFILE)
                    .show(getChildFragmentManager(), "SelectProfileFromMultipleSourcesDialog");
            }
        });

        labelTrackedObjectsHeading = (TextView) view.findViewById(R.id.labelHeading);
        ImageButton buttonAddObjectWithId = (ImageButton) view.findViewById(R.id.buttonAdd);
        buttonAddObjectWithId.setContentDescription(
                GlobalInstance.getStringResource(R.string.buttonAddTrackedPoint));
        buttonAddObjectWithId.setVisibility(View.VISIBLE);
        buttonAddObjectWithId.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                SelectObjectWithIdFromMultipleSourcesDialog.newInstance(
                        SelectObjectWithIdFromMultipleSourcesDialog.Target.ADD_TO_TRACKED_OBJECTS)
                    .show(getChildFragmentManager(), "SelectObjectWithIdFromMultipleSourcesDialog");
            }
        });

        listViewTrackedObjects = (ListView) view.findViewById(R.id.listView);
        TextView labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setText(
                GlobalInstance.getStringResource(R.string.labelNoTrackedObjectsHint));
        listViewTrackedObjects.setEmptyView(labelEmptyListView);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_LIST_POSITION, listPosition);
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(WalkersGuideService.ACTION_TRACKING_MODE_CHANGED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);

        WalkersGuideService.requestTrackingMode();
        requestUiUpdate();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WalkersGuideService.ACTION_TRACKING_MODE_CHANGED)) {
                TrackingMode selectedTrackingMode = (TrackingMode) intent.getSerializableExtra(WalkersGuideService.EXTRA_TRACKING_MODE);
                if (selectedTrackingMode != null) {
                    spinnerTrackingMode.setContentDescription(
                            String.format(
                                "%1$s: %2$s",
                                context.getResources().getString(R.string.spinnerTrackingModeCD),
                                selectedTrackingMode.name)
                            );
                    labelTrackingModeHint.setText(selectedTrackingMode.hint);

                    int index = 0;
                    for (TrackingMode trackingMode : TrackingMode.values()) {
                        if (trackingMode == selectedTrackingMode) {
                            spinnerTrackingMode.setSelection(index);
                            break;
                        }
                        index += 1;
                    }
                }
            }
        }
    };

    private void requestUiUpdate() {
        layoutTrackedProfile.configureAsSingleObject(
                settingsManagerInstance.getTrackedProfile());

        labelTrackedObjectsHeading.setText(
                GlobalInstance.getPluralResource(R.plurals.trackedObject, 0));
        listViewTrackedObjects.setAdapter(null);
        listViewTrackedObjects.setOnScrollListener(null);

        Executors.newSingleThreadExecutor().execute(() -> {
            final ArrayList<ObjectWithId> objectList = AccessDatabase
                .getInstance()
                .getObjectListFor(
                        new DatabaseProfileRequest(StaticProfile.trackedPoints()));
            (new Handler(Looper.getMainLooper())).post(() -> {
                if (isAdded()) {
                    if (! objectList.isEmpty()) {
                        loadTrackedObjectsSuccessful(objectList);
                    }
                }
            });
        });
    }

    private void loadTrackedObjectsSuccessful(ArrayList<ObjectWithId> objectList) {
        labelTrackedObjectsHeading.setText(
                GlobalInstance.getPluralResource(
                    R.plurals.trackedObject, objectList.size()));

        listViewTrackedObjects.setAdapter(
                new ObjectWithIdAdapter(
                    TrackingFragment.this.getContext(), objectList, null, StaticProfile.trackedPoints(), true, false));

        // list position
        listViewTrackedObjects.setSelection(listPosition);
        listViewTrackedObjects.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });
    }

}
