package org.walkersguide.android.shortcut;

import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.database.DatabaseProfile;
import java.util.ArrayList;

public enum StaticShortcutAction {

    OPEN_PLAN_ROUTE_DIALOG(
            "org.walkersguide.android.action.OPEN_PLAN_ROUTE_DIALOG"),
    OPEN_SAVE_CURRENT_LOCATION_DIALOG(
            "org.walkersguide.android.action.OPEN_SAVE_CURRENT_LOCATION_DIALOG"),
    OPEN_WHERE_AM_I_DIALOG(
            "org.walkersguide.android.action.OPEN_WHERE_AM_I_DIALOG");


    public static StaticShortcutAction lookUpById(String id) {
        if (id != null) {
            for (StaticShortcutAction action : StaticShortcutAction.values()) {
                if (action.id.equals(id)) {
                    return action;
                }
            }
        }
        return null;
    }


    public String id;

    private StaticShortcutAction(String id) {
        this.id = id;
    }

}
