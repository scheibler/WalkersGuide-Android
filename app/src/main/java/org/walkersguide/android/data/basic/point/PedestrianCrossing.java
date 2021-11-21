package org.walkersguide.android.data.basic.point;

import org.walkersguide.android.R;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.StringUtility;


public class PedestrianCrossing extends Point implements Serializable {
    private static final long serialVersionUID = 1l;

    private TrafficSignal trafficSignalsSound, trafficSignalsVibration;

    public PedestrianCrossing(JSONObject inputData) throws JSONException {
        super(inputData);
        // traffic signal attributes
        this.trafficSignalsSound = TrafficSignal.lookUpById(
                StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_TRAFFIC_SIGNALS_SOUND));
        this.trafficSignalsVibration = TrafficSignal.lookUpById(
                StringUtility.getNullableAndPositiveIntegerFromJsonObject(inputData, KEY_TRAFFIC_SIGNALS_VIBRATION));
    }


    /**
     * traffic signal properties
     *  -1: unknown (default)
     *   0: no
     *   1: yes
     */

    public enum TrafficSignal {
        NO(0, GlobalInstance.getStringResource(R.string.trafficSignalNo)),
        YES(1, GlobalInstance.getStringResource(R.string.trafficSignalYes));

        // lookup by id
        private static final Map<Integer,TrafficSignal> valuesById;
        static {
            valuesById = new HashMap<Integer,TrafficSignal>();
            for(TrafficSignal signal : TrafficSignal.values()) {
                valuesById.put(signal.id, signal);
            }
        }

        public static TrafficSignal lookUpById(Integer id) {
            return valuesById.get(id);
        }

        // constructor
        public int id;
        public String name;

        private TrafficSignal(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override public String toString() {
            return this.name;
        }
    }

    public TrafficSignal getTrafficSignalsSound() {
        return this.trafficSignalsSound;
    }

    public TrafficSignal getTrafficSignalsVibration() {
        return this.trafficSignalsVibration;
    }


    /**
     * to json
     */

    public static final String KEY_TRAFFIC_SIGNALS_SOUND = "traffic_signals_sound";
    public static final String KEY_TRAFFIC_SIGNALS_VIBRATION = "traffic_signals_vibration";

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        if (this.trafficSignalsSound != null) {
            jsonObject.put(KEY_TRAFFIC_SIGNALS_SOUND, this.trafficSignalsSound);
        }
        if (this.trafficSignalsVibration != null) {
            jsonObject.put(KEY_TRAFFIC_SIGNALS_VIBRATION, this.trafficSignalsVibration);
        }
        return jsonObject;
    }

}
