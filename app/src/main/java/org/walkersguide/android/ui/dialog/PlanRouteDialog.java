package org.walkersguide.android.ui.dialog;

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

import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;

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

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.HistoryPointProfile;
import org.walkersguide.android.data.route.WayClass;
import org.walkersguide.android.helper.PointUtility;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.RouteManager;
import org.walkersguide.android.server.RouteManager.RouteCalculationListener;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.adapter.WayClassAdapter;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog.ChildDialogCloseListener;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.RouteSettings;


public class PlanRouteDialog extends DialogFragment
    implements ChildDialogCloseListener, RouteCalculationListener {

    private AccessDatabase accessDatabaseInstance;
    private PositionManager positionManagerInstance;
    private RouteManager routeManagerInstance;
    private SettingsManager settingsManagerInstance;
    private boolean showGetCurrentLocationDialog;

    // query in progress vibration
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private Vibrator vibrator;

    // ui components
    private Button buttonStartPoint, buttonDestinationPoint;
    private LinearLayout layoutViaPointList;

    public static PlanRouteDialog newInstance(boolean showGetCurrentLocationDialog) {
        PlanRouteDialog planRouteDialogInstance = new PlanRouteDialog();
        Bundle args = new Bundle();
        args.putBoolean("showGetCurrentLocationDialog", showGetCurrentLocationDialog);
        planRouteDialogInstance.setArguments(args);
        return planRouteDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        accessDatabaseInstance = AccessDatabase.getInstance(context);
		positionManagerInstance = PositionManager.getInstance(context);
        routeManagerInstance = RouteManager.getInstance(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NEW_LOCATION);
        filter.addAction(Constants.ACTION_UPDATE_UI);
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
        // progress updater
        this.progressHandler = new Handler();
        this.progressUpdater = new ProgressUpdater();
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            showGetCurrentLocationDialog = savedInstanceState.getBoolean("showGetCurrentLocationDialog");
        } else {
            showGetCurrentLocationDialog = getArguments().getBoolean("showGetCurrentLocationDialog");
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_plan_route, nullParent);

        buttonStartPoint = (Button) view.findViewById(R.id.buttonStartPoint);
        buttonStartPoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                PointInputDialog pointInputDialog = PointInputDialog.newInstance(Constants.POINT_PUT_INTO.START);
                pointInputDialog.setTargetFragment(PlanRouteDialog.this, 1);
                pointInputDialog.show(getActivity().getSupportFragmentManager(), "PointInputDialog");
            }
        });

		layoutViaPointList = (LinearLayout) view.findViewById(R.id.layoutViaPointList);

        buttonDestinationPoint = (Button) view.findViewById(R.id.buttonDestinationPoint);
        buttonDestinationPoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                PointInputDialog pointInputDialog = PointInputDialog.newInstance(Constants.POINT_PUT_INTO.DESTINATION);
                pointInputDialog.setTargetFragment(PlanRouteDialog.this, 1);
                pointInputDialog.show(getActivity().getSupportFragmentManager(), "PointInputDialog");
            }
        });

        Button buttonNewRoute = (Button) view.findViewById(R.id.buttonNewRoute);
        buttonNewRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                RouteSettings routeSettings = settingsManagerInstance.getRouteSettings();
                routeSettings.removeStartPoint();
                routeSettings.clearViaPointList();
                routeSettings.removeDestinationPoint();
                positionManagerInstance.requestCurrentLocation();
            }
        });

        Button buttonAddViaPoint = (Button) view.findViewById(R.id.buttonAddViaPoint);
        buttonAddViaPoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                RouteSettings routeSettings = settingsManagerInstance.getRouteSettings();
                PointInputDialog pointInputDialog = PointInputDialog.newInstance(
                        Constants.POINT_PUT_INTO.VIA + routeSettings.getViaPointList().size());
                pointInputDialog.setTargetFragment(PlanRouteDialog.this, 1);
                pointInputDialog.show(getActivity().getSupportFragmentManager(), "PointInputDialog");
            }
        });

        Button buttonSwapRoute = (Button) view.findViewById(R.id.buttonSwapRoute);
        buttonSwapRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                RouteSettings routeSettings = settingsManagerInstance.getRouteSettings();
                PointWrapper tempPoint = routeSettings.getStartPoint();
                routeSettings.setStartPoint(routeSettings.getDestinationPoint());
                routeSettings.setDestinationPoint(tempPoint);
                positionManagerInstance.requestCurrentLocation();
            }
        });

        Button buttonExcludedWays = (Button) view.findViewById(R.id.buttonExcludedWays);
        buttonExcludedWays.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ExcludedWaysDialog.newInstance().show(
                        getActivity().getSupportFragmentManager(), "ExcludedWaysDialog");
            }
        });

        Button buttonRoutingWayClasses = (Button) view.findViewById(R.id.buttonRoutingWayClasses);
        buttonRoutingWayClasses.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectRoutingWayClassesDialog.newInstance().show(
                        getActivity().getSupportFragmentManager(), "SelectRoutingWayClassesDialog");
            }
        });

        // request gps location
        positionManagerInstance.requestCurrentLocation();

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
                        routeManagerInstance.calculateRoute(PlanRouteDialog.this);
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
                    dialog.dismiss();
                }
            });
            // show GetCurrentPositionDialog once on startup
            if (showGetCurrentLocationDialog) {
                GetCurrentPositionDialog getCurrentPositionDialog = GetCurrentPositionDialog.newInstance(Constants.POINT_PUT_INTO.START);
                getCurrentPositionDialog.setTargetFragment(PlanRouteDialog.this, 1);
                getCurrentPositionDialog.show(
                        getActivity().getSupportFragmentManager(), "GetCurrentPositionDialog");
                showGetCurrentLocationDialog = false;
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("showGetCurrentLocationDialog", showGetCurrentLocationDialog);
    }

    @Override public void childDialogClosed() {
        // request gps location again to refresh dialog
        positionManagerInstance.requestCurrentLocation();
    }

	@Override public void routeCalculationFinished(Context context, int returnCode, int routeId) {
        progressHandler.removeCallbacks(progressUpdater);
        if (returnCode == Constants.RC.OK
                && routeId != -1) {
            RouteSettings routeSettings = settingsManagerInstance.getRouteSettings();
            routeSettings.setSelectedRouteId(routeId);
            // add start and destination to route points history point profile
            PointWrapper startPoint = routeSettings.getStartPoint();
            if (startPoint != null
                    && ! GPS.class.isInstance(startPoint.getPoint())) {
                accessDatabaseInstance.addFavoritePointToProfile(startPoint, HistoryPointProfile.ID_ROUTE_POINTS);
            }
            PointWrapper destinationPoint = routeSettings.getDestinationPoint();
            if (destinationPoint != null
                    && ! GPS.class.isInstance(destinationPoint.getPoint())) {
                accessDatabaseInstance.addFavoritePointToProfile(destinationPoint, HistoryPointProfile.ID_ROUTE_POINTS);
            }
            // show router fragment of main activity
    		settingsManagerInstance.getGeneralSettings().setRecentOpenTab(Constants.MAIN_FRAGMENT.ROUTER);
            Intent mainActivityIntent = new Intent(context, MainActivity.class);
            mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(mainActivityIntent);
            vibrator.vibrate(250);
            dismiss();
        } else {
            // change positive button text back
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setText(context.getResources().getString(R.string.buttonGetRoute));
            }
            // error dialog
            if (isAdded()) {
                if (returnCode == Constants.RC.MAP_LOADING_FAILED
                        || returnCode == Constants.RC.WRONG_MAP_SELECTED) {
                    SelectMapDialog.newInstance()
                        .show(getActivity().getSupportFragmentManager(), "SelectMapDialog");
                } else {
                    SimpleMessageDialog.newInstance(
                            ServerUtility.getErrorMessageForReturnCode(context, returnCode))
                        .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
                }
            }
        }
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        progressHandler.removeCallbacks(progressUpdater);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            RouteSettings routeSettings = SettingsManager.getInstance(context).getRouteSettings();
            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)) {
                // start point button
                PointWrapper startPoint = routeSettings.getStartPoint();
                if (startPoint == null) {
                    buttonStartPoint.setText(
                            String.format(
                                "%1$s:\n%2$s",
                                context.getResources().getString(R.string.buttonStartPoint),
                                context.getResources().getString(R.string.labelNoPointSelected))
                            );
                } else {
                    buttonStartPoint.setText(
                            String.format(
                                "%1$s:\n%2$s",
                                context.getResources().getString(R.string.buttonStartPoint),
                                startPoint.toString())
                            );
                }

                // via points
                ArrayList<PointWrapper> viaPointList = routeSettings.getViaPointList();
                Timber.d("viaPointListSize: %1$d", viaPointList.size());
                if (layoutViaPointList.getChildCount() > viaPointList.size()) {
                    layoutViaPointList.removeViews(
                            viaPointList.size(), layoutViaPointList.getChildCount()-viaPointList.size());
                }
                int viaPointId = Constants.POINT_PUT_INTO.VIA;
                for (PointWrapper viaPoint : viaPointList) {
                    Button buttonViaPoint = (Button) layoutViaPointList.findViewById(viaPointId);
                    if (buttonViaPoint == null) {
                        buttonViaPoint = createViaPointButton(context, viaPointId);
                    }
                    if (viaPoint == null) {
                        buttonViaPoint.setText(
                                String.format(
                                    "%1$s %2$d:\n%3$s",
                                    context.getResources().getString(R.string.buttonViaPoint),
                                    (viaPointId - Constants.POINT_PUT_INTO.VIA) + 1,
                                    context.getResources().getString(R.string.labelNoPointSelected))
                                );
                    } else {
                        buttonViaPoint.setText(
                                String.format(
                                    "%1$s %2$d:\n%3$s",
                                    context.getResources().getString(R.string.buttonViaPoint),
                                    (viaPointId - Constants.POINT_PUT_INTO.VIA) + 1,
                                    viaPoint.toString())
                                );
                    }
                    if (layoutViaPointList.indexOfChild(buttonViaPoint) == -1) {
                        layoutViaPointList.addView(buttonViaPoint);
                    }
                    viaPointId += 1;
                }

                // destination point button
                PointWrapper destinationPoint = routeSettings.getDestinationPoint();
                if (destinationPoint == null) {
                    buttonDestinationPoint.setText(
                            String.format(
                                "%1$s:\n%2$s",
                                context.getResources().getString(R.string.buttonDestinationPoint),
                                context.getResources().getString(R.string.labelNoPointSelected))
                            );
                } else {
                    buttonDestinationPoint.setText(
                            String.format(
                                "%1$s:\n%2$s",
                                context.getResources().getString(R.string.buttonDestinationPoint),
                                destinationPoint.toString())
                            );
                }

            } else if(intent.getAction().equals(Constants.ACTION_UPDATE_UI)) {
                onStart();
            }
        }
    };

    private Button createViaPointButton(Context context, int id) {
        Timber.d("createViaPointButton: id=%1$d", id);
        Button button = new Button(context);
        button.setId(id);
        // layout params
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        button.setLayoutParams(lp);
        // top and bottom padding
        int padding = getResources().getDimensionPixelOffset(R.dimen.smallPadding);
        button.setPadding(0, padding, 0, padding);
        // click listener
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                PointInputDialog pointInputDialog = PointInputDialog.newInstance(view.getId());
                pointInputDialog.setTargetFragment(PlanRouteDialog.this, 1);
                pointInputDialog.show(getActivity().getSupportFragmentManager(), "PointInputDialog");
            }
        });
        return button;
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
            settingsManagerInstance = SettingsManager.getInstance(context);
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
                wayClassList = settingsManagerInstance.getRouteSettings().getWayClassList();
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
                            settingsManagerInstance.getRouteSettings().setWayClassList(adapter.getWayClassList());
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
