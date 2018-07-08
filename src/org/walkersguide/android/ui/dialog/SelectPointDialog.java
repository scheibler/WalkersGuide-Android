package org.walkersguide.android.ui.dialog;

import java.util.ArrayList;
import java.util.TreeMap;

import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.poi.FavoritesProfile;
import org.walkersguide.android.data.poi.POIProfile;
import org.walkersguide.android.data.poi.PointProfileObject;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.google.AddressManager;
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

public class SelectPointDialog extends DialogFragment implements ChildDialogCloseListener {

    public interface PARENT_GENERAL {
        public static final int ID = 0;
        // children
        public static final int CHILD_CURRENT_LOCATION = 0;
        public static final int CHILD_ADDRESS = 1;
    }
    public interface PARENT_FAVORITES {
        public static final int ID = 1;
    }
    public interface PARENT_POI {
        public static final int ID = 2;
    }

    private ChildDialogCloseListener childDialogCloseListener;
    private PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;
    private int pointPutInto;
	private ExpandableListView listViewPointSource;

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

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_single_expandable_list_view, nullParent);

        listViewPointSource = (ExpandableListView) view.findViewById(R.id.expandableListView);
        listViewPointSource.setOnChildClickListener(new OnChildClickListener() {
            @Override public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
                if (groupPosition == PARENT_GENERAL.ID
                        && childPosition == PARENT_GENERAL.CHILD_CURRENT_LOCATION) {
                    PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
                    if (currentLocation.equals(PositionManager.getDummyLocation(getActivity()))) {
                        SimpleMessageDialog.newInstance(
                                getResources().getString(R.string.messageError1004))
                            .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
                    } else {
                        PointUtility.putNewPoint(
                                getActivity(), currentLocation, pointPutInto);
                        if (childDialogCloseListener != null) {
                            childDialogCloseListener.childDialogClosed();
                        }
                        dismiss();
                    }
                } else if (groupPosition == PARENT_GENERAL.ID
                        && childPosition == PARENT_GENERAL.CHILD_ADDRESS) {
                    EnterAddressDialog enterAddressDialog = EnterAddressDialog.newInstance(pointPutInto);
                    enterAddressDialog.setTargetFragment(SelectPointDialog.this, 1);
                    enterAddressDialog.show(
                            getActivity().getSupportFragmentManager(), "EnterAddressDialog");
                } else if (groupPosition == PARENT_FAVORITES.ID) {
                    SelectFavoriteDialog selectFavoriteDialog = SelectFavoriteDialog.newInstance(
                            ((PointSourceAdapter) parent.getExpandableListAdapter()).getChild(groupPosition, childPosition),
                            pointPutInto);
                    selectFavoriteDialog.setTargetFragment(SelectPointDialog.this, 1);
                    selectFavoriteDialog.show(
                            getActivity().getSupportFragmentManager(), "SelectFavoriteDialog");
                } else if (groupPosition == PARENT_POI.ID) {
                    SelectPOIDialog selectPOIDialog = SelectPOIDialog.newInstance(
                            ((PointSourceAdapter) parent.getExpandableListAdapter()).getChild(groupPosition, childPosition),
                            pointPutInto);
                    selectPOIDialog.setTargetFragment(SelectPointDialog.this, 1);
                    selectPOIDialog.show(
                            getActivity().getSupportFragmentManager(), "SelectPOIDialog");
                }
                return true;
            }
        });
        TextView labelExpandableListViewEmpty = (TextView) view.findViewById(R.id.labelExpandableListViewEmpty);
        labelExpandableListViewEmpty.setVisibility(View.GONE);

        PointSourceAdapter adapter = new PointSourceAdapter(getActivity());
    	listViewPointSource.setAdapter(adapter);
        for (int i=0; i<adapter.getGroupCount(); i++) {
            listViewPointSource.expandGroup(i);
        }

        String dialogTitle;
        switch (pointPutInto) {
            case Constants.POINT_PUT_INTO.START:
                dialogTitle = getResources().getString(R.string.selectPointDialogNameStart);
                break;
            case Constants.POINT_PUT_INTO.DESTINATION:
                dialogTitle = getResources().getString(R.string.selectPointDialogNameDestination);
                break;
            case Constants.POINT_PUT_INTO.SIMULATION:
                dialogTitle = getResources().getString(R.string.selectPointDialogNameSimulation);
                break;
            default:
                dialogTitle = "";
                break;
        }

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
    }

    @Override public void onStop() {
        super.onStop();
        childDialogCloseListener = null;
    }

    @Override public void childDialogClosed() {
        if (childDialogCloseListener != null) {
            childDialogCloseListener.childDialogClosed();
        }
        dismiss();
    }


    public class PointSourceAdapter extends BaseExpandableListAdapter {

        private Context context;
        private LayoutInflater m_inflater;
        private ArrayList<String> parentList;
        private TreeMap<Integer,String> favoritesProfileMap, poiProfileMap;

        public PointSourceAdapter(Context context) {
            this.context = context;
            this.m_inflater = LayoutInflater.from(context);
            // favorites and poi profiles
            AccessDatabase accessDatabaseInstance = AccessDatabase.getInstance(context);
            this.favoritesProfileMap = accessDatabaseInstance.getFavoritesProfileMap();
            this.poiProfileMap = accessDatabaseInstance.getPOIProfileMap();
            // fill group level of expandable list view
            this.parentList = new ArrayList<String>();
            this.parentList.add(context.getResources().getString(R.string.pointSourceParentGeneral));
            this.parentList.add(
                    String.format(
                        context.getResources().getString(R.string.pointSourceParentFavorites),
                        favoritesProfileMap.size())
                    );
            this.parentList.add(
                    String.format(
                        context.getResources().getString(R.string.pointSourceParentPOI),
                        poiProfileMap.size())
                    );
        }

        @Override public View getGroupView(int groupPosition, boolean isExpanded,
                View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                holder = new EntryHolder();
                convertView = m_inflater.inflate(R.layout.layout_single_text_view, parent, false);
                holder.label = (TextView) convertView.findViewById(R.id.label);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            holder.label.setText(
                    StringUtility.boldAndRed(
                        getGroup(groupPosition)));
            return convertView;
        }

        @Override public String getGroup(int groupPosition) {
            return this.parentList.get(groupPosition);
        }

        @Override public int getGroupCount() {
            return this.parentList.size();
        }

        @Override public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override public View getChildView(int groupPosition, int childPosition,
                boolean isLastChild, View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                holder = new EntryHolder();
                convertView = m_inflater.inflate(R.layout.layout_single_text_view, parent, false);
                holder.label = (TextView) convertView.findViewById(R.id.label);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            switch (groupPosition) {
                case PARENT_GENERAL.ID:
                    switch (childPosition) {
                        case PARENT_GENERAL.CHILD_CURRENT_LOCATION:
                            holder.label.setText(
                                    context.getResources().getString(R.string.pointSourceChildCurrentLocation));
                            break;
                        case PARENT_GENERAL.CHILD_ADDRESS:
                            holder.label.setText(
                                    context.getResources().getString(R.string.pointSourceChildAddress));
                            break;
                        default:
                            break;
                    }
                    break;
                case PARENT_FAVORITES.ID:
                    holder.label.setText(
                            this.favoritesProfileMap.get(
                                getChild(groupPosition, childPosition))
                            );
                    break;
                case PARENT_POI.ID:
                    holder.label.setText(
                            this.poiProfileMap.get(
                                getChild(groupPosition, childPosition))
                            );
                    break;
                default:
                    holder.label.setText("");
                    break;
            }
            return convertView;
        }

        @Override public Integer getChild(int groupPosition, int childPosition) {
            switch (groupPosition) {
                case SelectPointDialog.PARENT_GENERAL.ID:
                    return childPosition;
                case SelectPointDialog.PARENT_FAVORITES.ID:
                    int selectedFavoritesProfileId = -1;
                    int favoritesIndex = 0;
                    for(Integer profileId : this.favoritesProfileMap.keySet()) {
                        if (favoritesIndex == childPosition) {
                            selectedFavoritesProfileId = profileId;
                            break;
                        }
                        favoritesIndex += 1;
                    }
                    return selectedFavoritesProfileId;
                case SelectPointDialog.PARENT_POI.ID:
                    int selectedPOIProfileId = -1;
                    int poiIndex = 0;
                    for(Integer profileId : this.poiProfileMap.keySet()) {
                        if (poiIndex == childPosition) {
                            selectedPOIProfileId = profileId;
                            break;
                        }
                        poiIndex += 1;
                    }
                    return selectedPOIProfileId;
                default:
                    return -1;
            }
        }

        @Override public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override public int getChildrenCount(int groupPosition) {
            switch (groupPosition) {
                case SelectPointDialog.PARENT_GENERAL.ID:
                    return 2;
                case SelectPointDialog.PARENT_FAVORITES.ID:
                    return this.favoritesProfileMap.size();
                case SelectPointDialog.PARENT_POI.ID:
                    return this.poiProfileMap.size();
                default:
                    return 0;
            }
        }

        @Override public boolean hasStableIds() {
            return false;
        }

        @Override public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        private class EntryHolder {
            public TextView label;
        }
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
        private EditText editAddress;

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
            View view = inflater.inflate(R.layout.layout_single_edit_text, nullParent);

            editAddress = (EditText) view.findViewById(R.id.editInput);
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

        @Override public void onStop() {
            super.onStop();
            childDialogCloseListener = null;
        }

        private void tryToGetCoordinatesForAddress() {
            String address = editAddress.getText().toString();
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
            if (returnCode == Constants.ID.OK) {
                // add to addresses profile
                accessDatabaseInstance.addPointToFavoritesProfile(addressPoint, FavoritesProfile.ID_ADDRESS_POINTS);
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
            if (addressManagerRequest != null
                    && addressManagerRequest.getStatus() != AsyncTask.Status.FINISHED) {
                addressManagerRequest.cancel();
            }
        }
    }


    public static class SelectFavoriteDialog extends DialogFragment implements FavoritesProfileListener {

        private AccessDatabase accessDatabaseInstance;
        private ChildDialogCloseListener childDialogCloseListener;
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
            favoritesProfileId = getArguments().getInt("favoritesProfileId");
            pointPutInto = getArguments().getInt("pointPutInto");

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

            labelListViewEmpty = (TextView) view.findViewById(R.id.labelListViewEmpty);
            listViewPOI.setEmptyView(labelListViewEmpty);

            String dialogTitle;
            if (accessDatabaseInstance.getFavoritesProfileMap().containsKey(favoritesProfileId)) {
                dialogTitle = String.format(
                        getResources().getString(R.string.selectPOIDialogName),
                        accessDatabaseInstance.getFavoritesProfileMap().get(favoritesProfileId));
            } else {
                dialogTitle = String.format(
                        getResources().getString(R.string.selectPOIDialogName), "");
            }

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(dialogTitle)
                .setView(view)
                .setNeutralButton(
                        getResources().getString(R.string.dialogUpdate),
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
                // neutral button: update
                Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
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

        @Override public void onStop() {
            super.onStop();
            childDialogCloseListener = null;
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            progressHandler.removeCallbacks(progressUpdater);
        }

        private void requestFavoritesProfile() {
            listPosition = listViewPOI.getFirstVisiblePosition();
            listViewPOI.setAdapter(null);
            labelListViewEmpty.setText(
                    getResources().getString(R.string.messagePleaseWait));
            progressHandler.postDelayed(progressUpdater, 2000);
            FavoritesManager.getInstance(getActivity()).requestFavoritesProfile(
                    (SelectFavoriteDialog) this, favoritesProfileId);
        }

    	@Override public void favoritesProfileRequestFinished(int returnCode, String returnMessage, FavoritesProfile favoritesProfile) {
            progressHandler.removeCallbacks(progressUpdater);
            if (favoritesProfile != null
                    && favoritesProfile.getPointProfileObjectList() != null) {
                listViewPOI.setAdapter(
                        new ArrayAdapter<PointProfileObject>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            favoritesProfile.getPointProfileObjectList())
                        );
                if (listPosition > 0) {
                    listViewPOI.setSelection(listPosition);
                }
            }
            labelListViewEmpty.setText(returnMessage);
        }

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
            poiProfileId = getArguments().getInt("poiProfileId");
            pointPutInto = getArguments().getInt("pointPutInto");

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

            labelListViewEmpty = (TextView) view.findViewById(R.id.labelListViewEmpty);
            listViewPOI.setEmptyView(labelListViewEmpty);

            String dialogTitle;
            if (accessDatabaseInstance.getPOIProfileMap().containsKey(poiProfileId)) {
                dialogTitle = String.format(
                        getResources().getString(R.string.selectPOIDialogName),
                        accessDatabaseInstance.getPOIProfileMap().get(poiProfileId));
            } else {
                dialogTitle = String.format(
                        getResources().getString(R.string.selectPOIDialogName), "");
            }

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(dialogTitle)
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogMore),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setNeutralButton(
                        getResources().getString(R.string.dialogUpdate),
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
                // positive button: more results
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        // request more poi
                        requestPOIProfile(POIManager.ACTION_MORE_RESULTS);
                    }
                });
                // neutral button: more update
                Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        // update poi
                        requestPOIProfile(POIManager.ACTION_UPDATE);
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

        @Override public void onStop() {
            super.onStop();
            childDialogCloseListener = null;
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            progressHandler.removeCallbacks(progressUpdater);
        }

        private void requestPOIProfile(int requestAction) {
            listPosition = listViewPOI.getFirstVisiblePosition();
            listViewPOI.setAdapter(null);
            labelListViewEmpty.setText(
                    getResources().getString(R.string.messagePleaseWait));
            progressHandler.postDelayed(progressUpdater, 2000);
            POIManager.getInstance(getActivity()).requestPOIProfile(
                    (SelectPOIDialog) this, poiProfileId, requestAction);
        }

        @Override public void poiProfileRequestFinished(int returnCode, String returnMessage, POIProfile poiProfile) {
            progressHandler.removeCallbacks(progressUpdater);
            if (poiProfile != null
                    && poiProfile.getPointProfileObjectList() != null) {
                listViewPOI.setAdapter(
                        new ArrayAdapter<PointProfileObject>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            poiProfile.getPointProfileObjectList())
                        );
                if (listPosition > 0) {
                    listViewPOI.setSelection(listPosition);
                }
            }
            labelListViewEmpty.setText(returnMessage);
        }

        private class ProgressUpdater implements Runnable {
            public void run() {
                vibrator.vibrate(50);
                progressHandler.postDelayed(this, 2000);
            }
        }
    }

}
