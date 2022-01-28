package org.walkersguide.android.server.wg.poi;

import java.io.Serializable;
import org.walkersguide.android.data.profile.Profile;

import java.util.ArrayList;
import org.walkersguide.android.database.util.AccessDatabase;


public class PoiProfile extends Profile implements Serializable {
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


    private ArrayList<PoiCategory> poiCategoryList;
    private boolean includeFavorites;

    public PoiProfile(long id, String name,
            ArrayList<PoiCategory> poiCategoryList, boolean includeFavorites) {
        super(id, name);
        this.poiCategoryList = poiCategoryList;
        this.includeFavorites = includeFavorites;
    }

    public ArrayList<PoiCategory> getPoiCategoryList() {
        return this.poiCategoryList;
    }

    public boolean getIncludeFavorites() {
        return this.includeFavorites;
    }

    public boolean setValues(String newName, ArrayList<PoiCategory> newPoiCategoryList, boolean newIncludeFavorites) {
        this.setName(newName);
        this.poiCategoryList = newPoiCategoryList;
        this.includeFavorites = newIncludeFavorites;
        // update in database
        return AccessDatabase.getInstance().updatePoiProfile(this);
    }

    public boolean removeFromDatabase() {
        return AccessDatabase.getInstance().removePoiProfile(this);
    }

}
