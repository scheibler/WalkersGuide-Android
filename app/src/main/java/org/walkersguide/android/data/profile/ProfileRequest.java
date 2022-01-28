package org.walkersguide.android.data.profile;

import java.io.Serializable;
import org.walkersguide.android.util.Helper;
import android.text.TextUtils;


public abstract class ProfileRequest implements Serializable {
    private static final long serialVersionUID = 1l;


    private String searchTerm;

    public ProfileRequest(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public abstract Profile getProfile();

    public boolean hasProfile() {
        return getProfile() != null;
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

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        if (this.hasProfile()) {
            result = prime * result + getProfile().hashCode();
        }
        if (hasSearchTerm()) {
            result = prime * result + this.searchTerm.hashCode();
        }
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ProfileRequest))
            return false;
        ProfileRequest other = (ProfileRequest) obj;
        return Helper.compareObjects(this.getProfile(), other.getProfile())
                && Helper.compareObjects(this.searchTerm, other.getSearchTerm());
    }

}
