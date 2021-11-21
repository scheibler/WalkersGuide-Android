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
import org.walkersguide.android.util.StringUtility;
import java.util.List;


public class SelectIntegerDialog extends DialogFragment {
    public static final String REQUEST_SELECT_INTEGER = "selectInteger";
    public static final String EXTRA_TOKEN = "token";
    public static final String EXTRA_INTEGER = "integer";


    // instance constructors

    public static SelectIntegerDialog newInstance(Token token, Integer selectedInteger) {
        SelectIntegerDialog dialog = new SelectIntegerDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_TOKEN, token);
        args.putSerializable(KEY_SELECTED_INTEGER, selectedInteger);
        dialog.setArguments(args);
        return dialog;
    }

    // dialog
    private static final String KEY_TOKEN = "token";
    private static final String KEY_SELECTED_INTEGER = "selectedInteger";

    public enum Token {
        COMPASS_DIRECTION
    }

    private Token token;
    private List<Integer> integerList;

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
                            if (which >= 0
                                    && which < integerList.size()) {
                                Bundle result = new Bundle();
                                result.putSerializable(EXTRA_TOKEN, token);
                                result.putInt(EXTRA_INTEGER, integerList.get(which));
                                getParentFragmentManager().setFragmentResult(REQUEST_SELECT_INTEGER, result);
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
