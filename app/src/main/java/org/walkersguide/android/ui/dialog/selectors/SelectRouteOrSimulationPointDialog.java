package org.walkersguide.android.ui.dialog.selectors;

import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
import org.walkersguide.android.ui.fragment.ObjectListFragment.SelectObjectListener;
import org.walkersguide.android.ui.fragment.object_list.ObjectListFromDatabaseFragment;
import org.walkersguide.android.ui.fragment.object_list.PoiListFromServerFragment;
import org.walkersguide.android.ui.dialog.creators.EnterAddressDialog;
import org.walkersguide.android.ui.dialog.creators.EnterAddressDialog.EnterAddressListener;
import org.walkersguide.android.ui.dialog.creators.EnterCoordinatesDialog;
import org.walkersguide.android.ui.dialog.creators.EnterCoordinatesDialog.EnterCoordinatesListener;
import org.walkersguide.android.server.poi.PoiProfile;
import org.walkersguide.android.server.poi.PoiProfileRequest;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.ui.dialog.selectors.SelectDatabaseProfileDialog.SelectDatabaseProfileListener;
import org.walkersguide.android.ui.dialog.selectors.SelectPoiProfileDialog.SelectPoiProfileListener;
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



public class SelectRouteOrSimulationPointDialog extends DialogFragment
        implements EnterAddressListener, EnterCoordinatesListener,
                   SelectDatabaseProfileListener, SelectPoiProfileListener, SelectObjectListener {
    private static final String KEY_WHERE_TO_PUT = "whereToPut";

    public interface SelectRouteOrSimulationPointListener {
        public void routeOrSimulationPointSelected(Point point, WhereToPut whereToPut);
    }

    public enum WhereToPut {
        ROUTE_START_POINT, ROUTE_VIA_POINT_1, ROUTE_VIA_POINT_2, ROUTE_VIA_POINT_3, ROUTE_DESTINATION_POINT, SIMULATION_POINT
    }


    private SelectRouteOrSimulationPointListener listener;
    private WhereToPut whereToPut;

    public static SelectRouteOrSimulationPointDialog newInstance(WhereToPut whereToPut) {
        SelectRouteOrSimulationPointDialog dialog = new SelectRouteOrSimulationPointDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_WHERE_TO_PUT, whereToPut);
        dialog.setArguments(args);
        return dialog;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof SelectRouteOrSimulationPointListener) {
            listener = (SelectRouteOrSimulationPointListener) getTargetFragment();
        } else if (context instanceof Activity
                && (Activity) context instanceof SelectRouteOrSimulationPointListener) {
            listener = (SelectRouteOrSimulationPointListener) context;
        }
    }

    @Override public void onDetach() {
        super.onDetach();
        listener = null;
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
                EnterAddressDialog eaDialog = EnterAddressDialog.newInstance();
                eaDialog.setTargetFragment(SelectRouteOrSimulationPointDialog.this, 1);
                eaDialog.show(getActivity().getSupportFragmentManager(), "EnterAddressDialog");
                break;
            case ENTER_COORDINATES:
                EnterCoordinatesDialog ecDialog = EnterCoordinatesDialog.newInstance();
                ecDialog.setTargetFragment(SelectRouteOrSimulationPointDialog.this, 1);
                ecDialog.show(getActivity().getSupportFragmentManager(), "EnterCoordinatesDialog");
                break;
            case FAVORITES:
                ObjectListFromDatabaseFragment sfDialog = ObjectListFromDatabaseFragment.createDialog(
                        new DatabaseProfileRequest(DatabasePointProfile.FAVORITES), false);
                sfDialog.setTargetFragment(SelectRouteOrSimulationPointDialog.this, 1);
                sfDialog.show(getActivity().getSupportFragmentManager(), "SelectFavoriteDialog");
                break;
            case HISTORY:
                SelectDatabaseProfileDialog sdpDialog = SelectDatabaseProfileDialog.pointProfiles(null);
                sdpDialog.setTargetFragment(SelectRouteOrSimulationPointDialog.this, 1);
                sdpDialog.show(getActivity().getSupportFragmentManager(), "SelectDatabaseProfileDialog");
                break;
            case POI:
                SelectPoiProfileDialog sppDialog = SelectPoiProfileDialog.newInstance(null);
                sppDialog.setTargetFragment(SelectRouteOrSimulationPointDialog.this, 1);
                sppDialog.show(getActivity().getSupportFragmentManager(), "SelectPoiProfileDialog");
                break;
        }
    }

    private void pointSelected(Point point) {
        if (listener != null) {
            listener.routeOrSimulationPointSelected(point, whereToPut);
            dismiss();
        }
    }


    // listeners

    @Override public void addressPointCreated(StreetAddress address) {
        pointSelected(address);
    }

    @Override public void coordinatesPointCreated(GPS coordinates) {
        pointSelected(coordinates);
    }

    @Override public void objectSelected(ObjectWithId object) {
        if (object instanceof Point) {
            pointSelected((Point) object);
        }
    }

    // select profile sublisteners

    @Override public void databaseProfileSelected(DatabaseProfile newProfile) {
        ObjectListFromDatabaseFragment spDialog = ObjectListFromDatabaseFragment.createDialog(
                new DatabaseProfileRequest(newProfile), false);
        spDialog.setTargetFragment(SelectRouteOrSimulationPointDialog.this, 1);
        spDialog.show(getActivity().getSupportFragmentManager(), "SelectPointDialog");
    }

    @Override public void poiProfileSelected(PoiProfile newProfile) {
        PoiListFromServerFragment spDialog = PoiListFromServerFragment.createDialog(
                new PoiProfileRequest(newProfile), false);
        spDialog.setTargetFragment(SelectRouteOrSimulationPointDialog.this, 1);
        spDialog.show(getActivity().getSupportFragmentManager(), "SelectPointDialog");
    }

}
