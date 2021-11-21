package org.walkersguide.android.database;

import org.walkersguide.android.util.StringUtility;
import java.io.Serializable;
import android.text.TextUtils;


public class DatabaseProfileRequest implements Serializable {
    private static final long serialVersionUID = 1l;


    private DatabaseProfile profile;
    private String searchTerm;
    private SortMethod sortMethod;

    public DatabaseProfileRequest(DatabaseProfile profile) {
        this.profile = profile;
        this.searchTerm = null;
        this.sortMethod = SortMethod.ACCESSED_DESC;
    }

    public DatabaseProfileRequest(DatabaseProfile profile, String searchTerm, SortMethod sortMethod) {
        this.profile = profile;
        this.searchTerm = searchTerm;
        this.sortMethod = sortMethod;
    }

    public DatabaseProfile getProfile() {
        return this.profile;
    }

    public void setProfile(DatabaseProfile newProfile) {
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

    public SortMethod getSortMethod() {
        return this.sortMethod;
    }

    public boolean hasDefaultSortMethod() {
        return this.sortMethod == SortMethod.ACCESSED_DESC;
    }

    public void setSortMethod(SortMethod newSortMethod) {
        this.sortMethod = newSortMethod;
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.profile.hashCode();
        if (hasSearchTerm()) {
            result = prime * result + this.searchTerm.hashCode();
        }
        result = prime * result + this.sortMethod.hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof DatabaseProfileRequest))
            return false;
        DatabaseProfileRequest other = (DatabaseProfileRequest) obj;
        return this.profile ==  other.getProfile()
            && StringUtility.compareObjects(this.searchTerm, other.getSearchTerm())
            && this.sortMethod == other.getSortMethod();
    }

}
