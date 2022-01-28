package org.walkersguide.android.database.profile;

import java.io.Serializable;
import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.profile.Profile;
import java.util.Locale;
import java.util.ArrayList;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.database.DatabaseProfile;


public class FavoritesProfile extends DatabaseProfile implements Serializable {
    private static final long serialVersionUID = 1l;


    public static FavoritesProfile create(long id) {
        if (id == ID_FAVORITE_POINTS) {
            return favoritePoints();
        } else if (id == ID_FAVORITE_ROUTES) {
            return favoriteRoutes();
        } else {
            return null;
        }
    }

    public static ArrayList<FavoritesProfile> favoritesProfileList() {
        ArrayList<FavoritesProfile> profileList = new ArrayList<FavoritesProfile>();
        profileList.add(favoritePoints());
        profileList.add(favoriteRoutes());
        return profileList;
    }

    // ids
    private static final long ID_FAVORITE_POINTS = 1000000;
    private static final long ID_FAVORITE_ROUTES = 5000000;

    public static FavoritesProfile favoritePoints() {
        return new FavoritesProfile(
                ID_FAVORITE_POINTS,
                GlobalInstance.getStringResource(R.string.favoritesProfilePoints),
                DatabaseProfile.ForObject.POINTS);
    }

    public static FavoritesProfile favoriteRoutes() {
        return new FavoritesProfile(
                ID_FAVORITE_ROUTES,
                GlobalInstance.getStringResource(R.string.favoritesProfileRoutes),
                DatabaseProfile.ForObject.ROUTES);
    }


    /**
     * constructor
     */

    protected FavoritesProfile(long id, String name, ForObject forObject) {
        super(id, name, forObject);
    }

    public boolean isDefault() {
        return getId() == ID_FAVORITE_POINTS || getId() == ID_FAVORITE_ROUTES;
    }

}
