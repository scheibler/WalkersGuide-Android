package org.walkersguide.android.ui;

import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

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
import android.view.View;
import androidx.annotation.NonNull;
import android.widget.Button;
import android.text.style.LeadingMarginSpan;


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
     * an accessibility delegate, that tells talkback, that the selected ui element is a button
     */

    public static AccessibilityDelegateCompat getAccessibilityDelegateViewClassButton() {
        return new AccessibilityDelegateCompat() {
            @Override public void onInitializeAccessibilityNodeInfo(
                    @NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
            }
        };
    }


    /**
     * format strings
     */

    public static SpannableString bold(String text) {
        return styleString(text, 0, text.length(), true, false, false);
    }

    public static SpannableString red(String text) {
        return styleString(text, 0, text.length(), false, true, false);
    }

    public static SpannableString boldAndRed(String text) {
        return styleString(text, 0, text.length(), true, true, false);
    }

    private static SpannableString styleString(String text, int begin, int end,
            boolean bold, boolean red, boolean indent) {
        if (begin < 0 || begin >= text.length()) {
            begin = 0;
        }
        if (end < 0 || end >= text.length()) {
            end = text.length();
        }
        SpannableString spanString = new SpannableString(text);
        if (bold) {
            spanString.setSpan(
                    new StyleSpan(
                        Typeface.BOLD),
                    begin, end, 0);
        }
        if (red) {
            spanString.setSpan(
                    new ForegroundColorSpan(
                        ContextCompat.getColor(GlobalInstance.getContext(), R.color.heading)),
                    begin, end, 0);
        }
        if (indent) {
            spanString.setSpan(
                    new LeadingMarginSpan.Standard(2),
                    begin, end, 0);
        }
        return spanString;
    }

}
