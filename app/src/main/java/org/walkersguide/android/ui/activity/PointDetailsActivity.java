package org.walkersguide.android.ui.activity;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.Intersection;
import org.walkersguide.android.data.basic.point.POI;
import org.walkersguide.android.data.basic.point.Station;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.FavoritesProfile;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.ChildDialogCloseListener;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.ui.fragment.pointdetails.DeparturesFragment;
import org.walkersguide.android.ui.fragment.pointdetails.EntrancesFragment;
import org.walkersguide.android.ui.fragment.pointdetails.IntersectionWaysFragment;
import org.walkersguide.android.ui.fragment.pointdetails.PedestrianCrossingsFragment;
import org.walkersguide.android.ui.fragment.pointdetails.PointDetailsFragment;
import org.walkersguide.android.ui.view.CheckBoxGroupView;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.RouteSettings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Switch;
import android.widget.TextView;
import org.walkersguide.android.helper.PointUtility;
import android.view.Menu;
import java.util.ArrayList;
import org.walkersguide.android.data.basic.point.StreetAddress;
import org.json.JSONArray;

public class PointDetailsActivity extends AbstractActivity implements OnMenuItemClickListener {

	// instance variables
    private DirectionManager directionManagerInstance;
    private PositionManager positionManagerInstance;
    private PointWrapper pointWrapper;

    // activity ui components
	private ViewPager mViewPager;
    private TabLayout tabLayout;
    private int recentFragment;
    private TextView labelPointDistanceAndBearing;

    // point, address, entrance, gps,
	private PointPagerAdapter pointPagerAdapter;
	public FragmentCommunicator pointDetailsFragmentCommunicator;

	// intersection
	private IntersectionPagerAdapter intersectionPagerAdapter;
	public FragmentCommunicator intersectionWaysFragmentCommunicator;
	public FragmentCommunicator pedestrianCrossingsFragmentCommunicator;

    // poi
	private POIPagerAdapter poiPagerAdapter;
	public FragmentCommunicator entrancesFragmentCommunicator;

    // station
	private StationPagerAdapter stationPagerAdapter;
	public FragmentCommunicator departuresFragmentCommunicator;

	// fragment handler
	private Handler onFragmentEnabledHandler;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_point_details);
        directionManagerInstance = DirectionManager.getInstance(this);
        positionManagerInstance = PositionManager.getInstance(this);

        // load point
        try {
    		if (savedInstanceState != null) {
                pointWrapper = new PointWrapper(
                        this, new JSONObject(savedInstanceState.getString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED)));
            } else {
                pointWrapper = new PointWrapper(
		                this, new JSONObject(getIntent().getExtras().getString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "")));
            }
        } catch (JSONException e) {
            pointWrapper = null;
        }

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(
                getResources().getString(R.string.pointDetailsActivityTitle));

        if (pointWrapper != null) {
            getSupportActionBar().setTitle(
                    pointWrapper.getPoint().getType());
            // name, subtype and distance
    		TextView labelPointName = (TextView) findViewById(R.id.labelPointName);
            labelPointName.setText(
                    String.format(
                        getResources().getString(R.string.labelPointName),
                        pointWrapper.getPoint().getName())
                    );
    		TextView labelPointType = (TextView) findViewById(R.id.labelPointType);
            labelPointType.setText(
                    String.format(
                        getResources().getString(R.string.labelPointType),
                        pointWrapper.getPoint().getSubType())
                    );
    		labelPointDistanceAndBearing = (TextView) findViewById(R.id.labelPointDistanceAndBearing);

            // add to favorites
    		Button buttonPointFavorite = (Button) findViewById(R.id.buttonPointFavorite);
	    	buttonPointFavorite.setOnClickListener(new View.OnClickListener() {
		    	public void onClick(View view) {
                    SelectFavoritesProfilesForPointDialog.newInstance(pointWrapper)
                        .show(getSupportFragmentManager(), "SelectFavoritesProfilesForPointDialog");
                }
            });

            // simulate location
            Switch buttonPointSimulateLocation = (Switch) findViewById(R.id.buttonPointSimulateLocation);
            if (positionManagerInstance.getLocationSource() == Constants.LOCATION_SOURCE.SIMULATION
                    && pointWrapper.equals(positionManagerInstance.getCurrentLocation())) {
                buttonPointSimulateLocation.setChecked(true);
            }
            buttonPointSimulateLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                    if (isChecked) {
                        positionManagerInstance.setSimulatedLocation(pointWrapper);
                        positionManagerInstance.setLocationSource(
                                Constants.LOCATION_SOURCE.SIMULATION);
                    } else {
                        positionManagerInstance.setLocationSource(
                                Constants.LOCATION_SOURCE.GPS);
                    }
                }
            });

            // more options
            Button buttonMore = (Button) findViewById(R.id.buttonMore);
            buttonMore.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    PopupMenu popupMore = new PopupMenu(PointDetailsActivity.this, view);
                    popupMore.setOnMenuItemClickListener(PointDetailsActivity.this);
                    // start point
                    popupMore.getMenu().add(
                            Menu.NONE,
                            Constants.POINT_PUT_INTO.START,
                            1,
                            getResources().getString(R.string.menuItemAsRouteStartPoint));
                    // via points
                    ArrayList<PointWrapper> viaPointList = SettingsManager.getInstance(PointDetailsActivity.this).getRouteSettings().getViaPointList();
                    for (int viaPointIndex=0; viaPointIndex<viaPointList.size(); viaPointIndex++) {;
                        popupMore.getMenu().add(
                                Menu.NONE,
                                viaPointIndex+Constants.POINT_PUT_INTO.VIA,
                                viaPointIndex+2,
                                String.format(
                                    getResources().getString(R.string.menuItemAsRouteViaPoint),
                                    viaPointIndex+1));
                    }
                    // destination point
                    popupMore.getMenu().add(
                            Menu.NONE,
                            Constants.POINT_PUT_INTO.DESTINATION,
                            viaPointList.size()+2,
                            getResources().getString(R.string.menuItemAsRouteDestinationPoint));
                    popupMore.show();
                }
            });

            // ViewPager and TabLayout
            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.addOnPageChangeListener(new TabLayoutOnPageChangeListenerBugFree(tabLayout));
            tabLayout = (TabLayout) findViewById(R.id.tab_layout);

            int defaultRecentFragment;
            if (pointWrapper.getPoint() instanceof Station) {
                // set fragments for station
                stationPagerAdapter = new StationPagerAdapter(this);
                mViewPager.setAdapter(stationPagerAdapter);
                tabLayout.setupWithViewPager(mViewPager);
                tabLayout.setVisibility(View.VISIBLE);
                // default station fragment
                defaultRecentFragment = Constants.STATION_FRAGMENT.DETAILS;
            } else if (pointWrapper.getPoint() instanceof POI) {
                // set fragments for poi
                poiPagerAdapter = new POIPagerAdapter(this);
                mViewPager.setAdapter(poiPagerAdapter);
                tabLayout.setupWithViewPager(mViewPager);
                tabLayout.setVisibility(View.VISIBLE);
                // default poi fragment
                defaultRecentFragment = Constants.POI_FRAGMENT.DETAILS;
            } else if (pointWrapper.getPoint() instanceof Intersection) {
                // set fragments for intersection
                intersectionPagerAdapter = new IntersectionPagerAdapter(this);
                mViewPager.setAdapter(intersectionPagerAdapter);
                tabLayout.setupWithViewPager(mViewPager);
                tabLayout.setVisibility(View.VISIBLE);
                // default intersection fragment
                defaultRecentFragment = Constants.INTERSECTION_FRAGMENT.INTERSECTION_WAYS;
            } else {
                // set fragments for other point types
                pointPagerAdapter = new PointPagerAdapter(this);
                mViewPager.setAdapter(pointPagerAdapter);
                tabLayout.setVisibility(View.GONE);
                // default point fragment
                defaultRecentFragment = Constants.POINT_FRAGMENT.DETAILS;
            }

	    	// initialize handler for enabling fragment and open recent one
    		onFragmentEnabledHandler = new Handler();
            if (savedInstanceState != null) {
            	recentFragment = savedInstanceState.getInt("recentFragment", defaultRecentFragment);
            } else {
                recentFragment = defaultRecentFragment;
            }
            mViewPager.setCurrentItem(recentFragment);
        }
    }

    @Override public boolean onMenuItemClick(MenuItem item) {
        PointUtility.putNewPoint(
                PointDetailsActivity.this, pointWrapper, item.getItemId());
        PlanRouteDialog.newInstance().show(
                getSupportFragmentManager(), "PlanRouteDialog");
        return true;
    }

	@Override public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
        if (pointWrapper != null) {
            try {
                savedInstanceState.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, pointWrapper.toJson().toString());
            } catch (JSONException e) {
    	    	savedInstanceState.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
            }
        } else {
    	    savedInstanceState.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
        }
    	savedInstanceState.putInt("recentFragment", recentFragment);
	}

    @Override public void onPause() {
        super.onPause();
        if (pointWrapper != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(newLocationAndDirectionReceiver);
	    	leaveActiveFragment();
        }
    }

    @Override public void onResume() {
        super.onResume();
        if (pointWrapper != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_NEW_LOCATION);
            filter.addAction(Constants.ACTION_NEW_DIRECTION);
            LocalBroadcastManager.getInstance(this).registerReceiver(newLocationAndDirectionReceiver, filter);

            // request current location and direction values
            directionManagerInstance.requestCurrentDirection();
            positionManagerInstance.requestCurrentLocation();
            enterActiveFragment();
        }
    }


    /**
     * broadcast receiver
     */

    private BroadcastReceiver newLocationAndDirectionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (
                    (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                            && labelPointDistanceAndBearing.getText().toString().trim().equals(""))
                    || (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)
                        && intent.getIntExtra(Constants.ACTION_NEW_LOCATION_ATTR.INT_THRESHOLD_ID, -1) >= PositionManager.THRESHOLD1.ID)
                    || (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)
                        && intent.getIntExtra(Constants.ACTION_NEW_DIRECTION_ATTR.INT_THRESHOLD_ID, -1) >= DirectionManager.THRESHOLD1.ID)
                    ) {
                // update distance and bearing label
                labelPointDistanceAndBearing.setText(
                        String.format(
                            context.getResources().getString(R.string.labelPointDistanceAndBearing),
                            pointWrapper.distanceFromCurrentLocation(),
                            StringUtility.formatInstructionDirection(
                                context, pointWrapper.bearingFromCurrentLocation()))
                        );
            }
        }
    };


    /**
     * select favorites profiles for point
     */

    public static class SelectFavoritesProfilesForPointDialog extends DialogFragment implements ChildDialogCloseListener {

        private AccessDatabase accessDatabaseInstance;
        private PointWrapper selectedPoint;
        private TreeSet<Integer> checkedFavoritesProfileIds;
        private CheckBoxGroupView checkBoxGroupFavoritesProfiles;

        public static SelectFavoritesProfilesForPointDialog newInstance(PointWrapper selectedPoint) {
            SelectFavoritesProfilesForPointDialog selectFavoritesProfilesForPointDialogInstance = new SelectFavoritesProfilesForPointDialog();
            Bundle args = new Bundle();
            try {
                args.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, selectedPoint.toJson().toString());
            } catch (JSONException e) {
                args.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
            }
            selectFavoritesProfilesForPointDialogInstance.setArguments(args);
            return selectFavoritesProfilesForPointDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            try {
                selectedPoint = new PointWrapper(
                        getActivity(), new JSONObject(getArguments().getString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "")));
            } catch (JSONException e) {
                selectedPoint = PositionManager.getDummyLocation(getActivity());
            }

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_check_box_group, nullParent);

            checkBoxGroupFavoritesProfiles = (CheckBoxGroupView) view.findViewById(R.id.checkBoxGroup);
            if (selectedPoint.equals(PositionManager.getDummyLocation(getActivity()))) {
                SimpleMessageDialog dialog = SimpleMessageDialog.newInstance(
                        getResources().getString(R.string.messageErrorDataLoadingFailed));
                dialog.setTargetFragment(SelectFavoritesProfilesForPointDialog.this, 1);
                dialog.show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
            } else {
                if (savedInstanceState != null) {
                    checkedFavoritesProfileIds = new TreeSet<Integer>();
                    JSONArray jsonCheckedFavoritesProfileIdList = null;
                    try {
                        jsonCheckedFavoritesProfileIdList = new JSONArray(
                                savedInstanceState.getString("jsonCheckedFavoritesProfileIdList"));
                    } catch (JSONException e) {
                        jsonCheckedFavoritesProfileIdList = null;
                    } finally {
                        if (jsonCheckedFavoritesProfileIdList != null) {
                            for (int i=0; i<jsonCheckedFavoritesProfileIdList.length(); i++) {
                                try {
                                    checkedFavoritesProfileIds.add(jsonCheckedFavoritesProfileIdList.getInt(i));
                                } catch (JSONException e) {}
                            }
                        }
                    }
                } else {
                    checkedFavoritesProfileIds = accessDatabaseInstance.getCheckedFavoritesProfileIdsForPoint(selectedPoint);
                }

                for (Map.Entry<Integer,String> profile : accessDatabaseInstance.getFavoritesProfileMap().entrySet()) {
                    if (profile.getKey() == FavoritesProfile.ID_ADDRESS_POINTS
                            && ! (selectedPoint.getPoint() instanceof StreetAddress)) {
                                continue;
                    }
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
                            checkedFavoritesProfileIds = getCheckedItemsOfFavoritesProfilesCheckBoxGroup();
                            onStart();
                        }
                    });
                    checkBoxGroupFavoritesProfiles.put(checkBox);
                }
            }

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectFavoritesProfilesForPointDialogName))
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
                    checkBox.setChecked(
                            checkedFavoritesProfileIds.contains(checkBox.getId()));
                }

                // positive button
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        // remove unchecked profiles
                        TreeSet<Integer> favoritesProfileIdsToRemove = accessDatabaseInstance
                            .getCheckedFavoritesProfileIdsForPoint(selectedPoint);
                        favoritesProfileIdsToRemove.removeAll(checkedFavoritesProfileIds);
                        for (Integer favoritesProfileIdToRemove : favoritesProfileIdsToRemove) {
                            accessDatabaseInstance.removePointFromFavoritesProfile(selectedPoint, favoritesProfileIdToRemove);
                        }
                        // add profiles
                        TreeSet<Integer> favoritesProfileIdsToAdd = new TreeSet<Integer>(checkedFavoritesProfileIds);
                        favoritesProfileIdsToAdd.removeAll(
                                accessDatabaseInstance.getCheckedFavoritesProfileIdsForPoint(selectedPoint));
                        for (Integer favoritesProfileIdToAdd : favoritesProfileIdsToAdd) {
                            accessDatabaseInstance.addPointToFavoritesProfile(selectedPoint, favoritesProfileIdToAdd);
                        }
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
                        checkedFavoritesProfileIds = new TreeSet<Integer>();
                        if (checkBoxGroupFavoritesProfiles.nothingChecked()) {
                            checkedFavoritesProfileIds.addAll(accessDatabaseInstance.getFavoritesProfileMap().keySet());
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
            JSONArray jsonCheckedFavoritesProfileIdList = new JSONArray();
            for (Integer id : getCheckedItemsOfFavoritesProfilesCheckBoxGroup()) {
                jsonCheckedFavoritesProfileIdList.put(id);
            }
            savedInstanceState.putString("jsonCheckedFavoritesProfileIdList", jsonCheckedFavoritesProfileIdList.toString());
        }

        @Override public void childDialogClosed() {
            dismiss();
        }

        private TreeSet<Integer> getCheckedItemsOfFavoritesProfilesCheckBoxGroup() {
            TreeSet<Integer> favoritesProfileList = new TreeSet<Integer>();
            for (CheckBox checkBox : checkBoxGroupFavoritesProfiles.getCheckedCheckBoxList()) {
                favoritesProfileList.add(checkBox.getId());
            }
            return favoritesProfileList;
        }
    }


    /**
     * fragment management
     */

    private void leaveActiveFragment() {
        if (intersectionPagerAdapter != null) {
            switch (recentFragment) {
                case Constants.INTERSECTION_FRAGMENT.INTERSECTION_WAYS:
                    if (intersectionWaysFragmentCommunicator != null) {
                        intersectionWaysFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                case Constants.INTERSECTION_FRAGMENT.DETAILS:
                    if (pointDetailsFragmentCommunicator != null) {
                        pointDetailsFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                case Constants.INTERSECTION_FRAGMENT.PEDESTRIAN_CROSSINGS:
                    if (pedestrianCrossingsFragmentCommunicator != null) {
                        pedestrianCrossingsFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                default:
                    break;
            }
        } else if (pointPagerAdapter != null) {
            switch (recentFragment) {
                case Constants.POINT_FRAGMENT.DETAILS:
                    if (pointDetailsFragmentCommunicator != null) {
                        pointDetailsFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                default:
                    break;
            }
        } else if (poiPagerAdapter != null) {
            switch (recentFragment) {
                case Constants.POI_FRAGMENT.DETAILS:
                    if (pointDetailsFragmentCommunicator != null) {
                        pointDetailsFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                case Constants.POI_FRAGMENT.ENTRANCES:
                    if (entrancesFragmentCommunicator != null) {
                        entrancesFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                default:
                    break;
            }
        } else if (stationPagerAdapter != null) {
            switch (recentFragment) {
                case Constants.STATION_FRAGMENT.DEPARTURES:
                    if (departuresFragmentCommunicator != null) {
                        departuresFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                case Constants.STATION_FRAGMENT.DETAILS:
                    if (pointDetailsFragmentCommunicator != null) {
                        pointDetailsFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                case Constants.STATION_FRAGMENT.ENTRANCES:
                    if (entrancesFragmentCommunicator != null) {
                        entrancesFragmentCommunicator.onFragmentDisabled();
                    }
                    break;
                default:
                    break;
            }
        }
    }

	private void enterActiveFragment() {
		onFragmentEnabledHandler.postDelayed(new OnFragmentEnabledUpdater(recentFragment), 0);
	}

	private class OnFragmentEnabledUpdater implements Runnable {
		private static final int NUMBER_OF_RETRIES = 5;
		private int counter;
		private int currentFragment;

		public OnFragmentEnabledUpdater(int currentFragment) {
			this.counter = 0;
			this.currentFragment = currentFragment;
		}

        @Override public void run() {
            if (intersectionPagerAdapter != null) {
                switch (currentFragment) {
                    case Constants.INTERSECTION_FRAGMENT.INTERSECTION_WAYS:
                        if (intersectionWaysFragmentCommunicator != null) {
                            intersectionWaysFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    case Constants.INTERSECTION_FRAGMENT.DETAILS:
                        if (pointDetailsFragmentCommunicator != null) {
                            pointDetailsFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    case Constants.INTERSECTION_FRAGMENT.PEDESTRIAN_CROSSINGS:
                        if (pedestrianCrossingsFragmentCommunicator != null) {
                            pedestrianCrossingsFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    default:
                        return;
                }
            } else if (pointPagerAdapter != null) {
                switch (currentFragment) {
                    case Constants.POINT_FRAGMENT.DETAILS:
                        if (pointDetailsFragmentCommunicator != null) {
                            pointDetailsFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    default:
                        return;
                }
            } else if (poiPagerAdapter != null) {
                switch (recentFragment) {
                    case Constants.POI_FRAGMENT.DETAILS:
                        if (pointDetailsFragmentCommunicator != null) {
                            pointDetailsFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    case Constants.POI_FRAGMENT.ENTRANCES:
                        if (entrancesFragmentCommunicator != null) {
                            entrancesFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    default:
                        break;
                }
            } else if (stationPagerAdapter != null) {
                switch (recentFragment) {
                    case Constants.STATION_FRAGMENT.DEPARTURES:
                        if (departuresFragmentCommunicator != null) {
                            departuresFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    case Constants.STATION_FRAGMENT.DETAILS:
                        if (pointDetailsFragmentCommunicator != null) {
                            pointDetailsFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    case Constants.STATION_FRAGMENT.ENTRANCES:
                        if (entrancesFragmentCommunicator != null) {
                            entrancesFragmentCommunicator.onFragmentEnabled();
                            return;
                        }
                        break;
                    default:
                        break;
                }
            } else {
                return;
            }
            if (counter < NUMBER_OF_RETRIES) {
                counter += 1;
                onFragmentEnabledHandler.postDelayed(this, 100);
            }
        }
    }


	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */

	public class PointPagerAdapter extends FragmentStatePagerAdapter {

		public PointPagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case Constants.POINT_FRAGMENT.DETAILS:
                    return PointDetailsFragment.newInstance(pointWrapper);
                default:
                    return null;
            }
        }

		@Override public CharSequence getPageTitle(int position) {
            switch (position) {
                case Constants.POINT_FRAGMENT.DETAILS:
    				return getResources().getString(R.string.fragmentPointDetailsName);
                default:
                    return "";
            }
		}

		@Override public int getCount() {
			return Constants.PointFragmentValueArray.length;
		}
	}


	public class IntersectionPagerAdapter extends FragmentStatePagerAdapter {

		public IntersectionPagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case Constants.INTERSECTION_FRAGMENT.INTERSECTION_WAYS:
                    return IntersectionWaysFragment.newInstance(pointWrapper);
                case Constants.INTERSECTION_FRAGMENT.DETAILS:
                    return PointDetailsFragment.newInstance(pointWrapper);
                case Constants.INTERSECTION_FRAGMENT.PEDESTRIAN_CROSSINGS:
                    return PedestrianCrossingsFragment.newInstance(pointWrapper);
                default:
                    return null;
            }
        }

		@Override public CharSequence getPageTitle(int position) {
            switch (position) {
                case Constants.INTERSECTION_FRAGMENT.INTERSECTION_WAYS:
    				return getResources().getString(R.string.fragmentIntersectionWaysName);
                case Constants.INTERSECTION_FRAGMENT.DETAILS:
    				return getResources().getString(R.string.fragmentPointDetailsName);
                case Constants.INTERSECTION_FRAGMENT.PEDESTRIAN_CROSSINGS:
    				return getResources().getString(R.string.fragmentPedestrianCrossingsName);
                default:
                    return "";
            }
		}

		@Override public int getCount() {
			return Constants.IntersectionFragmentValueArray.length;
		}
	}


	public class POIPagerAdapter extends FragmentStatePagerAdapter {

		public POIPagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case Constants.POI_FRAGMENT.DETAILS:
                    return PointDetailsFragment.newInstance(pointWrapper);
                case Constants.POI_FRAGMENT.ENTRANCES:
                    return EntrancesFragment.newInstance(pointWrapper);
                default:
                    return null;
            }
        }

		@Override public CharSequence getPageTitle(int position) {
            switch (position) {
                case Constants.POI_FRAGMENT.DETAILS:
    				return getResources().getString(R.string.fragmentPointDetailsName);
                case Constants.POI_FRAGMENT.ENTRANCES:
    				return getResources().getString(R.string.fragmentEntrancesName);
                default:
                    return "";
            }
		}

		@Override public int getCount() {
			return Constants.POIFragmentValueArray.length;
		}
	}

	public class StationPagerAdapter extends FragmentStatePagerAdapter {

		public StationPagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case Constants.STATION_FRAGMENT.DEPARTURES:
                    return DeparturesFragment.newInstance(pointWrapper);
                case Constants.STATION_FRAGMENT.DETAILS:
                    return PointDetailsFragment.newInstance(pointWrapper);
                case Constants.STATION_FRAGMENT.ENTRANCES:
                    return EntrancesFragment.newInstance(pointWrapper);
                default:
                    return null;
            }
        }

		@Override public CharSequence getPageTitle(int position) {
            switch (position) {
                case Constants.STATION_FRAGMENT.DEPARTURES:
    				return getResources().getString(R.string.fragmentDeparturesName);
                case Constants.STATION_FRAGMENT.DETAILS:
    				return getResources().getString(R.string.fragmentPointDetailsName);
                case Constants.STATION_FRAGMENT.ENTRANCES:
    				return getResources().getString(R.string.fragmentEntrancesName);
                default:
                    return "";
            }
		}

		@Override public int getCount() {
			return Constants.StationFragmentValueArray.length;
		}
	}


    /**
     * A custom Page Change Listener which fixes the bug described here:
     * https://code.google.com/p/android/issues/detail?id=183123
     **/

    private class TabLayoutOnPageChangeListenerBugFree implements ViewPager.OnPageChangeListener {

        private final WeakReference<TabLayout> mTabLayoutRef;
        private int mPreviousScrollState;
        private int mScrollState;

        public TabLayoutOnPageChangeListenerBugFree(TabLayout tabLayout) {
            mTabLayoutRef = new WeakReference<TabLayout>(tabLayout);
        }

        @Override public void onPageScrollStateChanged(int state) {
            mPreviousScrollState = mScrollState;
            mScrollState = state;
        }

        @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            final TabLayout tabLayout = mTabLayoutRef.get();
            if (tabLayout != null) {
                final boolean updateText = (mScrollState == ViewPager.SCROLL_STATE_DRAGGING)
                    || (mScrollState == ViewPager.SCROLL_STATE_SETTLING
                            && mPreviousScrollState == ViewPager.SCROLL_STATE_DRAGGING);
                tabLayout.setScrollPosition(position, positionOffset, updateText);
            }
        }

        @Override public void onPageSelected(int position) {
            if (recentFragment != position) {
                leaveActiveFragment();
                final TabLayout tabLayout = mTabLayoutRef.get();
                if (tabLayout != null) {
                    tabLayout.getTabAt(position).select();
                }
                recentFragment = position;
                enterActiveFragment();
            }
        }
    }

}
