package org.walkersguide.android.ui.dialog.select;

import org.walkersguide.android.ui.fragment.ProfileListFragment;
import org.walkersguide.android.ui.fragment.profile_list.PoiProfileListFragment;
import org.walkersguide.android.ui.dialog.create.SaveCurrentLocationDialog;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
import org.walkersguide.android.ui.fragment.object_list.extended.PoiListFromServerFragment;
import org.walkersguide.android.ui.dialog.create.EnterAddressDialog;
import org.walkersguide.android.ui.dialog.create.EnterCoordinatesDialog;
import org.walkersguide.android.ui.dialog.create.PointFromCoordinatesLinkDialog;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;


import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;


import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import java.util.Arrays;
import org.walkersguide.android.util.GlobalInstance;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.ui.fragment.ObjectListFragment;
import org.walkersguide.android.ui.dialog.WhereAmIDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import timber.log.Timber;
import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.ui.fragment.profile_list.CollectionListFragment;
import android.widget.Toast;
import org.walkersguide.android.database.profile.static_profile.HistoryProfile;



public class SelectObjectWithIdFromMultipleSourcesDialog extends DialogFragment implements FragmentResultListener {
    public static final String REQUEST_SELECT_OBJECT_WITH_ID = "selectObjectWithIdFromMultipleSources";
    public static final String EXTRA_OBJECT_WITH_ID = "objectWithIdFromMultipleSources";
    public static final String EXTRA_TARGET = "targetForObjectWithIdFromMultipleSources";


    // instance constructors

    public static SelectObjectWithIdFromMultipleSourcesDialog newInstance(Target target) {
        SelectObjectWithIdFromMultipleSourcesDialog dialog = new SelectObjectWithIdFromMultipleSourcesDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_TARGET, target);
        dialog.setArguments(args);
        return dialog;
    }

    // dialog
    private static final String KEY_TARGET = "target";

    public enum Target {
        ROUTE_START_POINT, ROUTE_VIA_POINT_1, ROUTE_VIA_POINT_2, ROUTE_VIA_POINT_3, ROUTE_DESTINATION_POINT,
        ADD_TO_COLLECTION, ADD_TO_PINNED_POINTS_AND_ROUTES, ADD_TO_TRACKED_OBJECTS, SIMULATE_LOCATION, USE_AS_HOME_ADDRESS
    }

    private Target target;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SaveCurrentLocationDialog.REQUEST_SAVE_CURRENT_LOCATION, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    WhereAmIDialog.REQUEST_RESOLVE_COORDINATES, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    EnterAddressDialog.REQUEST_ENTER_ADDRESS, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    EnterCoordinatesDialog.REQUEST_ENTER_COORDINATES, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ProfileListFragment.REQUEST_SELECT_PROFILE, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    PointFromCoordinatesLinkDialog.REQUEST_FROM_COORDINATES_LINK, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ObjectListFragment.REQUEST_SELECT_OBJECT, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        Timber.d("onFragmentResult: %1$s", requestKey);
        if (requestKey.equals(SaveCurrentLocationDialog.REQUEST_SAVE_CURRENT_LOCATION)) {
            objectWithIdSelected(
                    (GPS) bundle.getSerializable(SaveCurrentLocationDialog.EXTRA_CURRENT_LOCATION));

        } else if (requestKey.equals(WhereAmIDialog.REQUEST_RESOLVE_COORDINATES)) {
            objectWithIdSelected(
                    (StreetAddress) bundle.getSerializable(WhereAmIDialog.EXTRA_STREET_ADDRESS));

        } else if (requestKey.equals(EnterAddressDialog.REQUEST_ENTER_ADDRESS)) {
            objectWithIdSelected(
                    (StreetAddress) bundle.getSerializable(EnterAddressDialog.EXTRA_STREET_ADDRESS));

        } else if (requestKey.equals(EnterCoordinatesDialog.REQUEST_ENTER_COORDINATES)) {
            objectWithIdSelected(
                    (Point) bundle.getSerializable(EnterCoordinatesDialog.EXTRA_COORDINATES));

        } else if (requestKey.equals(ProfileListFragment.REQUEST_SELECT_PROFILE)) {
            Profile selectedProfile = (Profile) bundle.getSerializable(ProfileListFragment.EXTRA_PROFILE);
            Timber.d("onFragmentResult: profile selected");
            if (selectedProfile instanceof DatabaseProfile) {
                ObjectListFromDatabaseFragment.selectObjectWithId((DatabaseProfile) selectedProfile)
                    .show(getChildFragmentManager(), "SelectPointDialog");
            } else if (selectedProfile instanceof PoiProfile) {
                PoiListFromServerFragment.selectObjectWithId((PoiProfile) selectedProfile)
                    .show(getChildFragmentManager(), "SelectPointDialog");
            }

        } else if (requestKey.equals(PointFromCoordinatesLinkDialog.REQUEST_FROM_COORDINATES_LINK)) {
            objectWithIdSelected(
                    (Point) bundle.getSerializable(PointFromCoordinatesLinkDialog.EXTRA_COORDINATES));

        } else if (requestKey.equals(ObjectListFragment.REQUEST_SELECT_OBJECT)) {
            objectWithIdSelected(
                    (ObjectWithId) bundle.getSerializable(ObjectListFragment.EXTRA_OBJECT_WITH_ID));
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        target = (Target) getArguments().getSerializable(KEY_TARGET);

        String dialogTitle = "";
        switch (target) {
            case ROUTE_START_POINT:
                dialogTitle = getResources().getString(R.string.selectObjectWithIdFromMultipleSourcesDialogTitleRouteStartPoint);
                break;
            case ROUTE_VIA_POINT_1:
                dialogTitle = getResources().getString(R.string.selectObjectWithIdFromMultipleSourcesDialogTitleRouteFirstViaPoint);
                break;
            case ROUTE_VIA_POINT_2:
                dialogTitle = getResources().getString(R.string.selectObjectWithIdFromMultipleSourcesDialogTitleRouteSecondViaPoint);
                break;
            case ROUTE_VIA_POINT_3:
                dialogTitle = getResources().getString(R.string.selectObjectWithIdFromMultipleSourcesDialogTitleRouteThirdViaPoint);
                break;
            case ROUTE_DESTINATION_POINT:
                dialogTitle = getResources().getString(R.string.selectObjectWithIdFromMultipleSourcesDialogTitleRouteDestinationPoint);
                break;
            case ADD_TO_COLLECTION:
                dialogTitle = getResources().getString(R.string.selectObjectWithIdFromMultipleSourcesDialogTitleAddToCollection);
                break;
            case ADD_TO_PINNED_POINTS_AND_ROUTES:
                dialogTitle = getResources().getString(R.string.selectObjectWithIdFromMultipleSourcesDialogTitleAddToPinnedPointsAndRoutes);
                break;
            case ADD_TO_TRACKED_OBJECTS:
                dialogTitle = getResources().getString(R.string.selectObjectWithIdFromMultipleSourcesDialogTitleAddToTrackedObjects);
                break;
            case SIMULATE_LOCATION:
                dialogTitle = getResources().getString(R.string.selectObjectWithIdFromMultipleSourcesDialogTitleSimulationPoint);
                break;
            case USE_AS_HOME_ADDRESS:
                dialogTitle = getResources().getString(R.string.selectObjectWithIdFromMultipleSourcesDialogTitleHomeAddress);
                break;
        }

        return new AlertDialog.Builder(getActivity())
            .setTitle(dialogTitle)
            .setItems(
                    new String[]{getResources().getString(R.string.messagePleaseWait)},
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
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

            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    executeAction(
                            (SourceAction) parent.getItemAtPosition(position));
                }
            });

            // fill listview
            ArrayList<SourceAction> sourceActionList = new ArrayList<SourceAction>(
                    Arrays.asList(SourceAction.values()));

            // remove actions "CURRENT_LOCATION" and "CLOSEST_ADDRESS"
            switch (target) {
                case ROUTE_VIA_POINT_1:
                case ROUTE_VIA_POINT_2:
                case ROUTE_VIA_POINT_3:
                case ROUTE_DESTINATION_POINT:
                case SIMULATE_LOCATION:
                    sourceActionList.remove(SourceAction.CURRENT_LOCATION);
                    sourceActionList.remove(SourceAction.CLOSEST_ADDRESS);
                    break;
            }

            // remove action "HOME_ADDRESS"
            switch (target) {
                case USE_AS_HOME_ADDRESS:
                    sourceActionList.remove(SourceAction.HOME_ADDRESS);
                    break;
            }

            // remove action history
            switch (target) {
                case ROUTE_START_POINT:
                case ROUTE_VIA_POINT_1:
                case ROUTE_VIA_POINT_2:
                case ROUTE_VIA_POINT_3:
                case ROUTE_DESTINATION_POINT:
                case ADD_TO_COLLECTION:
                case USE_AS_HOME_ADDRESS:
                    sourceActionList.remove(SourceAction.HISTORY);
                    break;
            }

            listViewItems.setAdapter(
                    new ArrayAdapter<SourceAction>(
                        getActivity(), android.R.layout.simple_list_item_1, sourceActionList));

            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });
        }
    }


    /**
     * SourceAction
     */

    private enum SourceAction {

        CURRENT_LOCATION(GlobalInstance.getStringResource(R.string.pointSelectFromCurrentLocation)),
        CLOSEST_ADDRESS(GlobalInstance.getStringResource(R.string.pointSelectFromClosestAddress)),
        HOME_ADDRESS(GlobalInstance.getStringResource(R.string.pointSelectFromHomeAddress)),
        ENTER_ADDRESS(GlobalInstance.getStringResource(R.string.pointSelectFromEnterAddress)),
        COLLECTIONS(GlobalInstance.getStringResource(R.string.pointSelectFromCollections)),
        POI(GlobalInstance.getStringResource(R.string.pointSelectFromPOI)),
        HISTORY(GlobalInstance.getStringResource(R.string.pointSelectFromHistory)),
        FROM_COORDINATES_LINK(GlobalInstance.getStringResource(R.string.pointSelectFromCoordinatesLink)),
        ENTER_COORDINATES(GlobalInstance.getStringResource(R.string.pointSelectFromEnterCoordinates));

        private String name;

        private SourceAction(String name) {
            this.name = name;
        }

        @Override public String toString() {
            return this.name;
        }
    }


    private void executeAction(SourceAction action) {
        switch (action) {

            case CURRENT_LOCATION:
                Point currentLocation = PositionManager.getInstance().getCurrentLocation();
                if (currentLocation == null) {
                    Toast.makeText(
                            getActivity(),
                            GlobalInstance.getStringResource(R.string.errorNoLocationFound),
                            Toast.LENGTH_LONG)
                        .show();
                    return;
                }

                switch (target) {
                    case ADD_TO_COLLECTION:
                        SaveCurrentLocationDialog.sendResultBundle(
                                getResources().getString(R.string.saveCurrentLocationDialogTitleCollection))
                            .show(getChildFragmentManager(), "SaveCurrentLocationDialog");
                        break;
                    case ADD_TO_PINNED_POINTS_AND_ROUTES:
                    case ADD_TO_TRACKED_OBJECTS:
                        SaveCurrentLocationDialog.sendResultBundle(
                                target == Target.ADD_TO_PINNED_POINTS_AND_ROUTES
                                ? getResources().getString(R.string.saveCurrentLocationDialogTitlePin)
                                : getResources().getString(R.string.saveCurrentLocationDialogTitleTrack))
                            .show(getChildFragmentManager(), "SaveCurrentLocationDialog");
                        break;
                    case USE_AS_HOME_ADDRESS:
                        SaveCurrentLocationDialog.sendResultBundle(
                                getResources().getString(R.string.saveCurrentLocationDialogTitleHome))
                            .show(getChildFragmentManager(), "SaveCurrentLocationDialog");
                        break;
                    default:
                        objectWithIdSelected(currentLocation);
                }
                break;

            case CLOSEST_ADDRESS:
                WhereAmIDialog.newInstance(true)
                    .show(getChildFragmentManager(), "WhereAmIDialog");
                break;

            case HOME_ADDRESS:
                Point homeAddress = SettingsManager.getInstance().getHomeAddress();
                if (homeAddress == null) {
                    Toast.makeText(
                            getActivity(),
                            GlobalInstance.getStringResource(R.string.errorNoHomeAddressSet),
                            Toast.LENGTH_LONG)
                        .show();
                    return;
                }
                objectWithIdSelected(homeAddress);
                break;

            case ENTER_ADDRESS:
                EnterAddressDialog.newInstance()
                    .show(getChildFragmentManager(), "EnterAddressDialog");
                break;

            case ENTER_COORDINATES:
                EnterCoordinatesDialog.newInstance()
                    .show(getChildFragmentManager(), "EnterCoordinatesDialog");
                break;

            case COLLECTIONS:
                CollectionListFragment.selectProfile()
                    .show(getChildFragmentManager(), "CollectionListFragment");
                break;

            case POI:
                PoiProfileListFragment.selectProfile()
                    .show(getChildFragmentManager(), "PoiProfileListFragment");
                break;

            case HISTORY:
                HistoryProfile historyProfile = null;
                if (target == Target.ADD_TO_PINNED_POINTS_AND_ROUTES) {
                    historyProfile = HistoryProfile.pinnedObjectsWithId();
                } else if (target == Target.ADD_TO_TRACKED_OBJECTS) {
                    historyProfile = HistoryProfile.trackedObjectsWithId();
                } else if (target == Target.SIMULATE_LOCATION) {
                    historyProfile = HistoryProfile.simulatedPoints();
                }
                if (historyProfile != null) {
                    ObjectListFromDatabaseFragment.selectObjectWithId(historyProfile)
                        .show(getChildFragmentManager(), "SelectPointDialog");
                }
                break;

            case FROM_COORDINATES_LINK:
                PointFromCoordinatesLinkDialog.newInstance()
                    .show(getChildFragmentManager(), "PointFromCoordinatesLinkDialog");
                break;
        }
    }

    private void objectWithIdSelected(ObjectWithId objectWithId) {
        if (objectWithId == null) {
            Toast.makeText(
                    getActivity(),
                    GlobalInstance.getStringResource(R.string.labelNothingSelected),
                    Toast.LENGTH_LONG)
                .show();
            return;
        }

        switch (target) {
            case ROUTE_START_POINT:
            case ROUTE_VIA_POINT_1:
            case ROUTE_VIA_POINT_2:
            case ROUTE_VIA_POINT_3:
            case ROUTE_DESTINATION_POINT:
            case SIMULATE_LOCATION:
            case USE_AS_HOME_ADDRESS:
                if (! (objectWithId instanceof Point)) {
                    Toast.makeText(
                            getActivity(),
                            GlobalInstance.getStringResource(R.string.messageObjectWithIdIncompatibleTargetPointRequired),
                            Toast.LENGTH_LONG)
                        .show();
                    return;
                }
                break;
        }

        Bundle result = new Bundle();
        result.putSerializable(EXTRA_OBJECT_WITH_ID, objectWithId);
        result.putSerializable(EXTRA_TARGET, target);
        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_OBJECT_WITH_ID, result);
        dismiss();
    }

}
