package org.walkersguide.android.ui.fragment.menu;

import org.walkersguide.android.ui.fragment.RootFragment;
import org.walkersguide.android.ui.dialog.ChangelogDialog;
import androidx.fragment.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TextView;
import java.util.Date;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.server.wg.status.OSMMap;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.R;
import android.content.Context;


import android.os.Bundle;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;


import androidx.fragment.app.Fragment;
import org.walkersguide.android.util.SettingsManager;


import org.walkersguide.android.ui.dialog.SimpleMessageDialog;

import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.status.ServerStatusTask;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.server.wg.WgException;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.widget.Button;
import android.content.DialogInterface;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import org.walkersguide.android.ui.UiHelper;
import android.text.method.LinkMovementMethod;
import android.net.Uri;


public class InfoFragment extends RootFragment {
    private static final String KEY_TASK_ID = "taskId";

    public static InfoFragment newInstance() {
        InfoFragment fragment = new InfoFragment();
        return fragment;
    }


    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;

    private TextView labelServerName, labelServerVersion, labelSelectedMapName, labelSelectedMapCreated;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
        taskId = savedInstanceState != null
            ? savedInstanceState.getLong(KEY_TASK_ID)
            : ServerTaskExecutor.NO_TASK_ID;
    }


    /**
     * create view
     */

    @Override public String getTitle() {
        return getResources().getString(R.string.fragmentInfoName);
    }

    @Override public int getLayoutResourceId() {
        return R.layout.fragment_info;
    }

    @Override public View configureView(View view, Bundle savedInstanceState) {
        TextView labelProgramVersion = (TextView) view.findViewById(R.id.labelProgramVersion);
        labelProgramVersion.setText(
                String.format(
                    "%1$s: %2$s",
                    GlobalInstance.getStringResource(R.string.labelInfoProgramVersion),
                    BuildConfig.VERSION_NAME)
                );

        TextView labelInfoLastChangelog = (TextView) view.findViewById(R.id.labelInfoLastChangelog);
        labelInfoLastChangelog.setText(
                UiHelper.urlStyle(
                    getResources().getString(R.string.labelInfoLastChangelog))
                );
        ViewCompat.setAccessibilityDelegate(
                labelInfoLastChangelog, UiHelper.getAccessibilityDelegateViewClassButton());
        labelInfoLastChangelog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ChangelogDialog.newInstance()
                    .show(getChildFragmentManager(), "ChangelogDialog");
            }
        });

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

        TextView labelInfoUserManual = (TextView) view.findViewById(R.id.labelInfoUserManual);
        labelInfoUserManual.setMovementMethod(LinkMovementMethod.getInstance());
        labelInfoUserManual.setText(
                UiHelper.fromHtml(
                    String.format(
                        getResources().getString(R.string.labelInfoUserManual),
                        getResources().getString(R.string.variableUserManualUrl))
                    )
                );
        labelInfoUserManual.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(
                        Uri.parse(getResources().getString(R.string.variableUserManualUrl)));
                getActivity().startActivity(i);
            }
        });
        ViewCompat.setAccessibilityDelegate(
                labelInfoUserManual, UiHelper.getAccessibilityDelegateViewClassButton());

        TextView labelInfoPrivacyPolicy = (TextView) view.findViewById(R.id.labelInfoPrivacyPolicy);
        labelInfoPrivacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());
        labelInfoPrivacyPolicy.setText(
                UiHelper.fromHtml(
                    String.format(
                        getResources().getString(R.string.labelInfoPrivacyPolicy),
                        getResources().getString(R.string.variablePrivacyPolicyUrl))
                    )
                );
        labelInfoPrivacyPolicy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(
                        Uri.parse(getResources().getString(R.string.variablePrivacyPolicyUrl)));
                getActivity().startActivity(i);
            }
        });
        ViewCompat.setAccessibilityDelegate(
                labelInfoPrivacyPolicy, UiHelper.getAccessibilityDelegateViewClassButton());

        TextView labelInfoDonate = (TextView) view.findViewById(R.id.labelInfoDonate);
        labelInfoDonate.setMovementMethod(LinkMovementMethod.getInstance());
        labelInfoDonate.setText(
                UiHelper.fromHtml(
                    String.format(
                        getResources().getString(R.string.labelInfoDonate),
                        getResources().getString(R.string.variableDonateUrl))
                    )
                );
        labelInfoDonate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(
                        Uri.parse(getResources().getString(R.string.variableDonateUrl)));
                getActivity().startActivity(i);
            }
        });
        ViewCompat.setAccessibilityDelegate(
                labelInfoDonate, UiHelper.getAccessibilityDelegateViewClassButton());

        labelServerName = (TextView) view.findViewById(R.id.labelServerName);
        labelServerVersion = (TextView) view.findViewById(R.id.labelServerVersion);
        labelSelectedMapName = (TextView) view.findViewById(R.id.labelSelectedMapName);
        labelSelectedMapCreated= (TextView) view.findViewById(R.id.labelSelectedMapCreated);

        TextView labelPublicTransportMessage = (TextView) view.findViewById(R.id.labelPublicTransportMessage);
        labelPublicTransportMessage.setMovementMethod(LinkMovementMethod.getInstance());
        labelPublicTransportMessage.setText(
                UiHelper.getPublicTransportDataSourceText());

        return view;
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onResume() {
        super.onResume();

        labelServerName.setVisibility(View.GONE);
        labelServerVersion.setVisibility(View.GONE);
        labelSelectedMapName.setVisibility(View.GONE);
        labelSelectedMapCreated.setVisibility(View.GONE);

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_STATUS_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);

        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            taskId = serverTaskExecutorInstance.executeTask(new ServerStatusTask());
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            serverTaskExecutorInstance.cancelTask(taskId);
        }
    }


    // background task results

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_STATUS_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_STATUS_TASK_SUCCESSFUL)) {
                    ServerInstance serverInstance = (ServerInstance) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_SERVER_INSTANCE);
                    if (serverInstance != null) {

                        // server name and version
                        labelServerName.setText(
                                String.format(
                                    "%1$s: %2$s",
                                    GlobalInstance.getStringResource(R.string.labelServerName),
                                    serverInstance.getServerName())
                                );
                        labelServerName.setVisibility(View.VISIBLE);
                        labelServerVersion.setText(
                                String.format(
                                    "%1$s: %2$s",
                                    GlobalInstance.getStringResource(R.string.labelServerVersion),
                                    serverInstance.getServerVersion())
                                );
                        labelServerVersion.setVisibility(View.VISIBLE);

                        // selected map data
                        OSMMap selectedMap = SettingsManager.getInstance().getSelectedMap();
                        // map name
                        labelSelectedMapName.setText(
                                String.format(
                                    "%1$s: %2$s",
                                    GlobalInstance.getStringResource(R.string.labelSelectedMapName),
                                    selectedMap != null ? selectedMap.getName() : "")
                                );
                        labelSelectedMapName.setVisibility(View.VISIBLE);
                        // map creation date
                        String formattedDate = "";
                        if (selectedMap != null) {
                            formattedDate = DateFormat.getMediumDateFormat(GlobalInstance.getContext()).format(
                                    new Date(selectedMap.getCreated()));
                        }
                        labelSelectedMapCreated.setText(
                                String.format(
                                    "%1$s: %2$s",
                                    GlobalInstance.getStringResource(R.string.labelSelectedMapCreated),
                                    formattedDate)
                                );
                        labelSelectedMapCreated.setVisibility(View.VISIBLE);
                    }

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    WgException wgException = (WgException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (wgException != null) {
                        Toast.makeText(
                                context,
                                wgException.getMessage(),
                                Toast.LENGTH_LONG)
                            .show();
                    }
                }
            }
        }
    };

}
