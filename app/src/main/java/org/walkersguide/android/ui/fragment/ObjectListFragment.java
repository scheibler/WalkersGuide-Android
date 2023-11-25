package org.walkersguide.android.ui.fragment;

import org.walkersguide.android.ui.adapter.ObjectWithIdAdapter;
import org.walkersguide.android.ui.interfaces.ViewChangedListener;
import org.walkersguide.android.sensor.bearing.AcceptNewBearing;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.walkersguide.android.server.wg.status.OSMMap;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;

import org.walkersguide.android.data.ObjectWithId;
    import org.walkersguide.android.ui.view.ObjectWithIdView;
    import org.walkersguide.android.ui.view.ObjectWithIdView.OnDefaultObjectActionListener;
import android.view.MenuItem;
import timber.log.Timber;

import android.os.Bundle;


import android.view.LayoutInflater;
import android.view.View;

import android.widget.ListView;
import android.widget.TextView;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import java.util.ArrayList;
import android.content.BroadcastReceiver;
import android.content.Context;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import androidx.core.view.ViewCompat;
import android.widget.AbsListView;
import android.content.Intent;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.WgException;
import org.walkersguide.android.server.ServerException;
import org.walkersguide.android.ui.dialog.select.SelectMapDialog;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.Helper;
import android.view.Menu;
import android.view.MenuInflater;
import org.walkersguide.android.sensor.position.AcceptNewPosition;
import org.walkersguide.android.data.object_with_id.Point;
import android.text.TextUtils;
import org.walkersguide.android.data.angle.Bearing;
import android.widget.HeaderViewListAdapter;
import org.walkersguide.android.ui.fragment.RootFragment;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import org.walkersguide.android.data.Profile;
import android.widget.ImageButton;


public abstract class ObjectListFragment extends RootFragment
        implements FragmentResultListener, MenuProvider, OnRefreshListener,
                   ObjectWithIdView.OnDefaultObjectActionListener, ViewChangedListener {
    public static final String REQUEST_SELECT_OBJECT = "selectObject";
    public static final String EXTRA_OBJECT_WITH_ID = "objectWithId";

    public abstract Profile getProfile();
    public abstract int getPluralResourceId();
    public abstract boolean isUiUpdateRequestInProgress();
    public abstract void requestUiUpdate();
    public abstract void requestMoreResults();


    public static class BundleBuilder extends RootFragment.BundleBuilder {
        public BundleBuilder() {
            super();
            setSelectObjectWithId(false);
            setAutoUpdate(false);
            setViewingDirectionFilter(false);
        }

        public BundleBuilder setSelectObjectWithId(boolean newState) {
            bundle.putBoolean(KEY_SELECT_OBJECT_WITH_ID, newState);
            return this;
        }
        public BundleBuilder setAutoUpdate(boolean newState) {
            bundle.putBoolean(KEY_AUTO_UPDATE, newState);
            return this;
        }
        public BundleBuilder setViewingDirectionFilter(boolean newState) {
            bundle.putBoolean(KEY_VIEWING_DIRECTION_FILTER, newState);
            return this;
        }

        public Bundle build() {
            return bundle;
        }
    }


    // dialog
    private static final String KEY_SELECT_OBJECT_WITH_ID = "selectObjectWithId";
    private static final String KEY_AUTO_UPDATE = "autoUpdate";
    private static final String KEY_VIEWING_DIRECTION_FILTER = "viewingDirectionFilter";
    private static final String KEY_LIST_POSITION = "listPosition";

    private boolean selectObjectWithId, autoUpdate, viewingDirectionFilter;
    private int listPosition;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        selectObjectWithId = getArguments().getBoolean(KEY_SELECT_OBJECT_WITH_ID);

        if (savedInstanceState != null) {
            autoUpdate = savedInstanceState.getBoolean(KEY_AUTO_UPDATE);
            viewingDirectionFilter = savedInstanceState.getBoolean(KEY_VIEWING_DIRECTION_FILTER);
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            autoUpdate = getArguments().getBoolean(KEY_AUTO_UPDATE);
            viewingDirectionFilter = getArguments().getBoolean(KEY_VIEWING_DIRECTION_FILTER);
            listPosition = 0;
        }

        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectMapDialog.REQUEST_SELECT_MAP, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        Timber.d("onFragmentResult: %1$s", requestKey);
        if (requestKey.equals(SelectMapDialog.REQUEST_SELECT_MAP)) {
            SettingsManager.getInstance().setSelectedMap(
                    (OSMMap) bundle.getSerializable(SelectMapDialog.EXTRA_MAP));
            resetListPosition();
            requestUiUpdate();
        }
    }

    @Override public void onDefaultObjectActionClicked(ObjectWithId objectWithId) {
        Bundle result = new Bundle();
        result.putSerializable(EXTRA_OBJECT_WITH_ID, objectWithId);
        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_OBJECT, result);
        dismiss();
    }


    /**
     * create view
     */

    private TextView labelHeading, labelEmptyListView, labelMoreResultsFooter;
    private SwipeRefreshLayout swipeRefreshListView, swipeRefreshEmptyTextView;
	private ListView listViewObject;

    @Override public String getDialogButtonText() {
        return getResources().getString(
                selectObjectWithId ? R.string.dialogCancel : R.string.dialogClose);
    }

    @Override public int getLayoutResourceId() {
        return R.layout.fragment_object_list;
    }

	@Override public View configureView(View view, Bundle savedInstanceState) {
        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        ImageButton buttonAddObjectWithId = (ImageButton) view.findViewById(R.id.buttonAdd);
        buttonAddObjectWithId.setVisibility(
                isAddButtonVisible() ? View.VISIBLE : View.GONE);
        buttonAddObjectWithId.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                addObjectWithIdButtonClicked(view);
            }
        });

        swipeRefreshListView = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshListView);
        swipeRefreshListView.setOnRefreshListener(this);
        listViewObject = (ListView) view.findViewById(R.id.listView);

        swipeRefreshEmptyTextView = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshEmptyTextView);
        swipeRefreshEmptyTextView.setOnRefreshListener(this);
        listViewObject.setEmptyView(swipeRefreshEmptyTextView);

        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                requestMoreResults();
            }
        });

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View footerView = inflater.inflate(R.layout.layout_single_text_view, listViewObject, false);
        labelMoreResultsFooter = (TextView) footerView.findViewById(R.id.label);
        labelMoreResultsFooter.setText(getResources().getString(R.string.labelMoreResults));
        labelMoreResultsFooter.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                requestMoreResults();
            }
        });

        return view;
    }

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(KEY_AUTO_UPDATE,  autoUpdate);
        savedInstanceState.putBoolean(KEY_VIEWING_DIRECTION_FILTER,  viewingDirectionFilter);
        savedInstanceState.putInt(KEY_LIST_POSITION,  listPosition);
    }

    public boolean getAutoUpdate() {
        return this.autoUpdate;
    }

    public boolean isAddButtonVisible() {
        return false;
    }

    public void addObjectWithIdButtonClicked(View view) {
    }

    public boolean getSelectObjectWithId() {
        return this.selectObjectWithId;
    }

    public ObjectWithIdAdapter getListAdapter() {
        if (listViewObject.getAdapter() != null) {
            if (listViewObject.getAdapter() instanceof HeaderViewListAdapter) {
                return (ObjectWithIdAdapter) ((HeaderViewListAdapter) listViewObject.getAdapter()).getWrappedAdapter();
            }
            return (ObjectWithIdAdapter) listViewObject.getAdapter();
        }
        return null;
    }

    public int getListPosition() {
        return this.listPosition;
    }

    public void resetListPosition() {
        listPosition = 0;
    }

    public String getEmptyObjectListMessage() {
        return null;
    }


    /*
     * swipe to refresh
     */

    @Override public void onRefresh() {
        Timber.d("swipe");
        swipeToRefreshDetected();
    }

    public void swipeToRefreshDetected() {
        Helper.vibrateOnce(
                Helper.VIBRATION_DURATION_SHORT, Helper.VIBRATION_INTENSITY_WEAK);
        requestUiUpdate();
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_object_list_fragment, menu);
    }

    @Override public void onPrepareMenu(@NonNull Menu menu) {
        // refresh
        MenuItem menuItemRefresh = menu.findItem(R.id.menuItemRefresh);
        menuItemRefresh.setTitle(
                isUiUpdateRequestInProgress()
                ? getResources().getString(R.string.menuItemCancel)
                : getResources().getString(R.string.menuItemRefresh));

        // checkboxes
        // list auto update
        MenuItem menuItemAutoUpdate = menu.findItem(R.id.menuItemAutoUpdate);
        menuItemAutoUpdate.setChecked(autoUpdate);
        // viewing direction filter
        MenuItem menuItemFilterResult = menu.findItem(R.id.menuItemFilterResult);
        menuItemFilterResult.setChecked(viewingDirectionFilter);
    }

    @Override public boolean onMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemRefresh) {
            refreshMenuItemClicked();
        } else if (item.getItemId() == R.id.menuItemAutoUpdate) {
            autoUpdate = ! autoUpdate;
            resetListPosition();
            requestUiUpdate();
        } else if (item.getItemId() == R.id.menuItemFilterResult) {
            viewingDirectionFilter = ! viewingDirectionFilter;
            resetListPosition();
            requestUiUpdate();
        } else if (item.getItemId() == R.id.menuItemJumpToTop) {
            listViewObject.setSelection(0);
        } else {
            return false;
        }
        return true;
    }

    public void refreshMenuItemClicked() {
        requestUiUpdate();
    }


    /**
     * pause and resume
     */
    private static final String SELECT_MAP_DIALOG_TAG = "SelectMapDialog";

    @Override public void onPause() {
        super.onPause();
        Timber.d("onPause");
        unregisterViewChangedBroadcastReceiver(viewChangedBroadcastReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        Timber.d("onResume");
        // broadcast filter
        IntentFilter filter = new IntentFilter();
        filter.addAction(PositionManager.ACTION_NEW_LOCATION);
        filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
        filter.addAction(DeviceSensorManager.ACTION_SHAKE_DETECTED);
        filter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        LocalBroadcastManager
            .getInstance(getActivity())
            .registerReceiver(mMessageReceiver, filter);
        // request ui update
        registerViewChangedBroadcastReceiver(viewChangedBroadcastReceiver);
        requestUiUpdate();
    }


    private BroadcastReceiver viewChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ViewChangedListener.ACTION_OBJECT_WITH_ID_LIST_CHANGED)) {
                requestUiUpdate();
            }
        }
    };


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        private AcceptNewPosition acceptNewPosition = AcceptNewPosition.newInstanceForObjectListUpdate();
        private AcceptNewBearing acceptNewBearing = AcceptNewBearing.newInstanceForObjectListUpdate();

        @Override public void onReceive(Context context, Intent intent) {
            if (getDialog() == null && ! getActivity().hasWindowFocus()) {
                if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)
                        && intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION) != null
                        && intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false)) {
                    requestUiUpdate();
                }
                return;
            }

            if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                Point currentLocation = (Point) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION);
                if (currentLocation == null) {
                    return;
                }
                if (intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false)) {
                    Timber.d("update cause of important");
                    requestUiUpdate();
                } else if (autoUpdate
                        && acceptNewPosition.updatePoint(currentLocation)) {
                    Timber.d("update cause of new position");
                    requestUiUpdate();
                    Helper.vibrateOnce(
                            Helper.VIBRATION_DURATION_SHORT, Helper.VIBRATION_INTENSITY_WEAK);
                }

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                ObjectWithIdAdapter listAdapter = getListAdapter();
                Bearing currentBearing = (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (listAdapter == null || currentBearing == null) {
                    return;
                }
                if (autoUpdate && viewingDirectionFilter
                        && acceptNewBearing.updateBearing(currentBearing)) {
                    Timber.d("notifyDataSetChanged viewingDirectionFilter");
                    listAdapter.notifyDataSetChanged();
                    updateHeadingListView();
                }

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_SHAKE_DETECTED)) {
                Helper.vibrateOnce(Helper.VIBRATION_DURATION_LONG);
                requestUiUpdate();

            } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                ServerException serverException = (ServerException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                if (serverException instanceof WgException
                        && ((WgException) serverException).showMapDialog()
                        && getChildFragmentManager().findFragmentByTag(SELECT_MAP_DIALOG_TAG) == null) {
                    SelectMapDialog.newInstance(
                            SettingsManager.getInstance().getSelectedMap())
                        .show(getChildFragmentManager(), SELECT_MAP_DIALOG_TAG);
                }
            }
        }
    };


    /**
     * responses
     */

    public void prepareRequest() {
        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelHeading.setTag(null);
        labelHeading.setText(
                GlobalInstance.getPluralResource(getPluralResourceId(), 0));

        // list view
        swipeRefreshListView.setRefreshing(true);
        listViewObject.setAdapter(null);
        listViewObject.setOnScrollListener(null);
        if (listViewObject.getFooterViewsCount() > 0) {
            listViewObject.removeFooterView(labelMoreResultsFooter);
        }

        swipeRefreshEmptyTextView.setRefreshing(true);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setClickable(false);
        labelEmptyListView.setText(
                GlobalInstance.getStringResource(R.string.messagePleaseWait));
    }

    public void populateUiAfterRequestWasSuccessful(String headingSecondLine, ObjectWithIdAdapter listAdapter) {
        labelHeading.setTag(headingSecondLine);
        swipeRefreshListView.setRefreshing(false);
        swipeRefreshEmptyTextView.setRefreshing(false);

        // fill list view
        listViewObject.setAdapter(listAdapter);
        labelEmptyListView.setText(getEmptyObjectListMessage());

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

        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        updateHeadingListView();
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
    }

    public void populateUiAfterRequestWasSuccessful(String headingSecondLine,
            ArrayList<? extends ObjectWithId> objectList) {
        populateUiAfterRequestWasSuccessful(
                headingSecondLine,
                new ObjectWithIdAdapter(
                    ObjectListFragment.this.getContext(), objectList,
                    selectObjectWithId ? this : null,
                    getProfile(), autoUpdate, viewingDirectionFilter));
    }

    public void populateUiAndShowMoreResultsFooterAfterRequestWasSuccessful(
            String headingSecondLine, ArrayList<? extends ObjectWithId> objectList) {
        // only poi profiles call this function for now
        populateUiAfterRequestWasSuccessful(headingSecondLine, objectList);

        // more results
        if (getListAdapter().isEmpty()) {
            labelEmptyListView.setClickable(true);
            labelEmptyListView.setText(
                    GlobalInstance.getStringResource(R.string.labelMoreResults));
        } else {
            listViewObject.addFooterView(labelMoreResultsFooter, null, true);
        }
    }

    public void populateUiAfterRequestFailed(String message) {
        swipeRefreshListView.setRefreshing(false);
        swipeRefreshEmptyTextView.setRefreshing(false);

        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        labelEmptyListView.setText(message);
    }

    private void updateHeadingListView() {
        String heading = null;

        // complete second heading line
        ArrayList<String> headingSecondLinePartList = new ArrayList<String>();
        String extraInformation = (String) labelHeading.getTag();
        if (! TextUtils.isEmpty(extraInformation)) {
            headingSecondLinePartList.add(extraInformation);
        }
        if (viewingDirectionFilter) {
            headingSecondLinePartList.add(
                    GlobalInstance.getStringResource(R.string.labelHeadingSecondLineViewingDirection));
        }
        if (autoUpdate) {
            headingSecondLinePartList.add(
                    GlobalInstance.getStringResource(R.string.labelHeadingSecondLineAutoUpdate));
        }

        // build
        ObjectWithIdAdapter listAdapter = getListAdapter();
        if (listAdapter != null) {
            heading = GlobalInstance.getPluralResource(
                    getPluralResourceId(), listAdapter.getCount());
            if (! headingSecondLinePartList.isEmpty()) {
                heading += String.format(
                        "\n%1$s", TextUtils.join(", ", headingSecondLinePartList));
            }
        } else {
            heading = GlobalInstance.getPluralResource(getPluralResourceId(), 0);
        }
        labelHeading.setText(heading);
    }


    /**
     * ObjectWithId adapter
     */

}
