package org.walkersguide.android.ui.fragment.main;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONException;
import org.walkersguide.android.R;
import org.walkersguide.android.data.profile.FavoritesProfile;
import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.data.profile.POIProfile;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.listener.POIProfileListener;
import org.walkersguide.android.listener.SelectPOIProfileListener;
import org.walkersguide.android.server.POIManager;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.dialog.SelectPOIProfileDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.ui.view.CheckBoxGroupView;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.POIFragmentSettings;
import org.walkersguide.android.util.TTSWrapper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.walkersguide.android.ui.dialog.SearchInPOIDialog;


public class POIFragment extends Fragment 
    implements FragmentCommunicator, OnMenuItemClickListener, POIProfileListener, SelectPOIProfileListener {

	// Store instance variables
    private AccessDatabase accessDatabaseInstance;
	private SettingsManager settingsManagerInstance;
    private TTSWrapper ttsWrapperInstance;

    // query in progress
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private Vibrator vibrator;

    // ui components
    private Button buttonSelectProfile, buttonRefresh;
    private ListView listViewPOI;
    private Switch buttonFilter;
    private TextView labelPOIFragmentHeader, labelMoreResultsFooter, labelListViewPOIEmpty;

	// newInstance constructor for creating fragment with arguments
	public static POIFragment newInstance() {
		POIFragment poiFragmentInstance = new POIFragment();
		return poiFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
			((MainActivity) activity).poiFragmentCommunicator = this;
		}
        // database access
        accessDatabaseInstance = AccessDatabase.getInstance(context);
        // settings manager
		settingsManagerInstance = SettingsManager.getInstance(context);
        // tts
        ttsWrapperInstance = TTSWrapper.getInstance(context);
        // progress updater
        this.progressHandler = new Handler();
        this.progressUpdater = new ProgressUpdater();
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_poi, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        // top layout

        ImageButton buttonPreviousProfile = (ImageButton) view.findViewById(R.id.buttonPreviousProfile);
        buttonPreviousProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                POIFragmentSettings poiFragmentSettings = settingsManagerInstance.getPOIFragmentSettings();
                // get id of previous profile
                int idOfPreviousProfile = -1;
                for(Integer profileId : accessDatabaseInstance.getPOIProfileMap().keySet()) {
                    if (profileId == poiFragmentSettings.getSelectedPOIProfileId()) {
                        break;
                    }
                    idOfPreviousProfile = profileId;
                }
                if (idOfPreviousProfile > -1) {
                    onFragmentDisabled();
                    poiFragmentSettings.setSelectedPOIProfileId(idOfPreviousProfile);
                    poiFragmentSettings.setSelectedPositionInPointList(0);
                    ttsWrapperInstance.speak(
                            accessDatabaseInstance.getNameOfPOIProfile(idOfPreviousProfile), true, true);
                    onFragmentEnabled();
                }
            }
        });

        buttonSelectProfile = (Button) view.findViewById(R.id.buttonSelectProfile);
        buttonSelectProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectPOIProfileDialog selectPOIProfileDialog = SelectPOIProfileDialog.newInstance(
                        settingsManagerInstance.getPOIFragmentSettings().getSelectedPOIProfileId());
                selectPOIProfileDialog.setTargetFragment(POIFragment.this, 1);
                selectPOIProfileDialog.show(getActivity().getSupportFragmentManager(), "SelectPOIProfileDialog");
            }
        });

        ImageButton buttonNextProfile = (ImageButton) view.findViewById(R.id.buttonNextProfile);
        buttonNextProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                POIFragmentSettings poiFragmentSettings = settingsManagerInstance.getPOIFragmentSettings();
                // get id of next profile
                int idOfNextProfile = -1;
                for(Integer profileId :  accessDatabaseInstance.getPOIProfileMap().descendingKeySet()) {
                    if (profileId == poiFragmentSettings.getSelectedPOIProfileId()) {
                        break;
                    }
                    idOfNextProfile = profileId;
                }
                if (idOfNextProfile > -1) {
                    onFragmentDisabled();
                    poiFragmentSettings.setSelectedPOIProfileId(idOfNextProfile);
                    poiFragmentSettings.setSelectedPositionInPointList(0);
                    ttsWrapperInstance.speak(
                            accessDatabaseInstance.getNameOfPOIProfile(idOfNextProfile), true, true);
                    onFragmentEnabled();
                }
            }
        });

        // content layout

        labelPOIFragmentHeader = (TextView) view.findViewById(R.id.labelPOIFragmentHeader);

        ImageButton buttonJumpToTop = (ImageButton) view.findViewById(R.id.buttonJumpToTop);
        buttonJumpToTop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (listViewPOI.getAdapter() != null) {
                    listViewPOI.setSelection(0);
                }
            }
        });

        listViewPOI = (ListView) view.findViewById(R.id.listViewPOI);
        listViewPOI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                PointProfileObject pointProfileObject = (PointProfileObject) parent.getItemAtPosition(position);
                // add to all points profile
                accessDatabaseInstance.addPointToFavoritesProfile(
                        pointProfileObject, FavoritesProfile.ID_ALL_POINTS);
                // open details activity
                Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                try {
                    detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, pointProfileObject.toJson().toString());
                } catch (JSONException e) {
                    detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                }
                startActivity(detailsIntent);
            }
        });
        listViewPOI.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                return true;
            }
        });

        labelListViewPOIEmpty    = (TextView) view.findViewById(R.id.labelListViewPOIEmpty);
        labelListViewPOIEmpty.setText(getResources().getString(R.string.labelMoreResults));
        labelListViewPOIEmpty.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // start poi profile update request
                preparePOIRequest(POIManager.ACTION_MORE_RESULTS);
            }
        });
        listViewPOI.setEmptyView(labelListViewPOIEmpty);

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View footerView = inflater.inflate(R.layout.layout_single_text_view, null, false);
        labelMoreResultsFooter = (TextView) footerView.findViewById(R.id.label);
        labelMoreResultsFooter.setText(getResources().getString(R.string.labelMoreResults));
        labelMoreResultsFooter.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // start poi profile update request
                preparePOIRequest(POIManager.ACTION_MORE_RESULTS);
            }
        });

        // bottom layout

        buttonRefresh = (Button) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                POIManager poiManagerInstance = POIManager.getInstance(getActivity());
                if (poiManagerInstance.requestInProgress()) {
                    poiManagerInstance.cancelRequest();
                } else {
                    onFragmentDisabled();
                    onFragmentEnabled();
                }
            }
        });

        buttonFilter = (Switch) view.findViewById(R.id.buttonFilter);
        buttonFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                onFragmentDisabled();
                POIFragmentSettings poiFragmentSettings = settingsManagerInstance.getPOIFragmentSettings();
                poiFragmentSettings.setDirectionFilterStatus(isChecked);
                poiFragmentSettings.setSelectedPositionInPointList(0);
                onFragmentEnabled();
            }
        });

        Button buttonMore = (Button) view.findViewById(R.id.buttonMore);
        buttonMore.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                PopupMenu popupMore = new PopupMenu(getActivity(), view);
                popupMore.setOnMenuItemClickListener(POIFragment.this);
                popupMore.inflate(R.menu.menu_poi_fragment_button_more);
                popupMore.getMenu().findItem(R.id.menuItemClearProfile).setVisible(false);
                popupMore.show();
            }
        });
    }

    @Override public boolean onMenuItemClick(MenuItem item) {
        POIFragmentSettings poiFragmentSettings = settingsManagerInstance.getPOIFragmentSettings();
        switch (item.getItemId()) {
            case R.id.menuItemSearchInProfile:
                SearchInPOIDialog.newInstance()
                    .show(getActivity().getSupportFragmentManager(), "SearchInPOIDialog");
                return true;
            case R.id.menuItemNewProfile:
                CreateOrEditPOIProfileDialog.newInstance(-1)
                    .show(getActivity().getSupportFragmentManager(), "CreateOrEditPOIProfileDialog");
                return true;
            case R.id.menuItemEditProfile:
                CreateOrEditPOIProfileDialog.newInstance(
                        poiFragmentSettings.getSelectedPOIProfileId())
                    .show(getActivity().getSupportFragmentManager(), "CreateOrEditPOIProfileDialog");
                return true;
            case R.id.menuItemClearProfile:
                return true;
            case R.id.menuItemRemoveProfile:
                RemovePOIProfileDialog.newInstance(
                        poiFragmentSettings.getSelectedPOIProfileId())
                    .show(getActivity().getSupportFragmentManager(), "ClearOrRemovePOIProfileDialog");
                return true;
            default:
                return false;
        }
    }

    @Override public void onFragmentEnabled() {
        POIFragmentSettings poiFragmentSettings = settingsManagerInstance.getPOIFragmentSettings();
        // selected poi profile name
        if (accessDatabaseInstance.getPOIProfileMap().containsKey(poiFragmentSettings.getSelectedPOIProfileId())) {
            buttonSelectProfile.setText(
                    accessDatabaseInstance.getPOIProfileMap().get(poiFragmentSettings.getSelectedPOIProfileId()));
        } else {
            buttonSelectProfile.setText(
                    getResources().getString(R.string.emptyProfileList));
        }
        // set direction filter status
        buttonFilter.setChecked(
                poiFragmentSettings.filterPointListByDirection());
        // listen for device shakes
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                shakeDetectedReceiver,
                new IntentFilter(Constants.ACTION_SHAKE_DETECTED));
        // request poi
        preparePOIRequest(POIManager.ACTION_UPDATE);
    }

	@Override public void onFragmentDisabled() {
        POIManager.getInstance(getActivity()).invalidateRequest((POIFragment) this);
        progressHandler.removeCallbacks(progressUpdater);
        // unregister shake broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(shakeDetectedReceiver);
        // list view
        listViewPOI.setAdapter(null);
        listViewPOI.setOnScrollListener(null);
    }

    private void preparePOIRequest(int requestAction) {
        // list view
        listViewPOI.setAdapter(null);
        listViewPOI.setOnScrollListener(null);
        if (listViewPOI.getFooterViewsCount() > 0) {
            listViewPOI.removeFooterView(labelMoreResultsFooter);
        }
        labelListViewPOIEmpty.setVisibility(View.GONE);
        // header and refresh button
        labelPOIFragmentHeader.setText(
                getResources().getString(R.string.messagePleaseWait));
        buttonRefresh.setText(
                getResources().getString(R.string.buttonCancel));
        // start poi profile update request
        progressHandler.postDelayed(progressUpdater, 2000);
        POIManager.getInstance(getActivity()).requestPOIProfile(
                (POIFragment) this,
                settingsManagerInstance.getPOIFragmentSettings().getSelectedPOIProfileId(),
                requestAction);
    }

    @Override public void poiProfileSelected(int poiProfileId) {
        onFragmentDisabled();
        POIFragmentSettings poiFragmentSettings = settingsManagerInstance.getPOIFragmentSettings();
        poiFragmentSettings.setSelectedPOIProfileId(poiProfileId);
        poiFragmentSettings.setSelectedPositionInPointList(0);
        onFragmentEnabled();
    }

	@Override public void poiProfileRequestFinished(int returnCode, String returnMessage, POIProfile poiProfile, boolean resetListPosition) {
        POIFragmentSettings poiFragmentSettings = settingsManagerInstance.getPOIFragmentSettings();
        buttonRefresh.setText(
                getResources().getString(R.string.buttonRefresh));
        progressHandler.removeCallbacks(progressUpdater);

        if (poiProfile != null
                && poiProfile.getPointProfileObjectList() != null) {
            ArrayList<PointProfileObject> listOfAllPOI = poiProfile.getPointProfileObjectList();

            // fill listview
            if (poiFragmentSettings.filterPointListByDirection()) {
                // only include, what's in front of the user
                ArrayList<PointProfileObject> listOfFilteredPOI = new ArrayList<PointProfileObject>();
                for (PointProfileObject pointProfileObject : listOfAllPOI) {
                    int bearing = pointProfileObject.bearingFromCenter();
                    if (bearing < 60 || bearing > 300) {
                        listOfFilteredPOI.add(pointProfileObject);
                    }
                }
                // header label
                labelPOIFragmentHeader.setText(
                        String.format(
                            getResources().getString(R.string.labelPOIFragmentHeaderSuccessFiltered),
                            listOfFilteredPOI.size(),
                            poiProfile.getLookupRadius())
                        );
                // fill adapter
                listViewPOI.setAdapter(
                        new ArrayAdapter<PointProfileObject>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            listOfFilteredPOI)
                        );

            } else {
                // take all poi
                labelPOIFragmentHeader.setText(
                        String.format(
                            getResources().getString(R.string.labelPOIFragmentHeaderSuccess),
                            listOfAllPOI.size(),
                            poiProfile.getLookupRadius())
                        );
                listViewPOI.setAdapter(
                        new ArrayAdapter<PointProfileObject>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            listOfAllPOI)
                        );
            }

            // more results
            if (listViewPOI.getAdapter().getCount() == 0) {
                if (poiProfile.getRadius() < poiProfile.getMaximalRadius()
                        && poiProfile.getNumberOfResults() < poiProfile.getMaximalNumberOfResults()) {
                    labelListViewPOIEmpty.setVisibility(View.VISIBLE);
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
                listViewPOI.setSelection(poiFragmentSettings.getSelectedPositionInPointList());
            }
            listViewPOI.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
                @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (settingsManagerInstance.getPOIFragmentSettings().getSelectedPositionInPointList() != firstVisibleItem) {
                        settingsManagerInstance.getPOIFragmentSettings().setSelectedPositionInPointList(firstVisibleItem);
                    }
                }
            });

        } else {
            labelPOIFragmentHeader.setText(returnMessage);
        }

        // error message dialog
        if (! (returnCode == Constants.ID.OK || returnCode == Constants.ID.CANCELLED)) {
            SimpleMessageDialog.newInstance(returnMessage)
                .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
        }
    }


    private BroadcastReceiver shakeDetectedReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            onFragmentDisabled();
            vibrator.vibrate(250);
            onFragmentEnabled();
        }
    };


    private class ProgressUpdater implements Runnable {
        public void run() {
            vibrator.vibrate(50);
            progressHandler.postDelayed(this, 2000);
        }
    }


    public static class CreateOrEditPOIProfileDialog extends DialogFragment {

        // Store instance variables
        private AccessDatabase accessDatabaseInstance;
        private InputMethodManager imm;
        private SettingsManager settingsManagerInstance;
        private int poiProfileId;

        // ui components
        private EditText editProfileName;
        private CheckBoxGroupView checkBoxGroupPOICategories;

        public static CreateOrEditPOIProfileDialog newInstance(int poiProfileId) {
            CreateOrEditPOIProfileDialog createOrEditPOIProfileDialogInstance = new CreateOrEditPOIProfileDialog();
            Bundle args = new Bundle();
            args.putInt("poiProfileId", poiProfileId);
            createOrEditPOIProfileDialogInstance.setArguments(args);
            return createOrEditPOIProfileDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            settingsManagerInstance = SettingsManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            poiProfileId = getArguments().getInt("poiProfileId", -1);
            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_create_or_edit_poi_profile, nullParent);
            // dialog title
            String dialogTitle;
            if (accessDatabaseInstance.getPOIProfileMap().containsKey(poiProfileId)) {
                dialogTitle = getResources().getString(R.string.editProfileDialogTitle);
            } else {
                dialogTitle = getResources().getString(R.string.newProfileDialogTitle);
            }

            editProfileName = (EditText) view.findViewById(R.id.editInput);
            editProfileName.setText(accessDatabaseInstance.getNameOfPOIProfile(poiProfileId));
            editProfileName.setInputType(InputType.TYPE_CLASS_TEXT);
            editProfileName.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editProfileName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        createOrEditPOIProfile();
                        return true;
                    }
                    return false;
                }
            });

            ImageButton buttonDelete = (ImageButton) view.findViewById(R.id.buttonDelete);
            buttonDelete.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    // clear edit text
                    editProfileName.setText("");
                    // show keyboard
                    imm.showSoftInput(editProfileName, InputMethodManager.SHOW_IMPLICIT);
                }
            });

            checkBoxGroupPOICategories = (CheckBoxGroupView) view.findViewById(R.id.checkBoxGroupPOICategories);
            ArrayList<POICategory> categoryListOfPOIProfile = accessDatabaseInstance.getCategoryListOfPOIProfile(poiProfileId);
            for (POICategory category : accessDatabaseInstance.getPOICategoryList()) {
                CheckBox checkBox = new CheckBox(getActivity());
                checkBox.setId(category.getId());
                checkBox.setLayoutParams(
                        new LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT)
                        );
                checkBox.setText(category.getName());
                checkBox.setChecked(
                        categoryListOfPOIProfile.contains(category));
                checkBoxGroupPOICategories.put(checkBox);
            }

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(dialogTitle)
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogOK),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                        )
                .setNeutralButton(
                        getResources().getString(R.string.dialogClear),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                        )
                .setNegativeButton(
                        getResources().getString(R.string.dialogCancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                        )
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
                        createOrEditPOIProfile();
                    }
                });
                // neutral button
                Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        checkBoxGroupPOICategories.uncheckAll();
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

        private void createOrEditPOIProfile() {
            // profile name
            String profileName = editProfileName.getText().toString();
            if (profileName.equals("")) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageProfileNameMissing),
                        Toast.LENGTH_LONG).show();
                return;
            } else {
                for(Map.Entry<Integer,String> profile : accessDatabaseInstance.getPOIProfileMap().entrySet()) {
                    if (poiProfileId != profile.getKey()
                            && profileName.equals(profile.getValue())) {
                        Toast.makeText(
                                getActivity(),
                                String.format(getResources().getString(R.string.messageProfileExists), profileName),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }

            ArrayList<POICategory> poiCategoryList = new ArrayList<POICategory>();
            for (CheckBox checkBox : checkBoxGroupPOICategories.getCheckedCheckBoxList()) {
                POICategory category = accessDatabaseInstance.getPOICategory(checkBox.getId());
                if (category != null) {
                    poiCategoryList.add(category);
                }
            }
            if (poiCategoryList.isEmpty()) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageError1033),
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (! accessDatabaseInstance.getPOIProfileMap().containsKey(poiProfileId)) {
                // create new profile
                int newProfileId = accessDatabaseInstance.addPOIProfile(profileName, poiCategoryList);
                if (newProfileId > -1) {
                    settingsManagerInstance.getPOIFragmentSettings().setSelectedPOIProfileId(newProfileId);
                } else {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(R.string.messageCouldNotCreateProfile),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            } else {
                // edit existing profile
                boolean updateSuccessful = accessDatabaseInstance.updateNameAndCategoryListOfPOIProfile(
                        poiProfileId, profileName, poiCategoryList);
                if (! updateSuccessful) {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(R.string.messageCouldNotEditProfile),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
            Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            dismiss();
        }
    }


    public static class RemovePOIProfileDialog extends DialogFragment {

        // Store instance variables
        private AccessDatabase accessDatabaseInstance;
        private SettingsManager settingsManagerInstance;
        private int poiProfileId;

        public static RemovePOIProfileDialog newInstance(int poiProfileId) {
            RemovePOIProfileDialog removePOIProfileDialogInstance = new RemovePOIProfileDialog();
            Bundle args = new Bundle();
            args.putInt("poiProfileId", poiProfileId);
            removePOIProfileDialogInstance.setArguments(args);
            return removePOIProfileDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            poiProfileId = getArguments().getInt("poiProfileId", -1);
            return new AlertDialog.Builder(getActivity())
                .setMessage(
                        String.format(
                            getResources().getString(R.string.removeProfileDialogTitle),
                            accessDatabaseInstance.getNameOfPOIProfile(poiProfileId)))
                .setPositiveButton(
                        getResources().getString(R.string.dialogYes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                accessDatabaseInstance.removePOIProfile(poiProfileId);
                                settingsManagerInstance.getPOIFragmentSettings().setSelectedPOIProfileId(-1);
                                Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                                dismiss();
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogNo),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss();
                            }
                        })
                .create();
        }
    }

}
