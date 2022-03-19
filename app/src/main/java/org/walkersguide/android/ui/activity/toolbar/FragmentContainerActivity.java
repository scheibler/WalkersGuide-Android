package org.walkersguide.android.ui.activity.toolbar;

import org.walkersguide.android.data.object_with_id.HikingTrail;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Location;

import org.walkersguide.android.ui.activity.ToolbarActivity;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.ui.fragment.InfoFragment;
import org.walkersguide.android.ui.fragment.SettingsFragment;
import org.walkersguide.android.ui.fragment.pt.DeparturesFragment;
import org.walkersguide.android.ui.fragment.pt.TripDetailsFragment;

import android.content.Context;

import android.os.Bundle;





import java.util.Date;

import org.walkersguide.android.R;
import android.content.Intent;
import androidx.fragment.app.Fragment;
import org.walkersguide.android.server.pt.PtUtility;
import org.walkersguide.android.ui.fragment.details.HikingTrailDetailsFragment;
import org.walkersguide.android.ui.fragment.details.RouteDetailsFragment;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.ui.fragment.details.PointDetailsFragment;
import org.walkersguide.android.data.object_with_id.Segment;
import org.walkersguide.android.ui.fragment.details.SegmentDetailsFragment;


public class FragmentContainerActivity extends ToolbarActivity {

    // show object details
    private static final String KEY_OBJECT_WITH_ID = "objectWithId";

    public static void showDetailsForObjectWithId(Context packageContext, ObjectWithId objectWithId) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.DETAILS_FOR_OBJECT_WITH_ID);
        intent.putExtra(KEY_OBJECT_WITH_ID, objectWithId);
        packageContext.startActivity(intent);
    }

    // showDepartures
    private static final String KEY_STATION = "station";
    private static final String KEY_DEPARTURE_TIME = "departureTime";

    public static void showDepartures(Context packageContext, Location station, Date departureTime) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.DEPARTURES);
        intent.putExtra(KEY_STATION, station);
        intent.putExtra(KEY_DEPARTURE_TIME, departureTime);
        packageContext.startActivity(intent);
    }

    // showTripDetails
    private static final String KEY_DEPARTURE = "departure";

    public static void showTripDetails(Context packageContext, Location station, Departure departure) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.TRIP_DETAILS);
        intent.putExtra(KEY_STATION, station);
        intent.putExtra(KEY_DEPARTURE, departure);
        packageContext.startActivity(intent);
    }

    // settings and info

    public static void showSettings(Context packageContext) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.SETTINGS);
        packageContext.startActivity(intent);
    }

    public static void showInfo(Context packageContext) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.INFO);
        packageContext.startActivity(intent);
    }


    // activity
    private static final String KEY_SHOW_FRAGMENT = "show";

    private enum Show {
        DETAILS_FOR_OBJECT_WITH_ID, DEPARTURES, TRIP_DETAILS, SETTINGS, INFO
    }


    @Override public int getLayoutResourceId() {
		return R.layout.activity_fragment_container;
    }

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Show showFragment = (Show) getIntent().getExtras().getSerializable(KEY_SHOW_FRAGMENT);

        Fragment fragment = null;
        if (showFragment != null) {
            switch (showFragment) {

                case DETAILS_FOR_OBJECT_WITH_ID:
                    ObjectWithId objectWithId = (ObjectWithId) getIntent().getExtras().getSerializable(KEY_OBJECT_WITH_ID);
                    if (objectWithId instanceof Point) {
                        fragment = PointDetailsFragment.newInstance((Point) objectWithId);
                        FragmentContainerActivity.this.setToolbarTitle(
                                GlobalInstance.getStringResource(R.string.fragmentPointDetailsName));
                    } else if (objectWithId instanceof HikingTrail) {
                        fragment = HikingTrailDetailsFragment.newInstance((HikingTrail) objectWithId);
                        FragmentContainerActivity.this.setToolbarTitle(
                                GlobalInstance.getStringResource(R.string.fragmentHikingTrailDetailsName));
                    } else if (objectWithId instanceof Route) {
                        fragment = RouteDetailsFragment.newInstance((Route) objectWithId);
                        FragmentContainerActivity.this.setToolbarTitle(
                                GlobalInstance.getStringResource(R.string.fragmentRouteDetailsName));
                    } else if (objectWithId instanceof Segment) {
                        fragment = SegmentDetailsFragment.newInstance((Segment) objectWithId);
                        FragmentContainerActivity.this.setToolbarTitle(
                                GlobalInstance.getStringResource(R.string.fragmentSegmentDetailsName));
                    }
                    break;

                case DEPARTURES:
                    Location departureStation = (Location) getIntent().getExtras().getSerializable(KEY_STATION);
                    Date departureTime = (Date) getIntent().getExtras().getSerializable(KEY_DEPARTURE_TIME);
                    fragment = DeparturesFragment.newInstance(departureStation, departureTime);
                    FragmentContainerActivity.this.setToolbarTitle(
                            PtUtility.getLocationName(departureStation));
                    break;

                case TRIP_DETAILS:
                    Location tripStation = (Location) getIntent().getExtras().getSerializable(KEY_STATION);
                    Departure tripDeparture = (Departure) getIntent().getExtras().getSerializable(KEY_DEPARTURE);
                    fragment = TripDetailsFragment.newInstance(tripStation, tripDeparture);
                    FragmentContainerActivity.this.setToolbarTitle(
                            String.format(
                                "%1$s%2$s, %3$s",
                                tripDeparture.line.product.code,
                                tripDeparture.line.label,
                                PtUtility.getLocationName(tripDeparture.destination))
                            );
                    break;

                case SETTINGS:
                    fragment = SettingsFragment.newInstance();
                    FragmentContainerActivity.this.setToolbarTitle(
                            GlobalInstance.getStringResource(R.string.fragmentSettingsName));
                    break;

                case INFO:
                    fragment = InfoFragment.newInstance();
                    FragmentContainerActivity.this.setToolbarTitle(
                            GlobalInstance.getStringResource(R.string.fragmentInfoName));
                    break;
            }
        }

        if (fragment != null) {
            String tag = showFragment.name();
            // only replace, if the fragment is not already attached
            if (getSupportFragmentManager().findFragmentByTag(tag) == null) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerView, fragment, tag)
                    .commit();
            }
        }
    }

}
