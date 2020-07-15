package org.walkersguide.android.ui.fragment.main;

import androidx.core.view.ViewCompat;
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

import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.widget.Toolbar;

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
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.json.JSONException;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.data.basic.wrapper.PointWrapper.SortByNameASC;
import org.walkersguide.android.data.poi.POICategory;
import org.walkersguide.android.data.profile.HistoryPointProfile;
import org.walkersguide.android.data.profile.POIProfile;
import org.walkersguide.android.data.sensor.attribute.NewLocationAttributes;
import org.walkersguide.android.data.sensor.threshold.DistanceThreshold;
import org.walkersguide.android.helper.PointUtility;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.server.POIManager;
import org.walkersguide.android.server.POIManager.HistoryPointProfileListener;
import org.walkersguide.android.server.POIManager.POIProfileListener;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.adapter.POIProfileAdapter;
import org.walkersguide.android.ui.adapter.POIProfilePointAdapter;
import org.walkersguide.android.ui.dialog.CreateOrEditPOIProfileDialog;
import org.walkersguide.android.ui.dialog.SelectMapDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog.ChildDialogCloseListener;
import org.walkersguide.android.ui.fragment.AbstractUITab;
import org.walkersguide.android.ui.listener.TextChangedListener;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.POISettings;


public class POIFragment extends AbstractUITab implements HistoryPointProfileListener, POIProfileListener {

    public enum ContentType {
        HISTORY_POINTS, POI
    }

	// Store instance variables
    private AccessDatabase accessDatabaseInstance;
    private InputMethodManager imm;
    private POIManager poiManagerInstance;
	private SettingsManager settingsManagerInstance;

    private ContentType contentType;
    private int pointPutInto;
    private String searchTerm;
    private int listPosition;

    // dialog variables
    private ChildDialogCloseListener childDialogCloseListener;

    // query in progress
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private Vibrator vibrator;

    // ui components
    private Toolbar toolbar;
    private AutoCompleteTextView editSearch;
    private Button buttonSelectProfile;
    private ImageButton buttonRefresh, buttonClearSearch;
    private ListView listViewPOI;
    private TextView labelHeading, labelEmptyListView, labelMoreResultsFooter;

	// newInstance constructor for creating dialog or embedded fragment
	public static POIFragment newInstance(ContentType contentType, int pointPutInto) {
		POIFragment poiFragmentInstance = new POIFragment();
        Bundle args = new Bundle();
        args.putSerializable("contentType", contentType);
        args.putInt("pointPutInto", pointPutInto);
        poiFragmentInstance.setArguments(args);
		return poiFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
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
        // ChildDialogCloseListener
        if (getTargetFragment() != null
                && getTargetFragment() instanceof ChildDialogCloseListener) {
            childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
        }
	}

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        childDialogCloseListener = null;
    }


    /**
     * menu
     */

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_poi_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        prepareMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (processMenuItemClick(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void prepareMenu(Menu menu) {
        // check or uncheck filter result menu item
        MenuItem menuItemFilterResult = menu.findItem(R.id.menuItemFilterResult);
        if (menuItemFilterResult != null) {
            menuItemFilterResult.setChecked(
                    settingsManagerInstance.getPOISettings().filterPointListByDirection());
        }
    }

    private boolean processMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemJumpToTop:
                if (listViewPOI.getAdapter() != null) {
                    listViewPOI.setSelection(0);
                }
                break;
            case R.id.menuItemFilterResult:
                POISettings poiSettings = settingsManagerInstance.getPOISettings();
                poiSettings.setDirectionFilterStatus(! poiSettings.filterPointListByDirection());
                listPosition = 0;
                // reload ui
                Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                break;
            default:
                return false;
        }
        return true;
    }


    /**
     * create view
     */

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        if (getDialog() != null) {
            // fragment is a dialog
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            // fragment is embetted
		    return configureView(
                   inflater.inflate(R.layout.fragment_poi, container, false),
                   savedInstanceState);
        }
	}

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = configureView(
                inflater.inflate(R.layout.fragment_poi, nullParent),
                savedInstanceState);

        String dialogTitle;
        if (contentType.equals(ContentType.POI)) {
            if (pointPutInto == Constants.POINT_PUT_INTO.NOWHERE) {
                dialogTitle = getResources().getString(R.string.fragmentPOIName);
            } else {
                dialogTitle = getResources().getString(R.string.poiFragmentPOIProfileDialogTitle);
            }
        } else {
            if (pointPutInto == Constants.POINT_PUT_INTO.NOWHERE) {
                dialogTitle = getResources().getString(R.string.menuItemLastVisitedPoints);
            } else {
                dialogTitle = getResources().getString(R.string.poiFragmentHistoryPointProfileDialogTitle);
            }
        }

        // configure the toolbar
        toolbar.setTitle(dialogTitle);
        toolbar.inflateMenu(R.menu.menu_toolbar_poi_fragment);
        prepareMenu(toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                boolean success = processMenuItemClick(item);
                prepareMenu(toolbar.getMenu());
                return success;
            }
        });
        toolbar.setVisibility(View.VISIBLE);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setView(view)
            .setNegativeButton(
                    pointPutInto == Constants.POINT_PUT_INTO.NOWHERE
                    ? getResources().getString(R.string.dialogClose)
                    : getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
            .create();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable("contentType", contentType);
        savedInstanceState.putInt("pointPutInto",  pointPutInto);
        savedInstanceState.putString("searchTerm", searchTerm);
        savedInstanceState.putInt("listPosition",  listPosition);
    }

	private View configureView(View view, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            contentType = (ContentType) savedInstanceState.getSerializable("contentType");
            pointPutInto = savedInstanceState.getInt("pointPutInto");
            searchTerm = savedInstanceState.getString("searchTerm");
            listPosition = savedInstanceState.getInt("listPosition");
        } else {
            contentType = (ContentType) getArguments().getSerializable("contentType");
            pointPutInto = getArguments().getInt("pointPutInto");
            if (contentType.equals(ContentType.POI)) {
                searchTerm = accessDatabaseInstance.getSearchTermOfPOIProfile(
                        settingsManagerInstance.getPOISettings().getSelectedPOIProfileId());
            } else {
                searchTerm = "";
            }
            listPosition = 0;
        }

        // top layout
        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setVisibility(View.GONE);

        buttonSelectProfile = (Button) view.findViewById(R.id.buttonSelectProfile);
        buttonSelectProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (contentType.equals(ContentType.POI)) {
                    SelectPOIProfileDialog.newInstance()
                        .show(getActivity().getSupportFragmentManager(), "SelectPOIProfileDialog");
                } else {
                    SelectHistoryPointProfileDialog.newInstance()
                        .show(getActivity().getSupportFragmentManager(), "SelectHistoryPointProfileDialog");
                }
            }
        });
        buttonSelectProfile.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (contentType.equals(ContentType.POI)) {
                    int selectedPOIProfileId = settingsManagerInstance.getPOISettings().getSelectedPOIProfileId();
                    if (accessDatabaseInstance.getPOIProfileMap().containsKey(selectedPOIProfileId)) {
                        CreateOrEditPOIProfileDialog.newInstance(selectedPOIProfileId)
                            .show(getActivity().getSupportFragmentManager(), "CreateOrEditPOIProfileDialog");
                    }
                }
                return true;
            }
        });

        editSearch = (AutoCompleteTextView) view.findViewById(R.id.editInput);
        editSearch.setHint(getResources().getString(R.string.dialogSearch));
        editSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        editSearch.setText(searchTerm);
        editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    if (contentType.equals(ContentType.POI)) {
                        // cancel poi profile request
                        if (poiManagerInstance.requestInProgress()) {
                            poiManagerInstance.cancelRequest();
                        }
                        // update poi profile search term
                        int selectedPOIProfileId = settingsManagerInstance.getPOISettings().getSelectedPOIProfileId();
                        String searchTermFromDatabase = accessDatabaseInstance.getSearchTermOfPOIProfile(selectedPOIProfileId);
                        if (! searchTerm.equals(searchTermFromDatabase)) {
                            accessDatabaseInstance.updateSearchTermOfPOIProfile(selectedPOIProfileId, searchTerm);
                        }
                        // reload poi profile
                        preparePOIRequest(getActivity(), POIManager.ACTION_UPDATE);
                    } else {
                        // cancel history point profile request
                        if (poiManagerInstance.historyPointProfileRequestInProgress()) {
                            poiManagerInstance.cancelHistoryPointProfileRequest();
                        }
                        // reload history point profile
                        prepareHistoryPointRequest(getActivity());
                    }
                    // add to search term history
                    if (! TextUtils.isEmpty(searchTerm)) {
                        settingsManagerInstance.getSearchTermHistory().addSearchTerm(searchTerm);
                    }
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
                if (contentType.equals(ContentType.POI)) {
                    // cancel poi profile request
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
                    preparePOIRequest(getActivity(), POIManager.ACTION_UPDATE);
                } else {
                    // cancel history point profile request
                    if (poiManagerInstance.historyPointProfileRequestInProgress()) {
                        poiManagerInstance.cancelHistoryPointProfileRequest();
                    }
                    // reload history point profile
                    prepareHistoryPointRequest(getActivity());
                }
            }
        });

        // content layout

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);

        buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (contentType.equals(ContentType.POI)) {
                    if (poiManagerInstance.requestInProgress()) {
                        // cancel poi profile request
                        poiManagerInstance.cancelRequest();
                    } else {
                        // reload poi profile
                        preparePOIRequest(getActivity(), POIManager.ACTION_UPDATE);
                    }
                } else {
                    if (poiManagerInstance.historyPointProfileRequestInProgress()) {
                        // cancel history point profile request
                        poiManagerInstance.cancelHistoryPointProfileRequest();
                    } else {
                        // reload history point profile
                        prepareHistoryPointRequest(getActivity());
                    }
                }
            }
        });

        listViewPOI = (ListView) view.findViewById(R.id.listView);
        listViewPOI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final PointProfileObject selectedPoint = (PointProfileObject) parent.getItemAtPosition(position);
                if (pointPutInto == Constants.POINT_PUT_INTO.NOWHERE) {
                    addToPointHistoryAndShowPointDetails(selectedPoint);
                } else {
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
                                    addToPointHistoryAndShowPointDetails(selectedPoint);
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    popupMore.show();
                }
            }
            private void addToPointHistoryAndShowPointDetails(PointProfileObject selectedPoint) {
                if (contentType.equals(ContentType.POI)) {
                    // add to all points profile
                    accessDatabaseInstance.addFavoritePointToProfile(
                            selectedPoint, HistoryPointProfile.ID_ALL_POINTS);
                }
                // show point details dialog
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
            }
        });
        listViewPOI.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                return true;
            }
        });

        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (contentType.equals(ContentType.POI)) {
                    // start poi profile update request
                    preparePOIRequest(getActivity(), POIManager.ACTION_MORE_RESULTS);
                }
            }
        });
        listViewPOI.setEmptyView(labelEmptyListView);

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View footerView = inflater.inflate(R.layout.layout_single_text_view, null, false);
        labelMoreResultsFooter = (TextView) footerView.findViewById(R.id.label);
        labelMoreResultsFooter.setText(getResources().getString(R.string.labelMoreResults));
        labelMoreResultsFooter.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (contentType.equals(ContentType.POI)) {
                    // start poi profile update request
                    preparePOIRequest(getActivity(), POIManager.ACTION_MORE_RESULTS);
                }
            }
        });

        return view;
    }


    /**
     * pause and resume
     */

    @Override public void fragmentVisible() {
        // broadcast filter
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_LOCATION);
        filter.addAction(Constants.ACTION_HISTORY_POINT_PROFILE_SELECTED);
        filter.addAction(Constants.ACTION_POI_PROFILE_SELECTED);
        filter.addAction(Constants.ACTION_POI_PROFILE_MODIFIED);
        filter.addAction(Constants.ACTION_SHAKE_DETECTED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
        // start request
        if (contentType.equals(ContentType.POI)) {
            // reload poi profile
            preparePOIRequest(getActivity(), POIManager.ACTION_UPDATE);
        } else {
            // reload history point profile
            prepareHistoryPointRequest(getActivity());
        }
    }

    @Override public void fragmentInvisible() {
        if (contentType.equals(ContentType.POI)) {
            poiManagerInstance.invalidateRequest((POIFragment) this);
        } else {
            poiManagerInstance.invalidateHistoryPointProfileRequest((POIFragment) this);
        }
        progressHandler.removeCallbacks(progressUpdater);
        // unregister broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)) {
                NewLocationAttributes newLocationAttributes = NewLocationAttributes.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_LOCATION_ATTRIBUTES));
                if (newLocationAttributes != null
                        && newLocationAttributes.getImmediateDistanceThreshold().isAtLeast(DistanceThreshold.ONE_HUNDRED_METERS)) {
                    reload(context);
                }
            } else if (intent.getAction().equals(Constants.ACTION_HISTORY_POINT_PROFILE_SELECTED)) {
                if (listViewPOI != null) {
                    listViewPOI.setSelection(0);
                }
                reload(context);
            } else if (intent.getAction().equals(Constants.ACTION_POI_PROFILE_SELECTED)) {
                if (editSearch != null) {
                    editSearch.setText(
                            AccessDatabase.getInstance(context).getSearchTermOfPOIProfile(
                                SettingsManager.getInstance(context).getPOISettings().getSelectedPOIProfileId()));
                }
                if (listViewPOI != null) {
                    listViewPOI.setSelection(0);
                }
                reload(context);
            } else if (intent.getAction().equals(Constants.ACTION_POI_PROFILE_MODIFIED)) {
                reload(context);
            } else if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                vibrator.vibrate(250);
                reload(context);
            }
        }

        private void reload(Context context) {
            if (contentType.equals(ContentType.POI)) {
                preparePOIRequest(context, POIManager.ACTION_UPDATE);
            } else {
                prepareHistoryPointRequest(context);
            }
        }
    };

    private class ProgressUpdater implements Runnable {
        public void run() {
            vibrator.vibrate(50);
            progressHandler.postDelayed(this, 2000);
        }
    }


    /**
     * history point request
     */

    private void prepareHistoryPointRequest(Context context) {
        int selectedHistoryPointProfileId = SettingsManager.getInstance(context).getPOISettings().getSelectedHistoryPointProfileId();
        // hide keyboard
        editSearch.dismissDropDown();
        imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);

        // selected history point profile name
        if (HistoryPointProfile.getProfileMap(context).containsKey(selectedHistoryPointProfileId)) {
            String historyPointProfileName = HistoryPointProfile.getProfileMap(context).get(selectedHistoryPointProfileId);
            buttonSelectProfile.setText(
                    String.format(
                        context.getResources().getString(R.string.buttonSelectProfile),
                        historyPointProfileName)
                    );
            buttonSelectProfile.setContentDescription(
                    String.format(
                        context.getResources().getString(R.string.buttonSelectProfile),
                        historyPointProfileName)
                    );
        } else {
            buttonSelectProfile.setText(
                    context.getResources().getString(R.string.buttonSelectProfileNone));
            buttonSelectProfile.setContentDescription(
                    context.getResources().getString(R.string.buttonSelectProfileNone));
        }

        // clear search button
        if (! TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.GONE) {
            buttonClearSearch.setVisibility(View.VISIBLE);
        } else if (TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.VISIBLE) {
            buttonClearSearch.setVisibility(View.GONE);
        }

        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        if (! TextUtils.isEmpty(searchTerm)) {
            labelHeading.setText(
                    context.getResources().getQuantityString(R.plurals.result, 0, 0));
        } else {
            labelHeading.setText(
                    context.getResources().getQuantityString(R.plurals.poi, 0, 0));
        }
        buttonRefresh.setContentDescription(
                context.getResources().getString(R.string.buttonCancel));
        buttonRefresh.setImageResource(R.drawable.cancel);

        // list view
        listViewPOI.setAdapter(null);
        listViewPOI.setOnScrollListener(null);
        if (listViewPOI.getFooterViewsCount() > 0) {
            listViewPOI.removeFooterView(labelMoreResultsFooter);
        }
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setClickable(false);
        labelEmptyListView.setText(
                context.getResources().getString(R.string.messagePleaseWait));

        // start history point profile update request
        progressHandler.postDelayed(progressUpdater, 2000);
        poiManagerInstance.requestHistoryPointProfile(
                (POIFragment) this, selectedHistoryPointProfileId);
    }

    @Override public void historyPointProfileRequestFinished(Context context, int returnCode, HistoryPointProfile historyPointProfile, boolean resetListPosition) {
        POISettings poiSettings = SettingsManager.getInstance(context).getPOISettings();
        buttonRefresh.setContentDescription(
                context.getResources().getString(R.string.buttonRefresh));
        buttonRefresh.setImageResource(R.drawable.refresh);
        progressHandler.removeCallbacks(progressUpdater);

        if (returnCode == Constants.RC.OK
                && historyPointProfile != null
                && historyPointProfile.getPointProfileObjectList() != null) {

            // header and listview
            ViewCompat.setAccessibilityLiveRegion(
                    labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
            if (poiSettings.filterPointListByDirection()) {
                ArrayList<PointProfileObject> listOfVDFilteredPoints = filterPointProfileObjectListByViewingDirection(
                        historyPointProfile.getPointProfileObjectList());

                if (! TextUtils.isEmpty(searchTerm)) {
                    // [x] search   [x] viewing direction
                    ArrayList<PointProfileObject> listOfVDFilteredAndSearchedHistoryPoints = filterPointProfileObjectListBySearchTermAndSortByName(
                            listOfVDFilteredPoints, searchTerm);
                    labelHeading.setText(
                            String.format(
                                context.getResources().getString(R.string.labelSelectHistoryPointDialogHeaderSearchFiltered),
                                context.getResources().getQuantityString(
                                    R.plurals.result, listOfVDFilteredAndSearchedHistoryPoints.size(), listOfVDFilteredAndSearchedHistoryPoints.size()),
                                searchTerm,
                                StringUtility.formatProfileSortCriteria(
                                    context, Constants.SORT_CRITERIA.NAME_ASC))
                            );
                    listViewPOI.setAdapter(
                            new ArrayAdapter<PointProfileObject>(
                                context, android.R.layout.simple_list_item_1, listOfVDFilteredAndSearchedHistoryPoints));

                } else {
                    // [ ] search   [x] viewing direction
                    labelHeading.setText(
                            String.format(
                                context.getResources().getString(R.string.labelSelectHistoryPointDialogHeaderSuccessFiltered),
                                context.getResources().getQuantityString(
                                    R.plurals.poi, listOfVDFilteredPoints.size(), listOfVDFilteredPoints.size()),
                                StringUtility.formatProfileSortCriteria(
                                    context, historyPointProfile.getSortCriteria()))
                            );
                    listViewPOI.setAdapter(
                            new ArrayAdapter<PointProfileObject>(
                                context, android.R.layout.simple_list_item_1, listOfVDFilteredPoints));
                }

            } else {
                // take all poi
                ArrayList<PointProfileObject> listOfAllHistoryPoints = historyPointProfile.getPointProfileObjectList();

                if (! TextUtils.isEmpty(searchTerm)) {
                    // [x] search   [ ] viewing direction
                    ArrayList<PointProfileObject> listOfSearchedHistoryPoints = filterPointProfileObjectListBySearchTermAndSortByName(
                            listOfAllHistoryPoints, searchTerm);
                    labelHeading.setText(
                            String.format(
                                context.getResources().getString(R.string.labelSelectHistoryPointDialogHeaderSearch),
                                context.getResources().getQuantityString(
                                    R.plurals.result, listOfSearchedHistoryPoints.size(), listOfSearchedHistoryPoints.size()),
                                searchTerm,
                                StringUtility.formatProfileSortCriteria(
                                    context, Constants.SORT_CRITERIA.NAME_ASC))
                            );
                    listViewPOI.setAdapter(
                            new ArrayAdapter<PointProfileObject>(
                                context, android.R.layout.simple_list_item_1, listOfSearchedHistoryPoints));

                } else {
                    // [ ] search   [ ] viewing direction
                    labelHeading.setText(
                            String.format(
                                context.getResources().getString(R.string.labelSelectHistoryPointDialogHeaderSuccess),
                                context.getResources().getQuantityString(
                                    R.plurals.poi, listOfAllHistoryPoints.size(), listOfAllHistoryPoints.size()),
                                StringUtility.formatProfileSortCriteria(
                                    context, historyPointProfile.getSortCriteria()))
                            );
                    listViewPOI.setAdapter(
                            new ArrayAdapter<PointProfileObject>(
                                context, android.R.layout.simple_list_item_1, listOfAllHistoryPoints));
                }
            }
            labelEmptyListView.setText("");

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
            ViewCompat.setAccessibilityLiveRegion(
                    labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
            labelEmptyListView.setText(
                    ServerUtility.getErrorMessageForReturnCode(context, returnCode));
        }
    }


    public static class SelectHistoryPointProfileDialog extends DialogFragment {

        // Store instance variables
        private SettingsManager settingsManagerInstance;
        private Integer[] historyPointProfileIdArray;
        private String[] historyPointProfileNameArray;

        public static SelectHistoryPointProfileDialog newInstance() {
            SelectHistoryPointProfileDialog selectHistoryPointProfileDialogInstance = new SelectHistoryPointProfileDialog();
            return selectHistoryPointProfileDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            historyPointProfileIdArray = new Integer[HistoryPointProfile.getProfileMap(getActivity()).size()];
            historyPointProfileNameArray = new String[HistoryPointProfile.getProfileMap(getActivity()).size()];
            int indexOfSelectedHistoryPointProfile = -1;
            int index = 0;
            for (Map.Entry<Integer,String> profile : HistoryPointProfile.getProfileMap(getActivity()).entrySet()) {
                historyPointProfileIdArray[index] = profile.getKey();
                historyPointProfileNameArray[index] = profile.getValue();
                // selected profile
                if (historyPointProfileIdArray[index] == settingsManagerInstance.getPOISettings().getSelectedHistoryPointProfileId()) {
                    indexOfSelectedHistoryPointProfile = index;
                }
                index += 1;
            }

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectHistoryPointProfileDialogTitle))
                .setSingleChoiceItems(
                        historyPointProfileNameArray,
                        indexOfSelectedHistoryPointProfile,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    settingsManagerInstance
                                        .getPOISettings()
                                        .setSelectedHistoryPointProfileId(historyPointProfileIdArray[which]);
                                } catch (ArrayIndexOutOfBoundsException e) {}
                                Intent intent = new Intent(Constants.ACTION_HISTORY_POINT_PROFILE_SELECTED);
                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
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


    /**
     * poi request
     */

    private void preparePOIRequest(Context context, int requestAction) {
        int selectedPOIProfileId = SettingsManager.getInstance(context).getPOISettings().getSelectedPOIProfileId();
        // hide keyboard
        editSearch.dismissDropDown();
        imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);

        // selected poi profile name
        if (AccessDatabase.getInstance(context).getPOIProfileMap().containsKey(selectedPOIProfileId)) {
            buttonSelectProfile.setText(
                    String.format(
                        context.getResources().getString(R.string.buttonSelectProfile),
                        AccessDatabase.getInstance(context).getPOIProfileMap().get(selectedPOIProfileId))
                    );
            // content description with poi categories
            ArrayList<String> categoryNameList = new ArrayList<String>();
            for (POICategory poiCategory : AccessDatabase.getInstance(context).getCategoryListOfPOIProfile(selectedPOIProfileId)) {
                categoryNameList.add(poiCategory.getName());
            }
            String categoryNameString = context.getResources().getString(R.string.poiCategoryNoneSelected);
            if (! categoryNameList.isEmpty()) {
                categoryNameString = TextUtils.join(", ", categoryNameList);
            }
            buttonSelectProfile.setContentDescription(
                    String.format(
                        context.getResources().getString(R.string.buttonSelectProfileCD),
                        AccessDatabase.getInstance(context).getPOIProfileMap().get(selectedPOIProfileId),
                        categoryNameString)
                    );
        } else {
            buttonSelectProfile.setText(
                    context.getResources().getString(R.string.buttonSelectProfileNone));
            buttonSelectProfile.setContentDescription(
                    context.getResources().getString(R.string.buttonSelectProfileNone));
        }

        // clear search button
        if (! TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.GONE) {
            buttonClearSearch.setVisibility(View.VISIBLE);
        } else if (TextUtils.isEmpty(searchTerm) && buttonClearSearch.getVisibility() == View.VISIBLE) {
            buttonClearSearch.setVisibility(View.GONE);
        }

        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        if (! TextUtils.isEmpty(searchTerm)) {
            labelHeading.setText(
                    context.getResources().getQuantityString(R.plurals.result, 0, 0));
        } else {
            labelHeading.setText(
                    context.getResources().getQuantityString(R.plurals.poi, 0, 0));
        }
        buttonRefresh.setContentDescription(
                context.getResources().getString(R.string.buttonCancel));
        buttonRefresh.setImageResource(R.drawable.cancel);

        // list view
        listViewPOI.setAdapter(null);
        listViewPOI.setOnScrollListener(null);
        if (listViewPOI.getFooterViewsCount() > 0) {
            listViewPOI.removeFooterView(labelMoreResultsFooter);
        }
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setClickable(false);
        labelEmptyListView.setText(
                context.getResources().getString(R.string.messagePleaseWait));

        // start poi profile update request
        progressHandler.postDelayed(progressUpdater, 2000);
        poiManagerInstance.requestPOIProfile(
                (POIFragment) this, selectedPOIProfileId, requestAction);
    }

	@Override public void poiProfileRequestFinished(Context context, int returnCode, POIProfile poiProfile, boolean resetListPosition) {
        POISettings poiSettings = settingsManagerInstance.getPOISettings();
        buttonRefresh.setContentDescription(
                context.getResources().getString(R.string.buttonRefresh));
        buttonRefresh.setImageResource(R.drawable.refresh);
        progressHandler.removeCallbacks(progressUpdater);

        if (returnCode == Constants.RC.OK
                && poiProfile != null
                && poiProfile.getPointProfileObjectList() != null) {

            // fill listview
            ViewCompat.setAccessibilityLiveRegion(
                    labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
            if (poiSettings.filterPointListByDirection()) {
                // filter by viewing direction
                ArrayList<PointProfileObject> listOfFilteredPOI = filterPointProfileObjectListByViewingDirection(
                        poiProfile.getPointProfileObjectList());
                // header label
                if (! TextUtils.isEmpty(poiProfile.getSearchTerm())) {
                    labelHeading.setText(
                            String.format(
                                context.getResources().getString(R.string.labelPOIFragmentHeaderSearchFiltered),
                                context.getResources().getQuantityString(
                                    R.plurals.result, listOfFilteredPOI.size(), listOfFilteredPOI.size()),
                                poiProfile.getSearchTerm(),
                                context.getResources().getQuantityString(
                                    R.plurals.meter, poiProfile.getLookupRadius(), poiProfile.getLookupRadius()))
                            );
                } else {
                    labelHeading.setText(
                            String.format(
                                context.getResources().getString(R.string.labelPOIFragmentHeaderSuccessFiltered),
                                context.getResources().getQuantityString(
                                    R.plurals.poi, listOfFilteredPOI.size(), listOfFilteredPOI.size()),
                                context.getResources().getQuantityString(
                                    R.plurals.meter, poiProfile.getLookupRadius(), poiProfile.getLookupRadius()))
                            );
                }
                // fill adapter
                listViewPOI.setAdapter(
                        new POIProfilePointAdapter(
                            context,
                            listOfFilteredPOI)
                        );

            } else {
                // take all poi
                ArrayList<PointProfileObject> listOfAllPOI = poiProfile.getPointProfileObjectList();
                // header field
                if (! TextUtils.isEmpty(poiProfile.getSearchTerm())) {
                    labelHeading.setText(
                            String.format(
                                context.getResources().getString(R.string.labelPOIFragmentHeaderSearch),
                                context.getResources().getQuantityString(
                                    R.plurals.result, listOfAllPOI.size(), listOfAllPOI.size()),
                                poiProfile.getSearchTerm(),
                                context.getResources().getQuantityString(
                                    R.plurals.meter, poiProfile.getLookupRadius(), poiProfile.getLookupRadius()))
                            );
                } else {
                    labelHeading.setText(
                            String.format(
                                context.getResources().getString(R.string.labelPOIFragmentHeaderSuccess),
                                context.getResources().getQuantityString(
                                    R.plurals.poi, listOfAllPOI.size(), listOfAllPOI.size()),
                                context.getResources().getQuantityString(
                                    R.plurals.meter, poiProfile.getLookupRadius(), poiProfile.getLookupRadius()))
                            );
                }
                listViewPOI.setAdapter(
                        new POIProfilePointAdapter(
                            context,
                            listOfAllPOI)
                        );
            }
            labelEmptyListView.setText("");

            // more results
            if (listViewPOI.getAdapter().getCount() == 0) {
                if (poiProfile.getRadius() < poiProfile.getMaximalRadius()
                        && poiProfile.getNumberOfResults() < poiProfile.getMaximalNumberOfResults()) {
                    labelEmptyListView.setClickable(true);
                    labelEmptyListView.setText(
                            context.getResources().getString(R.string.labelMoreResults));
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
            ViewCompat.setAccessibilityLiveRegion(
                    labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
            labelEmptyListView.setText(
                    ServerUtility.getErrorMessageForReturnCode(context, returnCode));
            // show select map dialog
            if (isAdded()
                    && (
                           returnCode == Constants.RC.MAP_LOADING_FAILED
                        || returnCode == Constants.RC.WRONG_MAP_SELECTED)
                    ) {
                SelectMapDialog.newInstance()
                    .show(getActivity().getSupportFragmentManager(), "SelectMapDialog");
            }
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
            filter.addAction(Constants.ACTION_POI_PROFILE_CREATED);
            filter.addAction(Constants.ACTION_POI_PROFILE_REMOVED);
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
                    Intent intent = new Intent(Constants.ACTION_POI_PROFILE_SELECTED);
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
                if (intent.getAction().equals(Constants.ACTION_POI_PROFILE_CREATED)
                        || intent.getAction().equals(Constants.ACTION_POI_PROFILE_REMOVED)) {
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


    /**
     * history point and poi profile
     */

    private ArrayList<PointProfileObject> filterPointProfileObjectListByViewingDirection(
            ArrayList<PointProfileObject> listOfAllPoints) {
        // only include, what's ahead
        ArrayList<PointProfileObject> listOfFilteredPoints = new ArrayList<PointProfileObject>();
        for (PointProfileObject pointProfileObject : listOfAllPoints) {
            Integer bearing = pointProfileObject.bearingFromCenter();
            if (bearing != null
                    && (bearing < 60 || bearing > 300)) {
                listOfFilteredPoints.add(pointProfileObject);
            }
        }
        return listOfFilteredPoints;
    }

    private ArrayList<PointProfileObject> filterPointProfileObjectListBySearchTermAndSortByName(
            ArrayList<PointProfileObject> listOfAllPoints, String searchTerm) {
        // filter point list by search term
        ArrayList<PointProfileObject> listOfFilteredPoints = new ArrayList<PointProfileObject>();
        for (PointProfileObject pointProfileObject : listOfAllPoints) {
            boolean match = true;
            for (String word : searchTerm.split("\\s")) {
                if (! pointProfileObject.toString().toLowerCase().contains(word.toLowerCase())) {
                    match = false;
                    break;
                }
            }
            if (match) {
                listOfFilteredPoints.add(pointProfileObject);
            }
        }
        // sort by name, ascending and return
        Collections.sort(
                listOfFilteredPoints, new SortByNameASC());
        return listOfFilteredPoints;
    }

}
