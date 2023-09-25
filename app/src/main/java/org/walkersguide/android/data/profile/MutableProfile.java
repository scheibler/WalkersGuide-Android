package org.walkersguide.android.data.profile;

import org.walkersguide.android.data.Profile;
import java.io.Serializable;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import org.walkersguide.android.database.profile.Collection;


public interface MutableProfile extends Serializable {

    public static class MutableProfileParams {
        public String name;
        public boolean isPinned;
    }


    public static MutableProfile load(long id) {
        PoiProfile poiProfile = PoiProfile.load(id);
        return poiProfile != null ? poiProfile : Collection.load(id);
    }


    public long getId();

    public default String getName() {
        MutableProfileParams params = getProfileParamsFromDatabase();
        return params != null ? params.name : "";
    }

    public default boolean isPinned() {
        MutableProfileParams params = getProfileParamsFromDatabase();
        return params != null ? params.isPinned : false;
    }

    public default boolean profileWasRemoved() {
        return getProfileParamsFromDatabase() == null;
    }


    public  MutableProfileParams getProfileParamsFromDatabase();
    public boolean setPinned(boolean pinned);
    public boolean renameProfile(String newName);
    public boolean removeProfile();

}
