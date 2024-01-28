package org.walkersguide.android.ui.fragment.tabs.overview;

import timber.log.Timber;
import org.walkersguide.android.data.profile.AnnouncementRadius;
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
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.ui.view.ProfileView;
import android.widget.ListView;
import android.widget.ImageButton;
import android.content.BroadcastReceiver;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.text.TextUtils;
import androidx.core.view.MenuProvider;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.lifecycle.Lifecycle;


public class TrackingFragment extends BaseOverviewFragment implements FragmentResultListener, MenuProvider {

    public static TrackingFragment newInstance() {
        TrackingFragment fragment = new TrackingFragment();
        return fragment;
    }


    private SettingsManager settingsManagerInstance;

    private Spinner spinnerTrackingMode;
    private TextView labelTrackingModeHint;
    // profile
    private ProfileView layoutTrackedProfile;
    private Spinner spinnerAnnouncementRadius;
    // tracked objects
    private TextView labelTrackedObjectsHeading;
    private ListView listViewTrackedObjects;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsManagerInstance = SettingsManager.getInstance();

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
                    && StaticProfile.trackedObjectsWithId().addObject(selectedObjectWithId)) {
                requestUiUpdate();
            }
        }
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tracking, container, false);
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        spinnerTrackingMode = (Spinner) view.findViewById(R.id.spinnerTrackingMode);
        ArrayAdapter<TrackingMode> trackingModeAdapter = new ArrayAdapter<TrackingMode>(
                TrackingFragment.this.getContext(),
                // layout for the collapsed state
                android.R.layout.simple_list_item_1,
                TrackingMode.values());
        // layout for the expanded/opened state
        trackingModeAdapter.setDropDownViewResource(R.layout.layout_single_text_view_checkbox);
        spinnerTrackingMode.setAdapter(trackingModeAdapter);
        spinnerTrackingMode.setOnItemSelectedListener(null);

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

        spinnerAnnouncementRadius = (Spinner) view.findViewById(R.id.spinnerAnnouncementRadius);
        ArrayAdapter<AnnouncementRadius> announcementRadiusAdapter = new ArrayAdapter<AnnouncementRadius>(
                TrackingFragment.this.getContext(),
                // layout for the collapsed state
                android.R.layout.simple_list_item_1,
                AnnouncementRadius.values());
        // layout for the expanded/opened state
        announcementRadiusAdapter.setDropDownViewResource(R.layout.layout_single_text_view_checkbox);
        spinnerAnnouncementRadius.setAdapter(announcementRadiusAdapter);
        // select
        spinnerAnnouncementRadius.setSelection(
                AnnouncementRadius.values().indexOf(
                    settingsManagerInstance.getTrackingModeAnnouncementRadius()));
        // must come after adapter and selection
        spinnerAnnouncementRadius.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView parent) {
            }
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                AnnouncementRadius selectedRadius = (AnnouncementRadius) parent.getItemAtPosition(position);
                if (selectedRadius != null
                        && ! selectedRadius.equals(settingsManagerInstance.getTrackingModeAnnouncementRadius())) {
                    settingsManagerInstance.setTrackingModeAnnouncementRadius(selectedRadius);
                }
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


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_tracking_fragment, menu);
    }

    @Override public boolean onMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemClearProfile) {
            AccessDatabase.getInstance().clearDatabaseProfile(StaticProfile.trackedObjectsWithId());
            requestUiUpdate();
        } else {
            return false;
        }
        return true;
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
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WalkersGuideService.ACTION_TRACKING_MODE_CHANGED)) {
                TrackingMode selectedTrackingMode = (TrackingMode) intent.getSerializableExtra(WalkersGuideService.EXTRA_TRACKING_MODE);
                Timber.d("onReceive action=ACTION_TRACKING_MODE_CHANGED, selected=%1$s", selectedTrackingMode);
                if (selectedTrackingMode != null) {
                    spinnerTrackingMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onNothingSelected(AdapterView parent) {
                        }
                        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            Timber.d("onItemSelected: %1$s", (TrackingMode) parent.getItemAtPosition(position));
                            WalkersGuideService.setTrackingMode(
                                    (TrackingMode) parent.getItemAtPosition(position), false);
                        }
                    });
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

    public void requestUiUpdate() {
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
                        new DatabaseProfileRequest(StaticProfile.trackedObjectsWithId()));
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
                    TrackingFragment.this.getContext(), objectList, null, StaticProfile.trackedObjectsWithId(), true, false));

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
