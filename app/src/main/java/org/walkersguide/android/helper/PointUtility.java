package org.walkersguide.android.helper;

import android.content.Context;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;


public class PointUtility {

    public static void putNewPoint(Context context, PointWrapper newPoint, int putInto) {
        switch (putInto) {
            case Constants.POINT_PUT_INTO.START:
                SettingsManager.getInstance(context).getRouteSettings().setStartPoint(newPoint);
                break;
            case Constants.POINT_PUT_INTO.DESTINATION:
                SettingsManager.getInstance(context).getRouteSettings().setDestinationPoint(newPoint);
                break;
            case Constants.POINT_PUT_INTO.SIMULATION:
                PositionManager.getInstance(context).setSimulatedLocation(newPoint);
                break;
            default:
                if (putInto >= Constants.POINT_PUT_INTO.VIA) {
                    SettingsManager.getInstance(context).getRouteSettings()
                        .replaceViaPointAtIndex(putInto-Constants.POINT_PUT_INTO.VIA, newPoint);
                }
                break;
        }
    }

}
