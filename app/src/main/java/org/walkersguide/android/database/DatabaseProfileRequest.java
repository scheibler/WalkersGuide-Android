package org.walkersguide.android.database;

import org.walkersguide.android.data.Profile;
import org.walkersguide.android.database.DatabaseProfile;

import org.walkersguide.android.data.profile.ProfileRequest;
import org.walkersguide.android.util.Helper;
import java.io.Serializable;
import android.text.TextUtils;
import org.walkersguide.android.database.profile.Collection;


public class DatabaseProfileRequest extends ProfileRequest implements Serializable {
    private static final long serialVersionUID = 1l;


    private DatabaseProfile profile;
    private ObjectTypeFilter objectTypeFilter;
    private SortMethod sortMethod;

    public DatabaseProfileRequest(DatabaseProfile profile) {
        super(null);
        this.profile = profile;
        this.objectTypeFilter = profile instanceof Collection ? ObjectTypeFilter.ALL : null;
        this.sortMethod = profile.getDefaultSortMethod();
    }

    public DatabaseProfileRequest(DatabaseProfile profile, String searchTerm, SortMethod sortMethod) {
        super(searchTerm);
        this.profile = profile;
        this.objectTypeFilter = profile instanceof Collection ? ObjectTypeFilter.ALL : null;
        this.sortMethod = sortMethod;
    }

    public DatabaseProfile getProfile() {
        return this.profile;
    }

    public void setProfile(DatabaseProfile newProfile) {
        this.profile = newProfile;
    }

    public ObjectTypeFilter getObjectTypeFilter() {
        return this.objectTypeFilter;
    }

    public boolean hasObjectTypeFilter() {
        return this.objectTypeFilter != null;
    }

    public void setObjectTypeFilter(ObjectTypeFilter newObjectTypeFilter) {
        this.objectTypeFilter = newObjectTypeFilter;
    }

    public SortMethod getSortMethod() {
        return this.sortMethod;
    }

    public boolean hasDefaultSortMethod() {
        return this.sortMethod == this.profile.getDefaultSortMethod();
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
