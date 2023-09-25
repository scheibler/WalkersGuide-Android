package org.walkersguide.android.data;

import org.walkersguide.android.R;
import java.io.Serializable;
import org.walkersguide.android.util.GlobalInstance;


public abstract class Profile implements Comparable<Profile>, Serializable {
    private static final long serialVersionUID = 1l;

    public static final int LAST_STATIC_PROFILE_ID = 49;
    // id ranges
    // StaticProfile: 10 <= ID <= 39
    // HistoryProfile: 40 <= ID <= 99
    // PoiProfile: 100 <= ID <= 999999
    // Collection: 1000000 <= ID <= 9999999


    public enum Icon {

        DATABASE(
                R.drawable.image_profile_icon_local,
                GlobalInstance.getStringResource(R.string.profileIconDatabase)),
        COLLECTION(
                R.drawable.image_profile_icon_local,
                GlobalInstance.getStringResource(R.string.profileIconCollection)),
        HISTORY(
                R.drawable.image_profile_icon_local,
                GlobalInstance.getStringResource(R.string.profileIconHistory)),
        POI_PROFILE(
                R.drawable.image_profile_icon_server,
                GlobalInstance.getStringResource(R.string.profileIconPoiProfile));

        public int resId;
        public String name;

        private Icon(int resId, String name) {
            this.resId = resId;
            this.name = name;
        }
    }


    private long id;

    public Profile(long id) {
        this.id = id;
    }

    public long getId() {
        return this.id;
    }

    public  abstract String getName();
    public abstract Icon getIcon();

    @Override public String toString() {
        return this.getName();
    }

	@Override public int hashCode() {
        return new Long(this.id).hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        } else if (obj == null) {
			return false;
        } else if (! (obj instanceof Profile)) {
			return false;
        }
		Profile other = (Profile) obj;
        return this.id == other.getId();
    }

    @Override public int compareTo(Profile other) {
        return this.getName().compareTo(other.getName());
    }

}
