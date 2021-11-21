package org.walkersguide.android.ui.builder;

import android.text.util.Linkify;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout.LayoutParams;
import android.widget.LinearLayout;

import android.view.View;

import android.widget.TextView;

import android.annotation.TargetApi;

import org.walkersguide.android.util.StringUtility;
import org.walkersguide.android.util.GlobalInstance;


public class TextViewBuilder {
    private static final int MARGIN_TOP = 8;

    private TextView label;

    public TextViewBuilder(String text) {
        this.label = new TextView(GlobalInstance.getContext());
        this.label.setFocusable(true);
        this.label.setText(text);
        // layout params
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        this.label.setLayoutParams(lp);
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
        return this;
    }

    @TargetApi(android.os.Build.VERSION_CODES.P)
    public TextViewBuilder isHeading() {
        this.label.setText(
                StringUtility.boldAndRed(
                    this.label.getText().toString()));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            this.label.setAccessibilityHeading(true);
        }
        return this;
    }

    public TextView create() {
        return this.label;
    }

}
