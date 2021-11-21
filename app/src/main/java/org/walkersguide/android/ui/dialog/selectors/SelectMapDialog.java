package org.walkersguide.android.ui.dialog.selectors;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import org.walkersguide.android.server.util.OSMMap;
import org.walkersguide.android.server.util.ServerInstance;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.ui.dialog.SendFeedbackDialog;
import org.walkersguide.android.util.GlobalInstance;

import java.util.concurrent.ExecutorService;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.walkersguide.android.server.util.ServerCommunicationException;
import org.walkersguide.android.server.util.ServerUtility;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;


public class SelectMapDialog extends DialogFragment {
    public static final String REQUEST_SELECT_MAP = "selectMap";
    public static final String EXTRA_MAP = "map";


    // instance constructors

    public static SelectMapDialog newInstance(OSMMap selectedMap) {
        SelectMapDialog dialog = new SelectMapDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_MAP, selectedMap);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_SELECTED_MAP = "selectedMap";

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Future getServerInstanceRequest;

    private OSMMap selectedMap;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        selectedMap = (OSMMap) getArguments().getSerializable(KEY_SELECTED_MAP);

        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectMapDialogTitle))
            .setItems(
                    new String[]{getResources().getString(R.string.messagePleaseWait)},
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .setNeutralButton(
                    getResources().getString(R.string.dialogSomethingMissing),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SendFeedbackDialog.newInstance(
                                    SendFeedbackDialog.Token.MAP_REQUEST)
                                .show(getParentFragmentManager(), "SendFeedbackDialog");
                            dismiss();
                        }
                    }
                    )
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    }
                    )
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        if (getServerInstanceRequest == null || getServerInstanceRequest.isDone()) {
            getServerInstanceRequest = this.executorService.submit(() -> {
                try {
                    final ServerInstance serverInstance = ServerUtility.getServerInstance(
                                SettingsManager.getInstance().getServerURL());
                    handler.post(() -> {
                        fillListView(serverInstance);
                    });

                } catch (ServerCommunicationException e) {
                    final ServerCommunicationException scException = e;
                    handler.post(() -> {
                        SimpleMessageDialog.newInstance(
                                ServerUtility.getErrorMessageForReturnCode(scException.getReturnCode()))
                            .show(getChildFragmentManager(), "SimpleMessageDialog");
                    });
                }
            });
        }
    }

    @Override public void onStop() {
        super.onStop();
    }

    private void fillListView(ServerInstance serverInstance) {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {

            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    OSMMap newMap = (OSMMap) parent.getItemAtPosition(position);
                    if (newMap != null) {
                        Bundle result = new Bundle();
                        result.putSerializable(EXTRA_MAP, newMap);
                        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_MAP, result);
                        dismiss();
                    }
                }
            });

            // fill listview
            ArrayList<OSMMap> osmMapList = new ArrayList<OSMMap>();
            for (OSMMap map : serverInstance.getAvailableMapList()) {
                osmMapList.add(map);
            }
            listViewItems.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listViewItems.setAdapter(
                    new ArrayAdapter<OSMMap>(
                        SelectMapDialog.this.getContext(), android.R.layout.select_dialog_singlechoice, osmMapList));
            // select list item
            if (selectedMap != null) {
                listViewItems.setItemChecked(
                        osmMapList.indexOf(selectedMap), true);
            }
        }
    }

}
