package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.p2p.P2pRouteTask;
import org.walkersguide.android.server.wg.p2p.P2pRouteRequest;
import org.walkersguide.android.server.wg.p2p.WayClassWeightSettings;

import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;
import org.walkersguide.android.ui.dialog.edit.ConfigureWayClassWeightsDialog;
import org.walkersguide.android.ui.dialog.select.SelectRouteOrSimulationPointDialog;
import org.walkersguide.android.ui.dialog.select.SelectRouteOrSimulationPointDialog.WhereToPut;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.LinearLayout;



import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.activity.toolbar.tabs.MainActivity;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.GlobalInstance;
import android.widget.CompoundButton;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.ui.dialog.select.SelectMapDialog;
import org.walkersguide.android.server.wg.status.OSMMap;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.WgException;
import timber.log.Timber;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import android.view.Menu;
import android.view.MenuItem;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.util.Helper;


public class PlanRouteDialog extends DialogFragment implements FragmentResultListener {
    private static final String KEY_TASK_ID = "taskId";

    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;

    private SettingsManager settingsManagerInstance;

    // query in progress vibration
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;

    // ui components
    private TextViewAndActionButton layoutStartPoint, layoutDestinationPoint;
    private LinearLayout layoutViaPointList;
    private SwitchCompat switchShowViaPointList;
    private TextViewAndActionButton layoutViaPoint1, layoutViaPoint2, layoutViaPoint3;

    public static PlanRouteDialog newInstance() {
        PlanRouteDialog dialog = new PlanRouteDialog();
        return dialog;
    }

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
        settingsManagerInstance = SettingsManager.getInstance();
        // progress updater
        this.progressHandler = new Handler();
        this.progressUpdater = new ProgressUpdater();

        // fragment result listener
        getChildFragmentManager()
            .setFragmentResultListener(
                    ConfigureWayClassWeightsDialog.REQUEST_WAY_CLASS_WEIGHTS_CHANGED, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectMapDialog.REQUEST_SELECT_MAP, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectRouteOrSimulationPointDialog.REQUEST_SELECT_POINT, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        Timber.d("onFragmentResult: %1$s", requestKey);
        if (requestKey.equals(ConfigureWayClassWeightsDialog.REQUEST_WAY_CLASS_WEIGHTS_CHANGED)) {
            settingsManagerInstance.setWayClassWeightSettings(
                    (WayClassWeightSettings) bundle.getSerializable(ConfigureWayClassWeightsDialog.EXTRA_WAY_CLASS_SETTINGS));
        } else if (requestKey.equals(SelectMapDialog.REQUEST_SELECT_MAP)) {
            settingsManagerInstance.setSelectedMap(
                    (OSMMap) bundle.getSerializable(SelectMapDialog.EXTRA_MAP));
            startRouteCalculation();
        } else if (requestKey.equals(SelectRouteOrSimulationPointDialog.REQUEST_SELECT_POINT)) {
            P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
            WhereToPut whereToPut = (WhereToPut) bundle.getSerializable(SelectRouteOrSimulationPointDialog.EXTRA_WHERE_TO_PUT);
            Point point = (Point) bundle.getSerializable(SelectRouteOrSimulationPointDialog.EXTRA_POINT);
            switch (whereToPut) {
                case ROUTE_START_POINT:
                    p2pRouteRequest.setStartPoint(point);
                    break;
                case ROUTE_DESTINATION_POINT:
                    p2pRouteRequest.setDestinationPoint(point);
                    break;
                case ROUTE_VIA_POINT_1:
                    p2pRouteRequest.setViaPoint1(point);
                    break;
                case ROUTE_VIA_POINT_2:
                    p2pRouteRequest.setViaPoint2(point);
                    break;
                case ROUTE_VIA_POINT_3:
                    p2pRouteRequest.setViaPoint3(point);
                    break;
            }
            settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
            updateUI();
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_plan_route, nullParent);

        layoutStartPoint = (TextViewAndActionButton) view.findViewById(R.id.layoutStartPoint);
        layoutStartPoint.setAutoUpdate(true);
        layoutStartPoint.setOnObjectDefaultActionListener(new TextViewAndActionButton.OnObjectDefaultActionListener() {
            @Override public void onObjectDefaultAction(TextViewAndActionButton view) {
                SelectRouteOrSimulationPointDialog.newInstance(
                        SelectRouteOrSimulationPointDialog.WhereToPut.ROUTE_START_POINT)
                    .show(getChildFragmentManager(), "SelectRouteOrSimulationPointDialog");
            }
        });

        layoutDestinationPoint = (TextViewAndActionButton) view.findViewById(R.id.layoutDestinationPoint);
        layoutDestinationPoint.setAutoUpdate(true);
        layoutDestinationPoint.setOnObjectDefaultActionListener(new TextViewAndActionButton.OnObjectDefaultActionListener() {
            @Override public void onObjectDefaultAction(TextViewAndActionButton view) {
                SelectRouteOrSimulationPointDialog.newInstance(
                        SelectRouteOrSimulationPointDialog.WhereToPut.ROUTE_DESTINATION_POINT)
                    .show(getChildFragmentManager(), "SelectRouteOrSimulationPointDialog");
            }
        });

        // via points
		layoutViaPointList = (LinearLayout) view.findViewById(R.id.layoutViaPointList);

        switchShowViaPointList = (SwitchCompat) view.findViewById(R.id.switchShowViaPointList);
        switchShowViaPointList.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                layoutViaPointList.setVisibility(
                        isChecked ? View.VISIBLE : View.GONE);
            }
        });

        layoutViaPoint1 = (TextViewAndActionButton) view.findViewById(R.id.layoutViaPoint1);
        layoutViaPoint1.setOnObjectDefaultActionListener(new TextViewAndActionButton.OnObjectDefaultActionListener() {
            @Override public void onObjectDefaultAction(TextViewAndActionButton view) {
                SelectRouteOrSimulationPointDialog.newInstance(
                        SelectRouteOrSimulationPointDialog.WhereToPut.ROUTE_VIA_POINT_1)
                    .show(getChildFragmentManager(), "SelectRouteOrSimulationPointDialog");
            }
        });

        layoutViaPoint2 = (TextViewAndActionButton) view.findViewById(R.id.layoutViaPoint2);
        layoutViaPoint2.setOnObjectDefaultActionListener(new TextViewAndActionButton.OnObjectDefaultActionListener() {
            @Override public void onObjectDefaultAction(TextViewAndActionButton view) {
                SelectRouteOrSimulationPointDialog.newInstance(
                        SelectRouteOrSimulationPointDialog.WhereToPut.ROUTE_VIA_POINT_2)
                    .show(getChildFragmentManager(), "SelectRouteOrSimulationPointDialog");
            }
        });

        layoutViaPoint3 = (TextViewAndActionButton) view.findViewById(R.id.layoutViaPoint3);
        layoutViaPoint3.setOnObjectDefaultActionListener(new TextViewAndActionButton.OnObjectDefaultActionListener() {
            @Override public void onObjectDefaultAction(TextViewAndActionButton view) {
                SelectRouteOrSimulationPointDialog.newInstance(
                        SelectRouteOrSimulationPointDialog.WhereToPut.ROUTE_VIA_POINT_3)
                    .show(getChildFragmentManager(), "SelectRouteOrSimulationPointDialog");
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.planRouteDialogTitle))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogCalculate),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNeutralButton(
                    getResources().getString(R.string.dialogOptions),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogBack),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            updatePositiveButtonText(
                    serverTaskExecutorInstance.taskInProgress(taskId));
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (serverTaskExecutorInstance.taskInProgress(taskId)) {
                        serverTaskExecutorInstance.cancelTask(taskId, true);
                    } else {
                        startRouteCalculation();
                    }
                }
            });

            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
                        showOptionsMenu(view);
                    }
                }
            });

            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });

            IntentFilter filter = new IntentFilter();
            filter.addAction(ServerTaskExecutor.ACTION_P2P_ROUTE_TASK_SUCCESSFUL);
            filter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
            filter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
            LocalBroadcastManager.getInstance(GlobalInstance.getContext()).registerReceiver(mMessageReceiver, filter);

            if (serverTaskExecutorInstance.taskInProgress(taskId)) {
                progressHandler.postDelayed(progressUpdater, 2000);
            }
            updateUI();
        }
    }

    @Override public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        progressHandler.removeCallbacks(progressUpdater);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            serverTaskExecutorInstance.cancelTask(taskId, true);
        }
    }

    private void startRouteCalculation() {
        updatePositiveButtonText(true);
        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            taskId = serverTaskExecutorInstance.executeTask(
                    new P2pRouteTask(
                        settingsManagerInstance.getP2pRouteRequest(),
                        settingsManagerInstance.getWayClassWeightSettings()));
            progressHandler.postDelayed(progressUpdater, 2000);
        }
    }

    private void updateUI() {
        P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
        layoutStartPoint.configureAsSingleObject(p2pRouteRequest.getStartPoint());
        layoutDestinationPoint.configureAsSingleObject(p2pRouteRequest.getDestinationPoint());

        // via point layout
        switchShowViaPointList.setChecked(p2pRouteRequest.hasViaPoint());
        layoutViaPointList.setVisibility(
                p2pRouteRequest.hasViaPoint() ? View.VISIBLE : View.GONE);

        Point viaPoint1 = p2pRouteRequest.getViaPoint1();
        layoutViaPoint1.configureAsSingleObject(
                viaPoint1,
                viaPoint1 != null ? viaPoint1.getName() : null,
                new TextViewAndActionButton.OnLayoutResetListener() {
                    @Override public void onLayoutReset(TextViewAndActionButton view) {
                        P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
                        p2pRouteRequest.setViaPoint1(null);
                        settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
                        updateUI();
                    }
                });

        Point viaPoint2 = p2pRouteRequest.getViaPoint2();
        layoutViaPoint2.configureAsSingleObject(
                viaPoint2,
                viaPoint2 != null ? viaPoint2.getName() : null,
                new TextViewAndActionButton.OnLayoutResetListener() {
                    @Override public void onLayoutReset(TextViewAndActionButton view) {
                        P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
                        p2pRouteRequest.setViaPoint2(null);
                        settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
                        updateUI();
                    }
                });

        Point viaPoint3 = p2pRouteRequest.getViaPoint3();
        layoutViaPoint3.configureAsSingleObject(
                viaPoint3,
                viaPoint3 != null ? viaPoint3.getName() : null,
                new TextViewAndActionButton.OnLayoutResetListener() {
                    @Override public void onLayoutReset(TextViewAndActionButton view) {
                        P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
                        p2pRouteRequest.setViaPoint3(null);
                        settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
                        updateUI();
                    }
                });
    }

    private void updatePositiveButtonText(boolean requestInProgress) {
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setText(
                    requestInProgress
                    ? getResources().getString(R.string.dialogCancel)
                    : getResources().getString(R.string.dialogCalculate));
        }
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_P2P_ROUTE_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    Timber.e("wrong task");
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_P2P_ROUTE_TASK_SUCCESSFUL)) {
                    Route newRoute = (Route) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_ROUTE);
                    DatabaseProfile.plannedRoutes().add(newRoute);
                    Helper.vibrateOnce(Helper.VIBRATION_DURATION_LONG);
                    MainActivity.loadRoute(
                            PlanRouteDialog.this.getContext(), newRoute);
                    dismiss();

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    WgException wgException = (WgException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (wgException != null) {
                        if (wgException.showMapDialog()) {
                            SelectMapDialog.newInstance(
                                    settingsManagerInstance.getSelectedMap())
                                .show(getChildFragmentManager(), "SelectMapDialog");
                        } else {
                            SimpleMessageDialog.newInstance(wgException.getMessage())
                                .show(getChildFragmentManager(), "SimpleMessageDialog");
                        }
                    }
                }

                progressHandler.removeCallbacks(progressUpdater);
                updatePositiveButtonText(false);
            }
        }
    };


    private class ProgressUpdater implements Runnable {
        public void run() {
            Helper.vibrateOnce(
                    Helper.VIBRATION_DURATION_SHORT, Helper.VIBRATION_INTENSITY_WEAK);
            progressHandler.postDelayed(this, 2000);
        }
    }


    /**
     * options menu
     */
    private static final int MENU_ITEM_SWAP = 1;
    private static final int MENU_ITEM_EXCLUDED_WAYS = 2;
    private static final int MENU_ITEM_ROUTING_WAY_CLASSES = 3;
    private static final int MENU_ITEM_CLEAR = 4;

    private void showOptionsMenu(View view) {
        PopupMenu optionsMenu = new PopupMenu(getActivity(), view);
        optionsMenu.getMenu().add(
                Menu.NONE, MENU_ITEM_SWAP, 1, GlobalInstance.getStringResource(R.string.planRouteMenuItemSwap));
        optionsMenu.getMenu().add(
                Menu.NONE, MENU_ITEM_EXCLUDED_WAYS, 1, GlobalInstance.getStringResource(R.string.planRouteMenuItemExcludedWays));
        optionsMenu.getMenu().add(
                Menu.NONE, MENU_ITEM_ROUTING_WAY_CLASSES, 1, GlobalInstance.getStringResource(R.string.planRouteMenuItemRoutingWayClasses));
        optionsMenu.getMenu().add(
                Menu.NONE, MENU_ITEM_CLEAR, 2, GlobalInstance.getStringResource(R.string.planRouteMenuItemClear));

        optionsMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {

                if (item.getItemId() == MENU_ITEM_SWAP) {
                    P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
                    p2pRouteRequest.swapStartAndDestinationPoints();
                    settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
                    updateUI();

                } else if (item.getItemId() == MENU_ITEM_EXCLUDED_WAYS) {
                    ObjectListFromDatabaseFragment.createDialog(DatabaseProfile.excludedRoutingSegments(), false)
                        .show(getChildFragmentManager(), "ExcludedWaysDialog");

                } else if (item.getItemId() == MENU_ITEM_ROUTING_WAY_CLASSES) {
                    ConfigureWayClassWeightsDialog.newInstance(
                            settingsManagerInstance.getWayClassWeightSettings())
                        .show(getChildFragmentManager(), "ConfigureWayClassWeightsDialog");

                } else if (item.getItemId() == MENU_ITEM_CLEAR) {
                    settingsManagerInstance.setP2pRouteRequest(P2pRouteRequest.getDefault());
                    updateUI();

                } else {
                    return false;
                }
                return true;
            }
        });

        optionsMenu.show();
    }

}
