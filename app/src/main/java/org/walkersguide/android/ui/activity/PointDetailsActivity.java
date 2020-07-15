package org.walkersguide.android.ui.activity;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;

import android.view.LayoutInflater;
import android.view.Menu;
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

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.point.Intersection;
import org.walkersguide.android.data.basic.point.POI;
import org.walkersguide.android.data.basic.point.Station;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.sensor.attribute.NewDirectionAttributes;
import org.walkersguide.android.data.sensor.attribute.NewLocationAttributes;
import org.walkersguide.android.data.sensor.threshold.BearingThreshold;
import org.walkersguide.android.data.sensor.threshold.DistanceThreshold;
import org.walkersguide.android.helper.PointUtility;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog.ChildDialogCloseListener;
import org.walkersguide.android.ui.fragment.pointdetails.pt.DeparturesFragment;
import org.walkersguide.android.ui.fragment.pointdetails.EntrancesFragment;
import org.walkersguide.android.ui.fragment.pointdetails.IntersectionWaysFragment;
import org.walkersguide.android.ui.fragment.pointdetails.PedestrianCrossingsFragment;
import org.walkersguide.android.ui.fragment.pointdetails.PointDetailsFragment;
import org.walkersguide.android.ui.view.CheckBoxGroupView;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;


public class PointDetailsActivity extends AbstractActivity implements OnMenuItemClickListener {

	// instance variables
    private DirectionManager directionManagerInstance;
    private PositionManager positionManagerInstance;
    private PointWrapper pointWrapper;

    // activity ui components
	private ViewPager viewPager;
    private FragmentPagerAdapter selectedTabAdapter;
    private TabLayout tabLayout;
    private int recentFragment;

    private TextView labelPointDistanceAndBearing;
    private Switch buttonPointSimulateLocation;

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
                    StringUtility.formatPointType(
                        PointDetailsActivity.this, pointWrapper.getPoint().getType()));
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
                    SelectPOIProfilesForPointDialog.newInstance(pointWrapper)
                        .show(getSupportFragmentManager(), "SelectPOIProfilesForPointDialog");
                }
            });

            // simulate location
            buttonPointSimulateLocation = (Switch) findViewById(R.id.buttonPointSimulateLocation);
            buttonPointSimulateLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                    boolean isSimulated = 
                           positionManagerInstance.getSimulationEnabled()
                        && pointWrapper.equals(positionManagerInstance.getCurrentLocation());
                    if (isChecked && ! isSimulated) {
                        positionManagerInstance.setSimulatedLocation(pointWrapper);
                        positionManagerInstance.setSimulationEnabled(true);
                    } else if (! isChecked && isSimulated) {
                        positionManagerInstance.setSimulationEnabled(false);
                    }
                }
            });

            // add to route
            Button buttonAddToRoute = (Button) findViewById(R.id.buttonAddToRoute);
            buttonAddToRoute.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    PopupMenu popupAddToRoute = new PopupMenu(PointDetailsActivity.this, view);
                    popupAddToRoute.setOnMenuItemClickListener(PointDetailsActivity.this);
                    // start point
                    popupAddToRoute.getMenu().add(
                            Menu.NONE,
                            Constants.POINT_PUT_INTO.START,
                            1,
                            getResources().getString(R.string.menuItemAsRouteStartPoint));
                    // via points
                    ArrayList<PointWrapper> viaPointList = SettingsManager.getInstance(PointDetailsActivity.this).getRouteSettings().getViaPointList();
                    for (int viaPointIndex=0; viaPointIndex<viaPointList.size(); viaPointIndex++) {;
                        popupAddToRoute.getMenu().add(
                                Menu.NONE,
                                viaPointIndex+Constants.POINT_PUT_INTO.VIA,
                                viaPointIndex+2,
                                String.format(
                                    getResources().getString(R.string.menuItemAsRouteViaPoint),
                                    viaPointIndex+1));
                    }
                    // destination point
                    popupAddToRoute.getMenu().add(
                            Menu.NONE,
                            Constants.POINT_PUT_INTO.DESTINATION,
                            viaPointList.size()+2,
                            getResources().getString(R.string.menuItemAsRouteDestinationPoint));
                    popupAddToRoute.show();
                }
            });

            // ViewPager and TabLayout
    		viewPager = (ViewPager) findViewById(R.id.pager);
            viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override public void onPageSelected(int position) {
                    if (recentFragment != position) {
                        setToolbarTitle(position);
                        recentFragment = position;
                    }
                }
            });
            tabLayout = (TabLayout) findViewById(R.id.tab_layout);

            int defaultRecentFragment;
            if (pointWrapper.getPoint() instanceof Station) {
                // set fragments for station
                StationPagerAdapter stationPagerAdapter = new StationPagerAdapter(this);
                viewPager.setAdapter(stationPagerAdapter);
                selectedTabAdapter = stationPagerAdapter;
                tabLayout.setupWithViewPager(viewPager);
                tabLayout.setVisibility(View.VISIBLE);
                // default station fragment
                defaultRecentFragment = Constants.STATION_FRAGMENT.DETAILS;
            } else if (pointWrapper.getPoint() instanceof POI) {
                // set fragments for poi
                POIPagerAdapter poiPagerAdapter = new POIPagerAdapter(this);
                viewPager.setAdapter(poiPagerAdapter);
                selectedTabAdapter = poiPagerAdapter;
                tabLayout.setupWithViewPager(viewPager);
                tabLayout.setVisibility(View.VISIBLE);
                // default poi fragment
                defaultRecentFragment = Constants.POI_FRAGMENT.DETAILS;
            } else if (pointWrapper.getPoint() instanceof Intersection) {
                // set fragments for intersection
                IntersectionPagerAdapter intersectionPagerAdapter = new IntersectionPagerAdapter(this);
                viewPager.setAdapter(intersectionPagerAdapter);
                selectedTabAdapter = intersectionPagerAdapter;
                tabLayout.setupWithViewPager(viewPager);
                tabLayout.setVisibility(View.VISIBLE);
                // default intersection fragment
                defaultRecentFragment = Constants.INTERSECTION_FRAGMENT.INTERSECTION_WAYS;
            } else {
                // set fragments for other point types (point, address, entrance, gps)
                PointPagerAdapter pointPagerAdapter = new PointPagerAdapter(this);
                viewPager.setAdapter(pointPagerAdapter);
                selectedTabAdapter = pointPagerAdapter;
                tabLayout.setVisibility(View.GONE);
                // default point fragment
                defaultRecentFragment = Constants.POINT_FRAGMENT.DETAILS;
            }

            if (savedInstanceState != null) {
            	recentFragment = savedInstanceState.getInt("recentFragment", defaultRecentFragment);
            } else {
                recentFragment = defaultRecentFragment;
            }
            setToolbarTitle(recentFragment);
            viewPager.setCurrentItem(recentFragment);
        }
    }

    @Override public boolean onMenuItemClick(MenuItem item) {
        PointUtility.putNewPoint(
                PointDetailsActivity.this, pointWrapper, item.getItemId());
        PlanRouteDialog.newInstance(false)
            .show(getSupportFragmentManager(), "PlanRouteDialog");
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
        }
    }

    private void setToolbarTitle(int tabIndex) {
        getSupportActionBar().setTitle(
                selectedTabAdapter.getPageTitle(tabIndex).toString());
    }


    /**
     * broadcast receiver
     */

    private BroadcastReceiver newLocationAndDirectionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            boolean updateDistanceAndBearingLabel = false;
            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)) {
                NewLocationAttributes newLocationAttributes = NewLocationAttributes.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_LOCATION_ATTRIBUTES));
                if (newLocationAttributes != null
                        && newLocationAttributes.getAggregatingDistanceThreshold().isAtLeast(DistanceThreshold.ZERO_METERS)) {
                    updateDistanceAndBearingLabel= true;
                }
                // point simulation switch
                if (pointWrapper != null
                        && buttonPointSimulateLocation != null) {
                    buttonPointSimulateLocation.setChecked(
                               positionManagerInstance.getSimulationEnabled()
                            && pointWrapper.equals(positionManagerInstance.getCurrentLocation()));
                }
            } else if (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)) {
                NewDirectionAttributes newDirectionAttributes = NewDirectionAttributes.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_DIRECTION_ATTRIBUTES));
                if (newDirectionAttributes != null
                        && newDirectionAttributes.getAggregatingBearingThreshold().isAtLeast(BearingThreshold.TEN_DEGREES)) {
                    updateDistanceAndBearingLabel= true;
                }
            }
            if (pointWrapper != null && updateDistanceAndBearingLabel) {
                labelPointDistanceAndBearing.setText(
                        String.format(
                            context.getResources().getString(R.string.labelPointDistanceAndBearing),
                            context.getResources().getQuantityString(
                                R.plurals.meter,
                                pointWrapper.distanceFromCurrentLocation(),
                                pointWrapper.distanceFromCurrentLocation()),
                            StringUtility.formatRelativeViewingDirection(
                                context, pointWrapper.bearingFromCurrentLocation()))
                        );
            }
        }
    };


    /**
     * select poi profiles for point
     */

    public static class SelectPOIProfilesForPointDialog extends DialogFragment implements ChildDialogCloseListener {

        private AccessDatabase accessDatabaseInstance;
        private PointWrapper selectedPoint;
        private TreeSet<Integer> checkedPOIProfileIds;
        private CheckBoxGroupView checkBoxGroupPOIProfiles;

        public static SelectPOIProfilesForPointDialog newInstance(PointWrapper selectedPoint) {
            SelectPOIProfilesForPointDialog selectPOIProfilesForPointDialogInstance = new SelectPOIProfilesForPointDialog();
            Bundle args = new Bundle();
            try {
                args.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, selectedPoint.toJson().toString());
            } catch (JSONException e) {
                args.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
            }
            selectPOIProfilesForPointDialogInstance.setArguments(args);
            return selectPOIProfilesForPointDialogInstance;
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
                selectedPoint = null;
            }

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_check_box_group, nullParent);

            checkBoxGroupPOIProfiles = (CheckBoxGroupView) view.findViewById(R.id.checkBoxGroup);
            if (selectedPoint == null) {
                SimpleMessageDialog dialog = SimpleMessageDialog.newInstance(
                        getResources().getString(R.string.messageErrorPointDataLoadingFailed));
                dialog.setTargetFragment(SelectPOIProfilesForPointDialog.this, 1);
                dialog.show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
            } else {
                if (savedInstanceState != null) {
                    checkedPOIProfileIds = new TreeSet<Integer>();
                    JSONArray jsonCheckedPOIProfileIdList = null;
                    try {
                        jsonCheckedPOIProfileIdList = new JSONArray(
                                savedInstanceState.getString("jsonCheckedPOIProfileIdList"));
                    } catch (JSONException e) {
                        jsonCheckedPOIProfileIdList = null;
                    } finally {
                        if (jsonCheckedPOIProfileIdList != null) {
                            for (int i=0; i<jsonCheckedPOIProfileIdList.length(); i++) {
                                try {
                                    checkedPOIProfileIds.add(jsonCheckedPOIProfileIdList.getInt(i));
                                } catch (JSONException e) {}
                            }
                        }
                    }
                } else {
                    checkedPOIProfileIds = accessDatabaseInstance.getCheckedProfileIdsForFavoritePoint(selectedPoint, true);
                }

                for (Map.Entry<Integer,String> profile : accessDatabaseInstance.getPOIProfileMap().entrySet()) {
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
                            checkedPOIProfileIds = getCheckedItemsOfPOIProfilesCheckBoxGroup();
                            onStart();
                        }
                    });
                    checkBoxGroupPOIProfiles.put(checkBox);
                }
            }

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectPOIProfilesForPointDialogName))
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
                for (CheckBox checkBox : checkBoxGroupPOIProfiles.getCheckBoxList()) {
                    checkBox.setChecked(
                            checkedPOIProfileIds.contains(checkBox.getId()));
                }

                // positive button
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        // remove unchecked profiles
                        TreeSet<Integer> poiProfileIdsToRemove = accessDatabaseInstance
                            .getCheckedProfileIdsForFavoritePoint(selectedPoint, true);
                        poiProfileIdsToRemove.removeAll(checkedPOIProfileIds);
                        for (Integer poiProfileIdToRemove : poiProfileIdsToRemove) {
                            accessDatabaseInstance.removeFavoritePointFromProfile(selectedPoint, poiProfileIdToRemove);
                        }
                        // add profiles
                        TreeSet<Integer> poiProfileIdsToAdd = new TreeSet<Integer>(checkedPOIProfileIds);
                        poiProfileIdsToAdd.removeAll(
                                accessDatabaseInstance.getCheckedProfileIdsForFavoritePoint(selectedPoint, true));
                        for (Integer poiProfileIdToAdd : poiProfileIdsToAdd) {
                            accessDatabaseInstance.addFavoritePointToProfile(selectedPoint, poiProfileIdToAdd);
                        }
                        dialog.dismiss();
                    }
                });

                // neutral button
                Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                if (checkBoxGroupPOIProfiles.nothingChecked()) {
                    buttonNeutral.setText(
                            getResources().getString(R.string.dialogAll));
                } else {
                    buttonNeutral.setText(
                            getResources().getString(R.string.dialogClear));
                }
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        checkedPOIProfileIds = new TreeSet<Integer>();
                        if (checkBoxGroupPOIProfiles.nothingChecked()) {
                            checkedPOIProfileIds.addAll(accessDatabaseInstance.getPOIProfileMap().keySet());
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
            JSONArray jsonCheckedPOIProfileIdList = new JSONArray();
            for (Integer id : getCheckedItemsOfPOIProfilesCheckBoxGroup()) {
                jsonCheckedPOIProfileIdList.put(id);
            }
            savedInstanceState.putString("jsonCheckedPOIProfileIdList", jsonCheckedPOIProfileIdList.toString());
        }

        @Override public void childDialogClosed() {
            dismiss();
        }

        private TreeSet<Integer> getCheckedItemsOfPOIProfilesCheckBoxGroup() {
            TreeSet<Integer> poiProfileList = new TreeSet<Integer>();
            for (CheckBox checkBox : checkBoxGroupPOIProfiles.getCheckedCheckBoxList()) {
                poiProfileList.add(checkBox.getId());
            }
            return poiProfileList;
        }
    }


    /**
     * fragment management
     */

	public class PointPagerAdapter extends FragmentPagerAdapter {

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


	public class IntersectionPagerAdapter extends FragmentPagerAdapter {

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


	public class POIPagerAdapter extends FragmentPagerAdapter {

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

	public class StationPagerAdapter extends FragmentPagerAdapter {

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

}
