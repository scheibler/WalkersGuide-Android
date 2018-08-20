package org.walkersguide.android.ui.fragment.main;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONException;
import org.walkersguide.android.R;
import org.walkersguide.android.data.profile.FavoritesProfile;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.FavoritesProfileListener;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.server.FavoritesManager;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.dialog.SearchInFavoritesDialog;
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
import android.widget.AbsListView;
import org.walkersguide.android.listener.SelectFavoritesProfileListener;
import org.walkersguide.android.ui.dialog.SelectFavoritesProfileDialog;
import org.walkersguide.android.ui.dialog.CreateOrEditFavoritesProfileDialog;
import org.walkersguide.android.ui.dialog.SaveCurrentPositionDialog;
import java.util.TreeSet;


public class FavoriteFragment extends Fragment 
    implements FragmentCommunicator, OnMenuItemClickListener, FavoritesProfileListener, SelectFavoritesProfileListener {

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
                    favoritesFragmentSettings.setSelectedPositionInPointList(0);
                    ttsWrapperInstance.speak(
                            accessDatabaseInstance.getNameOfFavoritesProfile(idOfPreviousProfile), true, true);
                    onFragmentEnabled();
                }
            }
        });

        buttonSelectProfile = (Button) view.findViewById(R.id.buttonSelectProfile);
        buttonSelectProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectFavoritesProfileDialog selectFavoritesProfileDialog = SelectFavoritesProfileDialog.newInstance(
                        settingsManagerInstance.getFavoritesFragmentSettings().getSelectedFavoritesProfileId());
                selectFavoritesProfileDialog.setTargetFragment(FavoriteFragment.this, 1);
                selectFavoritesProfileDialog.show(getActivity().getSupportFragmentManager(), "SelectFavoritesProfileDialog");
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
                    favoritesFragmentSettings.setSelectedPositionInPointList(0);
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
                    listViewPOI.setSelection(0);
                }
            }
        });

        listViewPOI = (ListView) view.findViewById(R.id.listViewPOI);
        listViewPOI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                PointProfileObject pointProfileObject = (PointProfileObject) parent.getItemAtPosition(position);
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
        TextView labelListViewPOIEmpty    = (TextView) view.findViewById(R.id.labelListViewPOIEmpty);
        labelListViewPOIEmpty.setVisibility(View.GONE);

        // bottom layout

        buttonRefresh = (Button) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                FavoritesManager favoritesManagerInstance = FavoritesManager.getInstance(getActivity());
                if (favoritesManagerInstance.favoritesProfileRequestInProgress()) {
                    favoritesManagerInstance.cancelFavoritesProfileRequest();
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
                FavoritesFragmentSettings favoritesFragmentSettings= settingsManagerInstance.getFavoritesFragmentSettings();
                favoritesFragmentSettings.setDirectionFilterStatus(isChecked);
                favoritesFragmentSettings.setSelectedPositionInPointList(0);
                onFragmentEnabled();
            }
        });

        Button buttonMore = (Button) view.findViewById(R.id.buttonMore);
        buttonMore.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                PopupMenu popupMore = new PopupMenu(getActivity(), view);
                popupMore.setOnMenuItemClickListener(FavoriteFragment.this);
                popupMore.inflate(R.menu.menu_favorite_fragment_button_more);
                // hide remove option on default profiles
                if (settingsManagerInstance.getFavoritesFragmentSettings().getSelectedFavoritesProfileId() < FavoritesProfile.ID_FIRST_USER_CREATED_PROFILE) {
                    popupMore.getMenu().findItem(R.id.menuItemAddFavoriteFromCurrentPosition).setVisible(false);
                    popupMore.getMenu().findItem(R.id.menuItemRemoveProfile).setVisible(false);
                }
                popupMore.show();
            }
        });
    }

    @Override public boolean onMenuItemClick(MenuItem item) {
        FavoritesFragmentSettings favoritesFragmentSettings= settingsManagerInstance.getFavoritesFragmentSettings();
        switch (item.getItemId()) {
            case R.id.menuItemAddFavoriteFromCurrentPosition:
                TreeSet<Integer> checkedFavoritesProfileIds = new TreeSet<Integer>();
                checkedFavoritesProfileIds.add(favoritesFragmentSettings.getSelectedFavoritesProfileId());
                SaveCurrentPositionDialog.newInstance(checkedFavoritesProfileIds)
                    .show(getActivity().getSupportFragmentManager(), "SaveCurrentPositionDialog");
                return true;
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

        // request favorites
        listViewPOI.setAdapter(null);
        listViewPOI.setOnScrollListener(null);
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
        FavoritesManager.getInstance(getActivity()).invalidateFavoritesProfileRequest((FavoriteFragment) this);
        progressHandler.removeCallbacks(progressUpdater);
        // unregister shake broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(shakeDetectedReceiver);
        // list view
        listViewPOI.setAdapter(null);
        listViewPOI.setOnScrollListener(null);
    }

    @Override public void favoritesProfileSelected(int favoritesProfileId) {
        onFragmentDisabled();
        FavoritesFragmentSettings favoritesFragmentSettings = settingsManagerInstance.getFavoritesFragmentSettings();
        favoritesFragmentSettings.setSelectedFavoritesProfileId(favoritesProfileId);
        favoritesFragmentSettings.setSelectedPositionInPointList(0);
        onFragmentEnabled();
    }

	@Override public void favoritesProfileRequestFinished(int returnCode, String returnMessage, FavoritesProfile favoritesProfile, boolean resetListPosition) {
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
                labelPOIFragmentHeader.setText(
                        String.format(
                            getResources().getString(R.string.labelFavoritesFragmentHeaderSuccessFiltered),
                            getResources().getQuantityString(
                                R.plurals.favorite, listOfFilteredPOI.size(), listOfFilteredPOI.size()),
                            StringUtility.formatProfileSortCriteria(
                                getActivity(), favoritesProfile.getSortCriteria()))
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
                            getResources().getString(R.string.labelFavoritesFragmentHeaderSuccess),
                            getResources().getQuantityString(
                                R.plurals.favorite, listOfAllPOI.size(), listOfAllPOI.size()),
                            StringUtility.formatProfileSortCriteria(
                                getActivity(), favoritesProfile.getSortCriteria()))
                        );
                // fill adapter
                listViewPOI.setAdapter(
                        new ArrayAdapter<PointProfileObject>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            listOfAllPOI)
                        );
            }

            // list position
            if (resetListPosition) {
                listViewPOI.setSelection(0);
            } else {
                listViewPOI.setSelection(favoritesFragmentSettings.getSelectedPositionInPointList());
            }
            listViewPOI.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
                @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (settingsManagerInstance.getFavoritesFragmentSettings().getSelectedPositionInPointList() != firstVisibleItem) {
                        settingsManagerInstance.getFavoritesFragmentSettings().setSelectedPositionInPointList(firstVisibleItem);
                    }
                }
            });

        } else {
            labelPOIFragmentHeader.setText(returnMessage);
        }

        // error message dialog
        if (! (returnCode == Constants.RC.OK || returnCode == Constants.RC.CANCELLED)) {
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
