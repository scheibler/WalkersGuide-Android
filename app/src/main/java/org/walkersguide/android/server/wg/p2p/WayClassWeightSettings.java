package org.walkersguide.android.server.wg.p2p;

import org.walkersguide.android.R;
import org.walkersguide.android.server.wg.p2p.wayclass.WayClassType;
import org.walkersguide.android.server.wg.p2p.wayclass.WayClassWeight;

import java.io.Serializable;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Map;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.lang.Comparable;
import org.walkersguide.android.util.GlobalInstance;


public class WayClassWeightSettings implements Serializable {
    private static final long serialVersionUID = 1l;

    public enum Preset {
        SHORTEST_ROUTE(
                1,
                GlobalInstance.getStringResource(R.string.wcwsPresetShortestRoute),
                presetShortestRoute()),
        URBAN_ON_FOOT(
                2,
                GlobalInstance.getStringResource(R.string.wcwsPresetUrbanOnFoot),
                presetUrbanOnFoot()),
        URBAN_BY_CAR(
                3,
                GlobalInstance.getStringResource(R.string.wcwsPresetUrbanByCar),
                presetUrbanByCar()),
        HIKING(
                4,
                GlobalInstance.getStringResource(R.string.wcwsPresetHiking),
                presetHiking());

        public static Preset matchesId(int presetId) {
            for (Preset preset : values()) {
                if (preset.id == presetId) {
                    return preset;
                }
            }
            return null;
        }

        public static Preset matchesSettings(WayClassWeightSettings settings) {
            for (Preset preset : values()) {
                if (preset.settings.equals(settings)) {
                    return preset;
                }
            }
            return null;
        }

        public int id;
        public String label;
        public WayClassWeightSettings settings;

        private Preset(int id, String label, WayClassWeightSettings settings) {
            this.id = id;
            this.label = label;
            this.settings = settings;
        }

        @Override public String toString() {
            return this.label.replace(" ", "\u00A0");
        }

        private static WayClassWeightSettings presetShortestRoute() {
            LinkedHashMap<WayClassType,WayClassWeight> map = new LinkedHashMap<WayClassType,WayClassWeight>();
            for (WayClassType type : WayClassType.values()) {
                map.put(type, WayClassWeight.NEUTRAL);
            }
            return new WayClassWeightSettings(map);
        }

        private static WayClassWeightSettings presetUrbanOnFoot() {
            LinkedHashMap<WayClassType,WayClassWeight> map = new LinkedHashMap<WayClassType,WayClassWeight>();
            map.put(WayClassType.BIG_STREETS, WayClassWeight.SLIGHTLY_PREFER);
            map.put(WayClassType.SMALL_STREETS, WayClassWeight.STRONGLY_PREFER);
            map.put(WayClassType.PAVED_WAYS, WayClassWeight.NEUTRAL);
            map.put(WayClassType.UNPAVED_WAYS, WayClassWeight.AVOID);
            map.put(WayClassType.STEPS, WayClassWeight.SLIGHTLY_AVOID);
            map.put(WayClassType.UNCLASSIFIED_WAYS, WayClassWeight.AVOID);
            return new WayClassWeightSettings(map);
        }

        private static WayClassWeightSettings presetUrbanByCar() {
            LinkedHashMap<WayClassType,WayClassWeight> map = new LinkedHashMap<WayClassType,WayClassWeight>();
            map.put(WayClassType.BIG_STREETS, WayClassWeight.STRONGLY_PREFER);
            map.put(WayClassType.SMALL_STREETS, WayClassWeight.NEUTRAL);
            map.put(WayClassType.PAVED_WAYS, WayClassWeight.EXCLUDE);
            map.put(WayClassType.UNPAVED_WAYS, WayClassWeight.EXCLUDE);
            map.put(WayClassType.STEPS, WayClassWeight.EXCLUDE);
            map.put(WayClassType.UNCLASSIFIED_WAYS, WayClassWeight.EXCLUDE);
            return new WayClassWeightSettings(map);
        }

        private static WayClassWeightSettings presetHiking() {
            LinkedHashMap<WayClassType,WayClassWeight> map = new LinkedHashMap<WayClassType,WayClassWeight>();
            map.put(WayClassType.BIG_STREETS, WayClassWeight.EXCLUDE);
            map.put(WayClassType.SMALL_STREETS, WayClassWeight.AVOID);
            map.put(WayClassType.PAVED_WAYS, WayClassWeight.STRONGLY_PREFER);
            map.put(WayClassType.UNPAVED_WAYS, WayClassWeight.NEUTRAL);
            map.put(WayClassType.STEPS, WayClassWeight.AVOID);
            map.put(WayClassType.UNCLASSIFIED_WAYS, WayClassWeight.AVOID);
            return new WayClassWeightSettings(map);
        }
    }


    private LinkedHashMap<WayClassType,WayClassWeight> typeWeightMap;

    public WayClassWeightSettings(LinkedHashMap<WayClassType,WayClassWeight> typeWeightMap) {
        this.typeWeightMap = typeWeightMap;
    }

    public int getNumberOfEntries() {
        return this.typeWeightMap.size();
    }

    public WayClassWeight getWeightFor(WayClassType type) {
        return this.typeWeightMap.get(type);
    }

    public void setWeightFor(WayClassType type, WayClassWeight newWeight) {
        if (type != null && newWeight != null) {
            this.typeWeightMap.put(type, newWeight);
        }
    }

    @Override public int hashCode() {
        int result = 1;
        for (Map.Entry<WayClassType,WayClassWeight> entry : typeWeightMap.entrySet()) {
            result += Double.valueOf(entry.getValue().weight).hashCode();
        }
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (! (obj instanceof WayClassWeightSettings)) {
            return false;
        }
        WayClassWeightSettings other = (WayClassWeightSettings) obj;
        if (this.typeWeightMap.size() != other.getNumberOfEntries()) {
            return false;
        }
        for (Map.Entry<WayClassType,WayClassWeight> entry : this.typeWeightMap.entrySet()) {
            if (entry.getValue() != other.getWeightFor(entry.getKey())) {
                return false;
            }
        }
        return true;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonWayClassTypeAndWeightMappings = new JSONObject();
        for (Map.Entry<WayClassType,WayClassWeight> entry : typeWeightMap.entrySet()) {
            jsonWayClassTypeAndWeightMappings.put(
                    entry.getKey().name().toLowerCase(Locale.ROOT), entry.getValue().weight);
        }
        return jsonWayClassTypeAndWeightMappings;
    }

}
