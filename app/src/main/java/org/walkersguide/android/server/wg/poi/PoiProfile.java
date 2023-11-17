package org.walkersguide.android.server.wg.poi;

import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.data.Profile.Icon;
import org.walkersguide.android.data.profile.MutableProfile;
import org.walkersguide.android.data.profile.MutableProfile.MutableProfileParams;
import org.walkersguide.android.data.Profile;

import java.util.ArrayList;
import org.walkersguide.android.database.util.AccessDatabase;
import android.text.TextUtils;
import org.walkersguide.android.database.profile.Collection;
import org.walkersguide.android.util.GlobalInstance;


public class PoiProfile extends Profile implements MutableProfile, Serializable {
    private static final long serialVersionUID = 1l;

    public static class PoiProfileParams extends MutableProfileParams {
        public ArrayList<PoiCategory> poiCategoryList;
        public ArrayList<Collection> collectionList;
    }


    public static PoiProfile create(String name, ArrayList<PoiCategory> poiCategoryList, ArrayList<Collection> collectionList) {
        PoiProfileParams params = new PoiProfileParams();
        params.name = name;
        params.isPinned = false;
        params.poiCategoryList = poiCategoryList;
        params.collectionList = collectionList;
        return AccessDatabase.getInstance().addPoiProfile(params);
    }

    public static PoiProfile load(long id) {
        if (AccessDatabase.getInstance().getPoiProfileParams(id) != null) {
            return new PoiProfile(id);
        }
        return null;
    }


    /**
     * constructor
     */

    private PoiProfile(long id) {
        super(id);
    }


    // profile class

    @Override public String getName() {
        return MutableProfile.super.getName();
    }

    @Override public Icon getIcon() {
        return Icon.POI_PROFILE;
    }

    @Override public String toString() {
        String text = super.toString();
        PoiProfileParams params = getProfileParamsFromDatabase();
        if (params != null) {
            // number of selected poi categories is mandatory
            String secondLine = GlobalInstance.getPluralResource(
                    R.plurals.poiCategory, params.poiCategoryList.size());
            // number of collections is optional
            if (! params.collectionList.isEmpty()) {
                secondLine += String.format(
                        ", %1$s",
                        GlobalInstance.getPluralResource(
                            R.plurals.collection, params.collectionList.size()));
            }
            text += String.format("\n%1$s", secondLine);
        }
        return text;
    }


    // MutableProfile

    @Override public PoiProfileParams getProfileParamsFromDatabase() {
        return  AccessDatabase.getInstance().getPoiProfileParams(getId());
    }

    @Override public boolean rename(String newName) {
        PoiProfileParams params = getProfileParamsFromDatabase();
        if (params != null && !TextUtils.isEmpty(newName)) {
            params.name = newName;
            return updateProfile(params);
        }
        return false;
    }

    @Override public boolean setPinned(boolean pinned) {
        PoiProfileParams params = getProfileParamsFromDatabase();
        if (params != null) {
            params.isPinned = pinned;
            return updateProfile(params);
        }
        return false;
    }

    @Override public boolean remove() {
        return AccessDatabase.getInstance().removePoiProfile(this.getId());
    }


    // params specific to this class

    public ArrayList<PoiCategory> getPoiCategoryList() {
        PoiProfileParams params = getProfileParamsFromDatabase();
        return params != null ? params.poiCategoryList : new ArrayList<PoiCategory>();
    }

    public boolean setPoiCategoryList(ArrayList<PoiCategory> newPoiCategoryList) {
        PoiProfileParams params = getProfileParamsFromDatabase();
        if (params != null && newPoiCategoryList != null) {
            params.poiCategoryList = newPoiCategoryList;
            return updateProfile(params);
        }
        return false;
    }

    public ArrayList<Collection> getCollectionList() {
        PoiProfileParams params = getProfileParamsFromDatabase();
        return params != null ? params.collectionList : new ArrayList<Collection>();
    }

    public boolean setCollectionList(ArrayList<Collection> newCollectionList) {
        PoiProfileParams params = getProfileParamsFromDatabase();
        if (params != null && newCollectionList != null) {
            params.collectionList = newCollectionList;
            return updateProfile(params);
        }
        return false;
    }

    private boolean updateProfile(PoiProfileParams newParams) {
        return AccessDatabase.getInstance().updatePoiProfile(this.getId(), newParams);
    }

}
