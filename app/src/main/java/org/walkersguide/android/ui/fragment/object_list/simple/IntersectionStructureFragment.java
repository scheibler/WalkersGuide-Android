package org.walkersguide.android.ui.fragment.object_list.simple;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
    import org.walkersguide.android.ui.view.ObjectWithIdView;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.Angle;
import org.walkersguide.android.tts.TTSWrapper;
import org.walkersguide.android.sensor.bearing.AcceptNewBearing;
import org.walkersguide.android.ui.fragment.object_list.SimpleObjectListFragment;
import org.walkersguide.android.ui.adapter.ObjectWithIdAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;




import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.ObjectWithId;
import java.util.ArrayList;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import timber.log.Timber;
import android.view.Menu;
import android.view.MenuItem;
import org.walkersguide.android.data.angle.RelativeBearing.Direction;
import androidx.annotation.NonNull;
import android.view.MenuInflater;
import org.walkersguide.android.data.object_with_id.Segment;
import androidx.core.view.MenuProvider;
import android.os.Bundle;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import java.util.Collections;
import android.view.View;
import android.view.ViewGroup;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.ui.view.UserAnnotationView;


public class IntersectionStructureFragment extends SimpleObjectListFragment implements MenuProvider {
    private static final String KEY_INTERSECTION = "intersection";
    private static final String KEY_ANNOUNCE_WAY_AHEAD = "announceWayAhead";

    public static IntersectionStructureFragment embedded(Intersection intersection) {
        IntersectionStructureFragment fragment = new IntersectionStructureFragment();
        Bundle bundle = new SimpleObjectListFragment.BundleBuilder(
                intersection.getSegmentList())
            .setIsEmbedded(true)
            .build();
        bundle.putSerializable(KEY_INTERSECTION, intersection);
        bundle.putBoolean(KEY_ANNOUNCE_WAY_AHEAD, true);
        fragment.setArguments(bundle);
        return fragment;
    }

    private Intersection intersection;
    private boolean announceWayAhead;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intersection = (Intersection) getArguments().getSerializable(KEY_INTERSECTION);
        if (savedInstanceState != null) {
            announceWayAhead = savedInstanceState.getBoolean(KEY_ANNOUNCE_WAY_AHEAD);
        } else {
            announceWayAhead = getArguments().getBoolean(KEY_ANNOUNCE_WAY_AHEAD);
        }
    }

    @Override public View configureView(View view, Bundle savedInstanceState) {
        view = super.configureView(view, savedInstanceState);
        LinearLayout layoutRuntimeSubViews = (LinearLayout) view.findViewById(R.id.layoutRuntimeSubViews);

        UserAnnotationView layoutUserAnnotation = new UserAnnotationView(getActivity());
        layoutUserAnnotation.setObjectWithId(intersection);
        layoutRuntimeSubViews.addView(layoutUserAnnotation);
        layoutRuntimeSubViews.setVisibility(View.VISIBLE);

        return view;
    }

    @Override public String getTitle() {
        return getSelectObjectWithId()
            ? getResources().getString(R.string.labelPleaseSelect)
            : getResources().getString(R.string.fragmentIntersectionStructureName);
    }

    @Override public int getPluralResourceId() {
        return R.plurals.street;
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(KEY_ANNOUNCE_WAY_AHEAD,  announceWayAhead);
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_object_list_intersection_structure_fragment, menu);
    }

    @Override public void onPrepareMenu(@NonNull Menu menu) {
        MenuItem menuItemAnnounceWayAhead = menu.findItem(R.id.menuItemAnnounceWayAhead);
        menuItemAnnounceWayAhead.setChecked(announceWayAhead);
    }

    @Override public boolean onMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemAnnounceWayAhead) {
            announceWayAhead = ! announceWayAhead;
        } else {
            return false;
        }
        return true;
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
        LocalBroadcastManager
            .getInstance(getActivity())
            .registerReceiver(mMessageReceiver, filter);
    }

    @Override public boolean  showObjectList() {
        super.populateUiAfterRequestWasSuccessful(
                null,
                new IntersectionSegmentAdapter(
                    IntersectionStructureFragment.this.getContext(),
                    intersection.getSegmentList()));
        return true;
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        private ArrayList<Direction> lastDirectionFromCurrentLocationList = null;
        private IntersectionSegment lastClosestSegment = null;

        @Override public void onReceive(Context context, Intent intent) {
            if (getDialog() == null && ! getActivity().hasWindowFocus()) {
                return;
            }

            if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                ObjectWithIdAdapter listAdapter = getListAdapter();
                Bearing currentBearing = (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (! (listAdapter instanceof IntersectionSegmentAdapter) || currentBearing == null) {
                    return;
                }

                // notify list changed
                if (getAutoUpdate()) {
                    ArrayList<Direction> currentDirectionFromCurrentLocationList = ((IntersectionSegmentAdapter) listAdapter)
                        .getDirectionFromCurrentLocationList();
                    if (! Helper.compareArrayLists(lastDirectionFromCurrentLocationList, currentDirectionFromCurrentLocationList)) {
                        lastDirectionFromCurrentLocationList = currentDirectionFromCurrentLocationList;
                        listAdapter.notifyDataSetChanged();
                    }
                }

                // find closest way and announce
                IntersectionSegment closestSegment = intersection.findClosestIntersectionSegmentTo(currentBearing, 3);
                if (closestSegment != null
                        && ! closestSegment.equals(lastClosestSegment)) {
                    lastClosestSegment = closestSegment;

                    String message = closestSegment.formatNameAndSubType();
                    if (closestSegment.isPartOfNextRouteSegment()) {
                        message += String.format(
                                ", %1$s", GlobalInstance.getStringResource(R.string.labelPartOfNextRouteSegment));
                    }

                    if (announceWayAhead) {
                        TTSWrapper.getInstance().screenReader(message);
                    }
                }
            }
        }
    };


    public static class IntersectionSegmentAdapter extends ObjectWithIdAdapter {

        private ArrayList<IntersectionSegment> intersectionSegmentList;

        public IntersectionSegmentAdapter(Context context, ArrayList<IntersectionSegment> intersectionSegmentList) {
            super(context, intersectionSegmentList, null, null, true, false);
            this.intersectionSegmentList = intersectionSegmentList;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            ObjectWithIdView layoutObject = null;
            if (convertView == null) {
                layoutObject = new ObjectWithIdView(super.getContext());
                layoutObject.setLayoutParams(
                        new LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            } else {
                layoutObject = (ObjectWithIdView) convertView;
            }

            IntersectionSegment intersectionSegment = this.intersectionSegmentList.get(position);
            layoutObject.setAutoUpdate(true);
            layoutObject.configureAsListItem(
                    intersectionSegment,
                    intersectionSegment.getRelativeBearingFromCurrentLocation() != null
                    ? String.format(
                        "%1$s: %2$s",
                        intersectionSegment.getRelativeBearingFromCurrentLocation().getDirection().toString(),
                        intersectionSegment.toString())
                    : intersectionSegment.toString());
            return layoutObject;
        }

        @Override public void notifyDataSetChanged() {
            Collections.sort(
                    intersectionSegmentList,
                    // bearing offset = 22 -> sort the ways, which are slightly to the left of the user, to the top of the list
                    Segment.SortByBearingRelativeTo.currentBearing(
                        Angle.Quadrant.Q1.max, true));
            super.updateObjectList(intersectionSegmentList);
            super.notifyDataSetChanged();
        }

        public ArrayList<Direction> getDirectionFromCurrentLocationList() {
            ArrayList<Direction> directionFromCurrentLocationList = new ArrayList<Direction>();
            for (IntersectionSegment intersectionSegment : this.intersectionSegmentList) {
                directionFromCurrentLocationList.add(
                        intersectionSegment.getRelativeBearingFromCurrentLocation().getDirection());
            }
            return directionFromCurrentLocationList;
        }
    }

}
