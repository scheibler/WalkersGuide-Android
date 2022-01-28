package org.walkersguide.android.server.wg.p2p;

import org.walkersguide.android.R;
import org.walkersguide.android.server.wg.p2p.wayclass.WayClassType;
import org.walkersguide.android.server.wg.p2p.wayclass.WayClassWeight;

import java.io.Serializable;
import java.util.HashMap;


public class WayClassWeightSettings implements Serializable {
    private static final long serialVersionUID = 1l;

    public static WayClassWeightSettings getDefault() {
        HashMap<WayClassType,WayClassWeight> map = new HashMap<WayClassType,WayClassWeight>();
        for (WayClassType type : WayClassType.values()) {
            map.put(type, type.defaultWeight);
        }
        return new WayClassWeightSettings(map);
    }


    private HashMap<WayClassType,WayClassWeight> typeWeightMap;

    public WayClassWeightSettings(HashMap<WayClassType,WayClassWeight> typeWeightMap) {
        this.typeWeightMap = typeWeightMap;
    }

    public WayClassWeight getWeightFor(WayClassType type) {
        return this.typeWeightMap.get(type);
    }

    public void setWeightFor(WayClassType type, WayClassWeight newWeight) {
        if (type != null && newWeight != null) {
            this.typeWeightMap.put(type, newWeight);
        }
    }

}
