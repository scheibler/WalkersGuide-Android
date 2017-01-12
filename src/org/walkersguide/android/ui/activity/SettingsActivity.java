package org.walkersguide.android.ui.activity;

import org.walkersguide.android.R;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

public class SettingsActivity extends AbstractActivity {

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(
                getResources().getString(R.string.settingsActivityTitle));
    }

}
