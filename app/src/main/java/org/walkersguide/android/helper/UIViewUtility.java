package org.walkersguide.android.helper;

import android.content.Context;

import android.view.ViewGroup.LayoutParams;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


public class UIViewUtility {

    public static Button createButton(Context context, int id, String text) {
    	Button button = new Button(context);
        button.setId(id);
    	button.setLayoutParams(
                new LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                );
        button.setText(text);
        return button;
    }

    public static TextView createTextView(Context context, int id, String text) {
    	TextView textView = new TextView(context);
        textView.setId(id);
    	textView.setLayoutParams(
                new LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                );
        textView.setText(text);
        return textView;
    }

}
