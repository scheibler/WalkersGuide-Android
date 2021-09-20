package org.walkersguide.android.ui.fragment;

import android.text.format.DateFormat;
import android.widget.TextView;
import java.util.Date;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.data.server.OSMMap;
import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.R;
import org.walkersguide.android.server.ServerStatusManager;
import org.walkersguide.android.server.ServerStatusManager.ServerStatusListener;
import org.walkersguide.android.util.SettingsManager.ServerSettings;
import org.walkersguide.android.util.Constants;
import android.content.Context;


import android.os.Bundle;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;


import androidx.fragment.app.Fragment;
import org.walkersguide.android.util.SettingsManager;


public class InfoFragment extends Fragment implements ServerStatusListener {

	public static InfoFragment newInstance() {
		InfoFragment fragment = new InfoFragment();
		return fragment;
	}

    private TextView labelServerName, labelServerVersion, labelSelectedMapName, labelSelectedMapCreated;

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_info, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        TextView labelProgramVersion = (TextView) view.findViewById(R.id.labelProgramVersion);
        labelProgramVersion.setText(
                String.format(
                    "%1$s: %2$s",
                    GlobalInstance.getStringResource(R.string.labelInfoProgramVersion),
                    BuildConfig.VERSION_NAME)
                );

        TextView labelInfoEMail = (TextView) view.findViewById(R.id.labelInfoEMail);
        labelInfoEMail.setText(
                String.format(
                    "%1$s: %2$s",
                    GlobalInstance.getStringResource(R.string.labelInfoEMail),
                    BuildConfig.CONTACT_EMAIL)
                );

        TextView labelInfoURL = (TextView) view.findViewById(R.id.labelInfoURL);
        labelInfoURL.setText(
                String.format(
                    "%1$s: %2$s",
                    GlobalInstance.getStringResource(R.string.labelInfoURL),
                    BuildConfig.CONTACT_WEBSITE)
                );

        labelServerName = (TextView) view.findViewById(R.id.labelServerName);
        labelServerVersion = (TextView) view.findViewById(R.id.labelServerVersion);
        labelSelectedMapName = (TextView) view.findViewById(R.id.labelSelectedMapName);
        labelSelectedMapCreated= (TextView) view.findViewById(R.id.labelSelectedMapCreated);
    }

    @Override public void onPause() {
        super.onPause();
        ServerStatusManager.getInstance(getActivity()).invalidateServerStatusRequest(this);
    }

    @Override public void onResume() {
        super.onResume();
        ServerSettings serverSettings = SettingsManager.getInstance().getServerSettings();

        labelServerName.setText(
                GlobalInstance.getStringResource(R.string.labelServerName));
        labelServerVersion.setText(
                GlobalInstance.getStringResource(R.string.labelServerVersion));
        labelSelectedMapName.setText(
                GlobalInstance.getStringResource(R.string.labelSelectedMapName));
        labelSelectedMapCreated.setText(
                GlobalInstance.getStringResource(R.string.labelSelectedMapCreated));

        ServerStatusManager.getInstance(getActivity()).requestServerStatus(
                InfoFragment.this, serverSettings.getServerURL());
    }

	@Override public void serverStatusRequestFinished(Context context, int returnCode, ServerInstance serverInstance) {
        if (returnCode == Constants.RC.OK
                && serverInstance != null) {
            // server name and version
	    	labelServerName.setText(
                    String.format(
                        "%1$s: %2$s",
    			    	GlobalInstance.getStringResource(R.string.labelServerName),
                        serverInstance.getServerName())
                    );
	    	labelServerVersion.setText(
                    String.format(
                        "%1$s: %2$s",
    			    	GlobalInstance.getStringResource(R.string.labelServerVersion),
                        serverInstance.getServerVersion())
                    );

            // selected map data
            OSMMap selectedMap = SettingsManager.getInstance().getServerSettings().getSelectedMap();
            if (selectedMap != null) {
                // map name
                labelSelectedMapName.setText(
                        String.format(
                            "%1$s: %2$s",
                            GlobalInstance.getStringResource(R.string.labelSelectedMapName),
                            selectedMap.getName())
                        );
                // map creation date
                String formattedDate = DateFormat.getMediumDateFormat(GlobalInstance.getContext()).format(
                        new Date(selectedMap.getCreated()));
                labelSelectedMapCreated.setText(
                        String.format(
                            "%1$s: %2$s",
                            GlobalInstance.getStringResource(R.string.labelSelectedMapCreated),
                            formattedDate)
                        );
            }
        }
    }

}
