package org.walkersguide.android.ui.fragment;

import androidx.fragment.app.Fragment;
import org.walkersguide.android.ui.builder.TextViewBuilder;


import android.os.Bundle;

import android.text.TextUtils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.LinearLayout;
import android.widget.TextView;



import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.data.basic.point.Entrance;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.point.Intersection;
import org.walkersguide.android.data.basic.point.PedestrianCrossing;
import org.walkersguide.android.data.basic.point.POI;
import org.walkersguide.android.data.basic.point.PointWithAddressData;
import org.walkersguide.android.data.basic.point.Station;
import org.walkersguide.android.data.basic.point.StreetAddress;
import org.walkersguide.android.data.basic.point.Line;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.activity.toolbar.tabs.PointDetailsActivity;


public class PointDetailsFragment extends Fragment {
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

        // attributes layout
		layoutAttributes = (LinearLayout) view.findViewById(R.id.linearLayout);
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
                                    getResources().getString(R.string.labelPointEntranceAttributesHeading))
                                .isHeading()
                                .create()
                            );

                    // label
                    layoutAttributes.addView(
                            new TextViewBuilder(
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

                    // vehicle list
                    if (! station.getVehicleList().isEmpty()) {
                        layoutAttributes.addView(
                                new TextViewBuilder(
                                        getResources().getString(R.string.labelPointStationAttributesHeading))
                                    .isHeading()
                                    .create()
                                );
                        layoutAttributes.addView(
                                new TextViewBuilder(
                                        String.format(
                                            "%1$s: %2$s",
                                            getResources().getString(R.string.labelPointStationVehicleTypes),
                                            TextUtils.join(", ", station.getVehicleList())))
                                    .create()
                                );
                    }

                    // lines
                    if (! station.getLineList().isEmpty()) {
                        layoutAttributes.addView(
                                new TextViewBuilder(
                                        getResources().getString(R.string.labelPointStationLinesHeading))
                                    .isHeading()
                                    .addTopMargin()
                                    .create()
                                );
                        for (Line line : station.getLineList()) {
                            layoutAttributes.addView(
                                    new TextViewBuilder(
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
                                    getResources().getString(R.string.labelPointPOIBuildingHeading))
                                .isHeading()
                                .addTopMargin()
                                .create()
                            );

                    // outer building
                    if (poi.getOuterBuilding() != null) {
                        TextView labelOuterBuilding = new TextViewBuilder(
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointPOIBuildingIsInside),
                                    poi.getOuterBuilding().toString()))
                            .create();
                        labelOuterBuilding.setTag(poi.getOuterBuilding());
                        labelOuterBuilding.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View view) {
                                PointDetailsActivity.start(
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
                                        String.format(
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
                                    getResources().getString(R.string.labelPointPOIContactHeading))
                                .isHeading()
                                .addTopMargin()
                                .create()
                            );

                    // post address
                    if (poi.hasAddress()) {
                        layoutAttributes.addView(
                                new TextViewBuilder(
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
                                getResources().getString(R.string.labelPointWithAddressDataHeading))
                            .isHeading()
                            .addTopMargin()
                            .create()
                        );

                // house number
                if (! TextUtils.isEmpty(pointWithAddressData.getHouseNumber())) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
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
                            getResources().getString(R.string.labelPointGPSDetailsHeading))
                        .isHeading()
                        .addTopMargin()
                        .create()
                    );

            // provider
            if (gps.getProvider() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                gps.formatProviderAndNumberOfSatellites())
                            .create()
                        );
            }

            // accuracy
            if (gps.getAccuracy() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                gps.formatAccuracyInMeters())
                            .create()
                        );
            }

            // altitude
            if (gps.getAltitude() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                gps.formatAltitudeInMeters())
                            .create()
                        );
            }

            // speed
            if (gps.getSpeed() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                gps.formatSpeedInKMH())
                            .create()
                        );
            }

            // timestamp
            layoutAttributes.addView(
                    new TextViewBuilder(
                            gps.formatTimestamp())
                        .create()
                    );

        } else if (point instanceof Intersection) {
            Intersection intersection  = (Intersection) point;

            // heading
            layoutAttributes.addView(
                    new TextViewBuilder(
                            getResources().getString(R.string.labelPointIntersectionAttributesHeading))
                        .isHeading()
                        .addTopMargin()
                        .create()
                    );

            // number of streets
            layoutAttributes.addView(
                    new TextViewBuilder(
                            String.format(
                                "%1$s: %2$d",
                                getResources().getString(R.string.labelPointIntersectionNumberOfStreets),
                                intersection.getNumberOfStreets()))
                        .create()
                    );
            layoutAttributes.addView(
                    new TextViewBuilder(
                            String.format(
                                "%1$s: %2$d",
                                getResources().getString(R.string.labelPointIntersectionNumberOfBigStreets),
                                intersection.getNumberOfStreetsWithName()))
                        .create()
                    );

            // pedestrian crossings nearby
            layoutAttributes.addView(
                    new TextViewBuilder(
                            String.format(
                                "%1$s: %2$d",
                                getResources().getString(R.string.labelPointIntersectionNumberOfPedestrianCrossingsNearBy),
                                intersection.getPedestrianCrossingList().size()))
                        .create()
                    );

        } else if (point instanceof PedestrianCrossing) {
            PedestrianCrossing pedestrianCrossing = (PedestrianCrossing) point;

            if (pedestrianCrossing.getTrafficSignalsSound() != null
                    || pedestrianCrossing.getTrafficSignalsVibration() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
                                getResources().getString(R.string.labelPointTrafficSignalsAttributesHeading))
                            .isHeading()
                            .addTopMargin()
                            .create()
                        );

                // traffic signals sound
                if (pedestrianCrossing.getTrafficSignalsSound() != null) {
                    layoutAttributes.addView(
                            new TextViewBuilder(
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
                                    String.format(
                                        "%1$s: %2$s",
                                        getResources().getString(R.string.labelPointTrafficSignalsVibration),
                                        pedestrianCrossing.getTrafficSignalsVibration().toString()))
                                .create()
                            );
                }
            }
        }

        // alt_name, old_name and note
        if (point.getAltName() != null) {
            layoutAttributes.addView(
                    new TextViewBuilder(
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
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelPointNote),
                                point.getNote()))
                        .create()
                    );
        }

        // accessibility attributes
        if (point.getTactilePaving() != null
                || point.getWheelchair() != null) {
            layoutAttributes.addView(
                    new TextViewBuilder(
                            getResources().getString(R.string.labelPointAccessibilityHeading))
                        .isHeading()
                        .addTopMargin()
                        .create()
                    );

            // tactile paving
            if (point.getTactilePaving() != null) {
                layoutAttributes.addView(
                        new TextViewBuilder(
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
                        getResources().getString(R.string.labelPointCoordinatesHeading))
                    .isHeading()
                    .addTopMargin()
                    .create()
                );
        layoutAttributes.addView(
                new TextViewBuilder(
                        String.format(
                            "%1$s: %2$f",
                            getResources().getString(R.string.labelGPSLatitude),
                            point.getLatitude()))
                    .create()
                );
        layoutAttributes.addView(
                new TextViewBuilder(
                        String.format(
                            "%1$s: %2$f",
                            getResources().getString(R.string.labelGPSLongitude),
                            point.getLongitude()))
                    .create()
                );
    }

}
