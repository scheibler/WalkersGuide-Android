package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.R;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.poi.FavoritesProfile;
import org.walkersguide.android.data.route.Route;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.google.AddressManager;
import org.walkersguide.android.helper.PointUtility;
import org.walkersguide.android.listener.AddressListener;
import org.walkersguide.android.listener.ChildDialogCloseListener;
import org.walkersguide.android.listener.RouteListener;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.RouteManager;
import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.RouteSettings;

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

public class PlanRouteDialog extends DialogFragment
    implements AddressListener, ChildDialogCloseListener, RouteListener {

    private AccessDatabase accessDatabaseInstance;
    private PositionManager positionManagerInstance;
    private RouteManager routeManagerInstance;
    private SettingsManager settingsManagerInstance;
    private AddressManager addressManagerRequest;

    // query in progress vibration
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private Vibrator vibrator;

    // ui components
    private Button buttonStartPoint, buttonDestinationPoint, buttonIndirectionFactor;

    public static PlanRouteDialog newInstance() {
        PlanRouteDialog planRouteDialogInstance = new PlanRouteDialog();
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
        addressManagerRequest = null;
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

        buttonStartPoint = (Button) view.findViewById(R.id.buttonStartPoint);
        buttonStartPoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectPointDialog selectPointDialog = SelectPointDialog.newInstance(Constants.POINT_PUT_INTO.START);
                selectPointDialog.setTargetFragment(PlanRouteDialog.this, 1);
                selectPointDialog.show(getActivity().getSupportFragmentManager(), "SelectPointDialog");
            }
        });

        buttonDestinationPoint = (Button) view.findViewById(R.id.buttonDestinationPoint);
        buttonDestinationPoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectPointDialog selectPointDialog = SelectPointDialog.newInstance(Constants.POINT_PUT_INTO.DESTINATION);
                selectPointDialog.setTargetFragment(PlanRouteDialog.this, 1);
                selectPointDialog.show(getActivity().getSupportFragmentManager(), "SelectPointDialog");
            }
        });

        Button buttonNewRoute = (Button) view.findViewById(R.id.buttonNewRoute);
        buttonNewRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                RouteSettings routeSettings = settingsManagerInstance.getRouteSettings();
                routeSettings.setStartPoint(PositionManager.getDummyLocation(getActivity()));
                routeSettings.setDestinationPoint(PositionManager.getDummyLocation(getActivity()));
                positionManagerInstance.requestCurrentLocation();
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

        buttonIndirectionFactor = (Button) view.findViewById(R.id.buttonIndirectionFactor);
        buttonIndirectionFactor.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectIndirectionFactorDialog.newInstance().show(
                        getActivity().getSupportFragmentManager(), "SelectIndirectionFactorDialog");
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
            // set indirection factor
            buttonIndirectionFactor.setText(
                    String.format(
                        "%1$s: %2$.1f",
                        getResources().getString(R.string.buttonIndirectionFactor),
                        settingsManagerInstance.getRouteSettings().getIndirectionFactor())
                    );
        }
    }

    @Override public void addressRequestFinished(int returnCode, String returnMessage, PointWrapper addressPoint) {
        if (returnCode == Constants.ID.OK) {
            PointUtility.putNewPoint(
                    getActivity(), addressPoint, addressManagerRequest.getPointPutIntoVariable());
            // request gps location again to refresh
            positionManagerInstance.requestCurrentLocation();
        }
    }

    @Override public void childDialogClosed() {
        // request gps location again to refresh dialog
        positionManagerInstance.requestCurrentLocation();
    }

	@Override public void routeCalculationFinished(int returnCode, String returnMessage, int routeId) {
        progressHandler.removeCallbacks(progressUpdater);
        if (returnCode == Constants.ID.OK) {
            RouteSettings routeSettings = settingsManagerInstance.getRouteSettings();
            routeSettings.setSelectedRouteId(routeId);
            // add start and destination to route points favorites profile
            PointWrapper startPoint = routeSettings.getStartPoint();
            if (! startPoint.equals(PositionManager.getDummyLocation(getActivity()))
                    && ! GPS.class.isInstance(startPoint.getPoint())) {
                accessDatabaseInstance.addPointToFavoritesProfile(startPoint, FavoritesProfile.ID_ROUTE_POINTS);
            }
            PointWrapper destinationPoint = routeSettings.getDestinationPoint();
            if (! destinationPoint.equals(PositionManager.getDummyLocation(getActivity()))
                    && ! GPS.class.isInstance(destinationPoint.getPoint())) {
                accessDatabaseInstance.addPointToFavoritesProfile(destinationPoint, FavoritesProfile.ID_ROUTE_POINTS);
            }
            // show router fragment of main activity
    		settingsManagerInstance.getGeneralSettings().setRecentOpenTab(Constants.MAIN_FRAGMENT.ROUTER);
            Intent mainActivityIntent = new Intent(getActivity(), MainActivity.class);
            mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(mainActivityIntent);
            vibrator.vibrate(250);
            dismiss();
        } else {
            // change positive button text back
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setText(getResources().getString(R.string.buttonGetRoute));
            }
            // error dialog
            SimpleMessageDialog.newInstance(returnMessage)
                .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
        }
    }

	@Override public void routeRequestFinished(int returnCode, String returnMessage, Route route) {
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (addressManagerRequest != null
                && addressManagerRequest.getStatus() != AsyncTask.Status.FINISHED) {
            addressManagerRequest.cancel();
        }
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        progressHandler.removeCallbacks(progressUpdater);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            RouteSettings routeSettings = SettingsManager.getInstance(context).getRouteSettings();
            if (intent.getAction().equals(Constants.ACTION_NEW_LOCATION)) {
                PointWrapper startPoint = routeSettings.getStartPoint();
                if (startPoint.equals(PositionManager.getDummyLocation(getActivity()))) {
                    buttonStartPoint.setText(
                            String.format(
                                "%1$s\n%2$s",
                                context.getResources().getString(R.string.buttonStartPoint),
                                context.getResources().getString(R.string.labelNoSimulatedPointSelected))
                            );
                } else {
                    buttonStartPoint.setText(
                            String.format(
                                "%1$s\n%2$s",
                                context.getResources().getString(R.string.buttonStartPoint),
                                startPoint.toString())
                            );
                    if (startPoint.getPoint() instanceof GPS
                            && (
                                   addressManagerRequest == null
                                || addressManagerRequest.getStatus() == AsyncTask.Status.FINISHED)
                            ) {
                        addressManagerRequest = new AddressManager(
                                getActivity(),
                                PlanRouteDialog.this,
                            startPoint.getPoint().getLatitude(),
                            startPoint.getPoint().getLongitude());
                        addressManagerRequest.setPointPutIntoVariable(Constants.POINT_PUT_INTO.START);
                        addressManagerRequest.execute();
                    }
                }
                PointWrapper destinationPoint = routeSettings.getDestinationPoint();
                if (destinationPoint.equals(PositionManager.getDummyLocation(getActivity()))) {
                    buttonDestinationPoint.setText(
                            String.format(
                                "%1$s\n%2$s",
                                context.getResources().getString(R.string.buttonDestinationPoint),
                                context.getResources().getString(R.string.labelNoSimulatedPointSelected))
                            );
                } else {
                    buttonDestinationPoint.setText(
                            String.format(
                                "%1$s\n%2$s",
                                context.getResources().getString(R.string.buttonDestinationPoint),
                                destinationPoint.toString())
                            );
                    if (destinationPoint.getPoint() instanceof GPS
                            && (
                                   addressManagerRequest == null
                                || addressManagerRequest.getStatus() == AsyncTask.Status.FINISHED)
                            ) {
                        addressManagerRequest = new AddressManager(
                                getActivity(),
                                PlanRouteDialog.this,
                            destinationPoint.getPoint().getLatitude(),
                            destinationPoint.getPoint().getLongitude());
                        addressManagerRequest.setPointPutIntoVariable(Constants.POINT_PUT_INTO.DESTINATION);
                        addressManagerRequest.execute();
                    }
                }

            } else if(intent.getAction().equals(Constants.ACTION_UPDATE_UI)) {
                onStart();
            }
        }
    };

    private class ProgressUpdater implements Runnable {
        public void run() {
            vibrator.vibrate(50);
            progressHandler.postDelayed(this, 2000);
        }
    }

}
