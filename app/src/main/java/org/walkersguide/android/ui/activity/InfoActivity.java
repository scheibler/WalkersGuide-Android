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
import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.data.server.OSMMap;
import org.walkersguide.android.data.server.ServerInstance;


public class InfoActivity extends AbstractActivity {

    private TextView labelServerName, labelServerVersion, labelSelectedMapName, labelSelectedMapCreated;

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
                    BuildConfig.VERSION_NAME)
                );

        TextView labelInfoEMail = (TextView) findViewById(R.id.labelInfoEMail);
        labelInfoEMail.setText(
                String.format(
                    "%1$s: %2$s",
                    getResources().getString(R.string.labelInfoEMail),
                    BuildConfig.CONTACT_EMAIL)
                );

        TextView labelInfoURL = (TextView) findViewById(R.id.labelInfoURL);
        labelInfoURL.setText(
                String.format(
                    "%1$s: %2$s",
                    getResources().getString(R.string.labelInfoURL),
                    BuildConfig.CONTACT_WEBSITE)
                );

        labelServerName = (TextView) findViewById(R.id.labelServerName);
        labelServerVersion = (TextView) findViewById(R.id.labelServerVersion);
        labelSelectedMapName = (TextView) findViewById(R.id.labelSelectedMapName);
        labelSelectedMapCreated= (TextView) findViewById(R.id.labelSelectedMapCreated);
    }

    @Override public void onResume() {
        super.onResume();

        ServerInstance serverInstance = ServerStatusManager.getInstance(this).getServerInstance();
        if (serverInstance != null) {
	    	labelServerName.setText(
                    String.format(
                        "%1$s: %2$s",
    			    	getResources().getString(R.string.labelServerName),
                        serverInstance.getServerName())
                    );
	    	labelServerVersion.setText(
                    String.format(
                        "%1$s: %2$s",
    			    	getResources().getString(R.string.labelServerVersion),
                        serverInstance.getServerVersion())
                    );
        } else {
	    	labelServerName.setText(
    			    getResources().getString(R.string.labelServerName));
	    	labelServerVersion.setText(
    			    getResources().getString(R.string.labelServerVersion));
        }

        OSMMap selectedMap = settingsManagerInstance.getServerSettings().getSelectedMap();
        if (selectedMap != null) {
            // map name
	    	labelSelectedMapName.setText(
                    String.format(
                        "%1$s: %2$s",
    			    	getResources().getString(R.string.labelSelectedMapName),
	    			    selectedMap.getName())
                    );
            // map creation date
            String formattedDate = DateFormat.getDateFormat(this).format(
                    new Date(selectedMap.getCreated()));
	    	labelSelectedMapCreated.setText(
                    String.format(
                        "%1$s: %2$s",
    				    getResources().getString(R.string.labelSelectedMapCreated),
    	    		    formattedDate)
                    );
        } else {
	    	labelSelectedMapName.setText(
    			    getResources().getString(R.string.labelSelectedMapName));
	    	labelSelectedMapCreated.setText(
    			    getResources().getString(R.string.labelSelectedMapCreated));
        }
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menuItemDirection).setVisible(false);
        menu.findItem(R.id.menuItemLocation).setVisible(false);
        menu.findItem(R.id.menuItemPlanRoute).setVisible(false);
        menu.findItem(R.id.menuItemRequestAddress).setVisible(false);
        menu.findItem(R.id.menuItemSaveCurrentPosition).setVisible(false);
        menu.findItem(R.id.menuItemSearchInFavorites).setVisible(false);
        menu.findItem(R.id.menuItemSearchInPOI).setVisible(false);
        menu.findItem(R.id.menuItemSettings).setVisible(false);
        menu.findItem(R.id.menuItemInfo).setVisible(false);
        return true;
    }

}
