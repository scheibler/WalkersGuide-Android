package org.walkersguide.android.ui.dialog;

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


public class InfoDialog extends DialogFragment {
    private static final String KEY_TASK_ID = "taskId";

	public static InfoDialog newInstance() {
		InfoDialog dialog = new InfoDialog();
		return dialog;
	}


    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;

    private TextView labelServerName, labelServerVersion, labelSelectedMapName, labelSelectedMapCreated;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
        }

        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_info, nullParent);

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

        return new AlertDialog.Builder(getActivity())
            .setTitle(GlobalInstance.getStringResource(R.string.fragmentInfoName))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogClose),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .create();
    }

    @Override public void onStart() {
        super.onStart();

        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });
        }

        labelServerName.setText(
                GlobalInstance.getStringResource(R.string.labelServerName));
        labelServerVersion.setText(
                GlobalInstance.getStringResource(R.string.labelServerVersion));
        labelSelectedMapName.setText(
                GlobalInstance.getStringResource(R.string.labelSelectedMapName));
        labelSelectedMapCreated.setText(
                GlobalInstance.getStringResource(R.string.labelSelectedMapCreated));

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_STATUS_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);

        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            taskId = serverTaskExecutorInstance.executeTask(new ServerStatusTask());
        }
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
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
                        labelServerVersion.setText(
                                String.format(
                                    "%1$s: %2$s",
                                    GlobalInstance.getStringResource(R.string.labelServerVersion),
                                    serverInstance.getServerVersion())
                                );

                        // selected map data
                        OSMMap selectedMap = SettingsManager.getInstance().getSelectedMap();
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
