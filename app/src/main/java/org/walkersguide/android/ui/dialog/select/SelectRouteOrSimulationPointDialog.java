package org.walkersguide.android.ui.dialog.select;

import org.walkersguide.android.data.profile.Profile;
import org.walkersguide.android.data.profile.ProfileGroup;
import org.walkersguide.android.database.profile.FavoritesProfile;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
import org.walkersguide.android.ui.fragment.object_list.extended.PoiListFromServerFragment;
import org.walkersguide.android.ui.dialog.create.EnterAddressDialog;
import org.walkersguide.android.ui.dialog.create.EnterCoordinatesDialog;
import org.walkersguide.android.ui.dialog.create.PointFromCoordinatesLinkDialog;
import android.app.AlertDialog;
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



public class SelectRouteOrSimulationPointDialog extends DialogFragment implements FragmentResultListener {
    public static final String REQUEST_SELECT_POINT = "selectPoint";
    public static final String EXTRA_WHERE_TO_PUT = "whereToPut";
    public static final String EXTRA_POINT = "point";


    // instance constructors

    public static SelectRouteOrSimulationPointDialog newInstance(WhereToPut whereToPut) {
        SelectRouteOrSimulationPointDialog dialog = new SelectRouteOrSimulationPointDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_WHERE_TO_PUT, whereToPut);
        dialog.setArguments(args);
        return dialog;
    }

    // dialog
    private static final String KEY_WHERE_TO_PUT = "whereToPut";

    public enum WhereToPut {
        ROUTE_START_POINT, ROUTE_VIA_POINT_1, ROUTE_VIA_POINT_2, ROUTE_VIA_POINT_3, ROUTE_DESTINATION_POINT, SIMULATION_POINT
    }

    private WhereToPut whereToPut;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                    SelectProfileDialog.REQUEST_SELECT_PROFILE, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    PointFromCoordinatesLinkDialog.REQUEST_FROM_COORDINATES_LINK, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ObjectListFragment.REQUEST_SELECT_OBJECT, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        Timber.d("onFragmentResult: %1$s", requestKey);
        if (requestKey.equals(WhereAmIDialog.REQUEST_RESOLVE_COORDINATES)) {
            pointSelected(
                    (StreetAddress) bundle.getSerializable(WhereAmIDialog.EXTRA_STREET_ADDRESS));
        } else if (requestKey.equals(EnterAddressDialog.REQUEST_ENTER_ADDRESS)) {
            pointSelected(
                    (StreetAddress) bundle.getSerializable(EnterAddressDialog.EXTRA_STREET_ADDRESS));
        } else if (requestKey.equals(EnterCoordinatesDialog.REQUEST_ENTER_COORDINATES)) {
            pointSelected(
                    (GPS) bundle.getSerializable(EnterCoordinatesDialog.EXTRA_COORDINATES));
        } else if (requestKey.equals(SelectProfileDialog.REQUEST_SELECT_PROFILE)) {
            Profile selectedProfile = (Profile) bundle.getSerializable(SelectProfileDialog.EXTRA_PROFILE);
            Timber.d("onFragmentResult: profile selected");
            if (selectedProfile instanceof DatabaseProfile) {
                ObjectListFromDatabaseFragment.createDialog((DatabaseProfile) selectedProfile, true)
                    .show(getChildFragmentManager(), "SelectPointDialog");
            } else if (selectedProfile instanceof PoiProfile) {
                Timber.d("start poi dialog");
                PoiListFromServerFragment.createDialog((PoiProfile) selectedProfile, true)
                    .show(getChildFragmentManager(), "SelectPointDialog");
            }
        } else if (requestKey.equals(PointFromCoordinatesLinkDialog.REQUEST_FROM_COORDINATES_LINK)) {
            pointSelected(
                    (GPS) bundle.getSerializable(PointFromCoordinatesLinkDialog.EXTRA_COORDINATES));
        } else if (requestKey.equals(ObjectListFragment.REQUEST_SELECT_OBJECT)) {
            ObjectWithId newObject = (ObjectWithId) bundle.getSerializable(ObjectListFragment.EXTRA_OBJECT_WITH_ID);
            if (newObject instanceof Point) {
                pointSelected((Point) newObject);
            }
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        whereToPut = (WhereToPut) getArguments().getSerializable(KEY_WHERE_TO_PUT);

        String dialogTitle = "";
        switch (whereToPut) {
            case ROUTE_START_POINT:
                dialogTitle = getResources().getString(R.string.selectRouteOrSimulationPointDialogTitleRouteStartPoint);
                break;
            case ROUTE_VIA_POINT_1:
                dialogTitle = getResources().getString(R.string.selectRouteOrSimulationPointDialogTitleRouteFirstViaPoint);
                break;
            case ROUTE_VIA_POINT_2:
                dialogTitle = getResources().getString(R.string.selectRouteOrSimulationPointDialogTitleRouteSecondViaPoint);
                break;
            case ROUTE_VIA_POINT_3:
                dialogTitle = getResources().getString(R.string.selectRouteOrSimulationPointDialogTitleRouteThirdViaPoint);
                break;
            case ROUTE_DESTINATION_POINT:
                dialogTitle = getResources().getString(R.string.selectRouteOrSimulationPointDialogTitleRouteDestinationPoint);
                break;
            case SIMULATION_POINT:
                dialogTitle = getResources().getString(R.string.selectRouteOrSimulationPointDialogTitleSimulationPoint);
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
            listViewItems.setAdapter(
                    new ArrayAdapter<SourceAction>(
                        getActivity(),
                        android.R.layout.simple_list_item_1,
                        new ArrayList<SourceAction>(Arrays.asList(SourceAction.values()))));

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
        ENTER_ADDRESS(GlobalInstance.getStringResource(R.string.pointSelectFromEnterAddress)),
        ENTER_COORDINATES(GlobalInstance.getStringResource(R.string.pointSelectFromEnterCoordinates)),
        FAVORITES(GlobalInstance.getStringResource(R.string.pointSelectFromFavoritePoints)),
        POI(GlobalInstance.getStringResource(R.string.pointSelectFromPOI)),
        HISTORY(GlobalInstance.getStringResource(R.string.pointSelectFromHistoryPoints)),
        FROM_COORDINATES_LINK(GlobalInstance.getStringResource(R.string.pointSelectFromCoordinatesLink));

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
                    SimpleMessageDialog.newInstance(
                            getResources().getString(R.string.errorNoLocationFound))
                        .show(getChildFragmentManager(), "SimpleMessageDialog");
                } else {
                    pointSelected(currentLocation);
                }
                break;
            case CLOSEST_ADDRESS:
                WhereAmIDialog.newInstance(true)
                    .show(getChildFragmentManager(), "WhereAmIDialog");
                break;
            case ENTER_ADDRESS:
                EnterAddressDialog.newInstance()
                    .show(getChildFragmentManager(), "EnterAddressDialog");
                break;
            case ENTER_COORDINATES:
                EnterCoordinatesDialog.newInstance()
                    .show(getChildFragmentManager(), "EnterCoordinatesDialog");
                break;
            case FAVORITES:
                ObjectListFromDatabaseFragment.createDialog(FavoritesProfile.favoritePoints(), true)
                    .show(getChildFragmentManager(), "SelectFavoriteDialog");
                break;
            case HISTORY:
                SelectProfileDialog.newInstance(ProfileGroup.POINT_HISTORY)
                    .show(getChildFragmentManager(), "SelectProfileDialog");
                break;
            case POI:
                SelectProfileDialog.newInstance(ProfileGroup.POI)
                    .show(getChildFragmentManager(), "SelectProfileDialog");
                break;
            case FROM_COORDINATES_LINK:
                PointFromCoordinatesLinkDialog.newInstance()
                    .show(getChildFragmentManager(), "PointFromCoordinatesLinkDialog");
                break;
        }
    }

    private void pointSelected(Point point) {
        Bundle result = new Bundle();
        result.putSerializable(EXTRA_WHERE_TO_PUT, whereToPut);
        result.putSerializable(EXTRA_POINT, point);
        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_POINT, result);
        dismiss();
    }

}
