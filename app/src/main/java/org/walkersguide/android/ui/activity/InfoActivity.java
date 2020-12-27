package org.walkersguide.android.ui.activity;

import android.content.Context;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import android.text.format.DateFormat;

import android.view.Menu;

import android.widget.TextView;

import java.util.Date;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.data.server.OSMMap;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.R;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.server.ServerStatusManager.ServerStatusListener;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager.ServerSettings;


public class InfoActivity extends AbstractActivity implements ServerStatusListener {

    private TextView labelServerName, labelServerVersion, labelSelectedMapName, labelSelectedMapCreated;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_info);

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

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menuItemDirection).setVisible(false);
        menu.findItem(R.id.menuItemLocation).setVisible(false);
        return true;
    }

    @Override public void onPause() {
        super.onPause();
        ServerStatusManager.getInstance(this).invalidateServerStatusRequest(this);
    }

    @Override public void onResume() {
        super.onResume();
        ServerSettings serverSettings = settingsManagerInstance.getServerSettings();

        labelServerName.setText(
                getResources().getString(R.string.labelServerName));
        labelServerVersion.setText(
                getResources().getString(R.string.labelServerVersion));
        labelSelectedMapName.setText(
                getResources().getString(R.string.labelSelectedMapName));
        labelSelectedMapCreated.setText(
                getResources().getString(R.string.labelSelectedMapCreated));

        ServerStatusManager.getInstance(this).requestServerStatus(
                (InfoActivity) this, serverSettings.getServerURL());
    }

	@Override public void serverStatusRequestFinished(Context context, int returnCode, ServerInstance serverInstance) {
        if (returnCode == Constants.RC.OK
                && serverInstance != null) {
            // server name and version
	    	labelServerName.setText(
                    String.format(
                        "%1$s: %2$s",
    			    	context.getResources().getString(R.string.labelServerName),
                        serverInstance.getServerName())
                    );
	    	labelServerVersion.setText(
                    String.format(
                        "%1$s: %2$s",
    			    	context.getResources().getString(R.string.labelServerVersion),
                        serverInstance.getServerVersion())
                    );

            // selected map data
            OSMMap selectedMap = settingsManagerInstance.getServerSettings().getSelectedMap();
            if (selectedMap != null) {
                // map name
                labelSelectedMapName.setText(
                        String.format(
                            "%1$s: %2$s",
                            context.getResources().getString(R.string.labelSelectedMapName),
                            selectedMap.getName())
                        );
                // map creation date
                String formattedDate = DateFormat.getMediumDateFormat(context).format(
                        new Date(selectedMap.getCreated()));
                labelSelectedMapCreated.setText(
                        String.format(
                            "%1$s: %2$s",
                            context.getResources().getString(R.string.labelSelectedMapCreated),
                            formattedDate)
                        );
            }
        }
    }

}
