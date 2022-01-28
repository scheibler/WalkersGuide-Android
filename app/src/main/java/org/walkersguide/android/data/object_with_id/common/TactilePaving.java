package org.walkersguide.android.data.object_with_id.common;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import java.util.Map;
import java.util.HashMap;


public enum TactilePaving {

    NO(0, GlobalInstance.getStringResource(R.string.tactilePavingNo)),
    YES(1, GlobalInstance.getStringResource(R.string.tactilePavingYes)),
    INCORRECT(2, GlobalInstance.getStringResource(R.string.tactilePavingIncorrect));

    // lookup by id
    private static final Map<Integer,TactilePaving> valuesById;
    static {
        valuesById = new HashMap<Integer,TactilePaving>();
        for(TactilePaving tactilePaving : TactilePaving.values()) {
            valuesById.put(tactilePaving.id, tactilePaving);
        }
    }

    public static TactilePaving lookUpById(Integer id) {
        return valuesById.get(id);
    }

    // constructor
    public int id;
    public String name;

    private TactilePaving(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override public String toString() {
        return this.name;
    }

}
