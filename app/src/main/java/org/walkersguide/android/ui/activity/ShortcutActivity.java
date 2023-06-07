package org.walkersguide.android.ui.activity;

import org.walkersguide.android.shortcut.StaticShortcutAction;
import org.walkersguide.android.shortcut.PinnedShortcutUtility;
import org.walkersguide.android.ui.dialog.create.SaveCurrentLocationDialog;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.bearing.BearingSensorValue;
import org.walkersguide.android.sensor.bearing.BearingSensor;
import org.walkersguide.android.sensor.bearing.BearingSensorAccuracyRating;
import android.net.Uri;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;
import org.walkersguide.android.ui.dialog.toolbar.BearingDetailsDialog;
import org.walkersguide.android.ui.dialog.toolbar.LocationDetailsDialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.Manifest;

import android.os.Bundle;

import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;


import android.view.Menu;
import android.view.View;

import android.widget.ImageButton;
import android.widget.TextView;



import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import androidx.appcompat.app.AppCompatDelegate;
import org.walkersguide.android.data.object_with_id.Point;
import androidx.appcompat.widget.Toolbar;
import timber.log.Timber;
import android.app.Dialog;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import org.walkersguide.android.ui.dialog.edit.RenameObjectDialog;
import android.widget.Toast;
import android.view.MenuItem;
import androidx.core.app.NavUtils;
import androidx.core.app.TaskStackBuilder;
import org.walkersguide.android.ui.activity.toolbar.MainActivity;
import org.walkersguide.android.server.wg.poi.PoiProfile;
import android.annotation.TargetApi;


public class ShortcutActivity extends AppCompatActivity {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate action: %1$s", getIntent().getAction());

        if (PinnedShortcutUtility.PINNED_ACTION_OPEN_POI_PROFILE.equals(getIntent().getAction())) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                PoiProfile newPoiProfile = PoiProfile.load(
                        extras.getLong(PinnedShortcutUtility.EXTRA_POI_PROFILE_ID, -1l));
                if (newPoiProfile != null) {
                    MainActivity.loadPoiProfile(
                            ShortcutActivity.this, newPoiProfile);
                    finishAndRemoveTask();
                    return;
                }
            }
        }

        // enable static shortcut
        StaticShortcutAction staticShortcutAction = StaticShortcutAction.lookUpById(getIntent().getAction());
        if (staticShortcutAction != null) {
            GlobalInstance
                .getInstance()
                .enableStaticShortcutAction(staticShortcutAction);
        }

        Intent intent = new Intent(ShortcutActivity.this, MainActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAndRemoveTask();
    }

}
