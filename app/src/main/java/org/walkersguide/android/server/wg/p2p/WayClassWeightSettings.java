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
import android.text.TextUtils;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;


public class WayClassWeightSettings implements Serializable {
    private static final long serialVersionUID = 1l;

    public static WayClassWeightSettings createShortestRoute() {
        return new WayClassWeightSettings(
                ID_SHORTEST_ROUTE,
                GlobalInstance.getStringResource(R.string.wcwsPresetShortestRoute),
                weightsForShortestRoute());
    }

    private static final String ID_SHORTEST_ROUTE = "SHORTEST_ROUTE";
    private static Map<WayClassType,WayClassWeight> weightsForShortestRoute() {
        Map<WayClassType,WayClassWeight> map = new LinkedHashMap<>();
        for (WayClassType type : WayClassType.values()) {
            map.put(type, WayClassWeight.NEUTRAL);
        }
        return map;
    }

    public static WayClassWeightSettings createUrbanOnFoot() {
        return new WayClassWeightSettings(
                ID_URBAN_ON_FOOT,
                GlobalInstance.getStringResource(R.string.wcwsPresetUrbanOnFoot),
                weightsForUrbanOnFoot());
    }

    private static final String ID_URBAN_ON_FOOT = "URBAN_ON_FOOT";
    private static Map<WayClassType,WayClassWeight> weightsForUrbanOnFoot() {
        Map<WayClassType,WayClassWeight> map = new LinkedHashMap<>();
        map.put(WayClassType.BIG_STREETS, WayClassWeight.SLIGHTLY_PREFER);
        map.put(WayClassType.SMALL_STREETS, WayClassWeight.STRONGLY_PREFER);
        map.put(WayClassType.PAVED_WAYS, WayClassWeight.NEUTRAL);
        map.put(WayClassType.UNPAVED_WAYS, WayClassWeight.AVOID);
        map.put(WayClassType.STEPS, WayClassWeight.SLIGHTLY_AVOID);
        map.put(WayClassType.UNCLASSIFIED_WAYS, WayClassWeight.AVOID);
        return map;
    }

    public static WayClassWeightSettings createUrbanByCar() {
        return new WayClassWeightSettings(
                ID_URBAN_BY_CAR,
                GlobalInstance.getStringResource(R.string.wcwsPresetUrbanByCar),
                weightsForUrbanByCar());
    }

    private static final String ID_URBAN_BY_CAR = "URBAN_BY_CAR";
    private static Map<WayClassType,WayClassWeight> weightsForUrbanByCar() {
        Map<WayClassType,WayClassWeight> map = new LinkedHashMap<>();
        map.put(WayClassType.BIG_STREETS, WayClassWeight.STRONGLY_PREFER);
        map.put(WayClassType.SMALL_STREETS, WayClassWeight.NEUTRAL);
        map.put(WayClassType.PAVED_WAYS, WayClassWeight.EXCLUDE);
        map.put(WayClassType.UNPAVED_WAYS, WayClassWeight.EXCLUDE);
        map.put(WayClassType.STEPS, WayClassWeight.EXCLUDE);
        map.put(WayClassType.UNCLASSIFIED_WAYS, WayClassWeight.EXCLUDE);
        return map;
    }

    public static WayClassWeightSettings createHiking() {
        return new WayClassWeightSettings(
                ID_HIKING,
                GlobalInstance.getStringResource(R.string.wcwsPresetHiking),
                weightsForHiking());
    }

    private static final String ID_HIKING = "HIKING";
    private static Map<WayClassType,WayClassWeight> weightsForHiking() {
        Map<WayClassType,WayClassWeight> map = new LinkedHashMap<>();
        map.put(WayClassType.BIG_STREETS, WayClassWeight.EXCLUDE);
        map.put(WayClassType.SMALL_STREETS, WayClassWeight.AVOID);
        map.put(WayClassType.PAVED_WAYS, WayClassWeight.STRONGLY_PREFER);
        map.put(WayClassType.UNPAVED_WAYS, WayClassWeight.NEUTRAL);
        map.put(WayClassType.STEPS, WayClassWeight.AVOID);
        map.put(WayClassType.UNCLASSIFIED_WAYS, WayClassWeight.AVOID);
        return map;
    }

    public static List<WayClassWeightSettings> allSettings() {
        List<WayClassWeightSettings> wayClassWeightSettingsList = new ArrayList<>();
        wayClassWeightSettingsList.add(WayClassWeightSettings.createShortestRoute());
        wayClassWeightSettingsList.add(WayClassWeightSettings.createUrbanOnFoot());
        wayClassWeightSettingsList.add(WayClassWeightSettings.createUrbanByCar());
        wayClassWeightSettingsList.add(WayClassWeightSettings.createHiking());
        return wayClassWeightSettingsList;
    }


    private String id, name;
    private Map<WayClassType,WayClassWeight> typeWeightMap;

    public WayClassWeightSettings(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.typeWeightMap = weightsForShortestRoute();
    }

    private WayClassWeightSettings(String id, String name, Map<WayClassType,WayClassWeight> typeWeightMap) {
        this.id = id;
        this.name = name;
        this.typeWeightMap = typeWeightMap;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public WayClassWeight getWeightFor(WayClassType type) {
        return this.typeWeightMap.get(type);
    }

    public void setWeightFor(WayClassType type, WayClassWeight newWeight) {
        if (type != null && newWeight != null) {
            this.typeWeightMap.put(type, newWeight);
        }
    }

    public String formatWayClassWeightsForContentDescription() {
        List<String> formattedWayClassWeightSettings = new ArrayList<>();
        for (Map.Entry<WayClassType,WayClassWeight> entry : typeWeightMap.entrySet()) {
            formattedWayClassWeightSettings.add(
                    String.format("%1$s: %2$s", entry.getKey().label, entry.getValue().label));
        }
        return TextUtils.join(".\n", formattedWayClassWeightSettings);
    }

    public JSONObject serializeWayClassWeightsForServerRequest() throws JSONException {
        JSONObject jsonWayClassTypeAndWeightMappings = new JSONObject();
        for (Map.Entry<WayClassType,WayClassWeight> entry : typeWeightMap.entrySet()) {
            jsonWayClassTypeAndWeightMappings.put(
                    entry.getKey().name().toLowerCase(Locale.ROOT), entry.getValue().weight);
        }
        return jsonWayClassTypeAndWeightMappings;
    }

    @Override public String toString() {
        return this.name;
    }

    @Override public int hashCode() {
        return this.id.hashCode();
    }

    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        else if (obj == null) return false;
        else if (! (obj instanceof WayClassWeightSettings)) return false;

        WayClassWeightSettings other = (WayClassWeightSettings) obj;
        return this.id.equals(other.getId());
    }

}
