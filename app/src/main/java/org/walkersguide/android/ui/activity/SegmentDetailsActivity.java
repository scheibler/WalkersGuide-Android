package org.walkersguide.android.ui.activity;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Handler;

import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;

import android.text.InputType;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.segment.Footway;
import org.walkersguide.android.data.basic.segment.IntersectionSegment;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.data.sensor.attribute.NewDirectionAttributes;
import org.walkersguide.android.data.sensor.Direction;
import org.walkersguide.android.data.sensor.threshold.BearingThreshold;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.ui.fragment.segmentdetails.NextIntersectionsFragment;
import org.walkersguide.android.ui.fragment.segmentdetails.SegmentDetailsFragment;
import org.walkersguide.android.util.Constants;


public class SegmentDetailsActivity extends AbstractActivity {

	// instance variables
    private DirectionManager directionManagerInstance;
    private SegmentWrapper segmentWrapper;

    // activity ui components
	private ViewPager viewPager;
    private FragmentPagerAdapter selectedTabAdapter;
    private TabLayout tabLayout;
    private int recentFragment;

    private TextView labelSegmentDirection;
    private Switch buttonSegmentExcludeFromRouting, buttonSegmentSimulateDirection;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_segment_details);
        directionManagerInstance = DirectionManager.getInstance(this);

        // load segment
        try {
    		if (savedInstanceState != null) {
                segmentWrapper = new SegmentWrapper(
                        this, new JSONObject(savedInstanceState.getString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED)));
            } else {
                segmentWrapper = new SegmentWrapper(
		                this, new JSONObject(getIntent().getExtras().getString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "")));
            }
        } catch (JSONException e) {
            segmentWrapper = null;
        }

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(
                getResources().getString(R.string.segmentDetailsActivityTitle));

    	LinearLayout layoutFootwaySpecific = (LinearLayout) findViewById(R.id.layoutFootwaySpecific);
        layoutFootwaySpecific.setVisibility(View.GONE);

        if (segmentWrapper != null) {
            // name and subtype
    		TextView labelSegmentName = (TextView) findViewById(R.id.labelSegmentName);
            labelSegmentName.setText(
                    String.format(
                        getResources().getString(R.string.labelSegmentName),
                        segmentWrapper.getSegment().getName())
                    );
    		TextView labelSegmentType = (TextView) findViewById(R.id.labelSegmentType);
            labelSegmentType.setText(
                    String.format(
                        getResources().getString(R.string.labelSegmentType),
                        segmentWrapper.getSegment().getSubType())
                    );

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

            // load or hide bearing labels and switches
            if (segmentWrapper.getSegment() instanceof Footway) {
                layoutFootwaySpecific.setVisibility(View.VISIBLE);
                // direction label
        		labelSegmentDirection = (TextView) findViewById(R.id.labelSegmentDirection);

                // exclude from routing
        		buttonSegmentExcludeFromRouting = (Switch) findViewById(R.id.buttonSegmentExcludeFromRouting);
                buttonSegmentExcludeFromRouting.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                        boolean isExcluded = accessDatabaseInstance.getExcludedWaysList().contains(segmentWrapper);
                        if (isChecked && ! isExcluded) {
                            SetNameForExcludedWayDialog.newInstance(segmentWrapper).show(
                                    getSupportFragmentManager(), "SetNameForExcludedWayDialog");
                        } else if (! isChecked && isExcluded) {
                            accessDatabaseInstance.removeExcludedWaySegment(segmentWrapper);
                        }
                    }
                });

                // simulate direction
        		buttonSegmentSimulateDirection = (Switch) findViewById(R.id.buttonSegmentSimulateDirection);
                buttonSegmentSimulateDirection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                        boolean isSimulated =
                               directionManagerInstance.getSimulationEnabled()
                            && directionManagerInstance.getCurrentDirection() != null
                            && directionManagerInstance.getCurrentDirection().getBearing() == ((Footway) segmentWrapper.getSegment()).getBearing();
                        if (isChecked && ! isSimulated) {
                            directionManagerInstance.setSimulatedDirection(
                                    new Direction.Builder(
                                        SegmentDetailsActivity.this, ((Footway) segmentWrapper.getSegment()).getBearing())
                                    .build());
                            directionManagerInstance.setSimulationEnabled(true);
                        } else if (! isChecked && isSimulated) {
                            directionManagerInstance.setSimulationEnabled(false);
                        }
                    }
                });
            }

            int defaultRecentFragment;
            if (segmentWrapper.getSegment() instanceof IntersectionSegment) {
                // set fragments for intersection segment
                IntersectionSegmentPagerAdapter intersectionSegmentPagerAdapter = new IntersectionSegmentPagerAdapter(this);
                viewPager.setAdapter(intersectionSegmentPagerAdapter);
                selectedTabAdapter = intersectionSegmentPagerAdapter;
                tabLayout.setupWithViewPager(viewPager);
                tabLayout.setVisibility(View.VISIBLE);
                // default intersection segment fragment
                defaultRecentFragment = Constants.INTERSECTION_SEGMENT_FRAGMENT.DETAILS;
            } else {
                // set fragments for other segment types
                SegmentPagerAdapter segmentPagerAdapter = new SegmentPagerAdapter(this);
                viewPager.setAdapter(segmentPagerAdapter);
                selectedTabAdapter = segmentPagerAdapter;
                tabLayout.setVisibility(View.GONE);
                // default fragment for other segments
                defaultRecentFragment = Constants.SEGMENT_FRAGMENT.DETAILS;
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

	@Override public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
        if (segmentWrapper != null) {
            try {
                savedInstanceState.putString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, segmentWrapper.toJson().toString());
            } catch (JSONException e) {
    	    	savedInstanceState.putString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "");
            }
        } else {
    	    savedInstanceState.putString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "");
        }
    	savedInstanceState.putInt("recentFragment", recentFragment);
	}

    @Override public void onPause() {
        super.onPause();
        if (segmentWrapper != null) {
            if (segmentWrapper.getSegment() instanceof Footway) {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(newLocationAndDirectionReceiver);
            }
        }
    }

    @Override public void onResume() {
        super.onResume();
        if (segmentWrapper != null) {
            if (buttonSegmentExcludeFromRouting != null) {
                buttonSegmentExcludeFromRouting.setChecked(
                        accessDatabaseInstance.getExcludedWaysList().contains(segmentWrapper));
            }
            if (segmentWrapper.getSegment() instanceof Footway) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(Constants.ACTION_NEW_DIRECTION);
                LocalBroadcastManager.getInstance(this).registerReceiver(newLocationAndDirectionReceiver, filter);
                // request current direction
                directionManagerInstance.requestCurrentDirection();
            }
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
            if (intent.getAction().equals(Constants.ACTION_NEW_DIRECTION)) {
                NewDirectionAttributes newDirectionAttributes = NewDirectionAttributes.fromString(
                        context, intent.getStringExtra(Constants.ACTION_NEW_DIRECTION_ATTRIBUTES));
                if (newDirectionAttributes != null
                        && (
                               labelSegmentDirection.getText().toString().trim().equals("")
                            || newDirectionAttributes.getAggregatingBearingThreshold().isAtLeast(BearingThreshold.TEN_DEGREES))
                        ) {
                    // update direction label
                    labelSegmentDirection.setText(
                            String.format(
                                context.getResources().getString(R.string.labelSegmentDirection),
                                StringUtility.formatRelativeViewingDirection(
                                    context, ((Footway) segmentWrapper.getSegment()).bearingFromCurrentDirection()))
                            );
                    if (segmentWrapper != null
                            && buttonSegmentSimulateDirection != null) {
                        buttonSegmentSimulateDirection.setChecked(
                                   directionManagerInstance.getSimulationEnabled()
                                && directionManagerInstance.getCurrentDirection() != null
                                && directionManagerInstance.getCurrentDirection().getBearing() == ((Footway) segmentWrapper.getSegment()).getBearing());
                    }
                }
            }
        }
    };


    public static class SetNameForExcludedWayDialog extends DialogFragment {

        // Store instance variables
        private AccessDatabase accessDatabaseInstance;
        private InputMethodManager imm;
        private SegmentWrapper segmentWrapper;
        private EditText editSegmentDescription;

        public static SetNameForExcludedWayDialog newInstance(SegmentWrapper segmentWrapper) {
            SetNameForExcludedWayDialog setNameForExcludedWayDialog = new SetNameForExcludedWayDialog();
            Bundle args = new Bundle();
            try {
                args.putString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, segmentWrapper.toJson().toString());
            } catch (JSONException e) {
                args.putString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "");
            }
            setNameForExcludedWayDialog.setArguments(args);
            return setNameForExcludedWayDialog;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            try {
                segmentWrapper = new SegmentWrapper(
                        getActivity(), new JSONObject(getArguments().getString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "")));
            } catch (JSONException e) {
                segmentWrapper = null;
            }

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_edit_text, nullParent);

            editSegmentDescription = (EditText) view.findViewById(R.id.editInput);
            if (segmentWrapper != null) {
                editSegmentDescription.setText(segmentWrapper.getSegment().getName());
            }
            editSegmentDescription.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editSegmentDescription.setInputType(InputType.TYPE_CLASS_TEXT);
            editSegmentDescription.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        tryToExcludeWay();
                        return true;
                    }
                    return false;
                }
            });

            ImageButton buttonDelete = (ImageButton) view.findViewById(R.id.buttonDelete);
            buttonDelete.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    // clear edit text
                    editSegmentDescription.setText("");
                    // show keyboard
                    imm.showSoftInput(editSegmentDescription, InputMethodManager.SHOW_IMPLICIT);
                }
            });

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.setNameForExcludedWayDialogTitle))
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogExclude),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogCancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
            .setOnKeyListener(
                    new Dialog.OnKeyListener() {
                        @Override public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                close();
                                return true;
                            }
                            return false;
                        }
                    })
                .create();
        }

        @Override public void onStart() {
            super.onStart();
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                // dismiss immediately if segmentWrapper is null
                if (segmentWrapper == null) {
                    dialog.dismiss();
                }
                // positive button
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        tryToExcludeWay();
                    }
                });
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        close();
                    }
                });
            }
            // show keyboard
            new Handler().postDelayed(
                    new Runnable() {
                        @Override public void run() {
                            imm.showSoftInput(editSegmentDescription, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }, 50);
        }

        private void tryToExcludeWay() {
            String segmentDescription = editSegmentDescription.getText().toString().trim();
            if (! segmentDescription.equals("")
                    && ! segmentDescription.equals(segmentWrapper.getSegment().getName())) {
                try {
                    // add user description
                    JSONObject jsonSegmentWrapper = segmentWrapper.toJson();
                    jsonSegmentWrapper.put("user_description", segmentDescription);
                    // add to database
                    accessDatabaseInstance.addExcludedWaySegment(
                            new SegmentWrapper(getActivity(), jsonSegmentWrapper));
                } catch (JSONException e) {}
            } else {
                // add to database without modification
                accessDatabaseInstance.addExcludedWaySegment(segmentWrapper);
            }
            close();
        }

        private void close() {
            // reload ui and dismiss
            Intent intent = new Intent(Constants.ACTION_UPDATE_UI);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            dismiss();
        }
    }


    /**
     * fragment management
     */

	public class SegmentPagerAdapter extends FragmentPagerAdapter {

		public SegmentPagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case Constants.SEGMENT_FRAGMENT.DETAILS:
                    return SegmentDetailsFragment.newInstance(segmentWrapper);
                default:
                    return null;
            }
        }

		@Override public CharSequence getPageTitle(int position) {
            switch (position) {
                case Constants.SEGMENT_FRAGMENT.DETAILS:
    				return getResources().getString(R.string.fragmentSegmentDetailsName);
                default:
                    return "";
            }
		}

		@Override public int getCount() {
			return Constants.SegmentFragmentValueArray.length;
		}
	}


	public class IntersectionSegmentPagerAdapter extends FragmentPagerAdapter {

		public IntersectionSegmentPagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

        @Override public Fragment getItem(int position) {
            switch (position) {
                case Constants.INTERSECTION_SEGMENT_FRAGMENT.DETAILS:
                    return SegmentDetailsFragment.newInstance(segmentWrapper);
                case Constants.INTERSECTION_SEGMENT_FRAGMENT.NEXT_INTERSECTIONS:
                    return NextIntersectionsFragment.newInstance(segmentWrapper);
                default:
                    return null;
            }
        }

		@Override public CharSequence getPageTitle(int position) {
            switch (position) {
                case Constants.INTERSECTION_SEGMENT_FRAGMENT.DETAILS:
    				return getResources().getString(R.string.fragmentSegmentDetailsName);
                case Constants.INTERSECTION_SEGMENT_FRAGMENT.NEXT_INTERSECTIONS:
    				return getResources().getString(R.string.fragmentNextIntersectionsName);
                default:
                    return "";
            }
		}

		@Override public int getCount() {
			return Constants.IntersectionSegmentFragmentValueArray.length;
		}
	}

}
