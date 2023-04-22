package org.walkersguide.android.ui;

import androidx.core.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;

import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import timber.log.Timber;
import androidx.fragment.app.DialogFragment;
import android.view.Window;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import android.app.Dialog;
import android.app.Activity;
import androidx.fragment.app.FragmentActivity;
import java.util.List;
import org.walkersguide.android.ui.dialog.toolbar.BearingDetailsDialog;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;


public class UiHelper {


    /**
     * keyboard
     */

    public static boolean isDoSomeThingEditorAction(int givenActionId, int wantedActionId, KeyEvent event) {
        return givenActionId == wantedActionId
            || (   givenActionId == EditorInfo.IME_ACTION_UNSPECIFIED
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
    }

    public static void hideKeyboard(Activity activity) {
        if (activity != null) {
            hideKeyboard(activity.getWindow());
        }
    }

    public static void hideKeyboard(Fragment fragment) {
        if (fragment != null) {
            hideKeyboard(fragment.getActivity());
        }
    }

    public static void hideKeyboard(DialogFragment dialogFragment) {
        if (dialogFragment != null) {
            Dialog dialog = dialogFragment.getDialog();
            if (dialog != null) {
                hideKeyboard(dialog.getWindow());
            } else {
                hideKeyboard(dialogFragment.getActivity());
            }
        }
    }

    private static void hideKeyboard(Window window) {
        if (window != null) {
            (new WindowInsetsControllerCompat(window, window.getDecorView()))
                .hide(WindowInsetsCompat.Type.ime());
        }
    }


    /**
     * format strings
     */

    public static SpannableString boldAndRed(String text) {
        return boldAndRed(text, 0, text.length());
    }

    public static SpannableString boldAndRed(String text, int begin, int end) {
        if (begin < 0 || begin >= text.length()) {
            begin = 0;
        }
        if (end < 0 || end >= text.length()) {
            end = text.length();
        }
        SpannableString spanString = new SpannableString(text);
        spanString.setSpan(
                new StyleSpan(
                    Typeface.BOLD),
                begin, end, 0);
        spanString.setSpan(
                new ForegroundColorSpan(
                    ContextCompat.getColor(GlobalInstance.getContext(), R.color.heading)),
                begin, end, 0);
        return spanString;
    }

}
