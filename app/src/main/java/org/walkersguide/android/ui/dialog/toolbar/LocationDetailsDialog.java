    package org.walkersguide.android.ui.dialog.toolbar;

    import org.walkersguide.android.ui.view.ObjectWithIdView;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;


import android.os.Bundle;


import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;



import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.dialog.select.SelectObjectWithIdFromMultipleSourcesDialog;
import org.walkersguide.android.ui.dialog.WhereAmIDialog;
import org.walkersguide.android.ui.dialog.create.SaveCurrentLocationDialog;
import org.walkersguide.android.data.object_with_id.Point;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import org.walkersguide.android.util.GlobalInstance;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.ImageButton;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import timber.log.Timber;
import org.walkersguide.android.data.ObjectWithId;


public class LocationDetailsDialog extends DialogFragment implements FragmentResultListener {

    private PositionManager positionManagerInstance;

    private TextView labelGPSCoordinates, labelGPSAccuracy, labelGPSTime;
    private SwitchCompat buttonEnableSimulation;
    private ObjectWithIdView layoutSimulationPoint;

    public static LocationDetailsDialog newInstance() {
        LocationDetailsDialog dialog = new LocationDetailsDialog();
        return dialog;
    }

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        positionManagerInstance = PositionManager.getInstance();
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectObjectWithIdFromMultipleSourcesDialog.REQUEST_SELECT_OBJECT_WITH_ID, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectObjectWithIdFromMultipleSourcesDialog.REQUEST_SELECT_OBJECT_WITH_ID)) {
            SelectObjectWithIdFromMultipleSourcesDialog.Target objectWithIdTarget = (SelectObjectWithIdFromMultipleSourcesDialog.Target)
                bundle.getSerializable(SelectObjectWithIdFromMultipleSourcesDialog.EXTRA_TARGET);
            ObjectWithId selectedObjectWithId = (ObjectWithId) bundle.getSerializable(SelectObjectWithIdFromMultipleSourcesDialog.EXTRA_OBJECT_WITH_ID);
            if (objectWithIdTarget == SelectObjectWithIdFromMultipleSourcesDialog.Target.SIMULATE_LOCATION
                    && selectedObjectWithId instanceof Point) {
                positionManagerInstance.setSimulatedLocation((Point) selectedObjectWithId);
                buttonEnableSimulation.setChecked(true);
                updateSimulationPoint();
            }
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_location_details, nullParent);

        ImageButton buttonActionForCurrentLocation = (ImageButton) view.findViewById(R.id.buttonActionForCurrentLocation);
        buttonActionForCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Point currentSensorLocation = positionManagerInstance.getGPSLocation();
                if (currentSensorLocation != null) {
                    showContextMenuForCurrentLocation(view, currentSensorLocation);
                } else {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(R.string.errorNoLocationFound),
                            Toast.LENGTH_LONG)
                        .show();
                }
            }
        });

        // location sensor details
        labelGPSCoordinates = (TextView) view.findViewById(R.id.labelGPSCoordinates);
        labelGPSAccuracy = (TextView) view.findViewById(R.id.labelGPSAccuracy);
        labelGPSTime = (TextView) view.findViewById(R.id.labelGPSTime);

        // simulated point
        buttonEnableSimulation = (SwitchCompat) view.findViewById(R.id.buttonEnableSimulation);
        buttonEnableSimulation.setChecked(positionManagerInstance.getSimulationEnabled());
        buttonEnableSimulation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (positionManagerInstance.getSimulationEnabled() != isChecked) {
                    // check or uncheck simulation
                    if (isChecked && positionManagerInstance.getCurrentLocation() == null) {
                        // no simulated point selected
                        Toast.makeText(
                                getActivity(),
                                getResources().getString(R.string.labelNothingSelected),
                                Toast.LENGTH_LONG).show();
                        positionManagerInstance.setSimulationEnabled(false);
                        buttonEnableSimulation.setChecked(false);
                    } else {
                        positionManagerInstance.setSimulationEnabled(isChecked);
                    }
                }
            }
        });

        layoutSimulationPoint = (ObjectWithIdView) view.findViewById(R.id.layoutSimulationPoint);
        layoutSimulationPoint.setOnDefaultObjectActionListener(new ObjectWithIdView.OnDefaultObjectActionListener() {
            @Override public void onDefaultObjectActionClicked(ObjectWithId objectWithId) {
                SelectObjectWithIdFromMultipleSourcesDialog.newInstance(
                        SelectObjectWithIdFromMultipleSourcesDialog.Target.SIMULATE_LOCATION)
                    .show(getChildFragmentManager(), "SelectObjectWithIdFromMultipleSourcesDialog");
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.locationDetailsDialogTitle))
            .setView(view)
            .setNeutralButton(
                    getResources().getString(R.string.whereAmIDialogTitle),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogClose),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {

            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    WhereAmIDialog.newInstance()
                        .show(getChildFragmentManager(), "WhereAmIDialog");
                }
            });

            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(PositionManager.ACTION_NEW_GPS_LOCATION);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // request location
        positionManagerInstance.requestGPSLocation();
        // update simulation point button
        updateSimulationPoint();
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    private void updateSimulationPoint() {
        layoutSimulationPoint.configureAsSingleObject(positionManagerInstance.getSimulatedLocation());
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PositionManager.ACTION_NEW_GPS_LOCATION)) {

                // clear fields
                labelGPSCoordinates.setText(context.getResources().getString(R.string.labelGPSCoordinates));
                labelGPSAccuracy.setText(context.getResources().getString(R.string.labelGPSAccuracy));
                labelGPSTime.setText(context.getResources().getString(R.string.labelGPSTime));

                // get gps location
                GPS gpsLocation = (GPS) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION);
                if (gpsLocation != null) {

                    // fill labels
                    labelGPSCoordinates.setText(gpsLocation.formatCoordinates());
                    labelGPSAccuracy.setText(gpsLocation.formatAccuracyInMeters());
                    labelGPSTime.setText(gpsLocation.formatTimestamp());
                }
            }
        }
    };


    /**
     * current location context menu
     */
    private static final int MENU_ITEM_DETAILS = 1;
    private static final int MENU_ITEM_PIN = 2;
    private static final int MENU_ITEM_SHARE = 3;

    private void showContextMenuForCurrentLocation(final View view, final Point currentLocation) {
        PopupMenu contextMenu = new PopupMenu(view.getContext(), view);
        // details
        contextMenu.getMenu().add(
                Menu.NONE, MENU_ITEM_DETAILS, 1, GlobalInstance.getStringResource(R.string.contextMenuItemDetails));
        // pin
        contextMenu.getMenu().add(
                Menu.NONE, MENU_ITEM_PIN, 2, GlobalInstance.getStringResource(R.string.currentLocationMenuItemAddToPinnedPoints));
        // share
        SubMenu shareCoordinatesSubMenu = contextMenu.getMenu().addSubMenu(
                Menu.NONE, Menu.NONE, 4, GlobalInstance.getStringResource(R.string.contextMenuItemObjectWithIdShareCoordinates));
        Point.populateShareCoordinatesSubMenuEntries(shareCoordinatesSubMenu);

        contextMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == MENU_ITEM_DETAILS) {
                    LocationSensorDetailsDialog.newInstance()
                        .show(getChildFragmentManager(), "LocationSensorDetailsDialog");
                } else if (item.getItemId() == MENU_ITEM_PIN) {
                    SaveCurrentLocationDialog.addToDatabaseProfile()
                        .show(getChildFragmentManager(), "SaveCurrentLocationDialog");
                } else if (item.getItemId() == Point.MENU_ITEM_SHARE_APPLE_MAPS_LINK) {
                    currentLocation.startShareCoordinatesChooserActivity(
                            getActivity(), Point.SharingService.APPLE_MAPS);
                } else if (item.getItemId() == Point.MENU_ITEM_SHARE_GOOGLE_MAPS_LINK) {
                    currentLocation.startShareCoordinatesChooserActivity(
                            getActivity(), Point.SharingService.GOOGLE_MAPS);
                } else if (item.getItemId() == Point.MENU_ITEM_SHARE_OSM_ORG_LINK) {
                    currentLocation.startShareCoordinatesChooserActivity(
                            getActivity(), Point.SharingService.OSM_ORG);
                } else {
                    return false;
                }
                return true;
            }
        });

        contextMenu.show();
    }

}
