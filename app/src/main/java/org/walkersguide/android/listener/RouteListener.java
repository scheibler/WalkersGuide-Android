package org.walkersguide.android.listener;

import org.walkersguide.android.data.route.Route;


public interface RouteListener {
	public void routeCalculationFinished(int returnCode, String returnMessage, int routeId);
	public void routeRequestFinished(int returnCode, String returnMessage, Route route);
}
