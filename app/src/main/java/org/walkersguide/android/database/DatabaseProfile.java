package org.walkersguide.android.database;

import org.walkersguide.android.data.Profile.Icon;
import org.walkersguide.android.database.profile.Collection;
import org.walkersguide.android.database.profile.StaticProfile;
import java.io.Serializable;
import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.Profile;
import java.util.Locale;
import java.util.ArrayList;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.util.AccessDatabase;
import java.util.Arrays;


public abstract class DatabaseProfile extends Profile implements Serializable {
    private static final long serialVersionUID = 1l;


    public static DatabaseProfile load(long id) {
        Profile profile = Profile.load(id);
        return profile instanceof DatabaseProfile ? (DatabaseProfile) profile : null;
    }


    /**
     * constructor
     */

    private int pluralResId;
    private SortMethod defaultSortMethod;

    protected DatabaseProfile(long id, int pluralResId, SortMethod defaultSortMethod) {
        super(id);
        this.pluralResId = pluralResId;
        this.defaultSortMethod = defaultSortMethod;
    }

    public int getPluralResId() {
        return this.pluralResId;
    }

    public SortMethod getDefaultSortMethod() {
        return this.defaultSortMethod;
    }


    // profile class

    @Override public Icon getIcon() {
        return Icon.DATABASE;
    }


    // add / remove object from / to profile

    public boolean containsObject(ObjectWithId object) {
        return AccessDatabase
            .getInstance()
            .getDatabaseProfileListFor(object)
            .contains(this);
    }

    public boolean addObject(ObjectWithId object) {
        return AccessDatabase
            .getInstance()
            .addObjectToDatabaseProfile(object, this);
    }

    public boolean removeObject(ObjectWithId object) {
        return AccessDatabase
            .getInstance()
            .removeObjectFromDatabaseProfile(object, this);
    }

}
