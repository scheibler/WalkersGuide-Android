package org.walkersguide.android.ui.dialog;

import java.util.ArrayList;
import java.util.TreeMap;

import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.FavoritesProfile;
import org.walkersguide.android.data.profile.POIProfile;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.server.AddressManager;
import org.walkersguide.android.helper.PointUtility;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.AddressListener;
import org.walkersguide.android.listener.ChildDialogCloseListener;
import org.walkersguide.android.listener.FavoritesProfileListener;
import org.walkersguide.android.listener.POIProfileListener;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.FavoritesManager;
import org.walkersguide.android.server.POIManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import android.content.Intent;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.dialog.SelectFavoritesProfileDialog;
import org.walkersguide.android.ui.dialog.SelectPOIProfileDialog;
import org.walkersguide.android.listener.SelectFavoritesProfileListener;
import org.walkersguide.android.listener.SelectPOIProfileListener;
import android.content.BroadcastReceiver;
import android.widget.AbsListView;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.AutoCompleteTextView;
import org.json.JSONObject;
import android.text.TextUtils;


public class SelectPointDialog extends DialogFragment
    implements ChildDialogCloseListener, SelectFavoritesProfileListener, SelectPOIProfileListener {

    private ChildDialogCloseListener childDialogCloseListener;
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
                case Constants.POINT_SELECT_FROM.FROM_FAVORITES:
                    formattedPointSelectFromArray[i] = getResources().getString(R.string.pointSelectFromFavorites);
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

    @Override public void favoritesProfileSelected(int favoritesProfileId) {
        SelectFavoriteDialog selectFavoriteDialog = SelectFavoriteDialog.newInstance(favoritesProfileId, pointPutInto);
        selectFavoriteDialog.setTargetFragment(SelectPointDialog.this, 1);
        selectFavoriteDialog.show(
                getActivity().getSupportFragmentManager(), "SelectFavoriteDialog");
    }

    @Override public void poiProfileSelected(int poiProfileId) {
        SelectPOIDialog selectPOIDialog = SelectPOIDialog.newInstance(poiProfileId, pointPutInto);
        selectPOIDialog.setTargetFragment(SelectPointDialog.this, 1);
        selectPOIDialog.show(
                getActivity().getSupportFragmentManager(), "SelectPOIDialog");
    }

    @Override public void childDialogClosed() {
        close();
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        childDialogCloseListener = null;
    }

    private void executeAction(int pointSelectFromIndex) {
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
            case Constants.POINT_SELECT_FROM.FROM_FAVORITES:
                SelectFavoritesProfileDialog selectFavoritesProfileDialog = SelectFavoritesProfileDialog.newInstance(-1);
                selectFavoritesProfileDialog.setTargetFragment(SelectPointDialog.this, 1);
                selectFavoritesProfileDialog.show(
                        getActivity().getSupportFragmentManager(), "SelectFavoritesProfileDialog");
                break;
            case Constants.POINT_SELECT_FROM.FROM_POI:
                SelectPOIProfileDialog selectPOIProfileDialog = SelectPOIProfileDialog.newInstance(-1);
                selectPOIProfileDialog.setTargetFragment(SelectPointDialog.this, 1);
                selectPOIProfileDialog.show(
                        getActivity().getSupportFragmentManager(), "SelectPOIProfileDialog");
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

            ImageButton buttonDelete = (ImageButton) view.findViewById(R.id.buttonDelete);
            buttonDelete.setOnClickListener(new View.OnClickListener() {
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
                    accessDatabaseInstance.addPointToFavoritesProfile(createdPoint, FavoritesProfile.ID_USER_CREATED_POINTS);
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


    public static class SelectFavoriteDialog extends DialogFragment implements FavoritesProfileListener {

        private AccessDatabase accessDatabaseInstance;
        private ChildDialogCloseListener childDialogCloseListener;
        private FavoritesManager favoritesManagerInstance;
        private int favoritesProfileId, pointPutInto, listPosition;

        // query in progress vibration
        private Handler progressHandler;
        private ProgressUpdater progressUpdater;
        private Vibrator vibrator;

        // ui components
        private ListView listViewPOI;
        private TextView labelListViewEmpty;

        public static SelectFavoriteDialog newInstance(int favoritesProfileId, int pointPutInto) {
            SelectFavoriteDialog selectFavoriteDialogInstance = new SelectFavoriteDialog();
            Bundle args = new Bundle();
            args.putInt("favoritesProfileId", favoritesProfileId);
            args.putInt("pointPutInto", pointPutInto);
            selectFavoriteDialogInstance.setArguments(args);
            return selectFavoriteDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            favoritesManagerInstance = FavoritesManager.getInstance(context);
            if (getTargetFragment() != null
                    && getTargetFragment() instanceof ChildDialogCloseListener) {
                childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
            }
            // progress updater
            this.progressHandler = new Handler();
            this.progressUpdater = new ProgressUpdater();
            this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                favoritesProfileId = savedInstanceState.getInt("favoritesProfileId");
                pointPutInto = savedInstanceState.getInt("pointPutInto");
                listPosition = savedInstanceState.getInt("listPosition");
            } else {
                favoritesProfileId = getArguments().getInt("favoritesProfileId");
                pointPutInto = getArguments().getInt("pointPutInto");
                listPosition = 0;
            }


            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_list_view, nullParent);

            listViewPOI = (ListView) view.findViewById(R.id.listView);
            listViewPOI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    PointUtility.putNewPoint(
                            getActivity(),
                            (PointProfileObject) parent.getItemAtPosition(position),
                            pointPutInto);
                    if (childDialogCloseListener != null) {
                        childDialogCloseListener.childDialogClosed();
                    }
                    dismiss();
                }
            });
            listViewPOI.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                    PointProfileObject pointProfileObject = (PointProfileObject) parent.getItemAtPosition(position);
                    Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                    try {
                        detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, pointProfileObject.toJson().toString());
                    } catch (JSONException e) {
                        detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                    }
                    startActivity(detailsIntent);
                    return true;
                }
            });

            labelListViewEmpty = (TextView) view.findViewById(R.id.labelListViewEmpty);
            listViewPOI.setEmptyView(labelListViewEmpty);

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(accessDatabaseInstance.getNameOfFavoritesProfile(favoritesProfileId))
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogUpdate),
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
                // positive button: update
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        // update favorites
                        requestFavoritesProfile();
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
            // request poi
            requestFavoritesProfile();
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putInt("favoritesProfileId", favoritesProfileId);
            savedInstanceState.putInt("pointPutInto",  pointPutInto);
            savedInstanceState.putInt("listPosition",  listPosition);
        }

        @Override public void onStop() {
            super.onStop();
            favoritesManagerInstance.invalidateFavoritesProfileRequest((SelectFavoriteDialog) this);
            progressHandler.removeCallbacks(progressUpdater);
            // list view
            listViewPOI.setAdapter(null);
            listViewPOI.setOnScrollListener(null);
        }

        private void requestFavoritesProfile() {
            // start or cancel search
            if (favoritesManagerInstance.favoritesProfileRequestInProgress()) {
                favoritesManagerInstance.cancelFavoritesProfileRequest();
            } else {
                // update ui
                listViewPOI.setAdapter(null);
                listViewPOI.setOnScrollListener(null);
                labelListViewEmpty.setText(
                        getResources().getString(R.string.messagePleaseWait));
                final AlertDialog dialog = (AlertDialog)getDialog();
                if(dialog != null) {
                    Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    buttonPositive.setText(getResources().getString(R.string.dialogCancel));
                }
                // search in background
                favoritesManagerInstance.requestFavoritesProfile(
                        (SelectFavoriteDialog) this, favoritesProfileId);
                progressHandler.postDelayed(progressUpdater, 2000);
            }
        }

    	@Override public void favoritesProfileRequestFinished(int returnCode, String returnMessage, FavoritesProfile favoritesProfile, boolean resetListPosition) {
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setText(getResources().getString(R.string.dialogUpdate));
            }
            progressHandler.removeCallbacks(progressUpdater);

            if (favoritesProfile != null
                    && favoritesProfile.getPointProfileObjectList() != null) {
                if(dialog != null) {
                    dialog.setTitle(
                            String.format(
                                "%1$s: %2$s, %3$s",
                                accessDatabaseInstance.getNameOfFavoritesProfile(favoritesProfileId),
                                getResources().getQuantityString(
                                    R.plurals.favorite, favoritesProfile.getPointProfileObjectList().size(), favoritesProfile.getPointProfileObjectList().size()),
                                StringUtility.formatProfileSortCriteria(
                                    getActivity(), favoritesProfile.getSortCriteria()))
                            );
                }

                listViewPOI.setAdapter(
                        new ArrayAdapter<PointProfileObject>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            favoritesProfile.getPointProfileObjectList())
                        );
                labelListViewEmpty.setText("");

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
                if(dialog != null) {
                    dialog.setTitle(accessDatabaseInstance.getNameOfFavoritesProfile(favoritesProfileId));
                }
                labelListViewEmpty.setText(returnMessage);
            }

            // error message dialog
            if (! (returnCode == Constants.RC.OK || returnCode == Constants.RC.CANCELLED)) {
                SimpleMessageDialog.newInstance(returnMessage)
                    .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
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
                if (intent.getAction().equals(Constants.ACTION_UPDATE_UI)) {
                    requestFavoritesProfile();
                } else if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                    vibrator.vibrate(250);
                    requestFavoritesProfile();
                } else if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                        && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD3.ID) {
                    requestFavoritesProfile();
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
        private POIManager poiManagerInstance;
        private int poiProfileId, pointPutInto, listPosition;

        // query in progress vibration
        private Handler progressHandler;
        private ProgressUpdater progressUpdater;
        private Vibrator vibrator;

        // ui components
        private ListView listViewPOI;
        private TextView labelListViewEmpty;

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
            poiManagerInstance = POIManager.getInstance(context);
            if (getTargetFragment() != null
                    && getTargetFragment() instanceof ChildDialogCloseListener) {
                childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
            }
            // listen for intents
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_NEW_LOCATION);
            filter.addAction(Constants.ACTION_SHAKE_DETECTED);
            filter.addAction(Constants.ACTION_UPDATE_UI);
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
            } else {
                poiProfileId = getArguments().getInt("poiProfileId");
                pointPutInto = getArguments().getInt("pointPutInto");
                listPosition = 0;
            }

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_list_view, nullParent);

            listViewPOI = (ListView) view.findViewById(R.id.listView);
            listViewPOI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    PointUtility.putNewPoint(
                            getActivity(),
                            (PointProfileObject) parent.getItemAtPosition(position),
                            pointPutInto);
                    if (childDialogCloseListener != null) {
                        childDialogCloseListener.childDialogClosed();
                    }
                    dismiss();
                }
            });
            listViewPOI.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                    PointProfileObject pointProfileObject = (PointProfileObject) parent.getItemAtPosition(position);
                    Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                    try {
                        detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, pointProfileObject.toJson().toString());
                    } catch (JSONException e) {
                        detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                    }
                    startActivity(detailsIntent);
                    return true;
                }
            });

            labelListViewEmpty = (TextView) view.findViewById(R.id.labelListViewEmpty);
            listViewPOI.setEmptyView(labelListViewEmpty);

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(accessDatabaseInstance.getNameOfPOIProfile(poiProfileId))
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogUpdate),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setNeutralButton(
                        getResources().getString(R.string.dialogMore),
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
                // positive button: update
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        // update poi
                        requestPOIProfile(POIManager.ACTION_UPDATE);
                    }
                });
                // neutral  button: more results
                Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        // request more poi
                        requestPOIProfile(POIManager.ACTION_MORE_RESULTS);
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
            // request poi
            requestPOIProfile(POIManager.ACTION_UPDATE);
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putInt("poiProfileId", poiProfileId);
            savedInstanceState.putInt("pointPutInto",  pointPutInto);
            savedInstanceState.putInt("listPosition",  listPosition);
        }

        @Override public void onStop() {
            super.onStop();
            poiManagerInstance.invalidateRequest((SelectPOIDialog) this);
            progressHandler.removeCallbacks(progressUpdater);
            // list view
            listViewPOI.setAdapter(null);
            listViewPOI.setOnScrollListener(null);
        }

        private void requestPOIProfile(int requestAction) {
            // start or cancel search
            if (poiManagerInstance.requestInProgress()) {
                poiManagerInstance.cancelRequest();
            } else {
                // update ui
                listViewPOI.setAdapter(null);
                listViewPOI.setOnScrollListener(null);
                labelListViewEmpty.setText(
                        getResources().getString(R.string.messagePleaseWait));
                final AlertDialog dialog = (AlertDialog)getDialog();
                if(dialog != null) {
                    Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    buttonPositive.setText(getResources().getString(R.string.dialogCancel));
                    // hide more results button for the moment
                    Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                    buttonNeutral.setVisibility(View.GONE);
                }
                // search in background
                poiManagerInstance.requestPOIProfile(
                        (SelectPOIDialog) this, poiProfileId, requestAction);
                progressHandler.postDelayed(progressUpdater, 2000);
            }
        }

        @Override public void poiProfileRequestFinished(int returnCode, String returnMessage, POIProfile poiProfile, boolean resetListPosition) {
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setText(getResources().getString(R.string.dialogUpdate));
            }
            progressHandler.removeCallbacks(progressUpdater);

            if (poiProfile != null
                    && poiProfile.getPointProfileObjectList() != null) {
                if(dialog != null) {
                    dialog.setTitle(
                            String.format(
                                "%1$s: %2$s, %3$s",
                                accessDatabaseInstance.getNameOfPOIProfile(poiProfileId),
                                getResources().getQuantityString(
                                    R.plurals.poi, poiProfile.getPointProfileObjectList().size(), poiProfile.getPointProfileObjectList().size()),
                                getResources().getQuantityString(
                                    R.plurals.meter, poiProfile.getLookupRadius(), poiProfile.getLookupRadius()))
                            );
                }

                listViewPOI.setAdapter(
                        new ArrayAdapter<PointProfileObject>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            poiProfile.getPointProfileObjectList())
                        );
                labelListViewEmpty.setText("");

                // more results
                if(dialog != null
                        && poiProfile.getRadius() < poiProfile.getMaximalRadius()
                        && poiProfile.getNumberOfResults() < poiProfile.getMaximalNumberOfResults()) {
                    Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                    buttonNeutral.setVisibility(View.VISIBLE);
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
                if(dialog != null) {
                    dialog.setTitle(accessDatabaseInstance.getNameOfPOIProfile(poiProfileId));
                }
                labelListViewEmpty.setText(returnMessage);
            }

            // error message dialog
            if (! (returnCode == Constants.RC.OK || returnCode == Constants.RC.CANCELLED)) {
                SimpleMessageDialog.newInstance(returnMessage)
                    .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
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
                if (intent.getAction().equals(Constants.ACTION_UPDATE_UI)) {
                    requestPOIProfile(POIManager.ACTION_UPDATE);
                } else if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                    vibrator.vibrate(250);
                    requestPOIProfile(POIManager.ACTION_UPDATE);
                } else if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                        && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD3.ID) {
                    requestPOIProfile(POIManager.ACTION_UPDATE);
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
