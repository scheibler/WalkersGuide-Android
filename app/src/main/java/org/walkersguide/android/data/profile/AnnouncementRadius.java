package org.walkersguide.android.data.profile;

import org.walkersguide.android.R;

import org.walkersguide.android.util.SettingsManager;
import com.google.common.primitives.Ints;
import java.io.Serializable;
import java.util.ArrayList;
import org.walkersguide.android.util.GlobalInstance;


public class AnnouncementRadius implements Serializable {
    private static final long serialVersionUID = 1l;
    private static final int[] values = new int[]{ 25, 50, 100, 250, 500, 1000 };

    public static ArrayList<AnnouncementRadius> values() {
        ArrayList<AnnouncementRadius> announcementRadiusList = new ArrayList<AnnouncementRadius>();
        for (int meter : values) {
            announcementRadiusList.add(AnnouncementRadius.create(meter));
        }
        return announcementRadiusList;
    }

    public static AnnouncementRadius create(int meter) {
        if (Ints.contains(values, meter)) {
            return new AnnouncementRadius(meter);
        }
        return defaultRadius();
    }

    public static AnnouncementRadius defaultRadius() {
        return new AnnouncementRadius(values[1]);
    }

    public int meter;

    private AnnouncementRadius(int meter) throws IllegalArgumentException {
        this.meter = meter;
    }

    @Override public String toString() {
        return GlobalInstance.getPluralResource(R.plurals.meter, this.meter);
    }

    @Override public int hashCode() {
        return this.meter;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (! (obj instanceof AnnouncementRadius)) {
            return false;
        }
        AnnouncementRadius other = (AnnouncementRadius) obj;
        return this.meter == other.meter;
    }

}
