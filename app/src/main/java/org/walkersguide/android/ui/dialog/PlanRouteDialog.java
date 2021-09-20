package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.database.profiles.DatabaseSegmentProfile;
import org.walkersguide.android.ui.fragment.object_list.ObjectListFromDatabaseFragment;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.profiles.DatabasePointProfile;
    import org.walkersguide.android.ui.view.TextViewAndActionButton;
    import org.walkersguide.android.ui.view.TextViewAndActionButton.LabelTextConfig;
import org.walkersguide.android.ui.dialog.selectors.SelectRouteOrSimulationPointDialog;
import org.walkersguide.android.ui.dialog.selectors.SelectRouteOrSimulationPointDialog.SelectRouteOrSimulationPointListener;
import org.walkersguide.android.ui.dialog.selectors.SelectRouteOrSimulationPointDialog.WhereToPut;
import timber.log.Timber;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;

import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.server.route.WayClass;
import org.walkersguide.android.helper.PointUtility;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.route.RouteManager;
import org.walkersguide.android.server.route.RouteManager.RouteCalculationListener;
import org.walkersguide.android.ui.activity.toolbar.tabs.MainActivity;
import org.walkersguide.android.ui.adapter.WayClassAdapter;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog.ChildDialogCloseListener;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.PlanRouteSettings;
import org.walkersguide.android.util.GlobalInstance;
import android.view.MenuItem;
import org.walkersguide.android.data.sensor.attribute.NewLocationAttributes;
import org.walkersguide.android.data.sensor.threshold.DistanceThreshold;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.view.KeyEvent;
import org.walkersguide.android.data.route.Route;


public class PlanRouteDialog extends DialogFragment
        implements SelectRouteOrSimulationPointListener, RouteCalculationListener {
    private static final String KEY_SHOW_VIA_POINTS = "showViaPoints";

    private PositionManager positionManagerInstance;
    private RouteManager routeManagerInstance;
    private SettingsManager settingsManagerInstance;
    private boolean showViaPoints;

    // query in progress vibration
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private Vibrator vibrator;

    // ui components
    private TextViewAndActionButton layoutStartPoint, layoutDestinationPoint;
    private LinearLayout layoutViaPointList;
    private TextViewAndActionButton layoutViaPoint1, layoutViaPoint2, layoutViaPoint3;

    public static PlanRouteDialog newInstance() {
        PlanRouteDialog dialog = new PlanRouteDialog();
        return dialog;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
		positionManagerInstance = PositionManager.getInstance(context);
        routeManagerInstance = RouteManager.getInstance();
        settingsManagerInstance = SettingsManager.getInstance();
        // progress updater
        this.progressHandler = new Handler();
        this.progressUpdater = new ProgressUpdater();
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_plan_route, nullParent);

        layoutStartPoint = (TextViewAndActionButton) view.findViewById(R.id.layoutStartPoint);
        layoutStartPoint.setOnLabelClickListener(new TextViewAndActionButton.OnLabelClickListener() {
            @Override public void onLabelClick(TextViewAndActionButton view) {
                SelectRouteOrSimulationPointDialog dialog = SelectRouteOrSimulationPointDialog.newInstance(
                        SelectRouteOrSimulationPointDialog.WhereToPut.ROUTE_START_POINT);
                dialog.setTargetFragment(PlanRouteDialog.this, 1);
                dialog.show(getActivity().getSupportFragmentManager(), "SelectRouteOrSimulationPointDialog");
            }
        });

        layoutDestinationPoint = (TextViewAndActionButton) view.findViewById(R.id.layoutDestinationPoint);
        layoutDestinationPoint.setOnLabelClickListener(new TextViewAndActionButton.OnLabelClickListener() {
            @Override public void onLabelClick(TextViewAndActionButton view) {
                SelectRouteOrSimulationPointDialog dialog = SelectRouteOrSimulationPointDialog.newInstance(
                        SelectRouteOrSimulationPointDialog.WhereToPut.ROUTE_DESTINATION_POINT);
                dialog.setTargetFragment(PlanRouteDialog.this, 1);
                dialog.show(getActivity().getSupportFragmentManager(), "SelectRouteOrSimulationPointDialog");
            }
        });

        Button buttonNewRoute = (Button) view.findViewById(R.id.buttonNewRoute);
        buttonNewRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                PlanRouteSettings planRouteSettings = settingsManagerInstance.getPlanRouteSettings();
                planRouteSettings.setStartPoint(null);
                planRouteSettings.setDestinationPoint(null);
                planRouteSettings.clearViaPointList();
                updateUI();
            }
        });

        Button buttonSwapRoute = (Button) view.findViewById(R.id.buttonSwapRoute);
        buttonSwapRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                PlanRouteSettings planRouteSettings = settingsManagerInstance.getPlanRouteSettings();
                Point tempPoint = planRouteSettings.getStartPoint();
                planRouteSettings.setStartPoint(planRouteSettings.getDestinationPoint());
                planRouteSettings.setDestinationPoint(tempPoint);
                updateUI();
            }
        });

        Switch switchShowViaPointList = (Switch) view.findViewById(R.id.switchShowViaPointList);
        switchShowViaPointList.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                layoutViaPointList.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        switchShowViaPointList.setChecked(
                settingsManagerInstance.getPlanRouteSettings().hasViaPoint());

		layoutViaPointList = (LinearLayout) view.findViewById(R.id.layoutViaPointList);

        layoutViaPoint1 = (TextViewAndActionButton) view.findViewById(R.id.layoutViaPoint1);
        layoutViaPoint1.setOnLabelClickListener(new TextViewAndActionButton.OnLabelClickListener() {
            @Override public void onLabelClick(TextViewAndActionButton view) {
                SelectRouteOrSimulationPointDialog dialog = SelectRouteOrSimulationPointDialog.newInstance(
                        SelectRouteOrSimulationPointDialog.WhereToPut.ROUTE_VIA_POINT_1);
                dialog.setTargetFragment(PlanRouteDialog.this, 1);
                dialog.show(getActivity().getSupportFragmentManager(), "SelectRouteOrSimulationPointDialog");
            }
        });
        layoutViaPoint1.setOnMenuItemRemoveClickListener(new TextViewAndActionButton.OnMenuItemRemoveClickListener() {
            @Override public void onMenuItemRemoveClick(TextViewAndActionButton view) {
                settingsManagerInstance.getPlanRouteSettings().setViaPoint1(null);
            }
        });

        layoutViaPoint2 = (TextViewAndActionButton) view.findViewById(R.id.layoutViaPoint2);
        layoutViaPoint2.setOnLabelClickListener(new TextViewAndActionButton.OnLabelClickListener() {
            @Override public void onLabelClick(TextViewAndActionButton view) {
                SelectRouteOrSimulationPointDialog dialog = SelectRouteOrSimulationPointDialog.newInstance(
                        SelectRouteOrSimulationPointDialog.WhereToPut.ROUTE_VIA_POINT_2);
                dialog.setTargetFragment(PlanRouteDialog.this, 2);
                dialog.show(getActivity().getSupportFragmentManager(), "SelectRouteOrSimulationPointDialog");
            }
        });
        layoutViaPoint2.setOnMenuItemRemoveClickListener(new TextViewAndActionButton.OnMenuItemRemoveClickListener() {
            @Override public void onMenuItemRemoveClick(TextViewAndActionButton view) {
                settingsManagerInstance.getPlanRouteSettings().setViaPoint2(null);
            }
        });

        layoutViaPoint3 = (TextViewAndActionButton) view.findViewById(R.id.layoutViaPoint3);
        layoutViaPoint3.setOnLabelClickListener(new TextViewAndActionButton.OnLabelClickListener() {
            @Override public void onLabelClick(TextViewAndActionButton view) {
                SelectRouteOrSimulationPointDialog dialog = SelectRouteOrSimulationPointDialog.newInstance(
                        SelectRouteOrSimulationPointDialog.WhereToPut.ROUTE_VIA_POINT_3);
                dialog.setTargetFragment(PlanRouteDialog.this, 3);
                dialog.show(getActivity().getSupportFragmentManager(), "SelectRouteOrSimulationPointDialog");
            }
        });
        layoutViaPoint3.setOnMenuItemRemoveClickListener(new TextViewAndActionButton.OnMenuItemRemoveClickListener() {
            @Override public void onMenuItemRemoveClick(TextViewAndActionButton view) {
                settingsManagerInstance.getPlanRouteSettings().setViaPoint3(null);
            }
        });

        Button buttonExcludedWays = (Button) view.findViewById(R.id.buttonExcludedWays);
        buttonExcludedWays.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ObjectListFromDatabaseFragment.createDialog(
                        new DatabaseProfileRequest(DatabaseSegmentProfile.EXCLUDED_FROM_ROUTING), false)
                    .show(getActivity().getSupportFragmentManager(), "ExcludedWaysDialog");
            }
        });

        Button buttonRoutingWayClasses = (Button) view.findViewById(R.id.buttonRoutingWayClasses);
        buttonRoutingWayClasses.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectRoutingWayClassesDialog.newInstance().show(
                        getActivity().getSupportFragmentManager(), "SelectRoutingWayClassesDialog");
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.planRouteDialogTitle))
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.buttonGetRoute),
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
            .setOnKeyListener(
                    new Dialog.OnKeyListener() {
                        @Override public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                close();
                                return true;
                            }
                            return false;
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
            if (routeManagerInstance.routeCalculationInProgress()) {
                buttonPositive.setText(getResources().getString(R.string.dialogCancel));
            } else {
                buttonPositive.setText(getResources().getString(R.string.buttonGetRoute));
            }
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (routeManagerInstance.routeCalculationInProgress()) {
                        routeManagerInstance.cancelRouteCalculation();
                    } else {
                        routeManagerInstance.startRouteCalculation(PlanRouteDialog.this);
                        progressHandler.postDelayed(progressUpdater, 2000);
                        Button buttonPositive = (Button) view;
                        buttonPositive.setText(getResources().getString(R.string.dialogCancel));
                    }
                }
            });

            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    close();
                }
            });

            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_NEW_LOCATION);
            LocalBroadcastManager.getInstance(GlobalInstance.getContext()).registerReceiver(mMessageReceiver, filter);

            if (routeManagerInstance.routeCalculationInProgress()) {
                routeManagerInstance.updateRouteCalculationListener(PlanRouteDialog.this);
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

    @Override public void routeOrSimulationPointSelected(Point point, WhereToPut whereToPut) {
        PlanRouteSettings planRouteSettings = settingsManagerInstance.getPlanRouteSettings();
        switch (whereToPut) {
            case ROUTE_START_POINT:
                planRouteSettings.setStartPoint(point);
                break;
            case ROUTE_DESTINATION_POINT:
                planRouteSettings.setDestinationPoint(point);
                break;
            case ROUTE_VIA_POINT_1:
                planRouteSettings.setViaPoint1(point);
                break;
            case ROUTE_VIA_POINT_2:
                planRouteSettings.setViaPoint2(point);
                break;
            case ROUTE_VIA_POINT_3:
                planRouteSettings.setViaPoint3(point);
                break;
        }
        updateUI();
    }

    @Override public void routeCalculationSuccessful(Route route) {
        progressHandler.removeCallbacks(progressUpdater);
        settingsManagerInstance.setSelectedRoute(route);
        // show router fragment of main activity
        settingsManagerInstance.setSelectedTabForMainActivity(SettingsManager.DEFAULT_SELECTED_TAB_MAIN_ACTIVITY);
        Intent mainActivityIntent = new Intent(getActivity(), MainActivity.class);
        mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getActivity().startActivity(mainActivityIntent);
        vibrator.vibrate(250);
        dismiss();
    }

    @Override public void routeCalculationFailed(int returnCode) {
        progressHandler.removeCallbacks(progressUpdater);
        // change positive button text back
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setText(getResources().getString(R.string.buttonGetRoute));
        }
        // error dialog
        if (isAdded()) {
            if (returnCode == Constants.RC.MAP_LOADING_FAILED
                    || returnCode == Constants.RC.WRONG_MAP_SELECTED) {
                SelectMapDialog.newInstance()
                    .show(getActivity().getSupportFragmentManager(), "SelectMapDialog");
            } else {
                SimpleMessageDialog.newInstance(
                        ServerUtility.getErrorMessageForReturnCode(returnCode))
                    .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
            }
        }
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)) {
                NewLocationAttributes newLocationAttributes = NewLocationAttributes.fromString(
                        intent.getStringExtra(Constants.ACTION_NEW_LOCATION_ATTRIBUTES));
                if (newLocationAttributes != null
                        && newLocationAttributes.getAggregatingDistanceThreshold().isAtLeast(DistanceThreshold.TEN_METERS)) {
                    updateUI();
                }
            }
        }
    };

    private void updateUI() {
        PlanRouteSettings planRouteSettings = SettingsManager.getInstance().getPlanRouteSettings();
        layoutStartPoint.configureView(
                planRouteSettings.getStartPoint(), LabelTextConfig.start(false));
        layoutDestinationPoint.configureView(
                planRouteSettings.getDestinationPoint(), LabelTextConfig.destination(false));
        layoutViaPoint1.configureView(
                planRouteSettings.getViaPoint1(), LabelTextConfig.via(1, false), true, true);
        layoutViaPoint2.configureView(
                planRouteSettings.getViaPoint2(), LabelTextConfig.via(2, false), true, true);
        layoutViaPoint3.configureView(
                planRouteSettings.getViaPoint3(), LabelTextConfig.via(3, false), true, true);
    }

    private void close() {
        progressHandler.removeCallbacks(progressUpdater);
        if (routeManagerInstance.routeCalculationInProgress()) {
            routeManagerInstance.cancelRouteCalculation();
        }
        dismiss();
    }


    private class ProgressUpdater implements Runnable {
        public void run() {
            vibrator.vibrate(50);
            progressHandler.postDelayed(this, 2000);
        }
    }


    public static class SelectRoutingWayClassesDialog extends DialogFragment {

        private SettingsManager settingsManagerInstance;

        private ListView listViewWayClasses;
        private TextView labelListViewEmpty;

        public static SelectRoutingWayClassesDialog newInstance() {
            SelectRoutingWayClassesDialog selectRoutingWayClassesDialog = new SelectRoutingWayClassesDialog();
            return selectRoutingWayClassesDialog;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            settingsManagerInstance = SettingsManager.getInstance();
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            ArrayList<WayClass> wayClassList = new ArrayList<WayClass>();
            if (savedInstanceState != null) {
                JSONArray jsonWayClassList = null;
                try {
                    jsonWayClassList = new JSONArray(
                            savedInstanceState.getString("jsonWayClassList"));
                } catch (JSONException e) {
                    jsonWayClassList = null;
                } finally {
                    if (jsonWayClassList != null) {
                        for (int i=0; i<jsonWayClassList.length(); i++) {
                            try {
                                wayClassList.add(
                                        new WayClass(
                                            jsonWayClassList.getJSONObject(i)));
                            } catch (JSONException e) {}
                        }
                    }
                }
            } else {
                wayClassList = settingsManagerInstance.getPlanRouteSettings().getWayClassList();
            }

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_single_list_view, nullParent);

            listViewWayClasses = (ListView) view.findViewById(R.id.listView);
            listViewWayClasses.setAdapter(
                    new WayClassAdapter(getActivity(), wayClassList));

            labelListViewEmpty    = (TextView) view.findViewById(R.id.labelListViewEmpty);
            labelListViewEmpty.setVisibility(View.GONE);
            listViewWayClasses.setEmptyView(labelListViewEmpty);

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.selectRoutingWayClassesDialogTitle))
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogOK),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                        )
                .setNeutralButton(
                        getResources().getString(R.string.dialogDefault),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogCancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .create();
        }

        @Override public void onStart() {
            super.onStart();
            final AlertDialog dialog = (AlertDialog) getDialog();
            if (dialog != null) {
                // positive button
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        WayClassAdapter adapter = (WayClassAdapter) listViewWayClasses.getAdapter();
                        if (adapter != null) {
                            settingsManagerInstance.getPlanRouteSettings().setWayClassList(adapter.getWayClassList());
                            dialog.dismiss();
                        }
                    }
                });
                // neutral button
                Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        WayClassAdapter adapter = (WayClassAdapter) listViewWayClasses.getAdapter();
                        if (adapter != null) {
                            adapter.resetToDefaults();
                        }
                    }
                });
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            WayClassAdapter adapter = (WayClassAdapter) listViewWayClasses.getAdapter();
            if (adapter != null) {
                JSONArray jsonWayClassList = new JSONArray();
                for (WayClass wayClass : adapter.getWayClassList()) {
                    try {
                        jsonWayClassList.put(wayClass.toJson());
                    } catch (JSONException e) {}
                }
                savedInstanceState.putString("jsonWayClassList", jsonWayClassList.toString());
            }
        }
    }

}
