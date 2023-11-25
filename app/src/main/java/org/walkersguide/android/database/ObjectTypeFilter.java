package org.walkersguide.android.database;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;


public enum ObjectTypeFilter {

    ALL(1, GlobalInstance.getStringResource(R.string.objectTypeFilterAll)),
    POINTS(2, GlobalInstance.getStringResource(R.string.objectTypeFilterPoints)),
    ROUTES(3, GlobalInstance.getStringResource(R.string.objectTypeFilterRoutes)),
    SEGMENTS(4, GlobalInstance.getStringResource(R.string.objectTypeFilterSegments));


    private int id;
    private String name;

    private ObjectTypeFilter(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    @Override public String toString() {
        return this.name;
    }

}
