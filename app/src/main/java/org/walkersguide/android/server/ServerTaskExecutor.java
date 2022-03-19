package org.walkersguide.android.server;

import org.walkersguide.android.data.object_with_id.HikingTrail;
import org.walkersguide.android.server.wg.poi.PoiProfileResult;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Stop;

import org.walkersguide.android.server.wg.CancelRunningRequestTask;
import org.walkersguide.android.server.ServerException;
import org.walkersguide.android.server.wg.WgUtility;

import java.util.concurrent.ExecutorService;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.walkersguide.android.util.GlobalInstance;
import timber.log.Timber;
import java.util.HashMap;
import org.walkersguide.android.server.wg.status.ServerInstance;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import java.util.ArrayList;


public class ServerTaskExecutor {
    public static final long NO_TASK_ID = 0;
    public static final long INVALID_TASK_ID = -1;


    /**
     * singleton
     */

    private static ServerTaskExecutor serverTaskExecutorInstance;

    public static ServerTaskExecutor getInstance() {
        if (serverTaskExecutorInstance == null){
            serverTaskExecutorInstance = getInstanceSynchronized();
        }
        return serverTaskExecutorInstance;
    }

    private static synchronized ServerTaskExecutor getInstanceSynchronized() {
        if (serverTaskExecutorInstance == null){
            serverTaskExecutorInstance = new ServerTaskExecutor();
        }
        return serverTaskExecutorInstance;
    }

    private ServerTaskExecutor() {
    }


    /**
     * broadcasts
     */

    // contained by every outgoing broadcast
    public static final String EXTRA_TASK_ID = "taskId";

    private static Intent createActionIntentWithTaskId(String action, long taskId) {
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        return intent;
    }

    // successful tasks

    // address resolver

    // resolve address string
    public static final String ACTION_RESOLVE_ADDRESS_STRING_TASK_SUCCESSFUL = "action.resolveAddressStringTaskSuccessful";
    public static final String EXTRA_STREET_ADDRESS_LIST = "streetAddressList";

    public static void sendResolveAddressStringTaskSuccessfulBroadcast(long taskId, ArrayList<StreetAddress> addressList) {
        Intent intent = createActionIntentWithTaskId(ACTION_RESOLVE_ADDRESS_STRING_TASK_SUCCESSFUL, taskId);
        intent.putExtra(EXTRA_STREET_ADDRESS_LIST, addressList);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    // resolve coordinates
    public static final String ACTION_RESOLVE_COORDINATES_TASK_SUCCESSFUL = "action.resolveCoordinatesTaskSuccessful";
    public static final String EXTRA_STREET_ADDRESS = "streetAddress";

    public static void sendResolveCoordinatesTaskSuccessfulBroadcast(long taskId, StreetAddress address) {
        Intent intent = createActionIntentWithTaskId(ACTION_RESOLVE_COORDINATES_TASK_SUCCESSFUL, taskId);
        intent.putExtra(EXTRA_STREET_ADDRESS, address);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    // public transport

    // nearby stations
    public static final String ACTION_NEARBY_STATIONS_TASK_SUCCESSFUL = "action.nearbyStationsTaskSuccessful";
    public static final String EXTRA_STATION_LIST = "stationList";

    public static void sendNearbyStationsTaskSuccessfulBroadcast(long taskId, ArrayList<Location> stationList) {
        Intent intent = createActionIntentWithTaskId(ACTION_NEARBY_STATIONS_TASK_SUCCESSFUL, taskId);
        intent.putExtra(EXTRA_STATION_LIST, stationList);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    // station departures
    public static final String ACTION_STATION_DEPARTURES_TASK_SUCCESSFUL = "action.stationDeparturesTaskSuccessful";
    public static final String EXTRA_DEPARTURE_LIST = "departureList";

    public static void sendStationDeparturesTaskSuccessfulBroadcast(long taskId, ArrayList<Departure> departureList) {
        Intent intent = createActionIntentWithTaskId(ACTION_STATION_DEPARTURES_TASK_SUCCESSFUL, taskId);
        intent.putExtra(EXTRA_DEPARTURE_LIST, departureList);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    // trip details
    public static final String ACTION_TRIP_DETAILS_TASK_SUCCESSFUL = "action.tripDetailsTaskSuccessful";
    public static final String EXTRA_STOP_LIST = "stopList";

    public static void sendTripDetailsTaskSuccessfulBroadcast(long taskId, ArrayList<Stop> stopList) {
        Intent intent = createActionIntentWithTaskId(ACTION_TRIP_DETAILS_TASK_SUCCESSFUL, taskId);
        intent.putExtra(EXTRA_STOP_LIST, stopList);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    // wg server

    // hiking trails
    public static final String ACTION_HIKING_TRAILS_TASK_SUCCESSFUL = "action.hikingTrailsTaskSuccessful";
    public static final String EXTRA_HIKING_TRAIL_LIST  = "hikingTrailList";

    public static void sendHikingTrailsTaskSuccessfulBroadcast(long taskId, ArrayList<HikingTrail> hikingTrailList) {
        Intent intent = createActionIntentWithTaskId(ACTION_HIKING_TRAILS_TASK_SUCCESSFUL, taskId);
        intent.putExtra(EXTRA_HIKING_TRAIL_LIST, hikingTrailList);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    // poi
    public static final String ACTION_POI_PROFILE_TASK_SUCCESSFUL = "action.poiProfileTaskSuccessful";
    public static final String EXTRA_POI_PROFILE_RESULT  = "poiProfileResult";

    public static void sendPoiProfileTaskSuccessfulBroadcast(long taskId, PoiProfileResult result) {
        Intent intent = createActionIntentWithTaskId(ACTION_POI_PROFILE_TASK_SUCCESSFUL, taskId);
        intent.putExtra(EXTRA_POI_PROFILE_RESULT, result);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    // route
    public static final String ACTION_P2P_ROUTE_TASK_SUCCESSFUL = "action.p2pRouteTaskSuccessful";
    public static final String ACTION_STREET_COURSE_TASK_SUCCESSFUL = "action.streetCourseTaskSuccessful";
    public static final String EXTRA_ROUTE = "route";

    public static void sendP2pRouteTaskSuccessfulBroadcast(long taskId, Route route) {
        Intent intent = createActionIntentWithTaskId(ACTION_P2P_ROUTE_TASK_SUCCESSFUL, taskId);
        intent.putExtra(EXTRA_ROUTE, route);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    public static void sendStreetCourseTaskSuccessfulBroadcast(long taskId, Route route) {
        Intent intent = createActionIntentWithTaskId(ACTION_STREET_COURSE_TASK_SUCCESSFUL, taskId);
        intent.putExtra(EXTRA_ROUTE, route);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    // server status
    public static final String ACTION_SERVER_STATUS_TASK_SUCCESSFUL = "action.serverStatusTaskSuccessful";
    public static final String EXTRA_SERVER_INSTANCE = "serverInstance";

    public static void sendServerStatusTaskSuccessfulBroadcast(long taskId, ServerInstance serverInstance) {
        Intent intent = createActionIntentWithTaskId(ACTION_SERVER_STATUS_TASK_SUCCESSFUL, taskId);
        intent.putExtra(EXTRA_SERVER_INSTANCE, serverInstance);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    // cancel running request task
    public static final String ACTION_CANCEL_RUNNING_REQUEST_TASK_SUCCESSFUL = "action.cancelRunningRequestTaskSuccessful";

    public static void sendCancelRunningRequestSuccessfulBroadcast(long taskId) {
        Intent intent = createActionIntentWithTaskId(ACTION_CANCEL_RUNNING_REQUEST_TASK_SUCCESSFUL, taskId);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    // send feedback task
    public static final String ACTION_SEND_FEEDBACK_TASK_SUCCESSFUL = "action.sendFeedbackTaskSuccessful";

    public static void sendSendFeedbackSuccessfulBroadcast(long taskId) {
        Intent intent = createActionIntentWithTaskId(ACTION_SEND_FEEDBACK_TASK_SUCCESSFUL, taskId);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    // unsuccessful tasks

    // cancelled
    public static final String ACTION_SERVER_TASK_CANCELLED = "action.serverTaskCancelled";

    public static void sendServerTaskCancelledBroadcast(long taskId) {
        Timber.d("sendServerTaskCancelledBroadcast: %1$d", taskId);
        Intent intent = createActionIntentWithTaskId(ACTION_SERVER_TASK_CANCELLED, taskId);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    // failed
    public static final String ACTION_SERVER_TASK_FAILED = "action.serverTaskFailed";
    public static final String EXTRA_EXCEPTION = "exception";

    public static void sendServerTaskFailedBroadcast(long taskId, ServerException exception) {
        Intent intent = createActionIntentWithTaskId(ACTION_SERVER_TASK_FAILED, taskId);
        intent.putExtra(EXTRA_EXCEPTION, exception);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }


    /**
     * execute future
     */

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private HashMap<Long,Future> requestMap = new HashMap<Long,Future>();;

    public long executeTask(final ServerTask task) {
        Timber.d("new newTask: %1$s", task.getId());
        requestMap.put(
                task.getId(),
                this.executorService.submit(() -> {
                    try {
                        task.execute();
                    } catch (ServerException e) {
                        if (! task.isCancelled()) {
                            sendServerTaskFailedBroadcast(task.getId(), e);
                        }
                    }
                }));
        return task.getId();
    }

    public boolean taskInProgress(final long taskId) {
        Future request = this.requestMap.get(taskId);
        return request != null && ! request.isCancelled() && ! request.isDone();
    }

    public boolean taskIsCancelled(final long taskId) {
        Future request = this.requestMap.get(taskId);
        return request != null && request.isCancelled();
    }

    public void cancelTask(final long taskId) {
        cancelTask(taskId, false);
    }

    public void cancelTask(final long taskId, boolean sendCancelRunningRequestTask) {
        if (taskInProgress(taskId)) {
            this.requestMap.get(taskId).cancel(true);
            sendServerTaskCancelledBroadcast(taskId);
            if (sendCancelRunningRequestTask) {
                executeTask(new CancelRunningRequestTask());
            }
        }
    }

}
