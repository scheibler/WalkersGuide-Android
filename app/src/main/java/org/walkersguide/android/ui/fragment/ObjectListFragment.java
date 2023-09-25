package org.walkersguide.android.ui.fragment;

import org.walkersguide.android.sensor.bearing.AcceptNewBearing;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.walkersguide.android.server.wg.status.OSMMap;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import org.walkersguide.android.data.ObjectWithId;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;
import android.view.MenuItem;
import timber.log.Timber;

import android.os.Bundle;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ListView;
import android.widget.TextView;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import android.app.Dialog;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import java.util.ArrayList;
import android.content.BroadcastReceiver;
import android.content.Context;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import androidx.core.view.ViewCompat;
import android.widget.AbsListView;
import android.content.Intent;
import android.widget.BaseAdapter;
import java.util.Comparator;
import java.util.Collections;
import android.view.WindowManager;
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
import org.walkersguide.android.data.Angle;
import org.walkersguide.android.data.angle.Bearing;
import android.widget.HeaderViewListAdapter;
import org.walkersguide.android.data.angle.RelativeBearing;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.ui.fragment.RootFragment;
import org.walkersguide.android.tts.TTSWrapper;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;


public abstract class ObjectListFragment extends RootFragment
        implements FragmentResultListener, MenuProvider, OnRefreshListener {
    public static final String REQUEST_SELECT_OBJECT = "selectObject";
    public static final String EXTRA_OBJECT_WITH_ID = "objectWithId";

    public abstract int getPluralResourceId();
    public abstract boolean isUiUpdateRequestInProgress();
    public abstract void requestUiUpdate();
    public abstract void requestMoreResults();


    public static class BundleBuilder {
        protected Bundle bundle = new Bundle();

        public BundleBuilder() {
            bundle.putSerializable(KEY_DIALOG_MODE, DialogMode.DISABLED);
            setAutoUpdate(false);
            setViewingDirectionFilter(false);
            setAnnounceObjectAhead(false);
        }

        public BundleBuilder setIsDialog(boolean enableSelection) {
            bundle.putSerializable(
                    KEY_DIALOG_MODE,
                    enableSelection ? DialogMode.SELECT : DialogMode.DEFAULT);
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
        public BundleBuilder setAnnounceObjectAhead(boolean newState) {
            bundle.putBoolean(KEY_ANNOUNCE_OBJECT_AHEAD, newState);
            return this;
        }

        public Bundle build() {
            return bundle;
        }
    }


    // dialog
    private static final String KEY_DIALOG_MODE = "dialogMode";
    private static final String KEY_AUTO_UPDATE = "autoUpdate";
    private static final String KEY_VIEWING_DIRECTION_FILTER = "viewingDirectionFilter";
    private static final String KEY_ANNOUNCE_OBJECT_AHEAD = "announceObjectAhead";
    private static final String KEY_LIST_POSITION = "listPosition";

    private enum DialogMode {
        DISABLED, DEFAULT, SELECT
    }

    private DialogMode dialogMode;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        dialogMode = (DialogMode) getArguments().getSerializable(KEY_DIALOG_MODE);
        setShowsDialog(dialogMode != DialogMode.DISABLED);

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


    /**
     * create view
     */

    private boolean autoUpdate, viewingDirectionFilter, announceObjectAhead;
    private int listPosition;

    private TextView labelHeading, labelEmptyListView, labelMoreResultsFooter;
    private SwipeRefreshLayout swipeRefreshListView, swipeRefreshEmptyTextView;
	private ListView listViewObject;

    @Override public String getDialogButtonText() {
        return getResources().getString(
                dialogMode == DialogMode.SELECT
                ? R.string.dialogCancel
                : R.string.dialogClose);
    }

    @Override public int getLayoutResourceId() {
        return R.layout.fragment_object_list;
    }

	@Override public View configureView(View view, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            autoUpdate = savedInstanceState.getBoolean(KEY_AUTO_UPDATE);
            viewingDirectionFilter = savedInstanceState.getBoolean(KEY_VIEWING_DIRECTION_FILTER);
            announceObjectAhead = savedInstanceState.getBoolean(KEY_ANNOUNCE_OBJECT_AHEAD);
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            autoUpdate = getArguments().getBoolean(KEY_AUTO_UPDATE);
            viewingDirectionFilter = getArguments().getBoolean(KEY_VIEWING_DIRECTION_FILTER);
            announceObjectAhead = getArguments().getBoolean(KEY_ANNOUNCE_OBJECT_AHEAD);
            listPosition = 0;
        }

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
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
        savedInstanceState.putBoolean(KEY_ANNOUNCE_OBJECT_AHEAD,  announceObjectAhead);
        savedInstanceState.putInt(KEY_LIST_POSITION,  listPosition);
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

    public String getEmptyObjectListMessage() {
        return "";
    }

    public int getListPosition() {
        return this.listPosition;
    }

    public void resetListPosition() {
        listPosition = 0;
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
        Timber.d("menuItemRefresh: %1$s, isUiUpdateRequestInProgress: %2$s", menuItemRefresh, isUiUpdateRequestInProgress());
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
        // announce object ahead
        MenuItem menuItemAnnounceObjectAhead = menu.findItem(R.id.menuItemAnnounceObjectAhead);
        menuItemAnnounceObjectAhead.setChecked(announceObjectAhead);
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
        } else if (item.getItemId() == R.id.menuItemAnnounceObjectAhead) {
            announceObjectAhead = ! announceObjectAhead;
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
        filter.addAction(Segment.ACTION_EXCLUDED_FROM_ROUTING_STATUS_CHANGED);
        filter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        LocalBroadcastManager
            .getInstance(getActivity())
            .registerReceiver(mMessageReceiver, filter);
        // request ui update
        requestUiUpdate();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        private AcceptNewPosition acceptNewPosition = AcceptNewPosition.newInstanceForPoiListUpdate();
        private AcceptNewBearing acceptNewBearing = AcceptNewBearing.newInstanceForPoiListUpdate();
        private ObjectWithId lastClosestObject = null;

        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Segment.ACTION_EXCLUDED_FROM_ROUTING_STATUS_CHANGED)) {
                requestUiUpdate();

            } else if (! getActivity().hasWindowFocus()) {
                if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)
                        && intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION) != null
                        && intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false)) {
                    Timber.d("update cause of important while window doesnt have focus");
                    requestUiUpdate();
                }

            } else if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                Point currentLocation = (Point) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION);
                if (currentLocation != null) {
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
                }

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                ObjectWithIdAdapter listAdapter = getListAdapter();
                Bearing currentBearing = (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (listAdapter != null && currentBearing != null) {
                    Point currentLocation = PositionManager.getInstance().getCurrentLocation();
                    // find closest object
                    ObjectWithId closestObject = null;
                    RelativeBearing closestRelativeBearing = null;
                    for (int i=0; i<listAdapter.getCount(); i++) {
                        RelativeBearing relativeBearing = null;
                        if (listAdapter.getItem(i) instanceof Point
                                && currentLocation != null
                                && i < 40) {
                            relativeBearing = currentLocation
                                .bearingTo(((Point) listAdapter.getItem(i)))
                                .relativeTo(currentBearing);
                        } else if (listAdapter.getItem(i) instanceof Segment) {
                            relativeBearing = ((Segment) listAdapter.getItem(i))
                                .getBearing()
                                .relativeTo(currentBearing);
                        }
                        if (relativeBearing != null
                                && (
                                       closestRelativeBearing == null
                                    || closestRelativeBearing.compareTo(relativeBearing) == 1)) {
                            closestObject = listAdapter.getItem(i);
                            closestRelativeBearing = relativeBearing;
                        }
                    }
                    boolean lastClosestObjectUpdated = false;
                    if (closestObject != null
                            && ! closestObject.equals(lastClosestObject)
                            && closestRelativeBearing != null
                            && closestRelativeBearing.getDirection() == RelativeBearing.Direction.STRAIGHT_AHEAD) {
                        lastClosestObject = closestObject;
                        lastClosestObjectUpdated = true;
                    }

                    // auto-update
                    if (autoUpdate) {
                        if (viewingDirectionFilter && acceptNewBearing.updateBearing(currentBearing)) {
                            Timber.d("notifyDataSetChanged viewingDirectionFilter");
                            listAdapter.notifyDataSetChanged();
                        } else if (listAdapter.hasListComparator() && lastClosestObjectUpdated) {
                            // hack: update list adapter, if child is an IntersectionStructureFragment
                            listAdapter.notifyDataSetChanged();
                        }
                    }

                    // announce
                    if (announceObjectAhead && lastClosestObjectUpdated) {
                        String message = null;
                        if (closestObject instanceof Point) {
                            Point point = (Point) closestObject;
                            message = point.formatNameAndSubType();
                            if (currentLocation != null) {
                                message += String.format(
                                        ", %1$s",
                                        GlobalInstance.getPluralResource(
                                            R.plurals.inMeters,
                                            point.distanceTo(currentLocation)));
                            }
                        } else if (closestObject instanceof Segment) {
                            Segment segment = (Segment) closestObject;
                            message = segment.formatNameAndSubType();
                            if (segment instanceof IntersectionSegment
                                    && ((IntersectionSegment) segment).isPartOfNextRouteSegment()) {
                                message += String.format(
                                        ", %1$s", GlobalInstance.getStringResource(R.string.labelPartOfNextRouteSegment));
                                    }
                        } else {
                            message = closestObject.getName();
                        }
                        Timber.d("new object ahead: %1$s, %2$d°", message, closestRelativeBearing.getDegree());
                        TTSWrapper.getInstance().screenReader(message);
                    }
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

    public void populateUiAfterRequestWasSuccessful(String headingSecondLine,
            ArrayList<? extends ObjectWithId> objectList,
            boolean showIsFavoriteIndicator, boolean showMenuItemRemoveObject) {
        labelHeading.setTag(headingSecondLine);
        swipeRefreshListView.setRefreshing(false);
        swipeRefreshEmptyTextView.setRefreshing(false);

        // fill list view
        listViewObject.setAdapter(
                new ObjectWithIdAdapter(
                    ObjectListFragment.this.getContext(), objectList,
                    showIsFavoriteIndicator, showMenuItemRemoveObject));
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

    public void populateUiAndShowMoreResultsFooterAfterRequestWasSuccessful(
            String headingSecondLine, ArrayList<? extends ObjectWithId> objectList) {
        // only poi profiles call this function for now
        populateUiAfterRequestWasSuccessful(headingSecondLine, objectList, true, false);

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

    public class ObjectWithIdAdapter extends BaseAdapter {

        private Context context;
        private ArrayList<? extends ObjectWithId> objectList, filteredObjectList;
        private boolean showIsFavoriteIndicator, showMenuItemRemoveObject;
        private Comparator<ObjectWithId> listComparator;

        public ObjectWithIdAdapter(Context context, ArrayList<? extends ObjectWithId> objectList,
                boolean showIsFavoriteIndicator, boolean showMenuItemRemoveObject) {
            this.context = context;
            this.objectList = objectList;
            this.filteredObjectList = populateFilteredObjectList();
            this.showIsFavoriteIndicator = showIsFavoriteIndicator;
            this.showMenuItemRemoveObject = showMenuItemRemoveObject;
            this.listComparator = null;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            TextViewAndActionButton layoutTextViewAndActionButton = null;
            if (convertView == null) {
                layoutTextViewAndActionButton = new TextViewAndActionButton(this.context, null, true);
                layoutTextViewAndActionButton.setLayoutParams(
                        new LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            } else {
                layoutTextViewAndActionButton = (TextViewAndActionButton) convertView;
            }

            if (dialogMode == DialogMode.SELECT) {
                layoutTextViewAndActionButton.setOnObjectDefaultActionListener(new TextViewAndActionButton.OnObjectDefaultActionListener() {
                    @Override public void onObjectDefaultAction(TextViewAndActionButton view) {
                        dismiss();
                        Bundle result = new Bundle();
                        result.putSerializable(EXTRA_OBJECT_WITH_ID, view.getObject());
                        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_OBJECT, result);
                    }
                });
            }

            TextViewAndActionButton.OnLayoutResetListener listenerRemoveObject = null;
            if (this.showMenuItemRemoveObject) {
                listenerRemoveObject = new TextViewAndActionButton.OnLayoutResetListener() {
                    @Override public void onLayoutReset(TextViewAndActionButton view) {
                        ObjectWithId objectToRemove = view.getObject();
                        if (objectToRemove != null) {
                            objectToRemove.removeFromDatabase();
                            requestUiUpdate();
                        }
                    }
                };
            }

            layoutTextViewAndActionButton.setAutoUpdate(autoUpdate);
            layoutTextViewAndActionButton.configureAsListItem(
                    getItem(position), this.showIsFavoriteIndicator, listenerRemoveObject);
            return layoutTextViewAndActionButton;
        }

        @Override public int getCount() {
            return this.filteredObjectList.size();
        }

        @Override public ObjectWithId getItem(int position) {
            return this.filteredObjectList.get(position);
        }

        @Override public long getItemId(int position) {
            return position;
        }

        @Override public void notifyDataSetChanged() {
            if (this.listComparator != null) {
                Collections.sort(this.objectList, this.listComparator);
            }
            this.filteredObjectList = populateFilteredObjectList();
            updateHeadingListView();
            // the following must be put after the object list was sorted and updated
            super.notifyDataSetChanged();
        }

        public boolean isEmpty() {
            return this.filteredObjectList.isEmpty();
        }

        public boolean  hasListComparator() {
            return this.listComparator != null;
        }

        public void setListComparator(Comparator<ObjectWithId> newComparator) {
            this.listComparator = newComparator;
        }

        private ArrayList<? extends ObjectWithId> populateFilteredObjectList() {
            if (this.objectList == null) {
                return new ArrayList<ObjectWithId>();
            } else if (! viewingDirectionFilter) {
                return this.objectList;
            } else {
                // only include, what's ahead
                ArrayList<ObjectWithId> filteredObjectList = new ArrayList<ObjectWithId>();
                for (ObjectWithId object : this.objectList) {
                    if (object instanceof Point) {
                        if (((Point) object)
                                .bearingFromCurrentLocation()
                                .relativeToCurrentBearing()
                                .withinRange(Angle.Quadrant.Q7.min, Angle.Quadrant.Q1.max)) {
                            filteredObjectList.add(object);
                        }
                    } else {
                        filteredObjectList.add(object);
                    }
                }
                return filteredObjectList;
            }
        }


        private class EntryHolder {
            public TextViewAndActionButton layoutTextViewAndActionButton;
        }
    }

}
