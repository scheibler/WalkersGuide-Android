package org.walkersguide.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import android.support.v4.app.DialogFragment;

import android.text.TextUtils;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.HistoryPointProfile;
import org.walkersguide.android.helper.PointUtility;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.AddressManager;
import org.walkersguide.android.server.AddressManager.AddressListener;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.dialog.SelectPointWrapperFromListDialog;
import org.walkersguide.android.ui.dialog.SelectPointWrapperFromListDialog.PointWrapperSelectedListener;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog.ChildDialogCloseListener;
import org.walkersguide.android.ui.fragment.main.POIFragment;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;

import timber.log.Timber;


public class PointInputDialog extends DialogFragment implements ChildDialogCloseListener {

    private ChildDialogCloseListener childDialogCloseListener;
    private PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;
    private int pointPutInto;
    private PointWrapper selectedPoint;

    public static PointInputDialog newInstance(int pointPutInto) {
        PointInputDialog pointInputDialogInstance = new PointInputDialog();
        Bundle args = new Bundle();
        args.putInt("pointPutInto", pointPutInto);
        pointInputDialogInstance.setArguments(args);
        return pointInputDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof ChildDialogCloseListener) {
            childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
        }
        positionManagerInstance = PositionManager.getInstance(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
        selectedPoint = null;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        pointPutInto = getArguments().getInt("pointPutInto");
        Timber.d("pointPutInto: %1$d", pointPutInto);
        int[] inputCategoryArray = Constants.PointSelectFromValueArrayWithoutCurrentLocation;
        String dialogTitle = "";
        switch (pointPutInto) {
            case Constants.POINT_PUT_INTO.START:
                selectedPoint = settingsManagerInstance.getRouteSettings().getStartPoint();
                inputCategoryArray = Constants.PointSelectFromValueArray;
                dialogTitle = getResources().getString(R.string.pointInputDialogNameStart);
                break;
            case Constants.POINT_PUT_INTO.DESTINATION:
                selectedPoint = settingsManagerInstance.getRouteSettings().getDestinationPoint();
                dialogTitle = getResources().getString(R.string.pointInputDialogNameDestination);
                break;
            case Constants.POINT_PUT_INTO.SIMULATION:
                selectedPoint = settingsManagerInstance.getLocationSettings().getSimulatedLocation();
                dialogTitle = getResources().getString(R.string.pointInputDialogNameSimulation);
                break;
            default:
                if (pointPutInto >= Constants.POINT_PUT_INTO.VIA) {
                    // via point
                    ArrayList<PointWrapper> viaPointList = settingsManagerInstance.getRouteSettings().getViaPointList();
                    int viaPointIndex = pointPutInto - Constants.POINT_PUT_INTO.VIA;
                    if (viaPointIndex >= 0 && viaPointIndex < viaPointList.size()) {
                        selectedPoint = viaPointList.get(viaPointIndex);
                    }
                    dialogTitle = String.format(
                            getResources().getString(R.string.pointInputDialogNameVia),
                            (pointPutInto - Constants.POINT_PUT_INTO.VIA) + 1);
                }
                break;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_point_input, nullParent);

        // selected point
        TextView labelSelectedPointHeading = (TextView) view.findViewById(R.id.labelSelectedPointHeading);
        labelSelectedPointHeading.setVisibility(View.GONE);
        TextView labelSelectedPointName = (TextView) view.findViewById(R.id.labelSelectedPointName);
        labelSelectedPointName.setVisibility(View.GONE);

        Button buttonSelectedPointDetails = (Button) view.findViewById(R.id.buttonSelectedPointDetails);
        buttonSelectedPointDetails.setVisibility(View.GONE);
        buttonSelectedPointDetails.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
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

        Button buttonSelectedPointRemove = (Button) view.findViewById(R.id.buttonSelectedPointRemove);
        buttonSelectedPointRemove.setVisibility(View.GONE);
        buttonSelectedPointRemove.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                switch (pointPutInto) {
                    case Constants.POINT_PUT_INTO.START:
                        settingsManagerInstance.getRouteSettings().removeStartPoint();
                        break;
                    case Constants.POINT_PUT_INTO.DESTINATION:
                        settingsManagerInstance.getRouteSettings().removeDestinationPoint();
                        break;
                    case Constants.POINT_PUT_INTO.SIMULATION:
                        settingsManagerInstance.getLocationSettings().removeSimulatedLocation();
                        break;
                    default:
                        if (pointPutInto >= Constants.POINT_PUT_INTO.VIA) {
                            settingsManagerInstance.getRouteSettings().removeViaPointAtIndex(
                                    pointPutInto-Constants.POINT_PUT_INTO.VIA);
                        }
                        break;
                }
                close();
            }
        });

        if (selectedPoint != null) {
            labelSelectedPointHeading.setVisibility(View.VISIBLE);
            labelSelectedPointName.setText(selectedPoint.getPoint().getName());
            labelSelectedPointName.setVisibility(View.VISIBLE);
            buttonSelectedPointDetails.setVisibility(View.VISIBLE);
            buttonSelectedPointRemove.setVisibility(View.VISIBLE);
        }

        // input categories
        TextView labelInputCategoryHeading = (TextView) view.findViewById(R.id.labelHeading);
        labelInputCategoryHeading.setText(getResources().getString(R.string.labelInputCategoryHeading));
        ImageButton buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setVisibility(View.GONE);
        ListView listViewInputCategories = (ListView) view.findViewById(R.id.listView);
        listViewInputCategories.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                executeAction((Integer) parent.getItemAtPosition(position));
            }
        });
        listViewInputCategories.setAdapter(new InputCategoryAdapter(getActivity(), inputCategoryArray));
        TextView labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setVisibility(View.GONE);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(dialogTitle)
            .setView(view)
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

    private void executeAction(int selectedInputCategoryId) {
        switch (selectedInputCategoryId) {
            case Constants.POINT_SELECT_FROM.CURRENT_LOCATION:
                GetCurrentPositionDialog getCurrentPositionDialog = GetCurrentPositionDialog.newInstance(pointPutInto);
                getCurrentPositionDialog.setTargetFragment(PointInputDialog.this, 1);
                getCurrentPositionDialog.show(
                        getActivity().getSupportFragmentManager(), "GetCurrentPositionDialog");
                break;
            case Constants.POINT_SELECT_FROM.ENTER_ADDRESS:
                EnterAddressDialog enterAddressDialog = EnterAddressDialog.newInstance(pointPutInto);
                enterAddressDialog.setTargetFragment(PointInputDialog.this, 1);
                enterAddressDialog.show(
                        getActivity().getSupportFragmentManager(), "EnterAddressDialog");
                break;
            case Constants.POINT_SELECT_FROM.ENTER_COORDINATES:
                EnterCoordinatesDialog enterCoordinatesDialog = EnterCoordinatesDialog.newInstance(pointPutInto);
                enterCoordinatesDialog.setTargetFragment(PointInputDialog.this, 1);
                enterCoordinatesDialog.show(
                        getActivity().getSupportFragmentManager(), "EnterCoordinatesDialog");
                break;
            case Constants.POINT_SELECT_FROM.FROM_HISTORY_POINTS:
                // reset to all points category
                settingsManagerInstance.getPOISettings()
                    .setSelectedHistoryPointProfileId(HistoryPointProfile.ID_ALL_POINTS);
                // open dialog
                POIFragment selectHistoryPointDialog = POIFragment.newInstance(
                        POIFragment.ContentType.HISTORY_POINTS, pointPutInto);
                selectHistoryPointDialog.setTargetFragment(PointInputDialog.this, 1);
                selectHistoryPointDialog.show(getActivity().getSupportFragmentManager(), "SelectHistoryPointDialog");
                break;
            case Constants.POINT_SELECT_FROM.FROM_POI:
                POIFragment selectPOIDialog = POIFragment.newInstance(
                        POIFragment.ContentType.POI, pointPutInto);
                selectPOIDialog.setTargetFragment(PointInputDialog.this, 1);
                selectPOIDialog.show(getActivity().getSupportFragmentManager(), "SelectPOIDialog");
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

    public class InputCategoryAdapter extends ArrayAdapter<Integer> {
        private LayoutInflater m_inflater;
        private ArrayList<Integer> categoryIdList;
        // constructor
        public InputCategoryAdapter(Context context, int[] categoryIdArray) {
            super(context, R.layout.layout_single_text_view);
            m_inflater = LayoutInflater.from(context);
            this.categoryIdList = new ArrayList<Integer>();
            for (int i=0; i<categoryIdArray.length; i++) {
                this.categoryIdList.add(categoryIdArray[i]);
            }
        }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            // load item layout
            EntryHolder holder;
            if (convertView == null) {
                convertView = m_inflater.inflate(R.layout.layout_single_text_view, parent, false);
                holder = new EntryHolder();
                holder.label = (TextView) convertView.findViewById(R.id.label);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            // fill label
            String categoryName = null;
            switch (getItem(position)) {
                case Constants.POINT_SELECT_FROM.CURRENT_LOCATION:
                    categoryName = getResources().getString(R.string.pointSelectFromCurrentLocation);
                    break;
                case Constants.POINT_SELECT_FROM.ENTER_ADDRESS:
                    categoryName = getResources().getString(R.string.pointSelectFromEnterAddress);
                    break;
                case Constants.POINT_SELECT_FROM.ENTER_COORDINATES:
                    categoryName = getResources().getString(R.string.pointSelectFromEnterCoordinates);
                    break;
                case Constants.POINT_SELECT_FROM.FROM_HISTORY_POINTS:
                    categoryName = getResources().getString(R.string.pointSelectFromHistoryPoints);
                    break;
                case Constants.POINT_SELECT_FROM.FROM_POI:
                    categoryName = getResources().getString(R.string.pointSelectFromPOI);
                    break;
                default:
                    categoryName = String.valueOf(getItem(position));
                    break;
            }
            holder.label.setText(categoryName);
            return convertView;
        }
        @Override public int getCount() {
            if (categoryIdList != null)
                return categoryIdList.size();
            return 0;
        }
        @Override public Integer getItem(int position) {
            return categoryIdList.get(position);
        }
        // layout
        private class EntryHolder {
            public TextView label;
        }
    }


    public static class EnterAddressDialog extends DialogFragment implements AddressListener, PointWrapperSelectedListener {

        // Store instance variables
        private ChildDialogCloseListener childDialogCloseListener;
        private AccessDatabase accessDatabaseInstance;
        private SettingsManager settingsManagerInstance;
        private InputMethodManager imm;
        private AddressManager addressManagerRequest;
        private int pointPutInto;
        private AutoCompleteTextView editAddress;
        private Switch buttonNearbyCurrentLocation;

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
            settingsManagerInstance = SettingsManager.getInstance(context);
            imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            addressManagerRequest = null;
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            pointPutInto = getArguments().getInt("pointPutInto");

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_enter_address, nullParent);

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

            buttonNearbyCurrentLocation = (Switch) view.findViewById(R.id.buttonNearbyCurrentLocation);
            if (savedInstanceState != null) {
                buttonNearbyCurrentLocation.setChecked(
                        savedInstanceState.getBoolean("nearbyCurrentLocationIsChecked"));
            } else {
                buttonNearbyCurrentLocation.setChecked(true);
            }

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

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putBoolean(
                    "nearbyCurrentLocationIsChecked", buttonNearbyCurrentLocation.isChecked());
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
                        getActivity(),
                        EnterAddressDialog.this,
                        address,
                        buttonNearbyCurrentLocation.isChecked());
                addressManagerRequest.execute();
            }
        }

        @Override public void addressRequestFinished(Context context, int returnCode, ArrayList<PointWrapper> addressPointList) {
            if (returnCode == Constants.RC.OK
                    && addressPointList != null) {
                // add to search history
                settingsManagerInstance.getSearchTermHistory().addSearchTerm(
                        editAddress.getText().toString().trim());
                // single result
                if (addressPointList.size() == 1) {
                    selectAddressPoint(context, addressPointList.get(0));
                } else if (isAdded()) {
                    SelectPointWrapperFromListDialog selectPointWrapperFromListDialog = SelectPointWrapperFromListDialog.newInstance(
                            context.getResources().getString(R.string.selectAddressPointFromListDialogTitle),
                            addressPointList);
                    selectPointWrapperFromListDialog.setTargetFragment(EnterAddressDialog.this, 1);
                    selectPointWrapperFromListDialog.show(
                        getActivity().getSupportFragmentManager(), "SelectPointWrapperFromListDialog");
                }
            } else {
                if (isAdded()) {
                    SimpleMessageDialog.newInstance(
                            ServerUtility.getErrorMessageForReturnCode(context, returnCode))
                        .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
                }
            }
        }

        @Override public void pointWrapperSelectedFromList(PointWrapper pointWrapper) {
            if (isAdded() && pointWrapper != null) {
                selectAddressPoint(getActivity(), pointWrapper);
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

        private void selectAddressPoint(Context context, PointWrapper addressPoint) {
            // put into
            PointUtility.putNewPoint(context, addressPoint, pointPutInto);
            // add to database
            AccessDatabase.getInstance(context).addFavoritePointToProfile(
                    addressPoint, HistoryPointProfile.ID_ADDRESS_POINTS);
            // reload ui
            if (childDialogCloseListener != null) {
                childDialogCloseListener.childDialogClosed();
            }
            dismiss();
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

}
