package org.walkersguide.android.ui.fragment.tabs.details;

import org.walkersguide.android.ui.activity.toolbar.FragmentContainerActivity;
import androidx.fragment.app.Fragment;
import org.walkersguide.android.ui.view.builder.TextViewBuilder;


import android.os.Bundle;

import android.text.TextUtils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.LinearLayout;
import android.widget.TextView;



import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.Entrance;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.data.object_with_id.point.Intersection;
import org.walkersguide.android.data.object_with_id.point.PedestrianCrossing;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.POI;
import org.walkersguide.android.data.object_with_id.point.PointWithAddressData;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.poi.Station;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.poi.station.Line;
import org.walkersguide.android.R;
import android.content.Intent;
import android.net.Uri;
import java.util.Locale;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import timber.log.Timber;
import org.walkersguide.android.ui.UiHelper;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.annotation.NonNull;


public class PointDetailsFragment extends Fragment implements MenuProvider {
    private static final String KEY_POINT = "point";

    private static final String OSM_NODE_URL = "https://www.openstreetmap.org/node/%1$d/";

	// Store instance variables
    private Point point;

	// ui components
    private LinearLayout layoutAttributes;

	// newInstance constructor for creating fragment with arguments
	public static PointDetailsFragment newInstance(Point point) {
		PointDetailsFragment fragment = new PointDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_POINT, point);
        fragment.setArguments(args);
		return fragment;
	}


    /**
     * create view
     */

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.layout_single_linear_layout, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        point = (Point) getArguments().getSerializable(KEY_POINT);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        // attributes layout
		layoutAttributes = (LinearLayout) view.findViewById(R.id.linearLayout);
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_point_and_segment_details_fragment, menu);
    }

    @Override public void onPrepareMenu(@NonNull Menu menu) {
        MenuItem menuItemOpenOsmWebsite = menu.findItem(R.id.menuItemOpenOsmWebsite);
        menuItemOpenOsmWebsite.setVisible(
                point != null && point.getOsmId() != null);
    }

    @Override public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemOpenOsmWebsite) {
            Intent openBrowserIntent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        String.format(Locale.ROOT, OSM_NODE_URL, point.getOsmId())));
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
        if (point == null) {
            return;
        }

        if (point instanceof PointWithAddressData) {
            PointWithAddressData pointWithAddressData = null;

            if (point instanceof Entrance) {
                pointWithAddressData = (Entrance) point;
                Entrance entrance = (Entrance) point;

                if (entrance.getLabel() != null) {
                    // heading
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                    getResources().getString(R.string.labelPointEntranceAttributesHeading))
                                .isHeading()
                                .create()
                            );

                    // label
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                    PointDetailsFragment.this.getContext(),
                                    String.format(
                                        "%1$s: %2$s",
                                        getResources().getString(R.string.labelPointEntranceLabel),
                                        entrance.getLabel()))
                                .create()
                            );
                }

            } else if (point instanceof StreetAddress) {
                pointWithAddressData = (StreetAddress) point;

            } else if (point instanceof POI) {
                POI poi = (POI) point;

                if (point instanceof Station) {
                    Station station = (Station) point;

                    if (       station.getLocalRef() != null
                            || station.getNetwork() != null
                            || station.getOperator() != null
                            || ! station.getVehicleList().isEmpty()) {
                        layoutAttributes.addView(
                                new TextViewBuilder(
                                        PointDetailsFragment.this.getContext(),
                                        getResources().getString(R.string.labelPointStationAttributesHeading))
                                    .isHeading()
                                    .create()
                                );

                        if (station.getLocalRef() != null) {
                            layoutAttributes.addView(
                                    new TextViewBuilder(
                                        PointDetailsFragment.this.getContext(),
                                        String.format(
                                            "%1$s: %2$s",
                                            getResources().getString(R.string.stationPlatform),
                                            station.getLocalRef()))
                                    .create()
                                    );
                        }
                        if (station.getNetwork() != null) {
                            layoutAttributes.addView(
                                    new TextViewBuilder(
                                        PointDetailsFragment.this.getContext(),
                                        String.format(
                                            "%1$s: %2$s",
                                            getResources().getString(R.string.labelStationNetwork),
                                            station.getNetwork()))
                                    .create()
                                    );
                        }
                        if (station.getOperator() != null) {
                            layoutAttributes.addView(
                                    new TextViewBuilder(
                                        PointDetailsFragment.this.getContext(),
                                        String.format(
                                            "%1$s: %2$s",
                                            getResources().getString(R.string.labelStationOperator),
                                            station.getOperator()))
                                    .create()
                                    );
                        }
                        if (! station.getVehicleList().isEmpty()) {
                            layoutAttributes.addView(
                                    new TextViewBuilder(
                                        PointDetailsFragment.this.getContext(),
                                        String.format(
                                            "%1$s: %2$s",
                                            getResources().getString(R.string.labelPointStationVehicleTypes),
                                            TextUtils.join(", ", station.getVehicleList())))
                                    .create()
                                    );
                        }
                    }

                    // lines
                    if (! station.getLineList().isEmpty()) {
                        layoutAttributes.addView(
                                new TextViewBuilder(
                                        PointDetailsFragment.this.getContext(),
                                        getResources().getString(R.string.labelPointStationLinesHeading))
                                    .isHeading()
                                    .addTopMargin()
                                    .create()
                                );
                        for (Line line : station.getLineList()) {
                            layoutAttributes.addView(
                                    new TextViewBuilder(
                                            PointDetailsFragment.this.getContext(),
                                            line.getDescription())
                                        .create()
                                    );
                        }
                    }
                }

                // poi building attributes
                if (poi.getOuterBuilding() != null
                        || ! poi.getEntranceList().isEmpty()) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                        PointDetailsFragment.this.getContext(),
                                    getResources().getString(R.string.labelPointPOIBuildingHeading))
                                .isHeading()
                                .addTopMargin()
                                .create()
                            );

                    // outer building
                    if (poi.getOuterBuilding() != null) {
                        TextView labelOuterBuilding = new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointPOIBuildingIsInside),
                                    poi.getOuterBuilding().toString()))
                            .create();
                        labelOuterBuilding.setTag(poi.getOuterBuilding());
                        labelOuterBuilding.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View view) {
                                FragmentContainerActivity.showObjectDetails(
                                        PointDetailsFragment.this.getContext(),
                                        (POI) view.getTag());
                            }
                        });
                        layoutAttributes.addView(labelOuterBuilding);
                    }

                    // number of entrances
                    if (! poi.getEntranceList().isEmpty()) {
                        layoutAttributes.addView(
                                new TextViewBuilder(
                                        PointDetailsFragment.this.getContext(),
                                        String.format(
                                            Locale.getDefault(),
                                            "%1$s: %2$d",
                                            getResources().getString(R.string.labelPointPOIBuildingNumberOfEntrances),
                                            poi.getEntranceList().size()))
                                    .create()
                                );
                    }
                }

                // contact attributes
                if (poi.hasAddress()
                        || poi.getEmail() != null
                        || poi.getPhone() != null
                        || poi.getWebsite() != null
                        || poi.getOpeningHours() != null) {
                    // heading
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                    PointDetailsFragment.this.getContext(),
                                    getResources().getString(R.string.labelPointPOIContactHeading))
                                .isHeading()
                                .addTopMargin()
                                .create()
                            );

                    // post address
                    if (poi.hasAddress()) {
                        layoutAttributes.addView(
                                new TextViewBuilder(
                                        PointDetailsFragment.this.getContext(),
                                        String.format(
                                            "%1$s: %2$s",
                                            getResources().getString(R.string.labelPointPOIContactPostAddress),
                                            poi.formatAddressLongLength()))
                                    .containsPostAddress()
                                    .create()
                                );
                    }

                    // email
                    if (poi.getEmail() != null) {
                        layoutAttributes.addView(
                                new TextViewBuilder(
                                        PointDetailsFragment.this.getContext(),
                                        String.format(
                                            "%1$s: %2$s",
                                        getResources().getString(R.string.labelPointPOIContactEMailAddress),
                                        poi.getEmail()))
                                .containsEmailAddress()
                                    .create()
                                );
                    }

                    // phone
                    if (! TextUtils.isEmpty(poi.getPhone())) {
                        layoutAttributes.addView(
                                new TextViewBuilder(
                                        PointDetailsFragment.this.getContext(),
                                        String.format(
                                            "%1$s: %2$s",
                                        getResources().getString(R.string.labelPointPOIContactPhoneNumber),
                                        poi.getPhone()))
                                    .containsPhoneNumber()
                                    .create()
                                );
                    }

                    // website
                    if (! TextUtils.isEmpty(poi.getWebsite())) {
                        layoutAttributes.addView(
                                new TextViewBuilder(
                                        PointDetailsFragment.this.getContext(),
                                        String.format(
                                            "%1$s: %2$s",
                                        getResources().getString(R.string.labelPointPOIContactWebsite),
                                        poi.getWebsite()))
                                    .containsUrl()
                                    .create()
                                );
                    }

                    // opening hours
                    if (! TextUtils.isEmpty(poi.getOpeningHours())) {
                        layoutAttributes.addView(
                                new TextViewBuilder(
                                        PointDetailsFragment.this.getContext(),
                                        String.format(
                                            "%1$s: %2$s",
                                        getResources().getString(R.string.labelPointPOIContactOpeningHours),
                                        poi.getOpeningHours()))
                                    .create()
                                );
                    }
                }
            }

            // address components
            if (pointWithAddressData != null
                    && (
                           pointWithAddressData.getHouseNumber() != null
                        || pointWithAddressData.getRoad() != null
                        || pointWithAddressData.getResidential() != null
                        || pointWithAddressData.getSuburb() != null
                        || pointWithAddressData.getCityDistrict() != null
                        || pointWithAddressData.getZipcode() != null
                        || pointWithAddressData.getCity() != null
                        || pointWithAddressData.getState() != null
                        || pointWithAddressData.getCountry() != null)) {

                // heading
                layoutAttributes.addView(
                        new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                getResources().getString(R.string.labelPointWithAddressDataHeading))
                            .isHeading()
                            .addTopMargin()
                            .create()
                        );

                // house number
                if (! TextUtils.isEmpty(pointWithAddressData.getHouseNumber())) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                    PointDetailsFragment.this.getContext(),
                                    String.format(
                                        "%1$s: %2$s",
                                        getResources().getString(R.string.labelPointWithAddressDataHouseNumber),
                                        pointWithAddressData.getHouseNumber()))
                                .create()
                            );
                }

                // road
                if (! TextUtils.isEmpty(pointWithAddressData.getRoad())) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                    PointDetailsFragment.this.getContext(),
                                    String.format(
                                        "%1$s: %2$s",
                                        getResources().getString(R.string.labelPointWithAddressDataRoad),
                                        pointWithAddressData.getRoad()))
                                .create()
                            );
                }

                // residential
                if (! TextUtils.isEmpty(pointWithAddressData.getResidential())) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                    PointDetailsFragment.this.getContext(),
                                    String.format(
                                        "%1$s: %2$s",
                                getResources().getString(R.string.labelPointWithAddressDataResidential),
                                pointWithAddressData.getResidential()))
                            .create()
                        );
                }

                // suburb
                if (! TextUtils.isEmpty(pointWithAddressData.getSuburb())) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                    PointDetailsFragment.this.getContext(),
                                    String.format(
                                        "%1$s: %2$s",
                                getResources().getString(R.string.labelPointWithAddressDataSuburb),
                                pointWithAddressData.getSuburb()))
                            .create()
                        );
                }

                // city district
                if (! TextUtils.isEmpty(pointWithAddressData.getCityDistrict())) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                    PointDetailsFragment.this.getContext(),
                                    String.format(
                                        "%1$s: %2$s",
                                getResources().getString(R.string.labelPointWithAddressDataCityDistrict),
                                pointWithAddressData.getCityDistrict()))
                            .create()
                        );
                }

                // postcode
                if (! TextUtils.isEmpty(pointWithAddressData.getZipcode())) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                    PointDetailsFragment.this.getContext(),
                                    String.format(
                                        "%1$s: %2$s",
                                getResources().getString(R.string.labelPointWithAddressDataPostcode),
                                pointWithAddressData.getZipcode()))
                            .create()
                        );
                }

                // city
                if (! TextUtils.isEmpty(pointWithAddressData.getCity())) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                    PointDetailsFragment.this.getContext(),
                                    String.format(
                                        "%1$s: %2$s",
                                getResources().getString(R.string.labelPointWithAddressDataCity),
                                pointWithAddressData.getCity()))
                            .create()
                        );
                }

                // state
                if (! TextUtils.isEmpty(pointWithAddressData.getState())) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                    PointDetailsFragment.this.getContext(),
                                    String.format(
                                        "%1$s: %2$s",
                                getResources().getString(R.string.labelPointWithAddressDataState),
                                pointWithAddressData.getState()))
                            .create()
                        );
                }

                // country
                if (! TextUtils.isEmpty(pointWithAddressData.getCountry())) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                    PointDetailsFragment.this.getContext(),
                                    String.format(
                                        "%1$s: %2$s",
                                getResources().getString(R.string.labelPointWithAddressDataCountry),
                                pointWithAddressData.getCountry()))
                            .create()
                        );
                }
            }

        } else if (point instanceof GPS) {
            GPS gps = (GPS) point;

            // heading
            layoutAttributes.addView(
                    new TextViewBuilder(
                            PointDetailsFragment.this.getContext(),
                            getResources().getString(R.string.labelPointGPSDetailsHeading))
                        .isHeading()
                        .addTopMargin()
                        .create()
                    );

            // provider
            if (gps.getProvider() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                gps.formatProviderAndNumberOfSatellites())
                            .create()
                        );
            }

            // accuracy
            if (gps.getAccuracy() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                gps.formatAccuracyInMeters())
                            .create()
                        );
            }

            // altitude
            if (gps.getAltitude() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                gps.formatAltitudeInMeters())
                            .create()
                        );
            }

            // speed
            if (gps.getSpeed() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                gps.formatSpeedInKMH())
                            .create()
                        );
            }

            // timestamp
            layoutAttributes.addView(
                    new TextViewBuilder(
                            PointDetailsFragment.this.getContext(),
                            gps.formatTimestamp())
                        .create()
                    );

        } else if (point instanceof Intersection) {
            Intersection intersection  = (Intersection) point;

            // heading
            layoutAttributes.addView(
                    new TextViewBuilder(
                            PointDetailsFragment.this.getContext(),
                            getResources().getString(R.string.labelPointIntersectionAttributesHeading))
                        .isHeading()
                        .addTopMargin()
                        .create()
                    );
            // number of streets
            layoutAttributes.addView(
                    new TextViewBuilder(
                            PointDetailsFragment.this.getContext(),
                            intersection.formatNumberOfStreets())
                        .create()
                    );
            // pedestrian crossings nearby
            layoutAttributes.addView(
                    new TextViewBuilder(
                            PointDetailsFragment.this.getContext(),
                            intersection.formatNumberOfCrossingsNearby())
                        .create()
                    );

        } else if (point instanceof PedestrianCrossing) {
            PedestrianCrossing pedestrianCrossing = (PedestrianCrossing) point;

            if (pedestrianCrossing.getTrafficSignalsSound() != null
                    || pedestrianCrossing.getTrafficSignalsVibration() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                getResources().getString(R.string.labelPointTrafficSignalsAttributesHeading))
                            .isHeading()
                            .addTopMargin()
                            .create()
                        );

                // traffic signals sound
                if (pedestrianCrossing.getTrafficSignalsSound() != null) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                    PointDetailsFragment.this.getContext(),
                                    String.format(
                                        "%1$s: %2$s",
                                        getResources().getString(R.string.labelPointTrafficSignalsSound),
                                        pedestrianCrossing.getTrafficSignalsSound().toString()))
                                .create()
                            );
                }

                // traffic signals vibration
                if (pedestrianCrossing.getTrafficSignalsVibration() != null) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
                                    PointDetailsFragment.this.getContext(),
                                    String.format(
                                        "%1$s: %2$s",
                                        getResources().getString(R.string.labelPointTrafficSignalsVibration),
                                        pedestrianCrossing.getTrafficSignalsVibration().toString()))
                                .create()
                            );
                }
            }
        }

        // notes
        if (point.getDescription() != null
                || point.getAltName() != null
                || point.getOldName() != null
                || point.getNote() != null
                || point.getWikidataUrl() != null) {
            layoutAttributes.addView(
                    new TextViewBuilder(
                            PointDetailsFragment.this.getContext(),
                            getResources().getString(R.string.labelPointNotesHeading))
                        .isHeading()
                        .addTopMargin()
                        .create()
                    );

            if (point.getDescription() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelSegmentFootwayDescription),
                                    point.getDescription()))
                            .create()
                        );
            }

            if (point.getAltName() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointAltName),
                                    point.getAltName()))
                            .create()
                        );
            }

            if (point.getOldName() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointOldName),
                                    point.getOldName()))
                            .create()
                        );
            }

            if (point.getNote() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointNote),
                                    point.getNote()))
                            .create()
                        );
            }

            if (point.getWikidataUrl() != null) {
                TextView labelWikidataUrl = new TextViewBuilder(
                        PointDetailsFragment.this.getContext(),
                        getResources().getString(R.string.labelPointWikidata))
                    .create();
                labelWikidataUrl.setTag(point.getWikidataUrl());
                Timber.d("wikidata: %1$s", point.getWikidataUrl());
                labelWikidataUrl.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        Intent intent = new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse((String) view.getTag()));
                        getActivity().startActivity(intent);
                    }
                });
                layoutAttributes.addView(labelWikidataUrl);
            }
        }

        // accessibility attributes
        if (point.getTactilePaving() != null
                || point.getWheelchair() != null) {
            layoutAttributes.addView(
                    new TextViewBuilder(
                            PointDetailsFragment.this.getContext(),
                            getResources().getString(R.string.labelPointAccessibilityHeading))
                        .isHeading()
                        .addTopMargin()
                        .create()
                    );

            // tactile paving
            if (point.getTactilePaving() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointTactilePaving),
                                    point.getTactilePaving().toString()))
                            .create()
                        );
            }

            // wheelchair
            if (point.getWheelchair() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                PointDetailsFragment.this.getContext(),
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointWheelchair),
                                    point.getWheelchair().toString()))
                            .create()
                        );
            }
        }

        // coordinates
        layoutAttributes.addView(
                new TextViewBuilder(
                        PointDetailsFragment.this.getContext(),
                        getResources().getString(R.string.labelPointCoordinatesHeading))
                    .isHeading()
                    .addTopMargin()
                    .create()
                );
        layoutAttributes.addView(
                new TextViewBuilder(
                        PointDetailsFragment.this.getContext(),
                        String.format(
                            Locale.getDefault(),
                            "%1$s: %2$f",
                            getResources().getString(R.string.labelGPSLatitude),
                            point.getLatitude()))
                    .create()
                );
        layoutAttributes.addView(
                new TextViewBuilder(
                        PointDetailsFragment.this.getContext(),
                        String.format(
                            Locale.getDefault(),
                            "%1$s: %2$f",
                            getResources().getString(R.string.labelGPSLongitude),
                            point.getLongitude()))
                    .create()
                );
    }

}
