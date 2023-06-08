package org.walkersguide.android.ui.fragment.details;

import android.os.Bundle;

import android.text.TextUtils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import org.walkersguide.android.ui.view.builder.TextViewBuilder;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.data.object_with_id.segment.RouteSegment;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.R;

import android.content.Intent;
import android.net.Uri;
import java.util.Locale;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import org.walkersguide.android.util.GlobalInstance;


public class SegmentDetailsFragment extends Fragment {
    private static final String KEY_SEGMENT = "segment";

    private static final String OSM_WAY_URL = "https://www.openstreetmap.org/way/%1$d/";

	// Store instance variables
    private Segment segment;

	// ui components
    private LinearLayout layoutAttributes;

	// newInstance constructor for creating fragment with arguments
	public static SegmentDetailsFragment newInstance(Segment segment) {
		SegmentDetailsFragment fragment = new SegmentDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SEGMENT, segment);
        fragment.setArguments(args);
		return fragment;
	}


    /**
     * create view
     */


	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
		return inflater.inflate(R.layout.layout_single_linear_layout, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        segment = (Segment) getArguments().getSerializable(KEY_SEGMENT);

        // attributes layout
		layoutAttributes = (LinearLayout) view.findViewById(R.id.linearLayout);
    }


    /**
     * menu
     */

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_point_and_segment_details_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public void onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItemOpenOsmWebsite = menu.findItem(R.id.menuItemOpenOsmWebsite);
        if (segment != null && segment.getOsmId() != null) {
            menuItemOpenOsmWebsite.setVisible(true);
        } else {
            menuItemOpenOsmWebsite.setVisible(false);
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemOpenOsmWebsite) {
            Intent openBrowserIntent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        String.format(Locale.ROOT, OSM_WAY_URL, segment.getOsmId())));
            getActivity().startActivity(openBrowserIntent);
            return true;
        } else {
            return false;
        }
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
    }

    @Override public void onResume() {
        super.onResume();
        layoutAttributes.removeAllViews();
        if (segment == null) {
            return;
        }

        if (segment instanceof IntersectionSegment) {
            IntersectionSegment intersectionSegment = (IntersectionSegment) segment;
            // heading
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                            getResources().getString(R.string.labelSegmentIntersectionAttributesHeading))
                        .isHeading()
                        .create()
                    );
            // intersection name
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelSegmentIntersectionName),
                                intersectionSegment.getIntersectionName()))
                        .create()
                    );

        } else if (segment instanceof RouteSegment) {
            RouteSegment routeSegment = (RouteSegment) segment;
            // heading
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                            getResources().getString(R.string.labelSegmentRouteAttributesHeading))
                        .isHeading()
                        .create()
                    );
            // segment length
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelSegmentRouteDistance),
                                GlobalInstance.getPluralResource(R.plurals.meter, routeSegment.getDistance())))
                        .create()
                    );
        }

        // all the rest
        //
        // heading
        layoutAttributes.addView(
                new TextViewBuilder(
                        SegmentDetailsFragment.this.getContext(),
                        getResources().getString(R.string.labelSegmentFootwayAttributesHeading))
                    .addTopMargin()
                    .isHeading()
                    .create()
                );

        // bearing
        layoutAttributes.addView(
                new TextViewBuilder(
                        SegmentDetailsFragment.this.getContext(),
                        String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.labelSegmentBearing),
                            segment.getBearing()))
                    .create()
                );

        // description
        if (segment.getDescription() != null) {
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelSegmentFootwayDescription),
                                segment.getDescription()))
                        .create()
                    );
        }

        // sidewalk
        if (segment.getSidewalk() != null) {
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelSegmentFootwaySidewalk),
                                segment.getSidewalk().toString()))
                        .create()
                    );
        }

        // surface
        if (! TextUtils.isEmpty(segment.getSurface())) {
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelSegmentFootwaySurface),
                                segment.getSurface()))
                        .create()
                    );
        }

        // smoothness
        if (segment.getSmoothness() != null) {
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelSegmentFootwaySmoothness),
                                segment.getSmoothness()))
                        .create()
                    );
        }

        // width
        if (segment.getWidth() != null) {
            int quantity = 0;
            if ((segment.getWidth() % 10) == 0
                        && (segment.getWidth() / 10) == 1) {
                quantity = 1;
            }
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelSegmentFootwayWidth),
                                getResources().getQuantityString(
                                    R.plurals.meterFloat, quantity, segment.getWidth())))
                        .create()
                    );
        }

        // lanes
        if (segment.getLanes() != null) {
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelSegmentFootwayNumberOfLanes),
                                segment.getLanes()))
                        .create()
                    );
        }

        // max speed
        if (segment.getMaxSpeed() != null) {
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelSegmentFootwayMaxSpeed),
                                segment.getMaxSpeed()))
                        .create()
                    );
        }

        // segregated cycleway
        if (segment.getSegregated() != null) {
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelSegmentFootwaySegregated),
                                segment.getSegregated()
                                ? getResources().getString(R.string.dialogYes)
                                : getResources().getString(R.string.dialogNo)))
                        .create()
                    );
        }

        // tram rails
        if (segment.getTramOnStreet() != null) {
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelSegmentFootwayTram),
                                segment.getTramOnStreet()
                                ? getResources().getString(R.string.dialogYes)
                                : getResources().getString(R.string.dialogNo)))
                        .create()
                    );
        }

        // accessibility attributes
        if (segment.getTactilePaving() != null
                || segment.getWheelchair() != null) {
            layoutAttributes.addView(
                    new TextViewBuilder(
                            SegmentDetailsFragment.this.getContext(),
                			getResources().getString(R.string.labelPointAccessibilityHeading))
                        .isHeading()
                        .addTopMargin()
                        .create()
                    );

            // tactile paving
            if (segment.getTactilePaving() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                SegmentDetailsFragment.this.getContext(),
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointTactilePaving),
                                    segment.getTactilePaving().toString()))
                            .create()
                        );
            }

            // wheelchair
            if (segment.getWheelchair() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                SegmentDetailsFragment.this.getContext(),
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointWheelchair),
                                    segment.getWheelchair().toString()))
                            .create()
                        );
            }
        }
    }

}
