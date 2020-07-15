package org.walkersguide.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import java.util.ArrayList;

import org.json.JSONException;

import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.R;
import android.widget.AbsListView;
import android.widget.ListView;
import org.json.JSONArray;
import android.app.Activity;
import org.walkersguide.android.ui.adapter.PointWrapperAdapter;
import android.widget.AdapterView;



public class SelectPointWrapperFromListDialog extends DialogFragment {

    public interface PointWrapperSelectedListener {
        public void pointWrapperSelectedFromList(PointWrapper pointWrapper);
    }

    private PointWrapperSelectedListener pointWrapperSelectedListener;
    private int listPosition;

    // ui components
    private ListView listViewPointWrapperes;
    private TextView labelListViewEmpty;

    public static SelectPointWrapperFromListDialog newInstance(String title, ArrayList<PointWrapper> pointWrapperList) {
        SelectPointWrapperFromListDialog selectPointWrapperFromListDialogInstance = new SelectPointWrapperFromListDialog();
        JSONArray jsonPointWrapperList = new JSONArray();
        for (PointWrapper pointWrapper : pointWrapperList) {
            try {
                jsonPointWrapperList.put(pointWrapper.toJson());
            } catch (JSONException | NullPointerException e) {}
        }
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("jsonPointWrapperList", jsonPointWrapperList.toString());
        selectPointWrapperFromListDialogInstance.setArguments(args);
        return selectPointWrapperFromListDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof PointWrapperSelectedListener) {
            pointWrapperSelectedListener = (PointWrapperSelectedListener) getTargetFragment();
        } else if (context instanceof Activity
                && (Activity) context instanceof PointWrapperSelectedListener) {
            pointWrapperSelectedListener = (PointWrapperSelectedListener) context;
                }
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
        View view = inflater.inflate(R.layout.layout_single_list_view, nullParent);

        ListView listViewPointWrapperes = (ListView) view.findViewById(R.id.listView);
        TextView labelListViewEmpty    = (TextView) view.findViewById(R.id.labelListViewEmpty);
        labelListViewEmpty.setVisibility(View.GONE);
        listViewPointWrapperes.setEmptyView(labelListViewEmpty);

        ArrayList<PointWrapper> pointWrapperList = new ArrayList<PointWrapper>();
        try {
            JSONArray jsonPointWrapperList = new JSONArray(
                    getArguments().getString("jsonPointWrapperList"));
            for (int i=0; i<jsonPointWrapperList.length(); i++) {
                pointWrapperList.add(
                        new PointWrapper(
                            getActivity(), jsonPointWrapperList.getJSONObject(i)));
            }
        } catch (JSONException e) {}
        listViewPointWrapperes.setAdapter(
                new PointWrapperAdapter(getActivity(), pointWrapperList));

        listViewPointWrapperes.setSelection(listPosition);
        listViewPointWrapperes.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });

        listViewPointWrapperes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                PointWrapper pointWrapper = (PointWrapper) parent.getItemAtPosition(position);
                if (pointWrapper != null && pointWrapperSelectedListener != null) {
                    pointWrapperSelectedListener.pointWrapperSelectedFromList(pointWrapper);
                    dismiss();
                }
            }
        });

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getArguments().getString("title"))
            .setView(view)
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    }
                    )
            .create();
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("listPosition",  listPosition);
    }

    @Override public void onStop() {
        super.onStop();
        pointWrapperSelectedListener = null;
    }

}
