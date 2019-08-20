package org.walkersguide.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import android.support.v4.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONException;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper.SortByNameASC;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.activity.SegmentDetailsActivity;
import org.walkersguide.android.ui.adapter.SegmentWrapperAdapter;
import org.walkersguide.android.util.Constants;


public class ExcludedWaysDialog extends DialogFragment {

    // Store instance variables
    private AccessDatabase accessDatabaseInstance;
    private int listPosition;

    // ui components
    private ListView listViewExcludedWays;
    private TextView labelHeading, labelEmptyListView;

    public static ExcludedWaysDialog newInstance() {
        ExcludedWaysDialog excludedWaysDialogInstance = new ExcludedWaysDialog();
        return excludedWaysDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        accessDatabaseInstance = AccessDatabase.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt("listPosition");
        } else {
            listPosition = -1;
        }

        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_heading_and_list_view_with_refresh_button, nullParent);
        // heading
        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        ImageButton buttonRefresh = (ImageButton) view.findViewById(R.id.buttonRefresh);
        buttonRefresh.setVisibility(View.GONE);

        listViewExcludedWays = (ListView) view.findViewById(R.id.listView);
        listViewExcludedWays.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                SegmentWrapper segmentWrapper = (SegmentWrapper) parent.getItemAtPosition(position);
                if (segmentWrapper != null) {
                    Intent detailsIntent = new Intent(getActivity(), SegmentDetailsActivity.class);
                    try {
                        detailsIntent.putExtra(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, segmentWrapper.toJson().toString());
                    } catch (JSONException e) {
                        detailsIntent.putExtra(Constants.SEGMENT_DETAILS_ACTIVITY_EXTRA.JSON_SEGMENT_SERIALIZED, "");
                    }
                    startActivity(detailsIntent);
                }
            }
        });
        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setVisibility(View.GONE);
        listViewExcludedWays.setEmptyView(labelEmptyListView);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.excludedWaysDialogTitle))
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
        }
        // load excluded ways
        prepareExcludedWays();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("listPosition",  listPosition);
    }

    private void prepareExcludedWays() {
        ArrayList<SegmentWrapper> excludedWaysList = accessDatabaseInstance.getExcludedWaysList();
        Collections.sort(excludedWaysList, new SortByNameASC());
        // heading
        labelHeading.setText(
                String.format(
                    getResources().getString(R.string.labelSelectHistoryPointDialogHeaderSuccess),
                    getResources().getQuantityString(
                        R.plurals.way, excludedWaysList.size(), excludedWaysList.size()),
                    StringUtility.formatProfileSortCriteria(
                        getActivity(), Constants.SORT_CRITERIA.NAME_ASC))
                );
        // list adapter
        listViewExcludedWays.setAdapter(
                new SegmentWrapperAdapter(getActivity(), excludedWaysList));
        // list position
        listViewExcludedWays.setSelection(listPosition);
        listViewExcludedWays.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });
    }

}
