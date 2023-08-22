package org.walkersguide.android.ui.fragment.tabs.routes;

import org.walkersguide.android.ui.OnUpdateUiListener;
import android.content.IntentFilter;
import androidx.core.view.MenuProvider;
import org.walkersguide.android.util.WalkersGuideService;
import org.walkersguide.android.util.WalkersGuideService.RouteRecordingState;
import org.walkersguide.android.ui.fragment.tabs.ObjectDetailsTabLayoutFragment;
import android.widget.LinearLayout.LayoutParams;
import org.walkersguide.android.ui.fragment.tabs.HistoryTabLayoutFragment;
import org.walkersguide.android.ui.adapter.SimpleObjectWithIdAdapter;
import org.walkersguide.android.R;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.AbsListView;
import java.util.concurrent.Executors;
import org.walkersguide.android.data.ObjectWithId;
import java.util.ArrayList;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.database.SortMethod;
import android.os.Handler;
import android.os.Looper;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.ui.dialog.select.SelectRouteOrSimulationPointDialog;
import org.walkersguide.android.ui.dialog.select.EnterRouteNameDialog;
import org.walkersguide.android.ui.dialog.select.SelectRouteOrSimulationPointDialog.WhereToPut;
import org.walkersguide.android.data.object_with_id.Point;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;

import org.walkersguide.android.ui.activity.MainActivity;
import org.walkersguide.android.ui.activity.MainActivityController;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentResultListener;
import android.content.Context;
import android.widget.Button;
import androidx.lifecycle.Lifecycle;
import timber.log.Timber;
import org.walkersguide.android.ui.view.ResolveCurrentAddressView;
import org.walkersguide.android.ui.dialog.create.SaveCurrentLocationDialog;
import org.walkersguide.android.data.object_with_id.point.GPS;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.content.BroadcastReceiver;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.fragment.app.DialogFragment;
import org.walkersguide.android.ui.view.EditTextAndClearInputButton;
import android.app.Dialog;
import android.view.inputmethod.EditorInfo;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import org.walkersguide.android.ui.UiHelper;
import android.text.TextUtils;
import android.widget.Toast;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog;


public class RecordRouteFragment extends Fragment
        implements FragmentResultListener, MenuProvider, OnUpdateUiListener {
    private final static String KEY_LIST_POSITION = "listPosition";

	public static RecordRouteFragment newInstance() {
		RecordRouteFragment fragment = new RecordRouteFragment();
		return fragment;
	}

    private TextView labelRecordedRouteStatus;
    private Button buttonStartRouteRecording, buttonPauseOrResumeRecording, buttonFinishRecording;
    private LinearLayout layoutRouteRecordingInProgress;

    // recorded routes
    private int listPosition;
    private TextView labelRecordedRoutesHeading;
	private ListView listViewRecordedRoutes;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SaveCurrentLocationDialog.REQUEST_SAVE_CURRENT_LOCATION, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    EnterRouteNameDialog.REQUEST_ENTER_ROUTE_NAME, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SaveCurrentLocationDialog.REQUEST_SAVE_CURRENT_LOCATION)) {
            WalkersGuideService.addPointToRecordedRoute(
                    (GPS) bundle.getSerializable(SaveCurrentLocationDialog.EXTRA_CURRENT_LOCATION));
        } else if (requestKey.equals(EnterRouteNameDialog.REQUEST_ENTER_ROUTE_NAME)) {
            WalkersGuideService.finishRouteRecording(
                    bundle.getString(EnterRouteNameDialog.EXTRA_ROUTE_NAME));
        }
    }

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_record_route, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            listPosition = 0;
        }

        // recorded routes

        labelRecordedRouteStatus = (TextView) view.findViewById(R.id.labelRecordedRouteStatus);

        buttonStartRouteRecording = (Button) view.findViewById(R.id.buttonStartRouteRecording);
        buttonStartRouteRecording.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                WalkersGuideService.startRouteRecording();
            }
        });

        layoutRouteRecordingInProgress = (LinearLayout) view.findViewById(R.id.layoutRouteRecordingInProgress);

        buttonPauseOrResumeRecording = (Button) view.findViewById(R.id.buttonPauseOrResumeRecording);
        buttonPauseOrResumeRecording.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                WalkersGuideService.pauseOrResumeRouteRecording();
            }
        });

        Button buttonAddPointManually = (Button) view.findViewById(R.id.buttonAddPointManually);
        buttonAddPointManually.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                SaveCurrentLocationDialog.newInstance(
                        getResources().getString(R.string.buttonAddPointManuallyCD))
                    .show(getChildFragmentManager(), "SaveCurrentLocationDialog");
            }
        });

        buttonFinishRecording = (Button) view.findViewById(R.id.buttonFinishRecording);
        buttonFinishRecording.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                EnterRouteNameDialog.newInstance()
                    .show(getChildFragmentManager(), "EnterRouteNameDialog");
            }
        });

        // recorded routes

        labelRecordedRoutesHeading = (TextView) view.findViewById(R.id.labelHeading);
        listViewRecordedRoutes = (ListView) view.findViewById(R.id.listView);
        TextView labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setText(
                GlobalInstance.getStringResource(R.string.labelNoRecordedRoutes));
        listViewRecordedRoutes.setEmptyView(labelEmptyListView);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_LIST_POSITION, listPosition);
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_record_route_fragment, menu);
    }

    @Override public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemCancelRouteRecording) {
            Dialog cancelRouteRecordingDialog = new AlertDialog.Builder(getActivity())
                .setMessage(getResources().getString(R.string.cancelRouteRecordingDialogTitle))
                .setPositiveButton(
                        getResources().getString(R.string.dialogYes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                WalkersGuideService.cancelRouteRecording();
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogNo),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .create();
            cancelRouteRecordingDialog.show();

        } else {
            return false;
        }
        return true;
    }


    /**
     * pause and resume
     */

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(WalkersGuideService.ACTION_ROUTE_RECORDING_CHANGED);
        filter.addAction(WalkersGuideService.ACTION_ROUTE_RECORDING_FAILED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);

        requestUiUpdate();
        WalkersGuideService.requestRouteRecordingState();
    }

    @Override public void onUpdateUi() {
        requestUiUpdate();
    }

    private void requestUiUpdate() {
        // set route recording ui to defaults
        labelRecordedRouteStatus.setText(
                String.format(
                    "%1$s: %2$s",
                    getResources().getString(R.string.labelRecordedRouteStatus),
                    GlobalInstance.getPluralResource(R.plurals.meter, 0))
                );
        buttonStartRouteRecording.setVisibility(View.VISIBLE);
        layoutRouteRecordingInProgress.setVisibility(View.GONE);

        labelRecordedRoutesHeading.setText(
                GlobalInstance.getPluralResource(R.plurals.recordedRoute, 0));
        listViewRecordedRoutes.setAdapter(null);
        listViewRecordedRoutes.setOnScrollListener(null);

        Executors.newSingleThreadExecutor().execute(() -> {
            final ArrayList<ObjectWithId> objectList = AccessDatabase
                .getInstance()
                .getObjectListFor(
                        new DatabaseProfileRequest(DatabaseProfile.recordedRoutes()));
            (new Handler(Looper.getMainLooper())).post(() -> {
                if (isAdded()) {
                    if (! objectList.isEmpty()) {
                        loadRecordedRoutesSuccessful(objectList);
                    }
                }
            });
        });
    }

    private void loadRecordedRoutesSuccessful(ArrayList<ObjectWithId> objectList) {
        labelRecordedRoutesHeading.setText(
                GlobalInstance.getPluralResource(R.plurals.recordedRoute, objectList.size()));

        listViewRecordedRoutes.setAdapter(
                new SimpleObjectWithIdAdapter(
                    RecordRouteFragment.this.getContext(), objectList, this, false));

        // list position
        listViewRecordedRoutes.setSelection(listPosition);
        listViewRecordedRoutes.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });
    }


    /**
     * broadcasts
     */

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(WalkersGuideService.ACTION_ROUTE_RECORDING_CHANGED)) {
                RouteRecordingState recordingState = (RouteRecordingState) intent.getSerializableExtra(WalkersGuideService.EXTRA_RECORDING_STATE);
                if (recordingState != null) {
                    switch (recordingState) {

                        case RUNNING:
                        case PAUSED:
                            String message = GlobalInstance.getPluralResource(
                                    R.plurals.meter,
                                    intent.getIntExtra(WalkersGuideService.EXTRA_DISTANCE, 0));
                            int numberOfPointsByUser = intent.getIntExtra(WalkersGuideService.EXTRA_NUMBER_OF_POINTS_BY_USER, 0);
                            if (numberOfPointsByUser > 0) {
                                message += String.format(
                                        ", %1$s",
                                        GlobalInstance.getPluralResource(
                                            R.plurals.manuallyAddedPoint,
                                            numberOfPointsByUser));
                            }
                            labelRecordedRouteStatus.setText(
                                    String.format(
                                        "%1$s: %2$s",
                                        context.getResources().getString(R.string.labelRecordedRouteStatus),
                                        message)
                                    );

                            buttonStartRouteRecording.setVisibility(View.GONE);
                            layoutRouteRecordingInProgress.setVisibility(View.VISIBLE);
                            buttonPauseOrResumeRecording.setText(
                                    recordingState == RouteRecordingState.RUNNING
                                    ? context.getResources().getString(R.string.buttonPauseRecording)
                                    : context.getResources().getString(R.string.buttonResumeRecording));
                            buttonPauseOrResumeRecording.setContentDescription(
                                    recordingState == RouteRecordingState.RUNNING
                                    ? context.getResources().getString(R.string.buttonPauseRecordingCD)
                                    : context.getResources().getString(R.string.buttonResumeRecordingCD));
                            buttonFinishRecording.setVisibility(
                                    intent.getIntExtra(WalkersGuideService.EXTRA_NUMBER_OF_POINTS, 0) >= 2
                                    ? View.VISIBLE : View.GONE);
                            break;

                        default:
                            requestUiUpdate();
                    }
                }

            } else if (intent.getAction().equals(WalkersGuideService.ACTION_ROUTE_RECORDING_FAILED)) {
                SimpleMessageDialog.newInstance(
                        intent.getStringExtra(WalkersGuideService.EXTRA_ROUTE_RECORDING_FAILED_MESSAGE))
                    .show(getChildFragmentManager(), "SimpleMessageDialog");
            }
        }
    };

}
