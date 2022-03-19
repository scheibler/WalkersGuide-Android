package org.walkersguide.android.data.profile;

import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.database.profile.FavoritesProfile;
import java.util.ArrayList;

public enum ProfileGroup {

    LOAD_PREVIOUS_ROUTE(1, false),
    POINT_HISTORY(2, false),
    ROUTE_HISTORY(3, false),
    POI(4, true);

    private int id;
    private boolean canCreateNewProfile;

    private ProfileGroup(int id, boolean canCreateNewProfile) {
        this.id = id;
        this.canCreateNewProfile = canCreateNewProfile;
    }

    public ArrayList<? extends Profile> getProfiles() {
        if (id == 1) {      // load previous route
            ArrayList<DatabaseProfile> profileList = new ArrayList<DatabaseProfile>();
            profileList.add(FavoritesProfile.favoriteRoutes());
            profileList.add(DatabaseProfile.plannedRoutes());
            profileList.add(DatabaseProfile.streetCourses());
            return profileList;

        } else if (this.id == 2) {      // point history
            ArrayList<DatabaseProfile> profileList = new ArrayList<DatabaseProfile>();
            profileList.add(DatabaseProfile.allPoints());
            profileList.add(DatabaseProfile.addressPoints());
            profileList.add(DatabaseProfile.intersectionPoints());
            profileList.add(DatabaseProfile.stationPoints());
            profileList.add(DatabaseProfile.simulatedPoints());
            return profileList;

        } else if (this.id == 3) {      // route history
            ArrayList<DatabaseProfile> profileList = new ArrayList<DatabaseProfile>();
            profileList.add(DatabaseProfile.allRoutes());
            profileList.add(DatabaseProfile.plannedRoutes());
            profileList.add(DatabaseProfile.streetCourses());
            return profileList;

        } else if (this.id == 4) {      // poi
            return AccessDatabase.getInstance().getPoiProfileList();

        } else {
            return null;
        }
    }

    public boolean getCanCreateNewProfile() {
        return this.canCreateNewProfile;
    }

}
