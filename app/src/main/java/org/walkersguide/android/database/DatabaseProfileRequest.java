package org.walkersguide.android.database;

import org.walkersguide.android.data.profile.Profile;
import org.walkersguide.android.database.DatabaseProfile;

import org.walkersguide.android.data.profile.ProfileRequest;
import org.walkersguide.android.util.Helper;
import java.io.Serializable;
import android.text.TextUtils;


public class DatabaseProfileRequest extends ProfileRequest implements Serializable {
    private static final long serialVersionUID = 1l;


    private DatabaseProfile profile;
    private SortMethod sortMethod;

    public DatabaseProfileRequest(DatabaseProfile profile) {
        super(null);
        this.profile = profile;
        this.sortMethod = SortMethod.ACCESSED_DESC;
    }

    public DatabaseProfileRequest(DatabaseProfile profile, String searchTerm, SortMethod sortMethod) {
        super(searchTerm);
        this.profile = profile;
        this.sortMethod = sortMethod;
    }

    public DatabaseProfile getProfile() {
        return this.profile;
    }

    public void setProfile(DatabaseProfile newProfile) {
        this.profile = newProfile;
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
        int result = super.hashCode();
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
        return super.equals((ProfileRequest) other)
            && this.sortMethod == other.getSortMethod();
    }

}
