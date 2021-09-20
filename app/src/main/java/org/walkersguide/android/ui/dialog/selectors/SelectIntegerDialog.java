    package org.walkersguide.android.ui.dialog.selectors;

import org.walkersguide.android.R;

import java.util.ArrayList;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.View;

import java.util.Collections;
import java.util.Calendar;
import java.util.Arrays;
import org.walkersguide.android.helper.StringUtility;
import java.util.List;


public class SelectIntegerDialog extends DialogFragment {
    private static final String KEY_TOKEN = "token";
    private static final String KEY_SELECTED_INTEGER = "selectedInteger";

    public interface IntegerSelector {
        public void integerSelected(Token token, Integer newInteger);
    }

    public enum Token {
        COMPASS_DIRECTION
    }


    // Store instance variables
    private IntegerSelector selector;
    private Token token;
    private List<Integer> integerList;

    public static SelectIntegerDialog newInstance(Token token, Integer selectedInteger) {
        SelectIntegerDialog dialog = new SelectIntegerDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_TOKEN, token);
        args.putSerializable(KEY_SELECTED_INTEGER, selectedInteger);
        dialog.setArguments(args);
        return dialog;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof IntegerSelector) {
            selector = (IntegerSelector) getTargetFragment();
        } else if (context instanceof Activity
                && (Activity) context instanceof IntegerSelector) {
            selector = (IntegerSelector) context;
        }
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        token = (Token) getArguments().getSerializable(KEY_TOKEN);

        String dialogTitle = "";
        switch (token) {
            case COMPASS_DIRECTION:
                dialogTitle = getResources().getString(R.string.selectIntegerDialogTitleCompassDirection);
                break;
        }

        integerList = new ArrayList<Integer>();
        switch (token) {
            case COMPASS_DIRECTION:
                integerList = Arrays.<Integer>asList(
                        new Integer[] {0, 23, 45, 68, 90, 113, 135, 158, 180, 203, 225, 248, 270, 293, 315, 338} );
                break;
        }

        int indexOfSelectedValue = -1;
        String[] formattedIntegerArray = new String[integerList.size()];
        for (int i=0; i<integerList.size(); i++) {
            switch (token) {
                case COMPASS_DIRECTION:
                    formattedIntegerArray[i] = String.format("%1$d°", integerList.get(i));
                    if (i%2 == 0) {
                        formattedIntegerArray[i] = String.format(
                                "%1$d° (%2$s)",
                                integerList.get(i),
                                StringUtility.formatGeographicDirection(integerList.get(i)));
                    } else {
                        formattedIntegerArray[i] = String.format("%1$d,5°", (integerList.get(i)-1));
                    }
                    break;
            }
            // selected
            if (integerList.get(i) == (Integer) getArguments().getSerializable(KEY_SELECTED_INTEGER)) {
                indexOfSelectedValue = i;
            }
        }

        return  new AlertDialog.Builder(getActivity())
            .setTitle(dialogTitle)
            .setSingleChoiceItems(
                    formattedIntegerArray,
                    indexOfSelectedValue,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (selector != null
                                    && which < integerList.size()) {
                                selector.integerSelected(
                                        token, integerList.get(which));
                            }
                            dismiss();
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
            .create();
    }

}
