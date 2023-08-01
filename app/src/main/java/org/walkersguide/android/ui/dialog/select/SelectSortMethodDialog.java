package org.walkersguide.android.ui.dialog.select;

import androidx.appcompat.app.AppCompatActivity;
import org.walkersguide.android.R;
import org.walkersguide.android.database.SortMethod;
import androidx.appcompat.app.AlertDialog;
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
    public static final String REQUEST_SELECT_SORT_METHOD = "selectSortMethod";
    public static final String EXTRA_SORT_METHOD = "sortMethod";


    // instance constructors

    public static SelectSortMethodDialog newInstance(
            ArrayList<SortMethod> sortMethodList, SortMethod selectedSortMethod) {
        SelectSortMethodDialog dialog= new SelectSortMethodDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SORT_METHOD_LIST, sortMethodList);
        args.putSerializable(KEY_SELECTED_SORT_METHOD, selectedSortMethod);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_SORT_METHOD_LIST = "sortMethodList";
    private static final String KEY_SELECTED_SORT_METHOD = "selectedSortMethod";

    private ArrayList<SortMethod> sortMethodList;
    private SortMethod selectedSortMethod;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        sortMethodList = (ArrayList<SortMethod>) getArguments().getSerializable(KEY_SORT_METHOD_LIST);
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
                        Bundle result = new Bundle();
                        result.putSerializable(EXTRA_SORT_METHOD, newSortMethod);
                        getParentFragmentManager().setFragmentResult(REQUEST_SELECT_SORT_METHOD, result);
                    }
                    dismiss();
                }
            });

            // fill listview
            listViewItems.setAdapter(
                    new ArrayAdapter<SortMethod>(
                        getActivity(), android.R.layout.select_dialog_singlechoice, sortMethodList));
            listViewItems.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            // select list item
            if (selectedSortMethod != null) {
                listViewItems.setItemChecked(
                        sortMethodList.indexOf(selectedSortMethod), true);
            }
        }
    }

}
