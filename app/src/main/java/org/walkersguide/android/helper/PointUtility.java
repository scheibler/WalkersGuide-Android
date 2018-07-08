package org.walkersguide.android.helper;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.RouteSettings;

import android.content.Context;

public class PointUtility {

    public static void putNewPoint(Context context, PointWrapper newPoint, int putInto) {
        switch (putInto) {
            case Constants.POINT_PUT_INTO.START:
            case Constants.POINT_PUT_INTO.DESTINATION:
                RouteSettings routeSettings = SettingsManager.getInstance(context).getRouteSettings();
                if (putInto == Constants.POINT_PUT_INTO.START) {
                    routeSettings.setStartPoint(newPoint);
                } else {
                    routeSettings.setDestinationPoint(newPoint);
                }
                break;
            case Constants.POINT_PUT_INTO.SIMULATION:
                PositionManager.getInstance(context).setSimulatedLocation(newPoint);
                break;
            default:
                break;
        }
    }

}
