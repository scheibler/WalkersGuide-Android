package org.walkersguide.android.ui.dialog.selectors;

import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
import org.walkersguide.android.ui.fragment.object_list.ObjectListFromDatabaseFragment;
import org.walkersguide.android.ui.fragment.object_list.PoiListFromServerFragment;
import org.walkersguide.android.ui.dialog.creators.EnterAddressDialog;
import org.walkersguide.android.ui.dialog.creators.EnterCoordinatesDialog;
import org.walkersguide.android.server.poi.PoiProfile;
import org.walkersguide.android.server.poi.PoiProfileRequest;
import org.walkersguide.android.database.DatabaseProfile;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;


import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;


import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.data.basic.point.StreetAddress;
import android.app.Activity;
import java.util.Arrays;
import org.walkersguide.android.util.GlobalInstance;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.ui.fragment.ObjectListFragment;



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
                    EnterAddressDialog.REQUEST_ENTER_ADDRESS, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    EnterCoordinatesDialog.REQUEST_ENTER_COORDINATES, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectDatabaseProfileDialog.REQUEST_SELECT_DATABASE_PROFILE, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectPoiProfileDialog.REQUEST_SELECT_POI_PROFILE, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ObjectListFragment.REQUEST_SELECT_OBJECT, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(EnterAddressDialog.REQUEST_ENTER_ADDRESS)) {
            pointSelected(
                    (StreetAddress) bundle.getSerializable(EnterAddressDialog.EXTRA_STREET_ADDRESS));
        } else if (requestKey.equals(EnterCoordinatesDialog.REQUEST_ENTER_COORDINATES)) {
            pointSelected(
                    (GPS) bundle.getSerializable(EnterCoordinatesDialog.EXTRA_COORDINATES));
        } else if (requestKey.equals(SelectDatabaseProfileDialog.REQUEST_SELECT_DATABASE_PROFILE)) {
            DatabaseProfile newDatabaseProfile = (DatabaseProfile) bundle.getSerializable(SelectDatabaseProfileDialog.EXTRA_DATABASE_PROFILE);
            ObjectListFromDatabaseFragment.createDialog(
                    new DatabaseProfileRequest(newDatabaseProfile), false)
                .show(getChildFragmentManager(), "SelectPointDialog");
        } else if (requestKey.equals(SelectPoiProfileDialog.REQUEST_SELECT_POI_PROFILE)) {
            PoiProfile newPoiProfile = (PoiProfile) bundle.getSerializable(SelectPoiProfileDialog.EXTRA_POI_PROFILE);
            PoiListFromServerFragment.createDialog(
                    new PoiProfileRequest(newPoiProfile), false)
                .show(getChildFragmentManager(), "SelectPointDialog");
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
        HISTORY(GlobalInstance.getStringResource(R.string.pointSelectFromHistoryPoints)),
        POI(GlobalInstance.getStringResource(R.string.pointSelectFromPOI));

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
                pointSelected(
                        PositionManager.getInstance().getCurrentLocation());
                break;
            case CLOSEST_ADDRESS:
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
                ObjectListFromDatabaseFragment.createDialog(
                        new DatabaseProfileRequest(DatabasePointProfile.FAVORITES), false)
                    .show(getChildFragmentManager(), "SelectFavoriteDialog");
                break;
            case HISTORY:
                SelectDatabaseProfileDialog.pointProfiles(null)
                    .show(getChildFragmentManager(), "SelectDatabaseProfileDialog");
                break;
            case POI:
                SelectPoiProfileDialog.newInstance(null)
                    .show(getChildFragmentManager(), "SelectPoiProfileDialog");
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
