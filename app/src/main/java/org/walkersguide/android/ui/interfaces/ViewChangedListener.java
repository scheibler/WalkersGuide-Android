package org.walkersguide.android.ui.interfaces;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.walkersguide.android.util.GlobalInstance;


public interface ViewChangedListener {

    public static final String ACTION_OBJECT_WITH_ID_LIST_CHANGED = "action.objectWithIdListChanged";

    public static void sendObjectWithIdListChangedBroadcast() {
        Intent intent = new Intent(ACTION_OBJECT_WITH_ID_LIST_CHANGED);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext())
            .sendBroadcast(intent);
    }

    public static final String ACTION_PROFILE_LIST_CHANGED = "action.profileListChanged";

    public static void sendProfileListChangedBroadcast() {
        Intent intent = new Intent(ACTION_PROFILE_LIST_CHANGED);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext())
            .sendBroadcast(intent);
    }


    public default void registerViewChangedBroadcastReceiver(BroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_OBJECT_WITH_ID_LIST_CHANGED);
        filter.addAction(ACTION_PROFILE_LIST_CHANGED);
        LocalBroadcastManager.getInstance(GlobalInstance.getInstance())
            .registerReceiver(receiver, filter);
    }

    public default void unregisterViewChangedBroadcastReceiver(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(GlobalInstance.getInstance())
            .unregisterReceiver(receiver);
    }

}
