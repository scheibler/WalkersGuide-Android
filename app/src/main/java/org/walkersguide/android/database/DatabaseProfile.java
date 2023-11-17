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

    public enum ForObjects {

        POINTS(
                GlobalInstance.getStringResource(R.string.databaseProfileForPoints),
                R.plurals.point),
        ROUTES(
                GlobalInstance.getStringResource(R.string.databaseProfileForRoutes),
                R.plurals.route),
        POINTS_AND_ROUTES(
                GlobalInstance.getStringResource(R.string.databaseProfileForPointsAndRoutes),
                R.plurals.pointAndRoute),
        SEGMENTS(
                GlobalInstance.getStringResource(R.string.databaseProfileForSegments),
                R.plurals.way);

        public static ArrayList<SortMethod> getSortMethodList(ForObjects forObjects) {
            ArrayList<SortMethod> sortMethodList = new ArrayList<SortMethod>();
            switch (forObjects) {
                case POINTS:
                case POINTS_AND_ROUTES:
                    sortMethodList.add(SortMethod.DISTANCE_ASC);
                    sortMethodList.add(SortMethod.DISTANCE_DESC);
                    break;
            }
            sortMethodList.add(SortMethod.NAME_ASC);
            sortMethodList.add(SortMethod.NAME_DESC);
            sortMethodList.add(SortMethod.ACCESSED_ASC);
            sortMethodList.add(SortMethod.ACCESSED_DESC);
            sortMethodList.add(SortMethod.CREATED_ASC);
            sortMethodList.add(SortMethod.CREATED_DESC);
            return sortMethodList;
        }

        public String name;
        public int pluralResId;

        private ForObjects(String name, int pluralResId) {
            this.name = name;
            this.pluralResId =pluralResId;
        }
    }

    public static DatabaseProfile load(long id) {
        Profile profile = Profile.load(id);
        return profile instanceof DatabaseProfile ? (DatabaseProfile) profile : null;
    }


    /**
     * constructor
     */

    private ForObjects forObjects;
    private SortMethod defaultSortMethod;

    protected DatabaseProfile(long id, ForObjects forObjects, SortMethod defaultSortMethod) {
        super(id);
        this.forObjects = forObjects;
        this.defaultSortMethod =
            getSortMethodList().contains(defaultSortMethod)
            ? defaultSortMethod
            : getSortMethodList().get(0);
    }

    public ForObjects getForObjects() {
        return this.forObjects;
    }

    public int getPluralResId() {
        return this.forObjects.pluralResId;
    }

    public ArrayList<SortMethod> getSortMethodList() {
        return ForObjects.getSortMethodList(this.forObjects);
    }

    public SortMethod getDefaultSortMethod() {
        return this.defaultSortMethod;
    }

    public boolean isForPoints() {
        switch (forObjects) {
            case POINTS:
            case POINTS_AND_ROUTES:
                return true;
        }
        return false;
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
