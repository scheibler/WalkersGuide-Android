package org.walkersguide.android.ui.dialog.selectors;

import org.walkersguide.android.R;
import org.walkersguide.android.database.SortMethod;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import android.app.Activity;
import java.util.Arrays;


public class SelectSortMethodDialog extends DialogFragment {
    private static final String KEY_SELECTED_SORT_METHOD = "selectedSortMethod";

    public interface SelectSortMethodListener {
        public void sortMethodSelected(SortMethod newSortMethod);
    }


    // Store instance variables
    private SelectSortMethodListener listener;
    private SortMethod selectedSortMethod;

    public static SelectSortMethodDialog newInstance(SortMethod selectedSortMethod) {
        SelectSortMethodDialog dialog= new SelectSortMethodDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SELECTED_SORT_METHOD, selectedSortMethod);
        dialog.setArguments(args);
        return dialog;
    }


    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof SelectSortMethodListener) {
            listener = (SelectSortMethodListener) getTargetFragment();
        } else if (context instanceof Activity
                && (Activity) context instanceof SelectSortMethodListener) {
            listener = (SelectSortMethodListener) context;
                }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        selectedSortMethod = (SortMethod) getArguments().getSerializable(KEY_SELECTED_SORT_METHOD);

        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.selectSortMethodDialogTitle))
            .setItems(
                    new String[]{getResources().getString(R.string.messagePleaseWait)},
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
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

    @Override public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {

            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    SortMethod newSortMethod = (SortMethod) parent.getItemAtPosition(position);
                    if (newSortMethod != null) {
                        if (listener != null) {
                            listener.sortMethodSelected(newSortMethod);
                        }
                    }
                    dismiss();
                }
            });

            // fill listview
            ArrayList<SortMethod> sortMethodList = new ArrayList<SortMethod>(Arrays.asList(SortMethod.values()));
            listViewItems.setAdapter(
                    new ArrayAdapter<SortMethod>(
                        getActivity(), android.R.layout.select_dialog_singlechoice, sortMethodList));

            // select list item
            if (selectedSortMethod != null) {
                listViewItems.setItemChecked(
                        sortMethodList.indexOf(selectedSortMethod), true);
            }
        }
    }

    @Override public void onStop() {
        super.onStop();
        listener = null;
    }

}
