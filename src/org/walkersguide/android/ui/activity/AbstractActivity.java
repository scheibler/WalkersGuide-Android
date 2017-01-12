package org.walkersguide.android.ui.activity;

import org.walkersguide.android.sensor.DirectionManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;

public abstract class AbstractActivity extends AppCompatActivity {

    public GlobalInstance globalInstance;
	public SettingsManager settingsManagerInstance;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        globalInstance = (GlobalInstance) getApplicationContext();
		settingsManagerInstance = SettingsManager.getInstance(this);
    }

    @Override public void onPause() {
        super.onPause();
        globalInstance.startActivityTransitionTimer();
    }

    @Override public void onResume() {
        super.onResume();
        globalInstance.stopActivityTransitionTimer();

        // check if gps is enabled
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (! lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // launch system settings activity
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        } else if (globalInstance.applicationWasInBackground()) {
            // activate sensors
            PositionManager.getInstance(this).startGPS();
            DirectionManager.getInstance(this).startSensors();
            globalInstance.setApplicationInBackground(false);
        }
    }

}
