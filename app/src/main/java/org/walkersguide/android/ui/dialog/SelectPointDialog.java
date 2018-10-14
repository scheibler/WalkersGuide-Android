package org.walkersguide.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;

import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;

import android.text.Editable;
import android.text.TextUtils;

import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.basic.wrapper.PointWrapper.SortByNameASC;
import org.walkersguide.android.data.profile.HistoryPointProfile;
import org.walkersguide.android.data.profile.POIProfile;
import org.walkersguide.android.helper.PointUtility;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.AddressListener;
import org.walkersguide.android.listener.ChildDialogCloseListener;
import org.walkersguide.android.listener.HistoryPointProfileListener;
import org.walkersguide.android.listener.POIProfileListener;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.AddressManager;
import org.walkersguide.android.server.POIManager;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.adapter.POIProfilePointAdapter;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.POISettings;
import org.walkersguide.android.util.TextChangedListener;


public class SelectPointDialog extends DialogFragment implements ChildDialogCloseListener {

    private ChildDialogCloseListener childDialogCloseListener;
    private AccessDatabase accessDatabaseInstance;
    private PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;
    private int pointPutInto;
    private PointWrapper selectedPoint;
    private int[] selectFromArray;

    public static SelectPointDialog newInstance(int pointPutInto) {
        SelectPointDialog selectPointDialogInstance = new SelectPointDialog();
        Bundle args = new Bundle();
        args.putInt("pointPutInto", pointPutInto);
        selectPointDialogInstance.setArguments(args);
        return selectPointDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof ChildDialogCloseListener) {
            childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
        }
        accessDatabaseInstance = AccessDatabase.getInstance(context);
        positionManagerInstance = PositionManager.getInstance(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        pointPutInto = getArguments().getInt("pointPutInto");
        selectedPoint = PositionManager.getDummyLocation(getActivity());

        switch (pointPutInto) {
            case Constants.POINT_PUT_INTO.START:
                selectedPoint = settingsManagerInstance.getRouteSettings().getStartPoint();
                selectFromArray = Constants.PointSelectFromValueArray;
                break;
            case Constants.POINT_PUT_INTO.DESTINATION:
                selectedPoint = settingsManagerInstance.getRouteSettings().getDestinationPoint();
                selectFromArray = Constants.PointSelectFromValueArray;
                break;
            case Constants.POINT_PUT_INTO.SIMULATION:
                selectedPoint = positionManagerInstance.getSimulatedLocation();
                selectFromArray = Constants.PointSelectFromValueArrayWithoutCurrentLocation;
                break;
            default:
                // via point
                ArrayList<PointWrapper> viaPointList = settingsManagerInstance.getRouteSettings().getViaPointList();
                int viaPointIndex = pointPutInto - Constants.POINT_PUT_INTO.VIA;
                if (viaPointIndex >= 0 && viaPointIndex < viaPointList.size()) {
                    selectedPoint = viaPointList.get(viaPointIndex);
                }
                selectFromArray = Constants.PointSelectFromValueArrayWithoutCurrentLocation;
                break;
        }

        String dialogTitle;
        switch (pointPutInto) {
            case Constants.POINT_PUT_INTO.START:
                if (selectedPoint.equals(PositionManager.getDummyLocation(getActivity()))) {
                    dialogTitle = getResources().getString(R.string.selectPointDialogNameStart);
                } else {
                    dialogTitle = String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.buttonStartPoint),
                            selectedPoint.getPoint().getName());
                }
                break;
            case Constants.POINT_PUT_INTO.DESTINATION:
                if (selectedPoint.equals(PositionManager.getDummyLocation(getActivity()))) {
                    dialogTitle = getResources().getString(R.string.selectPointDialogNameDestination);
                } else {
                    dialogTitle = String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.buttonDestinationPoint),
                            selectedPoint.getPoint().getName());
                }
                break;
            case Constants.POINT_PUT_INTO.SIMULATION:
                if (selectedPoint.equals(PositionManager.getDummyLocation(getActivity()))) {
                    dialogTitle = getResources().getString(R.string.selectPointDialogNameSimulation);
                } else {
                    dialogTitle = String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.buttonSimulationPoint),
                            selectedPoint.getPoint().getName());
                }
                break;
            default:
                // via points
                if (selectedPoint.equals(PositionManager.getDummyLocation(getActivity()))) {
                    dialogTitle = String.format(
                            getResources().getString(R.string.selectPointDialogNameVia),
                            (pointPutInto - Constants.POINT_PUT_INTO.VIA) + 1);
                } else {
                    dialogTitle = String.format(
                            "%1$s %2$d: %3$s",
                            getResources().getString(R.string.buttonViaPoint),
                            (pointPutInto - Constants.POINT_PUT_INTO.VIA) + 1,
                            selectedPoint.getPoint().getName());
                }
                break;
        }

        String[] formattedPointSelectFromArray = new String[selectFromArray.length];
        for (int i=0; i<selectFromArray.length; i++) {
            switch (selectFromArray[i]) {
                case Constants.POINT_SELECT_FROM.CURRENT_LOCATION:
                    formattedPointSelectFromArray[i] = getResources().getString(R.string.pointSelectFromCurrentLocation);
                    break;
                case Constants.POINT_SELECT_FROM.ENTER_ADDRESS:
                    formattedPointSelectFromArray[i] = getResources().getString(R.string.pointSelectFromEnterAddress);
                    break;
                case Constants.POINT_SELECT_FROM.ENTER_COORDINATES:
                    formattedPointSelectFromArray[i] = getResources().getString(R.string.pointSelectFromEnterCoordinates);
                    break;
                case Constants.POINT_SELECT_FROM.FROM_HISTORY_POINTS:
                    formattedPointSelectFromArray[i] = getResources().getString(R.string.pointSelectFromHistoryPoints);
                    break;
                case Constants.POINT_SELECT_FROM.FROM_POI:
                    formattedPointSelectFromArray[i] = getResources().getString(R.string.pointSelectFromPOI);
                    break;
                default:
                    formattedPointSelectFromArray[i] = String.valueOf(selectFromArray[i]);
                    break;
            }
        }

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(dialogTitle)
            .setSingleChoiceItems(
                    formattedPointSelectFromArray,
                    -1,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            int pointSelectFromIndex = -1;
                            try {
                                pointSelectFromIndex = selectFromArray[which];
                            } catch (ArrayIndexOutOfBoundsException e) {
                                pointSelectFromIndex = -1;
                            } finally {
                                if (pointSelectFromIndex > -1) {
                                    executeAction(pointSelectFromIndex);
                                }
                            }
                        }
                    })
            .setPositiveButton(
                    getResources().getString(R.string.dialogDetails),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNeutralButton(
                    getResources().getString(R.string.dialogRemove),
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
            .setOnKeyListener(
                    new Dialog.OnKeyListener() {
                        @Override public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                close();
                                return true;
                            }
                            return false;
                        }
                    })
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (! selectedPoint.equals(PositionManager.getDummyLocation(getActivity()))) {
                buttonPositive.setVisibility(View.VISIBLE);
            } else {
                buttonPositive.setVisibility(View.GONE);
            }
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                    try {
                        detailsIntent.putExtra(
                                Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, selectedPoint.toJson().toString());
                    } catch (JSONException e) {
                        detailsIntent.putExtra(
                                Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                    }
                    startActivity(detailsIntent);
                }
            });
            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (pointPutInto >= Constants.POINT_PUT_INTO.VIA) {
                buttonNeutral.setVisibility(View.VISIBLE);
            } else {
                buttonNeutral.setVisibility(View.GONE);
            }
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    settingsManagerInstance.getRouteSettings().removeViaPointAtIndex(
                            pointPutInto-Constants.POINT_PUT_INTO.VIA);
                    close();
                }
            });
            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    close();
                }
            });
        }
    }

    @Override public void childDialogClosed() {
        close();
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        childDialogCloseListener = null;
    }

    private void executeAction(int pointSelectFromIndex) {
        int menuItemPosition = 1;
        Button buttonNegative = null;
        AlertDialog dialog = (AlertDialog) getDialog();
        if(dialog != null) {
            buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        }

        switch (pointSelectFromIndex) {
            case Constants.POINT_SELECT_FROM.CURRENT_LOCATION:
                PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
                if (currentLocation.equals(PositionManager.getDummyLocation(getActivity()))) {
                    SimpleMessageDialog.newInstance(
                            getResources().getString(R.string.errorNoLocationFound))
                        .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
                } else {
                    PointUtility.putNewPoint(
                            getActivity(), currentLocation, pointPutInto);
                    close();
                }
                break;

            case Constants.POINT_SELECT_FROM.ENTER_ADDRESS:
                EnterAddressDialog enterAddressDialog = EnterAddressDialog.newInstance(pointPutInto);
                enterAddressDialog.setTargetFragment(SelectPointDialog.this, 1);
                enterAddressDialog.show(
                        getActivity().getSupportFragmentManager(), "EnterAddressDialog");
                break;

            case Constants.POINT_SELECT_FROM.ENTER_COORDINATES:
                EnterCoordinatesDialog enterCoordinatesDialog = EnterCoordinatesDialog.newInstance(pointPutInto);
                enterCoordinatesDialog.setTargetFragment(SelectPointDialog.this, 1);
                enterCoordinatesDialog.show(
                        getActivity().getSupportFragmentManager(), "EnterCoordinatesDialog");
                break;

            case Constants.POINT_SELECT_FROM.FROM_HISTORY_POINTS:
                if (buttonNegative != null) {
                    PopupMenu popupHistoryPointProfileList = new PopupMenu(getActivity(), buttonNegative);
                    // create menu
                    menuItemPosition = 1;
                    for (Map.Entry<Integer,String> profile : HistoryPointProfile.getProfileMap(getActivity()).entrySet()) {
                        popupHistoryPointProfileList.getMenu().add(
                                Menu.NONE,
                                profile.getKey(),
                                menuItemPosition,
                                profile.getValue());
                        menuItemPosition += 1;
                    }
                    popupHistoryPointProfileList.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override public boolean onMenuItemClick(MenuItem item) {
                            SelectHistoryPointDialog selectHistoryPointDialog = SelectHistoryPointDialog
                                .newInstance(item.getItemId(), pointPutInto);
                            selectHistoryPointDialog.setTargetFragment(SelectPointDialog.this, 1);
                            selectHistoryPointDialog.show(
                                    getActivity().getSupportFragmentManager(), "SelectHistoryPointDialog");
                            return true;
                        }
                    });
                    popupHistoryPointProfileList.show();
                }
                break;

            case Constants.POINT_SELECT_FROM.FROM_POI:
                if (buttonNegative != null) {
                    PopupMenu popupPOIProfileList = new PopupMenu(getActivity(), buttonNegative);
                    // create menu
                    menuItemPosition = 1;
                    for (Map.Entry<Integer,String> profile : accessDatabaseInstance.getPOIProfileMap().entrySet()) {
                        popupPOIProfileList.getMenu().add(
                                Menu.NONE,
                                profile.getKey(),
                                menuItemPosition,
                                profile.getValue());
                        menuItemPosition += 1;
                    }
                    popupPOIProfileList.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override public boolean onMenuItemClick(MenuItem item) {
                            SelectPOIDialog selectPOIDialog = SelectPOIDialog
                                .newInstance(item.getItemId(), pointPutInto);
                            selectPOIDialog.setTargetFragment(SelectPointDialog.this, 1);
                            selectPOIDialog.show(
                                    getActivity().getSupportFragmentManager(), "SelectPOIDialog");
                            return true;
                        }
                    });
                    popupPOIProfileList.show();
                }
                break;

            default:
                break;
        }
    }

    private void close() {
        if (childDialogCloseListener != null) {
            childDialogCloseListener.childDialogClosed();
        }
        dismiss();
    }


    public static class EnterAddressDialog extends DialogFragment implements AddressListener {

        // Store instance variables
        private ChildDialogCloseListener childDialogCloseListener;
        private AccessDatabase accessDatabaseInstance;
        private PositionManager positionManagerInstance;
        private SettingsManager settingsManagerInstance;
        private InputMethodManager imm;
        private AddressManager addressManagerRequest;
        private int pointPutInto;
        private AutoCompleteTextView editAddress;

        public static EnterAddressDialog newInstance(int pointPutInto) {
            EnterAddressDialog enterAddressDialogInstance = new EnterAddressDialog();
            Bundle args = new Bundle();
            args.putInt("pointPutInto", pointPutInto);
            enterAddressDialogInstance.setArguments(args);
            return enterAddressDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            if (getTargetFragment() != null
                    && getTargetFragment() instanceof ChildDialogCloseListener) {
                childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
            }
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            positionManagerInstance = PositionManager.getInstance(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
            imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            addressManagerRequest = null;
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            pointPutInto = getArguments().getInt("pointPutInto");

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_autocomplete_text_view, nullParent);

            editAddress = (AutoCompleteTextView) view.findViewById(R.id.editInput);
            editAddress.setHint(getResources().getString(R.string.editHintAddress));
            editAddress.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        tryToGetCoordinatesForAddress();
                        return true;
                    }
                    return false;
                }
            });
            // add auto complete suggestions
            ArrayAdapter<String> searchTermHistoryAdapter = new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_dropdown_item_1line,
                    settingsManagerInstance.getSearchTermHistory().getSearchTermList());
            editAddress.setAdapter(searchTermHistoryAdapter);

            ImageButton buttonClearInput = (ImageButton) view.findViewById(R.id.buttonClearInput);
            buttonClearInput.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    // clear edit text
                    editAddress.setText("");
                    // show keyboard
                    imm.showSoftInput(editAddress, InputMethodManager.SHOW_IMPLICIT);
                }
            });

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.enterAddressDialogName))
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogOK),
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
                // positive button
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        tryToGetCoordinatesForAddress();
                    }
                });
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
            // show keyboard
            new Handler().postDelayed(
                    new Runnable() {
                        @Override public void run() {
                            imm.showSoftInput(editAddress, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }, 50);
        }

        private void tryToGetCoordinatesForAddress() {
            String address = editAddress.getText().toString().trim();
            if (address.equals("")) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageAddressMissing),
                        Toast.LENGTH_LONG).show();
            } else {
                addressManagerRequest = new AddressManager(
                        getActivity(), EnterAddressDialog.this, address);
                addressManagerRequest.execute();
            }
        }

        @Override public void addressRequestFinished(int returnCode, String returnMessage, PointWrapper addressPoint) {
            if (returnCode == Constants.RC.OK) {
                // add to search history
                settingsManagerInstance.getSearchTermHistory().addSearchTerm(
                        editAddress.getText().toString().trim());
                // put into
                PointUtility.putNewPoint(getActivity(), addressPoint, pointPutInto);
                // reload ui
                if (childDialogCloseListener != null) {
                    childDialogCloseListener.childDialogClosed();
                }
                dismiss();
            } else {
                SimpleMessageDialog.newInstance(returnMessage)
                    .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
            }
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            childDialogCloseListener = null;
            if (addressManagerRequest != null
                    && addressManagerRequest.getStatus() != AsyncTask.Status.FINISHED) {
                addressManagerRequest.cancel();
            }
        }
    }


    public static class EnterCoordinatesDialog extends DialogFragment {

        // Store instance variables
        private AccessDatabase accessDatabaseInstance;
        private ChildDialogCloseListener childDialogCloseListener;
        private int pointPutInto;
        private EditText editLatitude, editLongitude, editName;

        public static EnterCoordinatesDialog newInstance(int pointPutInto) {
            EnterCoordinatesDialog enterCoordinatesDialogInstance = new EnterCoordinatesDialog();
            Bundle args = new Bundle();
            args.putInt("pointPutInto", pointPutInto);
            enterCoordinatesDialogInstance.setArguments(args);
            return enterCoordinatesDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            if (getTargetFragment() != null
                    && getTargetFragment() instanceof ChildDialogCloseListener) {
                childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
            }
            accessDatabaseInstance = AccessDatabase.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            pointPutInto = getArguments().getInt("pointPutInto");

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_enter_coordinates, nullParent);

            editLatitude = (EditText) view.findViewById(R.id.editLatitude);
            editLongitude = (EditText) view.findViewById(R.id.editLongitude);
            editName = (EditText) view.findViewById(R.id.editName);
            editName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        tryToGetAddressForCoordinates();
                        return true;
                    }
                    return false;
                }
            });

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.enterCoordinatesDialogName))
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogOK),
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
                // positive button
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        tryToGetAddressForCoordinates();
                    }
                });
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
        }

        private void tryToGetAddressForCoordinates() {
            // latitude
            double latitude = 1000000.0;
            try {
                latitude = Double.valueOf(editLatitude.getText().toString());
            } catch (NumberFormatException e) {
            } finally {
                if (latitude <= -180.0 || latitude > 180.0) {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(R.string.messageLatitudeMissing),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // longitude
            double longitude = 1000000.0;
            try {
                longitude = Double.valueOf(editLongitude.getText().toString());
            } catch (NumberFormatException e) {
            } finally {
                if (longitude <= -180.0 || longitude > 180.0) {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(R.string.messageLongitudeMissing),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // optional point name
            String name = editName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                name = String.format("%1$f, %2$f", latitude, longitude);
            }

            // create point
            PointWrapper createdPoint = null;
            try {
                JSONObject jsonCreatedPoint = new JSONObject();
                jsonCreatedPoint.put("name", name);
                jsonCreatedPoint.put("type", Constants.POINT.GPS);
                jsonCreatedPoint.put("sub_type", getResources().getString(R.string.currentLocationName));
                jsonCreatedPoint.put("lat", latitude);
                jsonCreatedPoint.put("lon", longitude);
                jsonCreatedPoint.put("time", System.currentTimeMillis());
                createdPoint = new PointWrapper(getActivity(), jsonCreatedPoint);
            } catch (JSONException e) {
                createdPoint = null;
            } finally {
                if (createdPoint != null) {
                    // put into
                    PointUtility.putNewPoint(getActivity(), createdPoint, pointPutInto);
                    // add to user created point history
                    accessDatabaseInstance.addFavoritePointToProfile(
                            createdPoint, HistoryPointProfile.ID_USER_CREATED_POINTS);
                    // reload ui
                    if (childDialogCloseListener != null) {
                        childDialogCloseListener.childDialogClosed();
                    }
                    dismiss();
                } else {
                    SimpleMessageDialog.newInstance(
                            getResources().getString(R.string.messageCantCreatePointFromCoordinates))
                    .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
                }
            }
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            childDialogCloseListener = null;
        }
    }


    public static class SelectHistoryPointDialog extends DialogFragment implements HistoryPointProfileListener {

        private AccessDatabase accessDatabaseInstance;
        private ChildDialogCloseListener childDialogCloseListener;
        private InputMethodManager imm;
        private POIManager poiManagerInstance;
    	private SettingsManager settingsManagerInstance;
        private int historyPointProfileId, pointPutInto, listPosition;
        private String searchTerm;

        // query in progress vibration
        private Handler progressHandler;
        private ProgressUpdater progressUpdater;
        private Vibrator vibrator;

        // ui components
        private AutoCompleteTextView editSearch;
        private ImageButton buttonRefresh, buttonClearSearch;
        private ListView listViewPOI;
        private TextView labelHeading, labelEmptyListView;

        public static SelectHistoryPointDialog newInstance(int historyPointProfileId, int pointPutInto) {
            SelectHistoryPointDialog selectHistoryPointDialogInstance = new SelectHistoryPointDialog();
            Bundle args = new Bundle();
            args.putInt("historyPointProfileId", historyPointProfileId);
            args.putInt("pointPutInto", pointPutInto);
            selectHistoryPointDialogInstance.setArguments(args);
            return selectHistoryPointDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            poiManagerInstance = POIManager.getInstance(context);
    		settingsManagerInstance = SettingsManager.getInstance(context);
            if (getTargetFragment() != null
                    && getTargetFragment() instanceof ChildDialogCloseListener) {
                childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
            }
            // listen for intents
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_SHAKE_DETECTED);
            LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
            // progress updater
            this.progressHandler = new Handler();
            this.progressUpdater = new ProgressUpdater();
            this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                historyPointProfileId = savedInstanceState.getInt("historyPointProfileId");
                pointPutInto = savedInstanceState.getInt("pointPutInto");
                listPosition = savedInstanceState.getInt("listPosition");
                searchTerm = savedInstanceState.getString("searchTerm");
            } else {
                historyPointProfileId = getArguments().getInt("historyPointProfileId");
                pointPutInto = getArguments().getInt("pointPutInto");
                listPosition = 0;
                searchTerm = "";
            }

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.fragment_poi, nullParent);

            Button buttonSelectProfile = (Button) view.findViewById(R.id.buttonSelectProfile);
            buttonSelectProfile.setVisibility(View.GONE);

            editSearch = (AutoCompleteTextView) view.findViewById(R.id.editInput);
            editSearch.setText(searchTerm);
            editSearch.setHint(getResources().getString(R.string.dialogSearch));
            editSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        // cancel running request
                        if (poiManagerInstance.historyPointProfileRequestInProgress()) {
                            poiManagerInstance.cancelHistoryPointProfileRequest();
                        }
                        // add to search term history
                        if (! TextUtils.isEmpty(searchTerm)) {
                            settingsManagerInstance.getSearchTermHistory().addSearchTerm(searchTerm);
                        }
                        // reload poi profile
                        prepareHistoryPointRequest();
                        return true;
                    }
                    return false;
                }
            });
            editSearch.addTextChangedListener(new TextChangedListener<AutoCompleteTextView>(editSearch) {
                @Override public void onTextChanged(AutoCompleteTextView view, Editable s) {
                    searchTerm = editSearch.getText().toString();
                    if (! TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.GONE) {
                        buttonClearSearch.setVisibility(View.VISIBLE);
                    } else if (TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.VISIBLE) {
                        buttonClearSearch.setVisibility(View.GONE);
                    }
                }
            });
            // add auto complete suggestions
            ArrayAdapter<String> searchTermHistoryAdapter = new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_dropdown_item_1line,
                    settingsManagerInstance.getSearchTermHistory().getSearchTermList());
            editSearch.setAdapter(searchTermHistoryAdapter);
            // hide keyboard
            getActivity().getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            buttonClearSearch = (ImageButton) view.findViewById(R.id.buttonClearInput);
            buttonClearSearch.setContentDescription(getResources().getString(R.string.buttonClearSearch));
            buttonClearSearch.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    // clear edit text
                    editSearch.setText("");
                    // cancel running request
                    if (poiManagerInstance.historyPointProfileRequestInProgress()) {
                        poiManagerInstance.cancelHistoryPointProfileRequest();
                    }
                    // reload poi profile
                    prepareHistoryPointRequest();
                }
            });

            labelHeading = (TextView) view.findViewById(R.id.labelHeading);
            buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
            buttonRefresh.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if (poiManagerInstance.historyPointProfileRequestInProgress()) {
                        poiManagerInstance.cancelHistoryPointProfileRequest();
                    } else {
                        prepareHistoryPointRequest();
                    }
                }
            });

            listViewPOI = (ListView) view.findViewById(R.id.listView);
            listViewPOI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    final PointProfileObject selectedPoint = (PointProfileObject) parent.getItemAtPosition(position);
                    PopupMenu popupMore = new PopupMenu(getActivity(), view);
                    popupMore.inflate(R.menu.menu_list_view_select_point_dialog);
                    popupMore.getMenu().findItem(R.id.menuItemRemove).setVisible(false);
                    popupMore.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.menuItemSelect:
                                    PointUtility.putNewPoint(
                                            getActivity(), selectedPoint, pointPutInto);
                                    if (childDialogCloseListener != null) {
                                        childDialogCloseListener.childDialogClosed();
                                    }
                                    dismiss();
                                    return true;
                                case R.id.menuItemShowDetails:
                                    Intent pointDetailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                                    try {
                                        pointDetailsIntent.putExtra(
                                                Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED,
                                                selectedPoint.toJson().toString());
                                    } catch (JSONException e) {
                                        pointDetailsIntent.putExtra(
                                                Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED,
                                                "");
                                    }
                                    getActivity().startActivity(pointDetailsIntent);
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    popupMore.show();
                }
            });
            labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
            listViewPOI.setEmptyView(labelEmptyListView);

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(
                        HistoryPointProfile.getProfileMap(getActivity()).get(historyPointProfileId))
                .setView(view)
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
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
            // request poi
            prepareHistoryPointRequest();
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putInt("historyPointProfileId", historyPointProfileId);
            savedInstanceState.putInt("pointPutInto",  pointPutInto);
            savedInstanceState.putInt("listPosition",  listPosition);
            savedInstanceState.putString("searchTerm", searchTerm);
        }

        @Override public void onStop() {
            super.onStop();
            poiManagerInstance.invalidateHistoryPointProfileRequest((SelectHistoryPointDialog) this);
            progressHandler.removeCallbacks(progressUpdater);
        }

        private void prepareHistoryPointRequest() {
            // hide keyboard
            editSearch.dismissDropDown();
            imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
            // clear search button
            if (! TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.GONE) {
                buttonClearSearch.setVisibility(View.VISIBLE);
            } else if (TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.VISIBLE) {
                buttonClearSearch.setVisibility(View.GONE);
            }

            // heading
            labelHeading.setText(
                    getResources().getString(R.string.messagePleaseWait));
            buttonRefresh.setContentDescription(
                    getResources().getString(R.string.buttonCancel));
            buttonRefresh.setImageResource(R.drawable.cancel);
            // list view
            listViewPOI.setAdapter(null);
            listViewPOI.setOnScrollListener(null);
            labelEmptyListView.setVisibility(View.GONE);

            // start history point profile update request
            progressHandler.postDelayed(progressUpdater, 2000);
            poiManagerInstance.requestHistoryPointProfile(
                    (SelectHistoryPointDialog) this, historyPointProfileId);
        }

    	@Override public void historyPointProfileRequestFinished(int returnCode, String returnMessage, HistoryPointProfile historyPointProfile, boolean resetListPosition) {
            POISettings poiSettings = settingsManagerInstance.getPOISettings();
            buttonRefresh.setContentDescription(
                    getResources().getString(R.string.buttonRefresh));
            buttonRefresh.setImageResource(R.drawable.refresh);
            progressHandler.removeCallbacks(progressUpdater);

            if (historyPointProfile != null
                    && historyPointProfile.getPointProfileObjectList() != null) {
                ArrayList<PointProfileObject> historyPointList = historyPointProfile.getPointProfileObjectList();
                if (! TextUtils.isEmpty(searchTerm)) {
                    // filter history point list by search term
                    for (Iterator<PointProfileObject> iterator = historyPointList.iterator(); iterator.hasNext();) {
                        PointProfileObject object = iterator.next();
                        for (String word : searchTerm.split("\\s")) {
                            if (! object.toString().toLowerCase().contains(word.toLowerCase())) {
                                // object does not match
                                iterator.remove();
                                break;
                            }
                        }
                    }
                    Collections.sort(
                            historyPointList, new SortByNameASC());
                }

                // header field and list view
                if (! TextUtils.isEmpty(searchTerm)) {
                    labelHeading.setText(
                            String.format(
                                getResources().getString(R.string.labelSelectHistoryPointDialogHeaderSearch),
                                getResources().getQuantityString(
                                    R.plurals.result, historyPointList.size(), historyPointList.size()),
                                searchTerm,
                                StringUtility.formatProfileSortCriteria(
                                    getActivity(), Constants.SORT_CRITERIA.NAME_ASC))
                            );
                } else {
                    labelHeading.setText(
                            String.format(
                                getResources().getString(R.string.labelSelectHistoryPointDialogHeaderSuccess),
                                getResources().getQuantityString(
                                    R.plurals.poi, historyPointList.size(), historyPointList.size()),
                                StringUtility.formatProfileSortCriteria(
                                    getActivity(), historyPointProfile.getSortCriteria()))
                            );
                }
                listViewPOI.setAdapter(
                        new ArrayAdapter<PointProfileObject>(
                            getActivity(), android.R.layout.simple_list_item_1, historyPointList));

                // list position
                if (resetListPosition) {
                    listViewPOI.setSelection(0);
                } else {
                    listViewPOI.setSelection(listPosition);
                }
                listViewPOI.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
                    @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        if (listPosition != firstVisibleItem) {
                            listPosition = firstVisibleItem;
                        }
                    }
                });

            } else {
                labelHeading.setText(returnMessage);
            }
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            childDialogCloseListener = null;
            progressHandler.removeCallbacks(progressUpdater);
            // unregister broadcast receiver
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        }

        private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                    vibrator.vibrate(250);
                    prepareHistoryPointRequest();
                }
            }
        };

        private class ProgressUpdater implements Runnable {
            public void run() {
                vibrator.vibrate(50);
                progressHandler.postDelayed(this, 2000);
            }
        }
    }


    public static class SelectPOIDialog extends DialogFragment implements POIProfileListener {

        private AccessDatabase accessDatabaseInstance;
        private ChildDialogCloseListener childDialogCloseListener;
        private InputMethodManager imm;
        private POIManager poiManagerInstance;
    	private SettingsManager settingsManagerInstance;
        private int poiProfileId, pointPutInto, listPosition;
        private String searchTerm;

        // query in progress vibration
        private Handler progressHandler;
        private ProgressUpdater progressUpdater;
        private Vibrator vibrator;

        // ui components
        private AutoCompleteTextView editSearch;
        private ImageButton buttonRefresh, buttonClearSearch;
        private ListView listViewPOI;
        private TextView labelHeading, labelEmptyListView, labelMoreResultsFooter;

        public static SelectPOIDialog newInstance(int poiProfileId, int pointPutInto) {
            SelectPOIDialog selectPOIDialogInstance = new SelectPOIDialog();
            Bundle args = new Bundle();
            args.putInt("poiProfileId", poiProfileId);
            args.putInt("pointPutInto", pointPutInto);
            selectPOIDialogInstance.setArguments(args);
            return selectPOIDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            poiManagerInstance = POIManager.getInstance(context);
    		settingsManagerInstance = SettingsManager.getInstance(context);
            if (getTargetFragment() != null
                    && getTargetFragment() instanceof ChildDialogCloseListener) {
                childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
            }
            // listen for intents
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_SHAKE_DETECTED);
            LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
            // progress updater
            this.progressHandler = new Handler();
            this.progressUpdater = new ProgressUpdater();
            this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                poiProfileId = savedInstanceState.getInt("poiProfileId");
                pointPutInto = savedInstanceState.getInt("pointPutInto");
                listPosition = savedInstanceState.getInt("listPosition");
                searchTerm = savedInstanceState.getString("searchTerm");
            } else {
                poiProfileId = getArguments().getInt("poiProfileId");
                pointPutInto = getArguments().getInt("pointPutInto");
                listPosition = 0;
                searchTerm = accessDatabaseInstance.getSearchTermOfPOIProfile(poiProfileId);
            }

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.fragment_poi, nullParent);

            Button buttonSelectProfile = (Button) view.findViewById(R.id.buttonSelectProfile);
            buttonSelectProfile.setVisibility(View.GONE);

            editSearch = (AutoCompleteTextView) view.findViewById(R.id.editInput);
            editSearch.setHint(getResources().getString(R.string.dialogSearch));
            editSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            editSearch.setText(searchTerm);
            editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        // cancel running request
                        if (poiManagerInstance.requestInProgress()) {
                            poiManagerInstance.cancelRequest();
                        }
                        // update poi profile search term
                        String searchTermFromDatabase = accessDatabaseInstance.getSearchTermOfPOIProfile(poiProfileId);
                        if (! searchTerm.equals(searchTermFromDatabase)) {
                            accessDatabaseInstance.updateSearchTermOfPOIProfile(poiProfileId, searchTerm);
                            if (! TextUtils.isEmpty(searchTerm)) {
                                settingsManagerInstance.getSearchTermHistory().addSearchTerm(searchTerm);
                            }
                        }
                        // reload poi profile
                        preparePOIRequest(POIManager.ACTION_UPDATE);
                        return true;
                    }
                    return false;
                }
            });
            editSearch.addTextChangedListener(new TextChangedListener<AutoCompleteTextView>(editSearch) {
                @Override public void onTextChanged(AutoCompleteTextView view, Editable s) {
                    searchTerm = editSearch.getText().toString();
                    if (! TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.GONE) {
                        buttonClearSearch.setVisibility(View.VISIBLE);
                    } else if (TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.VISIBLE) {
                        buttonClearSearch.setVisibility(View.GONE);
                    }
                }
            });
            // add auto complete suggestions
            ArrayAdapter<String> searchTermHistoryAdapter = new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_dropdown_item_1line,
                    settingsManagerInstance.getSearchTermHistory().getSearchTermList());
            editSearch.setAdapter(searchTermHistoryAdapter);
            // hide keyboard
            getActivity().getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            buttonClearSearch = (ImageButton) view.findViewById(R.id.buttonClearInput);
            buttonClearSearch.setContentDescription(getResources().getString(R.string.buttonClearSearch));
            buttonClearSearch.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    // clear edit text
                    editSearch.setText("");
                    // cancel running request
                    if (poiManagerInstance.requestInProgress()) {
                        poiManagerInstance.cancelRequest();
                    }
                    // update poi profile search term
                    String searchTermFromDatabase = accessDatabaseInstance.getSearchTermOfPOIProfile(poiProfileId);
                    if (! TextUtils.isEmpty(searchTermFromDatabase)) {
                        accessDatabaseInstance.updateSearchTermOfPOIProfile(poiProfileId, "");
                    }
                    // reload poi profile
                    preparePOIRequest(POIManager.ACTION_UPDATE);
                }
            });

            labelHeading = (TextView) view.findViewById(R.id.labelHeading);
            buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
            buttonRefresh.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if (poiManagerInstance.requestInProgress()) {
                        poiManagerInstance.cancelRequest();
                    } else {
                        preparePOIRequest(POIManager.ACTION_UPDATE);
                    }
                }
            });

            listViewPOI = (ListView) view.findViewById(R.id.listView);
            listViewPOI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    final PointProfileObject selectedPoint = (PointProfileObject) parent.getItemAtPosition(position);
                    PopupMenu popupMore = new PopupMenu(getActivity(), view);
                    popupMore.inflate(R.menu.menu_list_view_select_point_dialog);
                    popupMore.getMenu().findItem(R.id.menuItemRemove).setVisible(false);
                    popupMore.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.menuItemSelect:
                                    PointUtility.putNewPoint(
                                            getActivity(), selectedPoint, pointPutInto);
                                    if (childDialogCloseListener != null) {
                                        childDialogCloseListener.childDialogClosed();
                                    }
                                    dismiss();
                                    return true;
                                case R.id.menuItemShowDetails:
                                    Intent pointDetailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                                    try {
                                        pointDetailsIntent.putExtra(
                                                Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED,
                                                selectedPoint.toJson().toString());
                                    } catch (JSONException e) {
                                        pointDetailsIntent.putExtra(
                                                Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED,
                                                "");
                                    }
                                    getActivity().startActivity(pointDetailsIntent);
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    popupMore.show();
                }
            });

            labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
            labelEmptyListView.setText(getResources().getString(R.string.labelMoreResults));
            labelEmptyListView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    // start poi profile update request
                    preparePOIRequest(POIManager.ACTION_MORE_RESULTS);
                }
            });
            listViewPOI.setEmptyView(labelEmptyListView);

            View footerView = inflater.inflate(R.layout.layout_single_text_view, null, false);
            labelMoreResultsFooter = (TextView) footerView.findViewById(R.id.label);
            labelMoreResultsFooter.setText(getResources().getString(R.string.labelMoreResults));
            labelMoreResultsFooter.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    // start poi profile update request
                    preparePOIRequest(POIManager.ACTION_MORE_RESULTS);
                }
            });

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(accessDatabaseInstance.getNameOfPOIProfile(poiProfileId))
                .setView(view)
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
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
            // request poi
            preparePOIRequest(POIManager.ACTION_UPDATE);
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putInt("poiProfileId", poiProfileId);
            savedInstanceState.putInt("pointPutInto",  pointPutInto);
            savedInstanceState.putInt("listPosition",  listPosition);
            savedInstanceState.putString("searchTerm", searchTerm);
        }

        @Override public void onStop() {
            super.onStop();
            poiManagerInstance.invalidateRequest((SelectPOIDialog) this);
            progressHandler.removeCallbacks(progressUpdater);
        }

        private void preparePOIRequest(int requestAction) {
            // hide keyboard
            editSearch.dismissDropDown();
            imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
            // clear search button
            if (! TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.GONE) {
                buttonClearSearch.setVisibility(View.VISIBLE);
            } else if (TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.VISIBLE) {
                buttonClearSearch.setVisibility(View.GONE);
            }

            // heading
            labelHeading.setText(
                    getResources().getString(R.string.messagePleaseWait));
            buttonRefresh.setContentDescription(
                    getResources().getString(R.string.buttonCancel));
            buttonRefresh.setImageResource(R.drawable.cancel);
            // list view
            listViewPOI.setAdapter(null);
            listViewPOI.setOnScrollListener(null);
            if (listViewPOI.getFooterViewsCount() > 0) {
                listViewPOI.removeFooterView(labelMoreResultsFooter);
            }
            labelEmptyListView.setVisibility(View.GONE);

            // start poi profile update request
            progressHandler.postDelayed(progressUpdater, 2000);
            poiManagerInstance.requestPOIProfile(
                    (SelectPOIDialog) this, poiProfileId, requestAction);
        }

        @Override public void poiProfileRequestFinished(int returnCode, String returnMessage, POIProfile poiProfile, boolean resetListPosition) {
            POISettings poiSettings = settingsManagerInstance.getPOISettings();
            buttonRefresh.setContentDescription(
                    getResources().getString(R.string.buttonRefresh));
            buttonRefresh.setImageResource(R.drawable.refresh);
            progressHandler.removeCallbacks(progressUpdater);

            if (poiProfile != null
                    && poiProfile.getPointProfileObjectList() != null) {
                ArrayList<PointProfileObject> listOfAllPOI = poiProfile.getPointProfileObjectList();
                // header field
                if (! TextUtils.isEmpty(poiProfile.getSearchTerm())) {
                    labelHeading.setText(
                            String.format(
                                getResources().getString(R.string.labelPOIFragmentHeaderSearch),
                                getResources().getQuantityString(
                                    R.plurals.result, listOfAllPOI.size(), listOfAllPOI.size()),
                                poiProfile.getSearchTerm(),
                                getResources().getQuantityString(
                                    R.plurals.meter, poiProfile.getLookupRadius(), poiProfile.getLookupRadius()))
                            );
                } else {
                    labelHeading.setText(
                            String.format(
                                getResources().getString(R.string.labelPOIFragmentHeaderSuccess),
                                getResources().getQuantityString(
                                    R.plurals.poi, listOfAllPOI.size(), listOfAllPOI.size()),
                                getResources().getQuantityString(
                                    R.plurals.meter, poiProfile.getLookupRadius(), poiProfile.getLookupRadius()))
                            );
                }

                // fill listview
                listViewPOI.setAdapter(
                        new POIProfilePointAdapter(
                            getActivity(), listOfAllPOI));
                // more results
                if (listViewPOI.getAdapter().getCount() == 0) {
                    if (poiProfile.getRadius() < poiProfile.getMaximalRadius()
                            && poiProfile.getNumberOfResults() < poiProfile.getMaximalNumberOfResults()) {
                        labelEmptyListView.setVisibility(View.VISIBLE);
                            }
                } else {
                    if (poiProfile.getRadius() < poiProfile.getMaximalRadius()
                            && poiProfile.getNumberOfResults() < poiProfile.getMaximalNumberOfResults()) {
                        listViewPOI.addFooterView(labelMoreResultsFooter, null, true);
                            }
                }

                // list position
                if (resetListPosition) {
                    listViewPOI.setSelection(0);
                } else {
                    listViewPOI.setSelection(listPosition);
                }
                listViewPOI.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
                    @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        if (listPosition != firstVisibleItem) {
                            listPosition = firstVisibleItem;
                        }
                    }
                });

            } else {
                labelHeading.setText(returnMessage);
            }
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            childDialogCloseListener = null;
            progressHandler.removeCallbacks(progressUpdater);
            // unregister broadcast receiver
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        }

        private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                    vibrator.vibrate(250);
                    preparePOIRequest(POIManager.ACTION_UPDATE);
                }
            }
        };

        private class ProgressUpdater implements Runnable {
            public void run() {
                vibrator.vibrate(50);
                progressHandler.postDelayed(this, 2000);
            }
        }
    }

}
