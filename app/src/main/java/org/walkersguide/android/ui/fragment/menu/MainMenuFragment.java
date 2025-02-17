package org.walkersguide.android.ui.fragment.menu;

import android.content.ActivityNotFoundException;
import org.walkersguide.android.ui.dialog.SendFeedbackDialog;
import org.walkersguide.android.ui.fragment.menu.InfoFragment;
import org.walkersguide.android.ui.fragment.menu.SettingsFragment;

import org.walkersguide.android.ui.dialog.gpx.OpenGpxFileDialog;
import org.walkersguide.android.ui.dialog.create.PointFromCoordinatesLinkDialog;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.R;

import android.os.Bundle;

import android.view.View;

import androidx.annotation.NonNull;

import androidx.fragment.app.FragmentResultListener;
import android.content.Intent;
import org.walkersguide.android.ui.dialog.create.SaveCurrentLocationDialog;
import android.widget.Button;
import org.walkersguide.android.ui.view.ResolveCurrentAddressView;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.ui.fragment.profile_list.CollectionListFragment;
import org.walkersguide.android.ui.fragment.tabs.overview.HistoryFragment;
import org.walkersguide.android.ui.dialog.create.EnterAddressDialog;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.server.wg.p2p.P2pRouteRequest;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.sensor.PositionManager;
import android.widget.Toast;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.ui.activity.MainActivity;
import android.net.Uri;
import org.walkersguide.android.ui.fragment.RootFragment;


public class MainMenuFragment extends RootFragment implements FragmentResultListener {

    public static MainMenuFragment newInstance() {
        MainMenuFragment fragment = new MainMenuFragment();
        return fragment;
    }


    /**
     * create view
     */
    private ResolveCurrentAddressView layoutClosestAddress;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChildFragmentManager()
            .setFragmentResultListener(
                    EnterAddressDialog.REQUEST_ENTER_ADDRESS, this, this);
    }

    @Override public String getTitle() {
        return getResources().getString(R.string.mainMenuFragmentTitle);
    }

    @Override public int getLayoutResourceId() {
        return R.layout.fragment_main_menu;
    }

    @Override public View configureView(View view, Bundle savedInstanceState) {
        // new route menu items

        Button mainMenuItemNavigateToAddress = (Button) view.findViewById(R.id.mainMenuItemNavigateToAddress);
        mainMenuItemNavigateToAddress.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
                    MainMenuFragment.this.startActivityForResult(
                            Helper.getSpeechRecognitionIntent(
                                GlobalInstance.getStringResource(R.string.enterAddressDialogName)),
                            SPEECH_INPUT_REQUEST_ENTER_ADDRESS);
                } catch (ActivityNotFoundException a) {
                    EnterAddressDialog.newInstance(true)
                        .show(getChildFragmentManager(), "EnterAddressDialog");
                }
            }
        });

        Button mainMenuItemNavigateHome = (Button) view.findViewById(R.id.mainMenuItemNavigateHome);
        mainMenuItemNavigateHome.setOnClickListener(new View.OnClickListener() {
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

        Button mainMenuItemPlanRoute = (Button) view.findViewById(R.id.mainMenuItemPlanRoute);
        mainMenuItemPlanRoute.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.openPlanRouteDialog(false);
            }
        });

        Button mainMenuItemRecordRoute = (Button) view.findViewById(R.id.mainMenuItemRecordRoute);
        mainMenuItemRecordRoute.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                MainActivity.startRouteRecording(getActivity());
            }
        });

        Button mainMenuItemOpenGpxFile = (Button) view.findViewById(R.id.mainMenuItemOpenGpxFile);
        mainMenuItemOpenGpxFile.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.openDialog(
                        OpenGpxFileDialog.newInstance(null));
            }
        });

        // current location menu items

        layoutClosestAddress = (ResolveCurrentAddressView) view.findViewById(R.id.layoutClosestAddress);

        Button mainMenuItemSaveCurrentLocation = (Button) view.findViewById(R.id.mainMenuItemSaveCurrentLocation);
        mainMenuItemSaveCurrentLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mainActivityController.openDialog(
                        SaveCurrentLocationDialog.addToDatabaseProfile());
            }
        });

        Button mainMenuItemShareCurrentLocation = (Button) view.findViewById(R.id.mainMenuItemShareCurrentLocation);
        mainMenuItemShareCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Point currentLocation = PositionManager.getInstance().getCurrentLocation();
                if (currentLocation != null) {
                    currentLocation.startShareCoordinatesChooserActivity(
                            getActivity(), Point.SharingService.OSM_ORG);
                    // menu must be closed afterwards
                    mainActivityController.closeMainMenu();
                } else {
                    showNoLocationFoundToast();
                }
            }
        });

        Button mainMenuItemOpenSharedLocation = (Button) view.findViewById(R.id.mainMenuItemOpenSharedLocation);
        mainMenuItemOpenSharedLocation.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.openDialog(
                        PointFromCoordinatesLinkDialog.newInstance());
            }
        });

        // miscellaneous menu items

        Button mainMenuItemCollections = (Button) view.findViewById(R.id.mainMenuItemCollections);
        mainMenuItemCollections.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.embeddFragmentIfPossibleElseOpenAsDialog(
                        CollectionListFragment.newInstance());
            }
        });

        Button mainMenuItemHistory = (Button) view.findViewById(R.id.mainMenuItemHistory);
        mainMenuItemHistory.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.embeddFragmentIfPossibleElseOpenAsDialog(
                        HistoryFragment.newInstance());
            }
        });

        Button mainMenuItemSettings = (Button) view.findViewById(R.id.mainMenuItemSettings);
        mainMenuItemSettings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.embeddFragmentIfPossibleElseOpenAsDialog(
                        SettingsFragment.newInstance());
            }
        });

        Button mainMenuItemInfo = (Button) view.findViewById(R.id.mainMenuItemInfo);
        mainMenuItemInfo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.embeddFragmentIfPossibleElseOpenAsDialog(
                        InfoFragment.newInstance());
            }
        });

        Button mainMenuItemContactMe = (Button) view.findViewById(R.id.mainMenuItemContactMe);
        mainMenuItemContactMe.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.openDialog(
                        SendFeedbackDialog.newInstance(
                            SendFeedbackDialog.FeedbackToken.QUESTION));
            }
        });

        Button mainMenuItemUserManual = (Button) view.findViewById(R.id.mainMenuItemUserManual);
        mainMenuItemUserManual.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent openBrowserIntent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            getResources().getString(R.string.variableUserManualUrl)));
                getActivity().startActivity(openBrowserIntent);
                // menu must be closed afterwards
                mainActivityController.closeMainMenu();
            }
        });

        return view;
    }

    @Override public void onResume() {
        super.onResume();
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
     * navigate home / to address
     */

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(EnterAddressDialog.REQUEST_ENTER_ADDRESS)) {
            prepareRequestAndCalculateRoute(
                    (StreetAddress) bundle.getSerializable(EnterAddressDialog.EXTRA_STREET_ADDRESS));
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
        // menu should stay open while route calculation takes place
        // if the calculation was successful the menu is closed automatically
        mainActivityController.openPlanRouteDialog(true);
    }


    // speech recognition results for address string
    private final static int SPEECH_INPUT_REQUEST_ENTER_ADDRESS = 7855;

    @Override public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == SPEECH_INPUT_REQUEST_ENTER_ADDRESS) {
            String addressString = Helper.extractSpeechRecognitionResult(resultCode, resultData);
            if (addressString != null) {
                EnterAddressDialog.newInstance(addressString, true)
                    .show(getChildFragmentManager(), "EnterAddressDialog");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, resultData);
        }
    }

}
