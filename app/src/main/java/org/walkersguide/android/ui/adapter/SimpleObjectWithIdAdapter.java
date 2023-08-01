package org.walkersguide.android.ui.adapter;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.ui.view.TextViewAndActionButton;

import android.os.Bundle;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import android.content.Context;
import android.widget.BaseAdapter;
import java.util.Comparator;
import java.util.Collections;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.Angle;


public class SimpleObjectWithIdAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<? extends ObjectWithId> objectList;

    public SimpleObjectWithIdAdapter(Context context, ArrayList<? extends ObjectWithId> objectList) {
        this.context = context;
        this.objectList = objectList;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        TextViewAndActionButton layoutTextViewAndActionButton = null;
        if (convertView == null) {
            layoutTextViewAndActionButton = new TextViewAndActionButton(this.context, true);
            layoutTextViewAndActionButton.setLayoutParams(
                    new LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        } else {
            layoutTextViewAndActionButton = (TextViewAndActionButton) convertView;
        }
        layoutTextViewAndActionButton.configureAsListItem(getItem(position), true, null);
        return layoutTextViewAndActionButton;
    }

    @Override public int getCount() {
        return this.objectList.size();
    }

    @Override public ObjectWithId getItem(int position) {
        return this.objectList.get(position);
    }

    @Override public long getItemId(int position) {
        return position;
    }

    @Override public boolean isEmpty() {
        return false;
    }


    private class EntryHolder {
        public TextViewAndActionButton layoutTextViewAndActionButton;
    }

}
