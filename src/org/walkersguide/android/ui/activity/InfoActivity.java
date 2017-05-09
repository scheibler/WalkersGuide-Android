package org.walkersguide.android.ui.activity;

import java.util.Date;

import org.walkersguide.android.R;
import org.walkersguide.android.util.SettingsManager.GeneralSettings;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.Menu;
import android.widget.TextView;

public class InfoActivity extends AbstractActivity {

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_info);
        GeneralSettings generalSettings = settingsManagerInstance.getGeneralSettings();
        ServerSettings serverSettings = settingsManagerInstance.getServerSettings();

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(
                getResources().getString(R.string.infoActivityTitle));

        TextView labelProgramVersion = (TextView) findViewById(R.id.labelProgramVersion);
        labelProgramVersion.setText(
                String.format(
                    "%1$s: %2$s",
                    getResources().getString(R.string.labelInfoProgramVersion),
	    			generalSettings.getClientVersion())
                );

        if (serverSettings.getSelectedMap() != null) {
    		TextView labelSelectedMapName = (TextView) findViewById(R.id.labelSelectedMapName);
	    	labelSelectedMapName.setText(
                    String.format(
                        "%1$s: %2$s",
    			    	getResources().getString(R.string.labelSelectedMapName),
	    			    serverSettings.getSelectedMap().getName())
                    );
            if (serverSettings.getSelectedMap().getVersion() > 0) {
        		TextView labelSelectedMapVersion = (TextView) findViewById(R.id.labelSelectedMapVersion);
	        	labelSelectedMapVersion.setText(
                        String.format(
                            "%1$s: %2$d",
    			        	getResources().getString(R.string.labelSelectedMapVersion),
    	    			    serverSettings.getSelectedMap().getVersion())
                        );
            }
            if (serverSettings.getSelectedMap().getCreated() > 0l) {
                String formattedDate = DateFormat.getDateFormat(this).format(
                        new Date(serverSettings.getSelectedMap().getCreated()));
    	    	TextView labelSelectedMapCreated = (TextView) findViewById(R.id.labelSelectedMapCreated);
	    	    labelSelectedMapCreated.setText(
                        String.format(
                            "%1$s: %2$s",
    			    	    getResources().getString(R.string.labelSelectedMapCreated),
    	    			    formattedDate)
                        );
            }
        }
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menuItemDirection).setVisible(false);
        menu.findItem(R.id.menuItemLocation).setVisible(false);
        menu.findItem(R.id.menuItemSaveCurrentPosition).setVisible(false);
        menu.findItem(R.id.menuItemSettings).setVisible(false);
        menu.findItem(R.id.menuItemInfo).setVisible(false);
        return true;
    }

}
