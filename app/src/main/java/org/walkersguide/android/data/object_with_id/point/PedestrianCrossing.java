package org.walkersguide.android.data.object_with_id.point;

import org.walkersguide.android.R;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.data.object_with_id.Point;
import java.util.ArrayList;


public class PedestrianCrossing extends Point implements Serializable {
    private static final long serialVersionUID = 1l;

    private Boolean island, trafficSignalsSound, trafficSignalsVibration;
    private CrossingBarrier crossingBarrier;
    private Kerb kerb;

    public PedestrianCrossing(JSONObject inputData) throws JSONException {
        super(inputData);
        this.island = Helper.getNullableBooleanFromJsonObject(inputData, KEY_ISLAND);
        this.trafficSignalsSound = Helper.getNullableBooleanFromJsonObject(inputData, KEY_TRAFFIC_SIGNALS_SOUND);
        this.trafficSignalsVibration = Helper.getNullableBooleanFromJsonObject(inputData, KEY_TRAFFIC_SIGNALS_VIBRATION);
        this.crossingBarrier = Helper.getNullableEnumFromJsonObject(
                inputData, KEY_CROSSING_BARRIER, CrossingBarrier.class);
        this.kerb = Helper.getNullableEnumFromJsonObject(
                inputData, KEY_KERB, Kerb.class);
    }

    public Boolean getIsland() {
        return this.island;
    }

    public Boolean getTrafficSignalsSound() {
        return this.trafficSignalsSound;
    }

    public Boolean getTrafficSignalsVibration() {
        return this.trafficSignalsVibration;
    }

    @Override public String toString() {
        String description = super.toString();

        // second line: crossing attributes
        ArrayList<String> attributeList = new ArrayList<String>();

        if (this.crossingBarrier != null) {
            switch (this.crossingBarrier) {
                case NO:
                    attributeList.add(
                            GlobalInstance.getStringResource(R.string.pedestrianCrossingDetailsCrossingBarrierNo));
                    break;
                case YES:
                    attributeList.add(
                            GlobalInstance.getStringResource(R.string.pedestrianCrossingDetailsCrossingBarrierYes));
                    break;
                default:
                    attributeList.add(this.crossingBarrier.toString());
                    break;
            }
        }

        if (Boolean.TRUE.equals(this.island)) {
            attributeList.add(
                    GlobalInstance.getStringResource(R.string.pedestrianCrossingDetailsIslandYes));
        }
        if (Boolean.TRUE.equals(this.trafficSignalsSound)) {
            attributeList.add(
                    GlobalInstance.getStringResource(R.string.pedestrianCrossingDetailsSoundYes));
        }

        if (this.kerb != null) {
            switch (this.kerb) {
                case NO:
                case FLUSH:
                    attributeList.add(
                            GlobalInstance.getStringResource(R.string.pedestrianCrossingDetailsKerbNo));
                    break;
            }
        }

        if (super.getTactilePaving() != null) {
            switch (super.getTactilePaving()) {
                case YES:
                    attributeList.add(
                            GlobalInstance.getStringResource(R.string.pedestrianCrossingDetailsTactilePavingYes));
                    break;
            }
        }

        if (! attributeList.isEmpty()) {
            description += System.lineSeparator();
            description += String.format(
                    "%1$s %2$s",
                    GlobalInstance.getStringResource(R.string.fillingWordHas),
                    Helper.formatStringListWithFillWordAnd(attributeList));
        }
        return description;
    }


    /**
     * crossing barrier
     */

    public enum CrossingBarrier {
        FULL(GlobalInstance.getStringResource(R.string.crossingBarrierFull)),
        HALF(GlobalInstance.getStringResource(R.string.crossingBarrierHalf)),
        NO(GlobalInstance.getStringResource(R.string.crossingBarrierNo)),
        YES(GlobalInstance.getStringResource(R.string.crossingBarrierYes));

        private String label;

        private CrossingBarrier(String label) {
            this.label = label;
        }

        @Override public String toString() {
            return this.label;
        }
    }

    public CrossingBarrier getCrossingBarrier() {
        return this.crossingBarrier;
    }


    /**
     * kerb
     */

    public enum Kerb {
        FLUSH(GlobalInstance.getStringResource(R.string.kerbFlush)),
        LOWERED(GlobalInstance.getStringResource(R.string.kerbLowered)),
        ROLLED(GlobalInstance.getStringResource(R.string.kerbRolled)),
        RAISED(GlobalInstance.getStringResource(R.string.kerbRaised)),
        YES(GlobalInstance.getStringResource(R.string.kerbYes)),
        NO(GlobalInstance.getStringResource(R.string.kerbNo));

        private String label;

        private Kerb(String label) {
            this.label = label;
        }

        @Override public String toString() {
            return this.label;
        }
    }

    public Kerb getKerb() {
        return this.kerb;
    }


    /**
     * to json
     */

    public static final String KEY_CROSSING_BARRIER = "crossing_barrier";
    public static final String KEY_KERB = "kerb";
    public static final String KEY_ISLAND = "island";
    public static final String KEY_TRAFFIC_SIGNALS_SOUND = "traffic_signals_sound";
    public static final String KEY_TRAFFIC_SIGNALS_VIBRATION = "traffic_signals_vibration";

    @Override public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = super.toJson();
        // everything is optional
        jsonObject = Helper.putNullableBooleanToJsonObject(jsonObject, KEY_ISLAND, this.island);
        // the next two Boolean must produce a 0/1 int as bool representation for now
        // that's the last "true" for
        jsonObject = Helper.putNullableBooleanToJsonObject(
                jsonObject, KEY_TRAFFIC_SIGNALS_SOUND, this.trafficSignalsSound, true);
        jsonObject = Helper.putNullableBooleanToJsonObject(
                jsonObject, KEY_TRAFFIC_SIGNALS_VIBRATION, this.trafficSignalsVibration, true);
        // and the two new enums which already are represented by their values in json
        jsonObject = Helper.putNullableEnumToJsonObject(jsonObject, KEY_CROSSING_BARRIER, this.crossingBarrier);
        jsonObject = Helper.putNullableEnumToJsonObject(jsonObject, KEY_KERB, this.kerb);
        return jsonObject;
    }

}
