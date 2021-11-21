package org.walkersguide.android.ui.dialog;

import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

import org.walkersguide.android.R;
import org.walkersguide.android.data.route.HikingTrail;
import org.walkersguide.android.util.GlobalInstance;
import android.widget.TextView;
import android.widget.ImageButton;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import androidx.core.view.ViewCompat;
import android.widget.AbsListView;
import org.walkersguide.android.server.util.ServerUtility;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.sensor.PositionManager;
import javax.net.ssl.HttpsURLConnection;
import org.walkersguide.android.server.util.ServerInstance;
import org.json.JSONObject;
import org.walkersguide.android.server.util.ServerCommunicationException;
import org.json.JSONArray;
import java.io.IOException;
import org.json.JSONException;
import android.view.MenuItem;
import timber.log.Timber;
import org.walkersguide.android.data.basic.point.Point;

import java.util.concurrent.ExecutorService;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;
import java.io.InputStream;


public class HikingTrailsDialog extends DialogFragment {
    private static final int DEFAULT_TRAIL_RADIUS = 25000;

    // Store instance variables
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Future hikingTrailsRequest;

    private int listPosition;

	// ui components
	private ListView listViewHikingTrails;
    private TextView labelHeading, labelEmptyListView;
	private ImageButton buttonRefresh;

    public static HikingTrailsDialog newInstance() {
        HikingTrailsDialog dialog = new HikingTrailsDialog();
        return dialog;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt("listPosition");
        } else {
            listPosition = 0;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_heading_and_list_view_with_refresh_button, nullParent);

        buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                listPosition = 0;
                prepareRequest();
            }
        });

        listViewHikingTrails = (ListView) view.findViewById(R.id.listView);
        listViewHikingTrails.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                TrailDetailsDialog.newInstance(
                        (HikingTrail) parent.getItemAtPosition(position))
                    .show(getChildFragmentManager(), "TrailDetailsDialog");
            }
        });

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        listViewHikingTrails.setEmptyView(labelEmptyListView);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.hikingTrailsDialogTitle))
            .setView(view)
            .setNegativeButton(
                    getResources().getString(R.string.dialogClose),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dialog.dismiss();
                }
            });
            prepareRequest();
        }
    }

    @Override public void onStop() {
        super.onStop();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("listPosition",  listPosition);
    }

    private void updateRefreshButton(boolean showCancel) {
        if (showCancel) {
            buttonRefresh.setContentDescription(
                    getResources().getString(R.string.buttonCancel));
            buttonRefresh.setImageResource(R.drawable.cancel);
        } else {
            buttonRefresh.setContentDescription(
                    getResources().getString(R.string.buttonRefresh));
            buttonRefresh.setImageResource(R.drawable.refresh);
        }
    }

    private void prepareRequest() {
        updateRefreshButton(true);

        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelHeading.setText(
                getResources().getQuantityString(R.plurals.result, 0, 0));

        // list view
        listViewHikingTrails.setAdapter(null);
        listViewHikingTrails.setOnScrollListener(null);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setText(
                getResources().getString(R.string.messagePleaseWait));

        // query hiking trails
        if (hikingTrailsRequest == null || hikingTrailsRequest.isDone()) {
            hikingTrailsRequest = this.executorService.submit(() -> {
                try {
                    // get current location
                    PositionManager positionManagerInstance = PositionManager.getInstance();
                    Point currentLocation = positionManagerInstance.getCurrentLocation();
                    if (currentLocation == null) {
                        throw new ServerCommunicationException(Constants.RC.NO_LOCATION_FOUND);
                    }

                    JSONObject jsonServerParams = null;
                    try {
                        jsonServerParams = ServerUtility.createServerParamList();
                        jsonServerParams.put("lat", currentLocation.getLatitude());
                        jsonServerParams.put("lon", currentLocation.getLongitude());
                        jsonServerParams.put("radius", DEFAULT_TRAIL_RADIUS);
                    } catch (JSONException e) {
                        throw new ServerCommunicationException(Constants.RC.BAD_REQUEST);
                    }

                    HttpsURLConnection connection = null;
                    InputStream in = null;
                    ServerCommunicationException scException = null;
                    ArrayList<HikingTrail> hikingTrailList = null;
                    try {
                        ServerInstance serverInstance = ServerUtility.getServerInstance(
                                SettingsManager.getInstance().getServerURL());
                        connection = ServerUtility.connectToServer(
                                String.format(
                                    "%1$s/%2$s", serverInstance.getServerURL(), Constants.SERVER_COMMAND.GET_HIKING_TRAILS),
                                jsonServerParams);
                        in = ServerUtility.getInputStream(connection);
                        JSONArray jsonHikingTrailList = new JSONObject(
                                ServerUtility.getServerResponse(in))
                            .getJSONArray("hiking_trails");

                        // parse trails
                        hikingTrailList = new ArrayList<HikingTrail>();
                        for (int i=0; i<jsonHikingTrailList.length(); i++) {
                            hikingTrailList.add(
                                    new HikingTrail(
                                        jsonHikingTrailList.getJSONObject(i)));
                        }
                    } catch (ServerCommunicationException e) {
                        scException = e;
                    } catch (IOException e) {
                        Timber.e("ioError: %1$s", e.getMessage());
                        scException = new ServerCommunicationException(Constants.RC.CONNECTION_FAILED);
                    } catch (JSONException e) {
                        Timber.e("JSONException: %1$s", e.getMessage());
                        scException = new ServerCommunicationException(Constants.RC.BAD_RESPONSE);
                    } finally {
                        ServerUtility.cleanUp(connection, in);
                        if (scException != null) {
                            throw scException;
                        }
                    }

                    final ArrayList<HikingTrail> finalHikingTrailList = hikingTrailList;
                    handler.post(() -> {
                        hikingTrailsRequestSuccessful(finalHikingTrailList);
                    });

                } catch (ServerCommunicationException e) {
                    final ServerCommunicationException scException = e;
                    handler.post(() -> {
                        updateRefreshButton(false);
                        ViewCompat.setAccessibilityLiveRegion(
                                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                        labelEmptyListView.setText(
                                ServerUtility.getErrorMessageForReturnCode(scException.getReturnCode()));
                    });
                }
            });
        }
    }

    private void hikingTrailsRequestSuccessful(ArrayList<HikingTrail> hikingTrailList) {
        // heading
        updateRefreshButton(false);
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        labelHeading.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelHikingTrailsHeading),
                    GlobalInstance.getPluralResource(R.plurals.hikingTrail, hikingTrailList.size()),
                    DEFAULT_TRAIL_RADIUS / 1000)
                );

        // list adapter
        listViewHikingTrails.setAdapter(
                new ArrayAdapter<HikingTrail>(
                    HikingTrailsDialog.this.getContext(),
                    android.R.layout.simple_list_item_1,
                    hikingTrailList)
                );

        // list position
        listViewHikingTrails.setSelection(listPosition);
        listViewHikingTrails.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });

        // empty view
        labelEmptyListView.setText(
                GlobalInstance.getStringResource(R.string.labelNoHikingTrailsNearby));
    }


    /**
     * trail details dialog
     */

    public static class TrailDetailsDialog extends DialogFragment {

        private HikingTrail trail;

        public static TrailDetailsDialog newInstance(HikingTrail trail) {
            TrailDetailsDialog dialog = new TrailDetailsDialog();
            Bundle args = new Bundle();
            args.putSerializable("trail", trail);
            dialog.setArguments(args);
            return dialog;
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_trail_details, nullParent);

            trail = (HikingTrail) getArguments().getSerializable("trail");
            if (trail != null) {

                TextView labelTrailName = (TextView) view.findViewById(R.id.labelTrailName);
                labelTrailName.setText(
                        String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.labelTrailName),
                            trail.getName())
                        );

                TextView labelTrailDescription = (TextView) view.findViewById(R.id.labelTrailDescription);
                if (trail.getDescription() != null) {
                    labelTrailDescription.setText(
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelTrailDescription),
                                trail.getDescription())
                            );
                    labelTrailDescription.setVisibility(View.VISIBLE);
                } else {
                    labelTrailDescription.setVisibility(View.GONE);
                }

                TextView labelTrailLength = (TextView) view.findViewById(R.id.labelTrailLength);
                if (trail.getLength() != null) {
                    labelTrailLength.setText(
                            String.format(
                                "%1$s: %2$s km",
                                getResources().getString(R.string.labelTrailLength),
                                trail.getLength())
                            );
                    labelTrailLength.setVisibility(View.VISIBLE);
                } else {
                    labelTrailLength.setVisibility(View.GONE);
                }

                TextView labelTrailSymbol = (TextView) view.findViewById(R.id.labelTrailSymbol);
                if (trail.getSymbol() != null) {
                    labelTrailSymbol.setText(
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelTrailSymbol),
                                trail.getSymbol())
                            );
                    labelTrailSymbol.setVisibility(View.VISIBLE);
                } else {
                    labelTrailSymbol.setVisibility(View.GONE);
                }

                // distances

                TextView labelDistanceToTrailStart = (TextView) view.findViewById(R.id.labelDistanceToTrailStart);
                labelDistanceToTrailStart.setText(
                        String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.labelDistanceToTrailStart),
                            getResources().getQuantityString(
                                R.plurals.meter, trail.getDistanceToStart(), trail.getDistanceToStart()))
                        );

                TextView labelDistanceToTrailDestination = (TextView) view.findViewById(R.id.labelDistanceToTrailDestination);
                labelDistanceToTrailDestination.setText(
                        String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.labelDistanceToTrailDestination),
                            getResources().getQuantityString(
                                R.plurals.meter, trail.getDistanceToDestination(), trail.getDistanceToDestination()))
                        );

                TextView labelDistanceToTrailClosest = (TextView) view.findViewById(R.id.labelDistanceToTrailClosest);
                labelDistanceToTrailClosest.setText(
                        String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.labelDistanceToTrailClosest),
                            getResources().getQuantityString(
                                R.plurals.meter, trail.getDistanceToClosest(), trail.getDistanceToClosest()))
                        );
            }

            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.trailDetailsDialogTitle))
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.buttonGetRoute),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogCancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                        )
                .create();
        }

        @Override public void onStart() {
            super.onStart();
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                // positive button
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        // show direction selection popup menu
                        PopupMenu popupDirection = new PopupMenu(getActivity(), view);
                        popupDirection.inflate(R.menu.menu_button_hiking_trails_dialog);
                        popupDirection.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.menuItemDirectionForwards:
                                        return true;
                                    case R.id.menuItemDirectionBackwards:
                                        return true;
                                    default:
                                        return false;
                                }
                            }
                        });
                        popupDirection.show();
                        // change button text
                        Button buttonPositive = (Button) view;
                        buttonPositive.setText(getResources().getString(R.string.dialogCancel));
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

        @Override public void onStop() {
            super.onStop();
        }
    }

}
