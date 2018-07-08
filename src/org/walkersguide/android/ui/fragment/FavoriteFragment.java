package org.walkersguide.android.ui.fragment;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONException;
import org.walkersguide.android.R;
import org.walkersguide.android.data.poi.FavoritesProfile;
import org.walkersguide.android.data.poi.PointProfileObject;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.FavoritesProfileListener;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.server.FavoritesManager;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.FavoritesFragmentSettings;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class FavoriteFragment extends Fragment 
    implements FragmentCommunicator, OnMenuItemClickListener, FavoritesProfileListener {

	// Store instance variables
    private AccessDatabase accessDatabaseInstance;
	private SettingsManager settingsManagerInstance;
    private TTSWrapper ttsWrapperInstance;
    private Vibrator vibrator;

    // query in progress
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;

    // ui components
    private Button buttonSelectProfile, buttonRefresh;
    private ListView listViewPOI;
    private Switch buttonFilter;
    private TextView labelPOIFragmentHeader;

	// newInstance constructor for creating fragment with arguments
 	public static FavoriteFragment newInstance() {
 		FavoriteFragment favoriteFragmentInstance = new FavoriteFragment();
 		return favoriteFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
 			((MainActivity) activity).favoriteFragmentCommunicator = this;
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
                FavoritesFragmentSettings favoritesFragmentSettings= settingsManagerInstance.getFavoritesFragmentSettings();
                // get id of previous profile
                int idOfPreviousProfile = -1;
                for(Integer profileId : accessDatabaseInstance.getFavoritesProfileMap().keySet()) {
                    if (profileId == favoritesFragmentSettings.getSelectedFavoritesProfileId()) {
                        break;
                    }
                    idOfPreviousProfile = profileId;
                }
                if (idOfPreviousProfile > -1) {
                    onFragmentDisabled();
                    favoritesFragmentSettings.setSelectedFavoritesProfileId(idOfPreviousProfile);
                    ttsWrapperInstance.speak(
                            accessDatabaseInstance.getNameOfFavoritesProfile(idOfPreviousProfile), true, true);
                    onFragmentEnabled();
                }
            }
        });

        buttonSelectProfile = (Button) view.findViewById(R.id.buttonSelectProfile);
        buttonSelectProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectFavoritesProfileDialog.newInstance(
                        settingsManagerInstance.getFavoritesFragmentSettings().getSelectedFavoritesProfileId())
                    .show(getActivity().getSupportFragmentManager(), "SelectProfileDialog");
            }
        });

        ImageButton buttonNextProfile = (ImageButton) view.findViewById(R.id.buttonNextProfile);
        buttonNextProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                FavoritesFragmentSettings favoritesFragmentSettings= settingsManagerInstance.getFavoritesFragmentSettings();
                int idOfNextProfile = -1;
                for(Integer profileId :  accessDatabaseInstance.getFavoritesProfileMap().descendingKeySet()) {
                    if (profileId == favoritesFragmentSettings.getSelectedFavoritesProfileId()) {
                        break;
                    }
                    idOfNextProfile = profileId;
                }
                if (idOfNextProfile > -1) {
                    onFragmentDisabled();
                    favoritesFragmentSettings.setSelectedFavoritesProfileId(idOfNextProfile);
                    ttsWrapperInstance.speak(
                            accessDatabaseInstance.getNameOfFavoritesProfile(idOfNextProfile), true, true);
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
                    onFragmentDisabled();
                    settingsManagerInstance.getFavoritesFragmentSettings().setSelectedPositionInPointList(0);
                    onFragmentEnabled();
                }
            }
        });

        listViewPOI = (ListView) view.findViewById(R.id.listViewPOI);
        listViewPOI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                PointProfileObject pointProfileObject = (PointProfileObject) parent.getItemAtPosition(position);
                Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                try {
                    detailsIntent.putExtra("jsonPointSerialized", pointProfileObject.toJson().toString());
                } catch (JSONException e) {
                    detailsIntent.putExtra("jsonPointSerialized", "");
                }
                startActivity(detailsIntent);
            }
        });
        listViewPOI.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                return true;
            }
        });
        TextView labelListViewPOIEmpty    = (TextView) view.findViewById(R.id.labelListViewPOIEmpty);
        labelListViewPOIEmpty.setVisibility(View.GONE);

        // bottom layout

        buttonRefresh = (Button) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                FavoritesManager favoritesManagerInstance = FavoritesManager.getInstance(getActivity());
                if (favoritesManagerInstance.requestInProgress()) {
                    favoritesManagerInstance.cancelRequest();
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
                settingsManagerInstance.getFavoritesFragmentSettings().setDirectionFilterStatus(isChecked);
                onFragmentEnabled();
            }
        });

        Button buttonMore = (Button) view.findViewById(R.id.buttonMore);
        buttonMore.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                PopupMenu popupMore = new PopupMenu(getActivity(), view);
                popupMore.setOnMenuItemClickListener(FavoriteFragment.this);
                popupMore.inflate(R.menu.menu_poi_fragment_button_more);
                // hide remove option on default profiles
                if (settingsManagerInstance.getFavoritesFragmentSettings().getSelectedFavoritesProfileId() < FavoritesProfile.ID_FIRST_USER_CREATED_PROFILE) {
                    popupMore.getMenu().findItem(R.id.menuItemRemoveProfile).setVisible(false);
                }
                popupMore.show();
            }
        });
    }

    @Override public boolean onMenuItemClick(MenuItem item) {
        FavoritesFragmentSettings favoritesFragmentSettings= settingsManagerInstance.getFavoritesFragmentSettings();
        switch (item.getItemId()) {
            case R.id.menuItemNewProfile:
                CreateOrEditFavoritesProfileDialog.newInstance(-1)
                    .show(getActivity().getSupportFragmentManager(), "CreateOrEditFavoritesProfileDialog");
                return true;
            case R.id.menuItemEditProfile:
                CreateOrEditFavoritesProfileDialog.newInstance(
                        favoritesFragmentSettings.getSelectedFavoritesProfileId())
                    .show(getActivity().getSupportFragmentManager(), "CreateOrEditFavoritesProfileDialog");
                return true;
            case R.id.menuItemClearProfile:
                ClearOrRemovePOIProfileDialog.newInstance(
                        favoritesFragmentSettings.getSelectedFavoritesProfileId(), false)
                    .show(getActivity().getSupportFragmentManager(), "ClearOrRemovePOIProfileDialog");
                return true;
            case R.id.menuItemRemoveProfile:
                ClearOrRemovePOIProfileDialog.newInstance(
                        favoritesFragmentSettings.getSelectedFavoritesProfileId(), true)
                    .show(getActivity().getSupportFragmentManager(), "ClearOrRemovePOIProfileDialog");
                return true;
            default:
                return false;
        }
    }

    @Override public void onFragmentEnabled() {
        FavoritesFragmentSettings favoritesFragmentSettings= settingsManagerInstance.getFavoritesFragmentSettings();

        // selected poi profile name
        if (accessDatabaseInstance.getFavoritesProfileMap().containsKey(favoritesFragmentSettings.getSelectedFavoritesProfileId())) {
            buttonSelectProfile.setText(
                    accessDatabaseInstance.getFavoritesProfileMap().get(favoritesFragmentSettings.getSelectedFavoritesProfileId()));
        } else {
            buttonSelectProfile.setText(
                    getResources().getString(R.string.emptyProfileList));
        }
        // set direction filter status
        buttonFilter.setChecked(
                favoritesFragmentSettings.filterPointListByDirection());
        // listen for device shakes
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                shakeDetectedReceiver,
                new IntentFilter(Constants.ACTION_SHAKE_DETECTED));

        // request poi
        listViewPOI.setAdapter(null);
        labelPOIFragmentHeader.setText(
                getResources().getString(R.string.messagePleaseWait));
        buttonRefresh.setText(
                getResources().getString(R.string.buttonCancel));
        progressHandler.postDelayed(progressUpdater, 2000);
        FavoritesManager.getInstance(getActivity()).requestFavoritesProfile(
                (FavoriteFragment) this,
                favoritesFragmentSettings.getSelectedFavoritesProfileId());
    }

	@Override public void onFragmentDisabled() {
        FavoritesManager.getInstance(getActivity()).invalidateRequest();
        progressHandler.removeCallbacks(progressUpdater);
        // unregister shake broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(shakeDetectedReceiver);
        // save current list position
        if (listViewPOI.getAdapter() != null) {
            settingsManagerInstance.getFavoritesFragmentSettings().setSelectedPositionInPointList(
                    listViewPOI.getFirstVisiblePosition());
        }
    }

	@Override public void favoritesProfileRequestFinished(int returnCode, String returnMessage, FavoritesProfile favoritesProfile) {
        FavoritesFragmentSettings favoritesFragmentSettings= settingsManagerInstance.getFavoritesFragmentSettings();
        buttonRefresh.setText(
                getResources().getString(R.string.buttonRefresh));
        progressHandler.removeCallbacks(progressUpdater);

        if (favoritesProfile != null
                && favoritesProfile.getPointProfileObjectList() != null) {
            ArrayList<PointProfileObject> listOfAllPOI = favoritesProfile.getPointProfileObjectList();

            // fill listview
            if (favoritesFragmentSettings.filterPointListByDirection()) {
                // only include, what's in front of the user
                ArrayList<PointProfileObject> listOfFilteredPOI = new ArrayList<PointProfileObject>();
                for (PointProfileObject pointProfileObject : listOfAllPOI) {
                    int bearing = pointProfileObject.bearingFromCenter();
                    if (bearing < 60 || bearing > 300) {
                        listOfFilteredPOI.add(pointProfileObject);
                    }
                }
                // header label
                if (listOfFilteredPOI.size() == 1) {
                    labelPOIFragmentHeader.setText(
                            String.format(
                                getResources().getString(R.string.labelFavoritesFragmentHeaderSuccessSingularFiltered),
                                StringUtility.formatProfileSortCriteria(
                                        getActivity(),
                                        favoritesProfile.getSortCriteria()))
                            );
                } else {
                    labelPOIFragmentHeader.setText(
                            String.format(
                                getResources().getString(R.string.labelFavoritesFragmentHeaderSuccessPluralFiltered),
                                listOfFilteredPOI.size(),
                                StringUtility.formatProfileSortCriteria(
                                    getActivity(),
                                    favoritesProfile.getSortCriteria()))
                            );
                }
                // fill adapter
                listViewPOI.setAdapter(
                        new ArrayAdapter<PointProfileObject>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            listOfFilteredPOI)
                        );

            } else {
                // take all poi
                if (listOfAllPOI.size() == 1) {
                    labelPOIFragmentHeader.setText(
                            String.format(
                                getResources().getString(R.string.labelFavoritesFragmentHeaderSuccessSingular),
                                StringUtility.formatProfileSortCriteria(
                                    getActivity(),
                                    favoritesProfile.getSortCriteria()))
                            );
                } else {
                    labelPOIFragmentHeader.setText(
                            String.format(
                                getResources().getString(R.string.labelFavoritesFragmentHeaderSuccessPlural),
                                listOfAllPOI.size(),
                                StringUtility.formatProfileSortCriteria(
                                    getActivity(),
                                    favoritesProfile.getSortCriteria()))
                            );
                }
                // fill adapter
                listViewPOI.setAdapter(
                        new ArrayAdapter<PointProfileObject>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            listOfAllPOI)
                        );
            }

            // restore list position
            if (favoritesFragmentSettings.getSelectedPositionInPointList() > 0) {
                listViewPOI.setSelection(
                        favoritesFragmentSettings.getSelectedPositionInPointList());
            }

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
            System.out.println("xxx poiFragment shake detected");
            onFragmentDisabled();
            onFragmentEnabled();
        }
    };


    private class ProgressUpdater implements Runnable {
        public void run() {
            vibrator.vibrate(50);
            progressHandler.postDelayed(this, 2000);
        }
    }


    public static class SelectFavoritesProfileDialog extends DialogFragment {

        // Store instance variables
        private AccessDatabase accessDatabaseInstance;
        private SettingsManager settingsManagerInstance;

        public static SelectFavoritesProfileDialog newInstance(int favoritesProfileId) {
            SelectFavoritesProfileDialog selectFavoritesProfileDialogInstance = new SelectFavoritesProfileDialog();
            Bundle args = new Bundle();
            args.putInt("favoritesProfileId", favoritesProfileId);
            selectFavoritesProfileDialogInstance.setArguments(args);
            return selectFavoritesProfileDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            TreeMap<Integer,String> favoritesProfileMap = accessDatabaseInstance.getFavoritesProfileMap();
            String[] formattedFavoritesProfileNameArray = new String[favoritesProfileMap.size()];
            int indexOfSelectedFavoritesProfile = -1;
            int index = 0;
            for (Map.Entry<Integer,String> profile : favoritesProfileMap.entrySet()) {
                formattedFavoritesProfileNameArray[index] = profile.getValue();
                if (profile.getKey() == getArguments().getInt("favoritesProfileId", -1)) {
                    indexOfSelectedFavoritesProfile = index;
                }
                index += 1;
            }

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectProfileDialogTitle))
                .setSingleChoiceItems(
                        formattedFavoritesProfileNameArray,
                        indexOfSelectedFavoritesProfile,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                int selectedProfileId = -1;
                                int index = 0;
                                for(Integer profileId : accessDatabaseInstance.getFavoritesProfileMap().keySet()) {
                                    if (index == which) {
                                        selectedProfileId = profileId;
                                        break;
                                    }
                                    index += 1;
                                }
                                if (selectedProfileId > -1) {
                                    settingsManagerInstance.getFavoritesFragmentSettings().setSelectedFavoritesProfileId(selectedProfileId);
                                    Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                                }
                                dismiss();
                            }
                        }
                        )
                .setNegativeButton(
                        getResources().getString(R.string.dialogCancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss();
                            }
                        }
                        )
                .create();
        }
    }


    public static class CreateOrEditFavoritesProfileDialog extends DialogFragment {

        // Store instance variables
        private AccessDatabase accessDatabaseInstance;
        private InputMethodManager imm;
        private SettingsManager settingsManagerInstance;
        private int favoritesProfileId;

        // ui components
        private EditText editProfileName;
        private RadioGroup radioGroupFavoritesProfileSortCriteria;

        public static CreateOrEditFavoritesProfileDialog newInstance(int favoritesProfileId) {
            CreateOrEditFavoritesProfileDialog createOrEditFavoritesProfileDialogInstance = new CreateOrEditFavoritesProfileDialog();
            Bundle args = new Bundle();
            args.putInt("favoritesProfileId", favoritesProfileId);
            createOrEditFavoritesProfileDialogInstance.setArguments(args);
            return createOrEditFavoritesProfileDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            settingsManagerInstance = SettingsManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            favoritesProfileId = getArguments().getInt("favoritesProfileId", -1);
            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_create_or_edit_favorites_profile, nullParent);
            // dialog title
            String dialogTitle;
            if (accessDatabaseInstance.getFavoritesProfileMap().containsKey(favoritesProfileId)) {
                dialogTitle = getResources().getString(R.string.editProfileDialogTitle);
            } else {
                dialogTitle = getResources().getString(R.string.newProfileDialogTitle);
            }

            editProfileName = (EditText) view.findViewById(R.id.editInput);
            editProfileName.setText(accessDatabaseInstance.getNameOfFavoritesProfile(favoritesProfileId));
            editProfileName.setInputType(InputType.TYPE_CLASS_TEXT);
            editProfileName.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editProfileName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        createOrEditFavoritesProfile();
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

            radioGroupFavoritesProfileSortCriteria = (RadioGroup) view.findViewById(R.id.radioGroupFavoritesProfileSortCriteria);
            switch (accessDatabaseInstance.getSortCriteriaOfFavoritesProfile(favoritesProfileId)) {
                case Constants.SORT_CRITERIA.NAME_ASC:
                    radioGroupFavoritesProfileSortCriteria.check(R.id.radioButtonSortNameAsc);
                    break;
                case Constants.SORT_CRITERIA.NAME_DESC:
                    radioGroupFavoritesProfileSortCriteria.check(R.id.radioButtonSortNameDesc);
                    break;
                case Constants.SORT_CRITERIA.DISTANCE_ASC:
                    radioGroupFavoritesProfileSortCriteria.check(R.id.radioButtonSortDistanceAsc);
                    break;
                case Constants.SORT_CRITERIA.DISTANCE_DESC:
                    radioGroupFavoritesProfileSortCriteria.check(R.id.radioButtonSortDistanceDesc);
                    break;
                case Constants.SORT_CRITERIA.ORDER_ASC:
                    radioGroupFavoritesProfileSortCriteria.check(R.id.radioButtonSortOrderAsc);
                    break;
                case Constants.SORT_CRITERIA.ORDER_DESC:
                    radioGroupFavoritesProfileSortCriteria.check(R.id.radioButtonSortOrderDesc);
                    break;
                default:
                    radioGroupFavoritesProfileSortCriteria.check(R.id.radioButtonSortNameAsc);
                    break;
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
                        createOrEditFavoritesProfile();
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

        private void createOrEditFavoritesProfile() {
            // profile name
            String profileName = editProfileName.getText().toString();
            if (profileName.equals("")) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageProfileNameMissing),
                        Toast.LENGTH_LONG).show();
                return;
            } else {
                for(Map.Entry<Integer,String> profile : accessDatabaseInstance.getFavoritesProfileMap().entrySet()) {
                    if (favoritesProfileId != profile.getKey()
                            && profileName.equals(profile.getValue())) {
                        Toast.makeText(
                                getActivity(),
                                String.format(getResources().getString(R.string.messageProfileExists), profileName),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }

            // profile sort criteria
            int sortCriteria;
            switch (radioGroupFavoritesProfileSortCriteria.getCheckedRadioButtonId()) {
                case R.id.radioButtonSortNameAsc:
                    sortCriteria = Constants.SORT_CRITERIA.NAME_ASC;
                    break;
                case R.id.radioButtonSortNameDesc:
                    sortCriteria = Constants.SORT_CRITERIA.NAME_DESC;
                    break;
                case R.id.radioButtonSortDistanceAsc:
                    sortCriteria = Constants.SORT_CRITERIA.DISTANCE_ASC;
                    break;
                case R.id.radioButtonSortDistanceDesc:
                    sortCriteria = Constants.SORT_CRITERIA.DISTANCE_DESC;
                    break;
                case R.id.radioButtonSortOrderAsc:
                    sortCriteria = Constants.SORT_CRITERIA.ORDER_ASC;
                    break;
                case R.id.radioButtonSortOrderDesc:
                    sortCriteria = Constants.SORT_CRITERIA.ORDER_DESC;
                    break;
                default:
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(R.string.messageNoSortCriteria),
                            Toast.LENGTH_LONG).show();
                    return;
            }

            if (! accessDatabaseInstance.getFavoritesProfileMap().containsKey(favoritesProfileId)) {
                // create new profile
                int newProfileId = accessDatabaseInstance.addFavoritesProfile(profileName, sortCriteria);
                if (newProfileId > -1) {
                    settingsManagerInstance.getFavoritesFragmentSettings().setSelectedFavoritesProfileId(newProfileId);
                } else {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(R.string.messageCouldNotCreateProfile),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            } else {
                // edit existing profile
                boolean updateSuccessful = accessDatabaseInstance.updateNameAndSortCriteriaOfFavoritesProfile(
                        favoritesProfileId, profileName, sortCriteria);
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


    public static class ClearOrRemovePOIProfileDialog extends DialogFragment {

        // Store instance variables
        private AccessDatabase accessDatabaseInstance;
        private SettingsManager settingsManagerInstance;
        private int favoritesProfileId;
        private boolean remove;

        public static ClearOrRemovePOIProfileDialog newInstance(int favoritesProfileId, boolean remove) {
            ClearOrRemovePOIProfileDialog clearOrRemovePOIProfileDialogInstance = new ClearOrRemovePOIProfileDialog();
            Bundle args = new Bundle();
            args.putInt("favoritesProfileId", favoritesProfileId);
            args.putBoolean("remove", remove);
            clearOrRemovePOIProfileDialogInstance.setArguments(args);
            return clearOrRemovePOIProfileDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            favoritesProfileId = getArguments().getInt("favoritesProfileId", -1);
            remove = getArguments().getBoolean("remove", false);

            String dialogMessage;
            if (remove) {
                dialogMessage = String.format(
                        getResources().getString(R.string.removeProfileDialogTitle),
                        accessDatabaseInstance.getNameOfFavoritesProfile(favoritesProfileId));
            } else {
                dialogMessage = String.format(
                        getResources().getString(R.string.clearProfileDialogTitle),
                        accessDatabaseInstance.getNameOfFavoritesProfile(favoritesProfileId));
            }

            return new AlertDialog.Builder(getActivity())
                .setMessage(dialogMessage)
                .setPositiveButton(
                        getResources().getString(R.string.dialogYes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (remove) {
                                    accessDatabaseInstance.removeFavoritesProfile(favoritesProfileId);
                                    settingsManagerInstance.getFavoritesFragmentSettings().setSelectedFavoritesProfileId(-1);
                                } else {
                                    accessDatabaseInstance.clearFavoritesProfile(favoritesProfileId);
                                }
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
