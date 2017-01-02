package org.walkersguide.android.ui;

import org.walkersguide.android.utils.GlobalInstance;
import org.walkersguide.android.utils.SettingsManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

public abstract class AbstractActivity extends AppCompatActivity {

    public GlobalInstance globalInstance;
	public SettingsManager settingsManagerInstance;
    private IntentFilter filter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        globalInstance = (GlobalInstance) getApplicationContext();
		settingsManagerInstance = SettingsManager.getInstance(this);

        // create intent filter
        filter = new IntentFilter();
        //filter.addAction(Constants.COMMAND.CREATE_CATALOG);
    }

    @Override public void onPause() {
        super.onPause();
        globalInstance.startActivityTransitionTimer();
        // unregister broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        globalInstance.stopActivityTransitionTimer();
        // register broadcast receiver to listen to messages from dtPlayer
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
    }


    /**
     * broadcast receiver
     */

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            System.out.println("xxx onReceive " + intent.getAction());
            onPause();
            onResume();
        }
    };

}
