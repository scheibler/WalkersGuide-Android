package org.walkersguide.android.server.poi;

import java.io.Serializable;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.util.SettingsManager;
import android.text.TextUtils;


public class PoiProfileRequest implements Serializable {
    private static final long serialVersionUID = 1l;


    private PoiProfile profile;
    private String searchTerm;
    private boolean filterByViewingDirection;

    public PoiProfileRequest(PoiProfile profile) {
        this.profile = profile;
        this.searchTerm = null;
        this.filterByViewingDirection = false;
    }

    public PoiProfile getProfile() {
        return this.profile;
    }

    public boolean hasProfile() {
        return this.profile != null;
    }

    public void setProfile(PoiProfile newProfile) {
        this.profile = newProfile;
    }

    public String getSearchTerm() {
        return this.searchTerm;
    }

    public boolean hasSearchTerm() {
        return ! TextUtils.isEmpty(this.searchTerm);
    }

    public void setSearchTerm(String newSearchTerm) {
        this.searchTerm = newSearchTerm;
    }

    public boolean getFilterByViewingDirection() {
        return this.filterByViewingDirection;
    }

    public void toggleFilterByViewingDirection() {
        this.filterByViewingDirection = ! this.filterByViewingDirection;
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        if (this.hasProfile()) {
            // profile.hashCode() only compares profile ids
            result = prime * result + this.profile.hashCode();
            // we also require the profiles poi categories and include favorites contents for the request cache
            for (PoiCategory category : this.profile.getPoiCategoryList()) {
                result = prime * result + category.hashCode();
            }
            result = prime * result + (this.profile.getIncludeFavorites() ? 1 : 0);
        }
        if (hasSearchTerm()) {
            result = prime * result + this.searchTerm.hashCode();
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
        // search term
        if (! StringUtility.compareObjects(this.searchTerm, other.getSearchTerm())) {
            return false;
        }

        // poi profile comparisons
        //
        // compare profiles (ids only)
        if (! StringUtility.compareObjects(this.profile, other.getProfile())) {
            return false;
        }
        // compare some profile params
		PoiProfile thisProfile = this.profile;
		PoiProfile otherProfile = other.getProfile();
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

}
