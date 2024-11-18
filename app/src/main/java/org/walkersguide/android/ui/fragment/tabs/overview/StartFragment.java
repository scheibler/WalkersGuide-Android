package org.walkersguide.android.ui.fragment.tabs.overview;

import org.walkersguide.android.ui.dialog.create.ImportGpxFileDialog;
import org.walkersguide.android.ui.dialog.create.PointFromCoordinatesLinkDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.data.profile.MutableProfile;
import org.walkersguide.android.ui.adapter.PinnedObjectsAdapter;
import org.walkersguide.android.ui.adapter.PinnedObjectsAdapter.OnAddButtonClick;
import org.walkersguide.android.database.profile.StaticProfile;
import androidx.core.view.MenuProvider;
import org.walkersguide.android.R;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.fragment.app.FragmentResultListener;
import android.content.Context;
import androidx.lifecycle.Lifecycle;
import android.widget.ExpandableListView;
import android.content.BroadcastReceiver;
import org.walkersguide.android.data.Profile;
import android.widget.BaseExpandableListAdapter;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.ui.dialog.create.SaveCurrentLocationDialog;
import android.widget.Button;
import org.walkersguide.android.ui.view.ResolveCurrentAddressView;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.ui.fragment.profile_list.CollectionListFragment;
import org.walkersguide.android.ui.fragment.HistoryFragment;
import timber.log.Timber;
import org.walkersguide.android.ui.dialog.create.EnterAddressDialog;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.server.wg.p2p.P2pRouteRequest;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.sensor.PositionManager;
import android.widget.Toast;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.fragment.tabs.ObjectDetailsTabLayoutFragment;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;


public class StartFragment extends BaseOverviewFragment
    implements FragmentResultListener, MenuProvider, OnRefreshListener {

    public static StartFragment newInstance() {
        StartFragment fragment = new StartFragment();
        return fragment;
    }


    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChildFragmentManager()
            .setFragmentResultListener(
                    EnterAddressDialog.REQUEST_ENTER_ADDRESS, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    PointFromCoordinatesLinkDialog.REQUEST_FROM_COORDINATES_LINK, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ImportGpxFileDialog.REQUEST_IMPORT_OF_GPX_FILE_WAS_SUCCESSFUL, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(EnterAddressDialog.REQUEST_ENTER_ADDRESS)) {
            prepareRequestAndCalculateRoute(
                    (StreetAddress) bundle.getSerializable(EnterAddressDialog.EXTRA_STREET_ADDRESS));
        } else if (requestKey.equals(PointFromCoordinatesLinkDialog.REQUEST_FROM_COORDINATES_LINK)) {
            Point sharedLocation = (Point) bundle.getSerializable(PointFromCoordinatesLinkDialog.EXTRA_COORDINATES);
            if (sharedLocation != null) {
                mainActivityController.addFragment(
                        ObjectDetailsTabLayoutFragment.details(sharedLocation));
            }
        } else if (requestKey.equals(ImportGpxFileDialog.REQUEST_IMPORT_OF_GPX_FILE_WAS_SUCCESSFUL)) {
            DatabaseProfile profileFromGpxFileImport = (DatabaseProfile) bundle.getSerializable(ImportGpxFileDialog.EXTRA_GPX_FILE_PROFILE);
            if (profileFromGpxFileImport != null) {
                mainActivityController.addFragment(
                        CollectionListFragment.newInstance());
                mainActivityController.addFragment(
                        ObjectListFromDatabaseFragment.newInstance(profileFromGpxFileImport));
            }
        }
    }

    private void prepareRequestAndCalculateRoute(Point destination) {
        P2pRouteRequest p2pRouteRequest = P2pRouteRequest.getDefault();

        // start point
        Point currentLocation = PositionManager.getInstance().getCurrentLocation();
        if (currentLocation != null) {
            p2pRouteRequest.setStartPoint(currentLocation);
        } else {
            showNoLocationFoundToast();
            return;
        }

        // destination point
        if (destination != null) {
            p2pRouteRequest.setDestinationPoint(destination);
        } else {
            return;
        }

        SettingsManager.getInstance().setP2pRouteRequest(p2pRouteRequest);
        mainActivityController.openPlanRouteDialog(true);
    }


    /**
     * view creation
     */
    private SwipeRefreshLayout swipeRefreshResolveCurrentAddress;
    private ResolveCurrentAddressView layoutClosestAddress;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_start, container, false);
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        swipeRefreshResolveCurrentAddress = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshResolveCurrentAddress);
        swipeRefreshResolveCurrentAddress.setOnRefreshListener(this);
        layoutClosestAddress = (ResolveCurrentAddressView) view.findViewById(R.id.layoutClosestAddress);

        Button buttonNavigateToAddress = (Button) view.findViewById(R.id.buttonNavigateToAddress);
        buttonNavigateToAddress.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Helper.startSpeechRecognition(
                        StartFragment.this,
                        SPEECH_INPUT_REQUEST_ENTER_ADDRESS,
                        GlobalInstance.getStringResource(R.string.enterAddressDialogName));
            }
        });

        Button buttonNavigateHome = (Button) view.findViewById(R.id.buttonNavigateHome);
        buttonNavigateHome.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Point homeAddress = SettingsManager.getInstance().getHomeAddress();
                if (homeAddress == null) {
                    Toast.makeText(
                            getActivity(),
                            GlobalInstance.getStringResource(R.string.errorNoHomeAddressSet),
                            Toast.LENGTH_LONG)
                        .show();
                } else {
                    prepareRequestAndCalculateRoute(homeAddress);
                }
            }
        });

        Button buttonSaveCurrentLocation = (Button) view.findViewById(R.id.buttonSaveCurrentLocation);
        buttonSaveCurrentLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SaveCurrentLocationDialog.addToDatabaseProfile()
                    .show(getChildFragmentManager(), "SaveCurrentLocationDialog");
            }
        });

        Button buttonShareLocation = (Button) view.findViewById(R.id.buttonShareLocation);
        buttonShareLocation.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Point currentLocation = PositionManager.getInstance().getCurrentLocation();
                if (currentLocation != null) {
                    currentLocation.startShareCoordinatesChooserActivity(
                            getActivity(), Point.SharingService.OSM_ORG);
                } else {
                    showNoLocationFoundToast();
                }
            }
        });

        Button buttonLoadSharedLocation = (Button) view.findViewById(R.id.buttonLoadSharedLocation);
        buttonLoadSharedLocation.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                PointFromCoordinatesLinkDialog.newInstance()
                    .show(getChildFragmentManager(), "PointFromCoordinatesLinkDialog");
            }
        });

        Button buttonOpenGpxFile = (Button) view.findViewById(R.id.buttonOpenGpxFile);
        buttonOpenGpxFile.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                ImportGpxFileDialog.newInstance(null, false)
                    .show(getChildFragmentManager(), "ImportGpxFileDialog");
            }
        });

        Button buttonRecordRoute = (Button) view.findViewById(R.id.buttonRecordRoute);
        buttonRecordRoute.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                MainActivity.startRouteRecording(getActivity());
            }
        });

        Button buttonCollections = (Button) view.findViewById(R.id.buttonCollections);
        buttonCollections.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.addFragment(
                        CollectionListFragment.newInstance());
            }
        });

        Button buttonHistory = (Button) view.findViewById(R.id.buttonHistory);
        buttonHistory.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.addFragment(
                        HistoryFragment.newInstance());
            }
        });
    }

    @Override public void onPause() {
        super.onPause();
    }

    @Override public void onResume() {
        super.onResume();
        updateClosestAddress();
    }

    @Override public void onRefresh() {
        Timber.d("onRefresh");
        updateClosestAddress();
        Helper.vibrateOnce(
                Helper.VIBRATION_DURATION_SHORT, Helper.VIBRATION_INTENSITY_WEAK);
        swipeRefreshResolveCurrentAddress.setRefreshing(false);
    }

    public void requestUiUpdate() {
        updateClosestAddress();
    }

    private void updateClosestAddress() {
        layoutClosestAddress.requestAddressForCurrentLocation();
    }

    private void showNoLocationFoundToast() {
    Toast.makeText(
            getActivity(),
            GlobalInstance.getStringResource(R.string.errorNoLocationFound),
            Toast.LENGTH_LONG)
        .show();
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_start_fragment, menu);
    }

    @Override public boolean onMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemRefresh) {
            updateClosestAddress();
        } else {
            return false;
        }
        return true;
    }


    /**
     * speech recognition results
     */
    private final static int SPEECH_INPUT_REQUEST_ENTER_ADDRESS = 7855;

    @Override public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == SPEECH_INPUT_REQUEST_ENTER_ADDRESS) {
            String addressString = Helper.extractSpeechRecognitionResult(resultCode, resultData);
            if (addressString != null) {
                Timber.d("address string: %1$s", addressString);
                EnterAddressDialog.newInstance(addressString)
                    .show(getChildFragmentManager(), "EnterAddressDialog");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, resultData);
        }
    }

}
