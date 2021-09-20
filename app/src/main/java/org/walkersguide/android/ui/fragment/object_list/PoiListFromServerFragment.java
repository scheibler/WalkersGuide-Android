package org.walkersguide.android.ui.fragment.object_list;

import org.walkersguide.android.ui.fragment.ObjectListFragment;
import org.walkersguide.android.server.poi.PoiCategory;
import org.walkersguide.android.server.poi.PoiProfile;
import org.walkersguide.android.server.poi.PoiProfileManager;
import org.walkersguide.android.server.poi.PoiProfileManager.PoiProfileRequestListener;
import org.walkersguide.android.server.poi.PoiProfileRequest;
import org.walkersguide.android.server.poi.PoiProfileResult;

import org.walkersguide.android.ui.dialog.selectors.SelectPoiCategoriesDialog;
import org.walkersguide.android.ui.dialog.selectors.SelectPoiCategoriesDialog.SelectPoiCategoriesListener;
import org.walkersguide.android.ui.dialog.selectors.SelectPoiProfileDialog;
import org.walkersguide.android.ui.dialog.selectors.SelectPoiProfileDialog.SelectPoiProfileListener;

import org.walkersguide.android.R;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import timber.log.Timber;
import android.content.Context;


import android.os.Bundle;

import androidx.core.view.ViewCompat;


import android.view.LayoutInflater;
import android.view.View;

import android.widget.TextView;


import java.util.ArrayList;
import org.walkersguide.android.data.basic.point.Point;
import android.os.Handler;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.ui.dialog.SelectMapDialog;


public class PoiListFromServerFragment extends ObjectListFragment
        implements PoiProfileRequestListener, SelectPoiProfileListener, SelectPoiCategoriesListener {
    private static final String KEY_REQUEST = "request";

    private PoiProfileRequest request;
    private TextView labelMoreResultsFooter;

    // query in progress
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;

	public static PoiListFromServerFragment createDialog(
            PoiProfileRequest request, boolean showSelectProfileButton) {
        return newInstance(request, showSelectProfileButton, true);
    }

	public static PoiListFromServerFragment createFragment(
            PoiProfileRequest request, boolean showSelectProfileButton) {
        return newInstance(request, showSelectProfileButton, false);
    }

    private static PoiListFromServerFragment newInstance(
            PoiProfileRequest request, boolean showSelectProfileButton, boolean isDialog) {
		PoiListFromServerFragment fragment = new PoiListFromServerFragment();
        Bundle args = ObjectListFragment.createArgsBundle(isDialog, showSelectProfileButton, true);
        args.putSerializable(KEY_REQUEST, request);
        fragment.setArguments(args);
		return fragment;
	}

    @Override public void onAttach(Context context){
        super.onAttach(context);
        // progress updater
        this.progressHandler = new Handler();
        this.progressUpdater = new ProgressUpdater();
    }


    /**
     * menu
     */

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_server_point_list_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItemFilterResult = menu.findItem(R.id.menuItemFilterResult);
        if (menuItemFilterResult != null) {
            menuItemFilterResult.setChecked(request.getFilterByViewingDirection());
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemFilterResult:
                request.toggleFilterByViewingDirection();
                resetListPosition();
                requestUiUpdate();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * create view
     */

	@Override public View configureView(View view, Bundle savedInstanceState) {
        view = super.configureView(view, savedInstanceState);

        if (savedInstanceState != null) {
            request = (PoiProfileRequest) savedInstanceState.getSerializable(KEY_REQUEST);
        } else {
            request = (PoiProfileRequest) getArguments().getSerializable(KEY_REQUEST);
        }
        editSearch.setText(request.getSearchTerm());

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View footerView = inflater.inflate(R.layout.layout_single_text_view, null, false);
        labelMoreResultsFooter = (TextView) footerView.findViewById(R.id.label);
        labelMoreResultsFooter.setText(getResources().getString(R.string.labelMoreResults));
        labelMoreResultsFooter.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                requestMoreResults();
            }
        });

        return view;
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_REQUEST, request);
    }

    @Override public void clickedButtonSelectProfile() {
        SelectPoiProfileDialog dialog = SelectPoiProfileDialog.newInstance(request.getProfile());
        dialog.setTargetFragment(PoiListFromServerFragment.this, 1);
        dialog.show(getActivity().getSupportFragmentManager(), "SelectPoiProfileDialog");
    }

    @Override public void longClickedButtonSelectProfile() {
        PoiProfile profile = request.getProfile();
        if (profile != null) {
            SelectPoiCategoriesDialog dialog = SelectPoiCategoriesDialog.newInstance(profile.getPoiCategoryList());
            dialog.setTargetFragment(PoiListFromServerFragment.this, 1);
            dialog.show(getActivity().getSupportFragmentManager(), "SelectPoiCategoriesDialog");
        }
    }

    @Override public void clickedLabelEmptyListView() {
        requestMoreResults();
    }

    @Override public void searchTermChanged(String newSearchTerm) {
        request.setSearchTerm(newSearchTerm);
    }

    @Override public String getDialogTitle() {
        if (request.getProfile() != null
                && buttonSelectProfile.getVisibility() == View.GONE) {
            return request.getProfile().getName();
        } else {
            return getResources().getString(R.string.fragmentPOIName);
        }
    }


    /*
     * listener
     */

    @Override public void poiProfileSelected(PoiProfile newProfile) {
        request.setProfile(newProfile);
        resetListPosition();
        requestUiUpdate();
    }

    @Override public void poiCategoriesSelected(ArrayList<PoiCategory> newPoiCategoryList) {
        request.getProfile().setValues(
                request.getProfile().getName(), newPoiCategoryList, request.getProfile().getIncludeFavorites());
        resetListPosition();
        requestUiUpdate();
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        Timber.d("onPause");
        PoiProfileManager.getInstance().invalidatePoiProfileRequest((PoiListFromServerFragment) this);
    }

    @Override public void onResume() {
        super.onResume();
        Timber.d("onResume");
        requestUiUpdate();
    }


    /**
     * point list request and response
     */

    public void requestUiUpdate() {
        this.prepareRequest();
        PoiProfileManager.getInstance().startPoiProfileRequest(
                (PoiListFromServerFragment) this, request, PoiProfileManager.RequestAction.UPDATE);
    }

    public void requestMoreResults() {
        this.prepareRequest();
        PoiProfileManager.getInstance().startPoiProfileRequest(
                (PoiListFromServerFragment) this, request, PoiProfileManager.RequestAction.MORE_RESULTS);
    }

    @Override public void prepareRequest() {
        super.prepareRequest();
        progressHandler.postDelayed(progressUpdater, 2000);

        buttonSelectProfile.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.buttonSelectProfile),
                    request.getProfile().getName())
                );

        if (listViewObject.getFooterViewsCount() > 0) {
            listViewObject.removeFooterView(labelMoreResultsFooter);
        }
    }

    @Override public void poiProfileRequestSuccessful(PoiProfileResult result) {
        super.updateRefreshButtonAfterRequestWasFinished();
        progressHandler.removeCallbacks(progressUpdater);

        ArrayList<Point> pointList = result.getAllPointList();
        if (request.getFilterByViewingDirection()) {
            pointList =filterPointListByViewingDirection(pointList);
        }
        if (result.getResetListPosition()) {
            resetListPosition();
        }
        super.updateListViewAfterRequestWasSuccessful(pointList);

        // header
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        String heading = String.format(
                GlobalInstance.getStringResource(R.string.labelPOIFragmentHeaderSuccess),
                GlobalInstance.getPluralResource(R.plurals.poi, pointList.size()),
                GlobalInstance.getPluralResource(R.plurals.meter, result.getLookupRadius()));
        labelHeading.setText(heading);

        // more results
        if (listViewObject.getAdapter().getCount() == 0) {
            labelEmptyListView.setClickable(true);
            labelEmptyListView.setText(
                    GlobalInstance.getStringResource(R.string.labelMoreResults));
        } else {
            listViewObject.addFooterView(labelMoreResultsFooter, null, true);
        }
    }

    @Override public void poiProfileRequestFailed(int returnCode) {
        super.updateRefreshButtonAfterRequestWasFinished();
        progressHandler.removeCallbacks(progressUpdater);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        labelEmptyListView.setText(
                ServerUtility.getErrorMessageForReturnCode(returnCode));
        // show select map dialog
        if (isAdded()
                && (
                       returnCode == Constants.RC.MAP_LOADING_FAILED
                    || returnCode == Constants.RC.WRONG_MAP_SELECTED)) {
            SelectMapDialog.newInstance()
                .show(getActivity().getSupportFragmentManager(), "SelectMapDialog");
        }
    }

    private ArrayList<Point> filterPointListByViewingDirection(ArrayList<Point> listOfAllPoints) {
        // only include, what's ahead
        ArrayList<Point> listOfFilteredPoints = new ArrayList<Point>();
        for (Point point : listOfAllPoints) {
            Integer bearing = point.bearingFromCurrentLocation();
            if (bearing != null
                    && (bearing < 60 || bearing > 300)) {
                listOfFilteredPoints.add(point);
            }
        }
        return listOfFilteredPoints;
    }


    private class ProgressUpdater implements Runnable {
        public void run() {
            vibrator.vibrate(50);
            progressHandler.postDelayed(this, 2000);
        }
    }

}
