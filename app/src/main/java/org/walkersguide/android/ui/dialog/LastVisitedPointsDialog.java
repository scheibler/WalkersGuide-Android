package org.walkersguide.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Vibrator;

import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import org.json.JSONException;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointProfileObject;
import org.walkersguide.android.data.profile.HistoryPointProfile;
import org.walkersguide.android.data.route.RouteObject;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.HistoryPointProfileListener;
import org.walkersguide.android.R;
import org.walkersguide.android.server.POIManager;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;


public class LastVisitedPointsDialog extends DialogFragment implements HistoryPointProfileListener {
    private static final int NUMBER_OF_MAX_LAST_VISITED_POINTS = 100;

    private AccessDatabase accessDatabaseInstance;
    private POIManager poiManagerInstance;
    private Vibrator vibrator;
    private int listPosition;

    // ui components
    private ImageButton buttonRefresh;
    private ListView listViewLastVisitedPoints;
    private TextView labelHeading, labelEmptyListView;

    public static LastVisitedPointsDialog newInstance() {
        LastVisitedPointsDialog lastVisitedPointsDialogInstance = new LastVisitedPointsDialog();
        return lastVisitedPointsDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        accessDatabaseInstance = AccessDatabase.getInstance(context);
        poiManagerInstance = POIManager.getInstance(context);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        // listen for intents
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_SHAKE_DETECTED);
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
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
        View view = inflater.inflate(R.layout.fragment_poi, nullParent);

        Button buttonSelectProfile = (Button) view.findViewById(R.id.buttonSelectProfile);
        buttonSelectProfile.setVisibility(View.GONE);
        AutoCompleteTextView editSearch = (AutoCompleteTextView) view.findViewById(R.id.editInput);
        editSearch.setVisibility(View.GONE);
        ImageButton buttonClearSearch = (ImageButton) view.findViewById(R.id.buttonClearInput);
        buttonClearSearch.setVisibility(View.GONE);

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (poiManagerInstance.historyPointProfileRequestInProgress()) {
                    poiManagerInstance.cancelHistoryPointProfileRequest();
                } else {
                    prepareHistoryPointRequest();
                }
            }
        });

        listViewLastVisitedPoints = (ListView) view.findViewById(R.id.listView);
        listViewLastVisitedPoints.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                PointProfileObject selectedPoint = (PointProfileObject) parent.getItemAtPosition(position);
                Intent pointDetailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                try {
                    pointDetailsIntent.putExtra(
                            Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED,
                            selectedPoint.toJson().toString());
                } catch (JSONException e) {
                    pointDetailsIntent.putExtra(
                            Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED,
                            "");
                }
                getActivity().startActivity(pointDetailsIntent);
                dismiss();
            }
        });
        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        listViewLastVisitedPoints.setEmptyView(labelEmptyListView);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.menuItemLastVisitedPoints))
            .setView(view)
            .setNegativeButton(
                    getResources().getString(R.string.dialogClose),
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
            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dialog.dismiss();
                }
            });
        }
        // request poi
        prepareHistoryPointRequest();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("listPosition",  listPosition);
    }

    @Override public void onStop() {
        super.onStop();
        poiManagerInstance.invalidateHistoryPointProfileRequest((LastVisitedPointsDialog) this);
    }

    private void prepareHistoryPointRequest() {
        // heading
        labelHeading.setText(
                getResources().getString(R.string.messagePleaseWait));
        buttonRefresh.setContentDescription(
                getResources().getString(R.string.buttonCancel));
        buttonRefresh.setImageResource(R.drawable.cancel);
        // list view
        listViewLastVisitedPoints.setAdapter(null);
        listViewLastVisitedPoints.setOnScrollListener(null);
        labelEmptyListView.setVisibility(View.GONE);
        // start history point profile update request
        poiManagerInstance.requestHistoryPointProfile(
                (LastVisitedPointsDialog) this, HistoryPointProfile.ID_ALL_POINTS);
    }

	@Override public void historyPointProfileRequestFinished(int returnCode, String returnMessage, HistoryPointProfile historyPointProfile, boolean resetListPosition) {
        buttonRefresh.setContentDescription(
                getResources().getString(R.string.buttonRefresh));
        buttonRefresh.setImageResource(R.drawable.refresh);

        if (historyPointProfile != null
                && historyPointProfile.getPointProfileObjectList() != null) {
            ArrayList<PointProfileObject> historyPointList = historyPointProfile.getPointProfileObjectList();
            // truncate
            if (historyPointList.size() > NUMBER_OF_MAX_LAST_VISITED_POINTS) {
                historyPointList.subList(
                        NUMBER_OF_MAX_LAST_VISITED_POINTS, historyPointList.size())
                    .clear();
            }
            // header field and list view
            labelHeading.setText(
                    String.format(
                        getResources().getString(R.string.labelSelectHistoryPointDialogHeaderSuccess),
                        getResources().getQuantityString(
                            R.plurals.lastVisitedPoint, historyPointList.size(), historyPointList.size()),
                        StringUtility.formatProfileSortCriteria(
                            getActivity(), historyPointProfile.getSortCriteria()))
                    );
            listViewLastVisitedPoints.setAdapter(
                    new ArrayAdapter<PointProfileObject>(
                        getActivity(), android.R.layout.simple_list_item_1, historyPointList));

            // list position
            if (resetListPosition) {
                listViewLastVisitedPoints.setSelection(0);
            } else {
                listViewLastVisitedPoints.setSelection(listPosition);
            }
            listViewLastVisitedPoints.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
                @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (listPosition != firstVisibleItem) {
                        listPosition = firstVisibleItem;
                    }
                }
            });

        } else {
            labelHeading.setText(returnMessage);
        }
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_SHAKE_DETECTED)) {
                vibrator.vibrate(250);
                prepareHistoryPointRequest();
            }
        }
    };

}
