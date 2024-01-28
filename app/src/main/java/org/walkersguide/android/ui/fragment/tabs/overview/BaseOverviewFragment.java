package org.walkersguide.android.ui.fragment.tabs.overview;

import org.walkersguide.android.util.Helper;
import timber.log.Timber;
import org.walkersguide.android.ui.interfaces.ViewChangedListener;
import org.walkersguide.android.data.profile.MutableProfile;
import org.walkersguide.android.ui.adapter.PinnedObjectsAdapter;
import org.walkersguide.android.ui.adapter.PinnedObjectsAdapter.OnAddButtonClick;
import org.walkersguide.android.database.profile.StaticProfile;
import org.walkersguide.android.ui.fragment.profile_list.CollectionListFragment;
import org.walkersguide.android.ui.fragment.HistoryFragment;
import org.walkersguide.android.R;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import android.widget.TextView;
import android.widget.AbsListView;
import java.util.concurrent.Executors;
import org.walkersguide.android.data.ObjectWithId;
import java.util.ArrayList;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.database.DatabaseProfileRequest;
import android.os.Handler;
import android.os.Looper;
import org.walkersguide.android.ui.dialog.select.SelectProfileFromMultipleSourcesDialog;
import org.walkersguide.android.ui.dialog.select.SelectObjectWithIdFromMultipleSourcesDialog;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.MainActivityController;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentResultListener;
import android.content.Context;
import android.widget.Button;
import androidx.lifecycle.Lifecycle;
import org.walkersguide.android.ui.view.ResolveCurrentAddressView;
import android.widget.ExpandableListView;
import android.content.BroadcastReceiver;
import org.walkersguide.android.data.Profile;
import android.widget.BaseExpandableListAdapter;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import org.walkersguide.android.sensor.DeviceSensorManager;


public abstract class BaseOverviewFragment extends Fragment implements ViewChangedListener {
    private final static String KEY_LIST_POSITION = "listPosition";

    public abstract void requestUiUpdate();

    protected MainActivityController mainActivityController;
    protected int listPosition;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            listPosition = 0;
        }
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            if (activity instanceof MainActivity) {
                mainActivityController = (MainActivityController) ((MainActivity) activity);
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_LIST_POSITION, listPosition);
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        unregisterViewChangedBroadcastReceiver(viewChangedBroadcastReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        registerViewChangedBroadcastReceiver(viewChangedBroadcastReceiver);
        requestUiUpdate();
    }

    private BroadcastReceiver viewChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ViewChangedListener.ACTION_OBJECT_WITH_ID_LIST_CHANGED)
                    || intent.getAction().equals(ViewChangedListener.ACTION_PROFILE_LIST_CHANGED)) {
                requestUiUpdate();
            }
        }
    };

}
