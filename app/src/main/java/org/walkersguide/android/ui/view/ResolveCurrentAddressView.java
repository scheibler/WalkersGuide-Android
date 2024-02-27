        package org.walkersguide.android.ui.view;

import timber.log.Timber;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.view.builder.TextViewBuilder;
import android.widget.LinearLayout;
import android.content.Context;
import android.util.AttributeSet;
import android.os.Bundle;
import android.content.IntentFilter;
import org.walkersguide.android.server.ServerTaskExecutor;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.address.ResolveCoordinatesTask;
import android.content.BroadcastReceiver;
import android.content.Intent;
import org.walkersguide.android.data.object_with_id.point.point_with_address_data.StreetAddress;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.server.address.AddressException;
import org.walkersguide.android.sensor.position.AcceptNewPosition;


public class ResolveCurrentAddressView extends LinearLayout {

    public interface OnCurrentAddressResolvedListener {
        public void onCurrentAddressResolved(StreetAddress addressPoint);
    }

    private OnCurrentAddressResolvedListener onCurrentAddressResolvedListener;

    public void setOnCurrentAddressResolvedListener(OnCurrentAddressResolvedListener listener) {
        onCurrentAddressResolvedListener = listener;
    }


    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;

    public long getTaskId() {
        return this.taskId;
    }

    public void setTaskId(long newTaskId) {
        this.taskId = newTaskId;
    }

    public void cancelTask() {
        serverTaskExecutorInstance.cancelTask(taskId);
    }


    private ObjectWithIdView layoutCurrentAddress;

    public ResolveCurrentAddressView(Context context) {
        super(context);
        this.initUi(context);
    }

    public ResolveCurrentAddressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initUi(context);
    }

    private void initUi(Context context) {
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
        taskId = ServerTaskExecutor.NO_TASK_ID;
        onCurrentAddressResolvedListener = null;

        // configure enclosing linear layout
        setOrientation(LinearLayout.VERTICAL);

        // current address
        layoutCurrentAddress = new ObjectWithIdView(
                context, getResources().getString(R.string.pointSelectFromClosestAddress));
        layoutCurrentAddress.setLayoutParams(
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        addView(layoutCurrentAddress);
    }

    @Override public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).unregisterReceiver(localIntentReceiver);
        Timber.d("onDetachedFromWindow");
    }

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_RESOLVE_COORDINATES_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        localIntentFilter.addAction(PositionManager.ACTION_NEW_LOCATION);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).registerReceiver(localIntentReceiver, localIntentFilter);

        // request address
        PositionManager.getInstance().requestCurrentLocation();
        Timber.d("onAttachedToWindow");
    }

    public void requestAddressForCurrentLocation() {
        layoutCurrentAddress.reset();

        // get current position
        final Point currentLocation = PositionManager.getInstance().getCurrentLocation();
        if (currentLocation == null) {
            layoutCurrentAddress.setEmptyLabelText(
                    getResources().getString(R.string.errorNoLocationFound));
            return;
        }

        layoutCurrentAddress.setEmptyLabelText(
                getResources().getString(R.string.messagePleaseWait));
        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            taskId = serverTaskExecutorInstance.executeTask(
                    new ResolveCoordinatesTask(currentLocation));
        }
    }

    // background task results

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        private AcceptNewPosition acceptNewPosition = new AcceptNewPosition(50, 60, null);

        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_RESOLVE_COORDINATES_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_RESOLVE_COORDINATES_TASK_SUCCESSFUL)) {
                    StreetAddress addressPoint = (StreetAddress) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_STREET_ADDRESS);
                    if (addressPoint != null) {
                        layoutCurrentAddress.configureAsSingleObject(addressPoint);
                        if (onCurrentAddressResolvedListener != null) {
                            onCurrentAddressResolvedListener.onCurrentAddressResolved(addressPoint);
                        }
                    }

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {
                    layoutCurrentAddress.reset();
                    layoutCurrentAddress.setEmptyLabelText(
                            context.getResources().getString(R.string.errorReqRequestCancelled));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    AddressException addressException = (AddressException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (addressException != null) {
                        layoutCurrentAddress.reset();
                        layoutCurrentAddress.setEmptyLabelText(
                                addressException.getMessage());
                    }
                }

            } else if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                if (acceptNewPosition.updatePoint(
                            (Point) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION),
                            false,
                            intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false))) {
                    requestAddressForCurrentLocation();
                }
            }
        }
    };

}
