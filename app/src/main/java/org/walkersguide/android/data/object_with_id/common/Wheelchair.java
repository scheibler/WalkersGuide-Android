    package org.walkersguide.android.data.object_with_id.common;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import java.util.Map;
import java.util.HashMap;


public enum Wheelchair {

    NO(0, GlobalInstance.getStringResource(R.string.wheelchairNo)),
    LIMITED(1, GlobalInstance.getStringResource(R.string.wheelchairLimited)),
    YES(2, GlobalInstance.getStringResource(R.string.wheelchairYes));

    // lookup by id
    private static final Map<Integer,Wheelchair> valuesById;
    static {
        valuesById = new HashMap<Integer,Wheelchair>();
        for(Wheelchair wheelchair : Wheelchair.values()) {
            valuesById.put(wheelchair.id, wheelchair);
        }
    }

    public static Wheelchair lookUpById(Integer id) {
        return valuesById.get(id);
    }

    // constructor
    public int id;
    public String name;

    private Wheelchair(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override public String toString() {
        return this.name;
    }

}
