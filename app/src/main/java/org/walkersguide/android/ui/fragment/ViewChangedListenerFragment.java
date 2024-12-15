package org.walkersguide.android.ui.fragment;

import org.walkersguide.android.ui.interfaces.ViewChangedListener;

import android.os.Bundle;


import androidx.fragment.app.Fragment;

import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.MainActivityController;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;


public abstract class ViewChangedListenerFragment extends Fragment implements ViewChangedListener {
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
