package org.walkersguide.android.listener;

import java.util.ArrayList;

import org.walkersguide.android.data.station.Departure;

public interface DepartureResultListener {
	public void departureQuerySuccessful(ArrayList<Departure> departureList);
    public void departureQueryFailed(String error);
}
