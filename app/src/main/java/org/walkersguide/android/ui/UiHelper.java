package org.walkersguide.android.ui;

import timber.log.Timber;

import org.walkersguide.android.BuildConfig;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import androidx.core.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;

import org.walkersguide.android.R;
import org.walkersguide.android.util.GlobalInstance;
import androidx.fragment.app.DialogFragment;
import android.view.Window;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import android.app.Dialog;
import android.app.Activity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import androidx.annotation.NonNull;
import android.widget.Button;
import android.text.style.LeadingMarginSpan;
import android.text.Spanned;
import android.os.Build;
import android.text.Html;
import android.text.style.URLSpan;
import android.view.accessibility.AccessibilityEvent;
import android.graphics.Bitmap;
import android.graphics.Matrix;


public class UiHelper {

    public static int convertDpToPx(int dp) {
        return (int) (dp * GlobalInstance.getContext().getResources().getDisplayMetrics().density);
    }


    /**
     * TextView and EditText
     */

    public static View.AccessibilityDelegate getAccessibilityDelegateToMuteContentChangedEventsWhileFocussed() {
        return new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
                if (host.isAccessibilityFocused()
                        && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                    return;
                }
                super.onInitializeAccessibilityEvent(host, event);
            }
        };
    }

    public static boolean isDoSomeThingEditorAction(int givenActionId, int wantedActionId, KeyEvent event) {
        if (givenActionId == wantedActionId) {
            return true;
        } else if (givenActionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
            return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                || event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
        } else {
            return false;
        }
    }


    /**
     * hide keyboard
     */

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
     * fragment in background check
     */

    public static boolean isInBackground(Fragment fragment) {
        if (fragment instanceof DialogFragment
                && ((DialogFragment) fragment).getDialog() != null) {
            return false;
        }
        return fragment != null && ! fragment.getActivity().hasWindowFocus();
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
     * images
     */

    public static Bitmap rotateImage(Bitmap source, int angleInDegrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate((float) angleInDegrees);
        return Bitmap.createBitmap(
                source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
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

    public static SpannableString urlStyle(String text) {
        SpannableString spanString = new SpannableString(text);
        spanString.setSpan(new URLSpan(""), 0, spanString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spanString;
    }

    public static Spanned getPublicTransportDataSourceText() {
        return fromHtml(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelPublicTransportMessage),
                    BuildConfig.PTE_LINK_MAIN_WEBSITE,
                    BuildConfig.PTE_LINK_PROVIDER_LIST));
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html){
        if (html == null) {
            return new SpannableString("");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // FROM_HTML_MODE_LEGACY is the behaviour that was used for versions below android N
            // we are using this flag to give a consistent behaviour
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }

}
