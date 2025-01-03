package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.MainActivityController;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.wg.p2p.P2pRouteTask;
import org.walkersguide.android.server.wg.p2p.P2pRouteRequest;
import org.walkersguide.android.server.wg.p2p.WayClassWeightSettings;

import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
    import org.walkersguide.android.ui.view.ObjectWithIdView;
import org.walkersguide.android.ui.dialog.edit.ConfigureWayClassWeightsDialog;
import org.walkersguide.android.ui.dialog.select.SelectObjectWithIdFromMultipleSourcesDialog;
import org.walkersguide.android.ui.dialog.select.SelectObjectWithIdFromMultipleSourcesDialog.Target;
import androidx.appcompat.app.AlertDialog;
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
import org.walkersguide.android.ui.activity.MainActivity;
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
import org.walkersguide.android.database.profile.StaticProfile;
import org.walkersguide.android.util.Helper;
import org.walkersguide.android.data.ObjectWithId;


public class PlanRouteDialog extends DialogFragment implements FragmentResultListener {
    private static final String KEY_TASK_ID = "taskId";

    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;

    private SettingsManager settingsManagerInstance;

    // query in progress vibration
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;

    // ui components
    private ObjectWithIdView layoutStartPoint, layoutDestinationPoint;
    private LinearLayout layoutViaPointList;
    private SwitchCompat switchShowViaPointList;
    private ObjectWithIdView layoutViaPoint1, layoutViaPoint2, layoutViaPoint3;

    public static PlanRouteDialog newInstance() {
        return PlanRouteDialog.newInstance(false);
    }

    public static PlanRouteDialog newInstance(boolean startRouteCalculationImmediately) {
        PlanRouteDialog dialog = new PlanRouteDialog();
        Bundle args = new Bundle();
        args.putBoolean(KEY_START_ROUTE_CALCULATION_IMMEDIATELY, startRouteCalculationImmediately);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_START_ROUTE_CALCULATION_IMMEDIATELY = "startRouteCalculationImmediately";
    private static final String KEY_TRIED_TO_START_ROUTE_CALCULATION_IMMEDIATELY = "triedToStartRouteCalculationImmediately";

    private boolean triedToStartRouteCalculationImmediately;

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
                    SelectMapDialog.REQUEST_SELECT_MAP, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectObjectWithIdFromMultipleSourcesDialog.REQUEST_SELECT_OBJECT_WITH_ID, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        Timber.d("onFragmentResult: %1$s", requestKey);
        if (requestKey.equals(SelectMapDialog.REQUEST_SELECT_MAP)) {
            settingsManagerInstance.setSelectedMap(
                    (OSMMap) bundle.getSerializable(SelectMapDialog.EXTRA_MAP));
            startRouteCalculation();

        } else if (requestKey.equals(SelectObjectWithIdFromMultipleSourcesDialog.REQUEST_SELECT_OBJECT_WITH_ID)) {
            SelectObjectWithIdFromMultipleSourcesDialog.Target objectWithIdTarget = (SelectObjectWithIdFromMultipleSourcesDialog.Target)
                bundle.getSerializable(SelectObjectWithIdFromMultipleSourcesDialog.EXTRA_TARGET);
            ObjectWithId selectedObjectWithId = (ObjectWithId) bundle.getSerializable(SelectObjectWithIdFromMultipleSourcesDialog.EXTRA_OBJECT_WITH_ID);
            if (selectedObjectWithId instanceof Point) {
                P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
                Point point = (Point) selectedObjectWithId;
                switch (objectWithIdTarget) {
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
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
            triedToStartRouteCalculationImmediately = savedInstanceState.getBoolean(KEY_TRIED_TO_START_ROUTE_CALCULATION_IMMEDIATELY);
        } else {
            taskId = ServerTaskExecutor.NO_TASK_ID;
            triedToStartRouteCalculationImmediately = false;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_plan_route, nullParent);

        layoutStartPoint = (ObjectWithIdView) view.findViewById(R.id.layoutStartPoint);
        layoutStartPoint.setOnDefaultObjectActionListener(new ObjectWithIdView.OnDefaultObjectActionListener() {
            @Override public void onDefaultObjectActionClicked(ObjectWithIdView view, View subView, ObjectWithId objectWithId) {
                SelectObjectWithIdFromMultipleSourcesDialog.newInstance(
                        SelectObjectWithIdFromMultipleSourcesDialog.Target.ROUTE_START_POINT)
                    .show(getChildFragmentManager(), "SelectObjectWithIdFromMultipleSourcesDialog");
            }
        });

        layoutDestinationPoint = (ObjectWithIdView) view.findViewById(R.id.layoutDestinationPoint);
        layoutDestinationPoint.setOnDefaultObjectActionListener(new ObjectWithIdView.OnDefaultObjectActionListener() {
            @Override public void onDefaultObjectActionClicked(ObjectWithIdView view, View subView, ObjectWithId objectWithId) {
                SelectObjectWithIdFromMultipleSourcesDialog.newInstance(
                        SelectObjectWithIdFromMultipleSourcesDialog.Target.ROUTE_DESTINATION_POINT)
                    .show(getChildFragmentManager(), "SelectObjectWithIdFromMultipleSourcesDialog");
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

        layoutViaPoint1 = (ObjectWithIdView) view.findViewById(R.id.layoutViaPoint1);
        layoutViaPoint1.setOnDefaultObjectActionListener(new ObjectWithIdView.OnDefaultObjectActionListener() {
            @Override public void onDefaultObjectActionClicked(ObjectWithIdView view, View subView, ObjectWithId objectWithId) {
                SelectObjectWithIdFromMultipleSourcesDialog.newInstance(
                        SelectObjectWithIdFromMultipleSourcesDialog.Target.ROUTE_VIA_POINT_1)
                    .show(getChildFragmentManager(), "SelectObjectWithIdFromMultipleSourcesDialog");
            }
        });
        layoutViaPoint1.setOnRemoveObjectActionListener(new ObjectWithIdView.OnRemoveObjectActionListener() {
            @Override public void onRemoveObjectActionClicked(ObjectWithId objectWithId) {
                P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
                p2pRouteRequest.setViaPoint1(null);
                settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
                updateUI();
            }
        });

        layoutViaPoint2 = (ObjectWithIdView) view.findViewById(R.id.layoutViaPoint2);
        layoutViaPoint2.setOnDefaultObjectActionListener(new ObjectWithIdView.OnDefaultObjectActionListener() {
            @Override public void onDefaultObjectActionClicked(ObjectWithIdView view, View subView, ObjectWithId objectWithId) {
                SelectObjectWithIdFromMultipleSourcesDialog.newInstance(
                        SelectObjectWithIdFromMultipleSourcesDialog.Target.ROUTE_VIA_POINT_2)
                    .show(getChildFragmentManager(), "SelectObjectWithIdFromMultipleSourcesDialog");
            }
        });
        layoutViaPoint2.setOnRemoveObjectActionListener(new ObjectWithIdView.OnRemoveObjectActionListener() {
            @Override public void onRemoveObjectActionClicked(ObjectWithId objectWithId) {
                P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
                p2pRouteRequest.setViaPoint2(null);
                settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
                updateUI();
            }
        });

        layoutViaPoint3 = (ObjectWithIdView) view.findViewById(R.id.layoutViaPoint3);
        layoutViaPoint3.setOnDefaultObjectActionListener(new ObjectWithIdView.OnDefaultObjectActionListener() {
            @Override public void onDefaultObjectActionClicked(ObjectWithIdView view, View subView, ObjectWithId objectWithId) {
                SelectObjectWithIdFromMultipleSourcesDialog.newInstance(
                        SelectObjectWithIdFromMultipleSourcesDialog.Target.ROUTE_VIA_POINT_3)
                    .show(getChildFragmentManager(), "SelectObjectWithIdFromMultipleSourcesDialog");
            }
        });
        layoutViaPoint3.setOnRemoveObjectActionListener(new ObjectWithIdView.OnRemoveObjectActionListener() {
            @Override public void onRemoveObjectActionClicked(ObjectWithId objectWithId) {
                P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
                p2pRouteRequest.setViaPoint3(null);
                settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
                updateUI();
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

            // start calculation immediately
            if (! triedToStartRouteCalculationImmediately
                    && getArguments().getBoolean(KEY_START_ROUTE_CALCULATION_IMMEDIATELY)) {
                triedToStartRouteCalculationImmediately = true;
                // cancel old running task and start calculation
                if (serverTaskExecutorInstance.taskInProgress(taskId)) {
                    serverTaskExecutorInstance.cancelTask(taskId, true);
                }
                startRouteCalculation();
            }

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
        savedInstanceState.putBoolean(KEY_TRIED_TO_START_ROUTE_CALCULATION_IMMEDIATELY, triedToStartRouteCalculationImmediately);
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

        layoutViaPoint1.configureAsSingleObject(p2pRouteRequest.getViaPoint1());
        layoutViaPoint2.configureAsSingleObject(p2pRouteRequest.getViaPoint2());
        layoutViaPoint3.configureAsSingleObject(p2pRouteRequest.getViaPoint3());
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
                    MainActivity.loadRoute(
                            PlanRouteDialog.this.getContext(),
                            (Route) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_ROUTE));
                    Helper.vibrateOnce(Helper.VIBRATION_DURATION_LONG);
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
                Menu.NONE, MENU_ITEM_CLEAR, 1, GlobalInstance.getStringResource(R.string.planRouteMenuItemClear));
        optionsMenu.getMenu().add(
                Menu.NONE, MENU_ITEM_SWAP, 2, GlobalInstance.getStringResource(R.string.planRouteMenuItemSwap));
        optionsMenu.getMenu().add(
                Menu.NONE, MENU_ITEM_EXCLUDED_WAYS, 3, GlobalInstance.getStringResource(R.string.planRouteMenuItemExcludedWays));
        optionsMenu.getMenu().add(
                Menu.NONE, MENU_ITEM_ROUTING_WAY_CLASSES, 4, GlobalInstance.getStringResource(R.string.planRouteMenuItemRoutingWayClasses));

        optionsMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {

                if (item.getItemId() == MENU_ITEM_CLEAR) {
                    settingsManagerInstance.setP2pRouteRequest(P2pRouteRequest.getDefault());
                    updateUI();

                } else if (item.getItemId() == MENU_ITEM_SWAP) {
                    P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
                    p2pRouteRequest.swapStartAndDestinationPoints();
                    settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
                    updateUI();

                } else if (item.getItemId() == MENU_ITEM_EXCLUDED_WAYS) {
                    ObjectListFromDatabaseFragment.newInstance(StaticProfile.excludedRoutingSegments())
                        .show(getChildFragmentManager(), "excludedRoutingSegments");

                } else if (item.getItemId() == MENU_ITEM_ROUTING_WAY_CLASSES) {
                    ConfigureWayClassWeightsDialog.newInstance()
                        .show(getChildFragmentManager(), "ConfigureWayClassWeightsDialog");

                } else {
                    return false;
                }
                return true;
            }
        });

        optionsMenu.show();
    }

}
