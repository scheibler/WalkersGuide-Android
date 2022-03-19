package org.walkersguide.android.database;

import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.R;


public enum SortMethod {

    ACCESSED_ASC(1, GlobalInstance.getStringResource(R.string.sortAccessedAsc)),
    ACCESSED_DESC(2, GlobalInstance.getStringResource(R.string.sortAccessedDesc)),
    NAME_ASC(3, GlobalInstance.getStringResource(R.string.sortNameAsc)),
    NAME_DESC(4, GlobalInstance.getStringResource(R.string.sortNameDesc)),
    DISTANCE_ASC(5, GlobalInstance.getStringResource(R.string.sortDistanceAsc)),
    DISTANCE_DESC(6, GlobalInstance.getStringResource(R.string.sortDistanceDesc)),
    BEARING_ASC(7, GlobalInstance.getStringResource(R.string.sortBearingAsc)),
    BEARING_DESC(8, GlobalInstance.getStringResource(R.string.sortBearingDesc)),
    CREATED_ASC(9, GlobalInstance.getStringResource(R.string.sortCreatedAsc)),
    CREATED_DESC(10, GlobalInstance.getStringResource(R.string.sortCreatedDesc));


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
