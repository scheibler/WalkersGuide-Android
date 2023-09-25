package org.walkersguide.android.server.wg.poi;

import org.walkersguide.android.data.Profile;
import org.walkersguide.android.database.DatabaseProfile;

import org.walkersguide.android.data.profile.ProfileRequest;
import java.io.Serializable;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.database.profile.Collection;


public class PoiProfileRequest extends ProfileRequest implements Serializable {
    private static final long serialVersionUID = 1l;


    private PoiProfile profile;

    public PoiProfileRequest(PoiProfile profile) {
        super(null);
        this.profile = profile;
    }

    public PoiProfile getProfile() {
        return this.profile;
    }

    public void setProfile(PoiProfile newProfile) {
        this.profile = newProfile;
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        if (getProfile() instanceof PoiProfile) {
            PoiProfile poiProfile = (PoiProfile) getProfile();
            // profile.hashCode() in super method only compares profile ids
            // we also require the selected profiles poi categories and collections for the request cache
            for (PoiCategory category : poiProfile.getPoiCategoryList()) {
                result = prime * result + category.hashCode();
            }
            for (Collection collection : poiProfile.getCollectionList()) {
                result = prime * result + collection.hashCode();
            }
        }
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof PoiProfileRequest))
            return false;
        PoiProfileRequest other = (PoiProfileRequest) obj;

        // super
        if (! super.equals((ProfileRequest) other)) {
            return false;
        }

        // compare some profile params
        if (getProfile() instanceof PoiProfile
                && other.getProfile() instanceof PoiProfile) {
            PoiProfile thisProfile = (PoiProfile) this.getProfile();
            PoiProfile otherProfile = (PoiProfile) other.getProfile();
            return Helper.compareArrayLists(
                    thisProfile.getPoiCategoryList(), otherProfile.getPoiCategoryList())
                && Helper.compareArrayLists(
                    thisProfile.getCollectionList(), otherProfile.getCollectionList());
        }
        return false;
    }

}
