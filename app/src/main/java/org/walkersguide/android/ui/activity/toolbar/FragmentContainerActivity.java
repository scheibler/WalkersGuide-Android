package org.walkersguide.android.ui.activity.toolbar;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Location;

import org.walkersguide.android.ui.fragment.object_list.ObjectListFromDatabaseFragment;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
import org.walkersguide.android.database.profiles.DatabaseRouteProfile;
import org.walkersguide.android.ui.activity.AbstractToolbarActivity;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.ui.fragment.InfoFragment;
import org.walkersguide.android.ui.fragment.SettingsFragment;
import org.walkersguide.android.ui.fragment.pt.DeparturesFragment;
import org.walkersguide.android.ui.fragment.pt.TripDetailsFragment;

import androidx.fragment.app.FragmentContainerView;
import android.content.Context;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import android.text.format.DateFormat;

import android.view.Menu;

import android.widget.TextView;

import java.util.Date;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.server.util.OSMMap;
import org.walkersguide.android.server.util.ServerInstance;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;
import android.content.Intent;
import androidx.fragment.app.Fragment;
import org.walkersguide.android.pt.PTHelper;
import org.walkersguide.android.ui.fragment.routing.RouteDetailsFragment;
import org.walkersguide.android.data.route.Route;


public class FragmentContainerActivity extends AbstractToolbarActivity {
    private static final String KEY_SHOW_FRAGMENT = "show";
    // showRouteDetails
    private static final String KEY_ROUTE = "route";
    // showDepartures and showTripDetails
    private static final String KEY_STATION = "station";
    // showDepartures
    private static final String KEY_DEPARTURE_TIME = "departureTime";
    // showTripDetails
    private static final String KEY_DEPARTURE = "departure";


    public static void showFavorites(Context packageContext) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.FAVORITES);
        packageContext.startActivity(intent);
    }

    public static void showPointHistory(Context packageContext) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.POINT_HISTORY);
        packageContext.startActivity(intent);
    }

    public static void showRouteHistory(Context packageContext) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.ROUTE_HISTORY);
        packageContext.startActivity(intent);
    }

    public static void showRouteDetails(Context packageContext, Route route) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.ROUTE_DETAILS);
        intent.putExtra(KEY_ROUTE, route);
        packageContext.startActivity(intent);
    }

    public static void showDepartures(Context packageContext, Location station, Date departureTime) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.DEPARTURES);
        intent.putExtra(KEY_STATION, station);
        intent.putExtra(KEY_DEPARTURE_TIME, departureTime);
        packageContext.startActivity(intent);
    }

    public static void showTripDetails(Context packageContext, Location station, Departure departure) {
        Intent intent = new Intent(packageContext, FragmentContainerActivity.class);
        intent.putExtra(KEY_SHOW_FRAGMENT, Show.TRIP_DETAILS);
        intent.putExtra(KEY_STATION, station);
        intent.putExtra(KEY_DEPARTURE, departure);
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


    private enum Show {
        FAVORITES, POINT_HISTORY, ROUTE_HISTORY, ROUTE_DETAILS, DEPARTURES, TRIP_DETAILS, SETTINGS, INFO
    }
    private Show showFragment;


	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        showFragment = (Show) getIntent().getExtras().getSerializable(KEY_SHOW_FRAGMENT);

        Fragment fragment = null;
        switch (showFragment) {

            case FAVORITES:
                fragment = ObjectListFromDatabaseFragment.createFragment(
                        new DatabaseProfileRequest(DatabasePointProfile.FAVORITES), false);
                FragmentContainerActivity.this.setToolbarTitle(
                        DatabasePointProfile.FAVORITES.getName());
                break;

            case POINT_HISTORY:
                fragment = ObjectListFromDatabaseFragment.createFragment(
                        new DatabaseProfileRequest(DatabasePointProfile.ALL_POINTS), true);
                FragmentContainerActivity.this.setToolbarTitle(
                        GlobalInstance.getStringResource(R.string.fragmentPointHistoryName));
                break;

            case ROUTE_HISTORY:
                fragment = ObjectListFromDatabaseFragment.createFragment(
                        new DatabaseProfileRequest(DatabaseRouteProfile.ALL_ROUTES), true);
                FragmentContainerActivity.this.setToolbarTitle(
                        GlobalInstance.getStringResource(R.string.fragmentRouteHistoryName));
                break;

            case ROUTE_DETAILS:
                fragment = RouteDetailsFragment.newInstance(
                        (Route) getIntent().getExtras().getSerializable(KEY_ROUTE));
                FragmentContainerActivity.this.setToolbarTitle(
                        GlobalInstance.getStringResource(R.string.routeDetailsFragmentTitle));
                break;

            case DEPARTURES:
                Location departureStation = (Location) getIntent().getExtras().getSerializable(KEY_STATION);
                Date departureTime = (Date) getIntent().getExtras().getSerializable(KEY_DEPARTURE_TIME);
                fragment = DeparturesFragment.newInstance(departureStation, departureTime);
                FragmentContainerActivity.this.setToolbarTitle(
                        PTHelper.getLocationName(departureStation));
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
                            PTHelper.getLocationName(tripDeparture.destination))
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

        if (fragment != null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainerView, fragment)
                .commit();
        }
    }


    /**
     * implement AbstractToolbarActivity and AbstractViewPagerActivity functions
     */

    public int getLayoutResourceId() {
		return R.layout.activity_fragment_container;
    }

    public void tabSelected(int tabIndex) {
    }

}
