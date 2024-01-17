package org.walkersguide.android.database;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;


public enum SortMethod {

    DISTANCE_ASC(1, GlobalInstance.getStringResource(R.string.sortDistanceAsc)),
    DISTANCE_DESC(2, GlobalInstance.getStringResource(R.string.sortDistanceDesc)),
    NAME_ASC(3, GlobalInstance.getStringResource(R.string.sortNameAsc)),
    NAME_DESC(4, GlobalInstance.getStringResource(R.string.sortNameDesc)),
    ACCESSED_ASC(5, GlobalInstance.getStringResource(R.string.sortAccessedAsc)),
    ACCESSED_DESC(6, GlobalInstance.getStringResource(R.string.sortAccessedDesc)),
    CREATED_ASC(7, GlobalInstance.getStringResource(R.string.sortCreatedAsc)),
    CREATED_DESC(8, GlobalInstance.getStringResource(R.string.sortCreatedDesc));


    private int id;
    private String name;

    private SortMethod(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public boolean isAscending() {
        return (this.id % 2) == 1;
    }

    @Override public String toString() {
        return this.name;
    }

}
