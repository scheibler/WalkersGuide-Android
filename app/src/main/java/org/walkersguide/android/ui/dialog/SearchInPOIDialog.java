package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.ui.dialog.SelectPOIProfileDialog;
import org.walkersguide.android.listener.SelectPOIProfileListener;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.database.DataSetObserver;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;

import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;

import android.text.Editable;
import android.text.TextUtils;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

import org.json.JSONException;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.data.profile.POIProfile;
import org.walkersguide.android.data.profile.SearchFavoritesProfile;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.google.AddressManager;
import org.walkersguide.android.helper.PointUtility;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.POIProfileListener;
import org.walkersguide.android.listener.SearchFavoritesProfileListener;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.FavoritesManager;
import org.walkersguide.android.server.POIManager;
import org.walkersguide.android.server.RouteManager;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.view.CheckBoxGroupView;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.TextChangedListener;
import android.widget.Spinner;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;


public class SearchInPOIDialog extends DialogFragment implements POIProfileListener {
    private static final int poiProfileId = SearchFavoritesProfile.ID_SEARCH;

	// Store instance variables
    private AccessDatabase accessDatabaseInstance;
    private InputMethodManager imm;
    private POIManager poiManagerInstance;
	private SettingsManager settingsManagerInstance;
    private String searchTerm;
    private int listPosition;
    private boolean searchStartedManually;

	// ui components
    private AutoCompleteTextView editSearch;
    private Button buttonSelectPOICategories;
    private TextView labelPOIFragmentHeader, labelMoreResultsFooter, labelListViewPOIEmpty;
    private ListView listViewPOI;

    // query in progress vibration
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private Vibrator vibrator;

    public static SearchInPOIDialog newInstance() {
        SearchInPOIDialog searchInPOIDialogInstance = new SearchInPOIDialog();
        Bundle args = new Bundle();
        args.putBoolean("searchStartedManually", false);
        searchInPOIDialogInstance.setArguments(args);
        return searchInPOIDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        accessDatabaseInstance = AccessDatabase.getInstance(context);
        poiManagerInstance = POIManager.getInstance(context);
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
            searchTerm = accessDatabaseInstance.getSearchTermOfPOIProfile(poiProfileId);
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

        buttonSelectPOICategories = (Button) view.findViewById(R.id.buttonSelectCategories);
        buttonSelectPOICategories.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectPOICategoriesForSearchDialog.newInstance().show(
                        getActivity().getSupportFragmentManager(), "SelectPOICategoriesForSearchDialog");
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

        labelListViewPOIEmpty    = (TextView) view.findViewById(R.id.labelListViewPOIEmpty);
        labelListViewPOIEmpty.setText(getResources().getString(R.string.labelMoreResults));
        labelListViewPOIEmpty.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                search(POIManager.ACTION_MORE_RESULTS);
            }
        });
        listViewPOI.setEmptyView(labelListViewPOIEmpty);
        labelListViewPOIEmpty.setVisibility(View.GONE);

        View footerView = inflater.inflate(R.layout.layout_single_text_view, null, false);
        labelMoreResultsFooter = (TextView) footerView.findViewById(R.id.label);
        labelMoreResultsFooter.setText(getResources().getString(R.string.labelMoreResults));
        labelMoreResultsFooter.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                search(POIManager.ACTION_MORE_RESULTS);
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.searchInPOIDialogTitle))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogSearch),
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
            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dialog.dismiss();
                }
            });
            // update ui
            // poi categories button
            updatePOICategoriesButton();
            // hide auto completion
            if (editSearch.getDropDownAnchor() != 0) {
                editSearch.dismissDropDown();
            }
            // restore last search
            if (searchStartedManually) {
                search(POIManager.ACTION_UPDATE);
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
        poiManagerInstance.invalidateRequest(SearchInPOIDialog.this);
        progressHandler.removeCallbacks(progressUpdater);
        // list view
        listViewPOI.setAdapter(null);
        listViewPOI.setOnScrollListener(null);
    }

    private void updatePOICategoriesButton() {
        ArrayList<POICategory> poiCategoryList = accessDatabaseInstance.getCategoryListOfPOIProfile(poiProfileId);
        buttonSelectPOICategories.setText(
                String.valueOf(poiCategoryList.size()));
        buttonSelectPOICategories.setContentDescription(
                String.format(
                    getResources().getString(R.string.buttonSelectCategoriesCD),
                    getResources().getQuantityString(
                        R.plurals.category, poiCategoryList.size(), poiCategoryList.size()))
                );
    }

    private void prepareForSearch() {
        if (TextUtils.isEmpty(searchTerm)) {
            SimpleMessageDialog.newInstance(
                    getResources().getString(R.string.messageError1030))
                .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
        } else if (! searchTerm.equals(accessDatabaseInstance.getSearchTermOfPOIProfile(poiProfileId))) {
            accessDatabaseInstance.updateSearchTermOfPOIProfile(poiProfileId, searchTerm);
            settingsManagerInstance.getSearchTermHistory().addSearchTerm(searchTerm);
        }
        search(POIManager.ACTION_UPDATE);
    }

    private void search(int requestAction) {
        // hide keyboard
        imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
        // start or cancel search
        if (poiManagerInstance.requestInProgress()) {
            poiManagerInstance.cancelRequest();
        } else {
            // update ui
            labelPOIFragmentHeader.setText(
                    getResources().getString(R.string.messagePleaseWait));
            listViewPOI.setAdapter(null);
            listViewPOI.setOnScrollListener(null);
            if (listViewPOI.getFooterViewsCount() > 0) {
                listViewPOI.removeFooterView(labelMoreResultsFooter);
            }
            labelListViewPOIEmpty.setVisibility(View.GONE);
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setText(getResources().getString(R.string.dialogCancel));
            }
            // search in background
            poiManagerInstance.requestPOIProfile(
                    SearchInPOIDialog.this, poiProfileId, requestAction);
            // start progress vibrator
            progressHandler.postDelayed(progressUpdater, 2000);
        }
    }

	@Override public void poiProfileRequestFinished(int returnCode, String returnMessage, POIProfile poiProfile, boolean resetListPosition) {
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setText(getResources().getString(R.string.dialogSearch));
        }
        progressHandler.removeCallbacks(progressUpdater);
        searchStartedManually = true;

        if (poiProfile != null
                && poiProfile.getPointProfileObjectList() != null) {
            ArrayList<PointProfileObject> listOfAllPOI = poiProfile.getPointProfileObjectList();
            labelPOIFragmentHeader.setText(
                    String.format(
                        getResources().getString(R.string.labelSearchPOIDialogHeaderSuccess),
                        getResources().getQuantityString(
                            R.plurals.result, listOfAllPOI.size(), listOfAllPOI.size()),
                        poiProfile.getSearchTerm(),
                        poiProfile.getLookupRadius())
                    );
            // fill adapter
            listViewPOI.setAdapter(
                    new ArrayAdapter<PointProfileObject>(
                        getActivity(),
                        android.R.layout.simple_list_item_1,
                        listOfAllPOI)
                    );

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
        // unregister broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_UPDATE_UI)) {
                // first update ui
                updatePOICategoriesButton();
                // then search again
                if (searchStartedManually) {
                    search(POIManager.ACTION_UPDATE);
                }
            } else if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                if (searchStartedManually) {
                    vibrator.vibrate(250);
                    search(POIManager.ACTION_UPDATE);
                }
            } else if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                    && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD3.ID) {
                if (searchStartedManually) {
                    search(POIManager.ACTION_UPDATE);
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


    public static class SelectPOICategoriesForSearchDialog extends DialogFragment implements SelectPOIProfileListener {

        private static final int poiProfileId = SearchFavoritesProfile.ID_SEARCH;
        private ArrayList<POICategory> checkedCategoryList;

        private AccessDatabase accessDatabaseInstance;
        private Button buttonQuickSelectPOICategories;
        private CheckBoxGroupView checkBoxGroupPOICategories;

        public static SelectPOICategoriesForSearchDialog newInstance() {
            SelectPOICategoriesForSearchDialog selectPOICategoriesForSearchDialogInstance = new SelectPOICategoriesForSearchDialog();
            return selectPOICategoriesForSearchDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                checkedCategoryList = new ArrayList<POICategory>();
                JSONArray jsonCheckedCategoryIdList = null;
                try {
                    jsonCheckedCategoryIdList = new JSONArray(
                            savedInstanceState.getString("jsonCheckedCategoryIdList"));
                } catch (JSONException e) {
                    jsonCheckedCategoryIdList = null;
                } finally {
                    if (jsonCheckedCategoryIdList != null) {
                        for (int i=0; i<jsonCheckedCategoryIdList.length(); i++) {
                            POICategory poiCategory = null;
                            try {
                                poiCategory = accessDatabaseInstance.getPOICategory(jsonCheckedCategoryIdList.getInt(i));
                            } catch (JSONException e) {
                                poiCategory = null;
                            } finally {
                                if (poiCategory != null) {
                                    checkedCategoryList.add(poiCategory);
                                }
                            }
                        }
                    }
                }
            } else {
                checkedCategoryList = accessDatabaseInstance.getCategoryListOfPOIProfile(poiProfileId);
            }

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_select_poi_categories_for_search_dialog, nullParent);

            buttonQuickSelectPOICategories = (Button) view.findViewById(R.id.buttonQuickSelectPOICategories);
            buttonQuickSelectPOICategories.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    SelectPOIProfileDialog selectPOIProfileDialog = SelectPOIProfileDialog.newInstance(matchPOIProfileToCheckedPOICategories());
                    selectPOIProfileDialog.setTargetFragment(SelectPOICategoriesForSearchDialog.this, 1);
                    selectPOIProfileDialog.show(getActivity().getSupportFragmentManager(), "SelectPOIProfileDialog");
                }
            });

            checkBoxGroupPOICategories = (CheckBoxGroupView) view.findViewById(R.id.checkBoxGroupPOICategories);
            for (POICategory category : accessDatabaseInstance.getPOICategoryList()) {
                CheckBox checkBox = new CheckBox(getActivity());
                checkBox.setId(category.getId());
                checkBox.setLayoutParams(
                        new LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT)
                        );
                checkBox.setText(category.getName());
                checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        checkedCategoryList = getCheckedItemsOfPOICategoriesCheckBoxGroup();
                        onStart();
                    }
                });
                checkBoxGroupPOICategories.put(checkBox);
            }

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectPOICategoriesForSearchDialogTitle))
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
                for (CheckBox checkBox : checkBoxGroupPOICategories.getCheckBoxList()) {
                    checkBox.setChecked(
                            checkedCategoryList.contains(
                                accessDatabaseInstance.getPOICategory(checkBox.getId())));
                }

                // positive button
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        accessDatabaseInstance.updateNameAndCategoryListOfPOIProfile(
                                poiProfileId,
                                accessDatabaseInstance.getNameOfPOIProfile(poiProfileId),
                                getCheckedItemsOfPOICategoriesCheckBoxGroup());
                        // update ui and dismiss
                        Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                        dialog.dismiss();
                    }
                });

                // neutral button
                Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                if (checkBoxGroupPOICategories.nothingChecked()) {
                    buttonNeutral.setText(
                            getResources().getString(R.string.dialogAll));
                } else {
                    buttonNeutral.setText(
                            getResources().getString(R.string.dialogClear));
                }
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        if (checkBoxGroupPOICategories.nothingChecked()) {
                            checkedCategoryList = accessDatabaseInstance.getPOICategoryList();
                        } else {
                            checkedCategoryList = new ArrayList<POICategory>();
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

                // select poi profile button
                int matchedPOIProfileId = matchPOIProfileToCheckedPOICategories();
                String matchedPOIProfileName = getResources().getString(R.string.labelNoPOIProfileMatches);
                if (accessDatabaseInstance.getPOIProfileMap().containsKey(matchedPOIProfileId)) {
                    matchedPOIProfileName = accessDatabaseInstance.getNameOfPOIProfile(matchedPOIProfileId);
                }
                buttonQuickSelectPOICategories.setContentDescription(
                        String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.buttonQuickSelectPOICategories),
                            matchedPOIProfileName)
                        );
                buttonQuickSelectPOICategories.setText(matchedPOIProfileName);
            }
        }

        @Override public void poiProfileSelected(int poiProfileId) {
            checkedCategoryList = accessDatabaseInstance.getCategoryListOfPOIProfile(poiProfileId);
            onStart();
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            JSONArray jsonCheckedCategoryIdList = new JSONArray();
            for (POICategory poiCategory : getCheckedItemsOfPOICategoriesCheckBoxGroup()) {
                jsonCheckedCategoryIdList.put(poiCategory.getId());
            }
            savedInstanceState.putString("jsonCheckedCategoryIdList", jsonCheckedCategoryIdList.toString());
        }

        private int matchPOIProfileToCheckedPOICategories() {
            ArrayList<POICategory> checkedPOICategoryList = getCheckedItemsOfPOICategoriesCheckBoxGroup();
            for(Integer profileId : accessDatabaseInstance.getPOIProfileMap().keySet()) {
                ArrayList<POICategory> poiCategoryListOfProfile = accessDatabaseInstance.getCategoryListOfPOIProfile(profileId);
                if (checkedPOICategoryList.size() == poiCategoryListOfProfile.size()
                        && checkedPOICategoryList.containsAll(poiCategoryListOfProfile)
                        && poiCategoryListOfProfile.containsAll(checkedPOICategoryList)) {
                    return profileId;
                }
            }
            return -1;
        }

        private ArrayList<POICategory> getCheckedItemsOfPOICategoriesCheckBoxGroup() {
            ArrayList<POICategory> poiCategoryList = new ArrayList<POICategory>();
            for (CheckBox checkBox : checkBoxGroupPOICategories.getCheckedCheckBoxList()) {
                POICategory category = accessDatabaseInstance.getPOICategory(checkBox.getId());
                if (category != null) {
                    poiCategoryList.add(category);
                }
            }
            return poiCategoryList;
        }
    }

}
