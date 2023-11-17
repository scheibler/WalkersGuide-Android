package org.walkersguide.android.database.profile;

import org.walkersguide.android.data.Profile.Icon;
import java.io.Serializable;
import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.Profile;
import org.walkersguide.android.database.SortMethod;
import java.util.Locale;
import java.util.ArrayList;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.util.AccessDatabase;
import java.util.Arrays;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.database.DatabaseProfile.ForObjects;
import org.walkersguide.android.data.profile.MutableProfile;
import org.walkersguide.android.data.profile.MutableProfile.MutableProfileParams;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;


public class Collection extends DatabaseProfile implements MutableProfile, Serializable {
    private static final long serialVersionUID = 1l;


    public static class CollectionParams extends MutableProfileParams {
    }


    public static Collection create(String name, boolean isPinned) {
        CollectionParams params = new CollectionParams();
        params.name = name;
        params.isPinned = isPinned;
        return AccessDatabase.getInstance().addCollection(params);
    }

    public static Collection load(long id) {
        if (AccessDatabase.getInstance().getCollectionParams(id) != null) {
            return new Collection(id);
        }
        return null;
    }

    // json helper functions

    public static ArrayList<Collection> listFromJson(JSONArray jsonCollectionIdList) {
        ArrayList<Collection> collectionList = new ArrayList<Collection>();
        for (int i=0; i<jsonCollectionIdList.length(); i++) {
            Collection collection = null;
            try {
                collection = new Collection(
                        jsonCollectionIdList.getLong(i));
            } catch (JSONException e) {}
            if (collection != null) {
                collectionList.add(collection);
            }
        }
        return collectionList;
    }

    public static JSONArray listToJson(ArrayList<Collection> collectionList) {
        JSONArray jsonCollectionIdList = new JSONArray();
        for (Collection collection : collectionList) {
            jsonCollectionIdList.put(collection.getId());
        }
        return jsonCollectionIdList;
    }


    /**
     * constructor
     */

    private Collection(long id) {
        super(id, ForObjects.POINTS_AND_ROUTES, SortMethod.DISTANCE_ASC);
    }


    // profile class

    @Override public String getName() {
        return MutableProfile.super.getName();
    }

    @Override public Icon getIcon() {
        return Icon.COLLECTION;
    }


    // MutableProfile

    @Override public CollectionParams getProfileParamsFromDatabase() {
        return  AccessDatabase.getInstance().getCollectionParams(getId());
    }

    @Override public boolean rename(String newName) {
        CollectionParams params = getProfileParamsFromDatabase();
        if (params != null && !TextUtils.isEmpty(newName)) {
            params.name = newName;
            return updateProfile(params);
        }
        return false;
    }

    @Override public boolean setPinned(boolean pinned) {
        CollectionParams params = getProfileParamsFromDatabase();
        if (params != null) {
            params.isPinned = pinned;
            return updateProfile(params);
        }
        return false;
    }

    @Override public boolean remove() {
        return AccessDatabase.getInstance().removeCollection(this.getId());
    }

    private boolean updateProfile(CollectionParams newParams) {
        return AccessDatabase.getInstance().updateCollection(this.getId(), newParams);
    }

}
