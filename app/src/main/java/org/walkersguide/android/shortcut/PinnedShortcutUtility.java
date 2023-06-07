package org.walkersguide.android.shortcut;

import android.annotation.TargetApi;
import android.os.Build;
import org.walkersguide.android.ui.activity.ShortcutActivity;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import android.content.pm.ShortcutInfo;
import java.util.ArrayList;
import android.content.pm.ShortcutManager;
import android.os.Bundle;
import org.walkersguide.android.ui.activity.toolbar.MainActivity;
import org.walkersguide.android.R;
import timber.log.Timber;
import android.content.Intent;
import java.util.Locale;
import org.walkersguide.android.util.GlobalInstance;
import android.graphics.drawable.Icon;


public class PinnedShortcutUtility {
    public static final String PINNED_ACTION_OPEN_POI_PROFILE =
        "org.walkersguide.android.action.OPEN_POI_PROFILE";
    public static final String EXTRA_POI_PROFILE_ID = "poiProfileId";
    private static final String SHORTCUT_POI_PROFILE_ID_PREFIX = "poi-profile-id";

    @TargetApi(Build.VERSION_CODES.O)
    public static boolean isPinShortcutsSupported() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
            && ((ShortcutManager) GlobalInstance.getContext().getSystemService(ShortcutManager.class)).isRequestPinShortcutSupported();
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void addPinnedShortcutForPoiProfile(long poiProfileId, String shortcutName) {
        if (isPinShortcutsSupported()) {
            Intent intent = new Intent(GlobalInstance.getContext(), ShortcutActivity.class);
            intent.setAction(PINNED_ACTION_OPEN_POI_PROFILE);
            intent.putExtra(EXTRA_POI_PROFILE_ID, poiProfileId);

            ShortcutInfo poiProfileShortcut = new ShortcutInfo.Builder(
                    GlobalInstance.getContext(),
                    String.format(
                        Locale.ROOT,
                        "%1$s.%2$d",
                        PINNED_ACTION_OPEN_POI_PROFILE,
                        poiProfileId))
                .setShortLabel(shortcutName)
                .setIcon(Icon.createWithResource(GlobalInstance.getContext(), R.drawable.ic_launcher))
                .setIntent(intent)
                .build();
            ((ShortcutManager) GlobalInstance.getContext().getSystemService(ShortcutManager.class))
                .requestPinShortcut(poiProfileShortcut, null);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void disableObsoletePinnedShortcuts() {
        if (isPinShortcutsSupported()) {
            ShortcutManager shortcutManager = (ShortcutManager) GlobalInstance.getContext().getSystemService(ShortcutManager.class);

            ArrayList<String> shortcutIdListToDisable = new ArrayList<String>();
            for (ShortcutInfo info : shortcutManager.getPinnedShortcuts()) {
                Bundle extras = info.getIntent().getExtras();
                Timber.d("pinned: %1$s", info.getId());

                // poi profile shortcuts
                if (info.getId().startsWith(PINNED_ACTION_OPEN_POI_PROFILE)) {
                    PoiProfile poiProfile = null;
                    if (extras != null) {
                        poiProfile = PoiProfile.load(
                                extras.getLong(EXTRA_POI_PROFILE_ID, -1l));
                    }
                    if (poiProfile == null) {
                        // corresponding poi profile not found
                        shortcutIdListToDisable.add(info.getId());
                    }
                }
            }

            if (! shortcutIdListToDisable.isEmpty()) {
                shortcutManager.disableShortcuts(
                        shortcutIdListToDisable, GlobalInstance.getStringResource(R.string.messageCorrespondingPoiProfileNotFound));
            }
        }
    }

}
