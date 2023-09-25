package org.walkersguide.android.data.object_with_id.common;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;
import java.util.Map;
import java.util.HashMap;


public enum ObjectClass {

    POINT(
            1,
            R.drawable.favorite,
            GlobalInstance.getStringResource(R.string.objectTypePoint)),
    ROUTE(
            2,
            R.drawable.favorite,
            GlobalInstance.getStringResource(R.string.objectTypeRoute)),
    SEGMENT(
            3,
            R.drawable.favorite,
            GlobalInstance.getStringResource(R.string.objectTypeSegment));

    // lookup by id
    private static final Map<Integer,ObjectClass> valuesById;
    static {
        valuesById = new HashMap<Integer,ObjectClass>();
        for(ObjectClass objectClass : ObjectClass.values()) {
            valuesById.put(objectClass.id, objectClass);
        }
    }

    public static ObjectClass lookUpById(Integer id) {
        return valuesById.get(id);
    }

    // constructor
    public int id, iconResId;
    public String name;

    private ObjectClass(int id, int iconResId, String name) {
        this.id = id;
        this.iconResId = iconResId;
        this.name = name;
    }

    @Override public String toString() {
        return this.name;
    }

}
