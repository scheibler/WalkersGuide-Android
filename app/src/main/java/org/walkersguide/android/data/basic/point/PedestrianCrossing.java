package org.walkersguide.android.data.basic.point;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class PedestrianCrossing extends Point {

    private int trafficSignalsSound, trafficSignalsVibration;

    public PedestrianCrossing(Context context, JSONObject inputData) throws JSONException {
        // point super constructor
        super(context, inputData);
        // traffic signal attributes
        try {
            this.trafficSignalsSound = inputData.getInt("traffic_signals_sound");
        } catch (JSONException e) {
            this.trafficSignalsSound = -1;
        }
        try {
            this.trafficSignalsVibration = inputData.getInt("traffic_signals_vibration");
        } catch (JSONException e) {
            this.trafficSignalsVibration = -1;
        }
    }

    /**
     * traffic signal properties
     *  -1: unknown (default)
     *   0: no
     *   1: yes
     */

    public interface TRAFFIC_SIGNAL {
        public static final int NO = 0;
        public static final int YES = 1;
    }

    public final static int[] TrafficSignalValueArray = {
        TRAFFIC_SIGNAL.NO, TRAFFIC_SIGNAL.YES
    };

    public int getTrafficSignalsSound() {
        return this.trafficSignalsSound;
    }

    public int getTrafficSignalsVibration() {
        return this.trafficSignalsVibration;
    }

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        // traffic signal attributes
        if (this.trafficSignalsSound > -1) {
            try {
                jsonObject.put("traffic_signals_sound", this.trafficSignalsSound);
            } catch (JSONException e) {}
        }
        if (this.trafficSignalsVibration > -1) {
            try {
                jsonObject.put("traffic_signals_vibration", this.trafficSignalsVibration);
            } catch (JSONException e) {}
        }
        return jsonObject;
    }

    @Override public String toString() {
        return super.toString();
    }

}
