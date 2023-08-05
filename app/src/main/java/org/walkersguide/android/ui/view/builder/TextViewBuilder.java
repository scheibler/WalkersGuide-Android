package org.walkersguide.android.ui.view.builder;

import org.walkersguide.android.ui.UiHelper;
import android.text.util.Linkify;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout.LayoutParams;
import android.widget.LinearLayout;

import android.view.View;

import android.widget.TextView;

import android.annotation.TargetApi;

import org.walkersguide.android.util.Helper;
import org.walkersguide.android.util.GlobalInstance;
import android.content.Context;
import android.view.Gravity;
import android.text.method.LinkMovementMethod;
import android.text.Spanned;
import java.lang.CharSequence;
import androidx.core.view.ViewCompat;


public class TextViewBuilder {
    private static final int MARGIN_TOP = 8;

    private TextView label;

    public TextViewBuilder(Context context, CharSequence text) {
        initialize(
                context,
                text,
                new LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    public TextViewBuilder(Context context, String text, LayoutParams lp) {
        initialize(context, text, lp);
    }

    private void initialize(Context context, CharSequence text, LayoutParams lp) {
        this.label = new TextView(context);
        this.label.setText(text);
        this.label.setLayoutParams(lp);
        this.label.setFocusable(true);
    }


    // optional

    public TextViewBuilder setId(int id) {
        this.label.setId(id);
        return this;
    }

    public TextViewBuilder addTopMargin() {
        MarginLayoutParams lp = (MarginLayoutParams) this.label.getLayoutParams();
        lp.topMargin = (int) (MARGIN_TOP * GlobalInstance.getContext().getResources().getDisplayMetrics().density);
        this.label.setLayoutParams(lp);
        return this;
    }

    public TextViewBuilder announceChanges() {
        this.label.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        return this;
    }

    public TextViewBuilder centerTextVertically() {
        this.label.setGravity(Gravity.CENTER_VERTICAL);
        return this;
    }

    public TextViewBuilder containsPhoneNumber() {
        this.label.setAutoLinkMask(Linkify.PHONE_NUMBERS);
        return this;
    }

    public TextViewBuilder containsEmailAddress() {
        this.label.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);
        return this;
    }

    public TextViewBuilder containsPostAddress() {
        this.label.setAutoLinkMask(Linkify.MAP_ADDRESSES);
        return this;
    }

    public TextViewBuilder containsUrl() {
        this.label.setAutoLinkMask(Linkify.WEB_URLS);
        this.label.setMovementMethod(LinkMovementMethod.getInstance());
        return this;
    }

    @TargetApi(android.os.Build.VERSION_CODES.P)
    public TextViewBuilder isHeading() {
        this.label.setText(
                UiHelper.boldAndRed(
                    this.label.getText().toString()));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            this.label.setAccessibilityHeading(true);
        }
        return this;
    }

    public TextViewBuilder isLabelFor(int id) {
        this.label.setLabelFor(id);
        return this;
    }

        public TextViewBuilder styleAsButton() {
            this.label.setText(
                    UiHelper.bold(
                        this.label.getText().toString()));
            ViewCompat.setAccessibilityDelegate(
                    this.label, UiHelper.getAccessibilityDelegateViewClassButton());
            return this;
        }


    // create TextView

    public TextView create() {
        return this.label;
    }

}
