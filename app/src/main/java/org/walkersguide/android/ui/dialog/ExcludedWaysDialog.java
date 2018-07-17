package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.R;
import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.util.Constants;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.ListView;
import android.widget.AdapterView;
import org.walkersguide.android.data.basic.wrapper.SegmentWrapper;
import org.walkersguide.android.ui.activity.SegmentDetailsActivity;
import org.walkersguide.android.ui.adapter.SegmentWrapperAdapter;
import android.view.View;
import org.json.JSONException;


public class ExcludedWaysDialog extends DialogFragment {

    // Store instance variables
    private AccessDatabase accessDatabaseInstance;

    public static ExcludedWaysDialog newInstance() {
        ExcludedWaysDialog excludedWaysDialogInstance = new ExcludedWaysDialog();
        return excludedWaysDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        accessDatabaseInstance = AccessDatabase.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.excludedWaysDialogTitle))
            .setItems(
                    new String[]{getResources().getString(R.string.messagePleaseWait)},
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }
                    )
            .setNegativeButton(
                    getResources().getString(R.string.dialogClose),
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
        if(dialog != null) {
            ListView listViewItems = (ListView) dialog.getListView();
            listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
                    } else {
                        // cancel
                        dismiss();
                    }
                }
            });
            SegmentWrapperAdapter segmentWrapperAdapter = new SegmentWrapperAdapter(
                    getActivity(), accessDatabaseInstance.getExcludedWaysList());
            listViewItems.setAdapter(segmentWrapperAdapter);
        }
    }

}
