package org.walkersguide.android.ui.fragment.main;

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

import android.text.Editable;
import android.text.TextUtils;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import org.json.JSONException;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.data.profile.HistoryPointProfile;
import org.walkersguide.android.data.profile.POIProfile;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.listener.POIProfileListener;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.POIManager;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.adapter.POIProfileAdapter;
import org.walkersguide.android.ui.adapter.POIProfilePointAdapter;
import org.walkersguide.android.ui.dialog.CreateOrEditPOIProfileDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.POISettings;
import org.walkersguide.android.util.TextChangedListener;
import org.walkersguide.android.data.poi.POICategory;


public class POIFragment extends Fragment implements FragmentCommunicator, POIProfileListener {

	// Store instance variables
    private AccessDatabase accessDatabaseInstance;
    private InputMethodManager imm;
    private POIManager poiManagerInstance;
	private SettingsManager settingsManagerInstance;
    private String searchTerm;
    private int listPosition;

    // query in progress
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private Vibrator vibrator;

    // ui components
    private AutoCompleteTextView editSearch;
    private Button buttonSelectProfile;
    private ImageButton buttonRefresh, buttonClearSearch;
    private ListView listViewPOI;
    private TextView labelHeading, labelEmptyListView, labelMoreResultsFooter;

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
        // input manager
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        // poi manager
        poiManagerInstance = POIManager.getInstance(context);
        // settings manager
		settingsManagerInstance = SettingsManager.getInstance(context);
        // progress updater
        this.progressHandler = new Handler();
        this.progressUpdater = new ProgressUpdater();
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_poi_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // check or uncheck filter result menu item
        MenuItem menuItemFilterResult = menu.findItem(R.id.menuItemFilterResult);
        menuItemFilterResult.setChecked(
                settingsManagerInstance.getPOISettings().filterPointListByDirection());
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemJumpToTop:
                if (listViewPOI.getAdapter() != null) {
                    listPosition = 0;
                }
                break;
            case R.id.menuItemFilterResult:
                POISettings poiSettings = settingsManagerInstance.getPOISettings();
                poiSettings.setDirectionFilterStatus(! poiSettings.filterPointListByDirection());
                listPosition = 0;
                if (poiManagerInstance.requestInProgress()) {
                    poiManagerInstance.cancelRequest();
                }
                preparePOIRequest(POIManager.ACTION_UPDATE);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
		return inflater.inflate(R.layout.fragment_poi, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            searchTerm = savedInstanceState.getString("searchTerm");
            listPosition = savedInstanceState.getInt("listPosition");
        } else {
            searchTerm = accessDatabaseInstance.getSearchTermOfPOIProfile(
                    settingsManagerInstance.getPOISettings().getSelectedPOIProfileId());
            listPosition = 0;
        }

        // top layout

        buttonSelectProfile = (Button) view.findViewById(R.id.buttonSelectProfile);
        buttonSelectProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectPOIProfileDialog.newInstance()
                    .show(getActivity().getSupportFragmentManager(), "SelectPOIProfileDialog");
            }
        });
        buttonSelectProfile.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                int selectedPOIProfileId = settingsManagerInstance.getPOISettings().getSelectedPOIProfileId();
                if (accessDatabaseInstance.getPOIProfileMap().containsKey(selectedPOIProfileId)) {
                    CreateOrEditPOIProfileDialog.newInstance(selectedPOIProfileId)
                        .show(getActivity().getSupportFragmentManager(), "CreateOrEditPOIProfileDialog");
                }
                return true;
            }
        });

        editSearch = (AutoCompleteTextView) view.findViewById(R.id.editInput);
        editSearch.setText(searchTerm);
        editSearch.setHint(getResources().getString(R.string.dialogSearch));
        editSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // cancel running request
                    if (poiManagerInstance.requestInProgress()) {
                        poiManagerInstance.cancelRequest();
                    }
                    // update poi profile search term
                    int selectedPOIProfileId = settingsManagerInstance.getPOISettings().getSelectedPOIProfileId();
                    String searchTermFromDatabase = accessDatabaseInstance.getSearchTermOfPOIProfile(selectedPOIProfileId);
                    if (! searchTerm.equals(searchTermFromDatabase)) {
                        accessDatabaseInstance.updateSearchTermOfPOIProfile(selectedPOIProfileId, searchTerm);
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
                int selectedPOIProfileId = settingsManagerInstance.getPOISettings().getSelectedPOIProfileId();
                String searchTermFromDatabase = accessDatabaseInstance.getSearchTermOfPOIProfile(selectedPOIProfileId);
                if (! TextUtils.isEmpty(searchTermFromDatabase)) {
                    accessDatabaseInstance.updateSearchTermOfPOIProfile(selectedPOIProfileId, "");
                }
                // reload poi profile
                preparePOIRequest(POIManager.ACTION_UPDATE);
            }
        });

        // content layout

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
                PointProfileObject pointProfileObject = (PointProfileObject) parent.getItemAtPosition(position);
                // add to all points profile
                accessDatabaseInstance.addFavoritePointToProfile(
                        pointProfileObject, HistoryPointProfile.ID_ALL_POINTS);
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

        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setText(getResources().getString(R.string.labelMoreResults));
        labelEmptyListView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // start poi profile update request
                preparePOIRequest(POIManager.ACTION_MORE_RESULTS);
            }
        });
        listViewPOI.setEmptyView(labelEmptyListView);

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
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("searchTerm", searchTerm);
        savedInstanceState.putInt("listPosition",  listPosition);
    }

    @Override public void onFragmentEnabled() {
        // listen for device shakes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_LOCATION);
        filter.addAction(Constants.ACTION_SHAKE_DETECTED);
        filter.addAction(Constants.ACTION_NEW_POI_PROFILE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // request poi
        preparePOIRequest(POIManager.ACTION_UPDATE);
    }

	@Override public void onFragmentDisabled() {
        poiManagerInstance.invalidateRequest((POIFragment) this);
        progressHandler.removeCallbacks(progressUpdater);
        // unregister shake broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    private void preparePOIRequest(int requestAction) {
        int selectedPOIProfileId = settingsManagerInstance.getPOISettings().getSelectedPOIProfileId();
        // hide keyboard
        editSearch.dismissDropDown();
        imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);

        // selected poi profile name
        if (accessDatabaseInstance.getPOIProfileMap().containsKey(selectedPOIProfileId)) {
            buttonSelectProfile.setText(
                    String.format(
                        getResources().getString(R.string.buttonSelectProfile),
                        accessDatabaseInstance.getPOIProfileMap().get(selectedPOIProfileId))
                    );
            // content description with poi categories
            ArrayList<String> categoryNameList = new ArrayList<String>();
            for (POICategory poiCategory : accessDatabaseInstance.getCategoryListOfPOIProfile(selectedPOIProfileId)) {
                categoryNameList.add(poiCategory.getName());
            }
            String categoryNameString = getResources().getString(R.string.poiCategoryNoneSelected);
            if (! categoryNameList.isEmpty()) {
                categoryNameString = TextUtils.join(", ", categoryNameList);
            }
            buttonSelectProfile.setContentDescription(
                    String.format(
                        getResources().getString(R.string.buttonSelectProfileCD),
                        accessDatabaseInstance.getPOIProfileMap().get(selectedPOIProfileId),
                        categoryNameString)
                    );
        } else {
            buttonSelectProfile.setText(
                    getResources().getString(R.string.buttonSelectProfileNone));
            buttonSelectProfile.setContentDescription(
                    getResources().getString(R.string.buttonSelectProfileNone));
        }

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
                (POIFragment) this, selectedPOIProfileId, requestAction);
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

            // fill listview
            if (poiSettings.filterPointListByDirection()) {
                // only include, what's ahead of the user
                ArrayList<PointProfileObject> listOfFilteredPOI = new ArrayList<PointProfileObject>();
                for (PointProfileObject pointProfileObject : listOfAllPOI) {
                    int bearing = pointProfileObject.bearingFromCenter();
                    if (bearing < 60 || bearing > 300) {
                        listOfFilteredPOI.add(pointProfileObject);
                    }
                }
                // header label
                if (! TextUtils.isEmpty(poiProfile.getSearchTerm())) {
                    labelHeading.setText(
                            String.format(
                                getResources().getString(R.string.labelPOIFragmentHeaderSearchFiltered),
                                getResources().getQuantityString(
                                    R.plurals.result, listOfFilteredPOI.size(), listOfFilteredPOI.size()),
                                poiProfile.getSearchTerm(),
                                getResources().getQuantityString(
                                    R.plurals.meter, poiProfile.getLookupRadius(), poiProfile.getLookupRadius()))
                            );
                } else {
                    labelHeading.setText(
                            String.format(
                                getResources().getString(R.string.labelPOIFragmentHeaderSuccessFiltered),
                                getResources().getQuantityString(
                                    R.plurals.poi, listOfFilteredPOI.size(), listOfFilteredPOI.size()),
                                getResources().getQuantityString(
                                    R.plurals.meter, poiProfile.getLookupRadius(), poiProfile.getLookupRadius()))
                            );
                }
                // fill adapter
                listViewPOI.setAdapter(
                        new POIProfilePointAdapter(
                            getActivity(),
                            listOfFilteredPOI)
                        );

            } else {
                // take all poi
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
                listViewPOI.setAdapter(
                        new POIProfilePointAdapter(
                            getActivity(),
                            listOfAllPOI)
                        );
            }

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


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                    && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD3.ID) {
            } else if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                // reload
                vibrator.vibrate(250);
                preparePOIRequest(POIManager.ACTION_UPDATE);
            } else if (intent.getAction().equals(Constants.ACTION_NEW_POI_PROFILE)) {
                if (editSearch != null) {
                    editSearch.setText(
                            accessDatabaseInstance.getSearchTermOfPOIProfile(
                                SettingsManager.getInstance(context).getPOISettings().getSelectedPOIProfileId()));
                }
                if (listViewPOI != null) {
                    listViewPOI.setSelection(0);
                }
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


    public static class SelectPOIProfileDialog extends DialogFragment {

        // Store instance variables
        private AccessDatabase accessDatabaseInstance;
        private SettingsManager settingsManagerInstance;
        private ListView listViewPOIProfiles;

        public static SelectPOIProfileDialog newInstance() {
            SelectPOIProfileDialog selectPOIProfileDialogInstance = new SelectPOIProfileDialog();
            return selectPOIProfileDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
            // listen for intents
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_NEW_POI_PROFILE);
            LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_list_view, nullParent);

            listViewPOIProfiles = (ListView) view.findViewById(R.id.listView);
            listViewPOIProfiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    POISettings poiSettings = settingsManagerInstance.getPOISettings();
                    poiSettings.setSelectedPOIProfileId(
                            (Integer) parent.getItemAtPosition(position));
                    Intent intent = new Intent(Constants.ACTION_NEW_POI_PROFILE);
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                    dismiss();
                }
            });

            TextView labelListViewEmpty    = (TextView) view.findViewById(R.id.labelListViewEmpty);
            labelListViewEmpty.setText(getResources().getString(R.string.errorNoPOIProfileCreated));
            listViewPOIProfiles.setEmptyView(labelListViewEmpty);

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectPOIProfileDialogTitle))
                .setView(view)
                .setNeutralButton(
                        getResources().getString(R.string.buttonAddPOIProfile),
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
                // neutral button
                Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        CreateOrEditPOIProfileDialog.newInstance(-1)
                            .show(getActivity().getSupportFragmentManager(), "CreateOrEditPOIProfileDialog");
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
            // fill list poi profile list
            listViewPOIProfiles.setAdapter(
                    new POIProfileAdapter(
                        getActivity(),
                        new ArrayList(accessDatabaseInstance.getPOIProfileMap().keySet()))
                    );
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        }

        private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.ACTION_NEW_POI_PROFILE)) {
                    if (listViewPOIProfiles != null) {
                        POIProfileAdapter poiProfileAdapter = (POIProfileAdapter) listViewPOIProfiles.getAdapter();
                        if (poiProfileAdapter != null) {
                            poiProfileAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        };
    }

}
