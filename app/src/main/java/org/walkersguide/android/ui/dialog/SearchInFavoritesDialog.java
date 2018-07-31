package org.walkersguide.android.ui.dialog;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.SearchFavoritesProfile;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.server.AddressManager;
import org.walkersguide.android.helper.PointUtility;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.RouteManager;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.FavoritesSearchSettings;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import java.util.ArrayList;
import org.walkersguide.android.helper.StringUtility;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.ListView;
import org.walkersguide.android.util.TextChangedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.AdapterView;
import android.text.TextUtils;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.listener.SearchFavoritesProfileListener;
import org.walkersguide.android.server.FavoritesManager;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.text.Editable;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.json.JSONException;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import org.walkersguide.android.ui.view.CheckBoxGroupView;
import android.widget.CheckBox;
import java.util.Map;
import android.database.DataSetObserver;
import android.widget.AbsListView;
import org.json.JSONArray;


public class SearchInFavoritesDialog extends DialogFragment implements SearchFavoritesProfileListener {
    private static final int poiProfileId = SearchFavoritesProfile.ID_SEARCH;

	// Store instance variables
    private InputMethodManager imm;
    private FavoritesManager favoritesManagerInstance;
	private SettingsManager settingsManagerInstance;
    private String searchTerm;
    private int listPosition;
    private boolean searchStartedManually;

	// ui components
    private AutoCompleteTextView editSearch;
    private Button buttonSelectFavoritesProfiles;
    private TextView labelPOIFragmentHeader;
    private ListView listViewPOI;

    // query in progress vibration
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private Vibrator vibrator;

    public static SearchInFavoritesDialog newInstance() {
        SearchInFavoritesDialog searchInFavoritesDialogInstance = new SearchInFavoritesDialog();
        Bundle args = new Bundle();
        args.putBoolean("searchStartedManually", false);
        searchInFavoritesDialogInstance.setArguments(args);
        return searchInFavoritesDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        favoritesManagerInstance = FavoritesManager.getInstance(context);
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        settingsManagerInstance = SettingsManager.getInstance(context);
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
            searchTerm = savedInstanceState.getString("searchTerm");
            listPosition = savedInstanceState.getInt("listPosition");
            searchStartedManually = savedInstanceState.getBoolean("searchStartedManually");
        } else {
            searchTerm = settingsManagerInstance.getFavoritesSearchSettings().getSearchTerm();
            listPosition = 0;
            searchStartedManually = getArguments().getBoolean("searchStartedManually");
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_search, nullParent);

        editSearch = (AutoCompleteTextView) view.findViewById(R.id.editSearch);
        editSearch.setText(searchTerm);
        editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    prepareForSearch();
                    return true;
                }
                return false;
            }
        });
        editSearch.addTextChangedListener(new TextChangedListener<AutoCompleteTextView>(editSearch) {
            @Override public void onTextChanged(AutoCompleteTextView view, Editable s) {
                searchTerm = editSearch.getText().toString();
            }
        });
        // add auto complete suggestions
        ArrayAdapter<String> searchTermHistoryAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                settingsManagerInstance.getSearchTermHistory().getSearchTermList());
        editSearch.setAdapter(searchTermHistoryAdapter);

        ImageButton buttonDelete = (ImageButton) view.findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // clear edit text
                editSearch.setText("");
                imm.showSoftInput(editSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        buttonSelectFavoritesProfiles = (Button) view.findViewById(R.id.buttonSelectCategories);
        buttonSelectFavoritesProfiles.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectFavoritesProfilesForSearchDialog.newInstance().show(
                        getActivity().getSupportFragmentManager(), "SelectFavoritesProfilesForSearchDialog");
            }
        });

        labelPOIFragmentHeader = (TextView) view.findViewById(R.id.labelPOIFragmentHeader);
        labelPOIFragmentHeader.setText(
                getResources().getString(R.string.labelStartSearch));

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
        TextView labelListViewPOIEmpty    = (TextView) view.findViewById(R.id.labelListViewPOIEmpty);
        labelListViewPOIEmpty.setVisibility(View.GONE);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.searchInFavoritesDialogTitle))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogSearch),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNeutralButton(
                    getResources().getString(R.string.dialogSort),
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
            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    prepareForSearch();
                }
            });
            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    SelectSortCriteriaForSearchDialog.newInstance().show(
                            getActivity().getSupportFragmentManager(), "SelectSortCriteriaForSearchDialog");
                }
            });
            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dialog.dismiss();
                }
            });
            // update ui
            // favorites profiles button
            updateFavoritesProfilesButton();
            // hide auto completion
            if (editSearch.getDropDownAnchor() != 0) {
                editSearch.dismissDropDown();
            }
            // restore last search
            if (searchStartedManually) {
                search();
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("searchTerm", searchTerm);
        savedInstanceState.putInt("listPosition",  listPosition);
        savedInstanceState.putBoolean("searchStartedManually", searchStartedManually);
    }

    @Override public void onStop() {
        super.onStop();
        favoritesManagerInstance.invalidateFavoritesSearchRequest(SearchInFavoritesDialog.this);
        progressHandler.removeCallbacks(progressUpdater);
        // list view
        listViewPOI.setAdapter(null);
        listViewPOI.setOnScrollListener(null);
    }

    private void updateFavoritesProfilesButton() {
        FavoritesSearchSettings favoritesSearchSettings = settingsManagerInstance.getFavoritesSearchSettings();
        buttonSelectFavoritesProfiles.setText(
                String.valueOf(
                    favoritesSearchSettings.getFavoritesProfileIdList().size()));
        buttonSelectFavoritesProfiles.setContentDescription(
                String.format(
                    getResources().getString(R.string.buttonSelectCategoriesCD),
                    getResources().getQuantityString(
                        R.plurals.profile,
                        favoritesSearchSettings.getFavoritesProfileIdList().size(),
                        favoritesSearchSettings.getFavoritesProfileIdList().size()))
                );
    }

    private void prepareForSearch() {
        if (TextUtils.isEmpty(searchTerm)) {
            SimpleMessageDialog.newInstance(
                    getResources().getString(R.string.messageError1030))
                .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
        } else if (! searchTerm.equals(settingsManagerInstance.getFavoritesSearchSettings().getSearchTerm())) {
            settingsManagerInstance.getFavoritesSearchSettings().setSearchTerm(searchTerm);
            settingsManagerInstance.getSearchTermHistory().addSearchTerm(searchTerm);
        }
        search();
    }

    private void search() {
        // hide keyboard
        imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
        // start or cancel search
        if (favoritesManagerInstance.searchRequestInProgress()) {
            favoritesManagerInstance.cancelSearchRequest();
        } else {
            // update ui
            labelPOIFragmentHeader.setText(
                    getResources().getString(R.string.messagePleaseWait));
            listViewPOI.setAdapter(null);
            listViewPOI.setOnScrollListener(null);
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setText(getResources().getString(R.string.dialogCancel));
            }
            // search in background
            FavoritesSearchSettings favoritesSearchSettings = settingsManagerInstance.getFavoritesSearchSettings();
            favoritesManagerInstance.requestFavoritesSearch(
                    SearchInFavoritesDialog.this,
                    favoritesSearchSettings.getSearchTerm(),
                    favoritesSearchSettings.getFavoritesProfileIdList(),
                    favoritesSearchSettings.getSortCriteria());
            // start progress vibrator
            progressHandler.postDelayed(progressUpdater, 2000);
        }
    }

	@Override public void searchFavoritesProfileRequestFinished(int returnCode,
            String returnMessage, SearchFavoritesProfile searchFavoritesProfile, boolean resetListPosition) {
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setText(getResources().getString(R.string.dialogSearch));
        }
        progressHandler.removeCallbacks(progressUpdater);
        searchStartedManually = true;

        FavoritesSearchSettings favoritesSearchSettings = settingsManagerInstance.getFavoritesSearchSettings();
        if (searchFavoritesProfile != null
                && searchFavoritesProfile.getPointProfileObjectList() != null) {
            ArrayList<PointProfileObject> listOfAllPOI = searchFavoritesProfile.getPointProfileObjectList();
            labelPOIFragmentHeader.setText(
                    String.format(
                        getResources().getString(R.string.labelSearchFavoritesDialogHeaderSuccess),
                        getResources().getQuantityString(
                            R.plurals.result, listOfAllPOI.size(), listOfAllPOI.size()),
                        favoritesSearchSettings.getSearchTerm(),
                        StringUtility.formatProfileSortCriteria(
                            getActivity(), searchFavoritesProfile.getSortCriteria()))
                    );

            // fill adapter
            listViewPOI.setAdapter(
                    new ArrayAdapter<PointProfileObject>(
                        getActivity(),
                        android.R.layout.simple_list_item_1,
                        listOfAllPOI)
                    );

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
            labelPOIFragmentHeader.setText(returnMessage);
        }

        // error message dialog
        if (! (returnCode == Constants.ID.OK || returnCode == Constants.ID.CANCELLED)) {
            SimpleMessageDialog.newInstance(returnMessage)
                .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
        }
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        progressHandler.removeCallbacks(progressUpdater);
        // unregister shake broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_UPDATE_UI)) {
                // first update ui
                updateFavoritesProfilesButton();
                // then search again
                if (searchStartedManually) {
                    search();
                }
            } else if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                if (searchStartedManually) {
                    vibrator.vibrate(250);
                    search();
                }
            } else if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                    && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD3.ID) {
                if (searchStartedManually) {
                    search();
                }
            }
        }
    };

    private class ProgressUpdater implements Runnable {
        public void run() {
            vibrator.vibrate(50);
            progressHandler.postDelayed(this, 2000);
        }
    }


    public static class SelectFavoritesProfilesForSearchDialog extends DialogFragment {

        private static final int poiProfileId = SearchFavoritesProfile.ID_SEARCH;
        private ArrayList<Integer> checkedProfileIdList;

        private AccessDatabase accessDatabaseInstance;
        private SettingsManager settingsManagerInstance;
        private CheckBoxGroupView checkBoxGroupFavoritesProfiles;

        public static SelectFavoritesProfilesForSearchDialog newInstance() {
            SelectFavoritesProfilesForSearchDialog selectFavoritesProfilesForSearchDialogInstance = new SelectFavoritesProfilesForSearchDialog();
            return selectFavoritesProfilesForSearchDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                checkedProfileIdList = new ArrayList<Integer>();
                JSONArray jsonCheckedProfileIdList = null;
                try {
                    jsonCheckedProfileIdList = new JSONArray(
                            savedInstanceState.getString("jsonCheckedProfileIdList"));
                } catch (JSONException e) {
                    jsonCheckedProfileIdList = null;
                } finally {
                    if (jsonCheckedProfileIdList != null) {
                        for (int i=0; i<jsonCheckedProfileIdList.length(); i++) {
                            try {
                                checkedProfileIdList.add(jsonCheckedProfileIdList.getInt(i));
                            } catch (JSONException e) {}
                        }
                    }
                }
            } else {
                checkedProfileIdList = settingsManagerInstance.getFavoritesSearchSettings().getFavoritesProfileIdList();
            }

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_check_box_group, nullParent);

            checkBoxGroupFavoritesProfiles = (CheckBoxGroupView) view.findViewById(R.id.checkBoxGroup);
            for (Map.Entry<Integer,String> profile : accessDatabaseInstance.getFavoritesProfileMap().entrySet()) {
                CheckBox checkBox = new CheckBox(getActivity());
                checkBox.setId(profile.getKey());
                checkBox.setLayoutParams(
                        new LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT)
                        );
                checkBox.setText(profile.getValue());
                checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        checkedProfileIdList = getCheckedItemsOfFavoritesProfilesCheckBoxGroup();
                        onStart();
                    }
                });
                checkBoxGroupFavoritesProfiles.put(checkBox);
            }

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectFavoritesProfilesForSearchDialogTitle))
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

                // check boxes
                for (CheckBox checkBox : checkBoxGroupFavoritesProfiles.getCheckBoxList()) {
                    checkBox.setChecked(checkedProfileIdList.contains(checkBox.getId()));
                }

                // positive button
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        settingsManagerInstance.getFavoritesSearchSettings()
                            .setFavoritesProfileIdList(getCheckedItemsOfFavoritesProfilesCheckBoxGroup());
                        // update ui and dismiss
                        Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                        dialog.dismiss();
                    }
                });

                // neutral button
                Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                if (checkBoxGroupFavoritesProfiles.nothingChecked()) {
                    buttonNeutral.setText(
                            getResources().getString(R.string.dialogAll));
                } else {
                    buttonNeutral.setText(
                            getResources().getString(R.string.dialogClear));
                }
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        checkedProfileIdList = new ArrayList<Integer>();
                        if (checkBoxGroupFavoritesProfiles.nothingChecked()) {
                            checkedProfileIdList.addAll(accessDatabaseInstance.getFavoritesProfileMap().keySet());
                        }
                        onStart();
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

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            JSONArray jsonCheckedProfileIdList = new JSONArray();
            for (Integer id : getCheckedItemsOfFavoritesProfilesCheckBoxGroup()) {
                jsonCheckedProfileIdList.put(id);
            }
            savedInstanceState.putString("jsonCheckedProfileIdList", jsonCheckedProfileIdList.toString());
        }

        private ArrayList<Integer> getCheckedItemsOfFavoritesProfilesCheckBoxGroup() {
            ArrayList<Integer> favoritesProfileList = new ArrayList<Integer>();
            for (CheckBox checkBox : checkBoxGroupFavoritesProfiles.getCheckedCheckBoxList()) {
                favoritesProfileList.add(checkBox.getId());
            }
            return favoritesProfileList;
        }
    }


    public static class SelectSortCriteriaForSearchDialog extends DialogFragment {

        private SettingsManager settingsManagerInstance;
        private RadioGroup radioGroupFavoritesSearchSortCriteria;

        public static SelectSortCriteriaForSearchDialog newInstance() {
            SelectSortCriteriaForSearchDialog selectSortCriteriaForSearchDialogInstance = new SelectSortCriteriaForSearchDialog();
            return selectSortCriteriaForSearchDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_radio_group, nullParent);

            radioGroupFavoritesSearchSortCriteria = (RadioGroup) view.findViewById(R.id.radioGroup);
            radioGroupFavoritesSearchSortCriteria.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (checkedId != settingsManagerInstance.getFavoritesSearchSettings().getSortCriteria()) {
                        settingsManagerInstance.getFavoritesSearchSettings().setSortCriteria(checkedId);
                        // update ui and dismiss
                        Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                        dismiss();
                    }
                }
            });

            for (int sortCriteriaId : Constants.SearchFavoritesProfileSortCriteriaValueArray) {
                RadioButton radioButton = new RadioButton(getActivity());
                radioButton.setId(sortCriteriaId);
                radioButton.setLayoutParams(
                        new LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT)
                        );
                switch (sortCriteriaId) {
                    case Constants.SORT_CRITERIA.DISTANCE_ASC:
                        radioButton.setText(getResources().getString(R.string.radioButtonSortDistanceAsc));
                        break;
                    case Constants.SORT_CRITERIA.DISTANCE_DESC:
                        radioButton.setText(getResources().getString(R.string.radioButtonSortDistanceDesc));
                        break;
                    case Constants.SORT_CRITERIA.NAME_ASC:
                        radioButton.setText(getResources().getString(R.string.radioButtonSortNameAsc));
                        break;
                    case Constants.SORT_CRITERIA.NAME_DESC:
                        radioButton.setText(getResources().getString(R.string.radioButtonSortNameDesc));
                        break;
                    case Constants.SORT_CRITERIA.ORDER_ASC:
                        radioButton.setText(getResources().getString(R.string.radioButtonSortOrderAsc));
                        break;
                    case Constants.SORT_CRITERIA.ORDER_DESC:
                        radioButton.setText(getResources().getString(R.string.radioButtonSortOrderDesc));
                        break;
                    default:
                        radioButton = null;
                        break;
                }
                if (radioButton != null) {
                    radioGroupFavoritesSearchSortCriteria.addView(radioButton);
                }
            }

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectSortCriteriaForSearchDialogTitle))
                .setView(view)
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
                // check radio group
                radioGroupFavoritesSearchSortCriteria.check(
                        settingsManagerInstance.getFavoritesSearchSettings().getSortCriteria());
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
        }
    }

}
