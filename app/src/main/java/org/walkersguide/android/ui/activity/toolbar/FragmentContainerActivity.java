package org.walkersguide.android.ui.activity.toolbar;

import org.walkersguide.android.ui.fragment.tabs.ContainerTabLayoutFragment;
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
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.ui.fragment.tabs.ObjectDetailsTabLayoutFragment;
import org.walkersguide.android.data.object_with_id.Segment;
import android.view.MenuItem;
import androidx.core.app.NavUtils;
import timber.log.Timber;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.poi.Station;
import org.walkersguide.android.data.object_with_id.segment.IntersectionSegment;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.POI;


public class FragmentContainerActivity extends ToolbarActivity {

    // show object details
    private static final String KEY_OBJECT_WITH_ID = "objectWithId";

    public static void showObjectDetails(Context packageContext, ObjectWithId objectWithId) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.OBJECT_DETAILS);
        intent.putExtra(KEY_OBJECT_WITH_ID, objectWithId);
        packageContext.startActivity(intent);
    }

    public static void showObjectDetailsTabDepartures(Context packageContext, Station station) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.OBJECT_DETAILS_TAB_DEPARTURES);
        intent.putExtra(KEY_OBJECT_WITH_ID, station);
        packageContext.startActivity(intent);
    }

    public static void showObjectDetailsTabEntrances(Context packageContext, POI poi) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.OBJECT_DETAILS_TAB_ENTRANCES);
        intent.putExtra(KEY_OBJECT_WITH_ID, poi);
        packageContext.startActivity(intent);
    }

    public static void showObjectDetailsTabStreetCourse(Context packageContext, IntersectionSegment intersectionSegment) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.OBJECT_DETAILS_TAB_STREET_COURSE);
        intent.putExtra(KEY_OBJECT_WITH_ID, intersectionSegment);
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

    // history, settings and info

    public static void showHistory(Context packageContext) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.HISTORY);
        packageContext.startActivity(intent);
    }

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
        OBJECT_DETAILS, OBJECT_DETAILS_TAB_DEPARTURES, OBJECT_DETAILS_TAB_ENTRANCES, OBJECT_DETAILS_TAB_STREET_COURSE,
        DEPARTURES, TRIP_DETAILS, HISTORY, SETTINGS, INFO
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

                case OBJECT_DETAILS:
                case OBJECT_DETAILS_TAB_DEPARTURES:
                case OBJECT_DETAILS_TAB_ENTRANCES:
                case OBJECT_DETAILS_TAB_STREET_COURSE:
                    ObjectWithId objectWithId = (ObjectWithId) getIntent().getExtras().getSerializable(KEY_OBJECT_WITH_ID);
                    if (objectWithId != null) {
                        if (showFragment == Show.OBJECT_DETAILS_TAB_DEPARTURES) {
                            fragment = ObjectDetailsTabLayoutFragment.departures((Station) objectWithId);
                        } else if (showFragment == Show.OBJECT_DETAILS_TAB_ENTRANCES) {
                            fragment = ObjectDetailsTabLayoutFragment.entrances((POI) objectWithId);
                        } else if (showFragment == Show.OBJECT_DETAILS_TAB_STREET_COURSE) {
                            fragment = ObjectDetailsTabLayoutFragment.streetCourse((IntersectionSegment) objectWithId);
                        } else {
                            fragment = ObjectDetailsTabLayoutFragment.details(objectWithId);
                        }
                        FragmentContainerActivity.this.setToolbarTitle(
                                GlobalInstance.getStringResource(R.string.fragmentPointDetailsName));
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
                                "%1$s, %2$s",
                                PtUtility.getLineLabel(tripDeparture.line, false),
                                PtUtility.getLocationName(tripDeparture.destination))
                            );
                    break;

                case HISTORY:
                    fragment = ContainerTabLayoutFragment.history();
                    FragmentContainerActivity.this.setToolbarTitle(
                            GlobalInstance.getStringResource(R.string.menuItemHistory));
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

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

}
