package org.walkersguide.android.ui.fragment.tabs.overview;

import androidx.core.view.MenuProvider;
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


public class OverviewFragment extends Fragment implements FragmentResultListener, MenuProvider {
    private final static String KEY_LIST_POSITION = "listPosition";

	public static OverviewFragment newInstance() {
		OverviewFragment fragment = new OverviewFragment();
		return fragment;
	}


    private MainActivityController mainActivityController;

    // pinned points
    private int listPosition;
    private TextView labelHeading;
	private ListView listViewPinnedPoints;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectRouteOrSimulationPointDialog.REQUEST_SELECT_POINT, this, this);
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

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectRouteOrSimulationPointDialog.REQUEST_SELECT_POINT)) {
            Point point = (Point) bundle.getSerializable(SelectRouteOrSimulationPointDialog.EXTRA_POINT);
            Timber.d("onFragmentResult: point=%1$s", point);
            WhereToPut whereToPut = (WhereToPut) bundle.getSerializable(SelectRouteOrSimulationPointDialog.EXTRA_WHERE_TO_PUT);
            if (point != null
                    && whereToPut == SelectRouteOrSimulationPointDialog.WhereToPut.PINNED_POINT) {
                DatabaseProfile.pinnedPoints().add(point);
                requestUiUpdate();
                // show
                mainActivityController.addFragment(
                        ObjectDetailsTabLayoutFragment.details(point));
            }
        }
    }

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_overview, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            listPosition = 0;
        }

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        ImageButton buttonAddPoint = (ImageButton) view.findViewById(R.id.buttonAction);
        buttonAddPoint.setContentDescription(
                getResources().getString(R.string.dialogAdd));
        buttonAddPoint.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                SelectRouteOrSimulationPointDialog.newInstance(
                        SelectRouteOrSimulationPointDialog.WhereToPut.PINNED_POINT)
                    .show(getChildFragmentManager(), "SelectRouteOrSimulationPointDialog");
            }
        });

        listViewPinnedPoints = (ListView) view.findViewById(R.id.listView);
        TextView labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setText(
                GlobalInstance.getStringResource(R.string.messageNoPinnedPoints));
        listViewPinnedPoints.setEmptyView(labelEmptyListView);

        /*
        Button buttonHistory = new Button(getActivity());
        buttonHistory.setLayoutParams(
                new LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        buttonHistory.setText(
                getResources().getString(R.string.buttonHistory));
        listViewPinnedPoints.addFooterView(buttonHistory, null, true);
        */

        Button buttonHistory = (Button) view.findViewById(R.id.buttonHistory);
        buttonHistory.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mainActivityController.addFragment(
                        HistoryTabLayoutFragment.newInstance());
            }
        });
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_LIST_POSITION, listPosition);
    }


    /**
     * menu
     */

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_toolbar_overview_fragment, menu);
    }

    @Override public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemRefresh) {
            listPosition = 0;
            requestUiUpdate();

        } else if (item.getItemId() == R.id.menuItemClearPinnedPoints) {
            AccessDatabase.getInstance().clearDatabaseProfile(DatabaseProfile.pinnedPoints());
            requestUiUpdate();

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
    }

    @Override public void onResume() {
        super.onResume();
        requestUiUpdate();
    }

    private void requestUiUpdate() {
        labelHeading.setText(
                GlobalInstance.getPluralResource(R.plurals.pinnedPoint, 0));
        listViewPinnedPoints.setAdapter(null);
        listViewPinnedPoints.setOnScrollListener(null);

        Executors.newSingleThreadExecutor().execute(() -> {
            final ArrayList<ObjectWithId> objectList = AccessDatabase
                .getInstance()
                .getObjectListFor(
                        new DatabaseProfileRequest(
                            DatabaseProfile.pinnedPoints(), null, SortMethod.DISTANCE_ASC));
            (new Handler(Looper.getMainLooper())).post(() -> {
                if (isAdded()) {
                    if (! objectList.isEmpty()) {
                        loadPinnedPointsSuccessful(objectList);
                    }
                }
            });
        });
    }

    private void loadPinnedPointsSuccessful(ArrayList<ObjectWithId> objectList) {
        labelHeading.setText(
                GlobalInstance.getPluralResource(R.plurals.pinnedPoint, objectList.size()));

        listViewPinnedPoints.setAdapter(
                new SimpleObjectWithIdAdapter(
                    OverviewFragment.this.getContext(), objectList));

        // list position
        listViewPinnedPoints.setSelection(listPosition);
        listViewPinnedPoints.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });
    }

}
