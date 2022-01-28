package org.walkersguide.android.server.wg.poi;

import org.walkersguide.android.data.profile.Profile;
import org.walkersguide.android.database.DatabaseProfile;

import org.walkersguide.android.data.profile.ProfileRequest;
import java.io.Serializable;
import org.walkersguide.android.util.Helper;


public class PoiProfileRequest extends ProfileRequest implements Serializable {
    private static final long serialVersionUID = 1l;


    private PoiProfile profile;
    private boolean filterByViewingDirection;

    public PoiProfileRequest(PoiProfile profile) {
        super(null);
        this.profile = profile;
        this.filterByViewingDirection = false;
    }

    public PoiProfile getProfile() {
        return this.profile;
    }

    public void setProfile(PoiProfile newProfile) {
        this.profile = newProfile;
    }

    public boolean getFilterByViewingDirection() {
        return this.filterByViewingDirection;
    }

    public void toggleFilterByViewingDirection() {
        this.filterByViewingDirection = ! this.filterByViewingDirection;
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        if (getProfile() instanceof PoiProfile) {
            PoiProfile poiProfile = (PoiProfile) getProfile();
            // profile.hashCode() in super method only compares profile ids
            // we also require the profiles poi categories and include favorites contents for the request cache
            for (PoiCategory category : poiProfile.getPoiCategoryList()) {
                result = prime * result + category.hashCode();
            }
            result = prime * result + (poiProfile.getIncludeFavorites() ? 1 : 0);
        }
        // missing filterByViewingDirection is intentional
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

            // poi category lists
            if (thisProfile.getPoiCategoryList().size() != otherProfile.getPoiCategoryList().size()) {
                return false;
            }
            for (PoiCategory category : thisProfile.getPoiCategoryList()) {
                if (! otherProfile.getPoiCategoryList().contains(category)) {
                    return false;
                }
            }
            for (PoiCategory category : otherProfile.getPoiCategoryList()) {
                if (! thisProfile.getPoiCategoryList().contains(category)) {
                    return false;
                }
            }

            // includeFavorites
            return thisProfile.getIncludeFavorites() == otherProfile.getIncludeFavorites();
        }
        return false;
    }

}
