package org.walkersguide.android.ui.fragment.segmentdetails;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.ui.activity.SegmentDetailsActivity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import org.walkersguide.android.data.basic.segment.Segment;
import org.walkersguide.android.data.basic.segment.Footway;
import org.walkersguide.android.data.basic.segment.IntersectionSegment;
import org.walkersguide.android.data.basic.segment.RouteSegment;
import org.walkersguide.android.util.Constants;
import android.text.TextUtils;
import org.walkersguide.android.helper.StringUtility;


public class SegmentDetailsFragment extends Fragment implements FragmentCommunicator {

    // constants
    private static final int TEXTVIEW_NO_AUTO_LINK = -1;
    private static final int TEXTVIEW_NO_ID = -1;

	// Store instance variables
    private SegmentWrapper segmentWrapper;

	// ui components
    private LinearLayout layoutAttributes;

	// newInstance constructor for creating fragment with arguments
	public static SegmentDetailsFragment newInstance(SegmentWrapper segmentWrapper) {
		SegmentDetailsFragment segmentDetailsFragmentInstance = new SegmentDetailsFragment();
        Bundle args = new Bundle();
        try {
            args.putString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, segmentWrapper.toJson().toString());
        } catch (JSONException e) {
            args.putString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "");
        }
        segmentDetailsFragmentInstance.setArguments(args);
		return segmentDetailsFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
			((SegmentDetailsActivity) activity).segmentDetailsFragmentCommunicator = this;
		}
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_point_details, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        try {
            segmentWrapper = new SegmentWrapper(
                    getActivity(), new JSONObject(getArguments().getString(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "")));
        } catch (JSONException e) {
            segmentWrapper = null;
        }

        // attributes layout
		layoutAttributes = (LinearLayout) view.findViewById(R.id.layoutAttributes);
    }

    @Override public void onFragmentEnabled() {
        layoutAttributes.removeAllViews();

        if (segmentWrapper != null
                && segmentWrapper.getSegment() instanceof Segment) {

            if (segmentWrapper.getSegment() instanceof Footway) {
                Footway footway = (Footway) segmentWrapper.getSegment();

                if (segmentWrapper.getSegment() instanceof IntersectionSegment) {
                    IntersectionSegment intersectionSegment = (IntersectionSegment) segmentWrapper.getSegment();
                    if (! TextUtils.isEmpty(intersectionSegment.getIntersectionName())) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                getResources().getString(R.string.labelSegmentIntersectionAttributesHeading),
                                true, TEXTVIEW_NO_AUTO_LINK);
                        addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelSegmentIntersectionName),
                                    intersectionSegment.getIntersectionName()),
                                false, TEXTVIEW_NO_AUTO_LINK);
                    }
                }

                if (segmentWrapper.getSegment() instanceof RouteSegment) {
                    RouteSegment routeSegment = (RouteSegment) segmentWrapper.getSegment();
                    if (routeSegment.getDistance() > 0) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                getResources().getString(R.string.labelSegmentRouteAttributesHeading),
                                true, TEXTVIEW_NO_AUTO_LINK);
                        addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$d",
                                    getResources().getString(R.string.labelSegmentRouteDistance),
                                    routeSegment.getDistance()),
                                false, TEXTVIEW_NO_AUTO_LINK);
                    }
                }

                // footway attributes
                if (! TextUtils.isEmpty(footway.getSurface())
                        || footway.getLanes() != -1
                        || footway.getSegregated() != -1
                        || footway.getSidewalk() != -1
                        || footway.getTram() != -1
                        || footway.getWidth() != -1.0) {
                    // heading
                   	addTextView(
                            TEXTVIEW_NO_ID,
                			getResources().getString(R.string.labelSegmentFootwayAttributesHeading),
                            true, TEXTVIEW_NO_AUTO_LINK);

                    // sidewalk
                    if (footway.getSidewalk() != -1) {
                        String sidewalkValue = null;
                        switch (footway.getSidewalk()) {
                            case Footway.SIDEWALK.NO:
                                sidewalkValue = getResources().getString(R.string.sidewalkNo);
                                break;
                            case Footway.SIDEWALK.LEFT:
                                sidewalkValue = getResources().getString(R.string.sidewalkLeft);
                                break;
                            case Footway.SIDEWALK.RIGHT:
                                sidewalkValue = getResources().getString(R.string.sidewalkRight);
                                break;
                            case Footway.SIDEWALK.BOTH:
                                sidewalkValue = getResources().getString(R.string.sidewalkBoth);
                                break;
                            default:
                                sidewalkValue = null;
                        }
                        if (sidewalkValue != null) {
                            addTextView(
                                    TEXTVIEW_NO_ID,
                                    String.format(
                                        "%1$s: %2$s",
                                        getResources().getString(R.string.labelSegmentFootwaySidewalk),
                                        sidewalkValue),
                                    false, TEXTVIEW_NO_AUTO_LINK);
                        }
                    }

                    // surface, width and number of lanes
                    // surface
                    if (! TextUtils.isEmpty(footway.getSurface())) {
                       	addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$s",
            	    		        getResources().getString(R.string.labelSegmentFootwaySurface),
                                    footway.getSurface()),
                                false, TEXTVIEW_NO_AUTO_LINK);
                    }
                    // width
                    if (footway.getWidth() != -1.0) {
                       	addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$.1f",
            	    		        getResources().getString(R.string.labelSegmentFootwayWidth),
                                    footway.getWidth()),
                                false, TEXTVIEW_NO_AUTO_LINK);
                    }
                    // number of lanes
                    if (footway.getLanes() != -1) {
                       	addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$d",
            	    		        getResources().getString(R.string.labelSegmentFootwayNumberOfLanes),
                                    footway.getLanes()),
                                false, TEXTVIEW_NO_AUTO_LINK);
                    }

                    // segregated and tram
                    // cycleway segregated
                    if (footway.getSegregated() != -1) {
                        String segregatedValue = null;
                        switch (footway.getSegregated()) {
                            case Footway.SEGREGATED.NO:
                                segregatedValue = getResources().getString(R.string.dialogNo);
                                break;
                            case Footway.SEGREGATED.YES:
                                segregatedValue = getResources().getString(R.string.dialogYes);
                                break;
                            default:
                                segregatedValue = null;
                        }
                        if (segregatedValue != null) {
                            addTextView(
                                    TEXTVIEW_NO_ID,
                                    String.format(
                                        "%1$s: %2$s",
                                        getResources().getString(R.string.labelSegmentFootwaySegregated),
                                        segregatedValue),
                                    false, TEXTVIEW_NO_AUTO_LINK);
                        }
                    }
                    // tram rails
                    if (footway.getTram() != -1) {
                        String tramValue = null;
                        switch (footway.getTram()) {
                            case Footway.TRAM.NO:
                                tramValue = getResources().getString(R.string.dialogNo);
                                break;
                            case Footway.TRAM.YES:
                                tramValue = getResources().getString(R.string.dialogYes);
                                break;
                            default:
                                tramValue = null;
                        }
                        if (tramValue != null) {
                            addTextView(
                                    TEXTVIEW_NO_ID,
                                    String.format(
                                        "%1$s: %2$s",
                                        getResources().getString(R.string.labelSegmentFootwayTram),
                                        tramValue),
                                    false, TEXTVIEW_NO_AUTO_LINK);
                        }
                    }
                }

                // accessibility attributes
                if (footway.getTactilePaving() != -1
                        || footway.getWheelchair() != -1) {
                    addTextView(
                            TEXTVIEW_NO_ID,
                            getResources().getString(R.string.labelPointAccessibilityHeading),
                            true, TEXTVIEW_NO_AUTO_LINK);
                    // tactile paving
                    String tactilePavingValue = null;
                    switch (footway.getTactilePaving()) {
                        case Footway.TACTILE_PAVING.NO:
                            tactilePavingValue = getResources().getString(R.string.dialogNo);
                            break;
                        case Footway.TACTILE_PAVING.YES:
                            tactilePavingValue = getResources().getString(R.string.dialogYes);
                            break;
                        default:
                            tactilePavingValue = null;
                    }
                    if (tactilePavingValue != null) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointTactilePaving),
                                    tactilePavingValue),
                                false, TEXTVIEW_NO_AUTO_LINK);
                    }
                    // wheelchair
                    String wheelchairValue = null;
                    switch (footway.getWheelchair()) {
                        case Footway.WHEELCHAIR.NO:
                            wheelchairValue = getResources().getString(R.string.dialogNo);
                            break;
                        case Footway.WHEELCHAIR.LIMITED:
                            wheelchairValue = getResources().getString(R.string.dialogLimited);
                            break;
                        case Footway.WHEELCHAIR.YES:
                            wheelchairValue = getResources().getString(R.string.dialogYes);
                            break;
                        default:
                            wheelchairValue = null;
                    }
                    if (wheelchairValue != null) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointWheelchair),
                                    wheelchairValue),
                                false, TEXTVIEW_NO_AUTO_LINK);
                    }
                }

                if (! TextUtils.isEmpty(footway.getUserDescription())) {
                    // user attributes heading
                    addTextView(
                            TEXTVIEW_NO_ID,
                            getResources().getString(R.string.labelSegmentUserAttributesHeading),
                            true, TEXTVIEW_NO_AUTO_LINK);
                    // user description
                    addTextView(
                            TEXTVIEW_NO_ID,
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelSegmentUserDescription),
                                footway.getUserDescription()),
                            false, TEXTVIEW_NO_AUTO_LINK);
                }
            }
                }
    }

	@Override public void onFragmentDisabled() {
    }

    private void addTextView(int id, String text, boolean isHeading, int autoLink) {
    	TextView label = new TextView(getActivity());
        // set id
        if (id != TEXTVIEW_NO_ID) {
            label.setId(id);
        }
        // layout params
        LayoutParams lp = new LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (isHeading && layoutAttributes.getChildCount() > 0) {
            ((MarginLayoutParams) lp).topMargin = (int) (10 * getResources().getDisplayMetrics().density);
        }
    	label.setLayoutParams(lp);
        // text
        if (isHeading) {
            label.setText(StringUtility.boldAndRed(text));
        } else {
            label.setText(text);
        }
        // auto link param
        if (autoLink != TEXTVIEW_NO_AUTO_LINK) {
            label.setAutoLinkMask(autoLink);
        }
        // focusable
		label.setFocusable(true);
        // add to attributes layout
    	layoutAttributes.addView(label);
    }

}
