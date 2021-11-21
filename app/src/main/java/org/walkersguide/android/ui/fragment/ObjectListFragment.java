package org.walkersguide.android.ui.fragment;

import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
import org.walkersguide.android.database.profiles.DatabaseSegmentProfile;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.SortMethod;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.server.poi.PoiCategory;
import org.walkersguide.android.server.poi.PoiProfile;
import org.walkersguide.android.server.poi.PoiProfileManager;
import org.walkersguide.android.server.poi.PoiProfileRequest;
import org.walkersguide.android.server.poi.PoiProfileResult;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import org.walkersguide.android.data.ObjectWithId;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;
    import org.walkersguide.android.ui.view.TextViewAndActionButton.LabelTextConfig;
import android.view.MenuItem;
import timber.log.Timber;

import android.os.Bundle;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import android.widget.ImageButton;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import android.widget.Button;
import android.widget.AutoCompleteTextView;
import android.app.Dialog;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.text.TextUtils;
import org.walkersguide.android.ui.listener.TextChangedListener;
import android.text.Editable;
import org.walkersguide.android.util.SettingsManager;
import androidx.fragment.app.DialogFragment;
import android.os.Vibrator;
import java.util.ArrayList;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import org.walkersguide.android.util.Constants;
import androidx.core.view.ViewCompat;
import android.widget.AbsListView;
import android.content.Intent;
import org.walkersguide.android.data.sensor.attribute.NewLocationAttributes;
import org.walkersguide.android.data.sensor.threshold.DistanceThreshold;
import android.widget.BaseAdapter;
import java.util.Comparator;
import android.app.Activity;
import java.util.Collections;
import android.view.WindowManager;
import android.annotation.TargetApi;
import android.os.Build;


public abstract class ObjectListFragment extends DialogFragment {
    public static final String REQUEST_SELECT_OBJECT = "selectObject";
    public static final String EXTRA_OBJECT_WITH_ID = "objectWithId";


    public abstract void clickedButtonSelectProfile();
    public abstract void longClickedButtonSelectProfile();
    public abstract void clickedLabelEmptyListView();
    public abstract void searchTermChanged(String newSearchTerm);
    public abstract String getDialogTitle();
    public abstract void requestUiUpdate();


    // dialog
    private static final String KEY_IS_DIALOG = "isDialog";
    private static final String KEY_SHOW_SELECT_PROFILE_BUTTON = "showSelectProfileButton";
    private static final String KEY_SHOW_SEARCH_FIELD = "showSearchField";

    protected Vibrator vibrator;
    private boolean isDialog;
    private int listPosition;

	// ui components
    // top layout
    protected Button buttonSelectProfile;
    protected AutoCompleteTextView editSearch;
    protected ImageButton buttonClearSearch;
    // main layout
    protected TextView labelHeading;
    protected ImageButton buttonRefresh;
	protected ListView listViewObject;
    protected TextView labelEmptyListView;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        isDialog = getArguments().getBoolean(KEY_IS_DIALOG, false);
        setShowsDialog(isDialog);
        vibrator = (Vibrator) GlobalInstance.getContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static Bundle createArgsBundle(boolean isDialog, boolean showSelectProfileButton, boolean showSearchField) {
        Bundle args = new Bundle();
        args.putBoolean(KEY_IS_DIALOG, isDialog);
        args.putBoolean(KEY_SHOW_SELECT_PROFILE_BUTTON, showSelectProfileButton);
        args.putBoolean(KEY_SHOW_SEARCH_FIELD, showSearchField);
        return args;
    }


    /**
     * menu
     */

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemJumpToTop:
                listViewObject.setSelection(0);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void processSelectedMenuItem(MenuItem selectedMenuItem) {
    }


    /**
     * create view
     */

    @Override public int getTheme() {
        return R.style.FullScreenDialog;
    }

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        if (getDialog() != null) {
            // fragment is a dialog
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            // fragment is embetted
		    return configureView(
                   inflater.inflate(R.layout.fragment_object_list, container, false),
                   savedInstanceState);
        }
	}

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = configureView(
                inflater.inflate(R.layout.fragment_object_list, nullParent),
                savedInstanceState);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getDialogTitle())
            .setView(view)
            .setNegativeButton(
                    getResources().getString(R.string.dialogClose),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
            .create();
    }

	public View configureView(View view, Bundle savedInstanceState) {
        // top layout
        buttonSelectProfile = (Button) view.findViewById(R.id.buttonSelectProfile);
        buttonSelectProfile.setVisibility(
                getArguments().getBoolean(KEY_SHOW_SELECT_PROFILE_BUTTON, true) ? View.VISIBLE : View.GONE);
        buttonSelectProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                clickedButtonSelectProfile();
            }
        });
        buttonSelectProfile.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                longClickedButtonSelectProfile();
                return true;
            }
        });

        editSearch = (AutoCompleteTextView) view.findViewById(R.id.editSearch);
        editSearch.setVisibility(
                getArguments().getBoolean(KEY_SHOW_SEARCH_FIELD, true) ? View.VISIBLE : View.GONE);
        editSearch.setHint(GlobalInstance.getStringResource(R.string.dialogSearch));
        editSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    listPosition = 0;
                    requestUiUpdate();
                    return true;
                }
                return false;
            }
        });
        editSearch.addTextChangedListener(new TextChangedListener<AutoCompleteTextView>(editSearch) {
            @Override public void onTextChanged(AutoCompleteTextView view, Editable s) {
                String newSearchTerm = view.getText().toString().trim();
                if (! TextUtils.isEmpty(newSearchTerm)) {
                    searchTermChanged(newSearchTerm);
                } else {
                    searchTermChanged(null);
                }
                showOrHideSearchFieldControls();
            }
        });
        // add auto complete suggestions
        editSearch.setAdapter(
                new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_dropdown_item_1line,
                    SettingsManager.getInstance().getSearchTermHistory()));

        buttonClearSearch = (ImageButton) view.findViewById(R.id.buttonClearSearch);
        buttonClearSearch.setContentDescription(GlobalInstance.getStringResource(R.string.buttonClearSearch));
        buttonClearSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // clear edit text
                searchTermChanged(null);
                listPosition = 0;
                requestUiUpdate();
            }
        });

        // content layout
        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                requestUiUpdate();
            }
        });

        listViewObject = (ListView) view.findViewById(R.id.listView);
        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                clickedLabelEmptyListView();
            }
        });
        listViewObject.setEmptyView(labelEmptyListView);

        // don't show keyboard automatically
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        return view;
    }

    private void hideKeyboard() {
        InputMethodManager imm =(InputMethodManager) GlobalInstance.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
    }

    private void showOrHideSearchFieldControls() {
        if (! TextUtils.isEmpty(editSearch.getText()) && buttonClearSearch.getVisibility() == View.GONE) {
            buttonClearSearch.setVisibility(View.VISIBLE);
        } else if (TextUtils.isEmpty(editSearch.getText()) && buttonClearSearch.getVisibility() == View.VISIBLE) {
            buttonClearSearch.setVisibility(View.GONE);
        }
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        Timber.d("onPause");
        hideKeyboard();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        Timber.d("onResume");
        // broadcast filter
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_LOCATION);
        filter.addAction(Constants.ACTION_SHAKE_DETECTED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
    }

    public void prepareRequest() {
        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelHeading.setText(
                GlobalInstance.getPluralResource(R.plurals.point, 0));
        buttonRefresh.setContentDescription(
                GlobalInstance.getStringResource(R.string.buttonCancel));
        buttonRefresh.setImageResource(R.drawable.cancel);

        // search field
        hideKeyboard();
        showOrHideSearchFieldControls();
        SettingsManager.getInstance().addToSearchTermHistory(
                editSearch.getText().toString().trim());

        // list view
        listViewObject.setAdapter(null);
        listViewObject.setOnScrollListener(null);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setClickable(false);
        labelEmptyListView.setText(
                GlobalInstance.getStringResource(R.string.messagePleaseWait));
    }

    public void resetListPosition() {
        listPosition = 0;
    }

    public void updateRefreshButtonAfterRequestWasFinished() {
        buttonRefresh.setContentDescription(
                GlobalInstance.getStringResource(R.string.buttonRefresh));
        buttonRefresh.setImageResource(R.drawable.refresh);
    }

    public void updateListViewAfterRequestWasSuccessful(ArrayList<? extends ObjectWithId> objectList) {
        listViewObject.setAdapter(
                new ObjectWithIdAdapter(
                    ObjectListFragment.this.getContext(), objectList));
        labelEmptyListView.setText("");

        // list position
        listViewObject.setSelection(listPosition);
        listViewObject.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });
    }


    /**
     * broadcasts
     */

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)) {
                NewLocationAttributes newLocationAttributes = NewLocationAttributes.fromString(
                        intent.getStringExtra(Constants.ACTION_NEW_LOCATION_ATTRIBUTES));
                if (newLocationAttributes != null
                        && newLocationAttributes.getImmediateDistanceThreshold().isAtLeast(DistanceThreshold.ONE_HUNDRED_METERS)) {
                    requestUiUpdate();
                }
            } else if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                vibrator.vibrate(250);
                requestUiUpdate();
            }
        }
    };


    /**
     * ObjectWithId adapter
     */

    public class ObjectWithIdAdapter extends BaseAdapter {

        private Context context;
        private ArrayList<? extends ObjectWithId> objectList;
        private boolean showIsFavoriteIndicator;

        public ObjectWithIdAdapter(Context context, ArrayList<? extends ObjectWithId> objectList) {
            this.context = context;
            this.objectList = objectList;
            this.showIsFavoriteIndicator = false;
            for (ObjectWithId object : objectList) {
                if (! object.isFavorite()) {
                    this.showIsFavoriteIndicator = true;
                    break;
                }
            }
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            TextViewAndActionButton layoutTextViewAndActionButton = null;
            if (convertView == null) {
                layoutTextViewAndActionButton = new TextViewAndActionButton(this.context);
                LayoutParams lp = new LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutTextViewAndActionButton.setLayoutParams(lp);
                if (isDialog) {
                    layoutTextViewAndActionButton.setOnLabelClickListener(new TextViewAndActionButton.OnLabelClickListener() {
                        @Override public void onLabelClick(TextViewAndActionButton view) {
                            Bundle result = new Bundle();
                            result.putSerializable(EXTRA_OBJECT_WITH_ID, view.getObject());
                            getParentFragmentManager().setFragmentResult(REQUEST_SELECT_OBJECT, result);
                            dismiss();
                        }
                    });
                }
                Timber.d("new");
            } else {
                layoutTextViewAndActionButton = (TextViewAndActionButton) convertView;
                Timber.d("cache");
            }
            layoutTextViewAndActionButton.configureView(
                    getItem(position), LabelTextConfig.empty(false), showIsFavoriteIndicator, false);
            return layoutTextViewAndActionButton;
        }

        @Override public int getCount() {
            if (this.objectList != null) {
                return this.objectList.size();
            }
            return 0;
        }

        @Override public ObjectWithId getItem(int position) {
            if (this.objectList != null) {
                return this.objectList.get(position);
            }
            return null;
        }

        @Override public long getItemId(int position) {
            return position;
        }

        public void sortObjectList(Comparator<ObjectWithId> comparator) {
            Collections.sort(objectList, comparator);
            notifyDataSetChanged();
        }


        private class EntryHolder {
            public TextViewAndActionButton layoutTextViewAndActionButton;
        }
    }

}
