package org.walkersguide.android.listener;

import java.util.ArrayList;

import org.walkersguide.android.data.station.Departure;

public interface DepartureResultListener {
	public void departureRequestFinished(int returnCode, String returnMessage, ArrayList<Departure> departureList);
}
