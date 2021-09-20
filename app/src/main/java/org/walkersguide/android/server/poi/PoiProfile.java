package org.walkersguide.android.server.poi;

import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;
import java.io.Serializable;
import java.util.ArrayList;
import org.walkersguide.android.database.util.AccessDatabase;


public class PoiProfile implements Serializable {
    private static final long serialVersionUID = 1l;

    public static ArrayList<PoiProfile> allProfiles() {
        return AccessDatabase.getInstance().getPoiProfileList();
    }

    public static PoiProfile create(String name,
            ArrayList<PoiCategory> poiCategoryList, boolean includeFavorites) {
        return AccessDatabase.getInstance().addPoiProfile(name, poiCategoryList, includeFavorites);
    }

    public static PoiProfile load(long id) {
        return AccessDatabase.getInstance().getPoiProfile(id);
    }


    private long id;
    private String name;
    private ArrayList<PoiCategory> poiCategoryList;
    private boolean includeFavorites;

    public PoiProfile(long id, String name,
            ArrayList<PoiCategory> poiCategoryList, boolean includeFavorites) {
        this.id = id;
        this.name = name;
        this.poiCategoryList = poiCategoryList;
        this.includeFavorites = includeFavorites;
    }

    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public ArrayList<PoiCategory> getPoiCategoryList() {
        return this.poiCategoryList;
    }

    public boolean getIncludeFavorites() {
        return this.includeFavorites;
    }

    public boolean setValues(String newName, ArrayList<PoiCategory> newPoiCategoryList, boolean newIncludeFavorites) {
        this.name = newName;
        this.poiCategoryList = newPoiCategoryList;
        this.includeFavorites = newIncludeFavorites;
        // update in database
        return AccessDatabase.getInstance().updatePoiProfile(this);
    }

    public boolean removeFromDatabase() {
        return AccessDatabase.getInstance().removePoiProfile(this);
    }

    @Override public String toString() {
        return this.name;
    }

	@Override public int hashCode() {
        return new Long(this.id).hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof PoiProfile)) {
			return false;
        }
		PoiProfile other = (PoiProfile) obj;
        return this.id == other.getId();
    }

}
