package org.walkersguide.android.ui.fragment.tabs.object_details;

import org.walkersguide.android.data.object_with_id.HikingTrail;


import android.os.Bundle;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;



import org.walkersguide.android.R;
import androidx.fragment.app.Fragment;



public class HikingTrailDetailsFragment extends Fragment {


    // instance constructors

	public static HikingTrailDetailsFragment newInstance(HikingTrail hikingTrail) {
		HikingTrailDetailsFragment fragment = new HikingTrailDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_HIKING_TRAIL, hikingTrail);
        fragment.setArguments(args);
		return fragment;
	}


    // fragment
    private static final String KEY_HIKING_TRAIL = "hikingTrail";


    /**
     * create view
     */

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_hiking_trail_details, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        HikingTrail trail = (HikingTrail) getArguments().getSerializable(KEY_HIKING_TRAIL);
        if (trail != null) {

            TextView labelTrailName = (TextView) view.findViewById(R.id.labelTrailName);
            labelTrailName.setText(
                    String.format(
                        "%1$s: %2$s",
                        getResources().getString(R.string.labelTrailName),
                        trail.getName())
                    );

            TextView labelTrailDescription = (TextView) view.findViewById(R.id.labelTrailDescription);
            if (trail.getDescription() != null) {
                labelTrailDescription.setText(
                        String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.labelTrailDescription),
                            trail.getDescription())
                        );
                labelTrailDescription.setVisibility(View.VISIBLE);
            } else {
                labelTrailDescription.setVisibility(View.GONE);
            }

            TextView labelTrailLength = (TextView) view.findViewById(R.id.labelTrailLength);
            if (trail.getLength() != null) {
                labelTrailLength.setText(
                        String.format(
                            "%1$s: %2$s km",
                            getResources().getString(R.string.labelTrailLength),
                            trail.getLength())
                        );
                labelTrailLength.setVisibility(View.VISIBLE);
            } else {
                labelTrailLength.setVisibility(View.GONE);
            }

            TextView labelTrailSymbol = (TextView) view.findViewById(R.id.labelTrailSymbol);
            if (trail.getSymbol() != null) {
                labelTrailSymbol.setText(
                        String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.labelTrailSymbol),
                            trail.getSymbol())
                        );
                labelTrailSymbol.setVisibility(View.VISIBLE);
            } else {
                labelTrailSymbol.setVisibility(View.GONE);
            }

            // distances

            TextView labelDistanceToTrailStart = (TextView) view.findViewById(R.id.labelDistanceToTrailStart);
            labelDistanceToTrailStart.setText(
                    String.format(
                        "%1$s: %2$s",
                        getResources().getString(R.string.labelDistanceToTrailStart),
                        getResources().getQuantityString(
                            R.plurals.meter, trail.getDistanceToStart(), trail.getDistanceToStart()))
                    );

            TextView labelDistanceToTrailDestination = (TextView) view.findViewById(R.id.labelDistanceToTrailDestination);
            labelDistanceToTrailDestination.setText(
                    String.format(
                        "%1$s: %2$s",
                        getResources().getString(R.string.labelDistanceToTrailDestination),
                        getResources().getQuantityString(
                            R.plurals.meter, trail.getDistanceToDestination(), trail.getDistanceToDestination()))
                    );

            TextView labelDistanceToTrailClosest = (TextView) view.findViewById(R.id.labelDistanceToTrailClosest);
            labelDistanceToTrailClosest.setText(
                    String.format(
                        "%1$s: %2$s",
                        getResources().getString(R.string.labelDistanceToTrailClosest),
                        getResources().getQuantityString(
                            R.plurals.meter, trail.getDistanceToClosest(), trail.getDistanceToClosest()))
                    );
        }
    }

    @Override public void onResume() {
        super.onResume();
    }

    @Override public void onPause() {
        super.onPause();
    }

}
